import re
from abc import ABC, abstractmethod

import structlog

from app.analysis.rules import RiskCategory, RiskFinding, RiskSeverity

log = structlog.get_logger()


class BaseAnalyzer(ABC):
    @abstractmethod
    def analyze(self, file_path: str, content: str) -> list[RiskFinding]:
        ...


class JavaAnalyzer(BaseAnalyzer):
    _CONTROLLER_PATTERN = re.compile(
        r"@(?:RestController|Controller|Path\s*\(|RequestMapping)", re.MULTILINE
    )
    _ENTITY_PATTERN = re.compile(r"@Entity\b", re.MULTILINE)
    _INJECT_FIELD = re.compile(r"^\s+@(?:Inject|Autowired)\s*$", re.MULTILINE)
    _METHOD_PATTERN = re.compile(
        r"(?:public|protected|private)\s+\w[\w<>,\s]*\s+\w+\s*\(", re.MULTILINE
    )
    _COMPLEX_LOGIC = re.compile(
        r"(?:if\s*\(|switch\s*\(|for\s*\(|while\s*\()", re.MULTILINE
    )
    _RETURN_ENTITY = re.compile(
        r"return\s+\w*(entity|Entity|repository\.\w+)", re.MULTILINE
    )

    def analyze(self, file_path: str, content: str) -> list[RiskFinding]:
        findings: list[RiskFinding] = []
        is_controller = bool(self._CONTROLLER_PATTERN.search(content))

        if is_controller:
            methods = self._METHOD_PATTERN.findall(content)
            lines = content.split("\n")
            if len(methods) > 5:
                findings.append(RiskFinding(
                    category=RiskCategory.EXCESSIVE_COUPLING,
                    severity=RiskSeverity.MEDIUM,
                    title="Controller com muitos métodos",
                    description=f"Controller possui {len(methods)} métodos. Controllers devem ser enxutos, delegando lógica para use cases.",
                    file_path=file_path,
                    evidence=f"{len(methods)} métodos públicos encontrados",
                    suggestion="Dividir em múltiplos controllers ou extrair lógica para serviços dedicados",
                ))

            if len(lines) > 200:
                findings.append(RiskFinding(
                    category=RiskCategory.EXCESSIVE_COUPLING,
                    severity=RiskSeverity.HIGH,
                    title="Controller excessivamente grande",
                    description=f"Controller possui {len(lines)} linhas, indicando responsabilidades demais.",
                    file_path=file_path,
                    evidence=f"{len(lines)} linhas de código",
                    suggestion="Refatorar para controllers menores com responsabilidade única",
                ))

            logic_count = len(self._COMPLEX_LOGIC.findall(content))
            if logic_count > 3:
                findings.append(RiskFinding(
                    category=RiskCategory.LAYER_SEPARATION_ISSUE,
                    severity=RiskSeverity.HIGH,
                    title="Lógica de negócio no controller",
                    description="Controller contém lógica complexa que deveria estar em um use case.",
                    file_path=file_path,
                    evidence=f"{logic_count} estruturas de controle (if/switch/for) encontradas",
                    suggestion="Mover lógica de negócio para classes de serviço/use case",
                ))

            if self._RETURN_ENTITY.search(content):
                findings.append(RiskFinding(
                    category=RiskCategory.LAYER_SEPARATION_ISSUE,
                    severity=RiskSeverity.MEDIUM,
                    title="Entidade retornada diretamente pelo controller",
                    description="Entidades JPA sendo retornadas na API, criando acoplamento entre persistência e interface.",
                    file_path=file_path,
                    evidence="return com referência a entity/repository encontrado",
                    suggestion="Usar DTOs/Records para respostas da API",
                ))

        field_injects = self._INJECT_FIELD.findall(content)
        if len(field_injects) > 0 and "@RequiredArgsConstructor" not in content:
            findings.append(RiskFinding(
                category=RiskCategory.EXCESSIVE_COUPLING,
                severity=RiskSeverity.LOW,
                title="Injeção de dependência por campo",
                description="Uso de @Inject/@Autowired em campos. Injeção por construtor facilita testes.",
                file_path=file_path,
                evidence=f"{len(field_injects)} injeções por campo encontradas",
                suggestion="Migrar para injeção por construtor",
            ))

        return findings


