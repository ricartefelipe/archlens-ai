package dev.archlens.application.service;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.archlens.domain.model.FileType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class FileClassifierTest {

    private final FileClassifier classifier = new FileClassifier();

    @Test
    @DisplayName("Reconhece artefatos de build e container por nome exato")
    void classifiesBuildAndContainerArtifacts() {
        assertAll(
                () -> assertEquals(FileType.DOCKERFILE, classifier.classify("infra/Dockerfile")),
                () -> assertEquals(FileType.DOCKERFILE, classifier.classify("Dockerfile.prod")),
                () -> assertEquals(FileType.DOCKER_COMPOSE, classifier.classify("docker-compose.yml")),
                () -> assertEquals(FileType.DOCKER_COMPOSE, classifier.classify("compose.yaml")),
                () -> assertEquals(FileType.MAVEN, classifier.classify("pom.xml")),
                () -> assertEquals(FileType.GRADLE, classifier.classify("build.gradle.kts")),
                () -> assertEquals(FileType.GRADLE, classifier.classify("settings.gradle")));
    }

    @Test
    @DisplayName("Reconhece pipelines de CI por caminho ou nome")
    void classifiesPipelines() {
        assertAll(
                () -> assertEquals(FileType.PIPELINE, classifier.classify("Jenkinsfile")),
                () -> assertEquals(FileType.PIPELINE, classifier.classify(".github/workflows/ci.yml")),
                () -> assertEquals(FileType.PIPELINE, classifier.classify("project/.gitlab-ci.yml")),
                () -> assertEquals(FileType.PIPELINE, classifier.classify("azure-pipelines.yml")));
    }

    @Test
    @DisplayName("OpenAPI e Kubernetes dependem do contexto do caminho")
    void classifiesContextSensitiveYaml() {
        assertAll(
                () -> assertEquals(FileType.OPENAPI, classifier.classify("docs/openapi.yaml")),
                () -> assertEquals(FileType.OPENAPI, classifier.classify("api/swagger.json")),
                () -> assertEquals(FileType.KUBERNETES, classifier.classify("k8s/deployment.yaml")),
                () -> assertEquals(FileType.KUBERNETES, classifier.classify("helm/values.yml")),
                () -> assertEquals(FileType.KUBERNETES, classifier.classify("manifests/api/deployment.yaml")),
                () -> assertEquals(FileType.KUBERNETES, classifier.classify("charts/app/values.yaml")),
                () -> assertEquals(FileType.YAML, classifier.classify("config/app.yaml")));
    }

    @Test
    @DisplayName("Reconhece extensões de linguagem comuns")
    void classifiesByExtension() {
        assertAll(
                () -> assertEquals(FileType.JAVA, classifier.classify("src/Main.java")),
                () -> assertEquals(FileType.DOTNET, classifier.classify("src/Program.cs")),
                () -> assertEquals(FileType.DOTNET, classifier.classify("Controllers/OrderController.cs")),
                () -> assertEquals(FileType.KOTLIN, classifier.classify("Main.kt")),
                () -> assertEquals(FileType.PYTHON, classifier.classify("app/main.py")),
                () -> assertEquals(FileType.GO, classifier.classify("cmd/server/main.go")),
                () -> assertEquals(FileType.TYPESCRIPT, classifier.classify("src/App.tsx")),
                () -> assertEquals(FileType.TYPESCRIPT, classifier.classify("lib/utils.ts")),
                () -> assertEquals(FileType.SQL, classifier.classify("db/001.sql")),
                () -> assertEquals(FileType.TERRAFORM, classifier.classify("infra/main.tf")),
                () -> assertEquals(FileType.TERRAFORM, classifier.classify("env/prod.tfvars")),
                () -> assertEquals(FileType.MARKDOWN, classifier.classify("README.md")),
                () -> assertEquals(FileType.SHELL, classifier.classify("scripts/run.sh")),
                () -> assertEquals(FileType.OTHER, classifier.classify("data.bin")));
    }

    @Test
    @DisplayName("isRelevant ignora diretórios de build e binários")
    void ignoresIrrelevantFiles() {
        assertAll(
                () -> assertFalse(classifier.isRelevant("node_modules/lib/index.js")),
                () -> assertFalse(classifier.isRelevant("target/classes/App.class")),
                () -> assertFalse(classifier.isRelevant("assets/logo.png")),
                () -> assertFalse(classifier.isRelevant("data.bin")),
                () -> assertTrue(classifier.isRelevant("src/main/java/App.java")),
                () -> assertTrue(classifier.isRelevant("src/Program.cs")),
                () -> assertFalse(classifier.isRelevant("vendor/github.com/lib/main.go")),
                () -> assertTrue(classifier.isRelevant("cmd/api/main.go")),
                () -> assertTrue(classifier.isRelevant("pom.xml")));
    }
}
