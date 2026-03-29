package dev.agrp.api;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class HealthApiTest {

    @LocalServerPort
    int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
        RestAssured.basePath = "";
    }

    @Test
    void health_endpoint_returns_up() {
        given()
            .accept("application/json")
        .when()
            .get("/health")
        .then()
            .statusCode(200)
            .body("status", equalTo("UP"));
    }

    @Test
    void swagger_ui_is_accessible() {
        given()
        .when()
            .get("/swagger-ui.html")
        .then()
            .statusCode(200);
    }
}
