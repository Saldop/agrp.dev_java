package dev.agrp.contract.openai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.stubbing.ServeEvent;
import org.junit.jupiter.api.BeforeEach;
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
        @ConfigureWireMock(name = "openai", baseUrlProperties = "openai.base-url")
})
@TestPropertySource(properties = "openai.api-key=test-key")
class OpenAiContractAnalyzerTest {

    @InjectWireMock("openai")
    WireMockServer openAiMock;

    @Autowired
    OpenAiContractAnalyzer analyzer;

    private String fixture;

    @BeforeEach
    void loadFixture() throws IOException {
        fixture = new String(
                getClass().getClassLoader()
                        .getResourceAsStream("fixtures/openai-contract-response.json")
                        .readAllBytes()
        );
    }

    @Test
    void analyze_parsesContractAnalysisResultFromResponse() {
        openAiMock.stubFor(post(urlEqualTo("/v1/chat/completions")).willReturn(okJson(fixture)));

        OpenAiAnalysisResult result = analyzer.analyze("anonymized contract text", "en");

        assertThat(result.contractType()).isEqualTo("Lease Agreement");
        assertThat(result.participants()).containsExactly("<PERSON_1>", "<PERSON_2>");
        assertThat(result.issues()).hasSize(1);
        assertThat(result.issues().get(0).severity()).isEqualTo(Severity.HIGH);
        assertThat(result.issues().get(0).description()).isNotBlank();
        assertThat(result.issues().get(0).originalClause()).isNotBlank();
        assertThat(result.issues().get(0).recommendation()).isNotBlank();
    }

    @Test
    void analyze_sendsCorrectModelAndResponseFormat() {
        openAiMock.stubFor(post(urlEqualTo("/v1/chat/completions")).willReturn(okJson(fixture)));

        analyzer.analyze("text", "en");

        openAiMock.verify(postRequestedFor(urlEqualTo("/v1/chat/completions"))
                .withRequestBody(matchingJsonPath("$.model", equalTo("o4-mini")))
                .withRequestBody(matchingJsonPath("$.response_format.type", equalTo("json_schema")))
                .withRequestBody(matchingJsonPath("$.response_format.json_schema.name",
                        equalTo("contract_analysis"))));
    }

    @Test
    void analyze_sendsAuthorizationHeader() {
        openAiMock.stubFor(post(urlEqualTo("/v1/chat/completions")).willReturn(okJson(fixture)));

        analyzer.analyze("text", "en");

        openAiMock.verify(postRequestedFor(urlEqualTo("/v1/chat/completions"))
                .withHeader("Authorization", equalTo("Bearer test-key")));
    }

    @Test
    void analyze_loadsLanguageSpecificSystemPrompt() throws Exception {
        openAiMock.stubFor(post(urlEqualTo("/v1/chat/completions")).willReturn(okJson(fixture)));

        analyzer.analyze("text", "en");
        analyzer.analyze("text", "cs");

        List<ServeEvent> events = openAiMock.getAllServeEvents();
        // events are in received order: index 0 = EN, index 1 = CS
        String systemPromptEn = extractSystemMessage(events.get(0).getRequest().getBodyAsString());
        String systemPromptCs = extractSystemMessage(events.get(1).getRequest().getBodyAsString());

        assertThat(systemPromptEn).isNotBlank();
        assertThat(systemPromptCs).isNotBlank();
        assertThat(systemPromptEn).isNotEqualTo(systemPromptCs);
    }

    private String extractSystemMessage(String requestBody) throws Exception {
        JsonNode root = new ObjectMapper().readTree(requestBody);
        return root.path("messages").get(0).path("content").asText();
    }
}
