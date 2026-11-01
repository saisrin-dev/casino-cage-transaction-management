package dev.saisrin.cage;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.*;
import java.time.*;
import java.util.*;
import java.util.stream.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class CageService {
  final Patrons patrons;
  final Types types;
  final Transactions txs;
  final Reports reports;
  final Revisions revisions;
  final Properties properties;
  final Audits audits;
  final Tasks tasks;
  final Batches batches;
  final Items items;
  final Configurations configs;
  final ObjectMapper json;
  final OutboxRepository outbox;
  final Subjects subjects;

  CageService(
      Patrons p,
      Types t,
      Transactions x,
      Reports r,
      Revisions v,
      Properties pr,
      Audits a,
      Tasks w,
      Batches b,
      Items i,
      Configurations c,
      ObjectMapper j,
      OutboxRepository o,
      Subjects sub) {
    patrons = p;
    types = t;
    txs = x;
    reports = r;
    revisions = v;
    properties = pr;
    audits = a;
    tasks = w;
    batches = b;
    items = i;
    configs = c;
    json = j;
    outbox = o;
    subjects = sub;
  }

  public Patrons patrons() {
    return patrons;
  }

  public Types types() {
    return types;
  }

  public Transactions txs() {
    return txs;
  }

  public Reports reports() {
    return reports;
  }

  public Revisions revisions() {
    return revisions;
  }

  public Properties properties() {
    return properties;
  }

  public Audits audits() {
    return audits;
  }

  public Tasks tasks() {
    return tasks;
  }

  public Batches batches() {
    return batches;
  }

  public Items items() {
    return items;
  }

  public Configurations configs() {
    return configs;
  }

  public OutboxRepository outbox() {
    return outbox;
  }

  String actor() {
    var a = SecurityContextHolder.getContext().getAuthentication();
    return a == null ? "system" : a.getName();
  }

  boolean can(String p) {
    var a = SecurityContextHolder.getContext().getAuthentication();
    return a != null && a.getAuthorities().stream().anyMatch(g -> g.getAuthority().equals(p));
  }

  void require(String p) {
    if (!can(p)) throw new AccessDeniedException("Permission required");
  }

  void read(String p) {
    if (!can(p) && !can(p + "_READ")) throw new AccessDeniedException("Permission required");
  }

  void check(boolean good, String message) {
    if (!good) throw new IllegalArgumentException(message);
  }

  <T> T found(Optional<T> r) {
    return r.orElseThrow(
        () -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found"));
  }

  String s(Map<String, Object> m, String k, String d) {
    Object v = m.get(k);
    return v == null ? d : v.toString().trim();
  }

  Long id(Map<String, Object> m, String k) {
    return Long.valueOf(s(m, k, "0"));
  }

  BigDecimal money(Map<String, Object> m, String key) {
    try {
      BigDecimal n = new BigDecimal(s(m, key, "0"));
      check(
          n.scale() <= 2 && n.signum() >= 0 && n.compareTo(new BigDecimal("999999999.99")) <= 0,
          "Invalid " + key + ": use positive USD with at most two decimal places");
      return n.setScale(2);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Invalid " + key);
    }
  }

  Map<String, Object> snapshot(Report r) {
    return Map.of(
        "report",
        r,
        "patron",
        patronView(r.patron),
        "additionalSubjects",
        subjects.findByReportId(r.id).stream()
            .map(
                link ->
                    Map.of("patron", patronView(link.patron), "relationship", link.relationship))
            .toList(),
        "simulation",
        true,
        "schema",
        "CAGE-MOCK-1");
  }

  String serialize(Object o) {
    try {
      return json.writeValueAsString(o);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  String reason(Map<String, Object> m) {
    String r = s(m, "reason", "");
    check(r.length() >= 5 && r.length() <= 2000, "Provide a reason (5–2000 characters)");
    return r;
  }

  void stale(Row row, Map<String, Object> m) {
    check(m.containsKey("version"), "Version is required");
    if (row.version != Long.parseLong(s(m, "version", "-1")))
      throw new ResponseStatusException(
          HttpStatus.CONFLICT, "This record changed. Refresh and try again.");
  }

  void audit(
      String scope,
      String type,
      Long id,
      String action,
      String reason,
      Object before,
      Object after) {
    AuditEvent a = new AuditEvent();
    a.scope = scope;
    a.entityType = type;
    a.entityId = id;
    a.action = action;
    a.actor = actor();
    a.reason = reason;
    a.beforeValue = serialize(before);
    a.afterValue = serialize(after);
    a.correlationId = UUID.randomUUID().toString();
    audits.save(a);
  }

  public Map<String, Object> patronView(Patron p) {
    Map<String, Object> v = json.convertValue(p, Map.class);
    v.put(
        "tinMasked",
        p.tin == null
            ? "Unavailable"
            : "•••-••-" + p.tin.substring(Math.max(0, p.tin.length() - 4)));
    v.put(
        "identityMasked",
        p.identityNumber == null
            ? "Unavailable"
            : "••••" + p.identityNumber.substring(Math.max(0, p.identityNumber.length() - 4)));
    return v;
  }

  public List<Map<String, Object>> patronList() {
    read("PATRON");
    return patrons.findAll().stream().map(this::patronView).toList();
  }

  public Patron savePatron(Long id, Map<String, Object> m) {
    require("PATRON");
    Patron p = id == null ? new Patron() : found(patrons.findById(id));
    Object before = id == null ? null : patronView(p);
    if (id != null) stale(p, m);
    check(p.mergedInto == null, "Edit the surviving profile");
    p.legalName = s(m, "legalName", p.legalName);
    check(
        p.legalName != null && p.legalName.length() > 2 && p.legalName.length() < 150,
        "Legal name is required");
    if (id == null
        && patrons.findAll().stream()
            .anyMatch(
                existing ->
                    existing.mergedInto == null
                        && existing.legalName.equalsIgnoreCase(p.legalName))) {
      check(
          can("SUPERVISE") && Boolean.parseBoolean(s(m, "duplicateReviewed", "false")),
          "Potential duplicate legal name. Review existing patrons; a supervisor may approve a"
              + " separate profile.");
    }
    p.aliasName = s(m, "aliasName", p.aliasName);
    p.address = s(m, "address", p.address);
    p.contact = s(m, "contact", p.contact);
    p.occupation = s(m, "occupation", p.occupation);
    p.country = s(m, "country", p.country);
    p.loyaltyId = s(m, "loyaltyId", p.loyaltyId);
    if ("".equals(p.loyaltyId)) p.loyaltyId = null;
    if (m.containsKey("dateOfBirth") && !s(m, "dateOfBirth", "").isBlank()) {
      p.dateOfBirth = LocalDate.parse(s(m, "dateOfBirth", ""));
      check(p.dateOfBirth.isBefore(LocalDate.now()), "Date of birth must be in the past");
    }
    p.identityStatus = s(m, "identityStatus", p.identityStatus);
    check(
        Set.of("VERIFIED", "UNVERIFIED", "EXPIRED", "UNAVAILABLE", "REFUSED")
            .contains(p.identityStatus),
        "Invalid identity status");
    p.identityType = s(m, "identityType", p.identityType);
    p.issuingAuthority = s(m, "issuingAuthority", p.issuingAuthority);
    p.residency = s(m, "residency", p.residency);
    check(Set.of("RESIDENT", "NONRESIDENT", "UNKNOWN").contains(p.residency), "Invalid residency");
    if (!s(m, "identityExpiry", "").isBlank())
      p.identityExpiry = LocalDate.parse(s(m, "identityExpiry", ""));
    if (!s(m, "tin", "").isBlank()) {
      check(s(m, "tin", "").startsWith("DEMO-"), "Use synthetic identifiers starting DEMO-");
      p.tin = s(m, "tin", "");
    }
    if (!s(m, "identityNumber", "").isBlank()) {
      check(
          s(m, "identityNumber", "").startsWith("DEMO-"),
          "Use synthetic identifiers starting DEMO-");
      p.identityNumber = s(m, "identityNumber", "");
    }
    p.temporary = Boolean.parseBoolean(s(m, "temporary", String.valueOf(p.temporary)));
    if (p.temporary) require("SUPERVISE");
    patrons.saveAndFlush(p);
    audit(
        "PATRON", "Patron", p.id, id == null ? "CREATE" : "EDIT", reason(m), before, patronView(p));
    return p;
  }

  public void mergePatron(Long id, Map<String, Object> m) {
    require("SUPERVISE");
    properties.lock();
    Patron old = found(patrons.findById(id)), target = found(patrons.findById(id(m, "targetId")));
    stale(old, m);
    check(
        !old.id.equals(target.id) && old.mergedInto == null && target.mergedInto == null,
        "Choose two separate unmerged profiles");
    Set<LocalDate> days = new HashSet<>();
    for (CageTransaction t : txs.findAll())
      if (t.patron.id.equals(old.id)) {
        days.add(t.gamingDay);
        t.patron = target;
        txs.save(t);
      }
    old.mergedInto = target;
    patrons.save(old);
    txs.flush();
    for (LocalDate day : days) {
      recalculate(old, day);
      recalculate(target, day);
    }
    audit(
        "PATRON",
        "Patron",
        id,
        "MERGE",
        reason(m),
        Map.of("oldId", id),
        Map.of("survivorId", target.id));
  }

  public CageTransaction createTransaction(Map<String, Object> m) {
    require("TRANSACTION");
    Property prop = properties.lock();
    String source = s(m, "sourceId", "");
    check(!source.isBlank() && source.length() < 150, "Source event ID is required");
    var existing = txs.findBySourceId(source);
    if (existing.isPresent()) {
      CageTransaction t = existing.get();
      check(
          t.patron.id.equals(id(m, "patronId"))
              && t.type.id.equals(id(m, "typeId"))
              && t.gross.compareTo(money(m, "gross")) == 0
              && t.cash.compareTo(money(m, "cash")) == 0
              && t.occurredAt.equals(Instant.parse(s(m, "occurredAt", ""))),
          "Source ID already belongs to a different request");
      return t;
    }
    Patron p = found(patrons.findById(id(m, "patronId")));
    check(p.mergedInto == null, "Choose the surviving patron");
    if (p.temporary) require("SUPERVISE");
    TransactionType type = found(types.findById(id(m, "typeId")));
    Instant at = Instant.parse(s(m, "occurredAt", ""));
    LocalDate local = at.atZone(ZoneId.of(prop.zone)).toLocalDate();
    check(!at.isAfter(Instant.now().plusSeconds(300)), "Transaction time cannot be in the future");
    check(
        type.active
            && "APPROVED".equals(type.state)
            && !local.isBefore(type.effectiveFrom)
            && (type.effectiveTo == null || !local.isAfter(type.effectiveTo)),
        "Type is inactive or outside its effective dates");
    CageTransaction t = new CageTransaction();
    t.patron = p;
    t.sourceId = source;
    t.type = type;
    t.typeLabel = type.label;
    t.typeVersion = type.version;
    t.treatment = type.treatment;
    t.occurredAt = at;
    t.gamingDay = Rules.gamingDay(at, prop.zone, prop.cutoff);
    t.propertyZone = prop.zone;
    t.gamingCutoff = prop.cutoff;
    t.ruleVersion = prop.version;
    t.station = s(m, "station", "Cage A");
    t.cashier = actor();
    t.reference = s(m, "reference", "");
    check(
        !t.reference.isBlank(), "Account, table, ticket, machine or payment reference is required");
    t.notes = s(m, "notes", "");
    t.gross = money(m, "gross");
    t.cash = money(m, "cash");
    check(
        t.gross.signum() > 0 && t.cash.compareTo(t.gross) <= 0,
        "Gross must be positive; cash cannot exceed gross");
    check(
        "USD".equals(s(m, "currency", "USD")),
        "This demo accepts USD only; foreign currency requires specialist processing");
    if (m.containsKey("conductorId")
        && !s(m, "conductorId", "").isBlank()
        && id(m, "conductorId") > 0) {
      t.conductor = found(patrons.findById(id(m, "conductorId")));
      t.relationship = s(m, "relationship", "");
      check(!t.relationship.isBlank(), "Conductor relationship required");
    }
    if ("NONCASH".equals(type.treatment))
      check(t.cash.signum() == 0, "Noncash types cannot contain a cash component");
    if ("IN".equals(type.treatment) || "BOTH".equals(type.treatment)) t.cashIn = t.cash;
    if ("OUT".equals(type.treatment) || "BOTH".equals(type.treatment)) t.cashOut = t.cash;
    t.status = s(m, "status", "POSTED");
    check(Set.of("DRAFT", "POSTED").contains(t.status), "Choose draft or posted");
    txs.saveAndFlush(t);
    audit(
        "TRANSACTION",
        "Transaction",
        t.id,
        "CREATE",
        s(m, "reason", "New cage transaction"),
        null,
        t);
    if (t.status.equals("POSTED")) {
      recalculate(p, t.gamingDay);
      createTax(t, type);
      publish(t, "POSTED");
      long count =
          txs.findByPatronIdAndGamingDayAndStatus(p.id, t.gamingDay, "POSTED").stream()
              .filter(x -> x.cashIn.signum() > 0 && x.cashIn.compareTo(new BigDecimal("10000")) < 0)
              .count();
      if (count >= 3)
        task(
            "SAR",
            "Synthetic referral: review repeated cash-in activity for patron #"
                + p.id
                + " / "
                + t.gamingDay,
            null);
    }
    return t;
  }

  void publish(CageTransaction t, String action) {
    OutboxEvent o = new OutboxEvent();
    o.aggregateId = t.id;
    o.payload =
        serialize(
            Map.of(
                "eventId",
                UUID.randomUUID().toString(),
                "transactionId",
                t.id,
                "patronId",
                t.patron.id,
                "action",
                action,
                "gamingDay",
                t.gamingDay.toString(),
                "cashIn",
                t.cashIn,
                "cashOut",
                t.cashOut,
                "at",
                Instant.now().toString()));
    outbox.save(o);
  }

  public CageTransaction transactionAction(Long id, String action, Map<String, Object> m) {
    require("TRANSACTION");
    properties.lock();
    CageTransaction t = found(txs.findById(id));
    stale(t, m);
    String why = reason(m);
    if (action.equals("post")) {
      check(t.status.equals("DRAFT"), "Only drafts can be posted");
      check(t.type.active, "Type no longer active");
      t.status = "POSTED";
      txs.saveAndFlush(t);
      recalculate(t.patron, t.gamingDay);
      createTax(t, t.type);
    } else if (action.equals("void") || action.equals("delete")) {
      if (t.status.equals("POSTED")) require("SUPERVISE");
      check(Set.of("DRAFT", "POSTED").contains(t.status), "Already reversed");
      String before = t.status;
      t.status = "VOIDED";
      txs.saveAndFlush(t);
      recalculate(t.patron, t.gamingDay);
      taxImpact(t);
      audit("TRANSACTION", "Transaction", id, "REVERSE", why, before, t.status);
    } else if (action.equals("correct")) {
      if (t.status.equals("POSTED")) require("SUPERVISE");
      check(
          Set.of("POSTED", "DRAFT").contains(t.status),
          "Only active transactions can be corrected");
      String previous = t.status;
      t.status = "CORRECTED";
      txs.saveAndFlush(t);
      Map<String, Object> replacement = new HashMap<>(m);
      replacement.put("status", previous);
      CageTransaction next = createTransaction(replacement);
      check(!next.id.equals(id), "Correction needs a new source ID");
      next.corrects = t;
      txs.saveAndFlush(next);
      recalculate(t.patron, t.gamingDay);
      taxImpact(t);
      publish(t, "CORRECT");
      audit("TRANSACTION", "Transaction", id, "CORRECT", why, t, next);
      return next;
    } else throw new IllegalArgumentException("Unknown action");
    publish(t, action.toUpperCase());
    audit("TRANSACTION", "Transaction", id, action.toUpperCase(), why, null, t);
    return t;
  }

  void taxImpact(CageTransaction t) {
    reports
        .findByKindAndGroupKey("TAX", "TX-" + t.id)
        .ifPresent(
            r -> {
              task("TAX", "Reconcile tax event after source reversal", r.id);
              if (r.snapshot == null) {
                r.status = "RETURNED";
                r.approvedRevision = null;
                r.approver = null;
                reports.save(r);
              }
            });
  }

  void task(String scope, String title, Long report) {
    if (tasks.findAll().stream()
        .noneMatch(
            t ->
                !t.resolved
                    && t.scope.equals(scope)
                    && Objects.equals(t.reportId, report)
                    && t.title.equals(title))) {
      WorkTask t = new WorkTask();
      t.scope = scope;
      t.title = title;
      t.reportId = report;
      tasks.save(t);
    }
  }

  void recalculate(Patron p, LocalDate day) {
    List<CageTransaction> active = txs.findByPatronIdAndGamingDayAndStatus(p.id, day, "POSTED");
    BigDecimal in = active.stream().map(t -> t.cashIn).reduce(BigDecimal.ZERO, BigDecimal::add),
        out = active.stream().map(t -> t.cashOut).reduce(BigDecimal.ZERO, BigDecimal::add);
    String key = p.id + ":" + day;
    var old = reports.findByKindAndGroupKey("CTR", key);
    if (old.isEmpty() && !Rules.ctr(in, out)) return;
    Report r = old.orElseGet(Report::new);
    if (r.id != null && r.cashIn.compareTo(in) == 0 && r.cashOut.compareTo(out) == 0) return;
    if (r.id != null && r.snapshot != null) {
      task("CTR", "Filed activity changed: assess corrected CTR", r.id);
      return;
    }
    Object before = r.id == null ? null : serialize(r);
    r.kind = "CTR";
    r.groupKey = key;
    r.patron = p;
    r.gamingDay = day;
    r.cashIn = in;
    r.cashOut = out;
    r.amount = in.max(out);
    r.dueDate = day.plusDays(15);
    r.obligationOpen = Rules.ctr(in, out);
    if (r.creator == null) r.creator = actor();
    else {
      r.revision++;
      r.approver = null;
      r.approvedRevision = null;
    }
    r.status = r.obligationOpen ? "DRAFT" : "RETURNED";
    r.updatedAt = Instant.now();
    reports.saveAndFlush(r);
    revise(r, "Aggregation recalculated");
    audit("CTR", "Report", r.id, "RECALCULATE", "Source activity changed", before, r);
  }

  void createTax(CageTransaction t, TransactionType type) {
    if (type.taxGame == null) return;
    var result =
        Rules.tax(
            type.taxGame,
            t.gross,
            BigDecimal.ZERO,
            t.occurredAt.atZone(ZoneId.of(t.propertyZone)).getYear(),
            t.patron.residency);
    if (!result.eligible()) {
      task("TAX", result.referral() + " (transaction " + t.id + ")", null);
      return;
    }
    Report r = new Report();
    r.kind = "TAX";
    r.groupKey = "TX-" + t.id;
    r.patron = t.patron;
    r.gamingDay = t.gamingDay;
    r.amount = result.reportable();
    r.withholding = result.withholding();
    r.game = type.taxGame;
    r.formType = type.taxGame.equals("PROMOTION") ? "1099-MISC" : "W-2G";
    r.taxRuleVersion = result.rule();
    r.creator = actor();
    r.dueDate = LocalDate.of(2027, 1, 31);
    r.fields =
        serialize(
            Map.of(
                "transactionId",
                t.id,
                "gross",
                t.gross,
                "wager",
                0,
                "label",
                "DEMO RULES — not an IRS form"));
    reports.saveAndFlush(r);
    revise(r, "Demo tax event created");
  }

  void revise(Report r, String why) {
    ReportRevision v = new ReportRevision();
    v.report = r;
    v.revision = r.revision;
    v.snapshot = serialize(snapshot(r));
    v.actor = actor();
    v.reason = why;
    revisions.save(v);
  }

  public Report createReport(String kind, Map<String, Object> m) {
    check(Set.of("SAR", "TAX").contains(kind), "CTR candidates are generated from posted activity");
    require(kind);
    Report r = new Report();
    r.kind = kind;
    r.groupKey = UUID.randomUUID().toString();
    r.patron = found(patrons.findById(id(m, "patronId")));
    r.creator = actor();
    r.gamingDay = LocalDate.now();
    r.detectionDate = LocalDate.parse(s(m, "detectionDate", LocalDate.now().toString()));
    check(!r.detectionDate.isAfter(LocalDate.now()), "Detection cannot be in the future");
    r.dueDate = r.detectionDate.plusDays(30);
    reports.saveAndFlush(r);
    return editReport(r.id, m, false);
  }

  public Report editReport(Long id, Map<String, Object> m, boolean versionCheck) {
    Report r = report(id, true);
    if (versionCheck) stale(r, m);
    check(
        r.snapshot == null && !Set.of("REJECTED_INTERNALLY", "SUPERSEDED").contains(r.status),
        "Create a correction version for filed or closed records");
    Object before = serialize(r);
    String why = reason(m);
    if (r.kind.equals("SAR")) {
      if (m.containsKey("detectionDate")) {
        LocalDate detection = LocalDate.parse(s(m, "detectionDate", ""));
        check(!detection.isAfter(LocalDate.now()), "Detection cannot be in the future");
        r.detectionDate = detection;
        r.dueDate = detection.plusDays(30);
      }
      r.narrative = s(m, "narrative", r.narrative);
      r.category = s(m, "category", r.category);
      r.amount = money(m, "amount");
      r.activityStart = LocalDate.parse(s(m, "activityStart", r.detectionDate.toString()));
      r.activityEnd = LocalDate.parse(s(m, "activityEnd", r.detectionDate.toString()));
      check(!r.activityEnd.isBefore(r.activityStart), "Activity end precedes start");
    } else if (r.kind.equals("TAX")) {
      r.formType = s(m, "formType", r.formType);
      check(!"1098".equals(r.formType), "1098 is a mortgage form and is not eligible for jackpots");
      r.game = s(m, "game", r.game == null ? "SLOT" : r.game);
      var result =
          Rules.tax(r.game, money(m, "gross"), money(m, "wager"), 2026, r.patron.residency);
      check(result.eligible(), result.referral());
      check(
          r.formType.equals(r.game.equals("PROMOTION") ? "1099-MISC" : "W-2G"),
          "Form type is not eligible for this tax event");
      if (r.groupKey.startsWith("TX-")) {
        CageTransaction source = found(txs.findById(Long.valueOf(r.groupKey.substring(3))));
        check(
            source.status.equals("POSTED") && source.gross.compareTo(money(m, "gross")) == 0,
            "Correct the source transaction first; winnings must reconcile");
      }
      r.amount = result.reportable();
      r.withholding = result.withholding();
      r.taxRuleVersion = result.rule();
      r.fields =
          serialize(
              Map.of(
                  "gross", money(m, "gross"), "wager", money(m, "wager"), "label", "DEMO RULES"));
    } else {
      r.fields = serialize(Map.of("reviewNotes", s(m, "reviewNotes", "")));
    }
    r.revision++;
    r.approvedRevision = null;
    r.approver = null;
    r.status = "DRAFT";
    r.updatedAt = Instant.now();
    reports.saveAndFlush(r);
    revise(r, why);
    audit(r.kind, "Report", id, "EDIT", why, before, r);
    return r;
  }

  public Report report(Long id, boolean write) {
    Report r = found(reports.findById(id));
    if (write) {
      if (!can(r.kind)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found");
    } else if (!can(r.kind) && !can(r.kind + "_READ"))
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Record not found");
    return r;
  }

  List<String> validation(Report r) {
    List<String> errors = new ArrayList<>();
    if (r.patron.temporary || !r.patron.identityStatus.equals("VERIFIED"))
      errors.add("Patron identity needs verification");
    if (r.patron.identityExpiry != null && r.patron.identityExpiry.isBefore(LocalDate.now()))
      errors.add("Patron identity has expired");
    if (r.patron.address == null || r.patron.dateOfBirth == null)
      errors.add("Patron address and date of birth are required");
    if (r.kind.equals("CTR") && !r.obligationOpen)
      errors.add("No active CTR threshold obligation; assess disposition");
    if (r.kind.equals("SAR")
        && (r.narrative == null || r.narrative.length() < 40 || r.category == null))
      errors.add("Enter a meaningful synthetic narrative (40+ characters) and activity category");
    return errors;
  }

  public Report reportAction(Long id, String action, Map<String, Object> m) {
    Report r = report(id, true);
    stale(r, m);
    String why = reason(m);
    String old = r.status;
    switch (action) {
      case "review" -> {
        check(
            Set.of("DRAFT", "RETURNED").contains(r.status),
            "Only drafts or returned reports may enter review");
        check(validation(r).isEmpty(), String.join("; ", validation(r)));
        r.status = "PENDING_REVIEW";
      }
      case "approve" -> {
        require("APPROVE");
        check(r.status.equals("PENDING_REVIEW"), "Send for review first");
        check(!actor().equals(r.creator), "Creators cannot approve their own report");
        check(validation(r).isEmpty(), String.join("; ", validation(r)));
        r.status = "APPROVED";
        r.approver = actor();
        r.approvedRevision = r.revision;
      }
      case "return" -> {
        require("APPROVE");
        check(
            Set.of("PENDING_REVIEW", "APPROVED").contains(r.status) && r.snapshot == null,
            "Only an unfiled review can be returned");
        r.status = "RETURNED";
        r.approver = null;
        r.approvedRevision = null;
      }
      case "reject", "close" -> {
        require("APPROVE");
        check(r.snapshot == null, "Filed records cannot be internally rejected");
        check(
            !action.equals("close") || r.kind.equals("SAR"), "Closure without filing is SAR only");
        r.status = action.equals("close") ? "CLOSED_WITHOUT_FILING" : "REJECTED_INTERNALLY";
        r.approvedRevision = null;
        if (r.kind.equals("CTR") && r.obligationOpen)
          task("CTR", "Internally rejected CTR: resolve outstanding obligation", id);
      }
      case "correction" -> {
        check(r.snapshot != null, "Corrections require a filed original");
        Report n = new Report();
        n.kind = r.kind;
        n.groupKey = UUID.randomUUID().toString();
        n.patron = r.patron;
        n.gamingDay = r.gamingDay;
        n.cashIn = r.cashIn;
        n.cashOut = r.cashOut;
        n.amount = r.amount;
        n.creator = actor();
        n.corrects = r;
        n.filingType = "CORRECTION";
        n.narrative = r.narrative;
        n.category = r.category;
        n.detectionDate = r.detectionDate;
        n.activityStart = r.activityStart;
        n.activityEnd = r.activityEnd;
        n.dueDate = r.dueDate;
        n.formType = r.formType;
        n.game = r.game;
        n.fields = r.fields;
        n.withholding = r.withholding;
        n.taxRuleVersion = r.taxRuleVersion;
        if (n.kind.equals("CTR")) {
          var current = txs.findByPatronIdAndGamingDayAndStatus(n.patron.id, n.gamingDay, "POSTED");
          n.cashIn = current.stream().map(t -> t.cashIn).reduce(BigDecimal.ZERO, BigDecimal::add);
          n.cashOut = current.stream().map(t -> t.cashOut).reduce(BigDecimal.ZERO, BigDecimal::add);
          n.amount = n.cashIn.max(n.cashOut);
        }
        reports.saveAndFlush(n);
        revise(n, why);
        audit(r.kind, "Report", n.id, "CORRECTION", why, r.id, n.id);
        return n;
      }
      default -> throw new IllegalArgumentException("Unknown report action");
    }
    r.updatedAt = Instant.now();
    reports.save(r);
    audit(r.kind, "Report", id, action.toUpperCase(), why, old, r.status);
    return r;
  }

  public FilingBatch makeBatch(Map<String, Object> m) {
    require("FILE");
    properties.lock();
    String kind = s(m, "kind", "");
    require(kind);
    check(Set.of("CTR", "SAR", "TAX").contains(kind), "Invalid report category");
    List<?> ids = (List<?>) m.get("reportIds");
    check(ids != null && !ids.isEmpty() && ids.size() <= 100, "Select 1–100 approved reports");
    check(ids.stream().distinct().count() == ids.size(), "Duplicate report selection");
    FilingBatch b = new FilingBatch();
    b.kind = kind;
    b.creator = actor();
    batches.saveAndFlush(b);
    for (Object id : ids) {
      Report r = report(Long.valueOf(id.toString()), true);
      check(
          r.kind.equals(kind)
              && r.status.equals("APPROVED")
              && Objects.equals(r.approvedRevision, r.revision)
              && r.snapshot == null,
          "Select approved, unsubmitted versions of one report category");
      check(
          !items.existsByReportIdAndRevision(r.id, r.revision),
          "Report version already in a batch");
      BatchItem item = new BatchItem();
      item.batch = b;
      item.report = r;
      item.revision = r.revision;
      item.snapshot = serialize(snapshot(r));
      items.save(item);
    }
    audit(kind, "Batch", b.id, "PREPARE", "Approved report selection", null, ids);
    return b;
  }

  public FilingBatch batch(Long id) {
    FilingBatch b = found(batches.findById(id));
    read(b.kind);
    return b;
  }

  void batchValid(FilingBatch b) {
    for (BatchItem i : items.findByBatchId(b.id)) {
      check(
          i.report.status.equals("APPROVED")
              && Objects.equals(i.report.approvedRevision, i.revision)
              && i.report.revision == i.revision,
          "Report changed or approval invalidated after batching");
    }
  }

  public String exportBatch(Long id) {
    require("FILE");
    FilingBatch b = batch(id);
    check(Set.of("PREPARED", "EXPORTED").contains(b.status), "Batch already transmitted");
    batchValid(b);
    String body =
        serialize(
            Map.of(
                "simulation",
                true,
                "schema",
                "CAGE-MOCK-1",
                "warning",
                "NOT A FINCEN OR IRS SUBMISSION FILE",
                "batchId",
                "MOCK-BATCH-" + id,
                "items",
                items.findByBatchId(id)));
    try {
      b.checksum =
          HexFormat.of()
              .formatHex(
                  java.security.MessageDigest.getInstance("SHA-256")
                      .digest(body.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    b.status = "EXPORTED";
    batches.save(b);
    for (BatchItem i : items.findByBatchId(id)) {
      i.report.filingStatus = "EXPORTED";
      reports.save(i.report);
    }
    audit(b.kind, "Batch", id, "EXPORT", "Simulated file export", null, b.checksum);
    return body;
  }

  public void saveConfig(Long id, Map<String, Object> m) {
    require("SETUP");
    Configuration c = found(configs.findById(id));
    stale(c, m);
    String old = c.payload;
    String payload = s(m, "payload", "");
    try {
      json.readTree(payload);
    } catch (Exception e) {
      throw new IllegalArgumentException("Configuration must be valid JSON");
    }
    c.payload = payload;
    c.reason = reason(m);
    c.changedBy = actor();
    configs.save(c);
    audit("SETUP", "Configuration", id, "PROPOSE", c.reason, old, c.payload);
    task(
        "SETUP",
        "Review proposed configuration " + c.code + "; not applied to statutory mappings",
        null);
  }

  public TransactionType saveType(Long id, Map<String, Object> m) {
    require("SETUP");
    TransactionType t = id == null ? new TransactionType() : found(types.findById(id));
    if (id != null) {
      stale(t, m);
      check(!t.active, "Clone an active historical definition to change its mapping");
    }
    t.code = s(m, "code", t.code);
    t.label = s(m, "label", t.label);
    t.treatment = s(m, "treatment", t.treatment);
    check(
        t.code != null
            && t.code.matches("[A-Z0-9_]{2,40}")
            && t.label != null
            && !t.label.isBlank(),
        "Unique code and label required");
    check(
        Set.of("IN", "OUT", "BOTH", "EXCLUDED", "NONCASH").contains(t.treatment),
        "Invalid currency treatment");
    t.createdBy = actor();
    t.state = "PROPOSED";
    t.effectiveFrom = LocalDate.parse(s(m, "effectiveFrom", LocalDate.now().toString()));
    types.saveAndFlush(t);
    audit("SETUP", "Type", t.id, "PROPOSE", reason(m), null, t);
    return t;
  }

  public void typeAction(Long id, String action, Map<String, Object> m) {
    properties.lock();
    TransactionType t = found(types.findById(id));
    stale(t, m);
    if (action.equals("activate")) {
      require("APPROVE");
      check(!actor().equals(t.createdBy), "Type maker cannot approve their own definition");
      t.active = true;
      t.state = "APPROVED";
      t.approvedBy = actor();
    } else {
      require("SETUP");
      check(action.equals("deactivate"), "Unknown type action");
      t.active = false;
    }
    types.save(t);
    audit("SETUP", "Type", id, action.toUpperCase(), reason(m), null, t);
  }
}
