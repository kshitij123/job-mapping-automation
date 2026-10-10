package com.nokri.taxonomy.agent;

import com.nokri.taxonomy.search.CompanySearchClient;
import com.nokri.taxonomy.search.SearchHit;
import org.bsc.langgraph4j.action.NodeAction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GoogleSearchNode implements NodeAction<MappingState> {
    private final CompanySearchClient searchClient;

    public GoogleSearchNode(CompanySearchClient searchClient) {
        this.searchClient = searchClient;
    }

    @Override
    public Map<String, Object> apply(MappingState state) {
        List<SearchHit> hits = searchClient.search(state.dumpCompanyName());
        Map<String, Object> update = new HashMap<>();
        update.put(MappingState.SEARCHED, true);
        update.put(MappingState.USED_GOOGLE, true);
        update.put(MappingState.RESOLVED_NAME, CompanyNameExtractor.fromHits(hits));
        update.put(MappingState.SEARCH_SNIPPETS, hits.stream().map(SearchHit::combinedText).toList());
        update.put(MappingState.ROUTE, "match");
        if (hits.isEmpty()) {
            update.put(MappingState.REASON, "search returned no company evidence");
        }
        return update;
    }
}
