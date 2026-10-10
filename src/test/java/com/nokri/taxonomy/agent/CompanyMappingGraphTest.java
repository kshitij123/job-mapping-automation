package com.nokri.taxonomy.agent;

import com.nokri.taxonomy.config.AppConfig;
import com.nokri.taxonomy.domain.CanonicalCompany;
import com.nokri.taxonomy.domain.DumpKeyword;
import com.nokri.taxonomy.domain.MappingResult;
import com.nokri.taxonomy.llm.CompanyJudge;
import com.nokri.taxonomy.llm.LlmJudgement;
import com.nokri.taxonomy.matching.CanonicalIndex;
import com.nokri.taxonomy.matching.MatchCandidate;
import com.nokri.taxonomy.search.CompanySearchClient;
import com.nokri.taxonomy.search.SearchHit;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CompanyMappingGraphTest {
    private final CanonicalIndex index = new CanonicalIndex(List.of(
            new CanonicalCompany("C1001", "TCS"),
            new CanonicalCompany("C1002", "Infosys Limited")
    ));

    @Test
    void exactCanonicalNameMapsWithoutSearchOrLlm() throws Exception {
        RecordingSearch search = new RecordingSearch();
        RecordingJudge llm = new RecordingJudge("C1001", true);
        CompanyMappingGraph graph = new CompanyMappingGraph(index, search, llm, config(true));

        MappingResult mapped = graph.map(new DumpKeyword("750300", "TCS", "recruiter@tcs.test"));
        assertEquals("MAPPED", mapped.matchStatus());
        assertEquals("C1001", mapped.canonicalId());
        assertFalse(mapped.usedGoogle());
        assertFalse(mapped.usedLlm());
        assertEquals(0, search.calls.size());
        assertEquals(0, llm.calls.get());
    }

    @Test
    void mismatchWithCompanyDomainSearchesGoogleThenMapsOfficialName() throws Exception {
        RecordingSearch search = new RecordingSearch();
        search.hits = List.of(new SearchHit(
                "Tata Consultancy Services",
                "Official website of Tata Consultancy Services",
                "https://www.tcs-india.com"
        ));
        RecordingJudge llm = new RecordingJudge("C1001", true);
        CompanyMappingGraph graph = new CompanyMappingGraph(index, search, llm, config(true));

        MappingResult mapped = graph.map(new DumpKeyword("750299", "tcs", "hr@tcs-india.com"));
        assertEquals("MAPPED", mapped.matchStatus());
        assertEquals("C1001", mapped.canonicalId());
        assertEquals("TCS", mapped.canonicalName());
        assertEquals("Tata Consultancy Services", mapped.resolvedCompanyName());
        assertTrue(mapped.usedGoogle());
        assertFalse(mapped.usedLlm());
        assertEquals(1, search.calls.size());
        assertEquals("tcs", search.calls.getFirst().keyword());
        assertEquals("tcs-india.com", search.calls.getFirst().domain());
        assertEquals(0, llm.calls.get());
    }

    @Test
    void personalEmailIgnoresDomainAndStillGooglesCompanyName() throws Exception {
        RecordingSearch search = new RecordingSearch();
        search.hits = List.of(new SearchHit(
                "Infosys Limited",
                "Infosys Limited official website",
                "https://www.infosys.com"
        ));
        RecordingJudge llm = new RecordingJudge("C1002", true);
        CompanyMappingGraph graph = new CompanyMappingGraph(index, search, llm, config(true));

        MappingResult mapped = graph.map(new DumpKeyword("68012", "Infosys", "campus@gmail.com"));
        assertEquals("MAPPED", mapped.matchStatus());
        assertEquals("C1002", mapped.canonicalId());
        assertTrue(mapped.usedGoogle());
        assertFalse(mapped.usedLlm());
        assertEquals(1, search.calls.size());
        assertEquals("Infosys", search.calls.getFirst().keyword());
        assertEquals("", search.calls.getFirst().domain());
    }

    @Test
    void googleNameNotInCanonicalGoesToLlm() throws Exception {
        RecordingSearch search = new RecordingSearch();
        search.hits = List.of(new SearchHit(
                "Weird Corporation",
                "Weird Corporation official",
                "https://www.weirdcorp.example"
        ));
        RecordingJudge llm = new RecordingJudge("C1002", true);
        CompanyMappingGraph graph = new CompanyMappingGraph(index, search, llm, config(true));

        MappingResult mapped = graph.map(new DumpKeyword("99901", "weirdcorp", "info@weirdcorp.example"));
        assertEquals("MAPPED", mapped.matchStatus());
        assertEquals("C1002", mapped.canonicalId());
        assertTrue(mapped.usedGoogle());
        assertTrue(mapped.usedLlm());
        assertEquals(1, search.calls.size());
        assertEquals(1, llm.calls.get());
        assertEquals("weirdcorp.example", search.calls.getFirst().domain());
    }

    @Test
    void phoneIsRejectedBeforeSearchOrLlm() throws Exception {
        RecordingSearch search = new RecordingSearch();
        RecordingJudge llm = new RecordingJudge("C1001", true);
        CompanyMappingGraph graph = new CompanyMappingGraph(index, search, llm, config(true));

        MappingResult rejected = graph.map(new DumpKeyword("69101", "9876543210", "a@gmail.com"));
        assertEquals("NOT_A_COMPANY", rejected.matchStatus());
        assertNull(rejected.canonicalId());
        assertEquals(0, search.calls.size());
        assertEquals(0, llm.calls.get());
    }

    private static AppConfig config(boolean llm) {
        return new AppConfig(
                Path.of("data/canonical.xlsx"),
                Path.of("data/dump.xlsx"),
                Path.of("data/mapping.xlsx"),
                0.82,
                0.50,
                llm,
                AppConfig.DEFAULT_LOCAL_LLM_BASE_URL,
                AppConfig.DEFAULT_MLX_MODEL,
                "mlx-local",
                null,
                null
        );
    }

    private static final class RecordingSearch implements CompanySearchClient {
        record Call(String keyword, String domain) {
        }

        final List<Call> calls = new ArrayList<>();
        List<SearchHit> hits = List.of();

        @Override
        public List<SearchHit> search(String keyword, String companyDomain) {
            calls.add(new Call(keyword, companyDomain == null ? "" : companyDomain));
            return hits;
        }
    }

    private static final class RecordingJudge implements CompanyJudge {
        final AtomicInteger calls = new AtomicInteger();
        private final String canonicalId;
        private final boolean isCompany;

        RecordingJudge(String canonicalId, boolean isCompany) {
            this.canonicalId = canonicalId;
            this.isCompany = isCompany;
        }

        @Override
        public LlmJudgement judge(String keyword, List<SearchHit> evidence, List<MatchCandidate> candidates) {
            calls.incrementAndGet();
            return new LlmJudgement(isCompany, canonicalId, "test llm");
        }
    }
}