class MigrationAnalyzer(BaseAnalyzer):
    _DROP_NO_IF_EXISTS = re.compile(
        r"DROP\s+(?:TABLE|COLUMN|INDEX)\s+(?!IF\s+EXISTS)", re.IGNORECASE | re.MULTILINE
    )
    _BULK_UPDATE = re.compile(
        r"(?:UPDATE|DELETE\s+FROM)\s+\w+\s*(?:;|$)", re.IGNORECASE | re.MULTILINE
    )
    _ALTER_DROP_COLUMN = re.compile(
        r"ALTER\s+TABLE\s+\w+\s+DROP\s+COLUMN", re.IGNORECASE | re.MULTILINE
    )

    def analyze(self, file_path: str, content: str) -> list[RiskFinding]:
        findings: list[RiskFinding] = []

        for match in self._DROP_NO_IF_EXISTS.finditer(content):
            findings.append(RiskFinding(
                category=RiskCategory.DESTRUCTIVE_MIGRATION,
                severity=RiskSeverity.CRITICAL,
                title="DROP sem IF EXISTS",
                description="Operação destrutiva sem verificação de existência. Pode falhar em execuções repetidas.",
                file_path=file_path,
                evidence=match.group(0).strip()[:100],
                suggestion="Adicionar IF EXISTS à operação DROP",
            ))

        for match in self._ALTER_DROP_COLUMN.finditer(content):
            findings.append(RiskFinding(
                category=RiskCategory.DESTRUCTIVE_MIGRATION,
                severity=RiskSeverity.HIGH,
                title="Remoção de coluna detectada",
                description="DROP COLUMN pode causar perda de dados e quebrar aplicações que dependem da coluna.",
                file_path=file_path,
                evidence=match.group(0).strip()[:100],
                suggestion="Considerar deprecação gradual em vez de remoção imediata",
            ))

        for match in self._BULK_UPDATE.finditer(content):
            stmt = match.group(0).strip()
            if "WHERE" not in stmt.upper():
                findings.append(RiskFinding(
                    category=RiskCategory.DESTRUCTIVE_MIGRATION,
                    severity=RiskSeverity.HIGH,
                    title="UPDATE/DELETE sem WHERE",
                    description="Operação em massa sem cláusula WHERE pode afetar todos os registros.",
                    file_path=file_path,
                    evidence=stmt[:100],
                    suggestion="Adicionar cláusula WHERE para limitar o escopo da operação",
                ))

        return findings


class DockerAnalyzer(BaseAnalyzer):
    _HEALTHCHECK = re.compile(r"HEALTHCHECK\s", re.IGNORECASE | re.MULTILINE)
    _USER_INSTRUCTION = re.compile(r"^USER\s", re.MULTILINE)
    _LATEST_TAG = re.compile(r"FROM\s+\S+:latest\b", re.IGNORECASE | re.MULTILINE)
    _NO_TAG = re.compile(r"FROM\s+(\S+)\s*$", re.MULTILINE)

    def analyze(self, file_path: str, content: str) -> list[RiskFinding]:
        findings: list[RiskFinding] = []
        is_dockerfile = file_path.lower().endswith("dockerfile") or "/dockerfile" in file_path.lower()

        if is_dockerfile:
            if not self._HEALTHCHECK.search(content):
                findings.append(RiskFinding(
                    category=RiskCategory.MISSING_HEALTH_CHECK,
                    severity=RiskSeverity.MEDIUM,
                    title="Dockerfile sem HEALTHCHECK",
                    description="Sem instrução HEALTHCHECK, o orquestrador não sabe se o container está saudável.",
                    file_path=file_path,
                    evidence="Nenhuma instrução HEALTHCHECK encontrada",
                    suggestion="Adicionar HEALTHCHECK com verificação HTTP ou processo",
                ))

            if not self._USER_INSTRUCTION.search(content):
                findings.append(RiskFinding(
                    category=RiskCategory.SECURITY_RISK,
                    severity=RiskSeverity.HIGH,
                    title="Container rodando como root",
                    description="Sem instrução USER, o container executa como root, representando risco de segurança.",
                    file_path=file_path,
                    evidence="Nenhuma instrução USER encontrada",
                    suggestion="Adicionar USER não-root ao Dockerfile",
                ))

            if self._LATEST_TAG.search(content):
                findings.append(RiskFinding(
                    category=RiskCategory.SECURITY_RISK,
                    severity=RiskSeverity.MEDIUM,
                    title="Uso de tag :latest",
                    description="Tag :latest não garante reprodutibilidade e pode introduzir mudanças inesperadas.",
                    file_path=file_path,
                    evidence="FROM com tag :latest detectado",
                    suggestion="Fixar versão específica da imagem base",
                ))

        if "docker-compose" in file_path.lower() or "compose.y" in file_path.lower():
            if "healthcheck" not in content.lower():
                findings.append(RiskFinding(
                    category=RiskCategory.MISSING_HEALTH_CHECK,
                    severity=RiskSeverity.MEDIUM,
                    title="Compose sem health checks",
                    description="Serviços no docker-compose sem health checks definidos.",
                    file_path=file_path,
                    evidence="Nenhum bloco healthcheck encontrado",
                    suggestion="Adicionar healthcheck para cada serviço",
                ))

        return findings


