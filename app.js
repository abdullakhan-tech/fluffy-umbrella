const $=s=>document.querySelector(s),$$=s=>[...document.querySelectorAll(s)];
const panels=$$('.panel'),navs=$$('.nav'),titles={overview:'SYSTEM OVERVIEW',assistant:'AI ASSISTANT',persona:'PERSONA BUILDER',settingsPanel:'AI CORE SETTINGS',collaboration:'COLLABORATION NEXUS',studio:'IMAGE & VIDEO STUDIO',code:'CODE COPILOT',prompts:'PROMPT NEXUS',knowledge:'KNOWLEDGE BASE',profile:'USER PROFILE',security:'API & SECURITY',models:'AI MODEL MATRIX'};
const store={get(k,fallback){try{return JSON.parse(localStorage.getItem('zaythar:'+k))??fallback}catch{return fallback}},set(k,v){try{localStorage.setItem('zaythar:'+k,JSON.stringify(v))}catch(e){console.warn('Local storage unavailable',e)}}};
function openPanel(id){panels.forEach(p=>p.classList.toggle('active-panel',p.id===id));navs.forEach(n=>n.classList.toggle('active',n.dataset.panel===id));$('#pageTitle').innerHTML=(titles[id]||id).replace(/ (.*)/,' <em>$1</em>');window.scrollTo({top:0,behavior:'smooth'})}
navs.forEach(n=>n.addEventListener('click',()=>openPanel(n.dataset.panel)));$$('[data-panel]').filter(x=>!x.classList.contains('nav')).forEach(x=>x.addEventListener('click',()=>openPanel(x.dataset.panel)));
function tick(){const d=new Date();$('#clock').textContent=d.toLocaleTimeString([], {hour12:false})}tick();setInterval(tick,1000);
function savePrefs(){store.set('preferences',{theme:document.body.classList.contains('light'),accent:getComputedStyle(document.documentElement).getPropertyValue('--accent').trim(),ranges:$$('input[type=range]').map(x=>x.value),checks:$$('input[type=checkbox]').map(x=>x.checked),persona:$('.persona-display')?$('#persona select')?.value:null})}
function restorePrefs(){const p=store.get('preferences',{});if(p.theme)document.body.classList.add('light');if(p.accent)document.documentElement.style.setProperty('--accent',p.accent);$$('input[type=range]').forEach((x,i)=>{if(p.ranges?.[i]!=null)x.value=p.ranges[i];updateRange(x)});$$('input[type=checkbox]').forEach((x,i)=>{if(p.checks?.[i]!=null)x.checked=p.checks[i]})}
function updateRange(r){const o=r.parentElement.querySelector('output');if(o)o.value=r.value+'%'}
restorePrefs();
$('#themeBtn').onclick=()=>{document.body.classList.toggle('light');savePrefs()};
$$('input[type=range]').forEach(r=>r.addEventListener('input',()=>updateRange(r)));
$$('input[type=checkbox]').forEach(x=>x.addEventListener('change',savePrefs));
$$('[data-accent]').forEach(b=>b.onclick=()=>{document.documentElement.style.setProperty('--accent',b.dataset.accent);savePrefs()});
$('#saveSettings').onclick=()=>{savePrefs();toast('Preferences saved on this device.')};
$('#initAvatar').onclick=()=>{savePrefs();toast('Persona preferences saved and will be sent with future chats.')};
function toast(t){let el=$('#toast');if(!el){el=document.createElement('div');el.id='toast';el.className='toast';document.body.append(el)}el.textContent=t;el.classList.add('show');clearTimeout(window.__toastTimer);window.__toastTimer=setTimeout(()=>el.classList.remove('show'),2600)}
const messages=$('#messages'),promptBox=$('#prompt');let busy=false,activeAgent='general';
function getMemorySession(){let id=store.get('memorySession',null);if(!id){id=window.crypto?.randomUUID?.()||('zy-'+Math.random().toString(36).slice(2)+Date.now().toString(36));store.set('memorySession',id)}return id}const memorySession=getMemorySession();
function addMessage(role,text,save=true){const el=document.createElement('div');el.className='message '+role;el.textContent=text;messages.appendChild(el);messages.scrollTop=messages.scrollHeight;if(save)persistChat();return el}
function persistChat(){store.set('chat',[...messages.querySelectorAll('.message')].map(x=>({role:x.classList.contains('user')?'user':'assistant',text:x.textContent})).slice(-100))}
function loadChat(){const history=store.get('chat',[]);if(!history.length)return;const empty=messages.querySelector('.empty-chat');if(empty)empty.remove();history.forEach(m=>addMessage(m.role,m.text,false))}
loadChat();
function localReply(q){if(/^(hi|hello|hey|salam|السلام)/i.test(q))return 'Salam! I am Zaythar AI. The backend is currently unavailable, so this is a limited local response.';return 'I could not reach the AI backend. Check that the FastAPI server is running and OPENAI_API_KEY is configured on the server.'}
function currentPersona(){const ranges=$$('#persona input[type=range]').map(r=>Number(r.value));return {empathy:ranges[0]??70,cognitive_capacity:ranges[1]??70,adaptability:ranges[2]??70,creativity:ranges[3]??70,style:$('#persona select')?.value||'Balanced & helpful'}}
async function send(q){q=q.trim();if(!q||busy)return;const empty=messages.querySelector('.empty-chat');if(empty)empty.remove();addMessage('user',q);promptBox.value='';busy=true;const pending=addMessage('assistant','PROCESSING REQUEST…');try{const r=await fetch('/api/chat',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({message:q,language:$('#chatLanguage').value,persona:currentPersona(),settings:store.get('preferences',{}),agent:activeAgent,provider:store.get('aiProvider','hybrid'),session_id:memorySession,history:store.get('chat',[]).slice(-16).map(m=>({role:m.role,content:m.text}))})});const data=await r.json().catch(()=>({}));if(!r.ok)throw new Error(data.detail||'Backend unavailable');pending.textContent=data.reply||'No response received.';persistChat();if($('#speakToggle')?.checked)speak(pending.textContent)}catch(e){pending.textContent=localReply(q);persistChat();toast(e.message||'AI service unavailable')}finally{busy=false;messages.scrollTop=messages.scrollHeight}}
$('#chatForm').addEventListener('submit',e=>{e.preventDefault();send(promptBox.value)});$$('.suggestion').forEach(b=>b.onclick=()=>send(b.textContent));
let recognition;const SR=window.SpeechRecognition||window.webkitSpeechRecognition;if(SR){recognition=new SR();recognition.continuous=false;recognition.interimResults=false;recognition.onresult=e=>send(e.results[0][0].transcript);recognition.onend=()=>$('#micBtn').textContent='🎙 VOICE';recognition.onerror=()=>{$('#micBtn').textContent='🎙 VOICE';toast('Microphone unavailable. Check browser permission.')}} 
$('#micBtn').onclick=()=>{if(!recognition){toast('Voice input is not supported in this browser. Try Chrome over HTTPS.');return}recognition.lang=$('#chatLanguage').value;$('#micBtn').textContent='LISTENING…';try{recognition.start()}catch(e){toast('Voice capture is already active.')}};
function speak(t){if(!('speechSynthesis'in window)){toast('Speech output is not supported in this browser.');return}speechSynthesis.cancel();const u=new SpeechSynthesisUtterance(t);u.lang=$('#chatLanguage').value;speechSynthesis.speak(u)}
$('#speakToggle')?.addEventListener('change',e=>{store.set('speakReplies',e.target.checked);if(!e.target.checked&&'speechSynthesis'in window)speechSynthesis.cancel()});if($('#speakToggle'))$('#speakToggle').checked=store.get('speakReplies',false);
$('#clearChat')?.addEventListener('click',()=>{messages.innerHTML='<div class="empty-chat">Your conversation will appear here.</div>';store.set('chat',[]);if('speechSynthesis'in window)speechSynthesis.cancel();toast('Conversation cleared.')});
$('#makeBrief').onclick=()=>{const p=$('#mediaPrompt').value.trim();$('#mediaResult').textContent=p?'Creative brief prepared: '+p+' — Style: '+document.querySelectorAll('.studio-controls select')[1].value+'. Connect an image/video provider to generate media.':'Enter a prompt first.'};
$('#copyCode').onclick=async()=>{const t=$('pre code').textContent;try{await navigator.clipboard.writeText(t);$('#copyCode').textContent='COPIED'}catch(e){$('#copyCode').textContent='COPY BLOCKED'}};
$$('.prompt-card').forEach(b=>b.onclick=()=>{openPanel('assistant');promptBox.value=b.dataset.prompt;promptBox.focus()});
$('#promptSearch').addEventListener('input',e=>$$('.prompt-card').forEach(b=>b.style.display=b.textContent.toLowerCase().includes(e.target.value.toLowerCase())?'block':'none'));
function renderNotes(){const list=$('#notesList');list.innerHTML='';store.get('notes',[]).forEach((note,i)=>{const row=document.createElement('div');row.className='note-item';const content=document.createElement('span');content.textContent=note.text;const del=document.createElement('button');del.type='button';del.className='note-delete';del.textContent='DELETE';del.setAttribute('aria-label','Delete note');del.onclick=()=>{const notes=store.get('notes',[]);notes.splice(i,1);store.set('notes',notes);renderNotes()};row.append(content,del);list.append(row)})}
$('#saveNote').onclick=async()=>{const v=$('#knowledgeNote').value.trim();if(!v)return;const notes=store.get('notes',[]);notes.unshift({text:v,created:new Date().toISOString()});store.set('notes',notes.slice(0,200));$('#knowledgeNote').value='';renderNotes();try{const r=await fetch('/api/memory',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({session_id:memorySession,content:v})});if(!r.ok)throw new Error('Memory sync unavailable');toast('Note saved locally and to Zaythar long-term memory.')}catch(e){toast('Note saved on this device; server memory sync unavailable.')}};renderNotes();
async function health(){try{const r=await fetch('/api/health',{cache:'no-store'});const d=await r.json();const ok=r.ok&&d.status==='ok';$('#apiStatus').textContent=ok?(d.ai_configured?'AI READY':'BACKEND ONLINE · KEY NEEDED'):'OFFLINE';$('#apiStatus').style.color=ok?'var(--green)':'var(--accent2)';$('#securityHealth').textContent=ok?(d.ai_configured?'ONLINE · AI CONFIGURED':'ONLINE · AI NOT CONFIGURED'):'OFFLINE';
  if(ok){
    const setProvider=(name,ready)=>{const label=$('#'+name+'State'),bar=$('#'+name+'Bar');if(label)label.textContent=ready?'READY':'NO KEY';if(bar)bar.style.width=ready?'100%':'0%';};
    setProvider('openai',!!d.providers?.openai);setProvider('gemini',!!d.providers?.gemini);
    const seconds=Number(d.uptime_seconds)||0;
    if($('#uptimeState'))$('#uptimeState').textContent=seconds<60?seconds+'s':Math.floor(seconds/60)+'m';
    if($('#uptimeBar'))$('#uptimeBar').style.width=Math.min(100,seconds/3600*100)+'%';
  }}catch(e){$('#apiStatus').textContent='OFFLINE';$('#apiStatus').style.color='var(--accent2)';$('#securityHealth').textContent='OFFLINE'}}health();
