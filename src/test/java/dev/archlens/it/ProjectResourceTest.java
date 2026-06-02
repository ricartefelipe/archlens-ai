package dev.archlens.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@QuarkusTest
@QuarkusTestResource(ArchLensTestResource.class)
class ProjectResourceTest {

    private static final String TENANT_HEADER = "X-Tenant-Id";

    @Test
    @DisplayName("POST /v1/projects cria projeto e retorna 201 com status CREATED")
    void createProjectReturns201() {
        given()
                .header(TENANT_HEADER, "tenant-create")
                .contentType(ContentType.JSON)
                .body("{\"name\":\"checkout\",\"description\":\"serviço de pagamento\"}")
                .when()
                .post("/v1/projects")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .body("tenantId", equalTo("tenant-create"))
                .body("name", equalTo("checkout"))
                .body("status", equalTo("CREATED"))
                .body("fileCount", is(0));
    }

    @Test
    @DisplayName("POST /v1/projects com nome em branco retorna 400")
    void createProjectWithBlankNameReturns400() {
        given()
                .header(TENANT_HEADER, "tenant-create")
                .contentType(ContentType.JSON)
                .body("{\"name\":\"\",\"description\":\"x\"}")
                .when()
                .post("/v1/projects")
                .then()
                .statusCode(400);
    }

    @Test
    @DisplayName("GET /v1/projects retorna apenas projetos do tenant informado")
    void listProjectsIsTenantScoped() {
        String name = "isolated-" + System.nanoTime();
        given()
                .header(TENANT_HEADER, "tenant-x")
                .contentType(ContentType.JSON)
                .body("{\"name\":\"" + name + "\",\"description\":null}")
                .when()
                .post("/v1/projects")
                .then()
                .statusCode(201);

        given()
                .header(TENANT_HEADER, "tenant-x")
                .when()
                .get("/v1/projects")
                .then()
                .statusCode(200)
                .body("findAll { it.name == '" + name + "' }.size()", is(1));

        given()
                .header(TENANT_HEADER, "tenant-y")
                .when()
                .get("/v1/projects")
                .then()
                .statusCode(200)
                .body("findAll { it.name == '" + name + "' }.size()", is(0));
    }

    @Test
    @DisplayName("GET /v1/projects/{id} retorna o projeto criado")
    void getProjectById() {
        String id = given()
                .header(TENANT_HEADER, "tenant-get")
                .contentType(ContentType.JSON)
                .body("{\"name\":\"api\",\"description\":null}")
                .when()
                .post("/v1/projects")
                .then()
                .statusCode(201)
                .extract().path("id");

        given()
                .header(TENANT_HEADER, "tenant-get")
                .when()
                .get("/v1/projects/" + id)
                .then()
                .statusCode(200)
                .body("id", equalTo(id))
                .body("name", equalTo("api"));
    }

    @Test
    @DisplayName("GET /v1/projects/{id} inexistente retorna 404")
    void getUnknownProjectReturns404() {
        given()
                .header(TENANT_HEADER, "tenant-get")
                .when()
                .get("/v1/projects/00000000-0000-0000-0000-000000000000")
                .then()
                .statusCode(404);
    }
}
