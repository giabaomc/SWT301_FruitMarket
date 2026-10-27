package selenium;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.support.ui.WebDriverWait;

/**
 * Common WebDriver setup for all Fruit_Market Selenium tests.
 *
 * The app must already be deployed and running (NetBeans: Run the Fruit_Market project first) at
 * {@link #baseUrl}. If it is not reachable, every Selenium test is skipped instead of failing, so
 * "Test Project" still reports the plain JUnit tests normally.
 *
 * Options (JVM system properties, e.g. test-sys-prop.headless=true in nbproject/project.properties):
 * baseUrl (default http://localhost:9999/Fruit_Market) and headless (default false).
 * The matching chromedriver is resolved automatically by Selenium Manager.
 */
public abstract class SeleniumTestBase {

    protected ChromeDriver driver;
    protected String baseUrl;

    // Seed accounts from sql/fruit_market.sql
    protected static final String ADMIN_EMAIL = "admin@vuonqua.local";
    protected static final String ADMIN_PASSWORD = "123456";
    protected static final String CUSTOMER_EMAIL = "customer@vuonqua.local";
    protected static final String CUSTOMER_PASSWORD = "123456";

    private static Boolean appReachable;

    @Before
    public void setUp() {
        baseUrl = System.getProperty("baseUrl", "http://localhost:9999/Fruit_Market");
        Assume.assumeTrue("Fruit_Market is not running at " + baseUrl + " - Run the project first", isAppReachable(baseUrl));

        ChromeOptions options = new ChromeOptions();
        if (Boolean.getBoolean("headless")) {
            options.addArguments("--headless=new");
        }
        driver = new ChromeDriver(options);
        driver.manage().timeouts().implicitlyWait(Duration.ofSeconds(5));
        driver.manage().window().maximize();
    }

    @After
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    /**
     * Clicks an element that loads a new page and waits until that page has replaced the current one.
     * The URL can't be used for this (login success and failure both end on .../productjsp), so the
     * current document is marked and we wait until a freshly loaded one without the mark is complete.
     */
    protected void clickAndWaitForNewPage(By locator) {
        driver.executeScript("window.beforeNavigation = true;");
        driver.findElement(locator).click();
        new WebDriverWait(driver, Duration.ofSeconds(10)).until(d -> Boolean.TRUE.equals(driver.executeScript(
                "return window.beforeNavigation === undefined && document.readyState === 'complete';")));
    }

    /** Checked once per test JVM: does the login page answer at all? */
    private static synchronized boolean isAppReachable(String baseUrl) {
        if (appReachable == null) {
            try {
                HttpURLConnection con = (HttpURLConnection) new URL(baseUrl + "/productjsp?service=login").openConnection();
                con.setConnectTimeout(3000);
                con.setReadTimeout(5000);
                appReachable = con.getResponseCode() == 200;
                con.disconnect();
            } catch (IOException ex) {
                appReachable = false;
            }
        }
        return appReachable;
    }
}
