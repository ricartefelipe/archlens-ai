from pathlib import Path
from uuid import UUID

import structlog

from app.analysis.adr_generator import AdrGenerator
from app.analysis.analyzers import AnalyzerFactory
from app.analysis.rules import AdrSuggestion, AnalysisResult, RiskFinding
from app.config import settings

log = structlog.get_logger()


class StaticAnalysisService:
    def __init__(self) -> None:
        self._adr_generator = AdrGenerator()

    async def analyze_project(self, project_id: UUID, tenant_id: str) -> AnalysisResult:
        base = Path(settings.storage_base_path) / str(project_id)
        log.info("analysis_start", project_id=str(project_id), base_path=str(base))

        if not base.exists():
            log.warning("project_dir_not_found", project_id=str(project_id), path=str(base))
            return AnalysisResult(
                project_id=project_id,
                summary="Diretório do projeto não encontrado. Execute o upload antes da análise.",
                findings=[],
                total_files_analyzed=0,
            )

        all_files = [p for p in base.rglob("*") if p.is_file()]
        all_findings: list[RiskFinding] = []
        files_analyzed = 0

        for file_path in all_files:
            relative = str(file_path.relative_to(base))
            analyzers = AnalyzerFactory.get_analyzers(relative)

            if not analyzers:
                continue

            try:
                content = file_path.read_text(encoding="utf-8", errors="replace")
            except Exception:
                log.warning("file_read_error", file_path=relative)
                continue

            for analyzer in analyzers:
                findings = analyzer.analyze(relative, content)
                all_findings.extend(findings)

            files_analyzed += 1

        summary = self._build_summary(all_findings, files_analyzed)

        log.info(
            "analysis_complete",
            project_id=str(project_id),
            files_analyzed=files_analyzed,
            findings=len(all_findings),
        )

        return AnalysisResult(
            project_id=project_id,
            summary=summary,
            findings=all_findings,
            total_files_analyzed=files_analyzed,
        )

    async def generate_adrs(self, findings: list[RiskFinding]) -> list[AdrSuggestion]:
        return self._adr_generator.generate_adrs(findings)

    def _build_summary(self, findings: list[RiskFinding], files_analyzed: int) -> str:
        if not findings:
            return f"Análise estática de {files_analyzed} arquivos concluída. Nenhum risco arquitetural identificado."

        critical = sum(1 for f in findings if f.severity.value == "CRITICAL")
        high = sum(1 for f in findings if f.severity.value == "HIGH")
        medium = sum(1 for f in findings if f.severity.value == "MEDIUM")
        low = sum(1 for f in findings if f.severity.value == "LOW")

        categories = set(f.category.value for f in findings)

        parts = [
            f"Análise estática de {files_analyzed} arquivos concluída.",
            f"Foram identificados {len(findings)} riscos arquiteturais:",
        ]
        severity_parts = []
        if critical > 0:
            severity_parts.append(f"{critical} críticos")
        if high > 0:
            severity_parts.append(f"{high} altos")
        if medium > 0:
            severity_parts.append(f"{medium} médios")
        if low > 0:
            severity_parts.append(f"{low} baixos")
        parts.append(", ".join(severity_parts) + ".")

        parts.append(f"Categorias afetadas: {', '.join(sorted(categories))}.")

        return " ".join(parts)
