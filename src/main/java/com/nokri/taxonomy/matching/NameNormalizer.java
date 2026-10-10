package com.nokri.taxonomy.matching;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class NameNormalizer {
    private static final Set<String> STOP_WORDS = Set.of(
            "ltd", "limited", "pvt", "private", "inc", "incorporated", "llc",
            "corp", "corporation", "co", "company", "the", "and", "of", "india",
            "jobs", "job", "careers"
    );
    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");

    private NameNormalizer() {
    }

    public static String normalize(String raw) {
        if (raw == null) {
            return "";
        }
        return NON_ALNUM.matcher(raw.toLowerCase(Locale.ROOT).trim()).replaceAll(" ").trim();
    }

    public static String compact(String raw) {
        return normalize(raw).replace(" ", "");
    }

    public static List<String> significantTokens(String raw) {
        return Arrays.stream(normalize(raw).split(" "))
                .filter(token -> !token.isBlank())
                .filter(token -> !STOP_WORDS.contains(token))
                .collect(Collectors.toList());
    }

    public static String acronym(String raw) {
        List<String> tokens = significantTokens(raw);
        if (tokens.size() < 2) {
            return "";
        }
        return tokens.stream()
                .map(token -> token.substring(0, 1))
                .collect(Collectors.joining());
    }
}
