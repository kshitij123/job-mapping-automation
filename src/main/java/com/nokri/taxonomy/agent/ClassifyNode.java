package com.nokri.taxonomy.agent;

import com.nokri.taxonomy.log.AgentRunLog;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.Map;

public class ClassifyNode implements NodeAction<MappingState> {
    @Override
    public Map<String, Object> apply(MappingState state) {
        if (KeywordClassifier.looksLikeCompanyKeyword(state.dumpCompanyName())) {
            return Map.of(MappingState.ROUTE, "match");
        }
        String reason = "keyword looks like email, phone, or non-company text";
        AgentRunLog.notACompany(reason);
        return Map.of(
                MappingState.ROUTE, "end",
                MappingState.MATCH_STATUS, "NOT_A_COMPANY",
                MappingState.MATCH_SCORE, 0.0,
                MappingState.REASON, reason
        );
    }
}
