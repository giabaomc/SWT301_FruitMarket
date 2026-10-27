package controllers;

import dao.DBContext;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit test for the addProduct function.
 * Module: ProductJSP (service = addProduct) + ProductDAO.insertProduct
 * Test cases: UTCID01 - UTCID15 (sheet "addProduct").
 *
 * The test drives ProductJSP.doPost with fake HttpServletRequest / HttpServletResponse
 * objects and checks the redirect target, the database state (a new row in tblProducts or not)
 * and whether an unhandled exception escaped. It runs against the Assignment database
 * (sql/fruit_market.sql) configured in ConnectDB.properties. Every row inserted by a test
 * is deleted again in tearDown, so the seed data is left untouched.
 *
 * Note: UTCID05 (empty productName) is expected to FAIL against the current code,
 * because the product is inserted although the name is empty (Defect ADDPRODUCT-D01).
 *
 * Tester: hieunthe191744
 */
public class AddProductTest {

    private static final String LIST_PAGE = "productjsp";

    private final DbAccess db = new DbAccess();

    // State of tblProducts before each test, used to find and remove the rows a test inserts
    private int maxIdBefore;
    private int countBefore;

    // Per-test state used by the fake request / response
    private Map<String, String> params;
    private String role;
    private Outcome outcome;

    /** Captures what ProductJSP did with the request. */
    static class Outcome {
        String redirectUrl;
        Throwable thrown;
        Map<String, Object> attributes = new HashMap<>();
    }

    /** Exposes the connection opened by DBContext (same connection the application uses). */
    static class DbAccess extends DBContext {
        Connection conn() {
            return connection;
        }
    }

    /** One row of tblProducts as read back from the database. */
    static class Row {
        String productName, image, categoryID;
        double price;
        int quantity, status;
        Date importDate, expiryDate;
    }

    @Before
    public void setUp() throws Exception {
        assertNotNull("Database connection unavailable - check ConnectDB.properties", db.conn());
        maxIdBefore = scalar("SELECT ISNULL(MAX(productID), 0) FROM tblProducts");
        countBefore = scalar("SELECT COUNT(*) FROM tblProducts");

        params = baseParams();
        role = "admin";
        outcome = null;
    }

    @After
    public void tearDown() throws Exception {
        if (db.conn() != null) {
            try (PreparedStatement ps = db.conn().prepareStatement(
                    "DELETE FROM tblProducts WHERE productID > ?")) {
                ps.setInt(1, maxIdBefore);
                ps.executeUpdate();
            }
            db.conn().close();
        }
    }

    // ------------------------------------------------------------------
    // UTCID01 - UTCID03: valid data (and boundary values that are accepted)
    // ------------------------------------------------------------------

    /** UTCID01 - Admin adds a product with valid data. Normal. */
    @Test
    public void utcid01_adminAddsProductWithValidData() throws Exception {
        send();
        assertRedirectToList();
        assertProductInserted("Cam sành Hà Giang", "images/cam-sanh.jpg", 35000, 100, "CAM",
                "2026-10-05", "2026-10-15", 1);
    }

    /** UTCID02 - Name is 150 characters (max length), image is empty, status DeActive. Boundary. */
    @Test
    public void utcid02_name150CharsEmptyImageDeactiveIsAccepted() throws Exception {
        String name = "a".repeat(150);
        params.put("productName", name);
        params.put("image", "");
        params.put("status", "0");
        send();
        assertRedirectToList();
        assertProductInserted(name, "", 35000, 100, "CAM", "2026-10-05", "2026-10-15", 0);
    }

    /** UTCID03 - Price 0, quantity 0 and expiry date equal to import date (lower boundaries). Boundary. */
    @Test
    public void utcid03_zeroPriceZeroQuantitySameDatesAreAccepted() throws Exception {
        params.put("price", "0");
        params.put("quantity", "0");
        params.put("expiryDate", "2026-10-05");
        send();
        assertRedirectToList();
        assertProductInserted("Cam sành Hà Giang", "images/cam-sanh.jpg", 0, 0, "CAM",
                "2026-10-05", "2026-10-05", 1);
    }

    // ------------------------------------------------------------------
    // UTCID04: access control
    // ------------------------------------------------------------------

    /** UTCID04 - A user who is not admin tries to add a product. Abnormal. */
    @Test
    public void utcid04_nonAdminCannotAddProduct() throws Exception {
        role = "customer";
        send();
        assertRedirectToList();
        assertNothingInserted();
    }

    // ------------------------------------------------------------------
    // UTCID05 - UTCID07: invalid productName / price, rejected without an exception
    // ------------------------------------------------------------------

    /** UTCID05 - Product name is empty. Abnormal. Expected to be rejected (currently inserted: defect). */
    @Test
    public void utcid05_emptyProductNameIsRejected() throws Exception {
        params.put("productName", "");
        send();
        assertRedirectToList();
        assertNothingInserted();
    }

