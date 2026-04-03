package dev.agrp.contract;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Tag(name = "Contracts", description = "Contract analysis")
@RestController
@RequestMapping("/contracts")
public class ContractController {

    private final ContractAnalysisService service;

    public ContractController(ContractAnalysisService service) {
        this.service = service;
    }

    @Operation(
            summary = "Analyze a contract PDF",
            description = "Extracts text from the uploaded PDF, anonymizes PII, analyzes the contract " +
                    "with an AI reasoning model, and returns structured findings."
    )
    @ApiResponse(responseCode = "200", description = "Analysis completed successfully",
            content = @Content(mediaType = MediaType.APPLICATION_JSON_VALUE,
                    schema = @Schema(implementation = ContractAnalysisResponse.class)))
    @ApiResponse(responseCode = "415", description = "Uploaded file is not a PDF",
            content = @Content)
    @ApiResponse(responseCode = "422", description = "Could not extract text from the uploaded PDF",
            content = @Content)
    @ApiResponse(responseCode = "502", description = "Upstream service (Presidio or OpenAI) unavailable",
            content = @Content)
    @PostMapping(value = "/analyze",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ContractAnalysisResponse> analyze(
            @Parameter(description = "PDF contract file", required = true)
            @RequestPart("file") MultipartFile file) {

        if (!"application/pdf".equalsIgnoreCase(file.getContentType())) {
            return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).build();
        }
        InputStream stream;
        try {
            stream = file.getInputStream();
        } catch (IOException e) {
            throw new ContractAnalysisException(
                    ContractAnalysisException.Stage.PDF_EXTRACTION, "Failed to read uploaded file", e);
        }
        return ResponseEntity.ok(service.analyze(stream));
    }
}
