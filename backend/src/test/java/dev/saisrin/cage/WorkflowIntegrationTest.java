package dev.saisrin.cage;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.math.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest(
    properties = {
      "cage.seed=true",
      "cage.demo-password=Integration-Test-2026",
      "cage.simulation-controls=true"
    })
@AutoConfigureMockMvc
@Transactional
@WithMockUser(
    username = "maker",
    authorities = {
      "PATRON",
      "TRANSACTION",
      "SUPERVISE",
      "CTR",
      "SAR",
      "TAX",
      "FILE",
      "SETUP",
      "APPROVE"
    })
class WorkflowIntegrationTest {
  @Autowired CageService s;
  @Autowired FilingService filing;
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  Patron p;
  TransactionType in, out;
  Instant at = Instant.now().minusSeconds(3600);

  @BeforeEach
  void patron() {
    p = new Patron();
    p.legalName = "Synthetic Integration " + UUID.randomUUID();
    p.dateOfBirth = LocalDate.of(1985, 5, 4);
    p.address = "Fictional integration address";
    p.identityStatus = "VERIFIED";
    p.identityExpiry = LocalDate.of(2030, 1, 1);
    p.tin = "DEMO-1234";
    s.patrons().saveAndFlush(p);
    in =
        s.types().findAll().stream()
            .filter(t -> t.code.equals("CHIP_BUY"))
            .findFirst()
            .orElseThrow();
    out =
        s.types().findAll().stream()
            .filter(t -> t.code.equals("CHIP_REDEEM"))
            .findFirst()
            .orElseThrow();
  }

  Map<String, Object> tx(TransactionType type, String amount) {
    return new HashMap<>(
        Map.of(
            "sourceId",
            UUID.randomUUID().toString(),
            "patronId",
            p.id,
            "typeId",
            type.id,
            "gross",
            amount,
            "cash",
            amount,
            "occurredAt",
            at.toString(),
            "reference",
            "DEMO-TEST"));
  }

  Report candidate() {
    return s.reports()
        .findByKindAndGroupKey("CTR", p.id + ":" + Rules.gamingDay(at, "America/New_York", "06:00"))
        .orElseThrow();
  }

  Map<String, Object> update(Row r) {
    return new HashMap<>(Map.of("version", r.version, "reason", "Integration test review reason"));
  }

  void as(String name, String... permissions) {
    SecurityContextHolder.getContext()
        .setAuthentication(
            new UsernamePasswordAuthenticationToken(
                name, "", Arrays.stream(permissions).map(SimpleGrantedAuthority::new).toList()));
  }

  Report approved() {
    s.createTransaction(tx(in, "10001"));
    Report r = candidate();
    s.reportAction(r.id, "review", update(r));
    as("independent-approver", "CTR", "APPROVE", "FILE");
    s.reportAction(r.id, "approve", update(r));
    return r;
  }

  @Test
  void multipleCashInTriggersOneCandidate() {
    s.createTransaction(tx(in, "6000"));
    s.createTransaction(tx(in, "4500"));
    Report r = candidate();
    assertEquals(0, r.cashIn.compareTo(new BigDecimal("10500")));
    long id = r.id;
    s.createTransaction(tx(in, "100"));
    assertEquals(id, candidate().id);
    assertEquals(0, candidate().cashIn.compareTo(new BigDecimal("10600")));
  }

  @Test
  void exactThresholdDoesNotTrigger() {
    s.createTransaction(tx(in, "10000"));
    assertFalse(s.reports().findAll().stream().anyMatch(r -> r.patron.id.equals(p.id)));
  }

  @Test
  void directionsDoNotCombine() {
    s.createTransaction(tx(in, "7000"));
    s.createTransaction(tx(out, "6000"));
    assertFalse(s.reports().findAll().stream().anyMatch(r -> r.patron.id.equals(p.id)));
  }

  @Test
  void bothDirectionsRetained() {
    s.createTransaction(tx(in, "12000"));
    s.createTransaction(tx(out, "11000"));
    assertEquals(0, candidate().cashIn.compareTo(new BigDecimal("12000")));
    assertEquals(0, candidate().cashOut.compareTo(new BigDecimal("11000")));
  }

