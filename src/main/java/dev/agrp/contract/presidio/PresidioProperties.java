package dev.agrp.contract.presidio;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "presidio")
public record PresidioProperties(String analyzerUrl, String anonymizerUrl) {}
