package dev.agrp.contract;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ContractExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ContractExceptionHandler.class);

    @ExceptionHandler(ContractAnalysisException.class)
    public ResponseEntity<ErrorResponse> handle(ContractAnalysisException e) {
        HttpStatus status = switch (e.getStage()) {
            case PDF_EXTRACTION -> HttpStatus.UNPROCESSABLE_ENTITY;
            default -> HttpStatus.BAD_GATEWAY;
        };
        log.error("Contract analysis failed at stage {}: {}", e.getStage(), e.getMessage(), e);
        String userMessage = switch (e.getStage()) {
            case PDF_EXTRACTION  -> "Could not extract text from the uploaded PDF.";
            case PII_ANALYSIS,
                 PII_ANONYMIZATION -> "Failed to process document with the PII service.";
            case AI_ANALYSIS    -> "Failed to analyze the contract. Please try again later.";
        };
        return ResponseEntity.status(status).body(new ErrorResponse(userMessage));
    }
}
