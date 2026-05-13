package dev.archlens.application.service;

import dev.archlens.domain.model.FileType;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class FileClassifier {

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
                || lower.contains("deploy/")) {
            if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
                return FileType.KUBERNETES;
            }
        }

        if (fileName.endsWith(".java")) return FileType.JAVA;
        if (fileName.endsWith(".kt") || fileName.endsWith(".kts")) return FileType.KOTLIN;
        if (fileName.endsWith(".py")) return FileType.PYTHON;
        if (fileName.endsWith(".sql")) return FileType.SQL;
        if (fileName.endsWith(".xml")) return FileType.XML;
        if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) return FileType.YAML;
        if (fileName.endsWith(".properties")) return FileType.PROPERTIES;
        if (fileName.endsWith(".json")) return FileType.JSON;
        if (fileName.endsWith(".md") || fileName.endsWith(".adoc")) return FileType.MARKDOWN;
        if (fileName.endsWith(".sh") || fileName.endsWith(".bash")) return FileType.SHELL;

        return FileType.OTHER;
    }

    public boolean isRelevant(String filePath) {
        String lower = filePath.toLowerCase();
        if (lower.contains("node_modules/") || lower.contains("target/")
                || lower.contains("build/") || lower.contains(".git/")
                || lower.contains("__pycache__/") || lower.contains(".gradle/")
                || lower.contains(".idea/") || lower.contains(".vscode/")) {
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
