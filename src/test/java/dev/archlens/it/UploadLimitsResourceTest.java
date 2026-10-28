package dev.archlens.it;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.notNullValue;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Limites de ingestão pela API HTTP: uma bomba de descompressão precisa ser
 * recusada com 413 (não 500) e sem vazar detalhes internos na resposta.
 */
@QuarkusTest
@QuarkusTestResource(ArchLensTestResource.class)
class UploadLimitsResourceTest {

    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String TENANT = "tenant-upload-limits";

    @Test
    @DisplayName("POST /upload com bomba de descompressão retorna 413 sem detalhes internos")
    void refusesDecompressionBombWith413() throws IOException {
        String projectId = given()
                .header(TENANT_HEADER, TENANT)
                .contentType(ContentType.JSON)
                .body("{\"name\":\"zip-bomb\",\"description\":null}")
                .when()
                .post("/v1/projects")
                .then()
                .statusCode(201)
                .body("id", notNullValue())
                .extract().path("id");

        given()
                .header(TENANT_HEADER, TENANT)
                .multiPart("file", "bomb.zip", expansionBomb(), "application/zip")
                .when()
                .post("/v1/projects/" + projectId + "/upload")
                .then()
                .statusCode(413)
                .body("message", containsString("recusado"))
                .body("message", not(containsString("Exception")))
                .body("message", not(containsString("/data/")));
    }

    @Test
    @DisplayName("POST /upload normal continua aceito")
    void acceptsRegularArchive() throws IOException {
        String projectId = given()
                .header(TENANT_HEADER, TENANT)
                .contentType(ContentType.JSON)
                .body("{\"name\":\"zip-ok\",\"description\":null}")
                .when()
                .post("/v1/projects")
                .then()
                .statusCode(201)
                .extract().path("id");

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(out)) {
            zos.putNextEntry(new ZipEntry("src/App.java"));
            zos.write("class App {}".getBytes(StandardCharsets.UTF_8));
            zos.closeEntry();
        }

        given()
                .header(TENANT_HEADER, TENANT)
                .multiPart("file", "ok.zip", out.toByteArray(), "application/zip")
                .when()
                .post("/v1/projects/" + projectId + "/upload")
                .then()
                .statusCode(200);
    }

    /** ~64 MB de zeros em poucos KB: acima do piso e da razão permitida. */
    private static byte[] expansionBomb() throws IOException {
        byte[] payload = new byte[16 * 1024 * 1024];
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(out)) {
            for (int i = 0; i < 4; i++) {
                zos.putNextEntry(new ZipEntry("src/Bomb" + i + ".java"));
                zos.write(payload);
                zos.closeEntry();
            }
        }
        return out.toByteArray();
    }
}
