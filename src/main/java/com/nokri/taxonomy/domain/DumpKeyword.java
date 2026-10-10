package com.nokri.taxonomy.domain;

public record DumpKeyword(String companyId, String companyName, String emailWebsite) {
    public DumpKeyword(String companyId, String companyName) {
        this(companyId, companyName, "");
    }
}
