package com.nokri.taxonomy.agent;

import com.nokri.taxonomy.config.AppConfig;
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
        Optional<MatchCandidate> best = index.bestMatch(state.dumpCompanyName(), state.resolvedCompanyName());
        Map<String, Object> update = new HashMap<>();
        if (best.isPresent() && best.get().score() >= config.acceptScore()) {
            MatchCandidate hit = best.get();
            update.put(MappingState.CANONICAL_ID, hit.company().canonicalId());
            update.put(MappingState.CANONICAL_NAME, hit.company().canonicalName());
            update.put(MappingState.MATCH_STATUS, "MAPPED");
            update.put(MappingState.MATCH_SCORE, hit.score());
            update.put(MappingState.REASON, "local " + hit.method() + " match");
            update.put(MappingState.ROUTE, "end");
            return update;
        }

        if (!state.searched()) {
            update.put(MappingState.ROUTE, "search");
            best.ifPresent(hit -> update.put(MappingState.MATCH_SCORE, hit.score()));
            return update;
        }

        if (best.isPresent() && best.get().score() >= config.llmScore() && config.llmEnabled()) {
            update.put(MappingState.MATCH_SCORE, best.get().score());
            update.put(MappingState.ROUTE, "llm");
            update.put(MappingState.REASON, "uncertain after search, ask llm");
            return update;
        }

        update.put(MappingState.MATCH_STATUS, "UNMATCHED");
        update.put(MappingState.MATCH_SCORE, best.map(MatchCandidate::score).orElse(0.0));
        update.put(MappingState.REASON, state.searched()
                ? "no confident canonical match after search"
                : "no confident canonical match");
        update.put(MappingState.ROUTE, "end");
        return update;
    }
}
