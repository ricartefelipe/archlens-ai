from pydantic import AliasChoices, Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    database_url: str = "postgresql://archlens:archlens@localhost:5432/archlens"
    embedding_provider: str = "local"

    embedding_dimension: int = Field(
        default=1536,
        validation_alias=AliasChoices(
            "EMBEDDING_DIMENSION",
            "ARCHLENS_EMBEDDING_DIMENSION",
        ),
    )

    openai_api_key: str = ""
    ollama_base_url: str = "http://localhost:11434"
    embedding_model: str = "text-embedding-3-small"

    #: Se true, faz uma chamada de teste ao arranque e compara comprimento ao embedding_dimension.
    embedding_dimension_verify: bool = Field(
        default=True,
        validation_alias=AliasChoices(
            "EMBEDDING_DIMENSION_VERIFY",
            "ARCHLENS_EMBEDDING_DIMENSION_VERIFY",
        ),
    )

    storage_base_path: str = "/tmp/archlens/projects"
    log_level: str = "INFO"
    backend_url: str = "http://localhost:8080"


settings = Settings()
