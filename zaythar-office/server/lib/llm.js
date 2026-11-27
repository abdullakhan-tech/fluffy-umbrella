// Server-only bridge: office agents call the existing Zaythar FastAPI router.
// Secrets remain in the office server environment and are never sent to the browser.
const env = process.env;
const baseUrl = (env.ZAYTHAR_API_URL || "").trim().replace(/\/$/, "");
const provider = env.ZAYTHAR_PROVIDER || "auto";
const model = env.ZAYTHAR_MODEL || "";
const fallbackKey = env.ANTHROPIC_API_KEY || "";
const fallbackModel = env.ANTHROPIC_MODEL || "claude-sonnet-5-5";

export const llmReady = () => Boolean(baseUrl || fallbackKey);

export async function ask(system, user, maxTokens = 1500) {
  if (baseUrl) {
    const headers = { "content-type": "application/json", accept: "application/json" };
    if (env.OFFICE_AGENT_TOKEN) headers.authorization = `Bearer ${env.OFFICE_AGENT_TOKEN}`;
    const response = await fetch(`${baseUrl}/api/v1/chat`, {
      method: "POST",
      headers,
      signal: AbortSignal.timeout(60000),
      body: JSON.stringify({
        provider,
        ...(model ? { model } : {}),
        personality: env.ZAYTHAR_PERSONALITY || "friday",
        temperature: 0.2,
        max_tokens: Math.min(Number(maxTokens) || 1500, 4096),
        messages: [
          { role: "system", content: String(system).slice(0, 12000) },
          { role: "user", content: String(user).slice(0, 12000) }
        ]
      })
    });
    const data = await response.json().catch(() => ({}));
    if (!response.ok) throw new Error(data.detail || data.error || `Zaythar backend HTTP ${response.status}`);
    if (!data.reply) throw new Error("Zaythar backend returned an empty reply");
    return String(data.reply);
  }

  if (!fallbackKey) throw new Error("Configure ZAYTHAR_API_URL to use the existing Zaythar AI engine.");
  const response = await fetch("https://api.anthropic.com/v1/messages", {
    method: "POST",
    signal: AbortSignal.timeout(60000),
    headers: { "content-type": "application/json", "x-api-key": fallbackKey, "anthropic-version": "2023-06-01" },
    body: JSON.stringify({
      model: fallbackModel,
      max_tokens: Math.min(Number(maxTokens) || 1500, 4096),
      system: String(system),
      messages: [{ role: "user", content: String(user) }]
    })
  });
  const data = await response.json();
  if (!response.ok) throw new Error(data.error?.message || response.status);
  return (data.content || []).map(part => part.text || "").join("");
}

export async function askJson(system, user, maxTokens) {
  const raw = await ask(system, user + "\nReturn valid JSON only; no Markdown.", maxTokens);
  const match = raw.match(/\{[\s\S]*\}/);
  if (!match) throw new Error("Agent model did not return JSON");
  return JSON.parse(match[0]);
}
