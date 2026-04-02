package dev.agrp.contract.presidio;

import java.util.Map;

public record AnonymizationResult(String anonymizedText, Map<String, String> tokenToReal) {}
