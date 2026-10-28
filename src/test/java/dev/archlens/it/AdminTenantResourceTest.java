package dev.archlens.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * A API de administração exige o papel {@code admin}. A recusa precisa chegar
 * como 403: o endpoint de status a sinaliza com {@code ForbiddenException}, e
 * um mapper de {@code Exception} transformava essa exceção de status em 500 —
 * falta de permissão apresentada como falha do servidor.
 */
@QuarkusTest
@QuarkusTestResource(ArchLensTestResource.class)
class AdminTenantResourceTest {

    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String TENANT = "tenant-admin-validation";

    @Test
    @DisplayName("PATCH /status sem papel admin retorna 403, não 500")
    void statusWithoutAdminRoleReturns403() {
        given()
                .header(TENANT_HEADER, TENANT)
                .contentType(ContentType.JSON)
                .body("{\"status\":\"ACTIVE\"}")
                .when()
                .patch("/v1/admin/tenants/" + TENANT + "/status")
                .then()
                .statusCode(403)
                .body("message", not(containsString("unexpected error")));
    }

    @Test
    @DisplayName("PUT /plan sem papel admin retorna 403")
    void planWithoutAdminRoleReturns403() {
        given()
                .header(TENANT_HEADER, TENANT)
                .contentType(ContentType.JSON)
                .body("{\"plan\":\"DIAGNOSTICO\",\"notes\":null}")
                .when()
                .put("/v1/admin/tenants/" + TENANT + "/plan")
                .then()
                .statusCode(403);
    }

    @Test
    @DisplayName("GET /v1/admin/tenants sem papel admin retorna 403")
    void listWithoutAdminRoleReturns403() {
        given()
                .header(TENANT_HEADER, TENANT)
                .when()
                .get("/v1/admin/tenants")
                .then()
                .statusCode(403);
    }
}
