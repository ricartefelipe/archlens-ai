from app.analysis.analyzers import (
    AnalyzerFactory,
    DockerAnalyzer,
    DotNetAnalyzer,
    GoAnalyzer,
    JavaAnalyzer,
    KubernetesAnalyzer,
    MigrationAnalyzer,
    OpenApiAnalyzer,
    PipelineAnalyzer,
    PythonAnalyzer,
    TerraformAnalyzer,
    YamlConfigAnalyzer,
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
    content = "name: deploy\njobs:\n  build:\n    steps:\n      - run: mvn -B package\n"
    findings = PipelineAnalyzer().analyze(".github/workflows/deploy.yml", content)
    categories = _categories(findings)

    assert RiskCategory.MISSING_TEST_COVERAGE in categories
    assert RiskCategory.LACK_OF_OBSERVABILITY in categories


def test_pipeline_analyzer_ignores_placeholder_sample():
    content = "name: ci\non: [push]\njobs:\n  build:\n    steps:\n      - run: echo \"build placeholder — sample ArchLens\"\n"
    assert PipelineAnalyzer().analyze(".github/workflows/ci.yml", content) == []


def test_java_analyzer_flags_sql_concatenation():
    content = """
    public class OrderController {
        public void create(String email) throws Exception {
            conn.createStatement().execute(
                "INSERT INTO customers (id, email) VALUES ('" + id + "','" + email + "')");
        }
    }
    """
    findings = JavaAnalyzer().analyze("OrderController.java", content)
    assert any(f.title == "SQL montado por concatenação de strings" for f in findings)


def test_dotnet_analyzer_flags_connection_string():
    content = """
    public class DbConfig {
        public string ConnectionString = "Server=localhost;Database=app;User=sa;Password=secret";
    }
    """
    findings = DotNetAnalyzer().analyze("DbConfig.cs", content)
    categories = _categories(findings)

    assert RiskCategory.SECURITY_RISK in categories
    assert any(f.severity == RiskSeverity.CRITICAL for f in findings)


def test_dotnet_analyzer_flags_async_void():
    content = """
    public class Worker {
        public async void Process() {
            await Task.Delay(1);
        }
    }
    """
    findings = DotNetAnalyzer().analyze("Worker.cs", content)
    categories = _categories(findings)

    assert RiskCategory.CONTRACT_VIOLATION in categories


def test_dotnet_analyzer_flags_fat_controller():
    methods = "\n".join(
        f"        public IActionResult Action{i}() {{ return Ok(); }}" for i in range(9)
    )
    content = f"""
    [ApiController]
    public class OrdersController {{
{methods}
    }}
    """
    findings = DotNetAnalyzer().analyze("OrdersController.cs", content)
    categories = _categories(findings)

    assert RiskCategory.EXCESSIVE_COUPLING in categories


def test_dotnet_analyzer_clean_class_has_no_findings():
    content = """
    public class Money {
        private readonly decimal _amount;
        public Money(decimal amount) { _amount = amount; }
        public decimal Amount => _amount;
    }
    """
    assert DotNetAnalyzer().analyze("Money.cs", content) == []


def test_yaml_config_analyzer_flags_secrets_and_open_cors():
    content = """
spring:
  datasource:
    password: super-secret-db
  cors:
    allowed-origins: "*"
"""
    findings = YamlConfigAnalyzer().analyze("application.yml", content)
    categories = _categories(findings)

    assert RiskCategory.SECURITY_RISK in categories
    assert any(f.severity == RiskSeverity.CRITICAL for f in findings)


def test_analyzer_factory_dispatch():
    assert any(isinstance(a, JavaAnalyzer) for a in AnalyzerFactory.get_analyzers("App.java"))
    assert any(isinstance(a, DotNetAnalyzer) for a in AnalyzerFactory.get_analyzers("Program.cs"))
    assert any(isinstance(a, MigrationAnalyzer) for a in AnalyzerFactory.get_analyzers("001.sql"))
    assert any(isinstance(a, DockerAnalyzer) for a in AnalyzerFactory.get_analyzers("Dockerfile"))
    assert any(isinstance(a, OpenApiAnalyzer) for a in AnalyzerFactory.get_analyzers("api/openapi.yaml"))
    assert any(isinstance(a, PipelineAnalyzer) for a in AnalyzerFactory.get_analyzers(".github/workflows/ci.yml"))
    assert any(isinstance(a, TerraformAnalyzer) for a in AnalyzerFactory.get_analyzers("infra/main.tf"))
    assert any(isinstance(a, TerraformAnalyzer) for a in AnalyzerFactory.get_analyzers("env/prod.tfvars"))
    assert any(isinstance(a, KubernetesAnalyzer) for a in AnalyzerFactory.get_analyzers("k8s/deployment.yaml"))
    assert any(isinstance(a, PythonAnalyzer) for a in AnalyzerFactory.get_analyzers("app/main.py"))
    assert any(isinstance(a, GoAnalyzer) for a in AnalyzerFactory.get_analyzers("cmd/server/main.go"))
    assert any(isinstance(a, TypeScriptAnalyzer) for a in AnalyzerFactory.get_analyzers("src/App.tsx"))
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


def test_python_analyzer_flags_eval_and_secrets():
    content = """
API_KEY = "sk-live-1234567890"

def run(user_input):
    eval(user_input)
    cursor.execute(f"SELECT * FROM users WHERE id = {user_id}")
"""
    findings = PythonAnalyzer().analyze("app/service.py", content)
    categories = _categories(findings)

    assert RiskCategory.SECURITY_RISK in categories
    assert any(f.severity == RiskSeverity.CRITICAL for f in findings)


def test_python_analyzer_clean_module():
    content = """
import os

def get_key() -> str:
    return os.environ["API_KEY"]
"""
    assert PythonAnalyzer().analyze("app/config.py", content) == []


def test_go_analyzer_flags_secrets_and_sql():
    content = """
package main

const apiKey = "super-secret-token"

func query(db *sql.DB, id string) {
    db.Query(fmt.Sprintf("SELECT * FROM users WHERE id = %s", id))
    http.ListenAndServe(":8080", nil)
}
"""
    findings = GoAnalyzer().analyze("cmd/main.go", content)
    categories = _categories(findings)

    assert RiskCategory.SECURITY_RISK in categories
    assert any(f.severity == RiskSeverity.CRITICAL for f in findings)


def test_typescript_analyzer_flags_xss_and_secrets():
    content = """
const apiKey = "sk-frontend-leaked-key-123456";

export function Widget({ html }: { html: string }) {
  return <div dangerouslySetInnerHTML={{ __html: html }} />;
}
"""
    findings = TypeScriptAnalyzer().analyze("src/Widget.tsx", content)
    categories = _categories(findings)

    assert RiskCategory.SECURITY_RISK in categories
    assert any(f.severity == RiskSeverity.CRITICAL for f in findings)
