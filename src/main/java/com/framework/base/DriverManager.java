package com.framework.base;

import com.framework.config.ConfigReader;
import com.framework.security.ZapHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.Proxy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.edge.EdgeDriver;
import org.openqa.selenium.edge.EdgeOptions;
import org.openqa.selenium.firefox.FirefoxDriver;
import org.openqa.selenium.firefox.FirefoxOptions;
import org.openqa.selenium.remote.CapabilityType;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Duration;

import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * Manages the {@link WebDriver} instance for the current thread.
 * <p>
 * A {@link ThreadLocal} guard is used so parallel test execution is safe:
 * every thread gets its own WebDriver that is created lazily and torn down
 * independently. Supports local execution on Chrome/Firefox/Edge and remote
 * Selenium Grid execution via {@link RemoteWebDriver}.
 */
public final class DriverManager {

    private static final Logger LOG = LogManager.getLogger(DriverManager.class);

    private static final ThreadLocal<WebDriver> DRIVER = new ThreadLocal<>();

    private DriverManager() {
        // utility class - no instantiation
    }

    /**
     * Initializes the WebDriver for the current thread if not already present.
     * Browser, headless mode and execution mode (local/grid) are read from config.
     */
    public static void initDriver() {
        if (DRIVER.get() != null) {
            LOG.debug("WebDriver already initialized for thread [{}]", Thread.currentThread().getName());
            return;
        }
        String browser = ConfigReader.get("browser", "chrome");
        boolean headless = ConfigReader.getBoolean("browser.headless", false);

        WebDriver driver;
        if ("grid".equalsIgnoreCase(ConfigReader.get("execution.mode", "local"))) {
            driver = createRemoteDriver(browser, headless);
        } else {
            driver = createLocalDriver(browser, headless);
        }

        configureDriver(driver);
        DRIVER.set(driver);
        LOG.info("WebDriver initialized for thread [{}] | browser=[{}] | headless=[{}]",
                Thread.currentThread().getName(), browser, headless);
    }

    /**
     * @return the WebDriver for the current thread, initializing it lazily if needed.
     */
    public static WebDriver getDriver() {
        if (DRIVER.get() == null) {
            initDriver();
        }
        return DRIVER.get();
    }

    /**
     * @return {@code true} if a WebDriver already exists for the current thread.
     */
    public static boolean isDriverInitialized() {
        return DRIVER.get() != null;
    }

    /**
     * Quits and removes the WebDriver for the current thread. Never throws.
     */
    public static void quitDriver() {
        WebDriver driver = DRIVER.get();
        if (driver != null) {
            try {
                driver.quit();
                LOG.debug("WebDriver quit for thread [{}]", Thread.currentThread().getName());
            } catch (Exception e) {
                LOG.error("Error while quitting WebDriver for thread [{}]", Thread.currentThread().getName(), e);
            } finally {
                DRIVER.remove();
            }
        }
    }

    private static WebDriver createLocalDriver(String browser, boolean headless) {
        switch (browser.toLowerCase()) {
            case "firefox":
                WebDriverManager.firefoxdriver().setup();
                FirefoxOptions firefoxOptions = new FirefoxOptions();
                if (headless) {
                    firefoxOptions.addArguments("-headless");
                }
                applyZapProxyIfEnabled(firefoxOptions);
                return new FirefoxDriver(firefoxOptions);

            case "edge":
                WebDriverManager.edgedriver().setup();
                EdgeOptions edgeOptions = new EdgeOptions();
                if (headless) {
                    edgeOptions.addArguments("--headless=new");
                }
                edgeOptions.addArguments("--disable-gpu", "--window-size=1920,1080");
                applyZapProxyIfEnabled(edgeOptions);
                return new EdgeDriver(edgeOptions);

            case "chrome":
            default:
                WebDriverManager.chromedriver().setup();
                ChromeOptions chromeOptions = new ChromeOptions();
                if (headless) {
                    chromeOptions.addArguments("--headless=new");
                }
                chromeOptions.addArguments("--no-sandbox", "--disable-dev-shm-usage",
                        "--disable-gpu", "--window-size=1920,1080");
                applyZapProxyIfEnabled(chromeOptions);
                return new ChromeDriver(chromeOptions);
        }
    }

    private static WebDriver createRemoteDriver(String browser, boolean headless) {
        String gridUrl = ConfigReader.get("grid.url", "http://localhost:4444/wd/hub");
        MutableCapabilities capabilities = createCapabilities(browser, headless);
        try {
            LOG.info("Connecting to Selenium Grid at [{}]", gridUrl);
            return new RemoteWebDriver(new URL(gridUrl), capabilities);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Invalid Selenium Grid URL configured: " + gridUrl, e);
        }
    }

    private static MutableCapabilities createCapabilities(String browser, boolean headless) {
        switch (browser.toLowerCase()) {
            case "firefox":
                FirefoxOptions firefoxOptions = new FirefoxOptions();
                if (headless) {
                    firefoxOptions.addArguments("-headless");
                }
                applyZapProxyIfEnabled(firefoxOptions);
                return firefoxOptions;
            case "edge":
                EdgeOptions edgeOptions = new EdgeOptions();
                if (headless) {
                    edgeOptions.addArguments("--headless=new");
                }
                applyZapProxyIfEnabled(edgeOptions);
                return edgeOptions;
            case "chrome":
            default:
                ChromeOptions chromeOptions = new ChromeOptions();
                if (headless) {
                    chromeOptions.addArguments("--headless=new");
                }
                chromeOptions.addArguments("--no-sandbox", "--disable-dev-shm-usage");
                applyZapProxyIfEnabled(chromeOptions);
                return chromeOptions;
        }
    }

    /**
     * When {@code security.zap.proxy.enabled=true}, routes this browser's HTTP/HTTPS
     * traffic through the ZAP proxy (derived from {@code security.zap.url}) so ZAP can
     * passively record every request the UI test makes, and relaxes TLS validation so
     * ZAP's MITM certificate on HTTPS traffic doesn't fail page loads.
     *
     * @param options mutable capabilities/options for the browser about to be created
     */
    private static void applyZapProxyIfEnabled(MutableCapabilities options) {
        if (!ZapHelper.isProxyModeEnabled()) {
            return;
        }
        if (!ZapHelper.isProxyListening()) {
            LOG.warn("security.zap.proxy.enabled=true but ZAP is not listening at [{}] - " +
                    "continuing without routing browser traffic through it", ZapHelper.getProxyAddress());
            return;
        }
        String proxyAddress = ZapHelper.getProxyAddress();

        Proxy proxy = new Proxy();
        proxy.setHttpProxy(proxyAddress);
        proxy.setSslProxy(proxyAddress);

        options.setCapability(CapabilityType.PROXY, proxy);
        options.setCapability(CapabilityType.ACCEPT_INSECURE_CERTS, true);
        LOG.info("Routing browser traffic through ZAP proxy at [{}]", proxyAddress);
    }

    private static void configureDriver(WebDriver driver) {
        driver.manage().window().maximize();
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(ConfigReader.getInt("timeout.implicit", 10)));
        driver.manage().timeouts().pageLoadTimeout(Duration.ofSeconds(ConfigReader.getInt("timeout.pageLoad", 60)));
    }
}
