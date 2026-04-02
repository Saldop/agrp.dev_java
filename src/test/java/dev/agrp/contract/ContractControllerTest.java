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
import static org.mockito.ArgumentMatchers.eq;
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
            "file", "contract.pdf", "application/pdf", "pdf-bytes".getBytes());

    @Test
    void analyze_returns200WithAnalysisResult() throws Exception {
        ContractIssue issue = new ContractIssue("Vague clause", Severity.HIGH, "original", "fix it");
        ContractAnalysisResult result = new ContractAnalysisResult(
                "Lease Agreement", List.of("Jan Novák"), List.of(issue));
        when(service.analyze(any(), eq("en"))).thenReturn(result);

        mockMvc.perform(multipart("/contracts/analyze").file(PDF_FILE).param("language", "en"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contractType").value("Lease Agreement"))
                .andExpect(jsonPath("$.participants[0]").value("Jan Novák"))
                .andExpect(jsonPath("$.issues[0].severity").value("HIGH"));
    }

    @Test
    void analyze_usesDefaultLanguageEn() throws Exception {
        when(service.analyze(any(), eq("en"))).thenReturn(
                new ContractAnalysisResult("NDA", List.of(), List.of()));

        mockMvc.perform(multipart("/contracts/analyze").file(PDF_FILE))
                .andExpect(status().isOk());
    }

    @Test
    void analyze_returns400ForUnsupportedLanguage() throws Exception {
        mockMvc.perform(multipart("/contracts/analyze").file(PDF_FILE).param("language", "de"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void analyze_returns422WhenPdfExtractionFails() throws Exception {
        when(service.analyze(any(), any())).thenThrow(
                new ContractAnalysisException(PDF_EXTRACTION, "bad pdf", null));

        mockMvc.perform(multipart("/contracts/analyze").file(PDF_FILE))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void analyze_returns502WhenPiiAnalysisFails() throws Exception {
        when(service.analyze(any(), any())).thenThrow(
                new ContractAnalysisException(PII_ANALYSIS, "presidio down", null));

        mockMvc.perform(multipart("/contracts/analyze").file(PDF_FILE))
                .andExpect(status().isBadGateway());
    }

    @Test
    void analyze_returns502WhenAiAnalysisFails() throws Exception {
        when(service.analyze(any(), any())).thenThrow(
                new ContractAnalysisException(AI_ANALYSIS, "openai error", null));

        mockMvc.perform(multipart("/contracts/analyze").file(PDF_FILE))
                .andExpect(status().isBadGateway());
    }
}
