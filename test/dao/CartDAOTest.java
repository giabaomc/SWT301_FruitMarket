
package dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import models.Cart;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class CartDAOTest {

    private CartDAO dao;
    private Connection connection;

    private int productId1;
    private int productId2;
    private int missingProductId;

    private String productName1;
    private double productPrice1;

    private String productName2;
    private double productPrice2;

    @Before
    public void setUp() throws Exception {
        dao = new CartDAO();
        connection = dao.connection;

        assertNotNull(
                "Không kết nối được database. Kiểm tra ConnectDB.properties.",
                connection
        );

        String sql = "SELECT TOP 2 productID, productName, price "
                   + "FROM tblProducts ORDER BY productID";

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            assertTrue("Database phải có ít nhất một sản phẩm.", rs.next());

            productId1 = rs.getInt("productID");
            productName1 = rs.getString("productName");
            productPrice1 = rs.getDouble("price");

            if (rs.next()) {
                productId2 = rs.getInt("productID");
                productName2 = rs.getString("productName");
                productPrice2 = rs.getDouble("price");
            } else {
                productId2 = productId1;
                productName2 = productName1;
                productPrice2 = productPrice1;
            }
        }

        missingProductId = findMissingProductId();
    }

    private int findMissingProductId() throws SQLException {
        String sql = "SELECT MAX(productID) FROM tblProducts";

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            rs.next();
            int maxId = rs.getInt(1);

            if (maxId < Integer.MAX_VALUE) {
                return maxId + 1;
            }
        }

        int candidate = Integer.MIN_VALUE;

        while (productExists(candidate)) {
            candidate++;
        }

        return candidate;
    }

    private boolean productExists(int id) throws SQLException {
        String sql = "SELECT 1 FROM tblProducts WHERE productID = ?";

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    @After
    public void tearDown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    @Test
    public void testUTCID01_GetExistingProduct() {
        Cart result = dao.getCart(productId1);

        assertNotNull(result);
        assertEquals(productId1, result.getProductID());
        assertEquals(productName1, result.getProductName());
        assertEquals(productPrice1, result.getPrice(), 0.001);
    }

    @Test
    public void testUTCID02_GetAnotherExistingProduct() {
        Cart result = dao.getCart(productId2);

        assertNotNull(result);
        assertEquals(productId2, result.getProductID());
        assertEquals(productName2, result.getProductName());
        assertEquals(productPrice2, result.getPrice(), 0.001);
    }

    @Test
    public void testUTCID03_InitialQuantityIsZero() {
        Cart result = dao.getCart(productId1);

        assertNotNull(result);
        assertEquals(0, result.getQuantity());
    }

    @Test
    public void testUTCID04_ProductDoesNotExist() {
        Cart result = dao.getCart(missingProductId);

        assertNull(result);
    }

    @Test
    public void testUTCID05_ProductIdZero() throws SQLException {
        Cart result = dao.getCart(0);

        if (productExists(0)) {
            assertNotNull(result);
            assertEquals(0, result.getProductID());
        } else {
            assertNull(result);
        }
    }

    @Test
    public void testUTCID06_NegativeProductId() throws SQLException {
        Cart result = dao.getCart(-1);

        if (productExists(-1)) {
            assertNotNull(result);
            assertEquals(-1, result.getProductID());
        } else {
            assertNull(result);
        }
    }

    @Test
    public void testUTCID07_VeryLargeProductId() throws SQLException {
        Cart result = dao.getCart(Integer.MAX_VALUE);

        if (productExists(Integer.MAX_VALUE)) {
            assertNotNull(result);
            assertEquals(Integer.MAX_VALUE, result.getProductID());
        } else {
            assertNull(result);
        }
    }

    @Test
    public void testUTCID08_RepeatedCallsReturnCorrectData() {
        Cart first = dao.getCart(productId1);
        Cart second = dao.getCart(productId1);

        assertNotNull(first);
        assertNotNull(second);

        assertEquals(first.getProductID(), second.getProductID());
        assertEquals(first.getProductName(), second.getProductName());
        assertEquals(first.getPrice(), second.getPrice(), 0.001);
        assertEquals(0, first.getQuantity());
        assertEquals(0, second.getQuantity());
    }

    @Test
    public void testUTCID09_DatabaseConnectionClosed() throws Exception {
        connection.close();

        Cart result = dao.getCart(productId1);

        assertNull(result);
    }
}
