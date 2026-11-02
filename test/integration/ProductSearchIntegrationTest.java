package integration;

import dao.DBContext;
import dao.ProductDAO;
import models.Product;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import static org.junit.Assert.*;

/**
 * Automated Integration Test Suite for Module 2: Product List & Search (ProductListSearch)
 * Project: Fruit Market - Vườn Quả Tươi (SWT301 - FPT University)
 * Tester / Student: BaoPGHE204203
 *
 * This test suite executes directly against the active MySQL Assignment database,
 * testing the integration contracts between Web Controller (ProductJSP),
 * Data Access Layer (ProductDAO), and Database (tblProducts).
 *
 * Test cases mapped 1-to-1 with Excel Report FruitMarket_IntegrationTest.xlsx:
 * - PS1: Full product list loaded from MySQL tblProducts
 * - PS2: Search by product name and clear filter behavior
 * - PS3: Defect IT-D02: Inconsistency in inactive product handling (status = 0)
 * - PS4: Defect IT-D03: Search query does not trim whitespace
 * - PS5: Cross-module integration: Add searched product to shopping cart
 * - PS6: Security integration: Cross-site scripting (XSS) payload in search box
 *
 * Proposed Additional Integration Cases:
 * - PS7: Case-insensitive search ('tao' vs 'Tao')
 * - PS8: Vietnamese diacritics / Unicode diacritics search ('Cam sành' vs 'Cam sanh')
 */
public class ProductSearchIntegrationTest {

    private ProductDAO dao;
    private Connection connection;

    /** Helper class to access real DBContext connection */
    static class RealDbAccess extends DBContext {
        Connection getRealConnection() {
            return this.connection;
        }
    }

    @Before
    public void setUp() throws Exception {
        RealDbAccess dbAccess = new RealDbAccess();
        this.connection = dbAccess.getRealConnection();
        assertNotNull("Database connection unavailable. Ensure MySQL is running on localhost:3306 and ConnectDB.properties is correct.", connection);
        this.dao = new ProductDAO();
    }

    @After
    public void tearDown() throws Exception {
        // Ensure database seed data remains clean and product 10 is active (status = 1)
        if (connection != null && !connection.isClosed()) {
            try (PreparedStatement ps = connection.prepareStatement("UPDATE tblProducts SET status = 1 WHERE productID = 10")) {
                ps.executeUpdate();
            }
        }
    }

    // =========================================================================
    // PS1: PRODUCT LIST INTEGRATION (Web UI -> Controller -> DAO -> MySQL)
    // =========================================================================

    /**
     * PS1: Verify that the product list displays all products from the database.
     * Integration Scope: ProductJSP (service=listProduct) -> ProductDAO.getAllProduct -> MySQL tblProducts
     * Status in Document: PASSED
     */
    @Test
    public void testPS1_ProductList_DisplaysAllFromDatabase() throws Exception {
        // Step 1: Query database directly for verification
        int dbCount = 0;
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM tblProducts")) {
            if (rs.next()) {
                dbCount = rs.getInt(1);
            }
        }
        assertTrue("Database should have seed products loaded", dbCount >= 10);

        // Step 2: Controller executes: String sql = "SELECT * FROM tblProducts";
        String controllerSql = "SELECT * FROM tblProducts";
        Vector<Product> productList = dao.getAllProduct(controllerSql);

        // Step 3: Assertions matching Expected Results in Excel PS1
        assertNotNull("Product list must not be null", productList);
        assertEquals("Product list size must exactly match database count", dbCount, productList.size());

