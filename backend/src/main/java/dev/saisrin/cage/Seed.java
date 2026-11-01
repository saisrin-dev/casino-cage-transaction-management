package dev.saisrin.cage;

import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "cage.seed", havingValue = "true")
class Seed implements CommandLineRunner {
  final CageService s;
  final Users users;
  final PasswordEncoder encoder;
  final String password;

  Seed(CageService s, Users u, PasswordEncoder e, @Value("${cage.demo-password}") String p) {
    this.s = s;
    users = u;
    encoder = e;
    password = p;
  }

  @Override
  @Transactional
  public void run(String... args) {
    if (users.count() > 0) return;
    if (password.length() < 12)
      throw new IllegalStateException("DEMO_PASSWORD must contain at least 12 characters");
    for (var pair :
        Map.of(
                "cashier",
                "CASHIER",
                "supervisor",
                "SUPERVISOR",
                "analyst",
                "ANALYST",
                "approver",
                "APPROVER,TAX",
                "filer",
                "FILER",
                "tax",
                "TAX",
                "admin",
                "ADMIN",
                "auditor",
                "AUDITOR")
            .entrySet()) {
      CageUser u = new CageUser();
      u.username = pair.getKey();
      u.displayName =
          pair.getKey().substring(0, 1).toUpperCase() + pair.getKey().substring(1) + " Demo";
      u.roles = pair.getValue();
      u.password = encoder.encode(password);
      users.save(u);
    }
    var privileges = new ArrayList<SimpleGrantedAuthority>();
    for (String p :
        List.of("PATRON", "TRANSACTION", "SUPERVISE", "CTR", "SAR", "TAX", "FILE", "SETUP"))
      privileges.add(new SimpleGrantedAuthority(p));
    SecurityContextHolder.getContext()
        .setAuthentication(new UsernamePasswordAuthenticationToken("seed", null, privileges));
    Property prop = s.properties().findById(1L).orElseThrow();
    String[][] definitions = {
      {"CHIP_BUY", "Chip or token purchase", "IN", ""},
      {"CHIP_REDEEM", "Chip redemption", "OUT", ""},
      {"FRONT_DEPOSIT", "Front money deposit", "IN", ""},
      {"FRONT_WITHDRAW", "Front money withdrawal", "OUT", ""},
      {"SAFE_DEPOSIT", "Safekeeping deposit", "IN", ""},
      {"SAFE_WITHDRAW", "Safekeeping withdrawal", "OUT", ""},
      {"MARKER_REPAY", "Marker repayment in cash", "IN", ""},
      {"MARKER_ADVANCE", "Marker advance in cash", "OUT", ""},
      {"CASH_WAGER", "Cash wager / money play", "IN", ""},
      {"TABLE_PAYOUT", "Table game cash payout", "OUT", ""},
      {"WIRE_FUND", "Cash funding outgoing wire", "IN", ""},
      {"WIRE_PROCEEDS", "Wire proceeds paid in cash", "OUT", ""},
      {"CHECK_BUY", "Casino check purchased with cash", "IN", ""},
      {"CHECK_CASH", "Eligible check cashed", "OUT", ""},
      {"EXCHANGE", "Currency / bill exchange (demo reportable legs)", "BOTH", ""},
      {"TITO", "TITO redemption", "OUT", ""},
      {"SLOT_JACKPOT", "Slot jackpot — CTR excluded", "EXCLUDED", "SLOT"},
      {"PROMOTION", "Promotional cash prize", "OUT", "PROMOTION"},
      {"INCENTIVE", "Travel reimbursement / incentive", "OUT", ""},
      {"MACHINE_CASH", "Known-patron device cash insertion (demo policy)", "IN", ""},
      {"NONCASH", "Noncash account transfer", "NONCASH", ""}
    };
    List<TransactionType> types = new ArrayList<>();
    for (String[] d : definitions) {
      TransactionType t = new TransactionType();
      t.code = d[0];
      t.label = d[1];
      t.treatment = d[2];
      t.taxGame = d[3].isEmpty() ? null : d[3];
      t.active = true;
      t.state = "APPROVED";
      t.createdBy = "seed-maker";
      t.approvedBy = "seed-approver";
      types.add(s.types().save(t));
    }
    s.types().flush();
    String[] names = {
      "Avery Juniper",
      "Jordan Vale",
      "Casey Brook",
      "Morgan Cedar",
      "Riley Fern",
      "Taylor River",
      "Quinn Aspen",
      "Dakota Moss",
      "Parker Glen",
      "Reese Willow",
      "Rowan Sage",
      "Skyler Pine",
      "Hayden Birch",
      "Cameron Lake",
      "Finley Ash",
      "Emerson Oak",
      "Harper Cove",
      "Logan Meadow",
      "Alex Summit",
      "Jamie Reed",
      "Drew Canyon",
      "Blake Harbor",
      "Elliot Grove",
      "River Stone"
    };
    List<Patron> patrons = new ArrayList<>();
    for (int i = 0; i < names.length; i++) {
      Patron p = new Patron();
      p.legalName = names[i] + " [Synthetic]";
      p.dateOfBirth = LocalDate.of(1980 + i % 15, 1 + i % 12, 5 + i % 20);
      p.address = (100 + i) + " Fictional Way, Demo City, NJ 00000";
      p.contact = "patron" + (i + 1) + "@example.invalid";
      p.occupation = "Synthetic test profile";
      p.loyaltyId = "DEMO-L" + (1000 + i);
      p.identityNumber = "DEMO-ID-" + (1000 + i);
      p.tin = "DEMO-TIN-" + (1000 + i);
      p.identityStatus = "VERIFIED";
      p.identityExpiry = LocalDate.of(2030, 12, 31);
      p.w9Status = i % 3 == 0 ? "VERIFIED" : "REQUESTED";
      patrons.add(s.patrons().save(p));
    }
    s.patrons().flush();
    Instant now = Instant.now().minusSeconds(600);
    int seq = 0;
    // Patrons 1–8 are isolated boundary scenarios. Background activity uses patrons 9–24 only.
    Object[][] cases = {
      {0, 0, "10001"},
      {1, 0, "6000"},
      {1, 0, "4500"},
      {2, 0, "10000"},
      {3, 0, "7000"},
      {3, 1, "6000"},
      {4, 0, "12000"},
      {4, 1, "11000"},
      {5, 16, "12000"},
      {6, 20, "12000"}
    };
    for (Object[] c : cases) {
      int type = (int) c[1];
      String amount = (String) c[2];
      s.createTransaction(
          tx(
              "SEED-" + (++seq),
              patrons.get((int) c[0]),
              types.get(type),
              amount,
              type == 20 ? "0" : amount,
              now));
    }
    for (int i = 0; i < 100; i++) {
      int pi = 8 + i % 16;
      Instant at = now.minus(Duration.ofDays(i % 4));
      s.createTransaction(
          tx(
              "SEED-" + (++seq),
              patrons.get(pi),
              types.get(i % 2),
              String.valueOf(200 + (i % 5) * 75),
              String.valueOf(200 + (i % 5) * 75),
              at));
    }
    // Exact cutoff fixture on a separate patron; totals must stay on separate gaming days.
    Instant cutoff =
        LocalDate.now(ZoneId.of(prop.zone)).atTime(6, 0).atZone(ZoneId.of(prop.zone)).toInstant();
    if (cutoff.isAfter(now)) cutoff = cutoff.minus(Duration.ofDays(1));
    s.createTransaction(
        tx(
            "SEED-" + (++seq),
            patrons.get(7),
            types.get(0),
            "6000",
            "6000",
            cutoff.minusSeconds(1)));
    s.createTransaction(
        tx("SEED-" + (++seq), patrons.get(7), types.get(0), "6000", "6000", cutoff));
    Map<String, Object> sar = new HashMap<>();
    sar.put("patronId", patrons.get(10).id);
    sar.put("amount", "7500");
    sar.put("category", "POSSIBLE_STRUCTURING");
    sar.put(
        "narrative",
        "SYNTHETIC TRAINING SCENARIO: repeated cash activity is referred for analyst review. This"
            + " narrative does not describe a real person or a finding of wrongdoing.");
    sar.put("reason", "Synthetic review training fixture");
    s.createReport("SAR", sar);
    for (String code :
        List.of(
            "REPORTING_RULES", "TAX_RULES", "FILING_SETTINGS", "AUDIT_SETTINGS", "SAR_REFERRALS")) {
      Configuration c = new Configuration();
      c.code = code;
      c.payload =
          s.serialize(
              Map.of(
                  "demo",
                  true,
                  "version",
                  "DEMO-2026-v1",
                  "mode",
                  "proposal only",
                  "notes",
                  code.equals("TAX_RULES")
                      ? "Limited demonstration fixtures; no production tax determination"
                      : "Changes recorded for review; statutory mappings cannot be bypassed"));
      c.changedBy = "seed";
      c.reason = "Initial demonstration configuration";
      s.configs().save(c);
    }
    // Two historical batches with frozen report snapshots and deterministic item outcomes.
    List<Report> ctrs = s.reports().findAll().stream().filter(r -> r.kind.equals("CTR")).toList();
    for (int i = 0; i < Math.min(2, ctrs.size()); i++) {
      Report r = ctrs.get(i);
      r.status = "APPROVED";
      r.approver = "seed-approver";
      r.approvedRevision = r.revision;
      r.snapshot =
          s.serialize(
              Map.of(
                  "reportId",
                  r.id,
                  "patron",
                  s.patronView(r.patron),
                  "cashIn",
                  r.cashIn,
                  "cashOut",
                  r.cashOut,
                  "simulation",
                  true));
      r.filingStatus = "RESPONSE_RECEIVED";
      r.externalOutcome = i == 0 ? "ACCEPTED" : "REJECTED";
      r.externalId = "MOCK-SEED-" + i;
      r.obligationOpen = i != 0;
      s.reports().save(r);
      FilingBatch b = new FilingBatch();
      b.kind = "CTR";
      b.creator = "seed-filer";
      b.status = "RESPONSE_RECEIVED";
      b.scenario = i == 0 ? "ACCEPT" : "REJECT";
      b.submittedAt = now;
      b.respondedAt = now;
      b.transportReceipt = "MOCK-SEED-RECEIPT-" + i;
      b.rawResponse = s.serialize(Map.of("simulation", true, "outcome", r.externalOutcome));
      s.batches().saveAndFlush(b);
      BatchItem item = new BatchItem();
      item.batch = b;
      item.report = r;
      item.revision = r.revision;
      item.snapshot = r.snapshot;
      item.outcome = r.externalOutcome;
      item.externalId = r.externalId;
      item.message = "Synthetic historical fixture";
      s.items().save(item);
    }
    SecurityContextHolder.clearContext();
  }

  Map<String, Object> tx(
      String source, Patron p, TransactionType type, String gross, String cash, Instant at) {
    return Map.of(
        "sourceId",
        source,
        "patronId",
        p.id,
        "typeId",
        type.id,
        "gross",
        gross,
        "cash",
        cash,
        "occurredAt",
        at.toString(),
        "reference",
        "DEMO-REF-" + source,
        "status",
        "POSTED");
  }
}
