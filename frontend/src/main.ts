import { DemoStore } from "./demo-store";
import { Component, HostListener, signal } from "@angular/core";
import { bootstrapApplication } from "@angular/platform-browser";
import { CommonModule } from "@angular/common";
import { FormsModule } from "@angular/forms";
type Row = Record<string, any>;
type Field = {
  key: string;
  label: string;
  type?: string;
  required?: boolean;
  options?: { value: any; label: string }[];
  readonly?: boolean;
};
@Component({
  selector: "app-root",
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: "./app.html",
})
class App {
  newSourceId() { return typeof crypto.randomUUID === 'function' ? crypto.randomUUID() : 'DEMO-' + Date.now() + '-' + Math.random().toString(36).slice(2); }
  Math = Math;
  Number = Number;
  asyncNoop = async () => {};
  store = new DemoStore((() => { try { return window.localStorage; } catch { return null; } })());
  me: Row | null = null;
  txForm: Row = {};
  txSaved: Row | null = null;
  get entryType() { return this.types.find(t => t['id'] === +this.txForm['typeId']); }
  get entryPatron() { return this.patrons.find(p => p['id'] === +this.txForm['patronId']); }
  get cashPreview() { return this.money(this.txForm['cash']); }
  startEntry() {
    this.txForm = {sourceId: this.newSourceId(), occurredAt: new Date().toISOString(), station: 'Main cage', status: 'POSTED', gross: '', cash: '', reason: 'Synthetic demo entry'};
    this.txSaved = null;
    this.go('entry');
  }
  async submitEntry() {
    const ok = await this.run(async () => { this.txSaved = await this.api('/transactions', 'POST', this.txForm); }, 'Transaction saved in this browser');
    if (ok) { this.txForm = {...this.txForm, sourceId: this.newSourceId(), gross: '', cash: '', reference: ''}; }
  }
  downloadEvidence(a: Row) {
    const raw = atob(a['data'].split(',')[1]);
    const bytes = Uint8Array.from(raw, ch => ch.charCodeAt(0));
    const url = URL.createObjectURL(new Blob([bytes], {type: 'application/octet-stream'}));
    const link = document.createElement('a'); link.href = url; link.download = a['filename']; link.click(); URL.revokeObjectURL(url);
  }
  resetDemo() {
    this.open('Reset browser demo', [this.f('confirmation','Type RESET to restore sample data','text',true)], {}, async v => {
      if (v['confirmation'] !== 'RESET') throw Error('Type RESET to confirm.');
      this.store.reset(); this.detail = null; this.txSaved = null;
    });
  }

