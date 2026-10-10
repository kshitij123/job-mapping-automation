package com.nokri.taxonomy.agent;

import com.nokri.taxonomy.search.SearchHit;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CompanyNameExtractor {
    private static final Pattern TITLE_PREFIX = Pattern.compile("^(.*?)(\\s[-–|:].*)?$");

    private CompanyNameExtractor() {
    }

    public static String fromHits(List<SearchHit> hits) {
        if (hits == null || hits.isEmpty()) {
            return "";
        }
        String title = hits.getFirst().title();
        if (title == null || title.isBlank()) {
            return "";
        }
        Matcher matcher = TITLE_PREFIX.matcher(title.trim());
        if (matcher.matches()) {
            return matcher.group(1).trim();
        }
        return title.trim();
    }
}
