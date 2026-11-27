// Optional Firecrawl adapter. FIRECRAWL_API_KEY must exist only on the server.
const apiKey = process.env.FIRECRAWL_API_KEY || "";
const endpoint = "https://api.firecrawl.dev/v1";

async function request(path, payload) {
  if (!apiKey) throw new Error("FIRECRAWL_API_KEY is not configured");
  const response = await fetch(endpoint + path, {
    method: "POST",
    signal: AbortSignal.timeout(30000),
    headers: { authorization: `Bearer ${apiKey}`, "content-type": "application/json" },
    body: JSON.stringify(payload)
  });
  const data = await response.json().catch(() => ({}));
  if (!response.ok || data.success === false) {
    throw new Error(data.error || `Firecrawl HTTP ${response.status}`);
  }
  return data;
}

export const firecrawlReady = () => Boolean(apiKey);

export async function firecrawlSearch(query, limit = 6) {
  const data = await request("/search", { query: String(query).slice(0, 200), limit: Math.min(Math.max(limit, 1), 10) });
  return (data.data || []).map(item => ({
    title: item.title || "",
    url: item.url || "",
    snippet: String(item.description || item.markdown || "").slice(0, 1200)
  }));
}

export async function firecrawlScrape(url) {
  const target = new URL(url);
  if (!["http:", "https:"].includes(target.protocol)) throw new Error("Only HTTP(S) URLs are allowed");
  const data = await request("/scrape", { url: target.href, formats: ["markdown"] });
  return String(data.data?.markdown || data.markdown || "").slice(0, 6000);
}
