package com.nokri.taxonomy.agent;

import com.nokri.taxonomy.domain.CanonicalCompany;
import com.nokri.taxonomy.llm.CompanyJudge;
import com.nokri.taxonomy.llm.LlmJudgement;
import com.nokri.taxonomy.matching.CanonicalIndex;
import com.nokri.taxonomy.matching.MatchCandidate;
import com.nokri.taxonomy.search.SearchHit;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LlmJudgeNode implements NodeAction<MappingState> {
    private final CompanyJudge judge;
    private final CanonicalIndex index;

    public LlmJudgeNode(CompanyJudge judge, CanonicalIndex index) {
        this.judge = judge;
        this.index = index;
    }

    @Override
    public Map<String, Object> apply(MappingState state) {
        List<SearchHit> evidence = state.searchSnippets().stream()
                .map(text -> new SearchHit(text, "", ""))
                .toList();
        List<MatchCandidate> candidates = index.topMatches(
                firstNonBlank(state.resolvedCompanyName(), state.dumpCompanyName()),
                5
        );
        LlmJudgement judgement = judge.judge(state.dumpCompanyName(), evidence, candidates);
        Map<String, Object> update = new HashMap<>();
        update.put(MappingState.USED_LLM, true);
        update.put(MappingState.ROUTE, "end");
        if (!judgement.isCompany()) {
            update.put(MappingState.MATCH_STATUS, "NOT_A_COMPANY");
            update.put(MappingState.REASON, judgement.reason());
            return update;
        }
        return index.byId(judgement.canonicalId())
                .map(company -> mapped(update, company, judgement.reason()))
                .orElseGet(() -> unmatched(update, judgement.reason()));
    }

    private static Map<String, Object> mapped(Map<String, Object> update, CanonicalCompany company, String reason) {
        update.put(MappingState.CANONICAL_ID, company.canonicalId());
        update.put(MappingState.CANONICAL_NAME, company.canonicalName());
        update.put(MappingState.MATCH_STATUS, "MAPPED");
        update.put(MappingState.MATCH_SCORE, 0.9);
        update.put(MappingState.REASON, "llm: " + reason);
        return update;
    }

    private static Map<String, Object> unmatched(Map<String, Object> update, String reason) {
        update.put(MappingState.MATCH_STATUS, "UNMATCHED");
        update.put(MappingState.REASON, "llm: " + reason);
        return update;
    }

    private static String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return "";
    }
}
