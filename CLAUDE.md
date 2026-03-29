# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Spring Boot 3.4.4 REST API written in Java 21, built with Maven.

- **GroupId / ArtifactId**: `dev.agrp` / `agrp-dev`
- **Base package**: `dev.agrp`

## Build & Test Commands

```bash
# Build (compile + test)
./mvnw clean verify

# Run tests only
./mvnw test

# Run a single test class
./mvnw -Dtest=HealthControllerTest test

# Run the application (port 8080)
./mvnw spring-boot:run

# Package to a runnable JAR
./mvnw clean package
java -jar target/agrp-dev-0.0.1-SNAPSHOT.jar
```

## Architecture

```
src/main/java/dev/agrp/
├── AgrpApplication.java          # @SpringBootApplication entry point
└── health/
    ├── HealthController.java     # GET /health → {"status":"UP"}
    └── HealthResponse.java       # Java 21 record response body

src/test/java/dev/agrp/
├── health/
│   └── HealthControllerTest.java # @WebMvcTest unit test (web layer only)
└── api/
    └── HealthApiTest.java        # @SpringBootTest + RestAssured (full stack)
```

## Key URLs (local)

| URL | Description |
|-----|-------------|
| `http://localhost:8080/health` | Custom health endpoint |
| `http://localhost:8080/swagger-ui.html` | Swagger UI |
| `http://localhost:8080/v3/api-docs` | OpenAPI JSON spec |
| `http://localhost:8080/actuator/health` | Spring Boot Actuator health |

## Testing Strategy

- **Unit tests** (`dev.agrp.health.*Test`): `@WebMvcTest` + MockMvc — loads web layer only, fast.
- **API tests** (`dev.agrp.api.*Test`): `@SpringBootTest(RANDOM_PORT)` + RestAssured — full application stack on a random port.

## Dependency Notes

Two dependencies are not managed by the Spring Boot BOM and are pinned in `pom.xml` `<properties>`:
- `springdoc-openapi-starter-webmvc-ui` → `springdoc.version`
- `rest-assured` → `rest-assured.version`

## Adding New Endpoints

1. Create a subpackage under `dev.agrp` (e.g., `dev.agrp.users`).
2. Add a `@RestController` with `@RequestMapping`.
3. Annotate with `@Tag`, `@Operation`, `@ApiResponse` for Swagger docs.
4. Add a `@WebMvcTest` unit test in `src/test/java/dev/agrp/<package>/`.
5. Add integration assertions to a `*ApiTest` class in `src/test/java/dev/agrp/api/`.
