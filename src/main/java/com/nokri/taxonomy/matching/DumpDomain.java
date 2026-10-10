package com.nokri.taxonomy.matching;

import java.util.Locale;
import java.util.Set;

/**
 * Reads a company website/email from the dump and decides whether it is a
 * real company domain or a personal mailbox (gmail, yahoo, …).
 */
public final class DumpDomain {
    private static final Set<String> PERSONAL = Set.of(
            "gmail.com", "googlemail.com", "yahoo.com", "yahoo.co.in", "ymail.com",
            "hotmail.com", "outlook.com", "live.com", "msn.com", "icloud.com",
            "aol.com", "proton.me", "protonmail.com", "rediffmail.com", "rediff.com",
            "yopmail.com", "pookmail.com", "mail.com", "example.com", "example.org",
            "test", "demo.com"
    );

    private DumpDomain() {
    }

    public static String fromEmailOrWebsite(String emailWebsite) {
        if (emailWebsite == null || emailWebsite.isBlank()) {
            return "";
        }
        String value = emailWebsite.trim().toLowerCase(Locale.ROOT);
        int at = value.lastIndexOf('@');
        String host = at >= 0 ? value.substring(at + 1) : stripUrl(value);
        host = host.replaceFirst("^www\\.", "");
        int slash = host.indexOf('/');
        if (slash >= 0) {
            host = host.substring(0, slash);
        }
        return host.replaceAll("[^a-z0-9.-]", "");
    }

    /**
     * Company domain to confirm on Google, or empty when the dump email is personal/junk.
     */
    public static String companyDomainOrEmpty(String emailWebsite) {
        String host = fromEmailOrWebsite(emailWebsite);
        if (host.isBlank() || isPersonal(host)) {
            return "";
        }
        return host;
    }

    public static boolean isPersonal(String host) {
        if (host == null || host.isBlank()) {
            return true;
        }
        String h = host.toLowerCase(Locale.ROOT);
        return PERSONAL.contains(h);
    }

    private static String stripUrl(String value) {
        return value.replaceFirst("^https?://", "");
    }
}