document.addEventListener('keydown',e=>{if((e.ctrlKey||e.metaKey)&&e.key==='/'){e.preventDefault();openPanel('assistant');promptBox.focus()}});


// Holographic interface controls: local visual enhancements, not hardware telemetry.
const motionButton=$('#motionBtn');
if(motionButton){
  const enabled=store.get('motion3d',true);
  document.body.classList.toggle('motion-off',!enabled);
  motionButton.setAttribute('aria-pressed',String(enabled));
  motionButton.textContent=enabled?'3D FX ON':'3D FX OFF';
  motionButton.addEventListener('click',()=>{
    const next=!document.body.classList.contains('motion-off');
    document.body.classList.toggle('motion-off',!next);
    store.set('motion3d',next);
    motionButton.setAttribute('aria-pressed',String(next));
    motionButton.textContent=next?'3D FX ON':'3D FX OFF';
    toast(next?'Holographic effects enabled.':'Motion effects paused.');
  });
}
if(matchMedia('(hover:hover) and (pointer:fine)').matches){
  $$('.card,.module,.mini-stat,.plan-card').forEach(el=>{
    el.addEventListener('pointermove',e=>{
      if(document.body.classList.contains('motion-off'))return;
      const r=el.getBoundingClientRect(),x=(e.clientX-r.left)/r.width,y=(e.clientY-r.top)/r.height;
      el.style.transform='perspective(950px) rotateX('+((.5-y)*4)+'deg) rotateY('+((x-.5)*5)+'deg) translateZ(5px)';
    });
    el.addEventListener('pointerleave',()=>{el.style.transform=''});
  });
}
$$('input[type=range]').forEach((range,index)=>{
  range.addEventListener('input',()=>{
    updateRange(range);
    const values=store.get('rangeValues',{});
    values[index]=Number(range.value);
    store.set('rangeValues',values);
  });
});

