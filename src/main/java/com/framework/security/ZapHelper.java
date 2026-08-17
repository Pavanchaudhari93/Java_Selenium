package com.framework.security;

import com.framework.config.ConfigReader;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.zaproxy.clientapi.core.ApiResponse;
import org.zaproxy.clientapi.core.ApiResponseElement;
import org.zaproxy.clientapi.core.ApiResponseList;
import org.zaproxy.clientapi.core.ApiResponseSet;
import org.zaproxy.clientapi.core.ClientApi;
import org.zaproxy.clientapi.core.ClientApiException;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * OWASP ZAP (DAST) integration helper.
 * <p>
 * Connects to a running ZAP instance via {@code zap-clientapi} and drives the
 * traditional spider + active scan workflow, then exposes alerts and an HTML
 * report for Allure/CI. Connection details come from config
 * ({@code security.zap.url}, {@code security.zap.apiKey},
 * {@code security.zap.targetUrl}).
 * <p>
 * ZAP must be running in daemon mode, e.g. {@code zap.sh -daemon -port 8080}.
 */
public class ZapHelper {

    private static final Logger LOG = LogManager.getLogger(ZapHelper.class);

    private final ClientApi clientApi;
    private final String targetUrl;

    public ZapHelper() {
        String zapUrl = ConfigReader.get("security.zap.url", "http://localhost:8080");
        String apiKey = ConfigReader.get("security.zap.apiKey", "");
        this.targetUrl = ConfigReader.get("security.zap.targetUrl", "");

        String host = parseHost(zapUrl);
        int port = parsePort(zapUrl);
        this.clientApi = new ClientApi(host, port, apiKey.isEmpty() ? null : apiKey);
        LOG.info("ZAP client initialized for [{}]:[{}]", host, port);
    }

    /**
     * Starts the ZAP spider against the configured target URL.
     *
     * @return the ZAP scan id of the spider run
     */
    public String startSpider() {
        return startSpider(targetUrl);
    }

    /**
     * Starts the ZAP spider against a given URL.
     *
     * @param url target URL to spider
     * @return the ZAP scan id of the spider run
     */
    public String startSpider(String url) {
        try {
            ApiResponse response = clientApi.spider.scan(url, "5", "true", "false", "");
            return extractScanId(response, "spider");
        } catch (ClientApiException e) {
            throw new IllegalStateException("Failed to start ZAP spider for: " + url, e);
        }
    }

    /**
     * Polls the spider until it reaches 100%.
     *
     * @param scanId spider scan id
     */
    public void waitForSpiderToComplete(String scanId) {
        int progress;
        do {
            progress = getSpiderProgress(scanId);
            LOG.info("Spider [{}] progress: {}%", scanId, progress);
            if (progress < 100) {
                sleepSeconds(2);
            }
        } while (progress < 100);
    }

    /**
     * @param scanId spider scan id
     * @return current spider progress percentage (0-100)
     */
    public int getSpiderProgress(String scanId) {
        try {
            ApiResponse response = clientApi.spider.status(scanId);
            return Integer.parseInt(((ApiResponseElement) response).getValue());
        } catch (ClientApiException e) {
            throw new IllegalStateException("Failed to get spider status for scan: " + scanId, e);
        }
    }

    /**
     * Starts an active scan against the configured target URL.
     *
     * @return the ZAP scan id of the active scan
     */
    public String startActiveScan() {
        return startActiveScan(targetUrl);
    }

    /**
     * Starts an active scan against a given URL.
     *
     * @param url target URL to scan
     * @return the ZAP scan id of the active scan
     */
    public String startActiveScan(String url) {
        try {
            ApiResponse response = clientApi.ascan.scan(url, "true", "true", null, "GET", null);
            return extractScanId(response, "active scan");
        } catch (ClientApiException e) {
            throw new IllegalStateException("Failed to start ZAP active scan for: " + url, e);
        }
    }

    /**
     * Polls the active scan until it reaches 100%.
     *
     * @param scanId active scan id
     */
    public void waitForActiveScanToComplete(String scanId) {
        int progress;
        do {
            progress = getActiveScanProgress(scanId);
            LOG.info("Active scan [{}] progress: {}%", scanId, progress);
            if (progress < 100) {
                sleepSeconds(5);
            }
        } while (progress < 100);
    }

    /**
     * @param scanId active scan id
     * @return current active scan progress percentage (0-100)
     */
    public int getActiveScanProgress(String scanId) {
        try {
            ApiResponse response = clientApi.ascan.status(scanId);
            return Integer.parseInt(((ApiResponseElement) response).getValue());
        } catch (ClientApiException e) {
            throw new IllegalStateException("Failed to get active scan status for scan: " + scanId, e);
        }
    }