    /** UTCID06 - Product name is 151 characters (column is NVARCHAR(150)). Boundary. */
    @Test
    public void utcid06_productNameOver150CharsIsRejected() throws Exception {
        params.put("productName", "a".repeat(151));
        send();
        assertRedirectToList();
        assertNothingInserted();
    }

    /** UTCID07 - Price is -1 (below lower boundary). Abnormal. */
    @Test
    public void utcid07_negativePriceIsRejected() throws Exception {
        params.put("price", "-1");
        send();
        assertRedirectToList();
        assertNothingInserted();
    }

    // ------------------------------------------------------------------
    // UTCID08 - UTCID10: price / quantity problems
    // ------------------------------------------------------------------

    /** UTCID08 - Price is not a number ("abc"). Abnormal. NumberFormatException expected. */
    @Test
    public void utcid08_nonNumericPriceThrowsNumberFormatException() throws Exception {
        params.put("price", "abc");
        send();
        assertThrown(NumberFormatException.class);
        assertNothingInserted();
    }

    /** UTCID09 - Quantity is -1 (below lower boundary). Abnormal. */
    @Test
    public void utcid09_negativeQuantityIsRejected() throws Exception {
        params.put("quantity", "-1");
        send();
        assertRedirectToList();
        assertNothingInserted();
    }

    /** UTCID10 - Quantity is a decimal ("1.5"), not an integer. Abnormal. NumberFormatException expected. */
    @Test
    public void utcid10_decimalQuantityThrowsNumberFormatException() throws Exception {
        params.put("quantity", "1.5");
        send();
        assertThrown(NumberFormatException.class);
        assertNothingInserted();
    }

    // ------------------------------------------------------------------
    // UTCID11 - UTCID15: category, dates and status
    // ------------------------------------------------------------------

    /** UTCID11 - Category ID does not exist (violates the foreign key). Abnormal. */
    @Test
    public void utcid11_nonExistingCategoryIsRejected() throws Exception {
        params.put("categoryID", "XXX");
        send();
        assertRedirectToList();
        assertNothingInserted();
    }

    /** UTCID12 - Import date has an invalid format ("05/10/2026"). Abnormal. IllegalArgumentException expected. */
    @Test
    public void utcid12_invalidImportDateFormatThrowsIllegalArgumentException() throws Exception {
        params.put("importDate", "05/10/2026");
        send();
        assertThrown(IllegalArgumentException.class);
        assertNothingInserted();
    }

    /** UTCID13 - Expiry date is empty. Abnormal. IllegalArgumentException expected. */
    @Test
    public void utcid13_emptyExpiryDateThrowsIllegalArgumentException() throws Exception {
        params.put("expiryDate", "");
        send();
        assertThrown(IllegalArgumentException.class);
        assertNothingInserted();
    }

    /** UTCID14 - Expiry date is before import date. Abnormal. */
    @Test
    public void utcid14_expiryDateBeforeImportDateIsRejected() throws Exception {
        params.put("expiryDate", "2026-10-01");
        send();
        assertRedirectToList();
        assertNothingInserted();
    }

    /** UTCID15 - Status is 2 (only 0 and 1 are allowed). Abnormal. */
    @Test
    public void utcid15_statusOtherThan0Or1IsRejected() throws Exception {
        params.put("status", "2");
        send();
        assertRedirectToList();
        assertNothingInserted();
    }

    // ------------------------------------------------------------------
    // Helpers: request / response / session fakes
    // ------------------------------------------------------------------

    /** Default valid request for addProduct: an admin submits the add-product form with valid data. */
    private Map<String, String> baseParams() {
        Map<String, String> m = new HashMap<>();
        m.put("service", "addProduct");
        m.put("submit", "Thêm hoa quả");
        m.put("productName", "Cam sành Hà Giang");
        m.put("image", "images/cam-sanh.jpg");
        m.put("price", "35000");
        m.put("quantity", "100");
        m.put("categoryID", "CAM");
        m.put("importDate", "2026-10-05");
        m.put("expiryDate", "2026-10-15");
        m.put("status", "1");
        return m;
    }

    /** Runs ProductJSP.doPost and records the outcome. Exceptions are captured, not rethrown. */
    private void send() {
        outcome = new Outcome();
        try {
            new ProductJSP().doPost(fakeRequest(), fakeResponse());
        } catch (Throwable t) {
            outcome.thrown = t;
        }
    }

