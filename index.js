// Zaythar Office - Agent Browser server (Node 18+, no dependencies).
// Safe by design: key-protected, SSRF-blocked, rate-limited, social posts need human approval.
import http from 'node:http';
import dns from 'node:dns/promises';
import net from 'node:net';
import { timingSafeEqual } from 'node:crypto';
import fs from 'node:fs';
import * as gh from './lib/github.js';
import { llmReady } from './lib/llm.js';
import { agentStore } from './lib/store.js';
import { ROSTER, runJob, chat } from './agents.js';

const { PORT = 8787, OFFICE_KEY, ALLOW_ORIGIN = '*' } = process.env;
if (!OFFICE_KEY || OFFICE_KEY.length < 16) { console.error('Set OFFICE_KEY (16+ chars) in .env'); process.exit(1); }
const E = process.env, J = { 'content-type': 'application/json' };
const queue = [], hits = new Map(); let seq = 0;
setInterval(() => hits.clear(), 60000);
const DIR = new URL('./data/', import.meta.url), FILE = new URL('./data/state.json', import.meta.url);
let state = { memory: {}, tasks: [], approvals: [], jobs: [], audit: [] };
try { state = { ...state, ...JSON.parse(fs.readFileSync(FILE, 'utf8')) }; } catch {}
const save = () => { try { fs.mkdirSync(DIR, { recursive: true }); fs.writeFileSync(FILE, JSON.stringify(state)); } catch {} };
const AGENTS = Object.keys(ROSTER);

const isPriv = ip => net.isIPv4(ip)
  ? /^(0\.|10\.|127\.|169\.254\.|192\.168\.|172\.(1[6-9]|2\d|3[01])\.)/.test(ip)
  : /^(::1?$|f[cd]|fe80)/i.test(ip);
async function get(u, n = 0) {
  const url = new URL(u);
  if (!/^https?:$/.test(url.protocol)) throw Error('only http/https');
  const { address } = await dns.lookup(url.hostname);
  if (isPriv(address)) throw Error('blocked address');
  const r = await fetch(url, { redirect: 'manual', signal: AbortSignal.timeout(10000), headers: { 'user-agent': 'ZaytharOffice/1.0' } });
  const loc = r.headers.get('location');
  if (r.status >= 300 && r.status < 400 && loc && n < 3) return get(new URL(loc, url).href, n + 1);
  return r;
}
const text = h => h.replace(/<(script|style)[\s\S]*?<\/\1>/gi, '').replace(/<[^>]+>/g, ' ')
  .replace(/&nbsp;|&amp;|&quot;|&#39;/g, ' ').replace(/\s+/g, ' ').trim();
async function search(q) {
  const h = await (await get('https://html.duckduckgo.com/html/?q=' + encodeURIComponent(q))).text();
  const re = /<a[^>]*class="result__a"[^>]*href="([^"]+)"[^>]*>([\s\S]*?)<\/a>[\s\S]*?class="result__snippet"[^>]*>([\s\S]*?)<\/a>/g;
  const out = []; let m;
  while ((m = re.exec(h)) && out.length < 6) {
    let u = m[1]; const x = /uddg=([^&]+)/.exec(u); if (x) u = decodeURIComponent(x[1]);
    out.push({ title: text(m[2]), url: u, snippet: text(m[3]) });
  }
  return out;
}
const post = (url, headers, body) => fetch(url, { method: 'POST', headers: { ...J, ...headers }, body: JSON.stringify(body), signal: AbortSignal.timeout(10000) });
const P = {
  telegram: t => post(`https://api.telegram.org/bot${E.TELEGRAM_BOT_TOKEN}/sendMessage`, {}, { chat_id: E.TELEGRAM_CHAT_ID, text: t }),
  discord: t => post(E.DISCORD_WEBHOOK_URL, {}, { content: t }),
  mastodon: t => post(`${E.MASTODON_URL}/api/v1/statuses`, { authorization: 'Bearer ' + E.MASTODON_TOKEN }, { status: t }),
  x: t => post('https://api.x.com/2/tweets', { authorization: 'Bearer ' + E.X_USER_TOKEN }, { text: t.slice(0, 280) }),
};
const ready = { telegram: E.TELEGRAM_BOT_TOKEN && E.TELEGRAM_CHAT_ID, discord: E.DISCORD_WEBHOOK_URL,
  mastodon: E.MASTODON_URL && E.MASTODON_TOKEN, x: E.X_USER_TOKEN };
