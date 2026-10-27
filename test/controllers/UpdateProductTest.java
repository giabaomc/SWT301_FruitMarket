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
import java.util.HashMap;
import java.util.Map;
import models.Product;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Unit test for the updateProduct function.
 * Module: ProductJSP (service = updateProduct) + ProductDAO.updateProduct
 * Test cases: UTCID01 - UTCID21 (sheet "UpdateProduct").
 *
 * The test drives ProductJSP.doPost with fake HttpServletRequest / HttpServletResponse
 * objects and checks the redirect or forward target, the database state and whether an
 * unhandled exception escaped. It runs against the Assignment database (sql/fruit_market.sql)
 * configured in ConnectDB.properties. Product 1 is restored after every test.
 *
 * Tester: HaiVHE190224
 */
public class UpdateProductTest {

    private static final int PRODUCT_ID = 1;
    private static final String LIST_PAGE = "productjsp";
    private static final String EDIT_FORM = "jsp/updateProduct.jsp";

    private final DbAccess db = new DbAccess();
    private Product original;

    // Per-test state used by the fake request / response
    private Map<String, String> params;
    private String role;
    private Outcome outcome;

    /** Captures what ProductJSP did with the request. */
    static class Outcome {
        String redirectUrl;
        String forwardPath;
        Throwable thrown;
        Map<String, Object> attributes = new HashMap<>();
    }

    /** Exposes the connection opened by DBContext (same connection the application uses). */
    static class DbAccess extends DBContext {
        Connection conn() {
            return connection;
        }
    }

    @Before
    public void setUp() throws Exception {
        assertNotNull("Database connection unavailable - check ConnectDB.properties", db.conn());
        original = readProduct(PRODUCT_ID);
        assertNotNull("Seed product 1 must exist in tblProducts", original);

        params = baseParams();
        role = "admin";
        outcome = null;
    }

    @After
    public void tearDown() throws Exception {
        if (original != null && db.conn() != null) {
            writeRow(original);
        }
        if (db.conn() != null) {
            db.conn().close();
        }
    }

    // ------------------------------------------------------------------
    // UTCID01 - UTCID08: valid data (and boundary values that are accepted)
    // ------------------------------------------------------------------

    /** UTCID01 - Admin updates all fields with valid data. Normal. */
    @Test
    public void utcid01_adminUpdatesAllFieldsWithValidData() throws Exception {
        send();
        assertRedirectToList();
        assertDbRow("Cam sanh loai 1", "images/cam-sanh-moi.jpg", 40000, 120, "CAM",
                "2026-09-01", "2026-09-10", 1);
    }

    /** UTCID02 - Admin sets status to DeActive (0) with valid data. Normal. */
    @Test
    public void utcid02_adminDeactivatesProduct() throws Exception {
        params.put("status", "0");
        send();
        assertRedirectToList();
        assertDbRow("Cam sanh loai 1", "images/cam-sanh-moi.jpg", 40000, 120, "CAM",
                "2026-09-01", "2026-09-10", 0);
    }

    /** UTCID03 - Image is empty (column allows NULL). Boundary. */
    @Test
    public void utcid03_emptyImageIsAccepted() throws Exception {
        params.put("image", "");
        send();
        assertRedirectToList();
        assertDbRow("Cam sanh loai 1", "", 40000, 120, "CAM", "2026-09-01", "2026-09-10", 1);
    }

    /** UTCID04 - Price is 0 (lower valid boundary). Boundary. */
    @Test
    public void utcid04_priceZeroIsAccepted() throws Exception {
        params.put("price", "0");
        send();
        assertRedirectToList();
        assertDbRow("Cam sanh loai 1", "images/cam-sanh-moi.jpg", 0, 120, "CAM",
                "2026-09-01", "2026-09-10", 1);
    }

    /** UTCID05 - Price is -1 (below lower boundary). Boundary, expected to be rejected. */
    @Test
    public void utcid05_negativePriceIsRejected() throws Exception {
        params.put("price", "-1");
        send();
        assertStaysOnEditForm();
        assertDbUnchanged();
    }

