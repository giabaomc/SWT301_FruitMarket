package dao;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Automated version of docs/UnitTest_Login.xlsx, sheet "login" (UTCID01-17):
 * the "Return" and "Exception" rows for String UserDAO.checkLogin(String email, String pass).
 *
 * Precondition: the Assignment database is created and seeded from sql/fruit_market.sql and
 * reachable with web/WEB-INF/ConnectDB.properties.
 *
 * UTCID13 (LOGIN-D01) and UTCID17 (LOGIN-D02) are expected to FAIL until those defects are fixed.
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UserDAOCheckLoginTest {

    private static final String ADMIN = "admin@vuonqua.local";
    private static final String CUSTOMER = "customer@vuonqua.local";
    private static final String LAN = "lan@example.com";
    private static final String PASSWORD = "123456";

    /** "aaa…a@example.com" of exactly the given length (tblUsers.email is NVARCHAR(100)). */
    private static String emailOfLength(int length) {
        String domain = "@example.com";
        return "a".repeat(length - domain.length()) + domain;
    }

    // ---------- Normal ----------

    /** UTCID01 - admin account, correct password -> "admin" */
    @Test
    public void utcid01_adminValidLogin() {
        assertReturns("admin", ADMIN, PASSWORD);
    }

    /** UTCID02 - customer account, correct password -> "customer" */
    @Test
    public void utcid02_customerValidLogin() {
        assertReturns("customer", CUSTOMER, PASSWORD);
    }

    /** UTCID03 - another customer (lan@example.com), correct password -> "customer" */
    @Test
    public void utcid03_otherCustomerValidLogin() {
        assertReturns("customer", LAN, PASSWORD);
    }

    // ---------- Abnormal ----------

    /** UTCID04 - admin account, wrong password -> null */
    @Test
    public void utcid04_wrongPassword() {
        assertReturns(null, ADMIN, "654321");
    }

    /** UTCID05 - email not in the database -> null */
    @Test
    public void utcid05_emailNotExist() {
        assertReturns(null, "notfound@example.com", PASSWORD);
    }

    /** UTCID06 - empty email -> null */
    @Test
    public void utcid06_emptyEmail() {
        assertReturns(null, "", PASSWORD);
    }

    /** UTCID07 - empty password -> null */
    @Test
    public void utcid07_emptyPassword() {
        assertReturns(null, ADMIN, "");
    }

    /** UTCID08 - null email -> null */
    @Test
    public void utcid08_nullEmail() {
        assertReturns(null, null, PASSWORD);
    }

    /** UTCID09 - null password -> null */
    @Test
    public void utcid09_nullPassword() {
        assertReturns(null, ADMIN, null);
    }

    /** UTCID10 - SQL injection in email (admin@vuonqua.local' --) -> null */
    @Test
    public void utcid10_sqlInjectionCommentInEmail() {
        assertReturns(null, ADMIN + "' --", "x");
    }

    /** UTCID11 - SQL injection ' OR '1'='1 in email and password -> null */
    @Test
    public void utcid11_sqlInjectionOrTrue() {
        assertReturns(null, "' OR '1'='1", "' OR '1'='1");
    }

    /** UTCID12 - email with leading space -> null */
    @Test
    public void utcid12_emailLeadingSpace() {
        assertReturns(null, " " + ADMIN, PASSWORD);
    }

    /** UTCID13 - password with trailing space "123456 " -> null (defect LOGIN-D01) */
    @Test
    public void utcid13_passwordTrailingSpace() {
        assertReturns(null, ADMIN, PASSWORD + " ");
    }

    // ---------- Boundary ----------

    /** UTCID14 - 100-character email (max length), not registered -> null */
    @Test
    public void utcid14_email100Chars() {
        assertReturns(null, emailOfLength(100), PASSWORD);
    }

    /** UTCID15 - 101-character email -> null */
    @Test
    public void utcid15_email101Chars() {
        assertReturns(null, emailOfLength(101), PASSWORD);
    }

    /** UTCID16 - 256-character password -> null */
    @Test
    public void utcid16_password256Chars() {
        assertReturns(null, ADMIN, "a".repeat(256));
    }

    // ---------- Abnormal: precondition not met ----------

    /** UTCID17 - database connection unavailable -> null, no exception (defect LOGIN-D02) */
    @Test
    public void utcid17_databaseUnavailable() {
        assertReturns(DaoTestSupport.unreachableConfig(), null, ADMIN, PASSWORD);
    }

    private static void assertReturns(String expected, String email, String pass) {
        assertReturns(DaoTestSupport.realConfig(), expected, email, pass);
    }

    /** Checks both rows of the sheet: Return = expected, Exception = None. */
    private static void assertReturns(byte[] config, String expected, String email, String pass) {
        String actual;
        try {
            actual = DaoTestSupport.checkLogin(config, email, pass);
        } catch (Throwable ex) {
            fail("Expected no exception, but checkLogin threw " + ex);
            return;
        }
        assertEquals("checkLogin(" + quote(email) + ", " + quote(pass) + ")", expected, actual);
    }

    private static String quote(String s) {
        if (s == null) {
            return "null";
        }
        return s.length() > 30 ? "\"" + s.substring(0, 12) + "…\" (" + s.length() + " chars)" : "\"" + s + "\"";
    }
}
