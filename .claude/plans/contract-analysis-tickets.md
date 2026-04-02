# Contract Analysis Pipeline — Ticket Breakdown

## Context
The application exposes a single endpoint `POST /contracts/analyze` that accepts a PDF contract, anonymizes PII via Presidio, analyzes the anonymized text with OpenAI o4-mini (structured output), de-anonymizes the result, and returns structured JSON. The project currently has only a health check endpoint. This plan breaks the full pipeline into 7 independently testable tickets.

**Decisions locked in:**
- OpenAI model: `o4-mini` with `response_format: json_schema`
- Contract languages: English and Czech (`language` param: `en` / `cs`)
- Issue fields: `description`, `severity` (LOW/MEDIUM/HIGH), `originalClause`, `recommendation`

---

## Ticket 1 — PDF Text Extraction Service

**Package:** `dev.agrp.contract.pdf`

**Implement:**
- Pin `pdfbox.version` in `pom.xml` `<properties>` and add `org.apache.pdfbox:pdfbox` dependency (same pattern as `springdoc.version`)
- `PdfTextExtractor` `@Component`: `String extract(InputStream pdf)` using `PDDocument.load()` + `PDFTextStripper`
- `PdfExtractionException` (unchecked) for empty/corrupt input

**Acceptance criteria:**
- Returns non-blank text from `src/test/resources/contracts/sample.pdf`
- Throws `PdfExtractionException` on empty/corrupt input stream
- `./mvnw clean verify` passes

**Tests:** `PdfTextExtractorTest` — plain JUnit 5, no Spring context. Load PDF from classpath; negative test with empty byte array.

**Dependencies:** none

---

## Ticket 2 — Presidio Client (Analyzer + Anonymizer)

**Package:** `dev.agrp.contract.presidio`

**Implement:**
- Add `wiremock-spring-boot` (`org.wiremock.integrations:wiremock-spring-boot`, 3.x) to `pom.xml` `<scope>test</scope>`
- `PresidioProperties` `@ConfigurationProperties(prefix="presidio")`: `analyzerUrl`, `anonymizerUrl`
- `application.yml`: `presidio.analyzer-url: http://localhost:5002`, `presidio.anonymizer-url: http://localhost:5001`
- `PresidioClient` `@Component` using `RestClient`:
  - `List<PresidioEntity> analyze(String text, String language)` → `POST /analyze`
  - `AnonymizationResult anonymize(String text, List<PresidioEntity> entities)` → `POST /anonymize`
- Records: `PresidioAnalyzeRequest`, `PresidioEntity`, `PresidioAnonymizeRequest`, `AnonymizationResult(String anonymizedText, Map<String, String> tokenToReal)`
- Token map construction: slice original text by `[start, end)` from Anonymizer response `items`; store keys without angle brackets (e.g. `PERSON_1 → Jan Novák`)

**Acceptance criteria:**
- Request body shape matches Presidio REST API (`text`, `language`, `entities` array)
- `tokenToReal` correctly maps token key → real value for detected entities
- Uses `RestClient`, not `RestTemplate`
- `./mvnw clean verify` passes

**Tests:** `PresidioClientTest` — WireMock stubs for both Presidio endpoints using fixture JSON files in `src/test/resources/fixtures/`.

**Dependencies:** T1 (for pom.xml pattern; logically independent)

---

## Ticket 3 — OpenAI o4-mini Structured Output Client

**Package:** `dev.agrp.contract.openai`

**Implement:**
- `OpenAiProperties` `@ConfigurationProperties(prefix="openai")`: `apiKey`, `model` (default `o4-mini`), `baseUrl` (default `https://api.openai.com`)
- `application.yml`: `openai.api-key: ${OPENAI_API_KEY:}` — never fails to start when unset
- Prompt templates as classpath resources: `src/main/resources/prompts/contract-analysis-en.txt`, `contract-analysis-cs.txt` — loaded via `ResourceLoader`
- `OpenAiContractAnalyzer` `@Component` using `RestClient`:
  - `OpenAiAnalysisResult analyze(String anonymizedText, String language)` → `POST /v1/chat/completions`
  - Request includes `response_format: { type: "json_schema", json_schema: { schema: <inline schema> } }`
  - Selects prompt file by `language` param
