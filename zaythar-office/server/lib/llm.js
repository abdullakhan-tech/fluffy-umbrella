// Multi-provider AI Engine call supporting:
// 1. Google Gemini (Zaythar AI Engine default)
// 2. Zaythar FastAPI Backend Engine
// 3. Anthropic Claude
// 4. OpenAI
// 5. Fallback Mock for safe offline execution
const {
  GEMINI_API_KEY,
  GOOGLE_API_KEY,
  GEMINI_MODEL = 'gemini-2.5-flash',
  ZAYTHAR_BACKEND_URL,
  OPENAI_API_KEY,
  OPENAI_MODEL = 'gpt-4o-mini',
  ANTHROPIC_API_KEY,
  ANTHROPIC_MODEL = 'claude-sonnet-5-5'
} = process.env;

const geminiKey = GEMINI_API_KEY || GOOGLE_API_KEY;

export const llmReady = () => !!(geminiKey || ZAYTHAR_BACKEND_URL || ANTHROPIC_API_KEY || OPENAI_API_KEY);

export async function ask(system, user, max = 1500) {
  // 1. Google Gemini API (Zaythar AI engine primary)
  if (geminiKey) {
    const url = `https://generativelanguage.googleapis.com/v1beta/models/${GEMINI_MODEL}:generateContent?key=${geminiKey}`;
    const r = await fetch(url, {
      method: 'POST',
      signal: AbortSignal.timeout(60000),
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({
        system_instruction: { parts: [{ text: system }] },
        contents: [{ role: 'user', parts: [{ text: user }] }],
        generationConfig: { maxOutputTokens: max, temperature: 0.7 }
      })
    });
    const j = await r.json();
    if (!r.ok) throw Error(j.error?.message || `Gemini API error ${r.status}`);
    return j.candidates?.[0]?.content?.parts?.[0]?.text || '';
  }

  // 2. Zaythar FastAPI backend integration
  if (ZAYTHAR_BACKEND_URL) {
    const r = await fetch(`${ZAYTHAR_BACKEND_URL.replace(/\/$/, '')}/api/v1/chat`, {
      method: 'POST',
      signal: AbortSignal.timeout(60000),
      headers: { 'content-type': 'application/json' },
      body: JSON.stringify({
        message: `${system}\n\nTask:\n${user}`,
        session_id: 'office-agents'
      })
    });
    const j = await r.json();
    if (!r.ok) throw Error(j.detail || `Backend API error ${r.status}`);
    return j.response || '';
  }

  // 3. Anthropic Messages API
  if (ANTHROPIC_API_KEY) {
    const r = await fetch('https://api.anthropic.com/v1/messages', {
      method: 'POST',
      signal: AbortSignal.timeout(60000),
      headers: {
        'content-type': 'application/json',
        'x-api-key': ANTHROPIC_API_KEY,
        'anthropic-version': '2023-06-01'
      },
      body: JSON.stringify({
        model: ANTHROPIC_MODEL,
        max_tokens: max,
        system,
        messages: [{ role: 'user', content: user }]
      })
    });
    const j = await r.json();
    if (!r.ok) throw Error(j.error?.message || `Anthropic API error ${r.status}`);
    return j.content.map(c => c.text || '').join('');
  }

  // 4. OpenAI API
  if (OPENAI_API_KEY) {
    const r = await fetch('https://api.openai.com/v1/chat/completions', {
      method: 'POST',
      signal: AbortSignal.timeout(60000),
      headers: {
        'content-type': 'application/json',
        'authorization': `Bearer ${OPENAI_API_KEY}`
      },
      body: JSON.stringify({
        model: OPENAI_MODEL,
        max_tokens: max,
        messages: [
          { role: 'system', content: system },
          { role: 'user', content: user }
        ]
      })
    });
    const j = await r.json();
    if (!r.ok) throw Error(j.error?.message || `OpenAI API error ${r.status}`);
    return j.choices?.[0]?.message?.content || '';
  }

  // Fallback offline mock for tests or environments without live API credentials
  if (user.includes('Sirf valid JSON do')) {
    return JSON.stringify({
      final: 'Zaythar AI engine connected. Offline fallback operational.',
      reply: 'Ji boss, sab tayyar hai.',
      boss_say: 'Kaam banta hai.',
      assign: [{ agent: 'friday', task: 'Task assigned' }],
      reviews: [{ agent: 'friday', score: 8, feedback: 'Bohat acha', lesson: 'Hifazat pehle' }]
    });
  }
  return 'Zaythar AI Office engine ready.';
}

export async function askJson(system, user, max) {
  const t = await ask(system, user + '\nSirf valid JSON do, koi markdown nahi.', max);
  const m = t.match(/\{[\s\S]*\}/);
  if (!m) throw Error('model ne JSON nahi diya');
  return JSON.parse(m[0]);
}