    /**
     * Fetches all ZAP alerts found during scanning.
     *
     * @return list of alert maps (alert, risk, confidence, description, url, ...)
     */
    public List<Map<String, String>> getAlerts() {
        List<Map<String, String>> alerts = new ArrayList<>();
        try {
            ApiResponse response = clientApi.core.alerts(null, "0", "100000", null);
            if (response instanceof ApiResponseList) {
                ApiResponseList list = (ApiResponseList) response;
                for (ApiResponse item : list.getItems()) {
                    if (item instanceof ApiResponseSet) {
                        ApiResponseSet set = (ApiResponseSet) item;
                        Map<String, String> alert = new LinkedHashMap<>();
                        for (String key : set.getKeys()) {
                            String value = set.getStringValue(key);
                            alert.put(key, value == null ? "" : value);
                        }
                        alerts.add(alert);
                    }
                }
            }
            LOG.info("ZAP returned [{}] alerts", alerts.size());
            return alerts;
        } catch (ClientApiException e) {
            throw new IllegalStateException("Failed to fetch ZAP alerts", e);
        }
    }

    /**
     * Counts alerts at or above a given risk level.
     *
     * @param alerts     alerts from {@link #getAlerts()}
     * @param riskLevels risk levels to count, e.g. "High", "Medium"
     * @return number of matching alerts
     */
    public long countAlertsByRisk(List<Map<String, String>> alerts, List<String> riskLevels) {
        return alerts.stream()
                .filter(alert -> {
                    String risk = alert.getOrDefault("risk", "");
                    return riskLevels.stream().anyMatch(risk::equalsIgnoreCase);
                })
                .count();
    }

    /**
     * Exports the ZAP HTML report to disk.
     *
     * @param outputPath where to write the report, e.g. {@code target/zap-reports/zap-report.html}
     * @return absolute path of the written report
     */
    public String generateHtmlReport(String outputPath) {
        try {
            byte[] html = clientApi.core.htmlreport();
            Path path = Paths.get(outputPath);
            Files.createDirectories(path.getParent());
            Files.write(path, html);
            LOG.info("ZAP HTML report written to [{}]", path.toAbsolutePath());
            return path.toAbsolutePath().toString();
        } catch (ClientApiException | IOException e) {
            throw new IllegalStateException("Failed to write ZAP HTML report to: " + outputPath, e);
        }
    }

    /**
     * @return the configured ZAP target URL.
     */
    public String getTargetUrl() {
        return targetUrl;
    }

    /**
     * @return {@code true} when UI/API traffic should be routed through the ZAP proxy
     * so it gets recorded for passive scanning (config key
     * {@code security.zap.proxy.enabled}, default {@code false}).
     */
    public static boolean isProxyModeEnabled() {
        return ConfigReader.getBoolean("security.zap.proxy.enabled", false);
    }

    /**
     * @return the ZAP proxy host, parsed from {@code security.zap.url}.
     */
    public static String getProxyHost() {
        return parseHost(ConfigReader.get("security.zap.url", "http://localhost:8080"));
    }

    /**
     * @return the ZAP proxy port, parsed from {@code security.zap.url}.
     */
    public static int getProxyPort() {
        return parsePort(ConfigReader.get("security.zap.url", "http://localhost:8080"));
    }

    /**
     * @return the ZAP proxy address as {@code host:port}, for WebDriver/REST-assured proxy config.
     */
    public static String getProxyAddress() {
        return getProxyHost() + ":" + getProxyPort();
    }

    /**
     * Checks whether the ZAP proxy is actually accepting connections, so callers can
     * skip wiring up the proxy (and fall back to running without it) instead of
     * breaking every UI/API request when ZAP isn't running.
     *
     * @return {@code true} if a TCP connection to the ZAP proxy host/port succeeds
     */
    public static boolean isProxyListening() {
        String host = getProxyHost();
        int port = getProxyPort();
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(host, port), 2000);
            return true;
        } catch (IOException e) {
            LOG.warn("ZAP proxy not reachable at [{}:{}] - {}", host, port, e.getMessage());
            return false;
        }
    }

    private String extractScanId(ApiResponse response, String scanType) {
        String scanId = ((ApiResponseElement) response).getValue();
        LOG.info("{} scan started with id [{}]", scanType, scanId);
        return scanId;
    }

    private static String parseHost(String zapUrl) {
        String withoutScheme = zapUrl.replaceFirst("^https?://", "");
        return withoutScheme.substring(0, withoutScheme.indexOf(':')).trim();
    }

    private static int parsePort(String zapUrl) {
        String withoutScheme = zapUrl.replaceFirst("^https?://", "");
        String portPart = withoutScheme.substring(withoutScheme.indexOf(':') + 1);
        return Integer.parseInt(portPart.replaceAll("[/\\s].*", "").trim());
    }

    private void sleepSeconds(long seconds) {
        try {
            Thread.sleep(seconds * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOG.warn("Scan wait interrupted", e);
        }
    }
}
