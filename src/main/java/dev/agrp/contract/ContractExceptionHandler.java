package dev.agrp.contract;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = ContractController.class)
public class ContractExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ContractExceptionHandler.class);

    @ExceptionHandler(ContractAnalysisException.class)
    public ResponseEntity<Void> handle(ContractAnalysisException e) {
        HttpStatus status = switch (e.getStage()) {
            case PDF_EXTRACTION -> HttpStatus.UNPROCESSABLE_ENTITY;
            default -> HttpStatus.BAD_GATEWAY;
        };
        log.error("Contract analysis failed at stage {}: {}", e.getStage(), e.getMessage(), e);
        return ResponseEntity.status(status).build();
    }
}
