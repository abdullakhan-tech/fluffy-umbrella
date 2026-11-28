(()=>{
const API=(window.OFFICE_API||'').replace(/\/$/,'');if(!API)return;
const $=(t,p,x)=>{const e=document.createElement(t);if(x)e.textContent=x;if(p)p.appendChild(e);return e};
const key=()=>{let k=localStorage.getItem('office_key');if(!k){k=prompt('Office server key (OFFICE_KEY):')||'';localStorage.setItem('office_key',k)}return k};
const call=async(p,o)=>{const r=await fetch(API+p,{...o,headers:{'content-type':'application/json','x-office-key':key()}});const j=await r.json().catch(()=>({}));if(!r.ok)throw Error(j.error||r.status);return j};
window.webSearch=async q=>{try{const r=await call('/api/search?q='+encodeURIComponent(q.slice(0,200)));const t=(r.results||[]).slice(0,4).map(x=>'- '+x.title+': '+x.snippet+' ('+x.url+')').join('\n');return t?'Internet se taza natayej (source ke saath):\n'+t:''}catch(e){return ''}};
window.socialQueue=async text=>{try{await call('/api/social/queue',{method:'POST',body:JSON.stringify({text})});load()}catch(e){}};
const box=$('div',document.body);
box.style.cssText='position:fixed;top:calc(env(safe-area-inset-top,0px) + 8px);left:8px;right:8px;max-height:60vh;overflow:auto;background:var(--ui);color:var(--tx);border:1px solid #8886;border-radius:12px;padding:10px;display:none;z-index:9;font-size:14px;white-space:pre-wrap';
const bar=$('div',box);bar.style.cssText='display:flex;gap:6px;margin-bottom:8px';
const inp=$('input',bar);inp.placeholder='Search ya https:// URL';const go=$('button',bar,'Jao');
const out=$('div',box),qd=$('div',box);qd.style.marginTop='10px';
const tgl=document.createElement('b');tgl.textContent='🌐 Browser';
tgl.onclick=()=>{box.style.display=box.style.display==='none'?'block':'none';load()};
document.getElementById('chips').prepend(tgl);
go.onclick=async()=>{out.textContent='Dhoond raha hoon…';try{const v=inp.value.trim();
 if(/^https?:\/\//.test(v)){out.textContent=(await call('/api/fetch?url='+encodeURIComponent(v))).text}
 else{out.textContent=(await call('/api/search?q='+encodeURIComponent(v))).results.map(x=>x.title+'\n'+x.url+'\n'+x.snippet).join('\n\n')}}
 catch(e){out.textContent='Error: '+e.message}};
async function load(){try{const r=await call('/api/social/queue');qd.textContent='📣 Social queue (aap ki manzoori ke baghair post nahi hogi):';
 (r.items||[]).filter(i=>i.status==='pending').forEach(i=>{const d=$('div',qd);d.style.cssText='margin:8px 0;padding:8px;border:1px solid #8886;border-radius:8px';$('div',d,i.text);
  const a=$('button',d,'✅ Post karo');a.onclick=async()=>{a.disabled=true;try{const x=await call('/api/social/post',{method:'POST',body:JSON.stringify({id:i.id})});a.textContent='Posted: '+x.sent.join(', ')}catch(e){a.textContent='Error: '+e.message}}})}
 catch(e){qd.textContent='Server se rabta nahi: '+e.message}}
let mt=null;window.memSync=m=>{clearTimeout(mt);mt=setTimeout(()=>call('/api/memory',{method:'POST',body:JSON.stringify(m)}).catch(()=>{}),1500)};
window.notifyBoss=t=>{call('/api/notify',{method:'POST',body:JSON.stringify({text:'👑 Zaythar AI: '+t})}).catch(()=>{})};
setTimeout(()=>call('/api/memory').then(m=>{for(const k in (m.memory||{})){const a=LS[k],c=m.memory[k];if(!a||(c.xp||0)>(a.xp||0))LS[k]=c}AG.forEach(tag)}).catch(()=>{}),1500);
const jr=$('div',box);jr.style.cssText='display:flex;gap:6px;margin:10px 0 6px';
const jin=$('input',jr);jin.placeholder='Off-screen kaam (agents server par karenge)';const jb=$('button',jr,'Bhejo');const jo=$('div',box);let memView=false;
const mb=$('button',jr,'🧠');mb.title='Agents ki yaaddasht (dekhein/mitayein)';
mb.onclick=async()=>{memView=true;try{const m=(await call('/api/memory')).memory;jo.textContent='';Object.entries(m).forEach(([n,v])=>['facts','lessons'].forEach(k=>(v[k]||[]).forEach((x,i)=>{const d=$('div',jo,n+' · '+k+': '+x+'  ');const c=$('button',d,'✖');c.onclick=async()=>{await call('/api/memory/delete',{method:'POST',body:JSON.stringify({agent:n,kind:k,index:i})});mb.onclick()}})))}catch(e){jo.textContent='Error: '+e.message}};
jb.onclick=async()=>{memView=false;try{await call('/api/task',{method:'POST',body:JSON.stringify({text:jin.value})});jin.value='';poll()}catch(e){jo.textContent='Error: '+e.message}};
async function poll(){if(memView)return;try{const r=await call('/api/jobs'),j=(r.jobs||[]).slice(-1)[0];
 jo.textContent=j?'🛠 '+j.status+' · '+j.text+'\n'+(j.steps||[]).join('\n')+(j.final?'\n\n👑 '+j.final:''):'Koi job nahi.';
 const a=await call('/api/approvals');(a.items||[]).filter(i=>i.status==='pending').forEach(i=>{const d=$('div',jo);d.style.cssText='margin:8px 0;padding:8px;border:1px solid #8886;border-radius:8px';
  $('div',d,'🔧 '+i.by+' ka patch: '+i.title+' ('+i.data.files.map(f=>f.path).join(', ')+')');
  [['✅ PR banao',1],['✖ Radd',0]].forEach(([n,v])=>{const x=$('button',d,n);x.style.marginRight='6px';x.onclick=async()=>{x.disabled=true;try{const z=await call('/api/approvals',{method:'POST',body:JSON.stringify({id:i.id,approve:!!v})});x.textContent=z.url||z.error||'Ho gaya'}catch(e){x.textContent='Error: '+e.message}}})})}catch(e){}}
setInterval(()=>{if(box.style.display!=='none')poll()},4000);
const _cmd=window.cmd;
window.cmd=async(txt,local)=>{const m=/^\s*(jarvis|friday|omnivex|rio|carbon|neon|argon)[\s,:]+(.+)/i.exec(txt||'');
 if(m&&!local){const w=AG.find(x=>x.name===m[1].toLowerCase()),rp=document.getElementById('rp');
  try{say(w,'🤔');const r=await call('/api/chat',{method:'POST',body:JSON.stringify({agent:w.name,text:m[2]})});say(w,r.reply.slice(0,46));
   rp.style.cssText='display:block;max-height:38vh;white-space:pre-wrap';rp.textContent=cap(w.name)+': '+r.reply;speakBoss(r.reply);return}catch(e){w.bb.visible=false}}
 return _cmd(txt,local)};
let seeded=false,lastDone=null;
async function sync(){try{const r=await call('/api/jobs'),j=(r.jobs||[]).slice(-1)[0];window.srvBusy=!!(j&&j.status==='running');
 if(!seeded){seeded=true;if(j&&j.status!=='running')lastDone=j.id}
 else if(j&&j.status==='done'&&j.id!==lastDone){lastDone=j.id;say(BOSS,String(j.final).slice(0,46));AG.forEach(w=>{if(w.seated)act(w,'wave')});speakBoss(j.final)}}catch(e){}}
async function stat(){try{const s=await call('/api/status');drawStatus(['AI '+(s.llm?'✅':'❌')+'  GitHub '+(s.github?'✅':'❌')+'  Telegram '+(s.telegram?'✅':'❌'),'Approvals pending: '+s.pending+(s.running?'  · job chal raha hai':''),'— CI —',...String(s.checks||'-').split('\n').slice(0,4),'— Issues —',...String(s.issues||'-').split('\n').slice(0,3)])}catch(e){}}
setInterval(sync,5000);setInterval(stat,60000);sync();stat();
})();
