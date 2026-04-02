package dev.agrp.contract.presidio;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class DeAnonymizer {

    private static final Pattern TOKEN_PATTERN = Pattern.compile("<([A-Z_]+_\\d+)>");

    public List<String> deAnonymizeParticipants(List<String> participants, Map<String, String> tokenToReal) {
        if (tokenToReal.isEmpty()) {
            return participants;
        }
        return participants.stream()
                .map(p -> replaceTokens(p, tokenToReal))
                .toList();
    }

    private String replaceTokens(String text, Map<String, String> tokenToReal) {
        Matcher matcher = TOKEN_PATTERN.matcher(text);
        StringBuilder sb = new StringBuilder();
        while (matcher.find()) {
            String key = matcher.group(1);
            String replacement = tokenToReal.getOrDefault(key, matcher.group(0));
            matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }
}
