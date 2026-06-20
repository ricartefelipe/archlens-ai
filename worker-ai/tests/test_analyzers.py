from app.analysis.analyzers import (
    AnalyzerFactory,
    DockerAnalyzer,
    JavaAnalyzer,
    KubernetesAnalyzer,
    MigrationAnalyzer,
    OpenApiAnalyzer,
    PipelineAnalyzer,
    TerraformAnalyzer,
)
from app.analysis.rules import RiskCategory, RiskSeverity


def _categories(findings):
    return {f.category for f in findings}


def test_java_analyzer_flags_fat_controller():
    content = """
    @RestController
    public class OrderController {
        @Inject
        private OrderRepository repository;

        public String a() { if (x) {} return repository.findAll(); }
        public String b() { if (y) {} }
        public String c() { for (int i=0;i<3;i++) {} }
        public String d() { while (z) {} }
        public String e() {}
        public String f() {}
    }
    """
    findings = JavaAnalyzer().analyze("OrderController.java", content)
    categories = _categories(findings)

    assert RiskCategory.EXCESSIVE_COUPLING in categories
    assert RiskCategory.LAYER_SEPARATION_ISSUE in categories
    assert findings, "esperava ao menos um achado para controller gordo"


def test_java_analyzer_clean_class_has_no_findings():
    content = """
    public class Money {
        private final long cents;
        public Money(long cents) { this.cents = cents; }
        public long cents() { return cents; }
    }
    """
    assert JavaAnalyzer().analyze("Money.java", content) == []


def test_migration_analyzer_flags_destructive_statements():
    content = """
    DROP TABLE legacy_orders;
    ALTER TABLE users DROP COLUMN ssn;
    DELETE FROM audit_log;
    """
    findings = MigrationAnalyzer().analyze("001.sql", content)

    assert findings
    assert all(f.category == RiskCategory.DESTRUCTIVE_MIGRATION for f in findings)
    assert any(f.severity == RiskSeverity.CRITICAL for f in findings)


def test_migration_analyzer_ignores_scoped_statements():
    content = "DROP TABLE IF EXISTS tmp;\nDELETE FROM audit_log WHERE id = 1;\n"
    assert MigrationAnalyzer().analyze("002.sql", content) == []


def test_docker_analyzer_flags_missing_user_healthcheck_and_latest_tag():
    content = "FROM python:latest\nRUN pip install fastapi\nCMD [\"python\", \"main.py\"]\n"
    findings = DockerAnalyzer().analyze("Dockerfile", content)
    categories = _categories(findings)

    assert RiskCategory.MISSING_HEALTH_CHECK in categories
    assert RiskCategory.SECURITY_RISK in categories


def test_openapi_analyzer_flags_missing_errors_and_security():
    content = "openapi: 3.0.0\npaths:\n  /orders:\n    get:\n      responses:\n        '200':\n          description: ok\n"
    findings = OpenApiAnalyzer().analyze("openapi.yaml", content)
    categories = _categories(findings)

    assert RiskCategory.CONTRACT_VIOLATION in categories
    assert RiskCategory.SECURITY_RISK in categories


def test_pipeline_analyzer_flags_missing_stages():
    content = "name: deploy\njobs:\n  build:\n    steps:\n      - run: echo build\n"
    findings = PipelineAnalyzer().analyze(".github/workflows/deploy.yml", content)
    categories = _categories(findings)

    assert RiskCategory.MISSING_TEST_COVERAGE in categories
    assert RiskCategory.SECURITY_RISK in categories
    assert RiskCategory.LACK_OF_OBSERVABILITY in categories