  @Test
  void idempotencyAndPayloadCollision() {
    Map<String, Object> m = tx(in, "10001");
    CageTransaction first = s.createTransaction(m);
    assertEquals(first.id, s.createTransaction(m).id);
    m.put("gross", "15000");
    assertThrows(IllegalArgumentException.class, () -> s.createTransaction(m));
  }

  @Test
  void draftDoesNotAggregateUntilPosted() {
    Map<String, Object> m = tx(in, "10001");
    m.put("status", "DRAFT");
    CageTransaction t = s.createTransaction(m);
    assertFalse(s.reports().findAll().stream().anyMatch(r -> r.patron.id.equals(p.id)));
    s.transactionAction(t.id, "post", update(t));
    assertNotNull(candidate());
  }

  @Test
  void voidPreservesRecordAndRecalculates() {
    CageTransaction t = s.createTransaction(tx(in, "10001"));
    s.transactionAction(t.id, "void", update(t));
    assertEquals("VOIDED", s.txs().findById(t.id).orElseThrow().status);
    assertFalse(candidate().obligationOpen);
    assertEquals(0, candidate().cashIn.signum());
  }

  @Test
  void cashierCannotVoidPosted() {
    CageTransaction t = s.createTransaction(tx(in, "10001"));
    as("cashier", "TRANSACTION");
    assertThrows(AccessDeniedException.class, () -> s.transactionAction(t.id, "void", update(t)));
  }

  @Test
  void jackpotExcludedAndTaxCreated() {
    TransactionType jackpot =
        s.types().findAll().stream()
            .filter(t -> t.code.equals("SLOT_JACKPOT"))
            .findFirst()
            .orElseThrow();
    CageTransaction t = s.createTransaction(tx(jackpot, "12000"));
    assertEquals(0, t.cashOut.signum());
    assertFalse(
        s.reports().findAll().stream()
            .anyMatch(r -> r.kind.equals("CTR") && r.patron.id.equals(p.id)));
    assertTrue(s.reports().findByKindAndGroupKey("TAX", "TX-" + t.id).isPresent());
  }

  @Test
  void noncashCannotSneakInCurrency() {
    TransactionType noncash =
        s.types().findAll().stream()
            .filter(t -> t.code.equals("NONCASH"))
            .findFirst()
            .orElseThrow();
    assertThrows(IllegalArgumentException.class, () -> s.createTransaction(tx(noncash, "12000")));
  }

  @Test
  void creatorCannotSelfApprove() {
    s.createTransaction(tx(in, "10001"));
    Report r = candidate();
    s.reportAction(r.id, "review", update(r));
    assertThrows(IllegalArgumentException.class, () -> s.reportAction(r.id, "approve", update(r)));
  }

  @Test
  void materialEditRevokesApproval() {
    Report r = approved();
    s.editReport(
        r.id,
        new HashMap<>(
            Map.of(
                "version",
                r.version,
                "reason",
                "Revised review notes",
                "reviewNotes",
                "Source review updated")),
        true);
    assertEquals("DRAFT", r.status);
    assertNull(r.approvedRevision);
  }

  @Test
  void sourceChangeInvalidatesBatchedVersion() {
    Report r = approved();
    FilingBatch b = s.makeBatch(Map.of("kind", "CTR", "reportIds", List.of(r.id)));
    as("cashier", "TRANSACTION");
    s.createTransaction(tx(in, "10"));
    as("filer", "FILE", "CTR");
    assertThrows(IllegalArgumentException.class, () -> s.exportBatch(b.id));
  }

  @Test
  void internalRejectionLeavesTask() {
    s.createTransaction(tx(in, "10001"));
    Report r = candidate();
    s.reportAction(r.id, "reject", update(r));
    assertTrue(r.obligationOpen);
    assertTrue(
        s.tasks().findAll().stream()
            .anyMatch(t -> Objects.equals(t.reportId, r.id) && !t.resolved));
  }

