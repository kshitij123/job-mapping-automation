package com.nokri.taxonomy.search;

import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Local stand-in used when Google Custom Search keys are not configured.
 * Replace behavior by setting GOOGLE_API_KEY and GOOGLE_CSE_ID.
 */
public class OfflineCompanySearchClient implements CompanySearchClient {
    private static final Map<String, List<SearchHit>> KNOWN = Map.ofEntries(
            entry("tata consultancy services", "Tata Consultancy Services", "Indian IT services company, also known as TCS."),
            entry("tcs", "Tata Consultancy Services (TCS)", "TCS is an Indian multinational IT services and consulting company."),
            entry("tcs jobs", "TCS careers", "Job openings at Tata Consultancy Services."),
            entry("infosys", "Infosys Limited", "Infosys is an Indian information technology company."),
            entry("infosys ltd", "Infosys Ltd", "Official site of Infosys Limited."),
            entry("wipro", "Wipro Limited", "Wipro is an Indian multinational corporation."),
            entry("accenture india", "Accenture", "Accenture plc professional services company."),
            entry("amazon", "Amazon", "Amazon.com, Inc. e-commerce and cloud company."),
            entry("google llc", "Google LLC", "Google LLC is a subsidiary of Alphabet Inc."),
            entry("microsoft", "Microsoft", "Microsoft Corporation software and cloud company."),
            entry("ibm india", "IBM", "International Business Machines Corporation."),
            entry("reliance", "Reliance Industries", "Reliance Industries Limited Indian conglomerate."),
            entry("hdfc", "HDFC Bank", "HDFC Bank Limited Indian banking company."),
            entry("deloitte", "Deloitte", "Deloitte professional services network."),
            entry("cognizant technology solutions", "Cognizant", "Cognizant Technology Solutions IT services company."),
            entry("hcl", "HCL Technologies", "HCLTech Indian information technology company.")
    );

    @Override
    public List<SearchHit> search(String keyword) {
        String key = keyword == null ? "" : keyword.toLowerCase(Locale.ROOT).trim();
        List<SearchHit> hits = KNOWN.get(key);
        return hits == null ? List.of() : hits;
    }

    private static Map.Entry<String, List<SearchHit>> entry(String key, String title, String snippet) {
        return Map.entry(key, List.of(new SearchHit(title, snippet, "https://example.invalid/" + key.replace(' ', '-'))));
    }
}
