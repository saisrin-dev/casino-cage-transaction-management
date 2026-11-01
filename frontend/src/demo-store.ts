/** Browser-only portfolio simulator. No authentication, network, or production data. */
type Row = Record<string, any>;
const KEY = 'cageledger-browser-demo-v1';
export class DemoStore {
  state: Row;
  storageWarning = '';
  private storage: Pick<Storage, 'getItem' | 'setItem'> | null;
  constructor(storage: Pick<Storage, 'getItem' | 'setItem'> | null = null) {
    this.storage = storage;
    try { const saved = storage?.getItem(KEY); this.state = saved ? JSON.parse(saved) : this.seed();
      if (this.state.schema !== 1) this.state = this.seed();
    } catch { this.state = this.seed(); this.storageWarning = 'Browser storage is unavailable. Changes last for this visit only.'; }
  }
  reset() { const before = this.state; this.state = this.seed(); try { this.persist(); } catch(e) { this.state = before; throw e; } }
  persist() {
    if (!this.storage) { this.storageWarning = 'Changes last for this visit only.'; return; }
    try { this.storage.setItem(KEY, JSON.stringify(this.state)); }
    catch { throw Error('Browser storage is full or unavailable. This change was not saved. Remove evidence or reset the demo.'); }
  }
  id() { return ++this.state.seq; }
  day(value = new Date().toISOString()) {
    const d = new Date(value); if (!Number.isFinite(d.getTime())) throw Error('Enter a valid timestamp.');
    const parts = new Intl.DateTimeFormat('en-US', {timeZone:'America/New_York', year:'numeric',month:'2-digit',day:'2-digit',hour:'2-digit',hourCycle:'h23'}).formatToParts(d);
    const p = Object.fromEntries(parts.map(x => [x.type,x.value]));
    const local = new Date(`${p['year']}-${p['month']}-${p['day']}T12:00:00Z`);
    if (+p['hour'] < 6) local.setUTCDate(local.getUTCDate()-1);
    return local.toISOString().slice(0,10);
  }
  seed(): Row {
    const names = ['Avery Juniper','Jordan Vale','Casey Brook','Morgan Cedar','Riley Fern','Taylor River','Quinn Aspen','Dakota Moss','Parker Glen','Reese Willow','Rowan Sage','Skyler Pine'];
    const patrons = names.map((name,i) => ({id:i+1,version:0,legalName:name+' [Synthetic]',dateOfBirth:`198${i%10}-03-15`,address:`${100+i} Fictional Way, Demo City`,contact:`patron${i+1}@example.invalid`,country:'US',residency:'RESIDENT',occupation:'Demo profile',identityType:'DEMO_ID',identityNumber:'DEMO-'+(1000+i),identityMasked:'DEMO-••'+i,tinMasked:'DEMO-••••',identityStatus:i===4?'UNVERIFIED':'VERIFIED',w9Status:i%3===0?'REQUESTED':'VERIFIED',loyaltyId:'DEMO-'+(1000+i)}));
    const types = [['CHIP_BUY','Chip purchase','IN'],['CHIP_REDEEM','Chip redemption','OUT'],['FRONT_DEPOSIT','Front money deposit','IN'],['FRONT_WITHDRAW','Front money withdrawal','OUT'],['EXCHANGE','Currency exchange','BOTH'],['SLOT_JACKPOT','Slot jackpot','EXCLUDED'],['NONCASH','Noncash transfer','NONCASH']].map((t,i)=>({id:i+1,version:0,code:t[0],label:t[1],treatment:t[2],active:true,state:'APPROVED',createdBy:'Demo maker',approvedBy:'Demo approver'}));
    const now = new Date().toISOString();
    const amounts = [6500,4200,2800,1200,8750,3900,12000,450,3200,6700,950,2100,5800,1500,7600,2500,4800,1350];
    const transactions = amounts.map((cash,i) => {const type=types[i===1?0:i%4]; return {id:100+i,version:0,patron:patrons[i<2?0:i%names.length],type,typeLabel:type.label,treatment:type.treatment,gross:cash,cash,cashIn:type.treatment==='IN'?cash:0,cashOut:type.treatment==='OUT'?cash:0,occurredAt:now,gamingDay:this.day(now),gamingCutoff:'06:00',propertyZone:'America/New_York',sourceId:'DEMO-EVENT-'+i,reference:'DEMO-'+(2080+i),station:i%2?'VIP cage':'Main cage',status:'POSTED',cashier:'Demo visitor',notes:'Synthetic sample activity'};});
    const reports = [
      {id:201,kind:'CTR',patron:patrons[0],cashIn:16500,cashOut:0,status:'PENDING_REVIEW'},
      {id:202,kind:'SAR',patron:patrons[2],amount:8750,status:'DRAFT',category:'MINIMAL_PLAY',narrative:'Synthetic training scenario: repeated cash exchanges with minimal observed play.'},
      {id:203,kind:'TAX',patron:patrons[5],amount:12000,status:'APPROVED',formType:'W-2G',withholding:0,taxRuleVersion:'DEMO-2026'},
      {id:204,kind:'CTR',patron:patrons[6],cashIn:12000,cashOut:0,status:'DRAFT'},
      {id:205,kind:'CTR',patron:patrons[2],cashIn:10400,cashOut:0,status:'DRAFT'},
      {id:206,kind:'CTR',patron:patrons[4],cashIn:13550,cashOut:0,status:'DRAFT'}
    ].map(r=>({...r,version:0,revision:1,gamingDay:this.day(now),dueDate:new Date(Date.now()+7*86400000).toISOString().slice(0,10),creator:'Demo maker',approver:r.status==='APPROVED'?'Demo approver':null,filingType:'INITIAL',filingStatus:'NOT_SUBMITTED',externalOutcome:'NONE',obligationOpen:true,fields:'{}'}));
    return {schema:1,seq:300,patrons,types,transactions,reports,batches:[],audit:[],attachments:[],subjects:[],versions:[],tasks:[{id:250,scope:'CTR',title:'Review Avery Juniper cash activity',description:'Aggregate cash in exceeds the demo threshold.',resolved:false},{id:251,scope:'SAR',title:'Assess synthetic minimal-play referral',description:'Review the training narrative and disposition.',resolved:false}],property:{id:1,version:0,name:'Juniper Casino',zone:'America/New_York',cutoff:'06:00',contact:'demo@example.invalid',reportingScope:'Single-property browser simulation'},configurations:[{id:1,code:'GAMING_DAY',payload:JSON.stringify({zone:'America/New_York',cutoff:'06:00',ctrThreshold:10000}),state:'ACTIVE'}],users:[{id:1,displayName:'Demo visitor',username:'visitor',roles:'SANDBOX',enabled:true}]};
  }
  audit(type:string,id:number,action:string,reason:string) { this.state.audit.unshift({id:this.id(),entityType:type,entityId:id,action,reason,actor:'Demo visitor',occurredAt:new Date().toISOString(),createdAt:new Date().toISOString(),before:'',after:'Browser simulation'}); }
  cents(value:any) { if (!/^\d+(\.\d{1,2})?$/.test(String(value))) throw Error('Amounts must be nonnegative with at most two decimal places.'); const [a,b='']=String(value).split('.'); const n=Number(a)*100+Number(b.padEnd(2,'0')); if(!Number.isSafeInteger(n)||n>100000000000) throw Error('Amount is outside demo limits.'); return n; }
  aggregate() {
    const groups = new Map<string, Row>();
    for (const t of this.state.transactions) if(t.status==='POSTED') {const key=t.patron.id+':'+t.gamingDay; const g=groups.get(key)||{patron:t.patron,gamingDay:t.gamingDay,cashIn:0,cashOut:0}; g.cashIn+=Math.round(t.cashIn*100);g.cashOut+=Math.round(t.cashOut*100);groups.set(key,g);}
    for(const g of groups.values()) {
      let r=this.state.reports.find((r:Row)=>r.kind==='CTR'&&r.patron.id===g.patron.id&&r.gamingDay===g.gamingDay&&!r.snapshot&&!['SUPERSEDED','REJECTED_INTERNALLY'].includes(r.status));
      if(g.cashIn>1000000||g.cashOut>1000000) {
        if(!r) {r=this.report({kind:'CTR',patronId:g.patron.id});this.state.reports.unshift(r);}
        const changed=r.cashIn!==g.cashIn/100||r.cashOut!==g.cashOut/100;
        r.cashIn=g.cashIn/100;r.cashOut=g.cashOut/100;
        if(changed){r.status='DRAFT';r.approver=null;r.verified=false;r.revision++;}
      } else if(r) {r.cashIn=g.cashIn/100;r.cashOut=g.cashOut/100;r.status='SUPERSEDED';r.obligationOpen=false;}
    }
    for(const r of this.state.reports) if(r.kind==='CTR'&&!r.snapshot&&!groups.has(r.patron.id+':'+r.gamingDay)){r.status='SUPERSEDED';r.obligationOpen=false;}
  }
  report(body:Row):Row { const patron=this.state.patrons.find((p:Row)=>p.id===+body.patronId); if(!patron) throw Error('Select a patron.'); return {...body,id:this.id(),patron,version:0,revision:1,status:'DRAFT',gamingDay:this.day(),creator:'Demo visitor',filingType:'INITIAL',filingStatus:'NOT_SUBMITTED',externalOutcome:'NONE',obligationOpen:true,dueDate:new Date(Date.now()+15*86400000).toISOString().slice(0,10),taxRuleVersion:'DEMO-2026',fields:JSON.stringify(body),amount:Number(body.amount||body.gross||0),withholding:0}; }
  async request(path:string,method='GET',body:Row={}):Promise<any> {
    const before=JSON.stringify(this.state);
    try { const result=this.route(path,method,body); if(method!=='GET')this.persist(); return structuredClone(result??null); }
    catch(e){ this.state=JSON.parse(before); throw e; }
  }
  route(path:string,method:string,body:Row):any {
    const url=new URL(path,'https://demo.local'); const [,resource,idText,action]=url.pathname.split('/');const id=Number(idText);const s=this.state;
    const collection=s[resource]; const record=Array.isArray(collection)?collection.find((r:Row)=>r.id===id):undefined;
    const find=()=>{if(!record)throw Error('Record not found.');return record;};
    if(method==='GET') {
      switch(resource) {
        case 'demo':return {seeded:true,simulationControls:true};
        case 'me':return {username:'visitor',displayName:'Demo visitor',roles:'PUBLIC SANDBOX',permissions:['PATRON','TRANSACTION','CTR','SAR','TAX','SUPERVISE','APPROVE','FILE','SETUP','AUDIT']};
        case 'csrf':return {token:'browser-local'};
        case 'property':return s.property;
        case 'dashboard':{ const tx=s.transactions.filter((t:Row)=>t.status==='POSTED'&&t.gamingDay===this.day());return {patrons:s.patrons.length,transactions:tx.length,cashIn:tx.reduce((a:number,t:Row)=>a+Math.round(t.cashIn*100),0)/100,cashOut:tx.reduce((a:number,t:Row)=>a+Math.round(t.cashOut*100),0)/100,pending:s.reports.filter((r:Row)=>['DRAFT','PENDING_REVIEW','RETURNED'].includes(r.status)).length,awaiting:s.batches.filter((b:Row)=>b.status==='AWAITING_RESPONSE').length,rejected:s.reports.filter((r:Row)=>r.externalOutcome==='REJECTED').length,overdue:0,gamingDay:this.day(),recent:[...tx].reverse().slice(0,6),tasks:s.tasks.filter((t:Row)=>!t.resolved)};}
        case 'audit':return s.audit.filter((a:Row)=>(!url.searchParams.has('entityType')||a.entityType===url.searchParams.get('entityType'))&&(!url.searchParams.has('entityId')||a.entityId===Number(url.searchParams.get('entityId'))));
        case 'attachments':return s.attachments.filter((a:Row)=>a.ownerType===url.searchParams.get('ownerType')&&a.ownerId===Number(url.searchParams.get('ownerId')));
        case 'reports':if(action==='validation')return {errors:find().patron?[]:['Missing patron'],simulation:true,schema:'BROWSER-DEMO-1'};
          if(action==='transactions')return s.transactions.filter((t:Row)=>t.patron.id===find().patron.id&&t.gamingDay===record.gamingDay);
          if(action==='versions')return s.versions.filter((v:Row)=>v.reportId===id);
          if(action==='subjects')return s.subjects.filter((v:Row)=>v.reportId===id);
          return id?find():s.reports.filter((r:Row)=>r.kind===url.searchParams.get('kind'));
        case 'batches':if(action==='items')return find().items;return id?find():s.batches.filter((r:Row)=>r.kind===url.searchParams.get('kind'));
        default:if(Array.isArray(collection))return id?find():collection;
      }
    }
    if(resource==='logout')return null;
    if(resource==='property'){Object.assign(s.property,body);return s.property;}
    if(resource==='patrons') {
      if(action==='w9'){find().w9Status=body.status;this.audit('Patron',id,'W9',body.reason);return record;}
      if(action==='merge'){const target=s.patrons.find((p:Row)=>p.id===+body.targetId);if(!target||target.id===id)throw Error('Select a different surviving patron.');find().mergedInto=target.id;for(const t of s.transactions)if(t.patron.id===id)t.patron=target;for(const r of s.reports)if(r.patron.id===id)r.patron=target;this.aggregate();this.audit('Patron',id,'MERGE',body.reason);return record;}
      if(!body.legalName?.trim())throw Error('Legal name is required.');
      const p=id?find():{id:this.id(),version:0,w9Status:'REQUESTED'}; Object.assign(p,body,{identityMasked:'DEMO-••••',tinMasked:'DEMO-••••'});if(!id)s.patrons.unshift(p);this.audit('Patron',p.id,id?'EDIT':'CREATE',body.reason);return p;
    }
    if(resource==='transactions') {
      if(action==='void'||action==='post'){const t=find();if(action==='post'&&t.status!=='DRAFT')throw Error('Only a draft can be posted.');if(action==='void'&&!['POSTED','DRAFT'].includes(t.status))throw Error('This transaction cannot be voided.');t.status=action==='void'?'VOIDED':'POSTED';this.aggregate();this.audit('Transaction',id,action.toUpperCase(),body.reason);return t;}
      const patron=s.patrons.find((p:Row)=>p.id===+body.patronId&&!p.mergedInto),type=s.types.find((t:Row)=>t.id===+body.typeId&&t.active);
      if(!patron||!type)throw Error('Choose a patron and an active transaction type.');
      const gross=this.cents(body.gross),cash=this.cents(body.cash);if(gross<=0||cash>gross)throw Error('Gross must be positive and cash cannot exceed gross.');
      if(type.treatment==='NONCASH'&&cash>0)throw Error('A noncash transfer cannot include cash.');
      if(!body.reference?.trim()||!body.reason?.trim()||!body.station?.trim())throw Error('Reference, station, and reason are required.');
      if(s.transactions.some((t:Row)=>t.sourceId===body.sourceId))throw Error('This source event has already been recorded.');
      const day=this.day(body.occurredAt);if(new Date(body.occurredAt).getTime()>Date.now()+60000)throw Error('Event time cannot be in the future.');
      const t:Row={...body,id:this.id(),version:0,patron,type,typeLabel:type.label,treatment:type.treatment,gross:gross/100,cash:cash/100,cashIn:['IN','BOTH'].includes(type.treatment)?cash/100:0,cashOut:['OUT','BOTH'].includes(type.treatment)?cash/100:0,gamingDay:day,gamingCutoff:'06:00',propertyZone:'America/New_York',cashier:'Demo visitor',status:body.status==='DRAFT'?'DRAFT':'POSTED',conductor:s.patrons.find((p:Row)=>p.id===+body.conductorId)||null};
      if(action==='correct'){const old=find();if(old.status!=='POSTED')throw Error('Only posted transactions can be corrected.');old.status='CORRECTED';t['corrects']=old.id;}
      s.transactions.unshift(t);this.aggregate();this.audit('Transaction',t.id,action==='correct'?'CORRECT':'CREATE',body.reason);return t;
    }
    if(resource==='reports') {
      if(!id){const r=this.report(body);s.reports.unshift(r);this.audit('Report',r.id,'CREATE',body.reason);return r;}
      const r=find();
      if(action==='correction'){const copy={...r,id:this.id(),corrects:{id:r.id},snapshot:null,status:'DRAFT',revision:1,filingStatus:'NOT_SUBMITTED',externalOutcome:'NONE',approver:null};s.reports.unshift(copy);return copy;}
      if(r.snapshot)throw Error('Filed snapshots are frozen. Create a correction version.');
      if(action==='verify'){if(body.verified&&(!r.patron?.legalName||!r.patron?.address||r.patron?.identityStatus!=='VERIFIED'||Math.max(Number(r.cashIn||0),Number(r.cashOut||0))<=10000))throw Error('Resolve CTR validation issues before verification.');r.verified=!!body.verified;if(!r.verified&&r.status==='APPROVED'){r.status='DRAFT';r.approver=null;}this.audit('Report',id,'VERIFY',body.reason);return r;}
      if(action==='subjects'){const patron=s.patrons.find((p:Row)=>p.id===+body.patronId);if(!patron||patron.id===r.patron.id)throw Error('Choose an additional subject.');s.subjects.push({id:this.id(),reportId:id,patron,relationship:body.relationship});r.status='DRAFT';r.approver=null;r.verified=false;return r;}
      if(action==='approve'&&r.kind==='CTR'&&!r.verified)throw Error('Verify the CTR before simulated approval.');
      if(method==='PUT'){Object.assign(r,body,{status:'DRAFT',approver:null,verified:false,revision:r.revision+1,fields:JSON.stringify(body)});}
      else {const transitions:Row={review:['DRAFT','RETURNED'],approve:['PENDING_REVIEW'],return:['PENDING_REVIEW'],reject:['PENDING_REVIEW'],close:['PENDING_REVIEW']};if(!transitions[action]?.includes(r.status))throw Error('Action is unavailable in the current report state.');r.status=({review:'PENDING_REVIEW',approve:'APPROVED',return:'RETURNED',reject:'REJECTED_INTERNALLY',close:'CLOSED_WITHOUT_FILING'} as Row)[action];if(action==='approve')r.approver='Simulated approver';}
      s.versions.unshift({id:this.id(),reportId:id,revision:r.revision,snapshot:JSON.stringify(r)});this.audit('Report',id,action||'EDIT',body.reason);return r;
    }
    if(resource==='batches') {
      if(!id){const reports=s.reports.filter((r:Row)=>body.reportIds?.includes(r.id));if(!reports.length||reports.some((r:Row)=>r.kind!==body.kind||r.status!=='APPROVED'||r.snapshot))throw Error('Select approved, unfiled reports of the same kind.');const b={id:this.id(),kind:body.kind,version:0,status:'PREPARED',scenario:'ACCEPT',schemaVersion:'BROWSER-DEMO-1',createdAt:new Date().toISOString(),items:reports.map((r:Row)=>{r.snapshot=JSON.stringify(r);return {id:this.id(),reportId:r.id,snapshot:r.snapshot,status:'PREPARED'};})};s.batches.unshift(b);return b;}
      const b=find();if(action==='export'){if(b.status==='PREPARED')b.status='EXPORTED';return {simulation:true,...b};}
      if(action==='submit'){if(!['PREPARED','EXPORTED'].includes(b.status))throw Error('Batch has already been submitted.');b.scenario=body.scenario;b.status=body.scenario==='TIMEOUT'?'TRANSPORT_FAILED':'AWAITING_RESPONSE';b.transportReceipt='MOCK-'+b.id;}
      else if(action==='reconcile'){if(!['AWAITING_RESPONSE','TRANSPORT_FAILED'].includes(b.status))throw Error('Submit the batch first.');b.status='RECONCILED';for(const [i,item] of b.items.entries()){const outcome=b.scenario==='REJECT'||(b.scenario==='PARTIAL'&&i%2)?'REJECTED':b.scenario==='WARN'?'ACCEPTED_WITH_WARNINGS':'ACCEPTED';item.status=outcome;item.message='Simulated response only';const r=s.reports.find((r:Row)=>r.id===item.reportId);r.externalOutcome=outcome;r.filingStatus='RECONCILED';r.externalId='MOCK-'+r.id;r.obligationOpen=outcome==='REJECTED';}b.rawResponse=JSON.stringify(b.items);}
      else throw Error('Unknown batch action.');this.audit('Batch',id,action,body.reason);return b;
    }
    if(resource==='types'){if(action){find().active=action==='activate';record.state=record.active?'APPROVED':'INACTIVE';return record;}if(s.types.some((t:Row)=>t.code===body.code))throw Error('Type code must be unique.');const t={...body,id:this.id(),version:0,state:'PROPOSED',active:false,createdBy:'Demo visitor'};s.types.push(t);return t;}
    if(resource==='configurations'){JSON.parse(body.payload);Object.assign(find(),body,{state:'PROPOSED'});return record;}
    if(resource==='tasks'){find().resolved=true;this.audit('Task',id,'RESOLVE',body.reason);return record;}
    if(resource==='users')throw Error('This public demo has no user accounts. All features are available without login.');
    throw Error('This action is not available in the browser demo.');
  }
  async attach(file:File,ownerType:string,ownerId:number) {
    if(!file.size||file.size>1048576)throw Error('Choose a file between 1 byte and 1 MiB.');
    if(!['text/plain','application/pdf','image/png','image/jpeg'].includes(file.type))throw Error('Choose text, PDF, PNG, or JPEG.');
    const data=await new Promise<string>((resolve,reject)=>{const reader=new FileReader();reader.onload=()=>resolve(String(reader.result));reader.onerror=reject;reader.readAsDataURL(file);});
    const before=JSON.stringify(this.state);try{this.state.attachments.push({id:this.id(),ownerType,ownerId,filename:file.name,bytes:file.size,uploadedBy:'Demo visitor',data});this.audit(ownerType,ownerId,'ATTACH','Synthetic evidence');this.persist();}catch(e){this.state=JSON.parse(before);throw e;}
  }
}
