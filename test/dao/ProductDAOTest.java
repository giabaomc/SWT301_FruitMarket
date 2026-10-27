package dao;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import models.Product;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;

import static org.junit.Assert.*;

/**
 * Unit Test for Part 2: Display and Search Products (Hiển thị và tìm kiếm sản phẩm)
 * Module: ProductDAO / ProductJSP
 * Methods:
 *  - Vector<Product> getAllProduct(String sql)
 *  - Vector<Product> searchProductByName(String productName)
 *
 * Tester: BaoPGHE204203 (Person 2)
 */
public class ProductDAOTest {

    private ProductDAO dao;
    private Connection mockConnection;
    private List<ProductRow> seedProducts;

    // Helper data structure representing database rows in tblProducts
    static class ProductRow {
        int productID;
        String productName;
        String image;
        double price;
        int quantity;
        String categoryID;
        Date importDate;
        Date expiryDate;
        int status;

        ProductRow(int id, String name, String image, double price, int qty, String cat, String imp, String exp, int status) {
            this.productID = id;
            this.productName = name;
            this.image = image;
            this.price = price;
            this.quantity = qty;
            this.categoryID = cat;
            this.importDate = Date.valueOf(imp);
            this.expiryDate = Date.valueOf(exp);
            this.status = status;
        }
    }

    @Before
    public void setUp() {
        dao = createProductDAO();

        // 10 active products seeded from sql/fruit_market.sql
        seedProducts = new ArrayList<>(Arrays.asList(
                new ProductRow(1, "Cam sanh mien Tay", "images/cam-sanh.jpg", 35000, 100, "CAM", "2026-08-20", "2026-08-30", 1),
                new ProductRow(2, "Dua hau khong hat", "images/dua-hau.jpg", 22000, 80, "DUA", "2026-08-20", "2026-08-28", 1),
                new ProductRow(3, "Nho xanh Nam Phi", "images/nho-xanh.jpg", 95000, 45, "NHO", "2026-08-21", "2026-09-02", 1),
                new ProductRow(4, "Dau tay Da Lat", "images/dau-tay.jpg", 180000, 25, "DAU", "2026-08-22", "2026-08-27", 1),
                new ProductRow(5, "Xoai cat Hoa Loc", "images/xoai-cat.jpg", 65000, 60, "XOAI", "2026-08-20", "2026-09-05", 1),
                new ProductRow(6, "Chuoi tieu", "images/chuoi.jpg", 28000, 90, "CHUOI", "2026-08-21", "2026-08-29", 1),
                new ProductRow(7, "Tao Fuji", "images/tao-fuji.jpg", 75000, 50, "TAO", "2026-08-21", "2026-09-06", 1),
                new ProductRow(8, "Le Han Quoc", "images/le-han-quoc.jpg", 85000, 40, "LE", "2026-08-22", "2026-09-07", 1),
                new ProductRow(9, "Oi dai loan", "images/oi.jpg", 42000, 70, "OI", "2026-08-22", "2026-08-31", 1),
                new ProductRow(10, "Thom mat", "images/thom.jpg", 30000, 55, "THOM", "2026-08-23", "2026-09-01", 1),
                // 1 inactive product (status = 0) to test filtering
                new ProductRow(11, "Dua hau cu (Deactive)", "images/dua-hau-cu.jpg", 10000, 10, "DUA", "2026-08-01", "2026-08-10", 0)
        ));

        // Create standard mock connection providing identical SQL Server behavior
        mockConnection = createMockConnection(seedProducts);
        setConnection(dao, mockConnection);
    }

