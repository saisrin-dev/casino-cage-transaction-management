package dev.saisrin.cage;

import java.util.*;
import org.springframework.http.*;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
@Transactional
class EvidenceApi {
  final CageService s;
  final Attachments attachments;
  final Subjects subjects;

  EvidenceApi(CageService s, Attachments a, Subjects subjects) {
    this.s = s;
    attachments = a;
    this.subjects = subjects;
  }

  String authorize(String owner, Long id, boolean write) {
    switch (owner) {
      case "Report" -> {
        return s.report(id, write).kind;
      }
      case "Patron" -> {
        s.found(s.patrons().findById(id));
        if (write) s.require("PATRON");
        else s.read("PATRON");
        return "PATRON";
      }
      case "Transaction" -> {
        s.found(s.txs().findById(id));
        if (write) s.require("TRANSACTION");
        else s.read("TRANSACTION");
        return "TRANSACTION";
      }
      default -> throw new IllegalArgumentException("Unsupported evidence owner");
    }
  }

  @GetMapping("/attachments")
  Object list(@RequestParam String ownerType, @RequestParam Long ownerId) {
    authorize(ownerType, ownerId, false);
    return attachments.findByOwnerTypeAndOwnerId(ownerType, ownerId);
  }

  @PostMapping("/attachments")
  Object upload(
      @RequestParam String ownerType,
      @RequestParam Long ownerId,
      @RequestParam String reason,
      @RequestParam MultipartFile file)
      throws Exception {
    String scope = authorize(ownerType, ownerId, true);
    s.check(reason.length() >= 5, "Evidence reason required");
    s.check(!file.isEmpty() && file.getSize() <= 1024 * 1024, "Evidence file must be 1 byte–1 MiB");
    String name =
        Objects.toString(file.getOriginalFilename(), "evidence.txt")
            .replaceAll("[^A-Za-z0-9._-]", "_");
    s.check(name.length() < 150, "Filename too long");
    s.check(
        Set.of("text/plain", "application/pdf", "image/png", "image/jpeg")
            .contains(file.getContentType()),
        "Allowed evidence: plain text, PDF, PNG, JPEG");
    Attachment a = new Attachment();
    a.ownerType = ownerType;
    a.ownerId = ownerId;
    a.filename = name;
    a.mediaType = file.getContentType();
    a.bytes = (int) file.getSize();
    a.uploadedBy = s.actor();
    a.content = Base64.getEncoder().encodeToString(file.getBytes());
    attachments.saveAndFlush(a);
    if (ownerType.equals("Report")) {
      Report report = s.report(ownerId, true);
      if (report.snapshot == null) {
        report.revision++;
        report.status = "DRAFT";
        report.approver = null;
        report.approvedRevision = null;
        s.reports().save(report);
        s.revise(report, reason);
      }
    }
    s.audit(
        scope,
        ownerType,
        ownerId,
        "ATTACH",
        reason,
        null,
        Map.of("attachmentId", a.id, "bytes", a.bytes));
    return a;
  }

  @GetMapping("/attachments/{id}/download")
  ResponseEntity<byte[]> download(@PathVariable Long id) {
    Attachment a = s.found(attachments.findById(id));
    String scope = authorize(a.ownerType, a.ownerId, false);
    s.audit(
        scope,
        a.ownerType,
        a.ownerId,
        "EVIDENCE_ACCESS",
        "Permission-controlled evidence download",
        null,
        a.id);
    return ResponseEntity.ok()
        .contentType(MediaType.APPLICATION_OCTET_STREAM)
        .header("Content-Disposition", "attachment; filename=\"" + a.filename + "\"")
        .header("X-Content-Type-Options", "nosniff")
        .body(Base64.getDecoder().decode(a.content));
  }

  @GetMapping("/reports/{id}/subjects")
  Object subjects(@PathVariable Long id) {
    s.report(id, false);
    return subjects.findByReportId(id);
  }

  @PostMapping("/reports/{id}/subjects")
  Object subject(@PathVariable Long id, @RequestBody Map<String, Object> m) {
    Report r = s.report(id, true);
    s.stale(r, m);
    s.check(r.snapshot == null, "Submitted subjects are frozen; create a correction");
    Patron p = s.found(s.patrons().findById(s.id(m, "patronId")));
    s.check(!p.id.equals(r.patron.id), "Primary subject is already linked");
    ReportSubject link = new ReportSubject();
    link.report = r;
    link.patron = p;
    link.relationship = s.s(m, "relationship", "");
    s.check(!link.relationship.isBlank(), "Subject relationship required");
    subjects.saveAndFlush(link);
    r.revision++;
    r.approvedRevision = null;
    r.approver = null;
    r.status = "DRAFT";
    s.reports().save(r);
    s.revise(r, s.reason(m));
    s.audit(r.kind, "Report", id, "LINK_SUBJECT", s.reason(m), null, s.patronView(p));
    return link;
  }
}
