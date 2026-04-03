package dev.agrp.contract;

import dev.agrp.contract.openai.OpenAiAnalysisResult;
import dev.agrp.contract.openai.OpenAiContractAnalyzer;
import dev.agrp.contract.pdf.PdfExtractionException;
import dev.agrp.contract.pdf.PdfTextExtractor;
import dev.agrp.contract.presidio.AnonymizationResult;
import dev.agrp.contract.presidio.DeAnonymizer;
import dev.agrp.contract.presidio.PresidioClient;
import dev.agrp.contract.presidio.PresidioEntity;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.List;

@Service
public class ContractAnalysisService {

    private final PdfTextExtractor pdfTextExtractor;
    private final PresidioClient presidioClient;
    private final OpenAiContractAnalyzer openAiContractAnalyzer;

    public ContractAnalysisService(
            PdfTextExtractor pdfTextExtractor,
            PresidioClient presidioClient,
            OpenAiContractAnalyzer openAiContractAnalyzer) {
        this.pdfTextExtractor = pdfTextExtractor;
        this.presidioClient = presidioClient;
        this.openAiContractAnalyzer = openAiContractAnalyzer;
    }

    public ContractAnalysisResponse analyze(InputStream pdf) {
        String text = extractText(pdf);
        List<PresidioEntity> entities = detectEntities(text);
        AnonymizationResult anonymization = presidioClient.anonymize(text, entities);
        OpenAiAnalysisResult aiResult = analyzeWithAi(anonymization.anonymizedText());
        List<String> participants = DeAnonymizer.deAnonymizeParticipants(
                aiResult.participants(), anonymization.tokenToReal());
        return new ContractAnalysisResponse(aiResult.contractType(), participants, aiResult.issues());
    }

    private String extractText(InputStream pdf) {
        try {
            return pdfTextExtractor.extract(pdf);
        } catch (PdfExtractionException e) {
            throw new ContractAnalysisException(
                    ContractAnalysisException.Stage.PDF_EXTRACTION, "Failed to extract PDF text", e);
        }
    }

    private List<PresidioEntity> detectEntities(String text) {
        try {
            return presidioClient.analyze(text);
        } catch (Exception e) {
            throw new ContractAnalysisException(
                    ContractAnalysisException.Stage.PII_ANALYSIS, "Failed to detect PII entities", e);
        }
    }

    private OpenAiAnalysisResult analyzeWithAi(String anonymizedText) {
        try {
            return openAiContractAnalyzer.analyze(anonymizedText);
        } catch (Exception e) {
            throw new ContractAnalysisException(
                    ContractAnalysisException.Stage.AI_ANALYSIS, "Failed to analyze contract with AI", e);
        }
    }
}