$$('[data-agent]').forEach(agent=>{
  agent.addEventListener('click',()=>{
    activeAgent=agent.dataset.agent;
    openPanel('assistant');
    const names={general:'General AI',research:'Research specialist',coding:'Coding specialist',writing:'Writing specialist',analysis:'Analysis specialist',creative:'Creative specialist'};
    toast((names[activeAgent]||'AI specialist')+' selected. Describe your task.');
    promptBox.placeholder='Message '+(names[activeAgent]||'Zaythar AI')+'...';
    promptBox.focus();
  });
});


// Model Selection Matrix: selects a configured backend route; it does not provision provider access.
const providerNames={openai:'OPENAI',gemini:'GEMINI',hybrid:'HYBRID CORE'};
function renderProvider(){
  const chosen=store.get('aiProvider','hybrid');
  $$('[data-provider]').forEach(card=>{
    const active=card.dataset.provider===chosen;
    card.classList.toggle('selected',active);
    const label=card.querySelector('i');
    if(label)label.textContent=active?'ACTIVE ROUTE':'SELECT PROVIDER';
  });
  const status=$('#providerStatus');
  if(status)status.textContent=providerNames[chosen]||'HYBRID CORE';
}
$$('[data-provider]').forEach(card=>card.addEventListener('click',()=>{
  store.set('aiProvider',card.dataset.provider);
  renderProvider();
  toast((providerNames[card.dataset.provider]||'Provider')+' route selected for future chat requests.');
}));
renderProvider();
$('#openAssistantFromModels')?.addEventListener('click',()=>openPanel('assistant'));