  @Test
  void exportIsNotSubmissionOrAcceptance() {
    Report r = approved();
    FilingBatch b = s.makeBatch(Map.of("kind", "CTR", "reportIds", List.of(r.id)));
    String file = s.exportBatch(b.id);
    assertTrue(file.contains("NOT A FINCEN"));
    assertEquals("EXPORTED", b.status);
    assertEquals("UNCONFIRMED", r.externalOutcome);
    assertNull(r.snapshot);
  }

  @Test
  void submissionRemainsUnconfirmedUntilAck() {
    Report r = approved();
    FilingBatch b = s.makeBatch(Map.of("kind", "CTR", "reportIds", List.of(r.id)));
    Map<String, Object> m = update(b);
    m.put("scenario", "ACCEPT");
    filing.action(b.id, "submit", m);
    assertEquals("UNCONFIRMED", r.externalOutcome);
    assertNotNull(r.snapshot);
    filing.action(b.id, "reconcile", update(b));
    assertEquals("ACCEPTED", r.externalOutcome);
    assertTrue(r.externalId.startsWith("MOCK-"));
  }

  @Test
  void timeoutRequiresReconciliation() {
    Report r = approved();
    FilingBatch b = s.makeBatch(Map.of("kind", "CTR", "reportIds", List.of(r.id)));
    Map<String, Object> m = update(b);
    m.put("scenario", "TIMEOUT");
    filing.action(b.id, "submit", m);
    assertEquals("TRANSPORT_FAILED", b.status);
    assertThrows(IllegalArgumentException.class, () -> filing.action(b.id, "submit", update(b)));
    filing.action(b.id, "reconcile", update(b));
    assertEquals("ACCEPTED", r.externalOutcome);
  }

  @Test
  void submittedSnapshotSurvivesProfileChange() {
    Report r = approved();
    FilingBatch b = s.makeBatch(Map.of("kind", "CTR", "reportIds", List.of(r.id)));
    filing.action(b.id, "submit", update(b));
    String frozen = r.snapshot;
    as("maker", "PATRON");
    s.savePatron(
        p.id,
        Map.of(
            "version",
            p.version,
            "legalName",
            "Changed Synthetic Name",
            "reason",
            "Corrected legal name for test"));
    assertEquals(frozen, r.snapshot);
  }

  @Test
  void duplicateBatchMembershipBlocked() {
    Report r = approved();
    s.makeBatch(Map.of("kind", "CTR", "reportIds", List.of(r.id)));
    assertThrows(
        IllegalArgumentException.class,
        () -> s.makeBatch(Map.of("kind", "CTR", "reportIds", List.of(r.id))));
  }

  @Test
  void customTypeActivationNeedsIndependentApprover() {
    TransactionType t =
        s.saveType(
            null,
            Map.of(
                "code",
                "TEST_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                "label",
                "Synthetic custom cash",
                "treatment",
                "IN",
                "reason",
                "Test approved cash mapping"));
    assertThrows(IllegalArgumentException.class, () -> s.typeAction(t.id, "activate", update(t)));
    as("other-approver", "APPROVE");
    s.typeAction(t.id, "activate", update(t));
    assertTrue(t.active);
    as("cashier", "TRANSACTION");
    assertEquals(0, s.createTransaction(tx(t, "10001")).cashIn.compareTo(new BigDecimal("10001")));
  }

  @Test
  void deactivatedTypeNotAccepted() {
    TransactionType t = new TransactionType();
    t.code = "INACTIVE_" + UUID.randomUUID().toString().substring(0, 8);
    t.label = "Inactive";
    t.active = false;
    t.treatment = "IN";
    t.state = "APPROVED";
    s.types().saveAndFlush(t);
    assertThrows(IllegalArgumentException.class, () -> s.createTransaction(tx(t, "10001")));
  }

  @Test
  void sensitiveValueEncryptedAtRest() {
    String raw = jdbc.queryForObject("select tin from patron where id=?", String.class, p.id);
    assertNotEquals("DEMO-1234", raw);
    assertFalse(raw.contains("DEMO"));
    assertEquals("•••-••-1234", s.patronView(p).get("tinMasked"));
  }