    /** UTCID06 - Quantity is 0 (lower valid boundary). Boundary. */
    @Test
    public void utcid06_quantityZeroIsAccepted() throws Exception {
        params.put("quantity", "0");
        send();
        assertRedirectToList();
        assertDbRow("Cam sanh loai 1", "images/cam-sanh-moi.jpg", 40000, 0, "CAM",
                "2026-09-01", "2026-09-10", 1);
    }

    /** UTCID07 - Quantity is -1 (below lower boundary). Boundary, expected to be rejected. */
    @Test
    public void utcid07_negativeQuantityIsRejected() throws Exception {
        params.put("quantity", "-1");
        send();
        assertStaysOnEditForm();
        assertDbUnchanged();
    }

    /** UTCID08 - Expiry date equals import date (boundary of the date rule). Boundary. */
    @Test
    public void utcid08_expiryDateEqualToImportDateIsAccepted() throws Exception {
        params.put("expiryDate", "2026-09-01");
        send();
        assertRedirectToList();
        assertDbRow("Cam sanh loai 1", "images/cam-sanh-moi.jpg", 40000, 120, "CAM",
                "2026-09-01", "2026-09-01", 1);
    }

    // ------------------------------------------------------------------
    // UTCID09 - UTCID15: invalid data, expected to stay on the edit form
    // ------------------------------------------------------------------

    /** UTCID09 - Expiry date is before import date. Abnormal. */
    @Test
    public void utcid09_expiryDateBeforeImportDateIsRejected() throws Exception {
        params.put("expiryDate", "2026-08-31");
        send();
        assertStaysOnEditForm();
        assertDbUnchanged();
    }

    /** UTCID10 - Price is not a number ("abc"). Abnormal. */
    @Test
    public void utcid10_nonNumericPriceIsRejected() throws Exception {
        params.put("price", "abc");
        send();
        assertStaysOnEditForm();
        assertDbUnchanged();
    }

    /** UTCID11 - Quantity is a decimal ("1.5"), not an integer. Abnormal. */
    @Test
    public void utcid11_decimalQuantityIsRejected() throws Exception {
        params.put("quantity", "1.5");
        send();
        assertStaysOnEditForm();
        assertDbUnchanged();
    }

    /** UTCID12 - Import date is empty / wrong format. Abnormal. */
    @Test
    public void utcid12_emptyImportDateIsRejected() throws Exception {
        params.put("importDate", "");
        send();
        assertStaysOnEditForm();
        assertDbUnchanged();
    }

    /** UTCID13 - Product name is empty. Abnormal. */
    @Test
    public void utcid13_emptyProductNameIsRejected() throws Exception {
        params.put("productName", "");
        send();
        assertStaysOnEditForm();
        assertDbUnchanged();
    }

    /** UTCID14 - Product name is 151 characters (column is NVARCHAR(150)). Boundary. */
    @Test
    public void utcid14_productNameOver150CharsIsRejected() throws Exception {
        params.put("productName", "a".repeat(151));
        send();
        assertStaysOnEditForm();
        assertDbUnchanged();
    }

    /** UTCID15 - Category ID does not exist (violates foreign key). Abnormal. */
    @Test
    public void utcid15_nonExistingCategoryIsRejected() throws Exception {
        params.put("categoryID", "XXX");
        send();
        assertStaysOnEditForm();
        assertDbUnchanged();
    }

    // ------------------------------------------------------------------
    // UTCID16 - UTCID18: product does not exist
    // ------------------------------------------------------------------

    /** UTCID16 - Open the edit form (submit = null) for a product that does not exist. Abnormal. */
    @Test
    public void utcid16_openEditFormForNonExistingProduct() throws Exception {
        params.remove("submit");
        params.put("productID", "9999");
        send();
        assertRedirectToList();
        assertDbUnchanged();
    }

    /** UTCID17 - Save changes for product ID 9999 (does not exist). Abnormal. */
    @Test
    public void utcid17_saveNonExistingProductID9999() throws Exception {
        params.put("productID", "9999");
        send();
        assertRedirectToList();
        assertDbUnchanged();
    }

    /** UTCID18 - Save changes for product ID 0 (boundary, does not exist). Boundary. */
    @Test
    public void utcid18_saveNonExistingProductID0() throws Exception {
        params.put("productID", "0");
        send();
        assertRedirectToList();
        assertDbUnchanged();
    }

