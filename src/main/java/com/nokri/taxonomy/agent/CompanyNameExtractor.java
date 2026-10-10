package com.nokri.taxonomy.agent;

import com.nokri.taxonomy.search.SearchHit;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CompanyNameExtractor {
    private static final Pattern TITLE_PREFIX = Pattern.compile("^(.*?)(\\s[-–|:(\\[].*)?$");

    private CompanyNameExtractor() {
    }

    public static String fromHits(List<SearchHit> hits, String companyDomain) {
        SearchHit chosen = choose(hits, companyDomain);
        if (chosen == null) {
            return "";
        }
        String title = chosen.title();
        if (title == null || title.isBlank()) {
            return "";
        }
        Matcher matcher = TITLE_PREFIX.matcher(title.trim());
        if (matcher.matches() && matcher.group(1) != null && !matcher.group(1).isBlank()) {
            return matcher.group(1).trim();
        }
        return title.trim();
    }

    static SearchHit choose(List<SearchHit> hits, String companyDomain) {
        if (hits == null || hits.isEmpty()) {
            return null;
        }
        if (companyDomain != null && !companyDomain.isBlank()) {
            String domain = companyDomain.toLowerCase(Locale.ROOT);
            for (SearchHit hit : hits) {
                String blob = (nullToEmpty(hit.url()) + " " + nullToEmpty(hit.snippet()) + " " + nullToEmpty(hit.title()))
                        .toLowerCase(Locale.ROOT);
                if (blob.contains(domain)) {
                    return hit;
                }
            }
        }
        return hits.getFirst();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
