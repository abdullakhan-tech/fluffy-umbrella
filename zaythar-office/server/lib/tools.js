// ToolRegistry + PermissionBroker (docs/JARVIS_EXECUTION_MODEL.md ke mutabiq):
// har tool ke args bounded, timeout, risk tier (read / reversible / consequential) aur audit log.
import * as gh from './github.js';
import { agentStore } from './store.js';
import { ask } from './llm.js';
import { firecrawlSearch, firecrawlScrape, firecrawlCrawl } from './firecrawl.js';

const lim = (v, n) => String(v ?? '').slice(0, n);
const slug = s => String(s).toLowerCase().replace(/[^a-z0-9]+/g, '-').slice(0, 30);
const mem = (n, f) => { const S = agentStore(n), m = S.memory(); f(m); S.saveMemory(m); };

export const TOOLS = {
  web_search: { tier: 'read', run: async (a, c) => {
    try {
      const fc = await firecrawlSearch(lim(a.q, 200));
      if (fc && fc.length) return JSON.stringify(fc);
    } catch {}
    return c.d.search(lim(a.q, 200)).then(JSON.stringify);
  }},
  web_fetch: { tier: 'read', run: async (a, c) => {
    try {
      const fc = await firecrawlScrape(lim(a.url, 500));
      if (fc) return fc;
    } catch {}
    return c.d.fetchText(lim(a.url, 500));
  }},
  firecrawl_search: { tier: 'read', run: async a => {
    const res = await firecrawlSearch(lim(a.q, 200), a.limit || 5);
    return JSON.stringify(res || []);
  }},
  firecrawl_scrape: { tier: 'read', run: async a => {
    const res = await firecrawlScrape(lim(a.url, 500));
    return res || 'No content found';
  }},
  firecrawl_crawl: { tier: 'read', run: async a => {
    const res = await firecrawlCrawl(lim(a.url, 500), a.limit || 5);
    return JSON.stringify(res || {});
  }},
  repo_read: { tier: 'read', run: a => gh.repoRead(lim(a.path, 200)) },
  repo_list: { tier: 'read', run: a => gh.repoRead(lim(a.path, 200)) },
  repo_checks: { tier: 'read', run: () => gh.repoChecks() },
  repo_issues: { tier: 'read', run: () => gh.repoIssues() },
  consult: { tier: 'read', run: (a, c) => {
    const B = c.roster[a.agent]; if (!B || a.agent === c.from) throw Error('agent nahi mila');
    return ask(`Tum ${a.agent} ho (${B.role}). ${B.persona} Chhota, saaf mashwara do.`, lim(a.question, 500), 500); } },
  remember: { tier: 'reversible', run: (a, c) => { mem(c.from, m => { m.facts = [...m.facts, lim(a.fact, 120)].slice(-30); }); return 'yaad kar liya'; } },
  teach: { tier: 'reversible', run: (a, c) => { if (!c.roster[a.agent]) throw Error('agent nahi mila'); mem(a.agent, m => { m.lessons = [...m.lessons, lim(a.lesson, 90)].slice(-12); }); return 'sikha diya'; } },
  draft_post: { tier: 'reversible', run: (a, c) => { c.d.draft(lim(a.text, 500)); return 'Draft queue mein gaya, post manzoori ke baad hogi.'; } },
  propose_patch: { tier: 'consequential', run: (a, c) => {
    const files = (Array.isArray(a.files) ? a.files : []).slice(0, 5).map(f => ({ path: gh.safePath(lim(f.path, 200)), content: lim(f.content, 60000) }));
    if (!files.length) throw Error('files chahiye');
    const title = lim(a.title || 'Agent fix', 100);
    c.d.state.approvals.push({ id: Date.now().toString(36) + Math.random().toString(36).slice(2, 5), kind: 'pr', by: c.from, title, status: 'pending',
      data: { branch: 'agents/' + slug(title) + '-' + Date.now().toString(36), title, message: lim(a.message || title, 500), files } });
    c.d.save(); return 'Patch manzoori ke liye queue mein gaya (abhi repo mein kuch nahi badla).'; } },
};

export async function callTool(from, allowed, call, ctx) {
  const t = TOOLS[call.tool]; ctx = { ...ctx, from };
  const audit = ok => { const s = ctx.d.state; s.audit = [...(s.audit || []), { ts: Date.now(), agent: from, tool: String(call.tool).slice(0, 30), tier: t?.tier, ok }].slice(-200); };
  try {
    if (!t || !allowed.includes(call.tool)) throw Error('ye tool tumhare liye allowed nahi');
    const out = await Promise.race([Promise.resolve(t.run(call.args || {}, ctx)), new Promise((_, rj) => setTimeout(() => rj(Error('timeout')), 25000))]);
    audit(true); return out;
  } catch (e) { audit(false); throw e; }
}
