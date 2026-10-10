package com.nokri.taxonomy.log;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Human-readable per-row trace. File is wiped at each agent startup.
 * Default path: {@code logs/agent-run.log}
 */
public final class AgentRunLog {
    public static final Path DEFAULT_FILE = Path.of("logs/agent-run.log");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private static final Logger console = LoggerFactory.getLogger("com.nokri.taxonomy.run");

    private static PrintWriter writer;
    private static Path file = DEFAULT_FILE;

    private AgentRunLog() {
    }

    public static synchronized void startNewRun() {
        startNewRun(logPathFromEnv());
    }

    public static synchronized void startNewRun(Path path) {
        closeQuietly();
        file = path;
        try {
            Files.createDirectories(path.getParent());
            Files.deleteIfExists(path);
            writer = new PrintWriter(Files.newBufferedWriter(path, StandardCharsets.UTF_8), true);
            line("Company mapping agent run log");
            line("Started: " + LocalDateTime.now().format(TIME));
            line("File:    " + path.toAbsolutePath());
            line("");
        } catch (IOException e) {
            console.warn("Could not open run log {}: {}", path, e.getMessage());
            writer = null;
        }
    }

    public static synchronized void close() {
        closeQuietly();
    }

    public static Path file() {
        return file;
    }

    public static void rowStart(int index, int total, String id, String name, String email) {
        line("================================================================");
        line("ROW " + index + " / " + total);
        line("naukriCompanyId : " + empty(id));
        line("companyName     : " + empty(name));
        line("emailWebsite    : " + empty(email));
        line("----------------------------------------------------------------");
    }

    public static void notACompany(String why) {
        step("Dump companyName looks like email, phone, or non-company text — skip Google and LLM.");
        step("Reason: " + why);
    }

    public static void exactCanonical(String dumpName, String canonicalId, String canonicalName) {
        step("Canonical sheet already has exactly this name: \"" + dumpName + "\".");
        step("No Google needed. Mapping to " + canonicalName + " (" + canonicalId + ").");
    }

    public static void willGoogle(String dumpName) {
        step("Dump name \"" + dumpName + "\" is not written exactly as any canonical name.");
        step("I will Google to find the official company name.");
    }

    public static void googling(String query, String companyDomain) {
        if (companyDomain == null || companyDomain.isBlank()) {
            step("Dump email looks personal (or empty) — ignoring domain.");
            step("Googling for: " + query);
        } else {
            step("Googling for this company and confirming domain " + companyDomain + ".");
            step("Googling for: " + query);
        }
    }

    public static void googleFound(String officialName, String sourceUrl, boolean domainConfirmed) {
        if (officialName == null || officialName.isBlank()) {
            step("Google did not return a usable official company name.");
            return;
        }
        step("Official name from Google: \"" + officialName + "\".");
        if (sourceUrl != null && !sourceUrl.isBlank()) {
            step("Google source: " + sourceUrl);
        }
        if (domainConfirmed) {
            step("Dump company domain appeared on that Google result.");
        }
        step("Now searching the canonical sheet for \"" + officialName + "\".");
    }

    public static void canonicalMatched(String officialName, String canonicalId, String canonicalName, String method) {
        step("Canonical sheet matched \"" + officialName + "\" → " + canonicalName + " (" + canonicalId + ")"
                + (method == null || method.isBlank() ? "." : " via " + method + "."));
    }

    public static void canonicalMiss(String officialName) {
        String label = officialName == null || officialName.isBlank() ? "(empty official name)" : "\"" + officialName + "\"";
        step("Canonical sheet has no confident match for " + label + ".");
    }

    public static void askingLlm(String why) {
        step("Asking the local LLM. " + why);
    }

    public static void llmResult(String status, String canonicalId, String canonicalName, String reason) {
        step("LLM said: " + status
                + (canonicalName == null || canonicalName.isBlank() ? "" : " → " + canonicalName + " (" + canonicalId + ")")
                + (reason == null || reason.isBlank() ? "." : " (" + reason + ")."));
    }

    public static void rowEnd(String status, String canonicalId, String canonicalName) {
        String mapped = canonicalName == null || canonicalName.isBlank()
                ? ""
                : " → " + canonicalName + (canonicalId == null || canonicalId.isBlank() ? "" : " (" + canonicalId + ")");
        line("RESULT: " + empty(status) + mapped);
        line("================================================================");
        line("");
    }

    public static void info(String message) {
        line(message);
    }

    private static void step(String message) {
        line("• " + message);
    }

    private static synchronized void line(String message) {
        console.info(message);
        if (writer != null) {
            writer.println(message);
            writer.flush();
        }
    }

    private static void closeQuietly() {
        if (writer != null) {
            writer.flush();
            writer.close();
            writer = null;
        }
    }

    private static String empty(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }

    private static Path logPathFromEnv() {
        String override = System.getenv("AGENT_RUN_LOG");
        if (override != null && !override.isBlank()) {
            return Path.of(override);
        }
        return DEFAULT_FILE;
    }
}
