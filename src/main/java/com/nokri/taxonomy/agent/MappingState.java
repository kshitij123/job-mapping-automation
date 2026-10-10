package com.nokri.taxonomy.agent;

import com.nokri.taxonomy.domain.MappingResult;
import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MappingState extends AgentState {
    public static final String DUMP_COMPANY_ID = "dumpCompanyId";
    public static final String DUMP_COMPANY_NAME = "dumpCompanyName";
    public static final String DUMP_EMAIL_WEBSITE = "dumpEmailWebsite";
    public static final String COMPANY_DOMAIN = "companyDomain";
    public static final String ROUTE = "route";
    public static final String SEARCHED = "searched";
    public static final String SEARCH_SNIPPETS = "searchSnippets";
    public static final String RESOLVED_NAME = "resolvedCompanyName";
    public static final String CANONICAL_ID = "canonicalId";
    public static final String CANONICAL_NAME = "canonicalName";
    public static final String MATCH_STATUS = "matchStatus";
    public static final String MATCH_SCORE = "matchScore";
    public static final String USED_LLM = "usedLlm";
    public static final String USED_GOOGLE = "usedGoogle";
    public static final String REASON = "reason";

    public static final Map<String, Channel<?>> SCHEMA;

    static {
        Map<String, Channel<?>> schema = new HashMap<>();
        schema.put(SEARCH_SNIPPETS, Channels.appender(ArrayList::new));
        SCHEMA = Map.copyOf(schema);
    }

    public MappingState(Map<String, Object> initData) {
        super(initData);
    }

    public String dumpCompanyId() {
        return string(DUMP_COMPANY_ID);
    }

    public String dumpCompanyName() {
        return string(DUMP_COMPANY_NAME);
    }

    public String dumpEmailWebsite() {
        return string(DUMP_EMAIL_WEBSITE);
    }

    public String companyDomain() {
        return string(COMPANY_DOMAIN);
    }

    public String route() {
        return string(ROUTE);
    }

    public boolean searched() {
        return Boolean.TRUE.equals(value(SEARCHED).orElse(false));
    }

    @SuppressWarnings("unchecked")
    public List<String> searchSnippets() {
        return (List<String>) value(SEARCH_SNIPPETS).orElse(List.of());
    }

    public String resolvedCompanyName() {
        return string(RESOLVED_NAME);
    }

    public String canonicalId() {
        return string(CANONICAL_ID);
    }

    public String canonicalName() {
        return string(CANONICAL_NAME);
    }

    public String matchStatus() {
        return string(MATCH_STATUS);
    }

    public double matchScore() {
        return value(MATCH_SCORE).map(v -> ((Number) v).doubleValue()).orElse(0.0);
    }

    public boolean usedLlm() {
        return Boolean.TRUE.equals(value(USED_LLM).orElse(false));
    }

    public boolean usedGoogle() {
        return Boolean.TRUE.equals(value(USED_GOOGLE).orElse(false));
    }

    public String reason() {
        return string(REASON);
    }

    public MappingResult toResult() {
        return new MappingResult(
                dumpCompanyId(),
                dumpCompanyName(),
                blankToNull(canonicalId()),
                blankToNull(canonicalName()),
                blankToNull(matchStatus()) == null ? "UNMATCHED" : matchStatus(),
                matchScore(),
                blankToNull(resolvedCompanyName()),
                usedLlm(),
                usedGoogle(),
                reason()
        );
    }

    private String string(String key) {
        return value(key).map(Object::toString).orElse("");
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
