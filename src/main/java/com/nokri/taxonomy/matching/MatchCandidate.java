package com.nokri.taxonomy.matching;

import com.nokri.taxonomy.domain.CanonicalCompany;

public record MatchCandidate(CanonicalCompany company, double score, String method) {
}
