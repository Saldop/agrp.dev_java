package dev.agrp.contract.openai;

public record ContractIssue(
        String description,
        Severity severity,
        String originalClause,
        String recommendation
) {}
