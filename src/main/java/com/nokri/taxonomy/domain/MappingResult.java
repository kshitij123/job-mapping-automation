package com.nokri.taxonomy.domain;

public record MappingResult(
        String dumpCompanyId,
        String dumpCompanyName,
        String canonicalId,
        String canonicalName,
        String matchStatus,
        double matchScore,
        String resolvedCompanyName,
        boolean usedLlm,
        boolean usedGoogle,
        String reason
) {
}
