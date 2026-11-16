# API Reference

Base URL: same origin as the FastAPI app.

## GET /api/health
Returns service status, whether an AI key is configured, selected model name, and API version. It never returns the key.

Example:
```json
{"status":"ok","service":"Zaythar AI","ai_configured":false,"model":"gpt-4o-mini","version":"1.1.0"}
```

## POST /api/chat
Request:
```json
{
  "message":"Hello",
  "language":"en-US",
  "persona":{
    "empathy":70,
    "cognitive_capacity":70,
    "adaptability":70,
    "creativity":70,
    "style":"Balanced & helpful"
  },
  "settings":{},
  "agent":"general"
}
```

Allowed specialist modes: `general`, `research`, `coding`, `writing`, `analysis`, `creative`.

Successful response:
```json
{"reply":"Hello! How can I help?","model":"configured-model"}
```

Errors include 503 when no server API key is configured, 429 for provider quota/rate limits, and 502 for provider failures. Do not expose provider credentials in requests from the browser.
