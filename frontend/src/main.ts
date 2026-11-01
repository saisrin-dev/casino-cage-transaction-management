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
  subjectSearch: Row = {kind:'PATRON',card:'',number:'',first:'',last:'',birth:'',identity:''};
  searchResults: Row[] = [];
  searchedSubjects = false;
  searchOpen = true;
  resultsOpen = true;
  profileOpen = true;
  selectedSubject: Row | null = null;
  dayTransactions: Row[] = [];
  entrySelection: number | null = null;
  entryEditing: Row | null = null;
  pendingEvidence: File | null = null;
  entryTime = '';
  entryDate = '';
  ctrSearch: Row = {number:'',subject:'',first:'',last:'',date:'',state:'',incomplete:false};
  ctrErrorsOpen = false;
  launcher = [
    {id:'patrons', label:'Subject Manager',icon:'subject',shortcuts:[{label:'Profiles',action:'profiles',icon:'person'},{label:'Create',action:'create',icon:'plus'}]},
    {id:'entry', label:'Transaction Manager',icon:'transaction',shortcuts:[{label:'Entry',action:'entry',icon:'plus'},{label:'History',action:'history',icon:'document'}]},
    {id:'SAR', label:'SAR Manager',icon:'search',shortcuts:[{label:'Review',action:'Review',icon:'check'},{label:'Batch',action:'Batch',icon:'document'},{label:'History',action:'History',icon:'history'}]},
    {id:'CTR', label:'CTR Manager',icon:'cash',shortcuts:[{label:'Review',action:'Review',icon:'check'},{label:'Batch',action:'Batch',icon:'document'},{label:'History',action:'History',icon:'history'}]},
    {id:'setup', label:'Setup Manager',icon:'settings',shortcuts:[{label:'Property',action:'property',icon:'shield'},{label:'Types',action:'types',icon:'settings'}]},
    {id:'TAX', label:'Tax Forms Manager',icon:'tax',shortcuts:[{label:'Review',action:'Review',icon:'check'},{label:'W-9',action:'W-9 Register',icon:'document'}]},
  ];
  iconPath(key: string): string {
    return ({home:'M3 14 15 4l12 10M7 12v15h16V12M12 27v-9h6v9',subject:'M8 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8M4 14h8v8H4zM22 11a4 4 0 1 0 0-8 4 4 0 0 0 0 8M18 14h8v8h-8zM15 7V3m-3 2 3-2 3 2M9 26h12',person:'M15 14a5 5 0 1 0 0-10 5 5 0 0 0 0 10M6 27v-5a9 9 0 0 1 18 0v5',transaction:'M5 10a11 11 0 0 1 19-3m0-4v4h-4M25 20a11 11 0 0 1-19 3m0 4v-4h4M10 11h10v8H10zM15 12v6',plus:'M5 9h14M5 15h10M5 21h8M22 14v12m-6-6h12',search:'M20 20l8 8M14 24a10 10 0 1 0 0-20 10 10 0 0 0 0 20M10 10a4 4 0 1 1 8 0M8 19c0-5 12-5 12 0',check:'M9 6H5v22h20V6h-4M10 3h10v6H10zM9 18l4 4 8-9',document:'M9 3h17v22H9zM4 8v21h17M13 9h9M13 14h9M13 19h5',history:'M7 6h17v22H7zM11 3h9v6h-9zM11 14h9M11 19h9M11 24h5',cash:'M3 6h24v14H3zM7 23h20M10 26h17M15 17a4 4 0 1 0 0-8 4 4 0 0 0 0 8M5 9h2m16 8h2',settings:'M15 3v4m0 16v4M3 15h4m16 0h4M6 6l3 3m12 12 3 3M6 24l3-3M21 9l3-3M15 23a8 8 0 1 0 0-16 8 8 0 0 0 0 16M15 19a4 4 0 1 0 0-8 4 4 0 0 0 0 8',shield:'M15 3l11 5v7c0 7-11 13-11 13S4 22 4 15V8zM15 6v18',tax:'M6 3h18v25H6zM10 9h10M10 14h6M10 20h4M21 17l7 11m-7 0 7-11'} as Row)[key] || 'M5 5h20v20H5z';
  }
  async launch(id:string, action='') {
    if(id==='entry') { if(action==='history')await this.go('transactions'); else this.startEntry(); return; }
    await this.go(id);
    if(id==='patrons'&&action==='create')this.newPatron();
    else if(this.isReport&&action)this.switchTab(action);
  }
  localDateTime(value = new Date().toISOString()) {
    const d = new Date(value); return new Date(d.getTime()-d.getTimezoneOffset()*60000).toISOString().slice(0,16);
  }
  startEntry() {
    this.txForm = {sourceId: this.newSourceId(), occurredAt: new Date().toISOString(), station: 'Main cage', site: 'Juniper', status: 'POSTED', gross: '', cash: '', nonCash: '', reason: 'Synthetic demo entry', reference: 'DEMO-'+Date.now().toString().slice(-7)};
    this.entryTime=this.localDateTime();this.entryDate=this.store.day();this.txSaved=null;this.entryEditing=null;this.entrySelection=null;this.pendingEvidence=null;
    if(this.selectedSubject)this.txForm['patronId']=this.selectedSubject['id'];
    this.go('entry');
  }
  searchSubjects() {
    const q=this.subjectSearch;
    const text=(x:any)=>String(x||'').toLowerCase();
    this.searchResults=this.patrons.filter(p=>!p['mergedInto'] && (p['subjectKind']||'PATRON')===q['kind'] &&
      (!q['card']||text(p['loyaltyId']).includes(text(q['card']))) &&
      (!q['number']||String(p['id']).includes(q['number'])||('AML'+String(p['id']).padStart(10,'0')).includes(String(q['number']).toUpperCase())) &&
      (!q['first']||text(p['legalName']).includes(text(q['first']))) && (!q['last']||text(p['legalName']).includes(text(q['last']))) &&
      (!q['birth']||p['dateOfBirth']===q['birth']) && (!q['identity']||text(p['identityNumber']).includes(text(q['identity']))));
    this.searchedSubjects=true;this.resultsOpen=true;
  }
  clearSubjectSearch() { this.subjectSearch={kind:'PATRON',card:'',number:'',first:'',last:'',birth:'',identity:''};this.searchResults=[];this.searchedSubjects=false; }
  chooseSubject(p:Row) { this.selectedSubject=p;this.txForm['patronId']=p['id'];this.searchOpen=false;this.resultsOpen=false;this.profileOpen=true;this.entrySelection=null; }
  get subjectTransactions() { return this.dayTransactions.filter(t=>t['patron'].id===this.selectedSubject?.['id']&&t['gamingDay']===this.entryDate); }
  get entrySelected() { return this.dayTransactions.find(t=>t['id']===this.entrySelection); }
  beginEditEntry() {
    const t=this.entrySelected;if(!t)return;
    this.entryEditing=t;this.txForm={...t,patronId:t['patron'].id,typeId:t['type'].id,sourceId:this.newSourceId(),nonCash:t['nonCash']||Math.max(0,t['gross']-t['cash']),reason:'Synthetic transaction correction'};this.entryTime=this.localDateTime(t['occurredAt']);this.error='';
  }
  cancelEntry() { this.startEntry(); }
  entryFile(event:any) { const file=event.target.files?.[0];if(file&&file.size>1048576){this.error='Evidence must be 1 MiB or smaller.';event.target.value='';return;}this.pendingEvidence=file||null; }
  async submitEntry() {
    if(!this.selectedSubject){this.error='Search for and select a subject first.';return;}
    const when=new Date(this.entryTime);if(!Number.isFinite(when.getTime())){this.error='Enter a valid transaction date and time.';return;}
    const cash=Number(this.txForm['cash']||0),nonCash=Number(this.txForm['nonCash']||0);
    const body={...this.txForm,patronId:this.selectedSubject['id'],occurredAt:when.toISOString(),cash:cash.toFixed(2),gross:(cash+nonCash).toFixed(2),nonCash:nonCash.toFixed(2)};
    const ok=await this.run(async()=>{
      this.txSaved=await this.api('/transactions'+(this.entryEditing?'/'+this.entryEditing['id']+'/correct':''),'POST',body);
    },'Transaction saved in this browser');
    if(ok){
      this.entryDate=this.txSaved!['gamingDay'];this.entrySelection=this.txSaved!['id'];this.entryEditing=null;
      if(this.pendingEvidence) {try{await this.store.attach(this.pendingEvidence,'Transaction',this.txSaved!['id']);}catch(e){this.error='Transaction saved, but evidence could not be saved: '+this.message(e);}}
      this.pendingEvidence=null;this.txForm={...this.txForm,sourceId:this.newSourceId(),cash:'',nonCash:'',reference:'DEMO-'+Date.now().toString().slice(-7)};
    }
  }
  async voidEntry() {const t=this.entrySelected;if(t)this.confirmAction('Transaction',t,'void');}
  get ctrRows() {
    const q=this.ctrSearch;
    return this.filtered.filter(r=>(!q['number']||('CTR-'+r['id']).toLowerCase().includes(String(q['number']).toLowerCase()))&&
      (!q['subject']||('AML'+String(r['patron'].id).padStart(10,'0')).includes(String(q['subject']).toUpperCase()))&&
      (!q['first']||String(r['patron'].legalName).toLowerCase().includes(String(q['first']).toLowerCase()))&&
      (!q['last']||String(r['patron'].legalName).toLowerCase().includes(String(q['last']).toLowerCase()))&&
      (!q['date']||r['gamingDay']===q['date'])&&(!q['state']||r['status']===q['state'])&&(!q['incomplete']||this.ctrIssues(r).length>0));
  }
  ctrIssues(r:Row) {const p=r['patron']||{};return [!p.legalName?'Missing subject name':'',!p.address?'Missing address':'',p.identityStatus!=='VERIFIED'?'Identity needs review':'',Math.max(Number(r['cashIn']||0),Number(r['cashOut']||0))<=10000?'Below demo CTR threshold':''].filter(Boolean);}
  clearCtrSearch() {this.ctrSearch={number:'',subject:'',first:'',last:'',date:'',state:'',incomplete:false};this.query='';this.filter='';}
  async markCtrVerified(r:Row,event:any) { const checked=event.target.checked;if(checked&&this.ctrIssues(r).length){event.target.checked=false;this.error=this.ctrIssues(r).join('; ');return;}
    await this.run(()=>this.api('/reports/'+r['id']+'/verify','POST',{verified:checked,reason:'Synthetic CTR review verification'}),'CTR verification updated');
  }
  async approveCtrSelection() {const targets=this.reports.filter(r=>this.selected.includes(r['id']));
    if(!targets.length)return;
    if(targets.some(r=>!r['verified']||this.ctrIssues(r).length)){this.error='Verify every selected CTR and resolve validation issues before simulated approval.';return;}
    this.open('Approve selected CTRs',[this.f('reason','Review reason','textarea',true)],{},async v=>{for(const r of targets){if(['DRAFT','RETURNED'].includes(r['status']))await this.api('/reports/'+r['id']+'/review','POST',v);await this.api('/reports/'+r['id']+'/approve','POST',v);}});
  }
  exportCtr() {const cols=['CTR number','Gaming date','Cash in','Cash out','Subject','Name','State','Verified'];const rows=this.ctrRows.map(r=>['CTR-'+r['id'],r['gamingDay'],r['cashIn'],r['cashOut'],'AML'+String(r['patron'].id).padStart(10,'0'),r['patron'].legalName,r['status'],!!r['verified']]);const cell=(v:any)=>'"'+String(v??'').replaceAll('"','""')+'"';this.download([cols,...rows].map(row=>row.map(cell).join(',')).join('\r\n'),'MOCK-CTR-review.csv','text/csv');}
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
    { id: "dashboard", label: "Home", icon: "◈", group: "WORKSPACE" },
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
    if(this.selectedSubject)this.selectedSubject=this.patrons.find(p=>p["id"]===this.selectedSubject!["id"])||null;
    this.property = await this.api("/property");
    if (this.page === "entry") { this.data = []; this.dayTransactions = await this.api("/transactions"); }
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
      this.f("subjectKind", "Subject category", "select", true, ['PATRON','ORGANIZATION','UNKNOWN'].map(x=>({value:x,label:this.label(x)}))),
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
        subjectKind: this.subjectSearch["kind"] || "PATRON",
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