const keyOk = k => { const a = Buffer.from(String(k || '')), b = Buffer.from(OFFICE_KEY); return a.length === b.length && timingSafeEqual(a, b); };

let running = false, statusAt = 0, statusCache = {};
const deps = { state, save, search, fetchText: async u => text(await (await get(u)).text()).slice(0, 6000),
  draft: t => queue.push({ id: ++seq, text: String(t).slice(0, 500), status: 'pending' }),
  notify: async t => { if (ready.telegram) { try { return (await P.telegram(t)).ok; } catch {} } return false } };
function startJob(textIn) {
  const day = new Date().toISOString().slice(0, 10);
  if (state.cap?.day !== day) state.cap = { day, n: 0 };
  if (state.cap.n >= Number(process.env.MAX_JOBS_PER_DAY || 40)) return null;
  state.cap.n++;
  const job = { id: Date.now().toString(36), text: textIn, status: 'running', steps: [], final: '' };
  state.jobs = [...state.jobs, job].slice(-20); running = true; save();
  runJob(textIn, deps, job).catch(e => { job.status = 'failed'; job.steps.push('⚠️ ' + String(e.message).slice(0, 150)); }).finally(() => { running = false; save(); });
  return job.id;
}
// Off-screen heartbeat (band hai jab tak HEARTBEAT_MIN > 0 na ho): Boss khud CI aur issues dekhta hai.
if (Number(process.env.HEARTBEAT_MIN) > 0 && llmReady()) setInterval(() => { if (!running) startJob('Health sweep: CI checks aur open issues dekho, koi bug ya failure ho to batao aur zaroorat par fix propose karo.'); }, Number(process.env.HEARTBEAT_MIN) * 60000);

