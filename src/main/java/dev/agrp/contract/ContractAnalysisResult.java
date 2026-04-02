package dev.agrp.contract;

import dev.agrp.contract.openai.ContractIssue;

import java.util.List;

public record ContractAnalysisResult(
        String contractType,
        List<String> participants,
        List<ContractIssue> issues
) {}
