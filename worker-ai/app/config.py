from pydantic_settings import BaseSettings


class Settings(BaseSettings):
    database_url: str = "postgresql://archlens:archlens@localhost:5432/archlens"
    embedding_provider: str = "fake"
    openai_api_key: str = ""
    ollama_base_url: str = "http://localhost:11434"
    embedding_model: str = "text-embedding-3-small"
    embedding_dimension: int = 1536
    storage_base_path: str = "/tmp/archlens/projects"
    log_level: str = "INFO"
    backend_url: str = "http://localhost:8080"

    model_config = {"env_file": ".env", "env_file_encoding": "utf-8"}


settings = Settings()
