package com.nokri.taxonomy.search;

public record SearchHit(String title, String snippet, String url) {
    public String combinedText() {
        return String.join(" ", nullToEmpty(title), nullToEmpty(snippet));
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