    // ------------------------------------------------------------------
    // UTCID19 - UTCID21: access control
    // ------------------------------------------------------------------

    /** UTCID19 - Customer tries to save an update. Abnormal. */
    @Test
    public void utcid19_customerCannotSaveUpdate() throws Exception {
        role = "customer";
        send();
        assertRedirectToList();
        assertDbUnchanged();
    }

    /** UTCID20 - Anonymous user (no role in session) tries to save an update. Abnormal. */
    @Test
    public void utcid20_anonymousUserCannotSaveUpdate() throws Exception {
        role = null;
        send();
        assertRedirectToList();
        assertDbUnchanged();
    }

    /** UTCID21 - Customer tries to open the edit form. Abnormal. */
    @Test
    public void utcid21_customerCannotOpenEditForm() throws Exception {
        role = "customer";
        params.remove("submit");
        send();
        assertRedirectToList();
        assertDbUnchanged();
    }

    // ------------------------------------------------------------------
    // Helpers: request / response / session fakes
    // ------------------------------------------------------------------

    /** Default valid request for updateProduct: product 1, admin saves valid data. */
    private Map<String, String> baseParams() {
        Map<String, String> m = new HashMap<>();
        m.put("service", "updateProduct");
        m.put("submit", "Lưu thay đổi");
        m.put("productID", "1");
        m.put("productName", "Cam sanh loai 1");
        m.put("image", "images/cam-sanh-moi.jpg");
        m.put("price", "40000");
        m.put("quantity", "120");
        m.put("categoryID", "CAM");
        m.put("importDate", "2026-09-01");
        m.put("expiryDate", "2026-09-10");
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
            case "getRequestDispatcher" -> {
                outcome.forwardPath = (String) args[0];
                yield fakeDispatcher();
            }
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

    /** Expected: user stays on the edit form. */
    private void assertStaysOnEditForm() {
        assertNoUnhandledException();
        assertNull("Should not redirect away from the edit form", outcome.redirectUrl);
        assertEquals("Should stay on the edit form", EDIT_FORM, outcome.forwardPath);
    }

    /** Expected: product 1 in the database is exactly as it was before the request. */
    private void assertDbUnchanged() throws SQLException {
        assertEquals("Database must not change",
                original.toString(), readProduct(PRODUCT_ID).toString());
    }

    /** Expected: product 1 in the database has exactly these values. */
    private void assertDbRow(String name, String image, double price, int qty, String categoryId,
            String importDate, String expiryDate, int status) throws SQLException {
        Product expected = new Product(PRODUCT_ID, name, image, price, qty, categoryId,
                Date.valueOf(importDate), Date.valueOf(expiryDate), status);
        assertEquals("Product 1 in the database should be updated",
                expected.toString(), readProduct(PRODUCT_ID).toString());
    }

    // ------------------------------------------------------------------
    // Helpers: direct database access (used to read and restore product 1)
    // ------------------------------------------------------------------

    private Product readProduct(int id) throws SQLException {
        String sql = "SELECT productName, image, price, quantity, categoryID, importDate, expiryDate, status "
                + "FROM tblProducts WHERE productID = ?";
        try (PreparedStatement ps = db.conn().prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    return null;
                }
                return new Product(id, rs.getString(1), rs.getString(2), rs.getDouble(3),
                        rs.getInt(4), rs.getString(5), rs.getDate(6), rs.getDate(7), rs.getInt(8));
            }
        }
    }

    private void writeRow(Product p) throws SQLException {
        String sql = "UPDATE tblProducts SET productName = ?, image = ?, price = ?, quantity = ?, "
                + "categoryID = ?, importDate = ?, expiryDate = ?, status = ? WHERE productID = ?";
        try (PreparedStatement ps = db.conn().prepareStatement(sql)) {
            ps.setString(1, p.getProductName());
            ps.setString(2, p.getImage());
            ps.setDouble(3, p.getPrice());
            ps.setInt(4, p.getQuantity());
            ps.setString(5, p.getCategoryID());
            ps.setDate(6, p.getImportDate());
            ps.setDate(7, p.getExpiryDate());
            ps.setInt(8, p.getStatus());
            ps.setInt(9, p.getProductID());
            ps.executeUpdate();
        }
    }
}
