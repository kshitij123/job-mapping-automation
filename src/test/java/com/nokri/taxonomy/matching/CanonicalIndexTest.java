package com.nokri.taxonomy.matching;

import com.nokri.taxonomy.domain.CanonicalCompany;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanonicalIndexTest {
    private final CanonicalIndex index = new CanonicalIndex(List.of(
            new CanonicalCompany("C1001", "TCS"),
            new CanonicalCompany("C1002", "Infosys Limited"),
            new CanonicalCompany("C1014", "HCL Technologies")
    ));

    @Test
    void mapsFullNameToCanonicalAcronym() {
        MatchCandidate hit = index.bestMatch("Tata Consultancy Services").orElseThrow();
        assertEquals("C1001", hit.company().canonicalId());
        assertTrue(hit.score() >= 0.9);
    }

    @Test
    void mapsShortNameToLongCanonical() {
        MatchCandidate hit = index.bestMatch("Infosys").orElseThrow();
        assertEquals("C1002", hit.company().canonicalId());
        assertTrue(hit.score() >= 0.8);
    }

    @Test
    void mapsAcronymDumpToCanonicalLongName() {
        MatchCandidate hit = index.bestMatch("HCL").orElseThrow();
        assertEquals("C1014", hit.company().canonicalId());
    }
}
