package com.nokri.taxonomy.llm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nokri.taxonomy.config.AppConfig;
import com.nokri.taxonomy.matching.MatchCandidate;
import com.nokri.taxonomy.search.SearchHit;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.http.client.jdk.JdkHttpClient;
import dev.langchain4j.http.client.jdk.JdkHttpClientBuilder;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.response.ChatResponse;
import dev.langchain4j.model.openai.OpenAiChatModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Talks to any OpenAI-compatible chat API — default is local {@code mlx_lm.server} on the Mac.
 */
public class OpenAiCompatibleCompanyJudge implements CompanyJudge {
    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleCompanyJudge.class);
    private final ChatModel model;
    private final ObjectMapper mapper = new ObjectMapper();
    private final String promptTemplate;

    public OpenAiCompatibleCompanyJudge(AppConfig config) {
        HttpClient.Builder httpClientBuilder = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(Duration.ofSeconds(10));
        JdkHttpClientBuilder jdkHttpClientBuilder = JdkHttpClient.builder()
                .httpClientBuilder(httpClientBuilder);

        this.model = OpenAiChatModel.builder()
                .baseUrl(config.localLlmBaseUrl())
                .apiKey(config.localLlmApiKey())
                .modelName(config.localLlmModel())
                .temperature(0.0)
                .maxTokens(256)
                .timeout(Duration.ofSeconds(120))
                .httpClientBuilder(jdkHttpClientBuilder)
                .logRequests(false)
                .logResponses(false)
                .build();
        this.promptTemplate = loadPrompt();
        log.info("LLM judge -> {} model {}", config.localLlmBaseUrl(), config.localLlmModel());
    }

    @Override
    public LlmJudgement judge(String keyword, List<SearchHit> evidence, List<MatchCandidate> candidates) {
        String prompt = promptTemplate
                .replace("{{keyword}}", keyword)
                .replace("{{evidence}}", formatEvidence(evidence))
                .replace("{{candidates}}", formatCandidates(candidates));
        ChatResponse response = model.chat(UserMessage.from(prompt));
        String text = response.aiMessage() == null ? "" : response.aiMessage().text();
        log.info("LLM judgement raw: {}", text);
        return parse(text);
    }

    private LlmJudgement parse(String text) {
        String json = extractJson(text);
        try {
            JsonNode node = mapper.readTree(json);
            boolean isCompany = node.path("isCompany").asBoolean(false);
            String canonicalId = node.path("canonicalId").asText("");
            String reason = node.path("reason").asText("llm");
            if (canonicalId.isBlank()) {
                canonicalId = null;
            }
            return new LlmJudgement(isCompany, canonicalId, reason);
        } catch (Exception e) {
            return new LlmJudgement(false, null, "unparseable llm response");
        }
    }

    private static String extractJson(String text) {
        if (text == null) {
            return "{}";
        }
        String cleaned = text.replaceAll("(?s)<think>.*?</think>", "").trim();
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        start = text.indexOf('{');
        end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return "{}";
    }

    private static String formatEvidence(List<SearchHit> evidence) {
        if (evidence == null || evidence.isEmpty()) {
            return "(none)";
        }
        return evidence.stream()
                .limit(5)
                .map(hit -> "- " + hit.title() + ": " + hit.snippet())
                .collect(Collectors.joining("\n"));
    }

    private static String formatCandidates(List<MatchCandidate> candidates) {
        if (candidates == null || candidates.isEmpty()) {
            return "(none)";
        }
        return candidates.stream()
                .map(c -> c.company().canonicalId() + " | " + c.company().canonicalName() + " | score=" + c.score())
                .collect(Collectors.joining("\n"));
    }

    private static String loadPrompt() {
        try (InputStream in = OpenAiCompatibleCompanyJudge.class.getResourceAsStream("/prompts/company-judge.txt")) {
            if (in == null) {
                throw new IllegalStateException("Missing prompt resource /prompts/company-judge.txt");
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Could not load LLM prompt", e);
        }
    }
}
