package dev.archlens.application.service;

import java.util.Set;

import dev.archlens.domain.model.FileType;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class FileClassifier {

    private static final Set<String> SENSITIVE_EXTENSIONS = Set.of(
            ".pem", ".key", ".p12", ".pfx", ".jks", ".keystore", ".truststore",
            ".ppk", ".asc", ".gpg", ".kdbx", ".tfvars", ".tfstate", ".pub");

    private static final Set<String> SENSITIVE_NAMES = Set.of(
            ".npmrc", ".pypirc", ".netrc", "_netrc", ".htpasswd", ".pgpass",
            "terraform.tfstate.backup", "credentials", "kubeconfig");

    public FileType classify(String filePath) {
        String lower = filePath.toLowerCase();
        String fileName = lower.contains("/") ? lower.substring(lower.lastIndexOf('/') + 1) : lower;

        if (fileName.equals("dockerfile") || fileName.startsWith("dockerfile.")) {
            return FileType.DOCKERFILE;
        }
        if (fileName.equals("docker-compose.yml") || fileName.equals("docker-compose.yaml")
                || fileName.equals("compose.yml") || fileName.equals("compose.yaml")) {
            return FileType.DOCKER_COMPOSE;
        }
        if (fileName.equals("pom.xml")) {
            return FileType.MAVEN;
        }
        if (fileName.endsWith(".gradle") || fileName.endsWith(".gradle.kts") || fileName.equals("settings.gradle")) {
            return FileType.GRADLE;
        }
        if (fileName.equals("jenkinsfile") || fileName.endsWith(".jenkinsfile")
                || lower.contains(".github/workflows/") || lower.contains(".gitlab-ci")
                || lower.contains("azure-pipelines") || lower.contains("bitbucket-pipelines")) {
            return FileType.PIPELINE;
        }
        if (lower.contains("openapi") || lower.contains("swagger")) {
            if (fileName.endsWith(".json") || fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
                return FileType.OPENAPI;
            }
        }
        if (lower.contains("k8s/") || lower.contains("kubernetes/") || lower.contains("helm/")
                || lower.contains("deploy/") || lower.contains("manifests/") || lower.contains("charts/")) {
            if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
                return FileType.KUBERNETES;
            }
        }

        if (fileName.endsWith(".java")) return FileType.JAVA;
        if (fileName.endsWith(".cs")) return FileType.DOTNET;
        if (fileName.endsWith(".go")) return FileType.GO;
        if (fileName.endsWith(".ts") || fileName.endsWith(".tsx")
                || fileName.endsWith(".js") || fileName.endsWith(".jsx")) {
            return FileType.TYPESCRIPT;
        }
        if (fileName.endsWith(".kt") || fileName.endsWith(".kts")) return FileType.KOTLIN;
        if (fileName.endsWith(".py")) return FileType.PYTHON;
        if (fileName.endsWith(".sql")) return FileType.SQL;
        if (fileName.endsWith(".xml")) return FileType.XML;
        if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) return FileType.YAML;
        if (fileName.endsWith(".properties")) return FileType.PROPERTIES;
        if (fileName.endsWith(".json")) return FileType.JSON;
        if (fileName.endsWith(".md") || fileName.endsWith(".adoc")) return FileType.MARKDOWN;
        if (fileName.endsWith(".sh") || fileName.endsWith(".bash")) return FileType.SHELL;
        if (fileName.endsWith(".tf") || fileName.endsWith(".tfvars")) return FileType.TERRAFORM;

        return FileType.OTHER;
    }

    /**
     * Arquivos cuja razão de existir é guardar credencial: ficam fora da
     * extração, do índice, dos embeddings e de qualquer envio a provedor
     * externo. Configuração legítima (`.properties`, `.yml`, `.tf`) continua
     * sendo analisada — é lá que a análise procura segredo embutido como risco.
     */
    public boolean isSensitive(String filePath) {
        String lower = filePath.toLowerCase();
        String fileName = lower.contains("/") ? lower.substring(lower.lastIndexOf('/') + 1) : lower;

        if (lower.contains(".ssh/") || lower.contains(".gnupg/") || lower.contains(".aws/credentials")
                || lower.contains(".kube/config") || lower.contains(".docker/config.json")) {
            return true;
        }

        for (String extension : SENSITIVE_EXTENSIONS) {
            if (fileName.endsWith(extension)) {
                return true;
            }
        }

        if (SENSITIVE_NAMES.contains(fileName) || fileName.startsWith(".env")
                || fileName.startsWith("id_rsa") || fileName.startsWith("id_dsa")
                || fileName.startsWith("id_ecdsa") || fileName.startsWith("id_ed25519")) {
            return true;
        }

        // secrets.yml, prod.secrets.json, db-secret.yaml, credentials.json…
        return fileName.contains("secrets.") || fileName.contains("credentials.")
                || fileName.startsWith("secret.") || fileName.contains("-secret.")
                || fileName.contains("_secret.");
    }

    public boolean isRelevant(String filePath) {
        if (isSensitive(filePath)) {
            return false;
        }
        String lower = filePath.toLowerCase();
        if (lower.contains("node_modules/") || lower.contains("target/")
                || lower.contains("build/") || lower.contains(".git/")
                || lower.contains("__pycache__/") || lower.contains(".gradle/")
                || lower.contains("vendor/") || lower.contains(".idea/") || lower.contains(".vscode/")) {
            return false;
        }
        String fileName = lower.contains("/") ? lower.substring(lower.lastIndexOf('/') + 1) : lower;
        if (fileName.endsWith(".class") || fileName.endsWith(".jar") || fileName.endsWith(".war")
                || fileName.endsWith(".png") || fileName.endsWith(".jpg") || fileName.endsWith(".gif")
                || fileName.endsWith(".ico") || fileName.endsWith(".svg") || fileName.endsWith(".woff")
                || fileName.endsWith(".ttf") || fileName.endsWith(".eot") || fileName.endsWith(".lock")) {
            return false;
        }
        return classify(filePath) != FileType.OTHER;
    }
}
