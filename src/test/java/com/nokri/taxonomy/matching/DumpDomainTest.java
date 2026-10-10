package com.nokri.taxonomy.matching;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DumpDomainTest {
    @Test
    void companyEmailKeepsHost() {
        assertEquals("tcs-india.com", DumpDomain.companyDomainOrEmpty("hr@tcs-india.com"));
    }

    @Test
    void gmailIsIgnored() {
        assertEquals("", DumpDomain.companyDomainOrEmpty("campus@gmail.com"));
        assertTrue(DumpDomain.isPersonal("gmail.com"));
    }

    @Test
    void yahooCoInIsIgnored() {
        assertEquals("", DumpDomain.companyDomainOrEmpty("hareshshelar@yahoo.co.in"));
    }
}
