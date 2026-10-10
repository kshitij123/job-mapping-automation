package com.nokri.taxonomy.config;

import java.nio.file.Path;

public record AppConfig(
        Path canonicalFile,
        Path dumpFile,
        Path mappingFile,
        double acceptScore,
        double llmScore,
        boolean localLlmEnabled,
        String localLlmBaseUrl,
        String localLlmModel,
        String localLlmApiKey,
        String googleApiKey,
        String googleCseId
) {
    public static final String DEFAULT_MLX_MODEL = "mlx-community/Qwen2.5-3B-Instruct-4bit";
    public static final String DEFAULT_LOCAL_LLM_BASE_URL = "http://127.0.0.1:8080/v1";

    public static AppConfig fromEnv() {
        return new AppConfig(
                Path.of(envOr("CANONICAL_FILE", "data/canonical.xlsx")),
                Path.of(envOr("DUMP_FILE", "data/dump.xlsx")),
                Path.of(envOr("MAPPING_FILE", "data/mapping.xlsx")),
                Double.parseDouble(envOr("MATCH_ACCEPT_SCORE", "0.82")),
                Double.parseDouble(envOr("MATCH_LLM_SCORE", "0.50")),
                Boolean.parseBoolean(envOr("LOCAL_LLM_ENABLED", "true")),
                trimTrailingSlash(envOr("LOCAL_LLM_BASE_URL", DEFAULT_LOCAL_LLM_BASE_URL)),
                localLlmModelFromEnv(),
                envOr("LOCAL_LLM_API_KEY", "mlx-local"),
                firstEnv("GOOGLE_API_KEY", "GOOGLE_CSE_API_KEY"),
                firstEnv("GOOGLE_CSE_ID", "GOOGLE_SEARCH_ENGINE_ID")
        );
    }

    public boolean llmEnabled() {
        return localLlmEnabled;
    }

    public boolean googleEnabled() {
        return googleApiKey != null && !googleApiKey.isBlank()
                && googleCseId != null && !googleCseId.isBlank();
    }

    private static String envOr(String key, String fallback) {
        String value = System.getenv(key);
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String firstEnv(String... keys) {
        for (String key : keys) {
            String value = System.getenv(key);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return null;
    }

    private static String localLlmModelFromEnv() {
        String model = firstEnv("LOCAL_LLM_MODEL", "MLX_MODEL");
        return model == null ? DEFAULT_MLX_MODEL : model;
    }

    private static String trimTrailingSlash(String url) {
        if (url == null || url.isBlank()) {
            return DEFAULT_LOCAL_LLM_BASE_URL;
        }
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
