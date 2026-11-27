// Server-side model call (Anthropic Messages API). Key sirf .env mein, kabhi client mein nahi.
const { ANTHROPIC_API_KEY, ANTHROPIC_MODEL = 'claude-sonnet-5-5' } = process.env;
export const llmReady = () => !!ANTHROPIC_API_KEY;
export async function ask(system, user, max = 1500) {
  const r = await fetch('https://api.anthropic.com/v1/messages', {
    method: 'POST', signal: AbortSignal.timeout(60000),
    headers: { 'content-type': 'application/json', 'x-api-key': ANTHROPIC_API_KEY, 'anthropic-version': '2023-06-01' },
    body: JSON.stringify({ model: ANTHROPIC_MODEL, max_tokens: max, system, messages: [{ role: 'user', content: user }] }),
  });
  const j = await r.json();
  if (!r.ok) throw Error(j.error?.message || r.status);
  return j.content.map(c => c.text || '').join('');
}
export async function askJson(system, user, max) {
  const t = await ask(system, user + '\nSirf valid JSON do, koi markdown nahi.', max);
  const m = t.match(/\{[\s\S]*\}/);
  if (!m) throw Error('model ne JSON nahi diya');
  return JSON.parse(m[0]);
}
