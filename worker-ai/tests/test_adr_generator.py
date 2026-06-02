from app.analysis.adr_generator import AdrGenerator
from app.analysis.rules import RiskCategory, RiskFinding, RiskSeverity


def _finding(category, severity):
    return RiskFinding(
        category=category,
        severity=severity,
        title="t",
        description="d",
    )


def test_generates_adr_for_relevant_category_with_related_findings():
    findings = [
        _finding(RiskCategory.DESTRUCTIVE_MIGRATION, RiskSeverity.CRITICAL),
        _finding(RiskCategory.DESTRUCTIVE_MIGRATION, RiskSeverity.HIGH),
    ]
    adrs = AdrGenerator().generate_adrs(findings)

    assert len(adrs) == 1
    assert adrs[0].related_findings == [f.id for f in findings]
    assert adrs[0].status == "PROPOSED"


def test_low_severity_only_does_not_generate_adr():
    findings = [_finding(RiskCategory.EXCESSIVE_COUPLING, RiskSeverity.LOW)]
    assert AdrGenerator().generate_adrs(findings) == []


def test_one_adr_per_category():
    findings = [
        _finding(RiskCategory.DESTRUCTIVE_MIGRATION, RiskSeverity.CRITICAL),
        _finding(RiskCategory.SECURITY_RISK, RiskSeverity.HIGH),
        _finding(RiskCategory.MISSING_HEALTH_CHECK, RiskSeverity.MEDIUM),
    ]
    adrs = AdrGenerator().generate_adrs(findings)

    titles = {a.title for a in adrs}
    assert len(adrs) == 3
    assert len(titles) == 3


def test_empty_findings_yield_no_adrs():
    assert AdrGenerator().generate_adrs([]) == []
