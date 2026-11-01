package dev.saisrin.cage;

import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.*;
import org.springframework.http.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
class Api {
  final CageService s;
  final FilingService filing;
  final Users users;
  final PasswordEncoder encoder;

  Api(CageService s, FilingService f, Users u, PasswordEncoder e) {
    this.s = s;
    filing = f;
    users = u;
    encoder = e;
  }

  @GetMapping("/csrf")
  Map<String, String> csrf(CsrfToken token) {
    return Map.of("token", token.getToken(), "headerName", token.getHeaderName());
  }

  @GetMapping("/demo")
  Map<String, Object> demo(
      @Value("${cage.seed}") boolean seed, @Value("${cage.simulation-controls}") boolean controls) {
    return Map.of(
        "seeded",
        seed,
        "simulationControls",
        controls,
        "label",
        "Synthetic data • Simulated filings • DEMO tax rules");
  }

  @GetMapping("/me")
  Map<String, Object> me(Authentication a) {
    CageUser u = s.found(users.findByUsername(a.getName()));
    return Map.of(
        "username",
        u.username,
        "displayName",
        u.displayName,
        "roles",
        u.roles,
        "permissions",
        a.getAuthorities().stream().map(Object::toString).toList());
  }

  @GetMapping("/dashboard")
  Map<String, Object> dashboard() {
    s.read("PATRON");
    LocalDate today = Rules.gamingDay(Instant.now(), "America/New_York", "06:00");
    var txs =
        s.txs().findAll().stream()
            .filter(t -> t.status.equals("POSTED") && t.gamingDay.equals(today))
            .toList();
    var reports =
        s.reports().findAll().stream()
            .filter(r -> s.can(r.kind) || s.can(r.kind + "_READ"))
            .toList();
    Map<String, Object> v = new HashMap<>();
    v.put("patrons", s.patrons().count());
    v.put("transactions", txs.size());
    v.put("cashIn", txs.stream().map(t -> t.cashIn).reduce(BigDecimal.ZERO, BigDecimal::add));
    v.put("cashOut", txs.stream().map(t -> t.cashOut).reduce(BigDecimal.ZERO, BigDecimal::add));
    v.put(
        "pending",
        reports.stream()
            .filter(r -> Set.of("DRAFT", "PENDING_REVIEW", "RETURNED").contains(r.status))
            .count());
    v.put("rejected", reports.stream().filter(r -> r.externalOutcome.equals("REJECTED")).count());
    v.put(
        "awaiting",
        reports.stream().filter(r -> r.filingStatus.equals("AWAITING_RESPONSE")).count());
    v.put(
        "overdue",
        reports.stream()
            .filter(
                r -> r.obligationOpen && r.dueDate != null && r.dueDate.isBefore(LocalDate.now()))
            .count());
    v.put("gamingDay", today);
    v.put(
        "tasks",
        s.tasks().findAll().stream()
            .filter(t -> !t.resolved && (s.can(t.scope) || s.can(t.scope + "_READ")))
            .toList());
    v.put(
        "recent",
        txs.stream()
            .sorted(Comparator.comparing((CageTransaction t) -> t.id).reversed())
            .limit(6)
            .toList());
    return v;
  }

  @GetMapping("/patrons")
  Object patrons() {
    return s.patronList();
  }

  @GetMapping("/patrons/{id}")
  Object patron(@PathVariable Long id) {
    s.read("PATRON");
    return s.patronView(s.found(s.patrons().findById(id)));
  }

  @PostMapping("/patrons")
  Object createPatron(@RequestBody Map<String, Object> m) {
    return s.patronView(s.savePatron(null, m));
  }

  @PutMapping("/patrons/{id}")
  Object editPatron(@PathVariable Long id, @RequestBody Map<String, Object> m) {
    return s.patronView(s.savePatron(id, m));
  }

  @PostMapping("/patrons/{id}/merge")
  void merge(@PathVariable Long id, @RequestBody Map<String, Object> m) {
    s.mergePatron(id, m);
  }

  @PostMapping("/patrons/{id}/w9")
  @org.springframework.transaction.annotation.Transactional
  Object w9(@PathVariable Long id, @RequestBody Map<String, Object> m) {
    s.require("TAX");
    Patron p = s.found(s.patrons().findById(id));
    s.stale(p, m);
    String status = s.s(m, "status", "");
    s.check(
        Set.of("REQUESTED", "RECEIVED", "VERIFIED", "REPLACEMENT").contains(status),
        "Invalid W-9 state");
    String old = p.w9Status;
    p.w9Status = status;
    s.patrons().save(p);
    s.audit("TAX", "Patron", id, "W9", s.reason(m), old, status);
    return s.patronView(p);
  }

