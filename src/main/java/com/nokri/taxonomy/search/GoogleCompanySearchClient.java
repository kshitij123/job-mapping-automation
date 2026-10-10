package com.nokri.taxonomy.search;

import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.WebSearchOrganicResult;
import dev.langchain4j.web.search.WebSearchResults;
import dev.langchain4j.web.search.google.customsearch.GoogleCustomWebSearchEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class GoogleCompanySearchClient implements CompanySearchClient {
    private static final Logger log = LoggerFactory.getLogger(GoogleCompanySearchClient.class);
    private final WebSearchEngine engine;

    public GoogleCompanySearchClient(String apiKey, String cseId) {
        this.engine = GoogleCustomWebSearchEngine.builder()
                .apiKey(apiKey)
                .csi(cseId)
                .build();
    }

    @Override
    public List<SearchHit> search(String keyword) {
        WebSearchResults results = engine.search(keyword + " company official");
        List<SearchHit> hits = new ArrayList<>();
        if (results == null || results.results() == null) {
            return hits;
        }
        for (WebSearchOrganicResult result : results.results()) {
            hits.add(new SearchHit(
                    result.title(),
                    result.snippet(),
                    result.url() == null ? null : result.url().toString()
            ));
        }
        log.info("Google returned {} hits for '{}'", hits.size(), keyword);
        return hits;
    }
}
