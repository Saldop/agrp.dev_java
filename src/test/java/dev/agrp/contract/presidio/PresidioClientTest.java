package dev.agrp.contract.presidio;

import com.github.tomakehurst.wiremock.WireMockServer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

import java.io.IOException;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@EnableWireMock({
        @ConfigureWireMock(name = "presidio-analyzer", baseUrlProperties = "presidio.analyzer-url")
})
@TestPropertySource(properties = "openai.api-key=test-key")
class PresidioClientTest {

    @InjectWireMock("presidio-analyzer")
    WireMockServer analyzerMock;

    @Autowired
    PresidioClient client;

    @Test
    void analyze_parsesEntitiesFromResponse() throws IOException {
        String fixture = new String(
                getClass().getClassLoader()
                        .getResourceAsStream("fixtures/presidio-analyze-response.json")
                        .readAllBytes()
        );
        analyzerMock.stubFor(post(urlEqualTo("/analyze")).willReturn(okJson(fixture)));

        // Text matches the offsets in fixtures/presidio-analyze-response.json:
        // PERSON [11,20) = "Jan Novák", LOCATION [35,40) = "Praha"
        List<PresidioEntity> entities = client.analyze("My name is Jan Novák and I live in Praha.");

        assertThat(entities).hasSize(2);
        assertThat(entities.get(0).entityType()).isEqualTo("PERSON");
        assertThat(entities.get(0).start()).isEqualTo(11);
        assertThat(entities.get(0).end()).isEqualTo(20);
        assertThat(entities.get(1).entityType()).isEqualTo("LOCATION");
        assertThat(entities.get(1).start()).isEqualTo(35);
    }

    @Test
    void analyze_sendsTextLanguageAndEntitiesInBody() {
        analyzerMock.stubFor(post(urlEqualTo("/analyze")).willReturn(okJson("[]")));

        client.analyze("hello world");

        analyzerMock.verify(postRequestedFor(urlEqualTo("/analyze"))
                .withRequestBody(matchingJsonPath("$.text", equalTo("hello world")))
                .withRequestBody(matchingJsonPath("$.language", equalTo("en")))
                .withRequestBody(matchingJsonPath("$.entities")));
    }

    @Test
    void anonymize_replacesEntitiesWithNumberedTokens() {
        // "My name is Jan Novák and I live in Praha."
        //             ^11     ^20                ^35  ^40
        String text = "My name is Jan Novák and I live in Praha.";
        List<PresidioEntity> entities = List.of(
                new PresidioEntity("PERSON", 11, 20, 0.9),
                new PresidioEntity("LOCATION", 35, 40, 0.9)
        );

        AnonymizationResult result = client.anonymize(text, entities);

        assertThat(result.anonymizedText())
                .isEqualTo("My name is <PERSON_1> and I live in <LOCATION_1>.");
        assertThat(result.tokenToReal())
                .containsEntry("PERSON_1", "Jan Novák")
                .containsEntry("LOCATION_1", "Praha");
    }

    @Test
    void anonymize_numbersMultipleEntitiesOfSameType() {
        // "Jan Novák and Marie Svobodová signed the contract."
        //  ^0       ^9      ^14          ^29
        String text = "Jan Novák and Marie Svobodová signed the contract.";
        List<PresidioEntity> entities = List.of(
                new PresidioEntity("PERSON", 0, 9, 0.9),
                new PresidioEntity("PERSON", 14, 29, 0.9)
        );

        AnonymizationResult result = client.anonymize(text, entities);

        assertThat(result.anonymizedText())
                .contains("<PERSON_1>")
                .contains("<PERSON_2>")
                .doesNotContain("Jan Novák")
                .doesNotContain("Marie Svobodová");
        assertThat(result.tokenToReal())
                .containsEntry("PERSON_1", "Jan Novák")
                .containsEntry("PERSON_2", "Marie Svobodová");
    }

    @Test
    void anonymize_returnsOriginalTextWhenNoEntities() {
        String text = "No PII here.";

        AnonymizationResult result = client.anonymize(text, List.of());

        assertThat(result.anonymizedText()).isEqualTo(text);
        assertThat(result.tokenToReal()).isEmpty();
    }
}
