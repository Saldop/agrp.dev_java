package dev.agrp.contract.presidio;

import com.fasterxml.jackson.annotation.JsonProperty;

public record PresidioEntity(
        @JsonProperty("entity_type") String entityType,
        int start,
        int end,
        double score
) {}
