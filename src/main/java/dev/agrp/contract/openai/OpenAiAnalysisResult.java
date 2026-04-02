package dev.agrp.contract.openai;

import java.util.List;

public record OpenAiAnalysisResult(
        String contractType,
        List<String> participants,
        List<ContractIssue> issues
) {}