  @Test
  void invalidTaxFormBlocked() {
    Map<String, Object> m =
        new HashMap<>(
            Map.of(
                "patronId",
                p.id,
                "formType",
                "1098",
                "game",
                "SLOT",
                "gross",
                "12000",
                "wager",
                "0",
                "reason",
                "Tax applicability test"));
    assertThrows(IllegalArgumentException.class, () -> s.createReport("TAX", m));
  }

  @Test
  void sarRestrictedAtServer() throws Exception {
    mvc.perform(
            get("/api/reports?kind=SAR")
                .with(user("cashier").authorities(new SimpleGrantedAuthority("TRANSACTION"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void adminDoesNotGetSarByDefault() throws Exception {
    mvc.perform(
            get("/api/reports?kind=SAR")
                .with(user("admin").authorities(new SimpleGrantedAuthority("SETUP"))))
        .andExpect(status().isForbidden());
  }

  @Test
  void csrfRequiredOnMutations() throws Exception {
    mvc.perform(post("/api/patrons").contentType("application/json").content("{}"))
        .andExpect(status().isForbidden());
  }

  @Test
  void realLoginWorks() throws Exception {
    mvc.perform(
            post("/api/login")
                .with(csrf())
                .param("username", "analyst")
                .param("password", "Integration-Test-2026"))
        .andExpect(status().isOk());
  }

  @Test
  @org.springframework.transaction.annotation.Transactional(
      propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
  void concurrentThresholdCrossingsAreAtomic() throws Exception {
    var pool = java.util.concurrent.Executors.newFixedThreadPool(2);
    try {
      var futures = new ArrayList<java.util.concurrent.Future<?>>();
      for (int i = 0; i < 2; i++) {
        final int index = i;
        futures.add(
            pool.submit(
                () -> {
                  as("cashier-" + index, "TRANSACTION");
                  try {
                    s.createTransaction(tx(in, "6000"));
                  } finally {
                    SecurityContextHolder.clearContext();
                  }
                }));
      }
      for (var future : futures) future.get(20, java.util.concurrent.TimeUnit.SECONDS);
      Report r = candidate();
      assertEquals(0, r.cashIn.compareTo(new BigDecimal("12000")));
      assertEquals(
          1,
          s.reports().findAll().stream()
              .filter(x -> x.kind.equals("CTR") && x.patron.id.equals(p.id))
              .count());
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void partialBatchReconcilesItemsIndependently() {
    Report one = approved();
    as("maker", "PATRON", "TRANSACTION", "CTR", "APPROVE", "FILE");
    Patron second = new Patron();
    second.legalName = "Second Synthetic Subject";
    second.dateOfBirth = LocalDate.of(1980, 1, 1);
    second.address = "Synthetic address";
    second.identityStatus = "VERIFIED";
    s.patrons().saveAndFlush(second);
    Map<String, Object> request = tx(in, "11000");
    request.put("patronId", second.id);
    s.createTransaction(request);
    Report two =
        s.reports().findAll().stream()
            .filter(r -> r.kind.equals("CTR") && r.patron.id.equals(second.id))
            .findFirst()
            .orElseThrow();
    s.reportAction(two.id, "review", update(two));
    as("other-approver", "APPROVE", "FILE", "CTR");
    s.reportAction(two.id, "approve", update(two));
    FilingBatch b = s.makeBatch(Map.of("kind", "CTR", "reportIds", List.of(one.id, two.id)));
    Map<String, Object> m = update(b);
    m.put("scenario", "PARTIAL");
    filing.action(b.id, "submit", m);
    filing.action(b.id, "reconcile", update(b));
    var results = s.items().findByBatchId(b.id);
    assertEquals(1, results.stream().filter(x -> x.outcome.equals("ACCEPTED")).count());
    assertEquals(1, results.stream().filter(x -> x.outcome.equals("REJECTED")).count());
  }

  @Test
  void correctionPreservesOriginalAndLinksReplacement() {
    CageTransaction original = s.createTransaction(tx(in, "11000"));
    Map<String, Object> m = tx(in, "12000");
    m.putAll(update(original));
    CageTransaction next = s.transactionAction(original.id, "correct", m);
    assertEquals("CORRECTED", original.status);
    assertEquals(original.id, next.corrects.id);
    assertEquals(0, candidate().cashIn.compareTo(new BigDecimal("12000")));
  }

  @Test
  void filedChangeLeavesSnapshotAndOpensImpactTask() {
    Report r = approved();
    FilingBatch b = s.makeBatch(Map.of("kind", "CTR", "reportIds", List.of(r.id)));
    filing.action(b.id, "submit", update(b));
    String frozen = r.snapshot;
    as("cashier", "TRANSACTION");
    s.createTransaction(tx(in, "100"));
    assertEquals(frozen, r.snapshot);
    assertTrue(
        s.tasks().findAll().stream()
            .anyMatch(
                t ->
                    Objects.equals(t.reportId, r.id)
                        && t.title.contains("Filed activity changed")));
  }

  @Test
  void taxEditRecalculatesAndForeignCasesStop() {
    Map<String, Object> m =
        new HashMap<>(
            Map.of(
                "patronId",
                p.id,
                "formType",
                "W-2G",
                "game",
                "POKER",
                "gross",
                "6000",
                "wager",
                "0",
                "reason",
                "Synthetic tax eligibility review"));
    Report r = s.createReport("TAX", m);
    assertEquals(0, r.withholding.compareTo(new BigDecimal("1440")));
    m.put("gross", "7000");
    m.put("version", r.version);
    s.editReport(r.id, m, true);
    assertEquals(0, r.withholding.compareTo(new BigDecimal("1680")));
    p.residency = "NONRESIDENT";
    s.patrons().saveAndFlush(p);
    assertThrows(IllegalArgumentException.class, () -> s.createReport("TAX", m));
  }

  @Test
  void w9CannotEnterReportBatch() throws Exception {
    mvc.perform(
            post("/api/patrons/" + p.id + "/w9")
                .with(csrf())
                .contentType("application/json")
                .content(
                    s.serialize(
                        Map.of(
                            "version",
                            p.version,
                            "status",
                            "RECEIVED",
                            "reason",
                            "Synthetic W9 certification received"))))
        .andExpect(status().isOk());
    assertFalse(s.reports().findAll().stream().anyMatch(r -> r.patron.id.equals(p.id)));
  }

  @Test
  void attachmentDownloadRespectsSarPermission() throws Exception {
    Map<String, Object> m =
        Map.of(
            "patronId",
            p.id,
            "amount",
            "6000",
            "category",
            "OTHER",
            "narrative",
            "Synthetic training investigation narrative with no real suspicious activity or"
                + " identity.",
            "reason",
            "Synthetic investigation evidence");
    Report r = s.createReport("SAR", m);
    org.springframework.mock.web.MockMultipartFile file =
        new org.springframework.mock.web.MockMultipartFile(
            "file", "demo.txt", "text/plain", "Synthetic evidence".getBytes());
    mvc.perform(
            multipart("/api/attachments")
                .file(file)
                .param("ownerType", "Report")
                .param("ownerId", r.id.toString())
                .param("reason", "Synthetic supporting evidence")
                .with(csrf()))
        .andExpect(status().isOk());
    Long attachment =
        jdbc.queryForObject(
            "select id from attachment where owner_type='Report' and owner_id=?", Long.class, r.id);
    mvc.perform(
            get("/api/attachments/" + attachment + "/download")
                .with(user("cashier").authorities(new SimpleGrantedAuthority("TRANSACTION"))))
        .andExpect(status().isNotFound());
  }

  @Test
  void auditTableRejectsMutation() {
    CageTransaction t = s.createTransaction(tx(in, "10001"));
    assertThrows(
        org.springframework.dao.DataAccessException.class,
        () ->
            jdbc.update(
                "update audit_event set reason='tampered' where entity_type='Transaction' and"
                    + " entity_id=?",
                t.id));
  }
}