class OpenApiAnalyzer(BaseAnalyzer):
    _PATH_BLOCK = re.compile(r"(?:paths:.*?)(?=\Z|\ninfo:|\ncomponents:)", re.DOTALL)
    _ERROR_RESPONSES = re.compile(r"['\"]?(?:4\d{2}|5\d{2})['\"]?\s*:", re.MULTILINE)
    _SECURITY = re.compile(r"security(?:Schemes|:)", re.IGNORECASE | re.MULTILINE)

    def analyze(self, file_path: str, content: str) -> list[RiskFinding]:
        findings: list[RiskFinding] = []

        if "paths:" in content and not self._ERROR_RESPONSES.search(content):
            findings.append(RiskFinding(
                category=RiskCategory.CONTRACT_VIOLATION,
                severity=RiskSeverity.MEDIUM,
                title="OpenAPI sem respostas de erro",
                description="Especificação OpenAPI não define respostas 4xx/5xx para os endpoints.",
                file_path=file_path,
                evidence="Nenhuma resposta de erro (4xx/5xx) encontrada",
                suggestion="Adicionar respostas 400, 404 e 500 aos endpoints",
            ))

        if "paths:" in content and not self._SECURITY.search(content):
            findings.append(RiskFinding(
                category=RiskCategory.SECURITY_RISK,
                severity=RiskSeverity.HIGH,
                title="OpenAPI sem security scheme",
                description="Especificação não define esquemas de segurança.",
                file_path=file_path,
                evidence="Nenhum securitySchemes ou security encontrado",
                suggestion="Adicionar securitySchemes (Bearer, OAuth2, API Key)",
            ))

        return findings


class PipelineAnalyzer(BaseAnalyzer):
    _TEST_STAGE = re.compile(r"(?:test|tests|testing|pytest|junit|maven.*test|npm.*test)", re.IGNORECASE)
    _SECURITY_SCAN = re.compile(r"(?:sonar|snyk|trivy|codeql|sast|dast|security.scan)", re.IGNORECASE)
    _CACHE = re.compile(r"cache", re.IGNORECASE)

    def analyze(self, file_path: str, content: str) -> list[RiskFinding]:
        findings: list[RiskFinding] = []

        if not self._TEST_STAGE.search(content):
            findings.append(RiskFinding(
                category=RiskCategory.MISSING_TEST_COVERAGE,
                severity=RiskSeverity.HIGH,
                title="Pipeline sem etapa de testes",
                description="Pipeline de CI/CD não possui etapa de execução de testes.",
                file_path=file_path,
                evidence="Nenhuma referência a execução de testes encontrada",
                suggestion="Adicionar etapa de testes (unit, integration) ao pipeline",
            ))

        if not self._SECURITY_SCAN.search(content):
            findings.append(RiskFinding(
                category=RiskCategory.SECURITY_RISK,
                severity=RiskSeverity.MEDIUM,
                title="Pipeline sem scan de segurança",
                description="Pipeline não inclui análise de segurança (SAST/DAST).",
                file_path=file_path,
                evidence="Nenhuma ferramenta de security scan encontrada",
                suggestion="Adicionar Sonar, Snyk, Trivy ou CodeQL ao pipeline",
            ))

        if not self._CACHE.search(content):
            findings.append(RiskFinding(
                category=RiskCategory.LACK_OF_OBSERVABILITY,
                severity=RiskSeverity.LOW,
                title="Pipeline sem cache de artefatos",
                description="Pipeline não utiliza cache, resultando em builds mais lentos.",
                file_path=file_path,
                evidence="Nenhuma configuração de cache encontrada",
                suggestion="Adicionar cache de dependências (Maven/Gradle/npm/pip)",
            ))

        return findings


