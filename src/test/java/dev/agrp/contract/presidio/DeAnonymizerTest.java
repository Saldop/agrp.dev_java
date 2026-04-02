package dev.agrp.contract.presidio;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DeAnonymizerTest {

    private final DeAnonymizer deAnonymizer = new DeAnonymizer();

    @Test
    void deAnonymizeParticipants_replacesTokenWithRealValue() {
        List<String> result = deAnonymizer.deAnonymizeParticipants(
                List.of("<PERSON_1>"),
                Map.of("PERSON_1", "Jan Novák")
        );

        assertThat(result).containsExactly("Jan Novák");
    }

    @Test
    void deAnonymizeParticipants_replacesMultipleDistinctTokens() {
        List<String> result = deAnonymizer.deAnonymizeParticipants(
                List.of("<PERSON_1>", "<PERSON_2>"),
                Map.of("PERSON_1", "Jan Novák", "PERSON_2", "Marie Svobodová")
        );

        assertThat(result).containsExactly("Jan Novák", "Marie Svobodová");
    }

    @Test
    void deAnonymizeParticipants_handlesMultipleTokensInOneString() {
        List<String> result = deAnonymizer.deAnonymizeParticipants(
                List.of("<PERSON_1> and <PERSON_2>"),
                Map.of("PERSON_1", "Jan Novák", "PERSON_2", "Marie Svobodová")
        );

        assertThat(result).containsExactly("Jan Novák and Marie Svobodová");
    }

    @Test
    void deAnonymizeParticipants_leavesUnknownTokensUnchanged() {
        List<String> result = deAnonymizer.deAnonymizeParticipants(
                List.of("<PERSON_1>", "<PERSON_2>"),
                Map.of("PERSON_1", "Jan Novák")
        );

        assertThat(result).containsExactly("Jan Novák", "<PERSON_2>");
    }

    @Test
    void deAnonymizeParticipants_returnsOriginalListWhenMapIsEmpty() {
        List<String> participants = List.of("<PERSON_1>", "<PERSON_2>");

        List<String> result = deAnonymizer.deAnonymizeParticipants(participants, Map.of());

        assertThat(result).isSameAs(participants);
    }
}
