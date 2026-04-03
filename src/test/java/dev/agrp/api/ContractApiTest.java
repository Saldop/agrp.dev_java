package dev.agrp.api;

import com.github.tomakehurst.wiremock.WireMockServer;
import io.restassured.RestAssured;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.TestPropertySource;
import org.wiremock.spring.ConfigureWireMock;
import org.wiremock.spring.EnableWireMock;
import org.wiremock.spring.InjectWireMock;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static com.github.tomakehurst.wiremock.client.WireMock.okJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EnableWireMock({
        @ConfigureWireMock(name = "presidio-analyzer", baseUrlProperties = "presidio.analyzer-url"),
        @ConfigureWireMock(name = "openai", baseUrlProperties = "openai.base-url")
})
@TestPropertySource(properties = "openai.api-key=test-key")
class ContractApiTest {

    @LocalServerPort
    int port;

    @InjectWireMock("presidio-analyzer")
    WireMockServer analyzerMock;

    @InjectWireMock("openai")
    WireMockServer openAiMock;

    private byte[] testPdf;
    private String openAiFixture;

    @BeforeEach
    void setUp() throws Exception {
        RestAssured.port = port;
        testPdf = buildTestPdf("This is a sample lease agreement between the parties.");
        openAiFixture = new String(
                getClass().getClassLoader()
                        .getResourceAsStream("fixtures/openai-contract-response.json")
                        .readAllBytes()
        );
        // Return no entities so anonymize() is a no-op
        analyzerMock.stubFor(post(urlEqualTo("/analyze")).willReturn(okJson("[]")));
        openAiMock.stubFor(post(urlEqualTo("/v1/chat/completions")).willReturn(okJson(openAiFixture)));
    }

    @Test
    void analyze_returnsStructuredAnalysisForValidPdf() {
        given()
                .multiPart("file", "contract.pdf", testPdf, "application/pdf")
        .when()
                .post("/contracts/analyze")
        .then()
                .statusCode(200)
                .body("contractType", equalTo("Lease Agreement"))
                .body("participants", not(empty()))
                .body("issues", not(empty()))
                .body("issues[0].severity", equalTo("HIGH"))
                .body("issues[0].description", not(emptyString()))
                .body("issues[0].originalClause", not(emptyString()))
                .body("issues[0].recommendation", not(emptyString()));

        analyzerMock.verify(1, postRequestedFor(urlEqualTo("/analyze")));
        openAiMock.verify(1, postRequestedFor(urlEqualTo("/v1/chat/completions")));
    }

    private static byte[] buildTestPdf(String text) throws IOException {
        try (PDDocument doc = new PDDocument();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream content = new PDPageContentStream(doc, page)) {
                content.beginText();
                content.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                content.newLineAtOffset(50, 700);
                content.showText(text);
                content.endText();
            }
            doc.save(out);
            return out.toByteArray();
        }
    }
}
