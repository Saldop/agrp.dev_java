package dev.agrp.contract;

import dev.agrp.contract.openai.ContractIssue;
import dev.agrp.contract.openai.OpenAiAnalysisResult;
import dev.agrp.contract.openai.OpenAiContractAnalyzer;
import dev.agrp.contract.openai.Severity;
import dev.agrp.contract.pdf.PdfTextExtractor;
import dev.agrp.contract.presidio.AnonymizationResult;
import dev.agrp.contract.presidio.DeAnonymizer;
import dev.agrp.contract.presidio.PresidioClient;
import dev.agrp.contract.presidio.PresidioEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

import static dev.agrp.contract.ContractAnalysisException.Stage.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ContractAnalysisServiceTest {

    @Mock PdfTextExtractor pdfTextExtractor;
    @Mock PresidioClient presidioClient;
    @Mock OpenAiContractAnalyzer openAiContractAnalyzer;
    @Mock DeAnonymizer deAnonymizer;

    @InjectMocks
    ContractAnalysisService service;

    @Test
    void analyze_callsCollaboratorsInOrderAndReturnsDeAnonymizedResult() {
        List<PresidioEntity> entities = List.of(new PresidioEntity("PERSON", 0, 9, 0.9));
        AnonymizationResult anonymization = new AnonymizationResult(
                "anonymized text", Map.of("PERSON_1", "Jan Novák"));
        OpenAiAnalysisResult aiResult = new OpenAiAnalysisResult(
                "Lease Agreement",
                List.of("<PERSON_1>"),
                List.of(new ContractIssue("Vague clause", Severity.HIGH, "original", "fix it")));
        List<String> deAnonymizedParticipants = List.of("Jan Novák");

        when(pdfTextExtractor.extract(any())).thenReturn("raw text");
        when(presidioClient.analyze("raw text")).thenReturn(entities);
        when(presidioClient.anonymize("raw text", entities)).thenReturn(anonymization);
        when(openAiContractAnalyzer.analyze("anonymized text")).thenReturn(aiResult);
        when(deAnonymizer.deAnonymizeParticipants(List.of("<PERSON_1>"),
                Map.of("PERSON_1", "Jan Novák"))).thenReturn(deAnonymizedParticipants);

        ContractAnalysisResponse result = service.analyze(InputStream.nullInputStream());

        assertThat(result.contractType()).isEqualTo("Lease Agreement");
        assertThat(result.participants()).containsExactly("Jan Novák");
        assertThat(result.issues()).hasSize(1);

        InOrder inOrder = inOrder(pdfTextExtractor, presidioClient, openAiContractAnalyzer, deAnonymizer);
        inOrder.verify(pdfTextExtractor).extract(any());
        inOrder.verify(presidioClient).analyze(anyString());
        inOrder.verify(presidioClient).anonymize(anyString(), anyList());
        inOrder.verify(openAiContractAnalyzer).analyze(anyString());
        inOrder.verify(deAnonymizer).deAnonymizeParticipants(anyList(), anyMap());
    }

    @Test
    void analyze_wrapsExceptionWithPdfExtractionStage() {
        when(pdfTextExtractor.extract(any())).thenThrow(new RuntimeException("bad pdf"));

        assertThatThrownBy(() -> service.analyze(InputStream.nullInputStream()))
                .isInstanceOf(ContractAnalysisException.class)
                .satisfies(e -> assertThat(((ContractAnalysisException) e).getStage())
                        .isEqualTo(PDF_EXTRACTION));
    }

    @Test
    void analyze_wrapsExceptionWithPiiAnalysisStage() {
        when(pdfTextExtractor.extract(any())).thenReturn("text");
        when(presidioClient.analyze(anyString()))
                .thenThrow(new RuntimeException("presidio down"));

        assertThatThrownBy(() -> service.analyze(InputStream.nullInputStream()))
                .isInstanceOf(ContractAnalysisException.class)
                .satisfies(e -> assertThat(((ContractAnalysisException) e).getStage())
                        .isEqualTo(PII_ANALYSIS));
    }

    @Test
    void analyze_wrapsExceptionWithPiiAnonymizationStage() {
        when(pdfTextExtractor.extract(any())).thenReturn("text");
        when(presidioClient.analyze(anyString())).thenReturn(List.of());
        when(presidioClient.anonymize(anyString(), anyList()))
                .thenThrow(new RuntimeException("anonymize failed"));

        assertThatThrownBy(() -> service.analyze(InputStream.nullInputStream()))
                .isInstanceOf(ContractAnalysisException.class)
                .satisfies(e -> assertThat(((ContractAnalysisException) e).getStage())
                        .isEqualTo(PII_ANONYMIZATION));
    }

    @Test
    void analyze_wrapsExceptionWithAiAnalysisStage() {
        when(pdfTextExtractor.extract(any())).thenReturn("text");
        when(presidioClient.analyze(anyString())).thenReturn(List.of());
        when(presidioClient.anonymize(anyString(), anyList()))
                .thenReturn(new AnonymizationResult("anon text", Map.of()));
        when(openAiContractAnalyzer.analyze(anyString()))
                .thenThrow(new RuntimeException("openai error"));

        assertThatThrownBy(() -> service.analyze(InputStream.nullInputStream()))
                .isInstanceOf(ContractAnalysisException.class)
                .satisfies(e -> assertThat(((ContractAnalysisException) e).getStage())
                        .isEqualTo(AI_ANALYSIS));
    }
}