- Records/enums: `OpenAiAnalysisResult(String contractType, List<String> participants, List<ContractIssue> issues)`, `ContractIssue(String description, Severity severity, String originalClause, String recommendation)`, `Severity` enum `LOW/MEDIUM/HIGH`

**Acceptance criteria:**
- Request body contains `"model":"o4-mini"` and a `response_format` with `type:"json_schema"`
- `Authorization: Bearer <key>` header is set
- Czech language sends `contract-analysis-cs.txt` prompt; English sends `contract-analysis-en.txt`
- Correctly deserializes canned OpenAI JSON (including `Severity` enum) from fixture file
- `./mvnw clean verify` passes with `OPENAI_API_KEY` unset

**Tests:** `OpenAiContractAnalyzerTest` — WireMock stub for `POST /v1/chat/completions` with `src/test/resources/fixtures/openai-contract-response.json`. Assert all fields + one test per language path.

**Dependencies:** T2 (WireMock available; config pattern established)

---

## Ticket 4 — De-anonymization Service

**Package:** `dev.agrp.contract.presidio`

**Implement:**
- `DeAnonymizer` `@Component`:
  - `List<String> deAnonymizeParticipants(List<String> tokens, Map<String, String> tokenToReal)`
  - Single regex pass replacing `<TOKEN_N>` patterns with map values; unknown tokens pass through unchanged

**Acceptance criteria:**
- `["<PERSON_1>", "<PERSON_2>"]` → `["Jan Novák", "Marie Svobodová"]` with correct map
- Unknown tokens pass through unchanged
- Multiple tokens in one string handled correctly
- Null-safe for empty map
- `./mvnw clean verify` passes

**Tests:** `DeAnonymizerTest` — plain JUnit 5, no Spring context. Cover: single replacement, multi-token string, unknown token passthrough, empty map.

**Dependencies:** T2 (uses `tokenToReal` map shape defined there)

---

## Ticket 5 — Orchestration Service

**Package:** `dev.agrp.contract`

**Implement:**
- `ContractAnalysisService` `@Service` injecting `PdfTextExtractor`, `PresidioClient`, `OpenAiContractAnalyzer`, `DeAnonymizer`
- `ContractAnalysisResult analyze(InputStream pdf, String language)`:
  1. `PdfTextExtractor.extract(pdf)`
  2. `PresidioClient.analyze(text, language)`
  3. `PresidioClient.anonymize(text, entities)`
  4. `OpenAiContractAnalyzer.analyze(anonymizedText, language)`
  5. `DeAnonymizer.deAnonymizeParticipants(participants, tokenToReal)`
  6. Return `ContractAnalysisResult`
- `ContractAnalysisResult` record: `String contractType`, `List<String> participants`, `List<ContractIssue> issues`
- `ContractAnalysisException` (unchecked) with `Stage` enum: `PDF_EXTRACTION`, `PII_ANALYSIS`, `PII_ANONYMIZATION`, `AI_ANALYSIS` — wraps all collaborator exceptions

**Acceptance criteria:**
- Each collaborator called exactly once in correct order (verified via Mockito `InOrder`)
- Returned `participants` contain real values (de-anonymized), not tokens
- Each failing collaborator stage wrapped in `ContractAnalysisException` with correct `Stage`
- `./mvnw clean verify` passes

**Tests:** `ContractAnalysisServiceTest` — `@ExtendWith(MockitoExtension.class)`, all collaborators mocked. Happy path + one failure test per stage.

**Dependencies:** T1, T2, T3, T4

---

## Ticket 6 — Controller and OpenAPI Documentation

**Package:** `dev.agrp.contract`

