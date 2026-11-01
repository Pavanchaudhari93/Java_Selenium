package com.framework.reporting.comparison;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Reads every {@code *-result.json} file in an Allure results directory.
 *
 * <p>When TestNG retries a test, Allure writes one result file per attempt with the same
 * {@code historyId}. Only the latest attempt (highest {@code stop}) is kept, which matches
 * what the Allure report itself shows.
 */
public class AllureParser {

    /** Attachment name written by {@code com.framework.listeners.ConsoleLogAttachmentListener}. */
    public static final String CONSOLE_LOG_ATTACHMENT = "Console log";

    private static final Logger log = LogManager.getLogger(AllureParser.class);
    private static final String RESULT_FILE_SUFFIX = "-result.json";
    private static final long MAX_CONSOLE_LOG_BYTES = 512 * 1024;

    private final ObjectMapper mapper;

    public AllureParser() {
        this(new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false));
    }

    public AllureParser(ObjectMapper mapper) {
        this.mapper = Objects.requireNonNull(mapper, "mapper");
    }

    /**
     * @return results keyed by {@link TestResult#comparisonKey()}, in a stable order
     * @throws AllureComparisonException if the directory does not exist or cannot be listed
     */
    public Map<String, TestResult> parse(Path resultsDir) {
        if (resultsDir == null || !Files.isDirectory(resultsDir)) {
            throw new AllureComparisonException("Allure results directory not found: " + resultsDir);
        }

        List<Path> files;
        try (Stream<Path> stream = Files.list(resultsDir)) {
            files = stream
                    .filter(Files::isRegularFile)
                    .filter(p -> p.getFileName().toString().endsWith(RESULT_FILE_SUFFIX))
                    .sorted()
                    .toList();
        } catch (IOException e) {
            throw new AllureComparisonException("Failed to list Allure results in " + resultsDir, e);
        }

        if (files.isEmpty()) {
            log.warn("No *{} files found in {}", RESULT_FILE_SUFFIX, resultsDir);
            return Collections.emptyMap();
        }

        List<TestResult> parsed = files.stream()
                .map(this::readResult)
                .flatMap(Optional::stream)
                .toList();

        Map<String, TestResult> latestByKey = parsed.stream()
                .collect(Collectors.toMap(
                        TestResult::comparisonKey,
                        Function.identity(),
                        AllureParser::latestAttempt,
                        LinkedHashMap::new));

        log.info("Parsed {} result files from {} -> {} unique tests ({} retry attempts collapsed, {} files skipped)",
                files.size(), resultsDir, latestByKey.size(),
                parsed.size() - latestByKey.size(), files.size() - parsed.size());
        return Collections.unmodifiableMap(latestByKey);
    }

    private Optional<TestResult> readResult(Path file) {
        try {
            TestResult result = mapper.readValue(file.toFile(), TestResult.class);
            String key = result.comparisonKey();
            if (key == null || key.isBlank()) {
                log.warn("Skipping {}: no historyId or fullName", file.getFileName());
                return Optional.empty();
            }
            return Optional.of(result.isFailure() ? result.withConsoleLog(readConsoleLog(file.getParent(), result)) : result);
        } catch (IOException e) {
            log.warn("Skipping unreadable Allure result {}: {}", file.getFileName(), e.getMessage());
            return Optional.empty();
        }
    }

    // Older runs (before the listener existed) have no attachment; that is not an error.
    private String readConsoleLog(Path resultsDir, TestResult result) {
        Optional<TestResult.Attachment> attachment = result.attachments().stream()
                .filter(a -> CONSOLE_LOG_ATTACHMENT.equals(a.name()) && a.source() != null)
                .findFirst();
        if (attachment.isEmpty()) {
            return null;
        }
        Path logFile = resultsDir.resolve(attachment.get().source());
        try {
            if (Files.size(logFile) > MAX_CONSOLE_LOG_BYTES) {
                log.warn("Console log {} exceeds {} bytes; skipped", logFile.getFileName(), MAX_CONSOLE_LOG_BYTES);
                return null;
            }
            return Files.readString(logFile);
        } catch (IOException e) {
            log.warn("Console log attachment {} for {} could not be read: {}",
                    logFile.getFileName(), result.displayName(), e.getMessage());
            return null;
        }
    }

    private static TestResult latestAttempt(TestResult a, TestResult b) {
        return a.stop() >= b.stop() ? a : b;
    }
}