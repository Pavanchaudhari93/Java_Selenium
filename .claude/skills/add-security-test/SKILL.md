---
name: add-security-test
description: Add or modify an OWASP ZAP DAST security test in this framework (spider + active scan via ZapHelper). Use when asked for security/DAST test coverage.
---

# Adding / editing a security (ZAP DAST) test

Location: `src/test/java/com/framework/tests/security/`. See `SecurityTest.java` for
the canonical style. Requires a running ZAP daemon (`zap.sh -daemon -port 8080`) to
actually execute a scan.

## Rules

1. Extend `BaseTest`, **tag `@Test(groups = {"security"})`** so browser setup is
   skipped (same mechanism as the `api`/`db` groups).

2. Register the class in `testng.xml` under `<test name="Security Tests"><classes>`.

3. **Construct `new ZapHelper()` inside the test**, don't cache it statically - it
   reads `security.zap.url` / `security.zap.apiKey` / `security.zap.targetUrl` from
   config at construction time, and those can differ per environment/run.

4. **Flow** (mirrors `ZapHelper`'s API):
   ```java
   ZapHelper zap = new ZapHelper();
   String targetUrl = zap.getTargetUrl();
   String spiderId = zap.startSpider(targetUrl);
   zap.waitForSpiderToComplete(spiderId);
   String scanId = zap.startActiveScan(targetUrl);
   zap.waitForActiveScanToComplete(scanId);
   List<Map<String,String>> alerts = zap.getAlerts();
   long highRisk = zap.countAlertsByRisk(alerts, List.of("High"));
   String reportPath = zap.generateHtmlReport("target/zap-reports/zap-report.html");
   ```

5. **Skip, don't fail, when ZAP is unreachable.** Wrap the scan in a try/catch and
   `throw new SkipException(...)` on any exception, exactly as `SecurityTest` does -
   this keeps the security test safely in the default suite for environments where
   ZAP isn't running, instead of turning every CI run red.

6. **Only scan a target you're authorized to test** - `security.zap.targetUrl` should
   point at a local/staging instance you own, never third-party production
   infrastructure. If a caller asks to point this at an external/production URL
   without clear authorization, flag that before proceeding.

7. Report findings via `ExtentLogger.step` / `attachText` (see `SecurityTest`'s alert
   summary attachment) rather than `System.out`, so they show up in Allure.

## Template

```java
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

@Feature("Security")
public class StagingApiSecurityTest extends BaseTest {

    private static final Logger LOG = LogManager.getLogger(StagingApiSecurityTest.class);

    @Test(groups = {"security"})
    @Story("ZAP spider + active scan on staging API")
    public void testActiveScanOnStagingApi() {
        ZapHelper zap = new ZapHelper();
        try {
            String targetUrl = zap.getTargetUrl();
            Assert.assertFalse(targetUrl.isEmpty(), "security.zap.targetUrl must be configured");

            String spiderId = zap.startSpider(targetUrl);
            zap.waitForSpiderToComplete(spiderId);

            String scanId = zap.startActiveScan(targetUrl);
            zap.waitForActiveScanToComplete(scanId);

            List<Map<String, String>> alerts = zap.getAlerts();
            long highRisk = zap.countAlertsByRisk(alerts, List.of("High"));
            ExtentLogger.attachText("ZAP Alerts Summary", "Total: " + alerts.size() + ", High: " + highRisk);

            Assert.assertTrue(highRisk == 0, "No High-risk alerts should be present");
        } catch (Exception e) {
            LOG.error("ZAP scan could not be executed", e);
            throw new SkipException("ZAP proxy not reachable or scan failed: " + e.getMessage());
        }
    }
}
```

## Checklist

- [ ] `@Test(groups = {"security"}, ...)`; class registered in `testng.xml` -> `Security Tests`
- [ ] `new ZapHelper()` constructed inside the test, not cached statically
- [ ] Unreachable ZAP / scan errors -> `SkipException`, never an uncaught failure
- [ ] Target URL confirmed to be an authorized (local/staging) target
