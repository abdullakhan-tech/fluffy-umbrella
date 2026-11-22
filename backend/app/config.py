"""Application configuration and environment settings."""
from typing import List

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "Zaythar AI Companion Backend"
    version: str = "1.1.0"
    environment: str = "development"
    host: str = "127.0.0.1"
    port: int = 8000
    cors_origins: str = ""

    # Firebase Admin uses Application Default Credentials / workload identity.
    firebase_project_id: str = ""
    app_check_required: bool = True

    # Process-local limits are defense in depth, not a distributed quota.
    rate_limit_user_per_minute: int = 60
    rate_limit_ip_per_minute: int = 120
    max_request_body_bytes: int = 262144
    memory_db_path: str = "data/zaythar_memory.sqlite3"

    # Provider credentials remain server-side only.
    openai_api_key: str = ""
    gemini_api_key: str = ""
    ollama_base_url: str = "http://localhost:11434"
    default_ai_provider: str = "fallback"

    model_config = SettingsConfigDict(env_file=".env", extra="ignore")

    @property
    def cors_origins_list(self) -> List[str]:
        return [
            origin.strip()
            for origin in self.cors_origins.split(",")
            if origin.strip() and origin.strip() != "*"
        ]


settings = Settings()
