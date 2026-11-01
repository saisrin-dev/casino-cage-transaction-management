import { appendFileSync, readFileSync, existsSync, mkdirSync } from "node:fs";
import { dirname } from "node:path";
export function validateEvent(value) {
  const e = typeof value === "string" ? JSON.parse(value) : value;
  if (
    !e ||
    typeof e.eventId !== "string" ||
    !Number.isInteger(e.transactionId) ||
    !Number.isInteger(e.patronId) ||
    !["POSTED", "VOID", "CORRECT", "POST"].includes(e.action) ||
    !/^\d{4}-\d{2}-\d{2}$/.test(e.gamingDay)
  )
    throw Error("Invalid transaction event");
  return {
    eventId: e.eventId,
    transactionId: e.transactionId,
    patronId: e.patronId,
    action: e.action,
    gamingDay: e.gamingDay,
    cashIn: String(e.cashIn),
    cashOut: String(e.cashOut),
    at: e.at,
  };
}
export class EventStore {
  constructor(file) {
    this.file = file;
    mkdirSync(dirname(file), { recursive: true });
    this.events = existsSync(file)
      ? readFileSync(file, "utf8")
          .split("\n")
          .filter(Boolean)
          .map((x) => validateEvent(x))
      : [];
    this.ids = new Set(this.events.map((x) => x.eventId));
  }
  add(value) {
    const e = validateEvent(value);
    if (this.ids.has(e.eventId)) return false;
    appendFileSync(this.file, JSON.stringify(e) + "\n");
    this.events.push(e);
    this.ids.add(e.eventId);
    return true;
  }
  recent() {
    return this.events.slice(-100).reverse();
  }
}
