# GitHub par daalne ka tareeqa (ChatGPT ya aap khud)

Repo ki `main` protected hai, is liye naya branch + Pull Request banayein (seedha main par push nahi hoga).

```
cd fluffy-umbrella
git checkout -b add-zaythar-office
# zip ko extract karke `zaythar-office/` folder repo ke root mein rakhein
mkdir -p .github/workflows && cp zaythar-office/ci/zaythar-office.yml .github/workflows/
git add zaythar-office .github/workflows/zaythar-office.yml
git commit -m "Add Zaythar AI Office (boss + 7 agents, backend, tests)"
git push -u origin add-zaythar-office
```
Phir GitHub par PR kholein. `server/.env` kabhi commit na karein (`.gitignore` mein hai); keys sirf `.env` mein.
Test: `cd zaythar-office/server && npm test` (Node 18+).
