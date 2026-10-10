package com.nokri.taxonomy.agent;

import java.util.regex.Pattern;

public final class KeywordClassifier {
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w.-]+\\.[A-Za-z]{2,}$");
    private static final Pattern PHONE = Pattern.compile("^[+\\d][\\d\\s().-]{7,}$");

    private KeywordClassifier() {
    }

    public static boolean looksLikeCompanyKeyword(String keyword) {
        if (keyword == null) {
            return false;
        }
        String trimmed = keyword.trim();
        if (trimmed.length() < 2) {
            return false;
        }
        if (EMAIL.matcher(trimmed).matches()) {
            return false;
        }
        String digits = trimmed.replaceAll("\\D", "");
        if (PHONE.matcher(trimmed).matches() && digits.length() >= 8) {
            return false;
        }
        return trimmed.chars().anyMatch(Character::isLetter);
    }
}