// Profile name is a local display preference only; no account identity is implied.
const nameInput=$('#operatorName');
if(nameInput){
  nameInput.value=store.get('operatorName','OPERATOR 01');
  $('#saveProfile')?.addEventListener('click',()=>{
    const name=nameInput.value.trim().slice(0,32)||'OPERATOR 01';
    nameInput.value=name;
    store.set('operatorName',name);
    const heading=$('#profileHeading');
    if(heading)heading.textContent=name.toUpperCase();
    toast('Local operator profile saved.');
  });
  const savedName=store.get('operatorName','OPERATOR 01');
  if($('#profileHeading'))$('#profileHeading').textContent=savedName.toUpperCase();
}

// Extra visual controls map to CSS variables; these are visual preferences, not performance controls.
function applyVisualControls(){
  const prefs=store.get('visualControls',{glow:72,metal:65,contrast:80});
  document.documentElement.style.setProperty('--glow-strength',String(prefs.glow/100));
  document.documentElement.style.setProperty('--metal-strength',String(prefs.metal/100));
  document.documentElement.style.setProperty('--contrast-strength',String(prefs.contrast/100));
  $$('[data-visual]').forEach(input=>{
    const key=input.dataset.visual;
    input.value=prefs[key]??Number(input.value);
    updateRange(input);
    input.addEventListener('input',()=>{
      const next=store.get('visualControls',{glow:72,metal:65,contrast:80});
      next[key]=Number(input.value);
      store.set('visualControls',next);
      document.documentElement.style.setProperty('--'+(key==='glow'?'glow-strength':key==='metal'?'metal-strength':'contrast-strength'),String(Number(input.value)/100));
    });
  });
}
applyVisualControls();


