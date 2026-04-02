package dev.agrp.contract;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackageClasses = ContractController.class)
public class ContractExceptionHandler {

    @ExceptionHandler(ContractAnalysisException.class)
    public ResponseEntity<Void> handle(ContractAnalysisException e) {
        HttpStatus status = switch (e.getStage()) {
            case PDF_EXTRACTION -> HttpStatus.UNPROCESSABLE_ENTITY;
            default -> HttpStatus.BAD_GATEWAY;
        };
        return ResponseEntity.status(status).build();
    }
}
