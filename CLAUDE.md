# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Running with Docker

The easiest way to run the full stack (app + Presidio):

```bash
# Copy and fill in your OpenAI API key
cp .env.example .env   # then edit .env

# Build and start everything
docker-compose up --build
```

Required environment variable in `.env`:
```
OPENAI_API_KEY=sk-...
```

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

## Application Purpose

Contract analysis pipeline:

1. **Upload** — user uploads a PDF contract (`POST /contracts/analyze`)
2. **Extract** — PDFBox extracts plain text from the PDF
3. **Anonymize** — text is sent to Presidio Analyzer to detect PII entities (names, organizations, locations); the app then replaces them locally with numbered tokens like `<PERSON_1>`, retaining the token→real-value mapping in memory
4. **Analyze** — anonymized text is sent to an OpenAI reasoning model (structured output); the model returns:
   - contract type
   - participants (anonymized tokens)
   - legally problematic clauses/sections
5. **De-anonymize** — participant tokens in the structured output are replaced with real values using the retained mapping
6. **Respond** — final structured JSON is returned to the user

### External Services

| Service | Local URL | Purpose |
|---------|-----------|---------|
| Presidio Analyzer | `http://localhost:5002` | PII entity detection |
| OpenAI API | `https://api.openai.com` | Contract analysis via o4-mini (requires `OPENAI_API_KEY`) |

## Architecture

```
src/main/java/dev/agrp/
├── AgrpApplication.java          # @SpringBootApplication entry point
├── health/
│   ├── HealthController.java     # GET /health → {"status":"UP"}
│   └── HealthResponse.java       # Java 21 record response body
└── contract/
    ├── ContractController.java   # POST /contracts/analyze
    ├── ContractAnalysisService.java
    ├── ContractAnalysisResponse.java
    ├── ContractExceptionHandler.java
    ├── pdf/                      # PDFBox text extraction
    ├── presidio/                 # Presidio PII detection + local anonymization
    └── openai/                   # OpenAI o4-mini structured output client

src/test/java/dev/agrp/
├── health/
│   └── HealthControllerTest.java # @WebMvcTest unit test (web layer only)
├── contract/                     # Unit tests per component (Mockito / WireMock)
└── api/
    ├── HealthApiTest.java        # @SpringBootTest + RestAssured (full stack)
    └── ContractApiTest.java      # Full-stack contract pipeline test (WireMock)

src/test/resources/
├── contracts/                    # Sample PDFs + example responses
└── fixtures/                     # WireMock JSON fixtures
```

## Key URLs (local)

| URL | Description |
|-----|-------------|
| `http://localhost:8080/health` | Custom health endpoint |
| `http://localhost:8080/contracts/analyze` | Contract analysis endpoint |
| `http://localhost:8080/swagger-ui.html` | Swagger UI |
| `http://localhost:8080/v3/api-docs` | OpenAPI JSON spec |
| `http://localhost:8080/actuator/health` | Spring Boot Actuator health |

## Example Output

To see what the application produces without running it, see `src/test/resources/contracts/`:

| Contract | Response |
|----------|----------|
| `lease_agreement_en.pdf` | `lease_agreement_response.json` |
| `employment_agreement_en.pdf` | `empoyment_agreement_response.json` |

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
