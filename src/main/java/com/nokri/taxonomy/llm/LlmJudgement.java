package com.nokri.taxonomy.llm;

public record LlmJudgement(boolean isCompany, String canonicalId, String reason) {
}
