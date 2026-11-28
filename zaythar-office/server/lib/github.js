// Repo tools. Token (.env) ko sirf Contents + Pull requests ki ijazat dein. Main par seedha push kabhi nahi: hamesha naya branch + PR.
const { GITHUB_TOKEN, GITHUB_REPO } = process.env; // GITHUB_REPO = "owner/repo"
export const ghReady = () => !!(GITHUB_TOKEN && GITHUB_REPO);
const api = async (p, o = {}) => {
  const r = await fetch('https://api.github.com/repos/' + GITHUB_REPO + p, {
    ...o, signal: AbortSignal.timeout(15000),
    headers: { accept: 'application/vnd.github+json', authorization: 'Bearer ' + GITHUB_TOKEN, 'user-agent': 'zaythar-office', ...(o.body ? { 'content-type': 'application/json' } : {}) },
  });
  const j = await r.json().catch(() => ({}));
  if (!r.ok) throw Error(j.message || r.status);
  return j;
};
export const safePath = p => { if (/(^|\/)\.\.(\/|$)|^\//.test(p) || /(^|\/)(\.env|google-services\.json|.*\.(jks|keystore|pem))/i.test(p)) throw Error('path blocked'); return p; };
export const repoRead = async p => {
  const j = await api('/contents/' + safePath(p));
  return Array.isArray(j) ? j.map(x => x.type + ' ' + x.path).join('\n') : Buffer.from(j.content || '', 'base64').toString('utf8').slice(0, 8000);
};
export const repoChecks = async () => {
  const b = await api(''), r = await api('/commits/' + b.default_branch + '/check-runs');
  return (r.check_runs || []).slice(0, 15).map(c => c.name + ': ' + (c.conclusion || c.status)).join('\n') || 'koi check nahi';
};
export const repoIssues = async () => (await api('/issues?state=open&per_page=10')).map(i => '#' + i.number + ' ' + i.title).join('\n') || 'koi open issue nahi';
export async function openPr({ branch, title, message, files }) {
  const b = await api(''), ref = await api('/git/ref/heads/' + b.default_branch);
  await api('/git/refs', { method: 'POST', body: JSON.stringify({ ref: 'refs/heads/' + branch, sha: ref.object.sha }) });
  for (const f of files) {
    let sha; try { sha = (await api('/contents/' + safePath(f.path) + '?ref=' + branch)).sha; } catch {}
    await api('/contents/' + f.path, { method: 'PUT', body: JSON.stringify({ message, branch, content: Buffer.from(f.content).toString('base64'), sha }) });
  }
  const pr = await api('/pulls', { method: 'POST', body: JSON.stringify({ title, head: branch, base: b.default_branch, body: message + '\n\nZaythar Office agents ne banaya. Merge se pehle review zaroor karein.' }) });
  return pr.html_url;
}
