package dev.archlens.domain.model;

public enum FileType {

    JAVA("Java Source"),
    DOTNET(".NET Source"),
    KOTLIN("Kotlin Source"),
    XML("XML Configuration"),
    YAML("YAML Configuration"),
    PROPERTIES("Properties File"),
    JSON("JSON File"),
    SQL("SQL Migration"),
    DOCKERFILE("Dockerfile"),
    DOCKER_COMPOSE("Docker Compose"),
    KUBERNETES("Kubernetes Manifest"),
    OPENAPI("OpenAPI Specification"),
    MARKDOWN("Markdown Documentation"),
    GRADLE("Gradle Build"),
    MAVEN("Maven POM"),
    PIPELINE("CI/CD Pipeline"),
    TERRAFORM("Terraform"),
    PYTHON("Python Source"),
    SHELL("Shell Script"),
    OTHER("Other");

    private final String label;

    FileType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