// Interactive neural-field renderer: deterministic 3D projection, pointer parallax,
// responsive DPR scaling, and reduced-motion / visibility handling.
(function initNeuralField(){
  const canvases=[$('#neuralCanvas'),$('.neuralCanvasMini')].filter(Boolean);
  if(!canvases.length)return;
  const reduced=window.matchMedia('(prefers-reduced-motion: reduce)');
  const nodes=Array.from({length:76},(_,i)=>{
    const y=1-(i/(75))*2;
    const radius=Math.sqrt(Math.max(0,1-y*y));
    const angle=i*2.399963229728653;
    return {x:Math.cos(angle)*radius,y,z:Math.sin(angle)*radius,phase:i*0.73};
  });
  const pointer={x:0,y:0,targetX:0,targetY:0};
  let frame=0,angle=0,raf=0,active=true;
  const palette=()=>getComputedStyle(document.documentElement).getPropertyValue('--accent').trim()||'#ff3155';
  function render(canvas,now){
    const rect=canvas.getBoundingClientRect();if(!rect.width||!rect.height)return;
    const dpr=Math.min(window.devicePixelRatio||1,2),w=Math.round(rect.width*dpr),h=Math.round(rect.height*dpr);
    if(canvas.width!==w||canvas.height!==h){canvas.width=w;canvas.height=h}
    const ctx=canvas.getContext('2d');if(!ctx)return;
    ctx.setTransform(dpr,0,0,dpr,0,0);ctx.clearRect(0,0,rect.width,rect.height);
    const cx=rect.width/2,cy=rect.height/2,scale=Math.min(rect.width,rect.height)*.39;
    const spin=angle+(now*.00012);
    const cos=Math.cos(spin),sin=Math.sin(spin),tiltX=pointer.targetY*.22,tiltY=pointer.targetX*.22;
    const projected=nodes.map(n=>{
      let x=n.x*cos-n.z*sin,z=n.x*sin+n.z*cos,y=n.y;
      const yy=y*Math.cos(tiltX)-z*Math.sin(tiltX),zz=y*Math.sin(tiltX)+z*Math.cos(tiltX);
      const depth=2.7+zz*.55, perspective=2.7/depth;
      return {x:cx+x*scale*perspective,y:cy+yy*scale*perspective,z:zz,alpha:.28+(.72*(zz+1)/2),r:1.1+((zz+1)/2)*1.5};
    });
    const accent=palette();
    ctx.lineWidth=.65;
    for(let i=0;i<projected.length;i++){
      for(let j=i+1;j<projected.length;j++){
        const a=projected[i],b=projected[j],dx=a.x-b.x,dy=a.y-b.y,dist=Math.hypot(dx,dy);
        if(dist<rect.width*.105){
          ctx.strokeStyle=accent;ctx.globalAlpha=(1-dist/(rect.width*.105))*.24*Math.min(a.alpha,b.alpha);
          ctx.beginPath();ctx.moveTo(a.x,a.y);ctx.lineTo(b.x,b.y);ctx.stroke();
        }
      }
    }
    ctx.globalAlpha=1;
    projected.forEach((p,i)=>{
      const pulse=.65+.35*Math.sin(now*.002+nodes[i].phase);
      ctx.fillStyle=accent;ctx.globalAlpha=p.alpha*pulse;
      ctx.shadowBlur=8*pulse;ctx.shadowColor=accent;
      ctx.beginPath();ctx.arc(p.x,p.y,p.r*pulse,0,Math.PI*2);ctx.fill();
    });
    ctx.shadowBlur=0;ctx.globalAlpha=1;
    ctx.strokeStyle=accent;ctx.globalAlpha=.34;ctx.lineWidth=1;
    [0.82,1.02,1.2].forEach((r,k)=>{ctx.beginPath();ctx.ellipse(cx,cy,scale*r,scale*(.22+k*.025),spin*.25+k*.7,0,Math.PI*2);ctx.stroke()});
    ctx.globalAlpha=1;
  }
  function loop(now){
    if(!active)return;
    canvases.forEach(canvas=>render(canvas,now));
    if(!document.body.classList.contains('motion-off')&&!reduced.matches)raf=requestAnimationFrame(loop);
  }
  function start(){if(!active)return;cancelAnimationFrame(raf);raf=requestAnimationFrame(loop);if($('#neuralState'))$('#neuralState').textContent='SYNAPTIC FIELD ONLINE';if($('#rendererState'))$('#rendererState').textContent='ACTIVE';if($('#rendererBar'))$('#rendererBar').style.width='100%'}
  function stop(){cancelAnimationFrame(raf);canvases.forEach(canvas=>render(canvas,performance.now()))}
  const stage=$('.neural-stage');
  stage?.addEventListener('pointermove',e=>{const r=stage.getBoundingClientRect();pointer.targetX=((e.clientX-r.left)/r.width-.5)*2;pointer.targetY=((e.clientY-r.top)/r.height-.5)*2});
  stage?.addEventListener('pointerleave',()=>{pointer.targetX=0;pointer.targetY=0});
  document.addEventListener('visibilitychange',()=>{if(document.hidden)stop();else start()});
  const motionObserver=new MutationObserver(()=>{if(document.body.classList.contains('motion-off')||reduced.matches)stop();else start()});
  motionObserver.observe(document.body,{attributes:true,attributeFilter:['class']});
  reduced.addEventListener?.('change',()=>reduced.matches?stop():start());
  window.addEventListener('resize',()=>canvases.forEach(canvas=>render(canvas,performance.now())),{passive:true});
  if(matchMedia('(hover:hover) and (pointer:fine)').matches)start();else start();
})();


