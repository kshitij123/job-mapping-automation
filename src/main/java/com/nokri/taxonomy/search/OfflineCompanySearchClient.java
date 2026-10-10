package com.nokri.taxonomy.search;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Local stand-in used when Google Custom Search keys are not configured.
 */
public class OfflineCompanySearchClient implements CompanySearchClient {
    private static final Map<String, List<SearchHit>> KNOWN = Map.ofEntries(
            entry("tcs", "Tata Consultancy Services",
                    "Official site of Tata Consultancy Services (TCS).", "https://www.tcs-india.com"),
            entry("tata consultancy services", "Tata Consultancy Services",
                    "Tata Consultancy Services official website.", "https://www.tcs.com"),
            entry("tcs jobs", "Tata Consultancy Services",
                    "Careers at Tata Consultancy Services.", "https://www.tcs.com/careers"),
            entry("infosys", "Infosys Limited",
                    "Infosys Limited official website.", "https://www.infosys.com"),
            entry("infosys ltd", "Infosys Limited",
                    "Infosys Ltd official website.", "https://www.infosys.com"),
            entry("google ireland limited", "Google",
                    "Google Ireland Limited, a Google subsidiary.", "https://about.google"),
            entry("google llc", "Google",
                    "Google LLC official.", "https://about.google"),
            entry("wipro", "Wipro Limited",
                    "Wipro Limited official website.", "https://www.wipro.com"),
            entry("accenture india", "Accenture",
                    "Accenture in India.", "https://www.accenture.com"),
            entry("amazon", "Amazon",
                    "Amazon.com official.", "https://www.amazon.com"),
            entry("microsoft", "Microsoft",
                    "Microsoft Corporation official.", "https://www.microsoft.com"),
            entry("ibm india", "IBM",
                    "IBM India, International Business Machines.", "https://www.ibm.com"),
            entry("reliance", "Reliance Industries",
                    "Reliance Industries Limited official.", "https://www.ril.com"),
            entry("hdfc", "HDFC Bank",
                    "HDFC Bank Limited official.", "https://www.hdfcbank.com"),
            entry("deloitte", "Deloitte",
                    "Deloitte official.", "https://www.deloitte.com"),
            entry("cognizant technology solutions", "Cognizant",
                    "Cognizant Technology Solutions official.", "https://www.cognizant.com"),
            entry("hcl", "HCL Technologies",
                    "HCLTech official.", "https://www.hcltech.com"),
            entry("capgemini", "Capgemini",
                    "Capgemini official.", "https://www.capgemini.com"),
            entry("oracle india", "Oracle",
                    "Oracle India official.", "https://www.oracle.com")
    );

    @Override
    public List<SearchHit> search(String keyword, String companyDomain) {
        String key = keyword == null ? "" : keyword.toLowerCase(Locale.ROOT).trim();
        List<SearchHit> hits = KNOWN.getOrDefault(key, List.of());
        if (companyDomain == null || companyDomain.isBlank() || hits.isEmpty()) {
            return hits;
        }
        String domain = companyDomain.toLowerCase(Locale.ROOT);
        List<SearchHit> confirmed = hits.stream()
                .filter(hit -> containsDomain(hit, domain))
                .toList();
        return confirmed.isEmpty() ? hits : confirmed;
    }

    private static boolean containsDomain(SearchHit hit, String domain) {
        String blob = (hit.url() + " " + hit.snippet() + " " + hit.title()).toLowerCase(Locale.ROOT);
        return blob.contains(domain.toLowerCase(Locale.ROOT));
    }

    private static Map.Entry<String, List<SearchHit>> entry(String key, String title, String snippet, String url) {
        return Map.entry(key, List.of(new SearchHit(title, snippet, url)));
    }
}
