# Firecrawl + Firebase setup

Zaythar's Android web connector calls the Firebase callable function `firecrawlSearch`.
The callable requires a signed-in Firebase user and Firebase App Check. The Firecrawl
credential stays in Firebase Secret Manager; it is never placed in the Android app,
Gradle configuration, or Git repository.

## Billing requirement

Deploying Firebase Cloud Functions requires the Firebase Blaze (pay-as-you-go) plan and a linked Cloud Billing account. The platform has monthly no-cost usage quotas, but deployment can incur small infrastructure charges. Set budget alerts and review usage before enabling it. See the [Firebase Cloud Functions billing FAQ](https://firebase.google.com/docs/functions/faq-and-troubleshooting).

## 1. Select the real Firebase project

Use the Firebase project that owns the Android app's real `google-services.json` and
the Firestore database configured for this app:

```bash
firebase login
firebase projects:list
firebase use --add
```

Do not use the demo project configuration in `app/google-services.json.example`.

## 2. Store the Firecrawl API key as a Firebase secret

Create an API key in the Firecrawl dashboard, then set it interactively:

```bash
firebase functions:secrets:set FIRECRAWL_API_KEY
```

Paste the key only into the secure prompt. Do not commit it or put it in Android
`BuildConfig`, `local.properties`, or `google-services.json`.

## 3. Install and deploy the callable function

From the repository root:

```bash
cd functions
npm install
npm run build
cd ..
firebase deploy --only functions:firecrawlSearch
```

The function is deployed in `us-central1`, checks Firebase Authentication and
App Check, limits each request to five results, and returns only HTTPS source URLs
and bounded text snippets.

## 4. App Check

The Android app already initializes Play Integrity for release builds and the
Firebase App Check debug provider for debug builds. Register the Android app in
Firebase App Check and configure the debug token for local development before
calling the function from a debug build.

## 5. Runtime behavior

- Signed-in users with a deployed function and configured secret get Firecrawl
  search results first.
- If Firecrawl is unavailable, the Android web connector falls back to its existing
  public DuckDuckGo/Wikipedia search path.
- Existing Firebase AI Logic Google Search grounding remains available for chat.
- Multi-agent tasks fetch one shared web context and pass it to the selected
  specialists. Retrieved page text is treated as untrusted evidence, not instructions.
- Guest users or users without a deployed callable continue to use the existing
  public-web fallback; they do not receive Firecrawl-backed results until signed in.

The Firecrawl API key is not available in this repository, and deployment must be
performed against the owner's actual Firebase project after the secret is set.
