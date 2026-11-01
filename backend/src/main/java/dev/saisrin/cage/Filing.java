package dev.saisrin.cage;

import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.*;
import org.springframework.transaction.annotation.Transactional;

interface FilingAdapter {
  void submit(FilingBatch batch, String scenario);

  void reconcile(FilingBatch batch);
}

@Component
class MockFilingAdapter implements FilingAdapter {
  final Items items;
  final Reports reports;
  final Batches batches;

  MockFilingAdapter(Items i, Reports r, Batches b) {
    items = i;
    reports = r;
    batches = b;
  }

  public void submit(FilingBatch b, String scenario) {
    b.scenario = scenario;
    b.submittedAt = Instant.now();
    b.status = scenario.equals("TIMEOUT") ? "TRANSPORT_FAILED" : "AWAITING_RESPONSE";
    b.transportReceipt = scenario.equals("TIMEOUT") ? null : "MOCK-RECEIPT-" + b.id;
    for (BatchItem item : items.findByBatchId(b.id)) {
      Report r = item.report;
      r.snapshot = item.snapshot;
      r.filingStatus = b.status;
      r.externalOutcome = "UNCONFIRMED";
      reports.save(r);
    }
    batches.save(b);
  }

  public void reconcile(FilingBatch b) {
    if (b.scenario.equals("DELAY") && b.submittedAt.plusSeconds(15).isAfter(Instant.now())) return;
    List<Map<String, Object>> results = new ArrayList<>();
    int index = 0;
    for (BatchItem item : items.findByBatchId(b.id)) {
      String outcome =
          switch (b.scenario) {
            case "REJECT" -> "REJECTED";
            case "WARN" -> "ACCEPTED_WITH_WARNINGS";
            case "PARTIAL" -> index++ % 2 == 0 ? "ACCEPTED" : "REJECTED";
            default -> "ACCEPTED";
          };
      item.outcome = outcome;
      item.externalId = "MOCK-" + b.kind + "-" + b.id + "-" + item.id;
      item.message =
          outcome.equals("REJECTED")
              ? "MOCK-E101: simulated identity validation error"
              : outcome.equals("ACCEPTED_WITH_WARNINGS")
                  ? "MOCK-W001: simulated optional field warning"
                  : "Simulated acknowledgment";
      items.save(item);
      Report r = item.report;
      r.filingStatus = "RESPONSE_RECEIVED";
      r.externalOutcome = outcome;
      r.externalId = item.externalId;
      r.obligationOpen = outcome.equals("REJECTED");
      reports.save(r);
      if (r.corrects != null && !outcome.equals("REJECTED")) {
        r.corrects.obligationOpen = false;
        r.corrects.status = "SUPERSEDED";
        reports.save(r.corrects);
      }
      results.add(
          Map.of(
              "itemId",
              item.id,
              "outcome",
              outcome,
              "externalId",
              item.externalId,
              "message",
              item.message));
    }
    b.status = "RESPONSE_RECEIVED";
    b.respondedAt = Instant.now();
    b.rawResponse =
        new com.fasterxml.jackson.databind.ObjectMapper()
            .valueToTree(Map.of("simulation", true, "items", results))
            .toString();
    batches.save(b);
  }
}

@Component
class ProductionFilingAdapter implements FilingAdapter {
  public void submit(FilingBatch b, String s) {
    throw new IllegalStateException(
        "Production filing is not implemented. No external submission was made.");
  }

  public void reconcile(FilingBatch b) {
    throw new IllegalStateException("Production acknowledgment ingestion is not implemented.");
  }
}

@Service
@Transactional
class FilingService {
  final CageService s;
  final FilingAdapter adapter;
  final boolean controls;

  FilingService(
      CageService s,
      MockFilingAdapter mock,
      ProductionFilingAdapter prod,
      @Value("${cage.filing-mode}") String mode,
      @Value("${cage.simulation-controls}") boolean controls) {
    this.s = s;
    adapter = mode.equals("mock") ? mock : prod;
    this.controls = controls;
  }

  public FilingBatch action(Long id, String action, Map<String, Object> m) {
    s.require("FILE");
    s.properties().lock();
    FilingBatch b = s.batch(id);
    s.stale(b, m);
    String why = s.reason(m);
    if (action.equals("submit")) {
      s.check(
          Set.of("PREPARED", "EXPORTED").contains(b.status),
          "Reconcile the existing attempt before any retry");
      s.batchValid(b);
      String scenario = s.s(m, "scenario", "ACCEPT");
      s.check(
          Set.of("ACCEPT", "WARN", "REJECT", "PARTIAL", "DELAY", "TIMEOUT").contains(scenario),
          "Invalid simulation scenario");
      s.check(controls || scenario.equals("ACCEPT"), "Controlled response scenarios are disabled");
      adapter.submit(b, scenario);
    } else if (action.equals("reconcile")) {
      s.check(
          Set.of("AWAITING_RESPONSE", "TRANSPORT_FAILED").contains(b.status),
          "Nothing awaiting reconciliation");
      adapter.reconcile(b);
    } else throw new IllegalArgumentException("Unknown batch action");
    s.audit(b.kind, "Batch", id, action.toUpperCase(), why, null, b.status);
    return b;
  }
}
