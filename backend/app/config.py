"""Application configuration and environment settings."""

from typing import List
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "Zaythar AI Companion Backend"
    version: str = "1.0.0"
    environment: str = "development"
    host: str = "0.0.0.0"
    port: int = 8000
    cors_origins: str = "*"

    # Provider configurations (kept server-side only per Rule 2 and Rule 3)
    openai_api_key: str = ""
    gemini_api_key: str = ""
    ollama_base_url: str = "http://localhost:11434"
    default_ai_provider: str = "fallback"

    # Optional Stonic gateway; enabled only for OpenAI-compatible endpoints.
    stonic_base_url: str = ""
    stonic_api_key: str = ""
    stonic_model: str = "stonic-default"

    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    @property
    def cors_origins_list(self) -> List[str]:
        if self.cors_origins.strip() == "*":
            return ["*"]
        return [origin.strip() for origin in self.cors_origins.split(",") if origin.strip()]


settings = Settings()
