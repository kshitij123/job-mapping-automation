package com.nokri.taxonomy.agent;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class KeywordClassifierTest {
    @Test
    void rejectsContactJunk() {
        assertFalse(KeywordClassifier.looksLikeCompanyKeyword("john.doe@gmail.com"));
        assertFalse(KeywordClassifier.looksLikeCompanyKeyword("9876543210"));
        assertFalse(KeywordClassifier.looksLikeCompanyKeyword("+91 9988776655"));
    }

    @Test
    void acceptsCompanyLikeText() {
        assertTrue(KeywordClassifier.looksLikeCompanyKeyword("TCS"));
        assertTrue(KeywordClassifier.looksLikeCompanyKeyword("Tata Consultancy Services"));
    }
}
