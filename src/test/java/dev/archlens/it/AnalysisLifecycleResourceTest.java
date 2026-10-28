package dev.archlens.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

import dev.archlens.application.port.out.AnalysisRepositoryPort;
import dev.archlens.domain.model.AnalysisStatus;
import dev.archlens.infrastructure.jobs.StuckAnalysisSweeper;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Ciclo de vida da análise além do caminho feliz: cancelamento pelo usuário e
 * liberação de análises abandonadas pelo varredor.
 */
@QuarkusTest
@QuarkusTestResource(ArchLensTestResource.class)
class AnalysisLifecycleResourceTest {

    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String TENANT = "tenant-lifecycle";

    @Inject
    StuckAnalysisSweeper sweeper;

    @Inject
    AnalysisRepositoryPort analysisRepository;

    @Inject
    EntityManager entityManager;

    @Test
    @DisplayName("cancelamento: em andamento cancela, repetido é idempotente, terminal responde 409")
    void cancelLifecycle() {
        String projectId = createProject("cancel-me");
        String analysisId = createPendingAnalysis(projectId);

        given()
                .header(TENANT_HEADER, TENANT)
                .when()
                .post("/v1/projects/" + projectId + "/analyses/" + analysisId + "/cancel")
                .then()
                .statusCode(200)
                .body("status", equalTo("CANCELLED"));

        // Idempotente: cancelar de novo devolve o mesmo estado, sem erro.
        given()
                .header(TENANT_HEADER, TENANT)
                .when()
                .post("/v1/projects/" + projectId + "/analyses/" + analysisId + "/cancel")
                .then()
                .statusCode(200)
                .body("status", equalTo("CANCELLED"));

        // Estado terminal não volta atrás.
        forceStatus(analysisId, AnalysisStatus.COMPLETED);
        given()
                .header(TENANT_HEADER, TENANT)
                .when()
                .post("/v1/projects/" + projectId + "/analyses/" + analysisId + "/cancel")
                .then()
                .statusCode(409)
                .body("message", containsString("COMPLETED"));
    }

    @Test
    @DisplayName("outro tenant não cancela a análise: 404")
    void crossTenantCancelIsNotFound() {
        String projectId = createProject("cancel-isolado");
        String analysisId = createPendingAnalysis(projectId);

        given()
                .header(TENANT_HEADER, "tenant-intruso")
                .when()
                .post("/v1/projects/" + projectId + "/analyses/" + analysisId + "/cancel")
                .then()
                .statusCode(404);
    }

    @Test
    @DisplayName("varredor libera análise abandonada e preserva as recentes")
    void sweeperReleasesAbandonedAnalyses() {
        String projectId = createProject("sweep-me");
        String abandonedId = createPendingAnalysis(projectId);
        String freshId = createPendingAnalysis(projectId);

        // Envelhece só a primeira para além do prazo de abandono.
        ageAnalysis(abandonedId, Duration.ofHours(2));

        int released = sweeper.sweep(Instant.now());

        org.junit.jupiter.api.Assertions.assertTrue(released >= 1,
                "o varredor deveria liberar a análise envelhecida");

        given()
                .header(TENANT_HEADER, TENANT)
                .when()
                .get("/v1/projects/" + projectId + "/analyses/" + abandonedId)
                .then()
                .statusCode(200)
                .body("status", equalTo("FAILED"))
                .body("summary", containsString("sem progresso"));

        given()
                .header(TENANT_HEADER, TENANT)
                .when()
                .get("/v1/projects/" + projectId + "/analyses/" + freshId)
                .then()
                .statusCode(200)
                .body("status", equalTo("PENDING"));
    }

    // --- helpers -----------------------------------------------------------

    private String createProject(String name) {
        return given()
                .header(TENANT_HEADER, TENANT)
                .contentType(ContentType.JSON)
                .body("{\"name\":\"" + name + "-" + System.nanoTime() + "\",\"description\":null}")
                .when()
                .post("/v1/projects")
                .then()
                .statusCode(201)
                .extract().path("id");
    }

    /**
     * Insere a análise direto pelo repositório: criar pela API exigiria um
     * upload READY, e o alvo destes testes é a transição de estado, não o
     * pipeline de ingestão.
     */
    private String createPendingAnalysis(String projectId) {
        dev.archlens.domain.model.Analysis analysis = new dev.archlens.domain.model.Analysis();
        analysis.setId(UUID.randomUUID());
        analysis.setProjectId(UUID.fromString(projectId));
        analysis.setTenantId(TENANT);
        analysis.setStatus(AnalysisStatus.PENDING);
        analysis.setCreatedAt(Instant.now());
        analysis.setUpdatedAt(Instant.now());
        return analysisRepository.save(analysis).getId().toString();
    }

    @Transactional
    void ageAnalysis(String analysisId, Duration age) {
        entityManager.createNativeQuery(
                        "UPDATE analyses SET updated_at = :when WHERE id = :id")
                .setParameter("when", Instant.now().minus(age))
                .setParameter("id", UUID.fromString(analysisId))
                .executeUpdate();
    }

    @Transactional
    void forceStatus(String analysisId, AnalysisStatus status) {
        entityManager.createNativeQuery(
                        "UPDATE analyses SET status = :status WHERE id = :id")
                .setParameter("status", status.name())
                .setParameter("id", UUID.fromString(analysisId))
                .executeUpdate();
    }
}