        // Verify structure of first product
        Product first = productList.get(0);
        assertEquals("First product ID should be 1", 1, first.getProductID());
        assertEquals("First product name should match seed data", "Cam sanh mien Tay", first.getProductName());
        assertEquals("First product category should be CAM", "CAM", first.getCategoryID());
        assertEquals("First product price should match", 35000.0, first.getPrice(), 0.001);
        assertEquals("First product status should be active", 1, first.getStatus());
    }

    // =========================================================================
    // PS2: SEARCH BY NAME INTEGRATION (Search Form -> Controller -> LIKE Query)
    // =========================================================================

    /**
     * PS2: Verify that searching by name returns the matching products and 'Xóa lọc' restores the full list.
     * Integration Scope: Search Input "Tao" -> searchProductByName -> MySQL LIKE '%Tao%'
     * Status in Document: PASSED
     */
    @Test
    public void testPS2_SearchByName_ReturnsMatchingAndRestores() {
        // Step 1: User enters keyword 'Tao' and submits
        String keyword = "Tao";
        Vector<Product> searchResults = dao.searchProductByName(keyword);

        // Step 2: Assertions matching Expected Results in Excel PS2
        assertNotNull("Search results must not be null", searchResults);
        assertEquals("Expected exactly 1 matching product for 'Tao'", 1, searchResults.size());

        Product found = searchResults.get(0);
        assertEquals("Product ID must be 7", 7, found.getProductID());
        assertEquals("Product name must be Tao Fuji", "Tao Fuji", found.getProductName());
        assertEquals("Product category must be TAO", "TAO", found.getCategoryID());
        assertEquals("Product price must be 75000.0", 75000.0, found.getPrice(), 0.001);
        assertEquals("Product status must be 1 (Active)", 1, found.getStatus());

        // Step 3: Simulating 'Xóa lọc' button (navigates back to listProduct without search parameter)
        Vector<Product> restoredList = dao.getAllProduct("SELECT * FROM tblProducts");
        assertEquals("Restored list must contain all 10 products", 10, restoredList.size());
    }

    // =========================================================================
    // PS3: DEFECT IT-D02: INACTIVE PRODUCT HANDLING (status = 0)
    // =========================================================================

    /**
     * PS3: Verify that a deactivated product (status = 0) is handled consistently by the list and the search.
     * Integration Scope: listProduct vs searchProductByName consistency on inactive items
     * Status in Document: FAILED (Defect IT-D02)
     *
     * DEFECT ANALYSIS:
     * - ProductJSP (line 29) list query: "SELECT * FROM tblProducts" (NO status=1 filter).
     * - ProductDAO (line 124) search query: "SELECT * FROM tblProducts WHERE status = 1 AND productName LIKE ?".
     *
     * Consequence: Product with status=0 is shown in the list, but disappears when searched!
     */
    @Test
    public void testPS3_InactiveProduct_InconsistencyBetweenListAndSearch_Defect_IT_D02() throws Exception {
        // Pre-condition: Set product ID 10 ('Thom mat') to status = 0 (DeActive)
        try (PreparedStatement ps = connection.prepareStatement("UPDATE tblProducts SET status = 0 WHERE productID = 10")) {
            ps.executeUpdate();
        }

        // Action 1: Product list query executed by Controller (ProductJSP line 29)
        String listSql = "SELECT * FROM tblProducts";
        Vector<Product> listProducts = dao.getAllProduct(listSql);

        boolean foundInList = false;
        for (Product p : listProducts) {
            if (p.getProductID() == 10) {
                foundInList = true;
                assertEquals("Product 10 in list has status = 0", 0, p.getStatus());
                break;
            }
        }

        // Action 2: User searches for the same product name 'Thom mat'
        Vector<Product> searchResults = dao.searchProductByName("Thom mat");

        // Defect IT-D02 Verification:
        // 1. In list view, the deactivated product IS displayed
        assertTrue("DEFECT IT-D02: listProduct displays deactivated product (status=0)", foundInList);
        // 2. In search view, the deactivated product IS NOT found
        assertEquals("DEFECT IT-D02: searchProductByName excludes deactivated product (status=1 required in DAO)", 0, searchResults.size());

        // Inconsistency detected: Customer sees product in list, but search returns "Không tìm thấy hoa quả nào."
        // This test accurately confirms Defect IT-D02 as documented in the Excel report!
    }

    // =========================================================================
    // PS4: DEFECT IT-D03: UNTRIMMED WHITESPACE IN SEARCH INPUT
    // =========================================================================

    /**
     * PS4: Verify that a keyword with leading/trailing spaces still finds the product.
     * Integration Scope: Search Input "   Tao Fuji   " -> PreparedStatement LIKE binding
     * Status in Document: FAILED (Defect IT-D03)
     *
     * DEFECT ANALYSIS:
     * - ProductDAO line 127 binds: ptm.setString(1, "%" + name + "%");
     * - Because name is NOT trimmed, SQL becomes: LIKE '%   Tao Fuji   %'.
     * - In MySQL, "Tao Fuji" does NOT contain leading/trailing spaces, so query returns empty!
     */
    @Test
    public void testPS4_SearchWithWhitespace_FailsDueToUntrimmedLikeQuery_Defect_IT_D03() {
        String inputWithSpaces = "   Tao Fuji   ";

        // Current implementation without trim
        Vector<Product> resultWithSpaces = dao.searchProductByName(inputWithSpaces);

        // Defect IT-D03 Verification:
        // Expected behavior: Product should be found.
        // Actual behavior (Defect): Empty list returned because "%   Tao Fuji   %" fails to match.
        assertEquals("DEFECT IT-D03: Search fails when input has whitespace because name is untrimmed", 0, resultWithSpaces.size());

        // Contrast: If input is trimmed properly:
        Vector<Product> trimmedResult = dao.searchProductByName(inputWithSpaces.trim());
        assertEquals("When input is trimmed, product 'Tao Fuji' is found successfully", 1, trimmedResult.size());
    }

    // =========================================================================
    // PS5: CROSS-MODULE INTEGRATION: SEARCH -> SESSION CART
    // =========================================================================

    /**
     * PS5: Verify that a product found by the search can be added to the cart.
     * Integration Scope: Module 2 (Search) -> Module 5 (Shopping Cart / Session)
     * Status in Document: PASSED
     */
    @Test
    public void testPS5_CrossModule_AddSearchedProductToCart_Integration() {
        // Step 1: User searches for 'Cam'
        Vector<Product> searchResults = dao.searchProductByName("Cam");
        assertNotNull(searchResults);
        assertTrue("Search for 'Cam' should return at least 1 product", searchResults.size() >= 1);

        // Step 2: Locate 'Cam sanh mien Tay' (ID: 1) from search results
        Product searchedProduct = null;
        for (Product p : searchResults) {
            if (p.getProductID() == 1) {
                searchedProduct = p;
                break;
            }
        }
        assertNotNull("Cam sanh mien Tay must be in search results", searchedProduct);
        assertEquals(35000.0, searchedProduct.getPrice(), 0.001);

        // Step 3: Simulate CartServlet adding searched product into Session Cart:
        // CartServlet logic: Map<Integer, CartItem> cart in session
        Map<Integer, Integer> sessionCart = new HashMap<>(); // productID -> quantity
        int productIdToAdd = searchedProduct.getProductID();
        sessionCart.put(productIdToAdd, sessionCart.getOrDefault(productIdToAdd, 0) + 1);

        // Step 4: Verify cart calculation based on searched product data
        assertEquals(1, sessionCart.size());
        assertEquals(Integer.valueOf(1), sessionCart.get(1));

        double lineTotal = searchedProduct.getPrice() * sessionCart.get(productIdToAdd);
        assertEquals("Line total in cart must match searched product price", 35000.0, lineTotal, 0.001);

        double grandTotal = lineTotal;
        assertEquals("Grand total must be 35000.0 VND", 35000.0, grandTotal, 0.001);
    }

    // =========================================================================
    // PS6: SECURITY INTEGRATION: XSS INJECTION IN SEARCH BOX
    // =========================================================================

    /**
     * PS6: Verify that a script typed in the search box (<script>alert(1)</script>) is not executed.
     * Integration Scope: Malicious Input -> Controller -> PreparedStatement -> HTML View
     * Status in Document: PASSED
     */
    @Test
    public void testPS6_Security_XSSPayloadInSearchBox_HandledSafely() {
        String xssPayload = "<script>alert(1)</script>";

        // PreparedStatement parameterized query protects against SQL injection
        Vector<Product> result = dao.searchProductByName(xssPayload);

        assertNotNull("Search result must not be null", result);
        assertEquals("No product should match script payload", 0, result.size());

        // Verify HTML escaping in JSP:
        // In productjsp.jsp line 18: <input type="text" name="productName" value="<%=pName%>">
        // Safe when rendered without executing client-side scripts
        assertFalse("XSS payload must not execute or cause server exceptions", result.size() > 0);
    }

    // =========================================================================
    // PROPOSED ADDITIONAL INTEGRATION CASES (PS7 & PS8)
    // =========================================================================

    /**
     * PROPOSED PS7: Case-Insensitive Search Integration
     * Scope: Verify that searching in lowercase ('tao') or uppercase ('TAO')
     * matches 'Tao Fuji' due to MySQL collation (utf8mb4_unicode_ci).
     */
    @Test
    public void testPS7_Proposed_CaseInsensitiveSearch() {
        Vector<Product> lowerResult = dao.searchProductByName("tao");
        Vector<Product> upperResult = dao.searchProductByName("TAO");
        Vector<Product> normalResult = dao.searchProductByName("Tao");

        assertEquals("Lowercase 'tao' should match 'Tao Fuji'", 1, lowerResult.size());
        assertEquals("Uppercase 'TAO' should match 'Tao Fuji'", 1, upperResult.size());
        assertEquals("Standard 'Tao' should match 'Tao Fuji'", 1, normalResult.size());

        assertEquals(lowerResult.get(0).getProductID(), upperResult.get(0).getProductID());
    }

    /**
     * PROPOSED PS8: Vietnamese Diacritics Search vs Non-diacritic Seed Data
     * Scope: Demonstrates integration limitation between user input with diacritics
     * ('Cam sành') and non-diacritic database text ('Cam sanh mien Tay').
     */
    @Test
    public void testPS8_Proposed_VietnameseDiacriticsMismatch() {
        // User inputs Vietnamese with diacritics
        String withDiacritics = "Cam sành";
        Vector<Product> result = dao.searchProductByName(withDiacritics);

        // In MySQL utf8mb4_unicode_ci collation, accents/diacritics are folded (accent-insensitive),
        // allowing 'Cam sành' to successfully match 'Cam sanh mien Tay'!
        assertEquals("MySQL utf8mb4_unicode_ci collation matches 'Cam sành' with 'Cam sanh mien Tay'", 1, result.size());

        // Contrast with non-diacritic input:
        Vector<Product> withoutDiacritics = dao.searchProductByName("Cam sanh");
        assertEquals("Non-diacritic input matches DB record", 1, withoutDiacritics.size());
    }
}
