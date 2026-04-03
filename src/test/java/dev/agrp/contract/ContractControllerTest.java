package dev.agrp.contract;

import dev.agrp.contract.openai.ContractIssue;
import dev.agrp.contract.openai.Severity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static dev.agrp.contract.ContractAnalysisException.Stage.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ContractController.class)
class ContractControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockBean
    ContractAnalysisService service;

    private static final MockMultipartFile PDF_FILE = new MockMultipartFile(
            "file", "contract.pdf", "application/pdf", "%PDF-fake".getBytes());

    @Test
    void analyze_returns200WithAnalysisResult() throws Exception {
        ContractIssue issue = new ContractIssue("Vague clause", Severity.HIGH, "original", "fix it");
        ContractAnalysisResponse result = new ContractAnalysisResponse(
                "Lease Agreement", List.of("Jan Novák"), List.of(issue));
        when(service.analyze(any())).thenReturn(result);

        mockMvc.perform(multipart("/contracts/analyze").file(PDF_FILE))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contractType").value("Lease Agreement"))
                .andExpect(jsonPath("$.participants[0]").value("Jan Novák"))
                .andExpect(jsonPath("$.issues[0].severity").value("HIGH"));
    }

    @Test
    void analyze_returns415WhenFileIsNotPdf() throws Exception {
        MockMultipartFile textFile = new MockMultipartFile(
                "file", "contract.txt", "text/plain", "not a pdf".getBytes());

        mockMvc.perform(multipart("/contracts/analyze").file(textFile))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void analyze_returns422WhenPdfExtractionFails() throws Exception {
        when(service.analyze(any())).thenThrow(
                new ContractAnalysisException(PDF_EXTRACTION, "bad pdf", null));

        mockMvc.perform(multipart("/contracts/analyze").file(PDF_FILE))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.error").value("Could not extract text from the uploaded PDF."));
    }

    @Test
    void analyze_returns502WhenPiiAnalysisFails() throws Exception {
        when(service.analyze(any())).thenThrow(
                new ContractAnalysisException(PII_ANALYSIS, "presidio down", null));

        mockMvc.perform(multipart("/contracts/analyze").file(PDF_FILE))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("Failed to process document with the PII service."));
    }

    @Test
    void analyze_returns502WhenAiAnalysisFails() throws Exception {
        when(service.analyze(any())).thenThrow(
                new ContractAnalysisException(AI_ANALYSIS, "openai error", null));

        mockMvc.perform(multipart("/contracts/analyze").file(PDF_FILE))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.error").value("Failed to analyze the contract. Please try again later."));
    }
}
