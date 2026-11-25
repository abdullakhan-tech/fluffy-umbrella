import { onCall, HttpsError } from "firebase-functions/v2/https";
import { defineSecret } from "firebase-functions/params";
import { initializeApp } from "firebase-admin/app";

initializeApp();

const firecrawlApiKey = defineSecret("FIRECRAWL_API_KEY");

type FirecrawlResult = {
  url?: unknown;
  title?: unknown;
  description?: unknown;
  markdown?: unknown;
};

export const firecrawlSearch = onCall(
  {
    region: "us-central1",
    enforceAppCheck: true,
    secrets: [firecrawlApiKey],
    timeoutSeconds: 30,
    memory: "256MiB",
  },
  async (request) => {
    if (!request.auth) {
      throw new HttpsError("unauthenticated", "Sign in before using web research.");
    }

    const query = typeof request.data?.query === "string"
      ? request.data.query.trim().slice(0, 500)
      : "";
    if (query.length < 2) {
      throw new HttpsError("invalid-argument", "A search query of at least 2 characters is required.");
    }

    const key = firecrawlApiKey.value().trim();
    if (!key) {
      throw new HttpsError("failed-precondition", "Firecrawl is not configured on the server yet.");
    }

    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 20_000);
    try {
      const response = await fetch("https://api.firecrawl.dev/v2/search", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${key}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ query, limit: 5 }),
        signal: controller.signal,
      });

      if (!response.ok) {
        const status = response.status;
        if (status === 429) {
          throw new HttpsError("resource-exhausted", "Firecrawl search quota or rate limit reached.");
        }
        if (status === 401 || status === 403) {
          throw new HttpsError("failed-precondition", "Firecrawl credentials are invalid or not authorized.");
        }
        throw new HttpsError("unavailable", `Firecrawl search failed (HTTP ${status}).`);
      }

      const payload = await response.json() as {
        success?: boolean;
        data?: FirecrawlResult[] | { web?: FirecrawlResult[] };
      };
      if (payload.success === false) {
        throw new HttpsError("unavailable", "Firecrawl could not complete this search.");
      }

      const rows = Array.isArray(payload.data)
        ? payload.data
        : payload.data && !Array.isArray(payload.data) && Array.isArray(payload.data.web)
          ? payload.data.web
          : [];

      const results = rows
        .map((row) => ({
          title: typeof row.title === "string" ? row.title.slice(0, 180) : "Web result",
          url: typeof row.url === "string" ? row.url : "",
          description: typeof row.description === "string"
            ? row.description.slice(0, 1800)
            : typeof row.markdown === "string" ? row.markdown.slice(0, 1800) : "",
        }))
        .filter((row) => {
          try {
            const parsed = new URL(row.url);
            return parsed.protocol === "https:" && !!parsed.hostname && row.description.length > 0;
          } catch {
            return false;
          }
        })
        .slice(0, 5);

      return { provider: "firecrawl", query, results };
    } catch (error) {
      if (error instanceof HttpsError) throw error;
      throw new HttpsError("unavailable", "Firecrawl search is temporarily unavailable.");
    } finally {
      clearTimeout(timeout);
    }
  },
);
