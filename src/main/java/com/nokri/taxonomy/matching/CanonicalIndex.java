package com.nokri.taxonomy.matching;

import com.nokri.taxonomy.domain.CanonicalCompany;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class CanonicalIndex {
    private final List<CanonicalCompany> companies;
    private final Map<String, CanonicalCompany> byNormalizedName = new HashMap<>();
    private final Map<String, CanonicalCompany> byCompactName = new HashMap<>();
    private final Map<String, CanonicalCompany> byAcronym = new HashMap<>();

    public CanonicalIndex(List<CanonicalCompany> companies) {
        this.companies = List.copyOf(companies);
        for (CanonicalCompany company : this.companies) {
            byNormalizedName.put(NameNormalizer.normalize(company.canonicalName()), company);
            byCompactName.put(NameNormalizer.compact(company.canonicalName()), company);
            String acronym = NameNormalizer.acronym(company.canonicalName());
            if (acronym.length() >= 2) {
                byAcronym.putIfAbsent(acronym, company);
            }
            String compact = NameNormalizer.compact(company.canonicalName());
            if (compact.length() >= 2 && compact.length() <= 5) {
                byAcronym.putIfAbsent(compact, company);
            }
            List<String> tokens = NameNormalizer.significantTokens(company.canonicalName());
            if (!tokens.isEmpty() && tokens.getFirst().length() >= 2 && tokens.getFirst().length() <= 5) {
                byAcronym.putIfAbsent(tokens.getFirst(), company);
            }
        }
    }

    public List<CanonicalCompany> companies() {
        return companies;
    }

    /**
     * Direct map is allowed only when the dump name is written exactly like a canonical name
     * (trim only; case and spelling must match).
     */
    public Optional<CanonicalCompany> exactWritten(String name) {
        if (name == null) {
            return Optional.empty();
        }
        String trimmed = name.trim();
        if (trimmed.isEmpty()) {
            return Optional.empty();
        }
        return companies.stream()
                .filter(company -> company.canonicalName().equals(trimmed))
                .findFirst();
    }

    public Optional<MatchCandidate> bestMatch(String... names) {
        MatchCandidate best = null;
        for (String name : names) {
            if (name == null || name.isBlank()) {
                continue;
            }
            Optional<MatchCandidate> candidate = score(name);
            if (candidate.isPresent() && (best == null || candidate.get().score() > best.score())) {
                best = candidate.get();
            }
        }
        return Optional.ofNullable(best);
    }

    public List<MatchCandidate> topMatches(String name, int limit) {
        return companies.stream()
                .map(company -> new MatchCandidate(company, similarity(name, company), "fuzzy"))
                .sorted(Comparator.comparingDouble(MatchCandidate::score).reversed())
                .limit(limit)
                .toList();
    }

    private Optional<MatchCandidate> score(String name) {
        String normalized = NameNormalizer.normalize(name);
        String compact = NameNormalizer.compact(name);
        if (normalized.isBlank()) {
            return Optional.empty();
        }

        CanonicalCompany exact = byNormalizedName.get(normalized);
        if (exact != null) {
            return Optional.of(new MatchCandidate(exact, 1.0, "exact"));
        }
        CanonicalCompany compactHit = byCompactName.get(compact);
        if (compactHit != null) {
            return Optional.of(new MatchCandidate(compactHit, 0.97, "compact"));
        }

        CanonicalCompany acronymHit = byAcronym.get(compact);
        if (acronymHit != null) {
            return Optional.of(new MatchCandidate(acronymHit, 0.93, "acronym"));
        }
        for (String token : NameNormalizer.significantTokens(name)) {
            CanonicalCompany tokenHit = byAcronym.get(token);
            if (tokenHit != null) {
                return Optional.of(new MatchCandidate(tokenHit, 0.91, "token-acronym"));
            }
        }
        String generatedAcronym = NameNormalizer.acronym(name);
        if (generatedAcronym.length() >= 2) {
            CanonicalCompany reverse = byCompactName.get(generatedAcronym);
            if (reverse == null) {
                reverse = byNormalizedName.get(generatedAcronym);
            }
            if (reverse == null) {
                reverse = byAcronym.get(generatedAcronym);
            }
            if (reverse != null) {
                return Optional.of(new MatchCandidate(reverse, 0.93, "acronym"));
            }
        }

        return companies.stream()
                .map(company -> new MatchCandidate(company, similarity(name, company), "fuzzy"))
                .max(Comparator.comparingDouble(MatchCandidate::score));
    }

    private double similarity(String name, CanonicalCompany company) {
        String left = NameNormalizer.normalize(name);
        String right = NameNormalizer.normalize(company.canonicalName());
        if (left.equals(right)) {
            return 1.0;
        }

        double token = jaccard(NameNormalizer.significantTokens(name), NameNormalizer.significantTokens(company.canonicalName()));
        double compact = levenshteinRatio(NameNormalizer.compact(name), NameNormalizer.compact(company.canonicalName()));
        double acronymBoost = 0;
        String leftAcronym = NameNormalizer.acronym(name);
        String rightAcronym = NameNormalizer.acronym(company.canonicalName());
        String leftCompact = NameNormalizer.compact(name);
        String rightCompact = NameNormalizer.compact(company.canonicalName());
        if (!leftAcronym.isBlank() && (leftAcronym.equals(rightCompact) || leftAcronym.equals(rightAcronym))) {
            acronymBoost = 0.25;
        }
        if (!rightAcronym.isBlank() && rightAcronym.equals(leftCompact)) {
            acronymBoost = 0.25;
        }
        return Math.min(1.0, Math.max(token, compact) + acronymBoost);
    }

    private static double jaccard(List<String> left, List<String> right) {
        if (left.isEmpty() || right.isEmpty()) {
            return 0;
        }
        List<String> intersection = new ArrayList<>(left);
        intersection.retainAll(right);
        List<String> union = new ArrayList<>(left);
        for (String token : right) {
            if (!union.contains(token)) {
                union.add(token);
            }
        }
        return intersection.size() / (double) union.size();
    }

    private static double levenshteinRatio(String left, String right) {
        if (left.isEmpty() || right.isEmpty()) {
            return 0;
        }
        int distance = levenshtein(left, right);
        return 1.0 - (distance / (double) Math.max(left.length(), right.length()));
    }

    private static int levenshtein(String left, String right) {
        int[] previous = new int[right.length() + 1];
        int[] current = new int[right.length() + 1];
        for (int j = 0; j <= right.length(); j++) {
            previous[j] = j;
        }
        for (int i = 1; i <= left.length(); i++) {
            current[0] = i;
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                current[j] = Math.min(Math.min(current[j - 1] + 1, previous[j] + 1), previous[j - 1] + cost);
            }
            int[] swap = previous;
            previous = current;
            current = swap;
        }
        return previous[right.length()];
    }

    public Optional<CanonicalCompany> byId(String canonicalId) {
        if (canonicalId == null) {
            return Optional.empty();
        }
        return companies.stream()
                .filter(company -> company.canonicalId().equalsIgnoreCase(canonicalId.trim()))
                .findFirst();
    }

    @Override
    public String toString() {
        return "CanonicalIndex{size=" + companies.size() + ", locale=" + Locale.ROOT + "}";
    }
}
