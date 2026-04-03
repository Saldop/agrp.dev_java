package dev.agrp.contract.openai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ResourceLoader;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

@Component
public class OpenAiContractAnalyzer {

    private final RestClient restClient;
    private final OpenAiProperties properties;
    private final String systemPrompt;
    private final JsonNode schema;
    private final ObjectMapper objectMapper;

    public OpenAiContractAnalyzer(
            RestClient.Builder builder,
            OpenAiProperties properties,
            ResourceLoader resourceLoader,
            ObjectMapper objectMapper) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new IllegalStateException("OPENAI_API_KEY must be set");
        }
        this.restClient = builder
                .baseUrl(properties.baseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.apiKey())
                .build();
        this.properties = properties;
        this.systemPrompt = loadResource(resourceLoader, "classpath:prompts/contract-analysis-en.txt");
        this.schema = loadSchema(resourceLoader, objectMapper);
        this.objectMapper = objectMapper;
    }

    public OpenAiAnalysisResult analyze(String anonymizedText) {
        Map<String, Object> request = buildRequest(anonymizedText);

        String rawResponse = restClient.post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(request)
                .retrieve()
                .body(String.class);

        return parseResponse(rawResponse);
    }

    private static String loadResource(ResourceLoader resourceLoader, String location) {
        try {
            return resourceLoader.getResource(location).getContentAsString(StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("Resource not found: " + location, e);
        }
    }

    private static JsonNode loadSchema(ResourceLoader resourceLoader, ObjectMapper objectMapper) {
        var resource = resourceLoader.getResource("classpath:prompts/contract-analysis-schema.json");
        try {
            return objectMapper.readTree(resource.getInputStream());
        } catch (IOException e) {
            throw new IllegalArgumentException("Schema file not found", e);
        }
    }

    private Map<String, Object> buildRequest(String text) {
        return Map.of(
                "model", properties.model(),
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", text)
                ),
                "response_format", Map.of(
                        "type", "json_schema",
                        "json_schema", Map.of(
                                "name", "contract_analysis",
                                "strict", true,
                                "schema", schema
                        )
                )
        );
    }

    private OpenAiAnalysisResult parseResponse(String rawResponse) {
        try {
            JsonNode root = objectMapper.readTree(rawResponse);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                throw new RuntimeException("OpenAI response contained no choices");
            }
            String content = choices.get(0).path("message").path("content").asText();
            return objectMapper.readValue(content, OpenAiAnalysisResult.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to parse OpenAI response", e);
        }
    }
}
