import { test } from "node:test";
import assert from "node:assert/strict";
import { mkdtempSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import { join } from "node:path";
import { EventStore, validateEvent } from "../src/store.js";
const event = {
  eventId: "event-1",
  transactionId: 5,
  patronId: 12,
  action: "POSTED",
  gamingDay: "2026-10-31",
  cashIn: "6000.00",
  cashOut: "0.00",
  at: "2026-10-31T12:00:00Z",
};
test("deduplicates replay and preserves events over restart", () => {
  const dir = mkdtempSync(join(tmpdir(), "cage-"));
  try {
    const file = join(dir, "events.ndjson");
    const a = new EventStore(file);
    assert.equal(a.add(event), true);
    assert.equal(a.add(event), false);
    const b = new EventStore(file);
    assert.equal(b.add(event), false);
    assert.equal(b.recent().length, 1);
  } finally {
    rmSync(dir, { recursive: true, force: true });
  }
});
test("rejects malformed events", () => {
  assert.throws(() => validateEvent({ ...event, transactionId: "invalid" }));
  assert.throws(() => validateEvent("{broken"));
});
test("drops unexpected fields to keep stream free of identity details", () => {
  assert.equal(
    validateEvent({ ...event, narrative: "restricted", tin: "private" })
      .narrative,
    undefined,
  );
});
