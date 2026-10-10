package com.nokri.taxonomy.search;

import java.util.List;

public interface CompanySearchClient {
    List<SearchHit> search(String keyword);
}
