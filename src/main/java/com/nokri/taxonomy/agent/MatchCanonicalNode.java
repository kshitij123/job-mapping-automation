package com.nokri.taxonomy.agent;

import com.nokri.taxonomy.config.AppConfig;
import com.nokri.taxonomy.domain.CanonicalCompany;
import com.nokri.taxonomy.log.AgentRunLog;
import com.nokri.taxonomy.matching.CanonicalIndex;
import com.nokri.taxonomy.matching.MatchCandidate;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class MatchCanonicalNode implements NodeAction<MappingState> {
    private final CanonicalIndex index;
    private final AppConfig config;

    public MatchCanonicalNode(CanonicalIndex index, AppConfig config) {
        this.index = index;
        this.config = config;
    }

    @Override
    public Map<String, Object> apply(MappingState state) {
        Map<String, Object> update = new HashMap<>();
        if (!state.searched()) {
            Optional<CanonicalCompany> exact = index.exactWritten(state.dumpCompanyName());
            if (exact.isPresent()) {
                CanonicalCompany hit = exact.get();
                AgentRunLog.exactCanonical(state.dumpCompanyName(), hit.canonicalId(), hit.canonicalName());
                update.put(MappingState.CANONICAL_ID, hit.canonicalId());
                update.put(MappingState.CANONICAL_NAME, hit.canonicalName());
                update.put(MappingState.MATCH_STATUS, "MAPPED");
                update.put(MappingState.MATCH_SCORE, 1.0);
                update.put(MappingState.REASON, "dump name is written exactly as canonical");
                update.put(MappingState.ROUTE, "end");
                return update;
            }
            AgentRunLog.willGoogle(state.dumpCompanyName());
            update.put(MappingState.ROUTE, "search");
            update.put(MappingState.REASON, "dump name is not an exact canonical name");
            return update;
        }

        Optional<MatchCandidate> best = index.bestMatch(state.resolvedCompanyName());
        if (best.isPresent() && best.get().score() >= config.acceptScore()) {
            MatchCandidate hit = best.get();
            AgentRunLog.canonicalMatched(
                    state.resolvedCompanyName(),
                    hit.company().canonicalId(),
                    hit.company().canonicalName(),
                    hit.method()
            );
            update.put(MappingState.CANONICAL_ID, hit.company().canonicalId());
            update.put(MappingState.CANONICAL_NAME, hit.company().canonicalName());
            update.put(MappingState.MATCH_STATUS, "MAPPED");
            update.put(MappingState.MATCH_SCORE, hit.score());
            update.put(MappingState.REASON, "google official name matched canonical (" + hit.method() + ")");
            update.put(MappingState.ROUTE, "end");
            return update;
        }

        AgentRunLog.canonicalMiss(state.resolvedCompanyName());
        if (best.isPresent() && best.get().score() >= config.llmScore() && config.llmEnabled()) {
            AgentRunLog.askingLlm("Official-name score is uncertain.");
            update.put(MappingState.MATCH_SCORE, best.get().score());
            update.put(MappingState.ROUTE, "llm");
            update.put(MappingState.REASON, "uncertain after search, ask llm");
            return update;
        }

        if (config.llmEnabled() && !state.resolvedCompanyName().isBlank()) {
            AgentRunLog.askingLlm("Google name was not in the canonical sheet.");
            update.put(MappingState.ROUTE, "llm");
            update.put(MappingState.REASON, "google name not in canonical, ask llm");
            return update;
        }

        update.put(MappingState.MATCH_STATUS, "UNMATCHED");
        update.put(MappingState.MATCH_SCORE, best.map(MatchCandidate::score).orElse(0.0));
        update.put(MappingState.REASON, "google official name not found in canonical");
        update.put(MappingState.ROUTE, "end");
        return update;
    }
}
