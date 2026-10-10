package com.nokri.taxonomy;

import com.nokri.taxonomy.agent.CompanyMappingGraph;
import com.nokri.taxonomy.config.AppConfig;
import com.nokri.taxonomy.domain.DumpKeyword;
import com.nokri.taxonomy.domain.MappingResult;
import com.nokri.taxonomy.excel.ExcelIO;
import com.nokri.taxonomy.excel.SampleDataWriter;
import com.nokri.taxonomy.llm.CompanyJudge;
import com.nokri.taxonomy.llm.LocalMlxLauncher;
import com.nokri.taxonomy.llm.OpenAiCompatibleCompanyJudge;
import com.nokri.taxonomy.matching.CanonicalIndex;
import com.nokri.taxonomy.search.CompanySearchClient;
import com.nokri.taxonomy.search.GoogleCompanySearchClient;
import com.nokri.taxonomy.search.OfflineCompanySearchClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class CompanyMappingApplication {
    private static final Logger log = LoggerFactory.getLogger(CompanyMappingApplication.class);

    public static void main(String[] args) throws Exception {
        AppConfig config = AppConfig.fromEnv();
        SampleDataWriter samples = new SampleDataWriter();
        if (!Files.exists(config.canonicalFile())) {
            samples.writeCanonical(config.canonicalFile());
            log.info("Wrote sample canonical sheet to {}", config.canonicalFile());
        }
        if (!Files.exists(config.dumpFile())) {
            samples.writeDump(config.dumpFile());
            log.info("Wrote sample dump sheet to {}", config.dumpFile());
        }

        ExcelIO excel = new ExcelIO();
        CanonicalIndex index = new CanonicalIndex(excel.readCanonical(config.canonicalFile()));
        CompanySearchClient searchClient = config.googleEnabled()
                ? new GoogleCompanySearchClient(config.googleApiKey(), config.googleCseId())
                : new OfflineCompanySearchClient();
        if (!config.googleEnabled()) {
            log.warn("GOOGLE_API_KEY / GOOGLE_CSE_ID not set; using offline search fallback");
        }
        CompanyJudge judge = null;
        if (config.llmEnabled()) {
            if (LocalMlxLauncher.ensureRunning(config)) {
                judge = new OpenAiCompatibleCompanyJudge(config);
            } else {
                log.warn("Local LLM did not start; uncertain rows will stay UNMATCHED");
            }
        } else {
            log.warn("LOCAL_LLM_ENABLED=false; uncertain rows will not use the LLM judge");
        }

        CompanyMappingGraph graph = new CompanyMappingGraph(index, searchClient, judge, config);
        List<DumpKeyword> dump = excel.readDump(config.dumpFile());
        List<MappingResult> results = new ArrayList<>();
        for (DumpKeyword keyword : dump) {
            MappingResult result = graph.map(keyword);
            log.info("{} -> {} ({})", keyword.companyName(), result.matchStatus(), result.canonicalName());
            results.add(result);
        }
        excel.writeMapping(config.mappingFile(), results);
        log.info("Wrote mapping sheet to {}", config.mappingFile());
    }
}
