# Zaythar AI Office (Boss + 7 agents)

3D office jahan **Boss Zaythar AI** 7 agents ko kaam baantta hai, kaam parakhta hai, kamzor kaam dobara karwata hai aur agents ek doosre ko train karte hain.
Boss **Zaythar AI** aur 7 agents: Jarvis (Orchestrator), Friday (Coder), Omnivex (Researcher), Rio (Social), Carbon (Memory), Neon (UI/UX), Argon (QA). Har agent ki apni shakhsiyat, apne tools aur apna folder `server/data/<agent>/` (memory.json + db.json) hai.

## Folder
- `index.html` - office (Three.js), `config.js` - server URL, `browser.js` - Agent Browser panel
- `server/` - Node 18+ server (koi dependency nahi): internet search, page fetch, social queue
- `backup/ai-office-artifact.html` - claude.ai wale artifact ki aakhri copy

## Chalane ka tareeqa
1. `cd server && cp .env.example .env` phir `.env` mein lamba random `OFFICE_KEY` likhein (aur chahein to social tokens).
2. `npm start` (server `:8787` par chalega). Kisi bhi host par deploy kar sakte hain (Cloud Run, Render, VPS).
3. `config.js` mein `window.OFFICE_API` ko server ka URL dein; `.env` mein `ALLOW_ORIGIN` apne page ka origin rakhein.
4. `index.html` GitHub Pages par host karein. Pehli baar page `OFFICE_KEY` maangega (sirf aap ke browser mein save hoti hai).

## Kya kaam karta hai
- **Internet:** Nova (Researcher) kaam se pehle web search ke natayej leti hai; sab agents ko source ke saath maloomat milti hai. `🌐 Browser` button se aap khud bhi search/URL khol sakte hain.
- **Social media:** Herald ka acha (7+ score) draft `Social queue` mein aata hai. **Aap ✅ dabayen tab hi** Telegram, Discord, Mastodon ya X par post hota hai.
- **Seekhna:** lessons aur XP browser mein save hote hain aur agle kaam ke prompt mein jaate hain. Agents apna code khud nahi badalte.
- **claude.ai wale artifact mein** internet aur social nahi hota (published page doosri sites se connect nahi ho sakta); AI wala hissa wahan chalta hai.

## Hifazat
- API keys kabhi source ya commit mein na dalein (`.env` ignore hai).
- Server: key zaroori, localhost/private addresses block (SSRF), rate limit, 6000 chars tak page text.

## GitHub par push
```
git add zaythar-office && git commit -m "Add Zaythar AI Office" && git push
```

## Backend (server/)
`/api/search`, `/api/fetch` (internet) | `/api/social/queue`, `/api/social/post` (manzoori ke baad post) | `/api/agents` | `/api/memory` (XP aur lessons server par save) | `/api/tasks` | `/api/notify` (Zaythar AI ke phone par Telegram message). Data `server/data/state.json` mein save hota hai (git mein nahi jata).

## Off-screen agents (page band ho tab bhi)
`.env` mein `ANTHROPIC_API_KEY` dein. `POST /api/task {text}` par Boss khud faisla karta hai kaun kaam kare, agents tools chalate hain (internet, repo padhna, CI checks, issues), Boss kaam parakhta hai, kamzor kaam dobara karwata hai, mentor tip deta hai aur Telegram par aap ko khulasa bhejta hai.
- **Repo management:** `GITHUB_TOKEN` + `GITHUB_REPO` dein. Friday, Neon aur Argon fix **propose** karte hain; aap `Approvals` mein ✅ dabayen tab hi naya branch + Pull Request banta hai. Main par seedha push kabhi nahi hota. `.env`, `google-services.json` aur keys padhna block hai.
- **Heartbeat:** `HEARTBEAT_MIN=60` rakhein to Boss har ghante CI aur issues dekhta hai (default band).
- **Hadd:** agents sirf apne allowed tools use kar sakte hain, ek waqt mein ek job, har agent max 4 tool steps. Ye sab 'bina nigrani ke apne aap sab kuch' nahi karte; asli tabdeeliyon ke liye aap ki manzoori zaroori hai.
- Endpoints: `/api/agents`, `/api/task`, `/api/jobs`, `/api/approvals`, `/api/memory`.

## Jarvis-style upgrades (repo ke `docs/JARVIS_EXECUTION_MODEL.md` ke mutabiq)
- **ToolRegistry + PermissionBroker:** har tool ke bounded args, timeout aur risk tier (read / reversible / consequential). Consequential kaam (`propose_patch`) kabhi seedha nahi chalta, pehle manzoori queue. Har tool call `GET /api/audit` mein darj hoti hai.
- **Seedhi baat:** page mein `friday build fail kyun hai?` likhein to wahi agent (apni shakhsiyat aur memory ke saath) jawab deta hai (`POST /api/chat`).
- **Memory ke controls:** 🧠 button se har agent ki yaaddasht dekhein aur ✖ se mitayein.
- **Consult:** agents mushkil sawal par doosre agent se mashwara le sakte hain.
- **Awaaz:** 🎤 se bolein, 🔊 se jawab sunein (default band, aap ke control mein).
- **Live status:** office ki left deewar par repo ki CI/issues/approvals ki live screen; server job chale to Boss phone dekhta hai aur khatam par agents haath hilate hain.
- **Hadd:** `MAX_JOBS_PER_DAY` se roz ke jobs ki hadd (kharch par qaboo).
- **Tests:** `npm test` (4 tests: allowlist, approval gate, secret-file block, memory isolation).
- **CI:** `ci/zaythar-office.yml` ko `.github/workflows/` mein copy karein. Push ke liye `PUSH.md` dekhein.
