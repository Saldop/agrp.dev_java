package dev.agrp.contract.presidio;

import java.util.List;

record PresidioAnalyzeRequest(String text, String language, List<String> entities) {}
