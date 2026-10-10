package com.nokri.taxonomy.llm;

import com.nokri.taxonomy.matching.MatchCandidate;
import com.nokri.taxonomy.search.SearchHit;

import java.util.List;

public interface CompanyJudge {
    LlmJudgement judge(String keyword, List<SearchHit> evidence, List<MatchCandidate> candidates);
}