// Real multi-agent team runner: specialists execute concurrently on the backend;
// the browser only renders returned deliverables and never handles provider secrets.
(function initAgentOrchestrator(){
  const runButton=$('#runAgentTeam'),taskInput=$('#agentTask'),status=$('#agentRunStatus'),resultBox=$('#agentRunResult');
  if(!runButton||!taskInput||!status||!resultBox)return;
  runButton.addEventListener('click',async()=>{
    const task=taskInput.value.trim();
    const agents=$$('input[name="teamAgent"]:checked').map(input=>input.value);
    if(!task){toast('Pehle team ke liye task likhein.');taskInput.focus();return}
    if(!agents.length||agents.length>4){toast('1 se 4 specialists select karein.');return}
    runButton.disabled=true;runButton.textContent='TEAM PROCESSING…';
    status.textContent='Specialists are working in parallel. Waiting for coordinator synthesis…';
    resultBox.replaceChildren();
    try{
      const response=await fetch('/api/agents/run',{
        method:'POST',headers:{'Content-Type':'application/json'},
        body:JSON.stringify({
          task,agents,provider:store.get('aiProvider','hybrid'),session_id:memorySession,
          language:({'en-US':'English','ur-PK':'Urdu','ar-SA':'Arabic','hi-IN':'Hindi','fr-FR':'French','es-ES':'Spanish','de-DE':'German','zh-CN':'Chinese','ja-JP':'Japanese'})[$('#chatLanguage')?.value]||'English'
        })
      });
      const data=await response.json().catch(()=>({}));
      if(!response.ok)throw new Error(data.detail||'Agent service unavailable');
      status.textContent='Completed '+data.completed_agents+' of '+data.total_agents+' specialists · coordinator: '+(data.provider||'AI');
      const heading=document.createElement('h3');heading.textContent='COORDINATED RESULT';resultBox.append(heading);
      const answer=document.createElement('div');answer.className='agent-final';answer.textContent=data.reply||'Coordinator returned no final response.';resultBox.append(answer);
      const subheading=document.createElement('h3');subheading.textContent='SPECIALIST DELIVERABLES';resultBox.append(subheading);
      (data.agents||[]).forEach(item=>{
        const card=document.createElement('article');card.className='agent-result-card';
        const title=document.createElement('b');title.textContent=(item.agent||'AGENT').toUpperCase()+' · '+(item.status||'UNKNOWN').toUpperCase();
        const body=document.createElement('p');body.textContent=item.status==='completed'?item.output:(item.error||'Specialist failed.');
        card.append(title,body);resultBox.append(card);
      });
      if($('#messages')){addMessage('assistant','[MULTI-AGENT RESULT]\n'+(data.reply||''));}
    }catch(error){status.textContent='Team run failed. Check backend and provider configuration.';const detail=document.createElement('p');detail.textContent=error.message||'Unknown orchestration error';resultBox.append(detail);toast('Agent team could not complete the task.')}
    finally{runButton.disabled=false;runButton.textContent='INITIALIZE AGENT TEAM ↗'}
  });
})();