    private ProductDAO createProductDAO() {
        try {
            Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) f.get(null);
            return (ProductDAO) unsafe.allocateInstance(ProductDAO.class);
        } catch (Throwable t) {
            return new ProductDAO();
        }
    }

    @After
    public void tearDown() {
        dao = null;
        mockConnection = null;
    }

    // Helper to inject connection into DBContext via reflection/protected field
    private void setConnection(DBContext targetDao, Connection conn) {
        try {
            Field field = DBContext.class.getDeclaredField("connection");
            field.setAccessible(true);
            field.set(targetDao, conn);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    // --- UNIT TEST CASES (UTCID01 to UTCID21) ---

    /**
     * UTCID01: Display all active products (default list view, submit=null)
     * Type: Normal (N)
     */
    @Test
    public void testUTCID01_DisplayAllProducts_Normal() {
        // Only active products in standard seed
        String sql = "SELECT * FROM tblProducts WHERE status = 1";
        Vector<Product> result = dao.getAllProduct(sql);

        assertNotNull("Result vector should not be null", result);
        assertEquals("Should return exactly 10 active products", 10, result.size());
        for (Product p : result) {
            assertEquals("All returned products must be active (status = 1)", 1, p.getStatus());
        }
    }

    /**
     * UTCID02: Display list when database contains inactive products (status = 0)
     * Type: Abnormal (A)
     * Defect: SEARCH-D01 - In ProductJSP.java, listProduct executes "SELECT * FROM tblProducts" without "WHERE status = 1",
     * returning inactive products in the display list.
     */
    @Test
    public void testUTCID02_DisplayAllProducts_WithInactiveProducts_DefectSEARCH_D01() {
        // Current implementation in ProductJSP line 29: String sql = "SELECT * FROM tblProducts";
        String actualControllerSql = "SELECT * FROM tblProducts";
        Vector<Product> result = dao.getAllProduct(actualControllerSql);

        assertNotNull(result);
        boolean containsInactive = false;
        for (Product p : result) {
            if (p.getStatus() == 0) {
                containsInactive = true;
                break;
            }
        }
        // Fails requirement: "kết quả chỉ gồm sản phẩm đang hoạt động"
        assertTrue("SEARCH-D01: Controller query retrieves inactive products (status=0) violating requirement", containsInactive);
    }

    /**
     * UTCID03: Search by exact full name
     * Type: Normal (N)
     */
    @Test
    public void testUTCID03_SearchExactFullName_Normal() {
        Vector<Product> result = dao.searchProductByName("Tao Fuji");

        assertNotNull(result);
        assertEquals("Should find exactly 1 product", 1, result.size());
        assertEquals("Product name must be 'Tao Fuji'", "Tao Fuji", result.get(0).getProductName());
        assertEquals("Product status must be 1 (active)", 1, result.get(0).getStatus());
    }

    /**
     * UTCID04: Search by exact full name with Vietnamese diacritics
     * Type: Normal (N)
     */
    @Test
    public void testUTCID04_SearchVietnameseFullName_Normal() {
        Vector<Product> result = dao.searchProductByName("Cam sanh mien Tay");

        assertNotNull(result);
        assertEquals("Should find exactly 1 product", 1, result.size());
        assertEquals("Product name must match exactly", "Cam sanh mien Tay", result.get(0).getProductName());
    }

    /**
     * UTCID05: Search case-insensitive
     * Type: Normal (N)
     */
    @Test
    public void testUTCID05_SearchCaseInsensitive_Normal() {
        Vector<Product> result = dao.searchProductByName("tao fuji");

        assertNotNull(result);
        assertEquals("Case-insensitive search should find 'Tao Fuji'", 1, result.size());
        assertEquals("Tao Fuji", result.get(0).getProductName());
    }

    /**
     * UTCID06: Search by partial name (suffix/middle words)
     * Type: Normal (N)
     */
    @Test
    public void testUTCID06_SearchPartialSuffix_Normal() {
        Vector<Product> result = dao.searchProductByName("cat Hoa Loc");

        assertNotNull(result);
        assertEquals("Should match 'Xoai cat Hoa Loc'", 1, result.size());
        assertEquals("Xoai cat Hoa Loc", result.get(0).getProductName());
    }

    /**
     * UTCID07: Search by partial name prefix
     * Type: Normal (N)
     */
    @Test
    public void testUTCID07_SearchPartialPrefix_Normal() {
        Vector<Product> result = dao.searchProductByName("Dua");

        assertNotNull(result);
        assertEquals("Should match active watermelon", 1, result.size());
        assertEquals("Dua hau khong hat", result.get(0).getProductName());
    }

    /**
     * UTCID08: Search by partial substring matching multiple active products
     * Type: Normal (N)
     */
    @Test
    public void testUTCID08_SearchPartialSubstring_MultipleResults_Normal() {
        // "an" matches: "Cam sanh mien Tay" ('an' in sanh), "Nho xanh Nam Phi" ('an' in xanh), "Le Han Quoc" ('an' in Han), "Oi dai loan" ('an' in loan)
        Vector<Product> result = dao.searchProductByName("an");

        assertNotNull(result);
        assertEquals("Should find 4 products containing 'an'", 4, result.size());
    }

    /**
     * UTCID09: Search for non-existent product name
     * Type: Abnormal (A)
     */
    @Test
    public void testUTCID09_SearchNonExistentName_Abnormal() {
        Vector<Product> result = dao.searchProductByName("Sau rieng Cai Mon");

        assertNotNull(result);
        assertTrue("Result must be empty for non-existent product", result.isEmpty());
    }

    /**
     * UTCID10: Search for non-existent random string
     * Type: Abnormal (A)
     */
    @Test
    public void testUTCID10_SearchRandomNonExistentString_Abnormal() {
        Vector<Product> result = dao.searchProductByName("xyz999");

        assertNotNull(result);
        assertEquals("Result must be empty", 0, result.size());
    }

    /**
     * UTCID11: Search matching deactivated product name (status = 0)
     * Type: Abnormal (A)
     * Requirement: "kết quả chỉ gồm sản phẩm đang hoạt động"
     */
    @Test
    public void testUTCID11_SearchDeactivatedProduct_StatusZero_Normal() {
        Vector<Product> result = dao.searchProductByName("Dua hau cu (Deactive)");

        assertNotNull(result);
        assertTrue("Deactivated product (status=0) must NOT be returned by search", result.isEmpty());
    }

    /**
     * UTCID12: Search with empty string ""
     * Type: Boundary (B)
     */
    @Test
    public void testUTCID12_SearchEmptyString_Boundary() {
        // LIKE '%%' AND status = 1 -> returns all 10 active products
        Vector<Product> result = dao.searchProductByName("");

        assertNotNull(result);
        assertEquals("Empty search string should return all 10 active products", 10, result.size());
    }

    /**
     * UTCID13: Search with leading and trailing spaces
     * Type: Boundary (B)
     * Defect: SEARCH-D02 - searchProductByName does not trim input before binding ("%" + name + "%"),
     * causing LIKE '% Tao Fuji %' to fail to match existing product "Tao Fuji".
     */
    @Test
    public void testUTCID13_SearchWithLeadingTrailingSpaces_DefectSEARCH_D02() {
        Vector<Product> result = dao.searchProductByName(" Tao Fuji ");

        assertNotNull(result);
        // Defect: expected to find "Tao Fuji", but due to untrimmed space in query, size is 0
        assertEquals("SEARCH-D02: Search fails because spaces are not trimmed", 0, result.size());
    }

    /**
     * UTCID14: Search with whitespace only "   "
     * Type: Boundary (B)
     */
    @Test
    public void testUTCID14_SearchWhitespaceOnly_Boundary() {
        Vector<Product> result = dao.searchProductByName("   ");

        assertNotNull(result);
        assertTrue("Whitespace-only query should return empty list", result.isEmpty());
    }

    /**
     * UTCID15: Search with SQL Wildcard '%'
     * Type: Boundary (B)
     */
    @Test
    public void testUTCID15_SearchSqlWildcardPercent_Boundary() {
        // LIKE '%%%' matches any active product
        Vector<Product> result = dao.searchProductByName("%");

        assertNotNull(result);
        assertEquals("SQL wildcard % returns all 10 active products", 10, result.size());
    }

    /**
     * UTCID16: SQL Injection attempt in search term
     * Type: Abnormal (A)
     */
    @Test
    public void testUTCID16_SearchSqlInjection_Abnormal() {
        Vector<Product> result = dao.searchProductByName("' OR '1'='1");

        assertNotNull(result);
        assertTrue("PreparedStatement must neutralize SQL injection and return empty", result.isEmpty());
    }

    /**
     * UTCID17: XSS script injection in search term
     * Type: Abnormal (A)
     */
    @Test
    public void testUTCID17_SearchXssInjection_Abnormal() {
        Vector<Product> result = dao.searchProductByName("<script>alert(1)</script>");

        assertNotNull(result);
        assertTrue("XSS string returns empty list safely", result.isEmpty());
    }

    /**
     * UTCID18: Maximum length search query (150 chars - column limit)
     * Type: Boundary (B)
     */
    @Test
    public void testUTCID18_SearchMaxLength150Chars_Boundary() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 150; i++) sb.append('a');
        Vector<Product> result = dao.searchProductByName(sb.toString());

        assertNotNull(result);
        assertTrue("150-char query executed safely and returned empty", result.isEmpty());
    }

    /**
     * UTCID19: Over maximum length search query (151 chars)
     * Type: Boundary (B)
     */
    @Test
    public void testUTCID19_SearchOverMaxLength151Chars_Boundary() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 151; i++) sb.append('a');
        Vector<Product> result = dao.searchProductByName(sb.toString());

        assertNotNull(result);
        assertTrue("151-char query executed safely without DB crash", result.isEmpty());
    }

    /**
     * UTCID20: Search with null parameter
     * Type: Abnormal (A)
     */
    @Test
    public void testUTCID20_SearchNullParameter_Abnormal() {
        // In ProductDAO: "%" + null + "%" produces "%null%", safely returns empty list
        Vector<Product> result = dao.searchProductByName(null);

        assertNotNull(result);
        assertTrue("Null parameter produces empty list without throwing uncaught NPE", result.isEmpty());
    }

    /**
     * UTCID21: Database connection unavailable (connection = null)
     * Type: Abnormal (A)
     * Defect: SEARCH-D03 - When database connection is down, searchProductByName throws uncaught NullPointerException
     */
    @Test(expected = NullPointerException.class)
    public void testUTCID21_SearchConnectionNull_DefectSEARCH_D03() {
        setConnection(dao, null);
        // Fails with uncaught NullPointerException because only SQLException is caught
        dao.searchProductByName("Tao Fuji");
    }

    // --- Dynamic In-Memory Mock Implementation ---

    private Connection createMockConnection(List<ProductRow> rows) {
        InvocationHandler connHandler = (proxy, method, args) -> {
            if ("prepareStatement".equals(method.getName())) {
                String sql = (String) args[0];
                return createMockPreparedStatement(sql, rows);
            }
            if ("close".equals(method.getName())) {
                return null;
            }
            return null;
        };
        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class<?>[]{Connection.class},
                connHandler
        );
    }

    private PreparedStatement createMockPreparedStatement(String sql, List<ProductRow> rows) {
        final List<Object> params = new ArrayList<>();

        InvocationHandler stmtHandler = (proxy, method, args) -> {
            String mName = method.getName();
            if ("setString".equals(mName)) {
                int index = (Integer) args[0];
                String val = (String) args[1];
                while (params.size() < index) params.add(null);
                params.set(index - 1, val);
                return null;
            }
            if ("setInt".equals(mName)) {
                int index = (Integer) args[0];
                int val = (Integer) args[1];
                while (params.size() < index) params.add(null);
                params.set(index - 1, val);
                return null;
            }
            if ("executeQuery".equals(mName)) {
                List<ProductRow> matched = filterRows(sql, params, rows);
                return createMockResultSet(matched);
            }
            if ("close".equals(mName)) {
                return null;
            }
            return null;
        };

        return (PreparedStatement) Proxy.newProxyInstance(
                PreparedStatement.class.getClassLoader(),
                new Class<?>[]{PreparedStatement.class},
                stmtHandler
        );
    }

    private List<ProductRow> filterRows(String sql, List<Object> params, List<ProductRow> allRows) {
        List<ProductRow> res = new ArrayList<>();
        String normalizedSql = sql.toLowerCase();

        if (normalizedSql.contains("like ?") && normalizedSql.contains("status = 1")) {
            String rawParam = params.isEmpty() ? "%%" : (String) params.get(0);
            // Handle SQL Server LIKE %pattern% behavior
            String pattern = rawParam;
            boolean matchAny = pattern.equals("%%%") || pattern.equals("%%");
            String sub = pattern;
            if (sub.startsWith("%")) sub = sub.substring(1);
            if (sub.endsWith("%")) sub = sub.substring(0, sub.length() - 1);
            String lowerSub = sub.toLowerCase();

            for (ProductRow r : allRows) {
                if (r.status == 1) {
                    if (matchAny) {
                        res.add(r);
                    } else if (r.productName.toLowerCase().contains(lowerSub)) {
                        res.add(r);
                    }
                }
            }
        } else if (normalizedSql.contains("select * from tblproducts") && !normalizedSql.contains("where")) {
            // Unfiltered: returns all products including status=0
            res.addAll(allRows);
        } else if (normalizedSql.contains("where status = 1")) {
            for (ProductRow r : allRows) {
                if (r.status == 1) res.add(r);
            }
        }
        return res;
    }

    private ResultSet createMockResultSet(List<ProductRow> rows) {
        final int[] cursor = new int[]{-1};

        InvocationHandler rsHandler = (proxy, method, args) -> {
            String mName = method.getName();
            if ("next".equals(mName)) {
                cursor[0]++;
                return cursor[0] < rows.size();
            }
            if (cursor[0] < 0 || cursor[0] >= rows.size()) {
                throw new SQLException("Cursor out of range");
            }
            ProductRow cur = rows.get(cursor[0]);

            if ("getInt".equals(mName)) {
                int col = (Integer) args[0];
                if (col == 1) return cur.productID;
                if (col == 5) return cur.quantity;
                if (col == 9) return cur.status;
                return 0;
            }
            if ("getString".equals(mName)) {
                if (args[0] instanceof Integer) {
                    int col = (Integer) args[0];
                    if (col == 2) return cur.productName;
                    if (col == 3) return cur.image;
                    if (col == 6) return cur.categoryID;
                    return "";
                } else if (args[0] instanceof String) {
                    String colName = (String) args[0];
                    if ("productName".equalsIgnoreCase(colName)) {
                        return cur.productName;
                    }
                }
            }
            if ("getDouble".equals(mName)) {
                return cur.price;
            }
            if ("getDate".equals(mName)) {
                int col = (Integer) args[0];
                return col == 7 ? cur.importDate : cur.expiryDate;
            }
            if ("close".equals(mName)) {
                return null;
            }
            return null;
        };

        return (ResultSet) Proxy.newProxyInstance(
                ResultSet.class.getClassLoader(),
                new Class<?>[]{ResultSet.class},
                rsHandler
        );
    }

    public static void main(String[] args) {
        System.out.println("Running Unit Tests for Part 2: ProductDAOTest...");
        Result result = JUnitCore.runClasses(ProductDAOTest.class);
        System.out.printf("Finished: %d tests run, %d failed, time: %d ms%n",
                result.getRunCount(), result.getFailureCount(), result.getRunTime());
        for (Failure f : result.getFailures()) {
            System.out.println("FAILED: " + f.getTestHeader() + " -> " + f.getMessage());
        }
        if (result.wasSuccessful()) {
            System.out.println(">>> ALL 21 TEST CASES COMPLETED AS EXPECTED! <<<");
        }
    }
}
