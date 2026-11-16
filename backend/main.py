import os
from typing import Optional
from fastapi import FastAPI, HTTPException
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel, Field
from openai import OpenAI

app = FastAPI(title="Zaythar AI API", version="1.0.0")
app.add_middleware(CORSMiddleware, allow_origins=["*"], allow_methods=["*"], allow_headers=["*"])

class ChatRequest(BaseModel):
    message: str = Field(min_length=1, max_length=12000)
    language: Optional[str] = "en-US"

@app.get("/api/health")
def health():
    return {"status": "ok", "service": "Zaythar AI", "ai_configured": bool(os.getenv("OPENAI_API_KEY"))}

@app.post("/api/chat")
def chat(body: ChatRequest):
    api_key = os.getenv("OPENAI_API_KEY")
    if not api_key:
        raise HTTPException(status_code=503, detail="AI provider is not configured. Set OPENAI_API_KEY on the server.")
    try:
        client = OpenAI(api_key=api_key)
        response = client.chat.completions.create(
            model=os.getenv("OPENAI_MODEL", "gpt-4o-mini"),
            messages=[
                {"role": "system", "content": "You are Zaythar AI, a helpful, safe, multilingual personal assistant. Reply in the language used by the user. Be clear and practical."},
                {"role": "user", "content": body.message}
            ],
            temperature=0.7,
        )
        return {"reply": response.choices[0].message.content or ""}
    except Exception as exc:
        raise HTTPException(status_code=502, detail=f"AI provider request failed: {type(exc).__name__}")

app.mount("/", StaticFiles(directory=".", html=True), name="web")
