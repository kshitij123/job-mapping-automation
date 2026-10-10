package com.nokri.taxonomy.agent;

import com.nokri.taxonomy.config.AppConfig;
import com.nokri.taxonomy.domain.CanonicalCompany;
import com.nokri.taxonomy.domain.DumpKeyword;
import com.nokri.taxonomy.domain.MappingResult;
import com.nokri.taxonomy.matching.CanonicalIndex;
import com.nokri.taxonomy.search.OfflineCompanySearchClient;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class CompanyMappingGraphTest {
    @Test
    void mapsTcsAliasAndRejectsPhoneWithoutLlm() throws Exception {
        CanonicalIndex index = new CanonicalIndex(List.of(
                new CanonicalCompany("C1001", "TCS"),
                new CanonicalCompany("C1002", "Infosys Limited")
        ));
        AppConfig config = new AppConfig(
                Path.of("data/canonical.xlsx"),
                Path.of("data/dump.xlsx"),
                Path.of("data/mapping.xlsx"),
                0.82,
                0.50,
                false,
                AppConfig.DEFAULT_LOCAL_LLM_BASE_URL,
                AppConfig.DEFAULT_MLX_MODEL,
                "mlx-local",
                null,
                null
        );
        CompanyMappingGraph graph = new CompanyMappingGraph(index, new OfflineCompanySearchClient(), null, config);

        MappingResult mapped = graph.map(new DumpKeyword("67435", "Tata Consultancy Services"));
        assertEquals("MAPPED", mapped.matchStatus());
        assertEquals("C1001", mapped.canonicalId());

        MappingResult rejected = graph.map(new DumpKeyword("69101", "9876543210"));
        assertEquals("NOT_A_COMPANY", rejected.matchStatus());
        assertNull(rejected.canonicalId());
    }
}
