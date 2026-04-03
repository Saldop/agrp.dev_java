package dev.agrp.contract.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

record ChatRequest(
        String model,
        List<Message> messages,
        @JsonProperty("response_format") ResponseFormat responseFormat
) {
    record Message(String role, String content) {}

    record ResponseFormat(String type, @JsonProperty("json_schema") JsonSchema jsonSchema) {
        record JsonSchema(String name, boolean strict, JsonNode schema) {}
    }
}