// Session-scoped shared knowledge graph controls.
(function initKnowledgeGraph(){
  const subject=$('#factSubject'),predicate=$('#factPredicate'),object=$('#factObject');
  const save=$('#saveKnowledgeFact'),list=$('#knowledgeFactList');
  if(!subject||!predicate||!object||!save||!list)return;
  async function refresh(){
    try{
      const response=await fetch('/api/knowledge?session_id='+encodeURIComponent(memorySession));
      if(!response.ok)throw new Error('Knowledge service unavailable');
      const data=await response.json();list.replaceChildren();
      (data.facts||[]).forEach(fact=>{
        const row=document.createElement('div');row.className='knowledge-fact';
        const label=document.createElement('span');label.textContent=fact.subject+' · '+fact.predicate+' · '+fact.object;
        const del=document.createElement('button');del.type='button';del.textContent='DELETE';del.setAttribute('aria-label','Delete knowledge fact');
        del.addEventListener('click',async()=>{
          const response=await fetch('/api/knowledge/'+fact.id+'?session_id='+encodeURIComponent(memorySession),{method:'DELETE'});
          if(response.ok)refresh();else toast('Fact delete failed.');
        });
        row.append(label,del);list.append(row);
      });
    }catch(error){list.textContent='Knowledge graph unavailable until the backend is online.'}
  }
  save.addEventListener('click',async()=>{
    const values={subject:subject.value.trim(),predicate:predicate.value.trim(),object:object.value.trim()};
    if(!values.subject||!values.predicate||!values.object){toast('Subject, relation aur object teeno fill karein.');return}
    save.disabled=true;
    try{
      const response=await fetch('/api/knowledge',{method:'POST',headers:{'Content-Type':'application/json'},body:JSON.stringify({...values,session_id:memorySession,source:'operator'})});
      const data=await response.json().catch(()=>({}));
      if(!response.ok)throw new Error(data.detail||'Could not save fact');
      subject.value='';predicate.value='';object.value='';await refresh();toast('Knowledge fact saved.');
    }catch(error){toast(error.message||'Knowledge save failed.')}
    finally{save.disabled=false}
  });
  refresh();
})();