class TerraformAnalyzer(BaseAnalyzer):
    _LOCAL_BACKEND = re.compile(r'backend\s+"local"', re.MULTILINE)
    _TERRAFORM_BLOCK = re.compile(r"^\s*terraform\s*\{", re.MULTILINE)
    _PROVIDER_BLOCK = re.compile(r'^\s*provider\s+"[\w-]+"\s*\{', re.MULTILINE)
    _REQUIRED_PROVIDERS = re.compile(r"^\s*required_providers\s*\{", re.MULTILINE)
    _MODULE_BLOCK = re.compile(r'module\s+"[^"]+"\s*\{', re.MULTILINE)
    _RESOURCE_OR_MODULE = re.compile(
        r"^\s*(?:resource|module)\s+\"[^\"]+\"\s+\"[^\"]+\"\s*\{", re.MULTILINE
    )
    _HARDCODED_SECRET = re.compile(
        r"(?i)(password|secret|api_key|access_key|token|private_key|client_secret)\s*=\s*\"(?!\$\{)(?!\$\()([^\"]{3,})\"",
        re.MULTILINE,
    )
    _OPEN_CIDR = re.compile(
        r"(?i)(?:cidr_blocks|cidr_block|source_address_prefix(?:es)?)\s*=\s*\[[^\]]*0\.0\.0\.0/0[^\]]*\]",
        re.MULTILINE,
    )
    _AWS_RESOURCE = re.compile(
        r'^\s*resource\s+"aws_[^"]+"\s+"[^"]+"\s*\{', re.MULTILINE
    )
    _TAGS_BLOCK = re.compile(r"^\s*tags\s*=\s*\{", re.MULTILINE)

    _MONOLITH_LINE_THRESHOLD = 250
    _MONOLITH_BLOCK_THRESHOLD = 12

    def analyze(self, file_path: str, content: str) -> list[RiskFinding]:
        findings: list[RiskFinding] = []
        lower_path = file_path.lower()
        is_tfvars = lower_path.endswith(".tfvars")

        if self._LOCAL_BACKEND.search(content):
            findings.append(RiskFinding(
                category=RiskCategory.SECURITY_RISK,
                severity=RiskSeverity.HIGH,
                title="Backend de state local",
                description="State Terraform em disco local impede colaboração segura e locking remoto.",
                file_path=file_path,
                evidence='backend "local" detectado',
                suggestion="Migrar para backend remoto (S3+DynamoDB, GCS, Azure Blob, Terraform Cloud)",
            ))

        for match in self._HARDCODED_SECRET.finditer(content):
            findings.append(RiskFinding(
                category=RiskCategory.SECURITY_RISK,
                severity=RiskSeverity.CRITICAL,
                title="Credencial hardcoded em Terraform",
                description="Segredos em plain text no repositório expõem a infraestrutura.",
                file_path=file_path,
                evidence=match.group(0).strip()[:100],
                suggestion="Usar variáveis sensíveis, Vault ou secrets do CI/CD (TF_VAR_ / remote backend)",
            ))

        for match in self._OPEN_CIDR.finditer(content):
            findings.append(RiskFinding(
                category=RiskCategory.SECURITY_RISK,
                severity=RiskSeverity.HIGH,
                title="Regra de rede aberta (0.0.0.0/0)",
                description="Exposição pública ampla em security group ou firewall.",
                file_path=file_path,
                evidence=match.group(0).strip()[:100],
                suggestion="Restringir CIDR às redes corporativas ou usar security groups de origem específica",
            ))

        if not is_tfvars:
            findings.extend(self._analyze_modules(file_path, content))
            findings.extend(self._analyze_providers(file_path, content))
            findings.extend(self._analyze_structure(file_path, content))
            findings.extend(self._analyze_missing_tags(file_path, content))

        return findings

    def _analyze_modules(self, file_path: str, content: str) -> list[RiskFinding]:
        findings: list[RiskFinding] = []
        for match in self._MODULE_BLOCK.finditer(content):
            snippet = content[match.start(): match.start() + 900]
            if "source" not in snippet:
                continue
            if "version" in snippet:
                continue
            if re.search(r'source\s*=\s*"[^"]*\.git"', snippet):
                continue
            if re.search(r'source\s*=\s*"\./', snippet) or re.search(r'source\s*=\s*"\.\./', snippet):
                continue
            findings.append(RiskFinding(
                category=RiskCategory.CONTRACT_VIOLATION,
                severity=RiskSeverity.MEDIUM,
                title="Módulo Terraform sem versão fixada",
                description="Módulos de registry sem constraint de versão podem quebrar deploys reprodutíveis.",
                file_path=file_path,
                evidence=match.group(0).strip()[:80],
                suggestion='Adicionar version = "x.y.z" ao bloco module',
            ))
        return findings

    def _analyze_providers(self, file_path: str, content: str) -> list[RiskFinding]:
        if not self._PROVIDER_BLOCK.search(content):
            return []
        if self._REQUIRED_PROVIDERS.search(content):
            return []
        return [RiskFinding(
            category=RiskCategory.CONTRACT_VIOLATION,
            severity=RiskSeverity.MEDIUM,
            title="Provider sem required_providers",
            description="Versões de provider não pinadas aumentam risco de drift entre ambientes.",
            file_path=file_path,
            evidence="provider { } sem bloco required_providers no mesmo arquivo",
            suggestion="Centralizar required_providers com version/source em versions.tf",
        )]

    def _analyze_structure(self, file_path: str, content: str) -> list[RiskFinding]:
        lines = content.splitlines()
        block_count = len(self._RESOURCE_OR_MODULE.findall(content))
        if len(lines) >= self._MONOLITH_LINE_THRESHOLD and block_count >= self._MONOLITH_BLOCK_THRESHOLD:
            return [RiskFinding(
                category=RiskCategory.EXCESSIVE_COUPLING,
                severity=RiskSeverity.MEDIUM,
                title="Arquivo Terraform monolítico",
                description=f"Arquivo com {len(lines)} linhas e {block_count} blocos resource/module dificulta manutenção.",
                file_path=file_path,
                evidence=f"{len(lines)} linhas, {block_count} blocos",
                suggestion="Dividir por domínio (networking, compute, data) ou por ambiente",
            )]
        return []

    def _analyze_missing_tags(self, file_path: str, content: str) -> list[RiskFinding]:
        findings: list[RiskFinding] = []
        for match in self._AWS_RESOURCE.finditer(content):
            snippet = content[match.start(): match.start() + 600]
            if self._TAGS_BLOCK.search(snippet):
                continue
            findings.append(RiskFinding(
                category=RiskCategory.LACK_OF_OBSERVABILITY,
                severity=RiskSeverity.LOW,
                title="Recurso AWS sem tags",
                description="Tags ausentes dificultam cost allocation, ownership e auditoria.",
                file_path=file_path,
                evidence=match.group(0).strip()[:80],
                suggestion="Adicionar bloco tags com environment, owner e cost-center",
            ))
        return findings


class AnalyzerFactory:
    _ANALYZERS: dict[str, list[type[BaseAnalyzer]]] = {
        ".java": [JavaAnalyzer],
        ".kt": [JavaAnalyzer],
        ".sql": [MigrationAnalyzer],
        ".tf": [TerraformAnalyzer],
        ".tfvars": [TerraformAnalyzer],
    }

    @staticmethod
    def get_analyzers(file_path: str) -> list[BaseAnalyzer]:
        lower = file_path.lower()
        analyzers: list[BaseAnalyzer] = []

        if lower.endswith("dockerfile") or "/dockerfile" in lower:
            analyzers.append(DockerAnalyzer())
        if "docker-compose" in lower or "compose.y" in lower:
            analyzers.append(DockerAnalyzer())
        if "openapi" in lower or "swagger" in lower:
            analyzers.append(OpenApiAnalyzer())
        if ".github/workflows/" in lower or "jenkinsfile" in lower or ".gitlab-ci" in lower:
            analyzers.append(PipelineAnalyzer())

        for ext, analyzer_classes in AnalyzerFactory._ANALYZERS.items():
            if lower.endswith(ext):
                analyzers.extend(cls() for cls in analyzer_classes)

        return analyzers