  ready = false;
  busy = false;
  error = "";
  toast = "";
  demo: Row = {};
  page = "dashboard";
  tab = "Review";
  query = "";
  filter = "";
  pageIndex = 0;
  pageSize = 10;
  data: Row[] = [];
  patrons: Row[] = [];
  types: Row[] = [];
  reports: Row[] = [];
  batches: Row[] = [];
  users: Row[] = [];
  configs: Row[] = [];
  dashboard: Row = {};
  property: Row = {};
  selected: number[] = [];
  detail: Row | null = null;
  detailType = "";
  detailTab = "Details";
  history: Row[] = [];
  linked: Row[] = [];
  versions: Row[] = [];
  items: Row[] = [];
  drawer = false;
  attachments: Row[] = [];
  subjects: Row[] = [];
  modal = "";
  fields: Field[] = [];
  form: Row = {};
  editId: number | null = null;
  modalAction: ((v: Row) => Promise<any>) | null = null;
  menu = [
    { id: "dashboard", label: "Overview", icon: "◈", group: "WORKSPACE" },
    { id: "patrons", label: "Subject Manager", icon: "◎" },
    { id: "transactions", label: "Transaction Manager", icon: "⇄" },
    { id: "entry", label: "Transaction Entry", icon: "+" },
    { id: "CTR", label: "CTR Manager", icon: "▤", group: "COMPLIANCE" },
    { id: "SAR", label: "SAR Manager", icon: "◇" },
    { id: "TAX", label: "Tax Forms Manager", icon: "▧" },
    {
      id: "setup",
      label: "Setup Manager",
      icon: "⚙",
      group: "ADMINISTRATION",
    },
  ];
  constructor() {
    this.init();
  }
  async init() {
    try { this.demo = await this.api('/demo'); this.me = await this.api('/me'); await this.load(); }
    catch(e) { this.error = this.message(e); }
    finally { this.ready = true; }
  }
  async api(path: string, method = "GET", body?: any): Promise<any> {
    return this.store.request(path, method, body);
  }
  message(e: any) {
    return e?.message || String(e);
  }
  can(p: string) {
    return this.me?.["permissions"]?.includes(p) || false;
  }
  read(p: string) {
    return this.can(p) || this.can(p + "_READ");
  }
  visible(id: string) {
    return (
      id === "dashboard" || id === "entry" ||
      (id === "setup" && this.can("SETUP")) ||
      (id === "patrons" && this.read("PATRON")) ||
      (id === "transactions" && this.read("TRANSACTION")) ||
      (["CTR", "SAR", "TAX"].includes(id) && this.read(id))
    );
  }
  async run(fn: () => Promise<any>, message = "Saved successfully") {
    this.busy = true;
    this.error = "";
    try {
      await fn();
      this.toast = message;
      setTimeout(() => (this.toast = ""), 5000);
      await this.load();
      return true;
    } catch (e) {
      this.error = this.message(e);
      return false;
    } finally {
      this.busy = false;
    }
  }
  async go(page: string) {
    this.page = page;
    this.tab = "Review";
    this.query = "";
    this.filter = "";
    this.pageIndex = 0;
    this.selected = [];
    this.detail = null;
    this.data = [];
    await this.run(async () => {}, "");
  }
  async load() {
    if (!this.me) return;
    this.patrons = await this.api("/patrons");
    this.types = await this.api("/types");
    this.property = await this.api("/property");
    if (this.page === "entry") { this.data = []; }
    else if (this.page === "dashboard") {
      this.dashboard = await this.api("/dashboard");
      this.data = [];
    } else if (this.page === "patrons") this.data = this.patrons;
    else if (this.page === "transactions")
      this.data = await this.api("/transactions");
    else if (this.page === "setup") {
      this.configs = await this.api("/configurations");
      this.users = await this.api("/users");
      this.data = this.types;
    } else {
      this.reports = await this.api("/reports?kind=" + this.page);
      this.batches = await this.api("/batches?kind=" + this.page);
      this.switchTab(this.tab);
    }
  }
  switchTab(tab: string) {
    this.tab = tab;
    this.pageIndex = 0;
    this.selected = [];
    this.detail = null;
    this.query = "";
    this.filter = "";
    this.data =
      tab === "Batch"
        ? this.batches
        : tab === "W-9 Register"
          ? this.patrons
          : tab === "History"
            ? this.reports.filter(
                (r) =>
                  r.snapshot ||
                  [
                    "REJECTED_INTERNALLY",
                    "CLOSED_WITHOUT_FILING",
                    "SUPERSEDED",
                  ].includes(r.status),
              )
            : this.reports.filter(
                (r) =>
                  !r.snapshot &&
                  ![
                    "REJECTED_INTERNALLY",
                    "CLOSED_WITHOUT_FILING",
                    "SUPERSEDED",
                  ].includes(r.status),
              );
  }
  get title() {
    return this.menu.find((m) => m.id === this.page)?.label || "CageLedger";
  }
  get filtered() {
    const q = this.query.toLowerCase();
    return this.data.filter(
      (r) =>
        (!q || JSON.stringify(r).toLowerCase().includes(q)) &&
        (!this.filter ||
          (r.status || r.identityStatus || r.w9Status) === this.filter),
    );
  }
  get rows() {
    return this.filtered.slice(
      this.pageIndex * this.pageSize,
      (this.pageIndex + 1) * this.pageSize,
    );
  }
  get maxPage() {
    return Math.max(1, Math.ceil(this.filtered.length / this.pageSize));
  }
  get isReport() {
    return ["CTR", "SAR", "TAX"].includes(this.page);
  }
  get reportTabs() {
    return this.page === "TAX"
      ? ["Review", "Batch", "History", "W-9 Register"]
      : ["Review", "Batch", "History"];
  }
  money(v: any) {
    return new Intl.NumberFormat("en-US", {
      style: "currency",
      currency: "USD",
    }).format(Number(v || 0));
  }
  date(v: any) {
    return v
      ? new Date(v).toLocaleString("en-US", {
          timeZone: this.property["zone"] || "America/New_York",
          dateStyle: "medium",
          timeStyle: "short",
        })
      : "—";
  }
  label(v: any) {
    return String(v || "—").replaceAll("_", " ");
  }
  badge(v: any) {
    return ["APPROVED", "ACCEPTED", "VERIFIED", "POSTED"].includes(v)
      ? "good"
      : [
            "REJECTED",
            "REJECTED_INTERNALLY",
            "VOIDED",
            "EXPIRED",
            "TRANSPORT_FAILED",
          ].includes(v)
        ? "bad"
        : [
              "PENDING_REVIEW",
              "AWAITING_RESPONSE",
              "ACCEPTED_WITH_WARNINGS",
              "RETURNED",
            ].includes(v)
          ? "warn"
          : "neutral";
  }
  select(id: number, event: any) {
    this.selected = event.target.checked
      ? [...this.selected, id]
      : this.selected.filter((i) => i !== id);
  }
  patronOptions() {
    return this.patrons
      .filter((p) => !p["mergedInto"])
      .map((p) => ({
        value: p["id"],
        label: `P-${String(p["id"]).padStart(4, "0")} · ${p["legalName"]}`,
      }));
  }
  f(
    key: string,
    label: string,
    type = "text",
    required = false,
    options?: { value: any; label: string }[],
  ): Field {
    return { key, label, type, required, options };
  }
  open(
    title: string,
    fields: Field[],
    values: Row,
    action: (v: Row) => Promise<any>,
  ) {
    this.modal = title;
    this.fields = fields;
    this.form = { ...values };
    this.modalAction = action;
    this.error = "";
  }
  async save() {
    if (!this.modalAction) return;
    const previous = this.detail;
    const previousType = this.detailType;
    const ok = await this.run(() => this.modalAction!(this.form));
    if (ok) {
      this.modal = "";
      if (previous) await this.show(previous, previousType);
    }
  }
  newPatron(p?: Row) {
    const fields = [
      this.f("legalName", "Legal name", "text", true),
      this.f("aliasName", "Alias"),
      this.f("dateOfBirth", "Date of birth", "date", true),
      this.f("address", "Residential address", "text", true),
      this.f("country", "Country code", "text", true),
      this.f("contact", "Contact email", "email"),
      this.f("occupation", "Occupation / business"),
      this.f("loyaltyId", "Loyalty account"),
      this.f("identityType", "Identification type"),
      this.f("identityNumber", "Synthetic ID (DEMO- prefix)"),
      this.f("issuingAuthority", "Issuing authority"),
      this.f("identityExpiry", "Identity expiry", "date"),
      this.f("tin", "Synthetic TIN (DEMO- prefix)"),
      this.f(
        "identityStatus",
        "Verification status",
        "select",
        true,
        ["VERIFIED", "UNVERIFIED", "EXPIRED", "UNAVAILABLE", "REFUSED"].map(
          (x) => ({ value: x, label: x }),
        ),
      ),
      this.f(
        "residency",
        "Residency",
        "select",
        true,
        ["RESIDENT", "NONRESIDENT", "UNKNOWN"].map((x) => ({
          value: x,
          label: x,
        })),
      ),
      this.f("reason", "Reason", "textarea", true),
    ];
    if (this.can("SUPERVISE"))
      fields.splice(
        fields.length - 1,
        0,
        this.f(
          "temporary",
          "Unidentified activity exception",
          "select",
          false,
          [
            { value: false, label: "Named patron" },
            { value: true, label: "Temporary supervised identity" },
          ],
        ),
      );
    this.open(
      p ? "Edit patron" : "Create synthetic patron",
      fields,
      p || {
        country: "US",
        identityStatus: "UNVERIFIED",
        residency: "RESIDENT",
        identityType: "DEMO_ID",
      },
      (v) =>
        this.api("/patrons" + (p ? "/" + p["id"] : ""), p ? "PUT" : "POST", v),
    );
  }
  newTx(t?: Row) {
    const vals: Row = t
      ? {
          ...t,
          patronId: t["patron"].id,
          typeId: t["type"].id,
          conductorId: t["conductor"]?.id || "",
          sourceId: this.newSourceId(),
          occurredAt: t["occurredAt"],
        }
      : {
          sourceId: this.newSourceId(),
          occurredAt: new Date().toISOString(),
          station: "Main cage",
          gross: "",
          cash: "",
          status: "POSTED",
        };
    const fields = [
      this.f(
        "patronId",
        "Beneficiary / patron",
        "select",
        true,
        this.patronOptions(),
      ),
      this.f(
        "typeId",
        "Transaction type",
        "select",
        true,
        this.types
          .filter((t) => t["active"])
          .map((t) => ({
            value: t["id"],
            label: t["label"] + " · " + t["treatment"],
          })),
      ),
      this.f("gross", "Gross amount (USD)", "number", true),
      this.f("cash", "Actual cash component (USD)", "number", true),
      this.f(
        "occurredAt",
        "Event timestamp (ISO 8601 with offset)",
        "text",
        true,
      ),
      this.f("sourceId", "Unique source event ID", "text", true),
      this.f("station", "Cage / station", "text", true),
      this.f("reference", "Marker / machine / account reference", "text", true),
      this.f("conductorId", "Conductor, if different", "select", false, [
        { value: "", label: "Same as beneficiary" },
        ...this.patronOptions(),
      ]),
      this.f("relationship", "Conductor relationship"),
      this.f("status", "Save as", "select", true, [
        { value: "POSTED", label: "Post transaction" },
        { value: "DRAFT", label: "Save draft" },
      ]),
      this.f("notes", "Notes", "textarea"),
      this.f("reason", "Entry / correction reason", "textarea", true),
    ];
    this.open(
      t ? "Correct transaction · preserve original" : "Enter transaction",
      fields,
      vals,
      (v) =>
        this.api(
          "/transactions" + (t ? "/" + t["id"] + "/correct" : ""),
          "POST",
          v,
        ),
    );
  }
  newReport(r?: Row) {
    const kind = this.page;
    let fields: Field[] = [];
    let vals: Row = r
      ? { ...r }
      : {
          kind,
          patronId: "",
          detectionDate: new Date().toISOString().slice(0, 10),
          activityStart: new Date().toISOString().slice(0, 10),
          activityEnd: new Date().toISOString().slice(0, 10),
          formType: "W-2G",
          game: "SLOT",
          gross: "",
          wager: "0",
          reason: "",
        };
    if (!r)
      fields.push(
        this.f("patronId", "Patron", "select", true, this.patronOptions()),
      );
    if (kind === "SAR")
      fields.push(
        this.f("detectionDate", "Initial detection date", "date", true),
        this.f("activityStart", "Activity start", "date", true),
        this.f("activityEnd", "Activity end", "date", true),
        this.f("amount", "Suspicious amount (USD)", "number", true),
        this.f(
          "category",
          "Referral category",
          "select",
          true,
          [
            "POSSIBLE_STRUCTURING",
            "MINIMAL_PLAY",
            "THIRD_PARTY",
            "CONFLICTING_ID",
            "UNUSUAL_MOVEMENT",
            "BILL_STUFFING",
            "OTHER",
          ].map((x) => ({ value: x, label: this.label(x) })),
        ),
        this.f(
          "narrative",
          "Synthetic investigation narrative",
          "textarea",
          true,
        ),
      );
    else if (kind === "TAX") {
      if (r) {
        try {
          Object.assign(vals, JSON.parse(r["fields"]));
        } catch {}
      }
      fields.push(
        this.f(
          "game",
          "Game / prize category",
          "select",
          true,
          ["SLOT", "BINGO", "KENO", "POKER", "PROMOTION", "OTHER"].map((x) => ({
            value: x,
            label: x,
          })),
        ),
        this.f(
          "formType",
          "Eligible tax form",
          "select",
          true,
          ["W-2G", "1099-MISC", "1098"].map((x) => ({ value: x, label: x })),
        ),
        this.f("gross", "Source winnings (USD)", "number", true),
        this.f("wager", "Wager (USD)", "number", true),
      );
    } else fields.push(this.f("reviewNotes", "Review notes", "textarea", true));
    fields.push(this.f("reason", "Reason", "textarea", true));
    this.open(
      r
        ? "Edit report · approval will be revoked"
        : "Create " + kind + " · simulation",
      fields,
      vals,
      (v) =>
        this.api("/reports" + (r ? "/" + r["id"] : ""), r ? "PUT" : "POST", v),
    );
  }
  async show(row: Row, type?: string) {
    this.error = "";
    this.detailType =
      type ||
      (this.page === "patrons"
        ? "Patron"
        : this.page === "transactions"
          ? "Transaction"
          : this.tab === "W-9 Register"
            ? "Patron"
            : this.tab === "Batch"
              ? "Batch"
              : "Report");
    this.detailTab = "Details";
    this.drawer = false;
    try {
      this.detail =
        this.detailType === "Report"
          ? await this.api("/reports/" + row["id"])
          : this.detailType === "Patron"
            ? await this.api("/patrons/" + row["id"])
            : this.detailType === "Transaction"
              ? (await this.api("/transactions")).find(
                  (x: Row) => x["id"] === row["id"],
                )
              : this.detailType === "Batch"
                ? (await this.api("/batches?kind=" + row["kind"])).find(
                    (x: Row) => x["id"] === row["id"],
                  )
                : row;
      this.history = await this.api(
        "/audit?entityType=" + this.detailType + "&entityId=" + row["id"],
      );
      this.linked = [];
      this.items = [];
      this.versions = [];
      if (this.detailType === "Report") {
        this.linked = await this.api("/reports/" + row["id"] + "/transactions");
        this.versions = await this.api("/reports/" + row["id"] + "/versions");
      }
      if (this.detailType === "Patron") {
        if (this.read("TRANSACTION"))
          this.linked = (await this.api("/transactions")).filter(
            (t: Row) => t["patron"].id === row["id"],
          );
      }
      if (["Report", "Patron", "Transaction"].includes(this.detailType))
        this.attachments = await this.api(
          "/attachments?ownerType=" + this.detailType + "&ownerId=" + row["id"],
        );
      else this.attachments = [];
      if (this.detailType === "Report")
        this.subjects = await this.api("/reports/" + row["id"] + "/subjects");
      else this.subjects = [];
      if (this.detailType === "Batch")
        this.items = await this.api("/batches/" + row["id"] + "/items");
    } catch (e) {
      this.error = this.message(e);
    }
  }
  close() {
    this.detail = null;
  }
  confirmAction(type: string, row: Row, action: string) {
    this.open(
      this.label(action) + " · " + type,
      [this.f("reason", "Reason / resolution", "textarea", true)],
      { version: row["version"] },
      (v) =>
        this.api(
          "/" +
            (
              {
                Transaction: "transactions",
                Report: "reports",
                Type: "types",
              } as Row
            )[type] +
            "/" +
            row["id"] +
            "/" +
            action,
          "POST",
          v,
        ),
    );
  }
  async validate() {
    if (this.detail)
      await this.run(async () => {
        const r = await this.api(
          "/reports/" + this.detail!["id"] + "/validation",
        );
        if (r.errors.length) throw Error(r.errors.join("; "));
      }, "Demo schema validation passed");
  }
  async makeBatch() {
    await this.run(
      () =>
        this.api("/batches", "POST", {
          kind: this.page,
          reportIds: this.selected,
        }),
      "Simulated batch prepared",
    );
    this.switchTab("Batch");
  }
  batchAction(b: Row, action: string) {
    this.open(
      action === "submit"
        ? "Submit to filing simulator"
        : "Reconcile simulated response",
      [
        ...(action === "submit"
          ? [
              this.f(
                "scenario",
                "Mock response scenario",
                "select",
                true,
                (this.demo["simulationControls"]
                  ? ["ACCEPT", "WARN", "REJECT", "PARTIAL", "DELAY", "TIMEOUT"]
                  : ["ACCEPT"]
                ).map((x) => ({ value: x, label: x })),
              ),
            ]
          : []),
        this.f("reason", "Reason", "textarea", true),
      ],
      { version: b["version"], scenario: "ACCEPT" },
      (v) => this.api("/batches/" + b["id"] + "/" + action, "POST", v),
    );
  }
  async downloadBatch(b: Row) {
    await this.run(async () => {
      const data = await this.api(
        "/batches/" + b["id"] + "/export",
        "POST",
        {},
      );
      this.download(
        JSON.stringify(data, null, 2),
        "MOCK-batch-" + b["id"] + ".json",
        "application/json",
      );
    }, "Simulated export downloaded");
    if (this.detail)
      await this.show(
        this.batches.find((x) => x["id"] === b["id"]) || b,
        "Batch",
      );
  }
  download(data: string, name: string, type: string) {
    const url = URL.createObjectURL(new Blob([data], { type }));
    const a = document.createElement("a");
    a.href = url;
    a.download = name;
    a.click();
    URL.revokeObjectURL(url);
  }
  receipt() {
    if (!this.detail) return;
    const t = this.detail;
    this.download(
      [
        "CAGELEDGER — SYNTHETIC DEMO RECEIPT",
        "Transaction: " + t["id"],
        "Patron: " + t["patron"].legalName,
        "Type: " + t["typeLabel"],
        "Gross: " + this.money(t["gross"]),
        "Cash: " + this.money(t["cash"]),
        "Gaming day: " + t["gamingDay"],
        "Status: " + t["status"],
        "No payment was processed.",
      ].join("\n"),
      "MOCK-receipt-" + t["id"] + ".txt",
      "text/plain",
    );
  }
  recipient() {
    if (this.detail)
      this.download(
        "DEMO RULES / SIMULATED RECIPIENT COPY — NOT AN IRS FORM\n" +
          JSON.stringify(this.detail, null, 2),
        "MOCK-tax-copy-" + this.detail["id"] + ".txt",
        "text/plain",
      );
  }
  newType(t?: Row) {
    this.open(
      t ? "Clone transaction definition" : "Propose transaction type",
      [
        this.f("code", "Unique code", "text", true),
        this.f("label", "Display label", "text", true),
        this.f(
          "treatment",
          "Currency treatment",
          "select",
          true,
          ["IN", "OUT", "BOTH", "EXCLUDED", "NONCASH"].map((x) => ({
            value: x,
            label: x,
          })),
        ),
        this.f("effectiveFrom", "Effective from", "date", true),
        this.f("reason", "Compliance mapping rationale", "textarea", true),
      ],
      t
        ? { ...t, code: t["code"] + "_V2" }
        : {
            treatment: "IN",
            effectiveFrom: new Date().toISOString().slice(0, 10),
          },
      (v) => this.api("/types", "POST", v),
    );
  }
  propertyEdit() {
    this.open(
      "Property settings",
      [
        this.f("name", "Property name", "text", true),
        this.f("contact", "Filing contact", "email", true),
        this.f("reason", "Reason", "textarea", true),
      ],
      this.property,
      (v) => this.api("/property", "PUT", v),
    );
  }
  configEdit(c: Row) {
    this.open(
      "Propose configuration · " + c["code"],
      [
        this.f(
          "payload",
          "Configuration JSON (proposal only)",
          "textarea",
          true,
        ),
        this.f("reason", "Reason", "textarea", true),
      ],
      c,
      (v) => this.api("/configurations/" + c["id"], "PUT", v),
    );
  }
  newUser() {
    this.open(
      "Create demo user",
      [
        this.f("username", "Username", "text", true),
        this.f("displayName", "Display name", "text", true),
        this.f(
          "roles",
          "Role",
          "select",
          true,
          [
            "CASHIER",
            "SUPERVISOR",
            "ANALYST",
            "APPROVER",
            "FILER",
            "TAX",
            "ADMIN",
            "AUDITOR",
          ].map((x) => ({ value: x, label: x })),
        ),
        this.f("password", "Password (12+ characters)", "password", true),
      ],
      {},
      (v) => this.api("/users", "POST", v),
    );
  }
  w9(p: Row) {
    this.open(
      "W-9 register · retained internally",
      [
        this.f(
          "status",
          "Certification state",
          "select",
          true,
          ["REQUESTED", "RECEIVED", "VERIFIED", "REPLACEMENT"].map((x) => ({
            value: x,
            label: x,
          })),
        ),
        this.f("reason", "Reason", "textarea", true),
      ],
      { version: p["version"], status: p["w9Status"] },
      (v) => this.api("/patrons/" + p["id"] + "/w9", "POST", v),
    );
  }
  merge(p: Row) {
    this.open(
      "Merge into surviving patron",
      [
        this.f(
          "targetId",
          "Surviving profile",
          "select",
          true,
          this.patronOptions().filter((x) => x.value !== p["id"]),
        ),
        this.f("reason", "Merge decision", "textarea", true),
      ],
      { version: p["version"] },
      (v) => this.api("/patrons/" + p["id"] + "/merge", "POST", v),
    );
  }
  async uploadEvidence(event: any) {
    const file = event.target.files?.[0];
    if (!file || !this.detail) return;
    const current = this.detail;
    await this.run(() => this.store.attach(file, this.detailType, current!['id']), "Evidence saved in this browser");
    await this.show(this.detail, this.detailType);
  }
  linkSubject() {
    if (!this.detail) return;
    const d = this.detail;
    this.open(
      "Link additional report subject",
      [
        this.f(
          "patronId",
          "Additional subject",
          "select",
          true,
          this.patronOptions().filter((o) => o.value !== d["patron"].id),
        ),
        this.f("relationship", "Subject relationship", "text", true),
        this.f("reason", "Reason", "textarea", true),
      ],
      { version: d["version"] },
      (v) => this.api("/reports/" + d["id"] + "/subjects", "POST", v),
    );
  }
  resolveTask(t: Row) {
    this.open(
      "Resolve compliance task",
      [this.f("reason", "Documented resolution", "textarea", true)],
      {},
      (v) => this.api("/tasks/" + t["id"] + "/resolve", "POST", v),
    );
  }
  pretty(value: any) {
    try {
      return typeof value === "string"
        ? JSON.stringify(JSON.parse(value), null, 2)
        : JSON.stringify(value, null, 2);
    } catch {
      return String(value);
    }
  }
  @HostListener("document:keydown.escape") escape() {
    if (this.modal) this.modal = "";
    else if (this.detail) this.close();
    this.drawer = false;
  }
}
bootstrapApplication(App).catch(console.error);