**Implement:**
- `ContractController` `@RestController @RequestMapping("/contracts")`:
  - `POST /contracts/analyze` — `consumes = MULTIPART_FORM_DATA_VALUE`
  - Params: `@RequestPart("file") MultipartFile file`, `@RequestParam(defaultValue="en") String language`
  - Validate `language` ∈ `{"en","cs"}` → `400` otherwise
  - Delegates to `ContractAnalysisService`
- `@RestControllerAdvice` in `dev.agrp.contract` mapping `ContractAnalysisException` stages:
  - `PDF_EXTRACTION` → `422 Unprocessable Entity`
  - `PII_ANALYSIS`, `PII_ANONYMIZATION`, `AI_ANALYSIS` → `502 Bad Gateway`
- `ContractAnalysisResponse` record (API layer, mirrors `ContractAnalysisResult`)
- Full Swagger: `@Tag(name="Contracts")`, `@Operation`, `@ApiResponse` for `200`, `400`, `422`, `502`
- `application.yml`: `spring.servlet.multipart.max-file-size: 20MB`, `max-request-size: 20MB`

**Acceptance criteria:**
- Valid PDF + `language=en` → `200` with correct JSON shape
- `language=de` → `400`
- Service throwing `PDF_EXTRACTION` stage → `422`
- Service throwing `PII_ANALYSIS` stage → `502`
- Swagger UI lists the endpoint with all response codes
- `./mvnw clean verify` passes

**Tests:**
- `ContractControllerTest` — `@WebMvcTest(ContractController.class)` + `@MockBean ContractAnalysisService`. Test all HTTP mappings.
- `ContractApiTest` — `@SpringBootTest(RANDOM_PORT)` + RestAssured + WireMock stubs for Presidio + OpenAI. Upload real PDF, assert full response shape.

**Dependencies:** T5

---

## Ticket 7 — Integration Test Hardening

**Implement:**
- `src/test/resources/application-test.yml` overriding all external URLs to WireMock dynamic ports
- Shared `@TestConfiguration` / `AbstractIntegrationTest` base class using `DynamicPropertyRegistry` to inject WireMock ports for Presidio Analyzer, Presidio Anonymizer, and OpenAI
- Fixture JSON files:
  - `src/test/resources/fixtures/presidio-analyze-response.json`
  - `src/test/resources/fixtures/presidio-anonymize-response.json`
  - `src/test/resources/fixtures/openai-contract-response.json`
- Update `ContractApiTest` to use shared WireMock setup with `verify(postRequestedFor(...))` assertions
- `Dockerfile`: add `ENV OPENAI_API_KEY` comment documenting the required runtime variable

**Acceptance criteria:**
- `./mvnw clean verify` passes with no real outbound HTTP (WireMock `verify()` confirms all three services were called)
- `./mvnw test` passes with `OPENAI_API_KEY` unset
- No hardcoded `localhost:5001` / `localhost:5002` URLs remain in `src/main/`

**Tests:** The test infrastructure itself is the deliverable. Confirmed by `./mvnw clean verify` going green.

**Dependencies:** T6

---

## Sequencing

```
T1 (PDF extraction)
  └─ T2 (Presidio client)
       ├─ T3 (OpenAI client)          ← parallel with T4
       └─ T4 (De-anonymizer)          ← parallel with T3
            └─ T5 (Orchestration)     ← needs T1+T2+T3+T4
                 └─ T6 (Controller)
                      └─ T7 (Test hardening)
```

## Critical Files to Modify
- `pom.xml` — PDFBox, WireMock dependencies
- `src/main/resources/application.yml` — Presidio URLs, OpenAI config, multipart limits
- `src/main/resources/prompts/contract-analysis-en.txt` (new)
- `src/main/resources/prompts/contract-analysis-cs.txt` (new)
- `src/test/resources/fixtures/*.json` (new — 3 files)
- `src/test/resources/contracts/sample.pdf` (new — test PDF)
- `Dockerfile` — document `OPENAI_API_KEY` env var