    private HttpServletRequest fakeRequest() {
        InvocationHandler handler = (proxy, m, args) -> switch (m.getName()) {
            case "getParameter" -> params.get((String) args[0]);
            case "getSession" -> fakeSession();
            case "getRequestDispatcher" -> fakeDispatcher();
            case "setAttribute" -> {
                outcome.attributes.put((String) args[0], args[1]);
                yield null;
            }
            case "getAttribute" -> outcome.attributes.get((String) args[0]);
            default -> defaultValue(m.getReturnType());
        };
        return (HttpServletRequest) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{HttpServletRequest.class}, handler);
    }

    private HttpSession fakeSession() {
        InvocationHandler handler = (proxy, m, args) -> {
            if (m.getName().equals("getAttribute") && "role".equals(args[0])) {
                return role;
            }
            return defaultValue(m.getReturnType());
        };
        return (HttpSession) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{HttpSession.class}, handler);
    }

    private RequestDispatcher fakeDispatcher() {
        InvocationHandler handler = (proxy, m, args) -> defaultValue(m.getReturnType());
        return (RequestDispatcher) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{RequestDispatcher.class}, handler);
    }

    private HttpServletResponse fakeResponse() {
        InvocationHandler handler = (proxy, m, args) -> {
            if (m.getName().equals("sendRedirect")) {
                outcome.redirectUrl = (String) args[0];
            }
            return defaultValue(m.getReturnType());
        };
        return (HttpServletResponse) Proxy.newProxyInstance(
                getClass().getClassLoader(), new Class<?>[]{HttpServletResponse.class}, handler);
    }

    /** Returns a safe default for interface methods that the servlet does not use. */
    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0d;
        if (type == float.class) return 0.0f;
        if (type == short.class) return (short) 0;
        if (type == byte.class) return (byte) 0;
        if (type == char.class) return '\0';
        return null;
    }

    // ------------------------------------------------------------------
    // Helpers: assertions
    // ------------------------------------------------------------------

    private void assertNoUnhandledException() {
        assertNull("Unhandled exception escaped the servlet: " + outcome.thrown,
                outcome.thrown);
    }

    /** Expected: user is sent back to the product list. */
    private void assertRedirectToList() {
        assertNoUnhandledException();
        assertEquals("Should redirect to the product list", LIST_PAGE, outcome.redirectUrl);
    }

    /** Expected: the servlet fails with this exception and does not redirect. */
    private void assertThrown(Class<? extends Throwable> expected) {
        assertNotNull("Expected " + expected.getSimpleName() + " but nothing was thrown",
                outcome.thrown);
        assertTrue("Expected " + expected.getSimpleName() + " but was " + outcome.thrown,
                expected.isInstance(outcome.thrown));
        assertNull("Should not redirect after an exception", outcome.redirectUrl);
    }

    /** Expected: tblProducts has no new row. */
    private void assertNothingInserted() throws SQLException {
        assertEquals("No record should be inserted",
                countBefore, scalar("SELECT COUNT(*) FROM tblProducts"));
    }

    /** Expected: exactly one new row was inserted and it holds these values. */
    private void assertProductInserted(String name, String image, double price, int qty,
            String categoryId, String importDate, String expiryDate, int status) throws SQLException {
        assertEquals("Exactly one record should be inserted",
                countBefore + 1, scalar("SELECT COUNT(*) FROM tblProducts"));
        Row r = readNewRow();
        assertNotNull("The new product must exist in tblProducts", r);
        assertEquals("productName", name, r.productName);
        assertEquals("image", image, r.image);
        assertEquals("price", price, r.price, 0.001);
        assertEquals("quantity", qty, r.quantity);
        assertEquals("categoryID", categoryId, r.categoryID);
        assertEquals("importDate", Date.valueOf(importDate), r.importDate);
        assertEquals("expiryDate", Date.valueOf(expiryDate), r.expiryDate);
        assertEquals("status", status, r.status);
    }

    // ------------------------------------------------------------------
    // Helpers: direct database access (used to count rows and read the new row)
    // ------------------------------------------------------------------

    private int scalar(String sql) throws SQLException {
        try (Statement st = db.conn().createStatement(); ResultSet rs = st.executeQuery(sql)) {
            rs.next();
            return rs.getInt(1);
        }
    }

    /** Reads the row inserted by the current test (the one with the highest ID above maxIdBefore). */
    private Row readNewRow() throws SQLException {
        String sql = "SELECT TOP 1 productName, image, price, quantity, categoryID, importDate, "
                + "expiryDate, status FROM tblProducts WHERE productID > ? ORDER BY productID DESC";
        try (PreparedStatement ps = db.conn().prepareStatement(sql)) {
            ps.setInt(1, maxIdBefore);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                Row r = new Row();
                r.productName = rs.getString(1);
                r.image = rs.getString(2);
                r.price = rs.getDouble(3);
                r.quantity = rs.getInt(4);
                r.categoryID = rs.getString(5);
                r.importDate = rs.getDate(6);
                r.expiryDate = rs.getDate(7);
                r.status = rs.getInt(8);
                return r;
            }
        }
    }
}
