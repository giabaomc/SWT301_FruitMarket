package selenium;

import java.util.List;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * Automated version of docs/UnitTest_Login.xlsx, sheet "login": the "Screen message" rows
 * (UTCID01-16), checked by submitting the login form of the running application.
 *
 * Empty fields are sent by removing the form's HTML "required" check first; null fields are sent
 * by removing the input, so the request has no such parameter (request.getParameter returns null).
 * UTCID17 (database unavailable) is covered at unit level only, in dao.UserDAOCheckLoginTest.
 *
 * UTCID13 (LOGIN-D01) is expected to FAIL until that defect is fixed.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class LoginScreenTest extends SeleniumTestBase {

    private static final String LAN_EMAIL = "lan@example.com";
    private static final String LAN_PASSWORD = "123456";

    // ---------- Normal: session stores email & role, redirect to product list ----------

    /** UTCID01 - admin logs in -> product list, admin functions shown */
    @Test
    public void utcid01_adminValidLogin() {
        submitLogin(ADMIN_EMAIL, ADMIN_PASSWORD);
        assertLoggedIn(ADMIN_EMAIL, true);
    }

    /** UTCID02 - customer logs in -> product list, no admin functions */
    @Test
    public void utcid02_customerValidLogin() {
        submitLogin(CUSTOMER_EMAIL, CUSTOMER_PASSWORD);
        assertLoggedIn(CUSTOMER_EMAIL, false);
    }

    /** UTCID03 - lan@example.com logs in -> product list, no admin functions */
    @Test
    public void utcid03_otherCustomerValidLogin() {
        submitLogin(LAN_EMAIL, LAN_PASSWORD);
        assertLoggedIn(LAN_EMAIL, false);
    }

    // ---------- Abnormal / Boundary: stay on login page with "Login Failed!" ----------

    /** UTCID04 - wrong password -> "Login Failed!" */
    @Test
    public void utcid04_wrongPassword() {
        submitLogin(ADMIN_EMAIL, "654321");
        assertLoginFailed();
    }

    /** UTCID05 - email not in the database -> "Login Failed!" */
    @Test
    public void utcid05_emailNotExist() {
        submitLogin("notfound@example.com", ADMIN_PASSWORD);
        assertLoginFailed();
    }

    /** UTCID06 - empty email -> "Login Failed!" */
    @Test
    public void utcid06_emptyEmail() {
        submitLogin("", ADMIN_PASSWORD);
        assertLoginFailed();
    }

    /** UTCID07 - empty password -> "Login Failed!" */
    @Test
    public void utcid07_emptyPassword() {
        submitLogin(ADMIN_EMAIL, "");
        assertLoginFailed();
    }

    /** UTCID08 - null email (parameter missing) -> "Login Failed!" */
    @Test
    public void utcid08_nullEmail() {
        submitLogin(null, ADMIN_PASSWORD);
        assertLoginFailed();
    }

    /** UTCID09 - null password (parameter missing) -> "Login Failed!" */
    @Test
    public void utcid09_nullPassword() {
        submitLogin(ADMIN_EMAIL, null);
        assertLoginFailed();
    }

    /** UTCID10 - SQL injection in email (admin@vuonqua.local' --) -> "Login Failed!" */
    @Test
    public void utcid10_sqlInjectionCommentInEmail() {
        submitLogin(ADMIN_EMAIL + "' --", "x");
        assertLoginFailed();
    }

    /** UTCID11 - SQL injection ' OR '1'='1 -> "Login Failed!" */
    @Test
    public void utcid11_sqlInjectionOrTrue() {
        submitLogin("' OR '1'='1", "' OR '1'='1");
        assertLoginFailed();
    }

    /** UTCID12 - email with leading space -> "Login Failed!" */
    @Test
    public void utcid12_emailLeadingSpace() {
        submitLogin(" " + ADMIN_EMAIL, ADMIN_PASSWORD);
        assertLoginFailed();
    }

    /** UTCID13 - password with trailing space -> "Login Failed!" (defect LOGIN-D01) */
    @Test
    public void utcid13_passwordTrailingSpace() {
        submitLogin(ADMIN_EMAIL, ADMIN_PASSWORD + " ");
        assertLoginFailed();
    }

    /** UTCID14 - 100-character email (max length) -> "Login Failed!" */
    @Test
    public void utcid14_email100Chars() {
        submitLogin(emailOfLength(100), ADMIN_PASSWORD);
        assertLoginFailed();
    }

    /** UTCID15 - 101-character email -> "Login Failed!" */
    @Test
    public void utcid15_email101Chars() {
        submitLogin(emailOfLength(101), ADMIN_PASSWORD);
        assertLoginFailed();
    }

    /** UTCID16 - 256-character password -> "Login Failed!" */
    @Test
    public void utcid16_password256Chars() {
        submitLogin(ADMIN_EMAIL, "a".repeat(256));
        assertLoginFailed();
    }

    // ---------- helpers ----------

    /** "aaa…a@example.com" of exactly the given length. */
    private static String emailOfLength(int length) {
        String domain = "@example.com";
        return "a".repeat(length - domain.length()) + domain;
    }

    /** Fills and submits the login form; "" submits an empty field, null omits the field entirely. */
    private void submitLogin(String email, String password) {
        driver.get(baseUrl + "/productjsp?service=login");
        fillField("txtemail", email);
        fillField("txtpass", password);
        clickAndWaitForNewPage(By.cssSelector("input[type='submit']"));
    }

    private void fillField(String id, String value) {
        WebElement input = driver.findElement(By.id(id));
        if (value == null) {
            driver.executeScript("arguments[0].remove();", input);
        } else if (value.isEmpty()) {
            driver.executeScript("arguments[0].removeAttribute('required');", input);
        } else {
            input.sendKeys(value);
        }
    }

    private void assertLoggedIn(String email, boolean admin) {
        assertFalse("Expected redirect to the product list (search box), got: " + driver.getTitle(),
                driver.findElements(By.id("txtName")).isEmpty());
        assertTrue("No error message expected", driver.findElements(By.cssSelector(".alert-error")).isEmpty());
        assertEquals("Header should show the email stored in the session", "Xin chào, " + email, textOf(".nav-user"));
        List<WebElement> addLinks = driver.findElements(By.cssSelector("a[href='productjsp?service=addProduct']"));
        assertEquals("Admin-only functions should be " + (admin ? "shown" : "hidden") + " for the role stored in the session",
                admin, !addLinks.isEmpty());
    }

    private void assertLoginFailed() {
        assertNull("User should not be logged in", textOf(".nav-user"));
        assertEquals("Should stay on the login page", "Đăng nhập", textOf("h1.page-title"));
        assertEquals("Login Failed!", textOf(".alert-error"));
    }

    /** Text of the first element matching the selector, or null if there is none. */
    private String textOf(String cssSelector) {
        List<WebElement> found = driver.findElements(By.cssSelector(cssSelector));
        return found.isEmpty() ? null : found.get(0).getText();
    }
}