  @GetMapping("/transactions")
  Object txs() {
    s.read("TRANSACTION");
    return s.txs().findAll().stream()
        .sorted(Comparator.comparing((CageTransaction t) -> t.id).reversed())
        .toList();
  }

  @PostMapping("/transactions")
  Object createTx(@RequestBody Map<String, Object> m) {
    return s.createTransaction(m);
  }

  @PostMapping("/transactions/{id}/{action}")
  Object txAction(
      @PathVariable Long id, @PathVariable String action, @RequestBody Map<String, Object> m) {
    return s.transactionAction(id, action, m);
  }

  @GetMapping("/types")
  Object types() {
    return s.types().findAll();
  }

  @PostMapping("/types")
  Object type(@RequestBody Map<String, Object> m) {
    return s.saveType(null, m);
  }

  @PutMapping("/types/{id}")
  Object type(@PathVariable Long id, @RequestBody Map<String, Object> m) {
    return s.saveType(id, m);
  }

  @PostMapping("/types/{id}/{action}")
  void typeAction(
      @PathVariable Long id, @PathVariable String action, @RequestBody Map<String, Object> m) {
    s.typeAction(id, action, m);
  }

  @GetMapping("/reports")
  Object reports(@RequestParam String kind) {
    s.read(kind);
    return s.reports().findAll().stream()
        .filter(r -> r.kind.equals(kind))
        .sorted(Comparator.comparing((Report r) -> r.id).reversed())
        .toList();
  }

  @GetMapping("/reports/{id}")
  Object report(@PathVariable Long id) {
    return s.report(id, false);
  }

  @GetMapping("/reports/{id}/validation")
  Object validate(@PathVariable Long id) {
    return Map.of(
        "errors", s.validation(s.report(id, false)), "schema", "CAGE-MOCK-1", "simulation", true);
  }

  @GetMapping("/reports/{id}/versions")
  Object versions(@PathVariable Long id) {
    s.report(id, false);
    return s.revisions().findByReportIdOrderByRevisionDesc(id);
  }

  @GetMapping("/reports/{id}/transactions")
  Object contributing(@PathVariable Long id) {
    Report r = s.report(id, false);
    return s.txs().findAll().stream()
        .filter(t -> t.patron.id.equals(r.patron.id) && Objects.equals(t.gamingDay, r.gamingDay))
        .toList();
  }

  @PostMapping("/reports")
  Object createReport(@RequestBody Map<String, Object> m) {
    return s.createReport(s.s(m, "kind", "SAR"), m);
  }

  @PutMapping("/reports/{id}")
  Object editReport(@PathVariable Long id, @RequestBody Map<String, Object> m) {
    return s.editReport(id, m, true);
  }

  @PostMapping("/reports/{id}/{action}")
  Object reportAction(
      @PathVariable Long id, @PathVariable String action, @RequestBody Map<String, Object> m) {
    return s.reportAction(id, action, m);
  }

  @GetMapping("/batches")
  Object batches(@RequestParam String kind) {
    s.read(kind);
    return s.batches().findAll().stream()
        .filter(b -> b.kind.equals(kind))
        .sorted(Comparator.comparing((FilingBatch b) -> b.id).reversed())
        .toList();
  }

  @PostMapping("/batches")
  Object batch(@RequestBody Map<String, Object> m) {
    return s.makeBatch(m);
  }

  @GetMapping("/batches/{id}/items")
  Object batchItems(@PathVariable Long id) {
    s.batch(id);
    return s.items().findByBatchId(id);
  }

  @PostMapping(value = "/batches/{id}/export", produces = MediaType.APPLICATION_JSON_VALUE)
  ResponseEntity<String> export(@PathVariable Long id) {
    return ResponseEntity.ok()
        .header("Content-Disposition", "attachment; filename=MOCK-batch-" + id + ".json")
        .body(s.exportBatch(id));
  }

  @PostMapping("/batches/{id}/{action}")
  Object batchAction(
      @PathVariable Long id, @PathVariable String action, @RequestBody Map<String, Object> m) {
    return filing.action(id, action, m);
  }

  @GetMapping("/audit")
  Object audit(
      @RequestParam(required = false) String entityType,
      @RequestParam(required = false) Long entityId) {
    s.check(
        entityType != null && entityId != null || s.can("SETUP") || s.can("AUDIT"),
        "Select a record to view history");
    return s.audits().findAll().stream()
        .filter(
            a ->
                s.can(a.scope)
                    || s.can(a.scope + "_READ")
                    || (s.can("AUDIT") && !a.scope.equals("SAR") && !a.scope.equals("TAX")))
        .filter(a -> entityType == null || a.entityType.equals(entityType))
        .filter(a -> entityId == null || a.entityId.equals(entityId))
        .sorted(Comparator.comparing((AuditEvent a) -> a.id).reversed())
        .limit(200)
        .toList();
  }

