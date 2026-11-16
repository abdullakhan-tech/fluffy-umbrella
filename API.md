# API Reference

Base URL: same origin as the FastAPI app. Except for `GET /api/health` and account registration/login, API routes require an `Authorization: Bearer <session-token>` header. Obtain a session token from the authentication endpoints. Tokens are opaque, expire, and can be revoked by signing out. Do not place tokens in URLs, source code, or shared screenshots.

## Account lifecycle

### POST /api/auth/register
Creates an account only when the server has a strong `ZAYTHAR_REGISTRATION_TOKEN` configured. The invitation token is supplied in the request body and must be distributed privately.

```json
{"username":"operator01","password":"a-long-unique-passphrase","registration_token":"server-generated-invitation-secret"}
```

The response contains `token`, `expires_at`, and the new `user`. Passwords must contain 12–128 characters; usernames must contain 3–32 letters, numbers, dots, underscores or hyphens. Remove `ZAYTHAR_REGISTRATION_TOKEN` from the server environment to disable further registrations.

### POST /api/auth/login
```json
{"username":"operator01","password":"a-long-unique-passphrase"}
```

Returns an opaque bearer `token`, its Unix `expires_at`, and the account identity. Invalid credentials return a generic 401 response.

### GET /api/auth/me
Returns the identity associated with the current bearer token.

### POST /api/auth/logout
Revokes the current bearer token. Other sessions for the same account remain active until expiry or individually revoked.

## Public health

### GET /api/health
Returns service status, provider configured flags, version and process uptime. It never returns provider keys. This endpoint is public for health monitoring.

## Authenticated assistant

### POST /api/chat
```json
{
  "message":"Hello",
  "language":"en-US",
  "persona":{"empathy":70,"cognitive_capacity":70,"adaptability":70,"creativity":70,"style":"Balanced & helpful"},
  "agent":"general",
  "provider":"hybrid",
  "history":[{"role":"user","content":"Earlier question"}]
}
```

Allowed specialist modes: `general`, `research`, `coding`, `writing`, `analysis`, `creative`. History is limited to 16 user/assistant messages, each no longer than 6,000 characters. Client-supplied session IDs are rejected.

### POST /api/agents/run
```json
{"task":"Review this API design","agents":["research","coding"],"provider":"hybrid","language":"English"}
```

The specialist allow-list is `research`, `coding`, `analysis`, `writing`, `creative`; select one to four. Context is derived from the authenticated account.

## Per-account memory and knowledge

All routes below require authentication. Ownership is derived from the server-side account ID; no client session identifier or owner ID is accepted.

- `GET /api/memory` — list the current account's memories.
- `POST /api/memory` — body: `{"content":"A private note"}`.
- `DELETE /api/memory/{memory_id}` — delete only a memory owned by the current account.
- `DELETE /api/memory` — clear only the current account's memories.
- `GET /api/knowledge` — list the current account's explicit facts.
- `POST /api/knowledge` — body: `{"subject":"Zaythar","predicate":"uses","object":"hybrid AI","source":"user"}`.
- `DELETE /api/knowledge/{fact_id}` — delete only a fact owned by the current account.
- `DELETE /api/knowledge` — clear only the current account's facts.

## Error and rate-limit behavior

- `401`: missing, invalid or expired bearer token.
- `403`: invalid account invitation token.
- `409`: account name unavailable.
- `422`: invalid input, unsupported fields or specialist name.
- `429`: request rate limit reached; inspect `Retry-After`.
- `503`: registration disabled or AI providers unavailable.
- `502`: generic upstream processing failure.

Provider exception details are not returned to API clients. The built-in limiter is per-process and not sufficient by itself for multi-worker public hosting; configure a shared limiter at a trusted gateway and use HTTPS.
