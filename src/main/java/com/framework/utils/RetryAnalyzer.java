package com.framework.utils;

import com.framework.config.ConfigReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.IRetryAnalyzer;
import org.testng.ITestResult;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Retries failed tests a configurable number of times ({@code retry.count}).
 * <p>
 * Attempt counters are tracked per test method in the test context attributes,
 * which makes this implementation safe under parallel execution and when the
 * analyzer instance is shared. Attach it to every test automatically via
 * {@link com.framework.listeners.RetryListener} (IAnnotationTransformer).
 */
public class RetryAnalyzer implements IRetryAnalyzer {

    private static final Logger LOG = LogManager.getLogger(RetryAnalyzer.class);

    private static final String ATTEMPTS_ATTRIBUTE = "retryAnalyzerAttempts";

    @Override
    public boolean retry(ITestResult result) {
        int maxRetryCount = ConfigReader.getInt("retry.count", 0);
        if (maxRetryCount <= 0) {
            return false;
        }

        @SuppressWarnings("unchecked")
        Map<String, Integer> attempts = (Map<String, Integer>) result.getTestContext()
                .getAttribute(ATTEMPTS_ATTRIBUTE);
        if (attempts == null) {
            attempts = new ConcurrentHashMap<>();
            result.getTestContext().setAttribute(ATTEMPTS_ATTRIBUTE, attempts);
        }

        String methodKey = result.getMethod().getQualifiedName();
        int attempt = attempts.getOrDefault(methodKey, 0);
        if (attempt < maxRetryCount) {
            attempts.put(methodKey, attempt + 1);
            LOG.warn("Retrying failed test [{}] - attempt [{}/{}]",
                    result.getName(), attempt + 1, maxRetryCount);
            return true;
        }
        return false;
    }
}
