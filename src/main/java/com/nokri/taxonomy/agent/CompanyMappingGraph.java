package com.nokri.taxonomy.agent;

import com.nokri.taxonomy.config.AppConfig;
import com.nokri.taxonomy.domain.DumpKeyword;
import com.nokri.taxonomy.domain.MappingResult;
import com.nokri.taxonomy.llm.CompanyJudge;
import com.nokri.taxonomy.matching.CanonicalIndex;
import com.nokri.taxonomy.search.CompanySearchClient;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.GraphStateException;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncCommandAction;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;
import static org.bsc.langgraph4j.action.AsyncEdgeAction.edge_async;
import static org.bsc.langgraph4j.action.AsyncNodeAction.node_async;

public class CompanyMappingGraph {
    private final CompiledGraph<MappingState> graph;

    public CompanyMappingGraph(
            CanonicalIndex index,
            CompanySearchClient searchClient,
            CompanyJudge judge,
            AppConfig config
    ) throws GraphStateException {
        ClassifyNode classify = new ClassifyNode();
        MatchCanonicalNode match = new MatchCanonicalNode(index, config);
        GoogleSearchNode search = new GoogleSearchNode(searchClient);
        this.graph = new StateGraph<>(MappingState.SCHEMA, MappingState::new)
                .addNode("classify", node_async(classify))
                .addNode("match", node_async(match))
                .addNode("search", node_async(search))
                .addNode("llm", judge == null
                        ? node_async(state -> Map.of(
                        MappingState.MATCH_STATUS, "UNMATCHED",
                        MappingState.REASON, "llm requested but not configured",
                        MappingState.ROUTE, "end"))
                        : node_async(new LlmJudgeNode(judge, index)))
                .addEdge(START, "classify")
                .addConditionalEdges("classify", route(), Map.of(
                        "match", "match",
                        "end", END
                ))
                .addConditionalEdges("match", route(), Map.of(
                        "search", "search",
                        "llm", "llm",
                        "end", END
                ))
                .addEdge("search", "match")
                .addEdge("llm", END)
                .compile();
    }

    public MappingResult map(DumpKeyword keyword) {
        Map<String, Object> input = new HashMap<>();
        input.put(MappingState.DUMP_COMPANY_ID, keyword.companyId());
        input.put(MappingState.DUMP_COMPANY_NAME, keyword.companyName());
        input.put(MappingState.DUMP_EMAIL_WEBSITE, keyword.emailWebsite() == null ? "" : keyword.emailWebsite());
        MappingState finalState = graph.invoke(input).orElseThrow();
        return finalState.toResult();
    }

    private static AsyncCommandAction<MappingState> route() {
        return AsyncCommandAction.of(edge_async(state -> {
            String route = state.route();
            return (route == null || route.isBlank()) ? "end" : route;
        }));
    }

    public CompiledGraph<MappingState> compiled() {
        return Objects.requireNonNull(graph);
    }
}
