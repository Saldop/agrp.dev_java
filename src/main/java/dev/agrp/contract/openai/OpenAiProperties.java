package dev.agrp.contract.openai;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "openai")
public record OpenAiProperties(String apiKey, String model, String baseUrl) {}