def test_analyzer_factory_dispatch():
    assert any(isinstance(a, JavaAnalyzer) for a in AnalyzerFactory.get_analyzers("App.java"))
    assert any(isinstance(a, MigrationAnalyzer) for a in AnalyzerFactory.get_analyzers("001.sql"))
    assert any(isinstance(a, DockerAnalyzer) for a in AnalyzerFactory.get_analyzers("Dockerfile"))
    assert any(isinstance(a, OpenApiAnalyzer) for a in AnalyzerFactory.get_analyzers("api/openapi.yaml"))
    assert any(isinstance(a, PipelineAnalyzer) for a in AnalyzerFactory.get_analyzers(".github/workflows/ci.yml"))
    assert any(isinstance(a, TerraformAnalyzer) for a in AnalyzerFactory.get_analyzers("infra/main.tf"))
    assert any(isinstance(a, TerraformAnalyzer) for a in AnalyzerFactory.get_analyzers("env/prod.tfvars"))
    assert any(isinstance(a, KubernetesAnalyzer) for a in AnalyzerFactory.get_analyzers("k8s/deployment.yaml"))
    assert AnalyzerFactory.get_analyzers("README.md") == []


def test_kubernetes_analyzer_flags_security_and_health_gaps():
    content = """
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api
  namespace: default
spec:
  template:
    spec:
      hostNetwork: true
      containers:
        - name: api
          image: nginx:latest
          privileged: true
          env:
            - name: DB_PASSWORD
              value: super-secret
"""
    findings = KubernetesAnalyzer().analyze("k8s/deployment.yaml", content)
    categories = _categories(findings)

    assert RiskCategory.SECURITY_RISK in categories
    assert RiskCategory.MISSING_HEALTH_CHECK in categories
    assert any(f.severity == RiskSeverity.CRITICAL for f in findings)


def test_kubernetes_analyzer_ignores_non_manifest_yaml():
    content = "server:\n  port: 8080\nspring:\n  profiles: prod\n"
    assert KubernetesAnalyzer().analyze("config/app.yaml", content) == []


def test_kubernetes_analyzer_clean_manifest():
    content = """
apiVersion: apps/v1
kind: Deployment
metadata:
  name: api
  namespace: platform
spec:
  template:
    spec:
      securityContext:
        runAsNonRoot: true
        runAsUser: 10001
      containers:
        - name: api
          image: ghcr.io/org/api:1.2.3
          resources:
            requests:
              cpu: 100m
              memory: 128Mi
            limits:
              cpu: 500m
              memory: 512Mi
          livenessProbe:
            httpGet:
              path: /health
              port: 8080
          readinessProbe:
            httpGet:
              path: /ready
              port: 8080
          env:
            - name: DB_PASSWORD
              valueFrom:
                secretKeyRef:
                  name: api-secrets
                  key: password
"""
    findings = KubernetesAnalyzer().analyze("k8s/deployment.yaml", content)
    critical = [f for f in findings if f.severity == RiskSeverity.CRITICAL]

    assert critical == []


def test_terraform_analyzer_flags_security_and_contract_issues():
    content = """
terraform {
  backend "local" {
    path = "terraform.tfstate"
  }
}

provider "aws" {
  region = "us-east-1"
}

module "vpc" {
  source = "terraform-aws-modules/vpc/aws"
}

resource "aws_security_group" "web" {
  ingress {
    cidr_blocks = ["0.0.0.0/0"]
  }
}

db_password = "super-secret-123"
"""
    findings = TerraformAnalyzer().analyze("main.tf", content)
    categories = _categories(findings)

    assert RiskCategory.SECURITY_RISK in categories
    assert RiskCategory.CONTRACT_VIOLATION in categories
    assert any(f.severity == RiskSeverity.CRITICAL for f in findings)


def test_terraform_analyzer_clean_config():
    content = """
terraform {
  backend "s3" {
    bucket = "tf-state"
    key    = "prod"
    region = "us-east-1"
  }
}

terraform {
  required_providers {
    aws = {
      source  = "hashicorp/aws"
      version = "~> 5.0"
    }
  }
}

module "vpc" {
  source  = "terraform-aws-modules/vpc/aws"
  version = "5.1.0"
}

resource "aws_instance" "app" {
  tags = {
    Environment = "prod"
    Owner       = "platform"
  }
}
"""
    findings = TerraformAnalyzer().analyze("main.tf", content)
    security_findings = [f for f in findings if f.category == RiskCategory.SECURITY_RISK]

    assert security_findings == []
