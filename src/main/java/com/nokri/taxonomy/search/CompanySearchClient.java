package com.nokri.taxonomy.search;

import java.util.List;

public interface CompanySearchClient {
    /**
     * @param keyword dump company name
     * @param companyDomain empty when the dump email is personal; otherwise host to confirm
     */
    List<SearchHit> search(String keyword, String companyDomain);
}
