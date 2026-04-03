package dev.agrp.contract.presidio;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.*;

@Component
public class PresidioClient {

    private static final List<String> DEFAULT_ENTITIES = List.of(
            "PERSON", "LOCATION", "ORGANIZATION", "EMAIL_ADDRESS",
            "PHONE_NUMBER", "IBAN_CODE", "CREDIT_CARD", "URL", "NRP"
    );

    private final RestClient analyzerClient;

    public PresidioClient(RestClient.Builder builder, PresidioProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(30));
        this.analyzerClient = builder
                .baseUrl(properties.analyzerUrl())
                .requestFactory(factory)
                .build();
    }

    public List<PresidioEntity> analyze(String text) {
        return analyzerClient.post()
                .uri("/analyze")
                .contentType(MediaType.APPLICATION_JSON)
                .body(new PresidioAnalyzeRequest(text, "en", DEFAULT_ENTITIES, 0.7))
                .retrieve()
                .body(new ParameterizedTypeReference<>() {});
    }

    /**
     * Replaces detected entities in {@code text} with numbered tokens (e.g. {@code <PERSON_1>})
     * and returns the anonymized text together with a token→real-value mapping.
     * Anonymization is performed locally because Presidio Anonymizer does not support
     * per-instance numbered tokens natively.
     */
    public AnonymizationResult anonymize(String text, List<PresidioEntity> entities) {
        if (entities.isEmpty()) {
            return new AnonymizationResult(text, Map.of());
        }

        List<PresidioEntity> sorted = entities.stream()
                .sorted(Comparator.comparingInt(PresidioEntity::start))
                .toList();

        Map<String, Integer> typeCounters = new HashMap<>();
        Map<String, String> tokenToReal = new LinkedHashMap<>();
        List<EntityToken> replacements = new ArrayList<>();

        for (PresidioEntity entity : sorted) {
            int index = typeCounters.merge(entity.entityType(), 1, Integer::sum);
            String tokenKey = entity.entityType() + "_" + index;
            tokenToReal.put(tokenKey, text.substring(entity.start(), entity.end()));
            replacements.add(new EntityToken(entity.start(), entity.end(), "<" + tokenKey + ">"));
        }

        // Replace from end to start to keep earlier offsets valid
        StringBuilder sb = new StringBuilder(text);
        for (int i = replacements.size() - 1; i >= 0; i--) {
            EntityToken t = replacements.get(i);
            sb.replace(t.start(), t.end(), t.token());
        }

        return new AnonymizationResult(sb.toString(), Collections.unmodifiableMap(tokenToReal));
    }

    private record EntityToken(int start, int end, String token) {}
}
