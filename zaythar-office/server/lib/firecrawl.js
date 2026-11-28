// Firecrawl Web Crawler and Scraper Integration for Zaythar Office
const { FIRECRAWL_API_KEY, FIRECRAWL_API_URL = 'https://api.firecrawl.dev/v1' } = process.env;

export const firecrawlReady = () => !!FIRECRAWL_API_KEY;

export async function firecrawlSearch(query, limit = 5) {
  if (!FIRECRAWL_API_KEY) return null;
  const res = await fetch(`${FIRECRAWL_API_URL.replace(/\/$/, '')}/search`, {
    method: 'POST',
    signal: AbortSignal.timeout(15000),
    headers: {
      'content-type': 'application/json',
      'authorization': `Bearer ${FIRECRAWL_API_KEY}`
    },
    body: JSON.stringify({
      query: String(query).slice(0, 200),
      limit: Math.min(Math.max(limit, 1), 10)
    })
  });
  if (!res.ok) throw Error(`Firecrawl search error: ${res.status}`);
  const json = await res.json();
  const items = json.data || json.results || [];
  return items.map(item => ({
    title: item.title || item.metadata?.title || 'No title',
    url: item.url || '',
    snippet: item.description || item.snippet || (item.markdown || '').slice(0, 250)
  }));
}

export async function firecrawlScrape(url) {
  if (!FIRECRAWL_API_KEY) return null;
  const res = await fetch(`${FIRECRAWL_API_URL.replace(/\/$/, '')}/scrape`, {
    method: 'POST',
    signal: AbortSignal.timeout(20000),
    headers: {
      'content-type': 'application/json',
      'authorization': `Bearer ${FIRECRAWL_API_KEY}`
    },
    body: JSON.stringify({
      url,
      formats: ['markdown']
    })
  });
  if (!res.ok) throw Error(`Firecrawl scrape error: ${res.status}`);
  const json = await res.json();
  return (json.data?.markdown || json.markdown || '').slice(0, 8000);
}

export async function firecrawlCrawl(url, limit = 5) {
  if (!FIRECRAWL_API_KEY) return null;
  const res = await fetch(`${FIRECRAWL_API_URL.replace(/\/$/, '')}/crawl`, {
    method: 'POST',
    signal: AbortSignal.timeout(25000),
    headers: {
      'content-type': 'application/json',
      'authorization': `Bearer ${FIRECRAWL_API_KEY}`
    },
    body: JSON.stringify({
      url,
      limit: Math.min(Math.max(limit, 1), 10)
    })
  });
  if (!res.ok) throw Error(`Firecrawl crawl error: ${res.status}`);
  return await res.json();
}
