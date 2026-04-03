package dev.agrp.contract;

import dev.agrp.contract.openai.OpenAiAnalysisResult;
import dev.agrp.contract.openai.OpenAiContractAnalyzer;
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
    private final DeAnonymizer deAnonymizer;

    public ContractAnalysisService(
            PdfTextExtractor pdfTextExtractor,
            PresidioClient presidioClient,
            OpenAiContractAnalyzer openAiContractAnalyzer,
            DeAnonymizer deAnonymizer) {
        this.pdfTextExtractor = pdfTextExtractor;
        this.presidioClient = presidioClient;
        this.openAiContractAnalyzer = openAiContractAnalyzer;
        this.deAnonymizer = deAnonymizer;
    }

    public ContractAnalysisResponse analyze(InputStream pdf) {
        String text = extractText(pdf);
        List<PresidioEntity> entities = detectEntities(text);
        AnonymizationResult anonymization = anonymize(text, entities);
        OpenAiAnalysisResult aiResult = analyzeWithAi(anonymization.anonymizedText());
        List<String> participants = deAnonymizer.deAnonymizeParticipants(
                aiResult.participants(), anonymization.tokenToReal());
        return new ContractAnalysisResponse(aiResult.contractType(), participants, aiResult.issues());
    }

    private String extractText(InputStream pdf) {
        try {
            return pdfTextExtractor.extract(pdf);
        } catch (Exception e) {
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

    private AnonymizationResult anonymize(String text, List<PresidioEntity> entities) {
        try {
            return presidioClient.anonymize(text, entities);
        } catch (Exception e) {
            throw new ContractAnalysisException(
                    ContractAnalysisException.Stage.PII_ANONYMIZATION, "Failed to anonymize text", e);
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
