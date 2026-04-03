package dev.agrp.contract;

public class ContractAnalysisException extends RuntimeException {

    public enum Stage {
        PDF_EXTRACTION, PII_ANALYSIS, AI_ANALYSIS
    }

    private final Stage stage;

    public ContractAnalysisException(Stage stage, String message, Throwable cause) {
        super(message, cause);
        this.stage = stage;
    }

    public Stage getStage() {
        return stage;
    }
}
