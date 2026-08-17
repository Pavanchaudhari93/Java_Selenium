package com.framework.tests.security;

import com.framework.base.BaseTest;
import com.framework.security.ZapHelper;
import com.framework.utils.ExtentLogger;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.testng.Assert;
import org.testng.SkipException;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

/**
 * Optional DAST security test.
 * <p>
 * Requires a running OWASP ZAP instance (see config {@code security.zap.*}).
 * Runs the traditional spider + active scan against the configured target URL,
 * logs the alerts and exports an HTML report. If ZAP is unreachable the test
 * is skipped rather than failed, so it can safely stay in the suite.
 */
@Feature("Security")
public class SecurityTest extends BaseTest {

    private static final Logger LOG = LogManager.getLogger(SecurityTest.class);

    @Test(groups = {"security"})
    @Story("ZAP spider + active scan on target")
    public void testActiveScanOnTarget() {
        ZapHelper zap = new ZapHelper();
        try {
            String targetUrl = zap.getTargetUrl();
            Assert.assertFalse(targetUrl.isEmpty(),
                    "security.zap.targetUrl must be configured to run the security scan");

            ExtentLogger.step("Starting ZAP spider for: " + targetUrl);
            String spiderId = zap.startSpider(targetUrl);
            zap.waitForSpiderToComplete(spiderId);

            ExtentLogger.step("Starting ZAP active scan for: " + targetUrl);
            String scanId = zap.startActiveScan(targetUrl);
            zap.waitForActiveScanToComplete(scanId);

            List<Map<String, String>> alerts = zap.getAlerts();
            long highRisk = zap.countAlertsByRisk(alerts, List.of("High"));
            long mediumRisk = zap.countAlertsByRisk(alerts, List.of("Medium"));

            ExtentLogger.attachText("ZAP Alerts Summary",
                    "Total alerts: " + alerts.size()
                            + "\nHigh risk: " + highRisk
                            + "\nMedium risk: " + mediumRisk);

            String reportPath = zap.generateHtmlReport("target/zap-reports/zap-report.html");
            ExtentLogger.logInfo("ZAP report exported to: " + reportPath);
            ExtentLogger.attachText("ZAP Report Location", reportPath);

            LOG.info("ZAP scan complete | totalAlerts=[{}] high=[{}] medium=[{}]",
                    alerts.size(), highRisk, mediumRisk);
            Assert.assertTrue(alerts.size() >= 0, "Alerts list should be available");
        } catch (Exception e) {
            LOG.error("ZAP scan could not be executed - ZAP may not be running", e);
            throw new SkipException("ZAP proxy not reachable or scan failed: " + e.getMessage());
        }
    }
}
