# Zaythar AI provider streaming

The `POST /api/v1/chat/stream` endpoint now forwards incremental chunks from configured providers instead of waiting for a completed answer and splitting it into artificial word chunks.

## Provider configuration

Configure provider credentials only in the backend environment (never in Android resources, browser variables, or source control):

- `OPENAI_API_KEY` — enables OpenAI Chat Completions streaming.
- `GEMINI_API_KEY` — enables Gemini `streamGenerateContent` SSE.
- `OLLAMA_BASE_URL` — enables Ollama NDJSON streaming (defaults to `http://localhost:11434`).

When `provider=auto`, the server tries OpenAI, Gemini, then Ollama in that order, skipping providers without credentials/configuration. If no provider can stream, it returns the deterministic local fallback with `provider=local_engine`. A local fallback response is not a hosted LLM response.

## SSE event contract

Each event is a JSON object in an SSE `data:` line:

```json
{"chunk":"partial text","provider":"openai","done":false}
{"chunk":"","provider":"openai","done":true}
```

Clients should append each non-empty `chunk` to the current assistant message, display `provider`, and stop when `done` is true. Treat the provider field as diagnostic metadata, not proof of account billing or model entitlement.

## Operational notes

- API keys remain server-side and must be stored in the deployment secret manager.
- The current endpoint is text streaming; it is not a bidirectional audio/voice transport.
- Gemini Live API remains a separate Android voice feature. Its Firebase AI Logic integration is currently documented by Google as preview functionality; validate model availability and session limits before production rollout.
- Provider errors are redacted in logs and the router tries the next configured provider.
- Deploy behind HTTPS, restrict CORS to the actual client origins, and add authentication/rate limiting before exposing a public endpoint.