  @GetMapping("/property")
  Object property() {
    return s.found(s.properties().findById(1L));
  }

  @PutMapping("/property")
  @org.springframework.transaction.annotation.Transactional
  Object property(@RequestBody Map<String, Object> m) {
    s.require("SETUP");
    Property p = s.properties().lock();
    s.stale(p, m);
    Object old = s.serialize(p);
    p.name = s.s(m, "name", p.name);
    p.contact = s.s(m, "contact", p.contact);
    String zone = s.s(m, "zone", p.zone), cutoff = s.s(m, "cutoff", p.cutoff);
    s.check(
        zone.equals(p.zone) && cutoff.equals(p.cutoff),
        "Gaming-day rules are immutable in this demo; proposed changes require a migration and"
            + " compliance review");
    s.properties().save(p);
    s.audit("SETUP", "Property", 1L, "EDIT", s.reason(m), old, p);
    return p;
  }

  @GetMapping("/configurations")
  Object configs() {
    s.require("SETUP");
    return s.configs().findAll();
  }

  @PutMapping("/configurations/{id}")
  void config(@PathVariable Long id, @RequestBody Map<String, Object> m) {
    s.saveConfig(id, m);
  }

  @GetMapping("/users")
  Object users() {
    s.require("SETUP");
    return users.findAll();
  }

  @PostMapping("/users")
  @org.springframework.transaction.annotation.Transactional
  Object user(@RequestBody Map<String, Object> m) {
    s.require("SETUP");
    CageUser u = new CageUser();
    u.username = s.s(m, "username", "");
    s.check(u.username.matches("[a-z][a-z0-9._-]{2,40}"), "Invalid username");
    u.displayName = s.s(m, "displayName", u.username);
    u.roles = s.s(m, "roles", "");
    s.check(
        Arrays.stream(u.roles.split(",")).allMatch(Security.PERMISSIONS::containsKey),
        "Invalid role");
    String pass = s.s(m, "password", "");
    s.check(pass.length() >= 12, "Password must have at least 12 characters");
    u.password = encoder.encode(pass);
    users.save(u);
    s.audit(
        "SETUP",
        "User",
        u.id,
        "CREATE",
        "Administrator created demo account",
        null,
        Map.of("username", u.username, "roles", u.roles));
    return u;
  }

  @PostMapping("/tasks/{id}/resolve")
  @org.springframework.transaction.annotation.Transactional
  void task(@PathVariable Long id, @RequestBody Map<String, Object> m) {
    WorkTask t = s.found(s.tasks().findById(id));
    s.require(t.scope);
    s.check(
        !t.scope.equals("CTR")
            || t.reportId == null
            || !s.found(s.reports().findById(t.reportId)).obligationOpen,
        "CTR obligation remains open; resolve the reporting disposition first");
    t.resolved = true;
    t.resolution = s.reason(m);
    s.tasks().save(t);
    s.audit(t.scope, "Task", id, "RESOLVE", t.resolution, null, true);
  }
}

@RestControllerAdvice
class Errors {
  @ExceptionHandler(AccessDeniedException.class)
  ResponseEntity<?> denied(Exception e) {
    return ResponseEntity.status(403).body(Map.of("message", "Access denied"));
  }

  @ExceptionHandler(ResponseStatusException.class)
  ResponseEntity<?> response(ResponseStatusException e) {
    return ResponseEntity.status(e.getStatusCode())
        .body(Map.of("message", Objects.toString(e.getReason(), "Request failed")));
  }

  @ExceptionHandler({IllegalArgumentException.class, IllegalStateException.class})
  ResponseEntity<?> invalid(Exception e) {
    return ResponseEntity.badRequest()
        .body(Map.of("message", Objects.toString(e.getMessage(), "Invalid request")));
  }

  @ExceptionHandler({
    DataIntegrityViolationException.class,
    OptimisticLockingFailureException.class
  })
  ResponseEntity<?> conflict(Exception e) {
    return ResponseEntity.status(409)
        .body(
            Map.of(
                "message", "Duplicate data or concurrent change. Refresh and review the record."));
  }

  @ExceptionHandler(Exception.class)
  ResponseEntity<?> unexpected(Exception e) {
    org.slf4j.LoggerFactory.getLogger(Errors.class)
        .error("Request failed: {}", e.getClass().getSimpleName());
    return ResponseEntity.status(500)
        .body(Map.of("message", "Request could not be completed. Review server logs."));
  }
}