http.createServer(async (req, res) => {
  const send = (c, o) => { res.writeHead(c, { ...J, 'access-control-allow-origin': ALLOW_ORIGIN,
    'access-control-allow-headers': 'content-type,x-office-key', 'access-control-allow-methods': 'GET,POST,OPTIONS' }); res.end(JSON.stringify(o)); };
  if (req.method === 'OPTIONS') return send(204, {});
  try {
    const ip = req.socket.remoteAddress; hits.set(ip, (hits.get(ip) || 0) + 1);
    if (hits.get(ip) > 60) return send(429, { error: 'rate limit' });
    if (!keyOk(req.headers['x-office-key'])) return send(401, { error: 'bad key' });
    const u = new URL(req.url, 'http://x');
    const body = async () => { let d = ''; for await (const c of req) { d += c; if (d.length > 10000) throw Error('too big'); } return d ? JSON.parse(d) : {}; };
    if (u.pathname === '/api/health') return send(200, { ok: true, social: Object.keys(P).filter(k => ready[k]) });
    if (u.pathname === '/api/search') return send(200, { results: await search(String(u.searchParams.get('q') || '').slice(0, 200)) });
    if (u.pathname === '/api/fetch') {
      const r = await get(String(u.searchParams.get('url') || ''));
      return send(200, { url: r.url, text: text((await r.text()).slice(0, 1000000)).slice(0, 6000) });
    }
    if (u.pathname === '/api/social/queue' && req.method === 'GET') return send(200, { items: queue.slice(-30) });
    if (u.pathname === '/api/social/queue') {
      const t = String((await body()).text || '').trim().slice(0, 500); if (!t) return send(400, { error: 'empty' });
      queue.push({ id: ++seq, text: t, status: 'pending' }); return send(200, { ok: true });
    }
    if (u.pathname === '/api/social/post') {
      const b = await body(), it = queue.find(i => i.id === b.id && i.status === 'pending');
      const targets = Object.keys(P).filter(k => ready[k]);
      if (!it) return send(404, { error: 'not found' });
      if (!targets.length) return send(400, { error: 'koi social account configure nahi' });
      const sent = []; for (const k of targets) { try { if ((await P[k](it.text)).ok) sent.push(k); } catch {} }
      it.status = sent.length ? 'posted' : 'failed'; return send(200, { sent });
    }
    if (u.pathname === '/api/agents') return send(200, { boss: 'Zaythar AI', llm: llmReady(), github: gh.ghReady(), agents: AGENTS.map(n => ({ name: n, role: ROSTER[n].role, xp: agentStore(n).memory().xp })) });
    if (u.pathname === '/api/memory' && req.method === 'GET') return send(200, { memory: Object.fromEntries(AGENTS.map(n => [n, agentStore(n).memory()])) });
    if (u.pathname === '/api/memory') {
      const b = await body();
      for (const k of AGENTS) { const m = b[k]; if (m && Number.isFinite(m.xp)) { const S = agentStore(k), c = S.memory(); S.saveMemory({ ...c, xp: Math.max(c.xp || 0, Math.min(m.xp, 1e6)), lessons: [...new Set([...(c.lessons || []), ...(Array.isArray(m.lessons) ? m.lessons : []).map(x => String(x).slice(0, 90))])].slice(-12) }); } }
      return send(200, { ok: true });
    }
    if (u.pathname === '/api/task') {
      if (!llmReady()) return send(400, { error: 'ANTHROPIC_API_KEY set nahi' });
      if (running) return send(429, { error: 'ek kaam pehle se chal raha hai' });
      const t = String((await body()).text || '').trim().slice(0, 500); if (!t) return send(400, { error: 'empty' });
      const id = startJob(t); return id ? send(200, { id }) : send(429, { error: 'aaj ki hadd poori (MAX_JOBS_PER_DAY)' });
    }
    if (u.pathname === '/api/jobs') return send(200, { jobs: state.jobs.slice(-10) });
    if (u.pathname === '/api/approvals' && req.method === 'GET') return send(200, { items: state.approvals.slice(-30) });
    if (u.pathname === '/api/approvals') {
      const b = await body(), it = state.approvals.find(i => i.id === b.id && i.status === 'pending');
      if (!it) return send(404, { error: 'not found' });
      if (!b.approve) { it.status = 'rejected'; save(); return send(200, { ok: true }); }
      try { it.url = await gh.openPr(it.data); it.status = 'done'; } catch (e) { it.status = 'failed'; it.error = String(e.message).slice(0, 150); }
      save(); return send(200, { status: it.status, url: it.url, error: it.error });
    }
    if (u.pathname === '/api/chat') {
      if (!llmReady()) return send(400, { error: 'ANTHROPIC_API_KEY set nahi' });
      const b = await body(); if (!ROSTER[b.agent]) return send(404, { error: 'agent nahi mila' });
      const t = String(b.text || '').trim().slice(0, 500); if (!t) return send(400, { error: 'empty' });
      return send(200, { reply: await chat(b.agent, t) });
    }
    if (u.pathname === '/api/status') {
      if (Date.now() - statusAt > 60000) {
        statusAt = Date.now(); statusCache = { checks: '', issues: '' };
        if (gh.ghReady()) {
          try { statusCache.checks = await gh.repoChecks(); } catch (e) { statusCache.checks = 'Error: ' + String(e.message).slice(0, 60); }
          try { statusCache.issues = await gh.repoIssues(); } catch (e) { statusCache.issues = 'Error: ' + String(e.message).slice(0, 60); }
        }
      }
      return send(200, { ...statusCache, llm: llmReady(), github: gh.ghReady(), telegram: !!ready.telegram, running, pending: state.approvals.filter(i => i.status === 'pending').length });
    }
    if (u.pathname === '/api/audit') return send(200, { audit: (state.audit || []).slice(-50) });
    if (u.pathname === '/api/memory/delete') {
      const b = await body(); if (!ROSTER[b.agent] || !['facts', 'lessons'].includes(b.kind)) return send(400, { error: 'bad request' });
      const S = agentStore(b.agent), m = S.memory(); m[b.kind] = m[b.kind].filter((_, i) => i !== Number(b.index)); S.saveMemory(m); return send(200, { ok: true });
    }
    if (u.pathname === '/api/tasks') return send(200, { tasks: state.tasks.slice(-30) });
    if (u.pathname === '/api/notify') {
      const t = String((await body()).text || '').slice(0, 1000); if (!t) return send(400, { error: 'empty' });
      state.tasks = [...state.tasks, { ts: Date.now(), text: t.slice(0, 300) }].slice(-200); save();
      let sent = false; if (ready.telegram) { try { sent = (await P.telegram(t)).ok; } catch {} }
      return send(200, { sent });
    }
    send(404, { error: 'not found' });
  } catch (e) { send(500, { error: String(e.message || e).slice(0, 200) }); }
}).listen(PORT, () => console.log('Zaythar Office server on :' + PORT));
