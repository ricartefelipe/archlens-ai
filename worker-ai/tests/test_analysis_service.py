import asyncio
from uuid import uuid4

from app.analysis.rules import RiskCategory, RiskFinding, RiskSeverity
from app.service import analysis as analysis_module
from app.service.analysis import StaticAnalysisService


def test_build_summary_without_findings():
    summary = StaticAnalysisService()._build_summary([], 3)
    assert "Nenhum risco" in summary
    assert "3 arquivos" in summary


def test_build_summary_counts_by_severity():
    findings = [
        RiskFinding(category=RiskCategory.SECURITY_RISK, severity=RiskSeverity.CRITICAL, title="t", description="d"),
        RiskFinding(category=RiskCategory.SECURITY_RISK, severity=RiskSeverity.HIGH, title="t", description="d"),
    ]
    summary = StaticAnalysisService()._build_summary(findings, 1)

    assert "2 riscos" in summary
    assert "1 críticos" in summary
    assert "1 altos" in summary
    assert "SECURITY_RISK" in summary


def test_analyze_project_missing_dir(monkeypatch, tmp_path):
    monkeypatch.setattr(analysis_module.settings, "storage_base_path", str(tmp_path))
    result = asyncio.run(StaticAnalysisService().analyze_project(uuid4(), "tenant-1"))

    assert result.total_files_analyzed == 0
    assert result.findings == []
    assert "não encontrado" in result.summary


def test_analyze_project_scans_files_and_finds_risks(monkeypatch, tmp_path):
    project_id = uuid4()
    project_dir = tmp_path / str(project_id)
    project_dir.mkdir()
    (project_dir / "Dockerfile").write_text(
        "FROM python:latest\nRUN pip install fastapi\nCMD [\"python\", \"main.py\"]\n",
        encoding="utf-8",
    )

    monkeypatch.setattr(analysis_module.settings, "storage_base_path", str(tmp_path))
    result = asyncio.run(StaticAnalysisService().analyze_project(project_id, "tenant-1"))

    assert result.total_files_analyzed == 1
    assert result.findings
    categories = {f.category for f in result.findings}
    assert RiskCategory.SECURITY_RISK in categories
