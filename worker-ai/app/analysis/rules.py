from enum import Enum
from uuid import UUID, uuid4

from pydantic import BaseModel, Field


class RiskCategory(str, Enum):
    LACK_OF_OBSERVABILITY = "LACK_OF_OBSERVABILITY"
    EXCESSIVE_COUPLING = "EXCESSIVE_COUPLING"
    MISSING_CORRELATION_ID = "MISSING_CORRELATION_ID"
    LAYER_SEPARATION_ISSUE = "LAYER_SEPARATION_ISSUE"
    CONTRACT_VIOLATION = "CONTRACT_VIOLATION"
    DESTRUCTIVE_MIGRATION = "DESTRUCTIVE_MIGRATION"
    MISSING_HEALTH_CHECK = "MISSING_HEALTH_CHECK"
    MISSING_IDEMPOTENCY = "MISSING_IDEMPOTENCY"
    SECURITY_RISK = "SECURITY_RISK"
    MISSING_TEST_COVERAGE = "MISSING_TEST_COVERAGE"


class RiskSeverity(str, Enum):
    CRITICAL = "CRITICAL"
    HIGH = "HIGH"
    MEDIUM = "MEDIUM"
    LOW = "LOW"


class RiskFinding(BaseModel):
    id: UUID = Field(default_factory=uuid4)
    category: RiskCategory
    severity: RiskSeverity
    title: str
    description: str
    file_path: str | None = None
    evidence: str | None = None
    suggestion: str | None = None


class AnalysisResult(BaseModel):
    project_id: UUID
    summary: str
    findings: list[RiskFinding]
    total_files_analyzed: int


class AdrSuggestion(BaseModel):
    id: UUID = Field(default_factory=uuid4)
    title: str
    context: str
    decision: str
    consequences: str
    status: str = "PROPOSED"
    related_findings: list[UUID] = Field(default_factory=list)
