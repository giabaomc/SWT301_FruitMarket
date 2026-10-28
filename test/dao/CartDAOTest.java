package dao;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;

import models.Cart;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class CartDAOTest {

    private ClassLoader loader;
    private Object dao;
    private Method getCartMethod;
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
        loader = CartTestSupport.newLoader();
        dao = CartTestSupport.newCartDao(loader);
        getCartMethod = dao.getClass().getMethod("getCart", int.class);
        connection = CartTestSupport.connectionOf(loader, dao);

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

    private Cart getCart(int productId) {
        try {
            return (Cart) getCartMethod.invoke(dao, productId);
        } catch (InvocationTargetException ex) {
            throw new AssertionError("CartDAO.getCart(" + productId + ") ném exception: " + ex.getCause(),
                    ex.getCause());
        } catch (IllegalAccessException ex) {
            throw new IllegalStateException(ex);
        }
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
        Cart result = getCart(productId1);

        assertNotNull(result);
        assertEquals(productId1, result.getProductID());
        assertEquals(productName1, result.getProductName());
        assertEquals(productPrice1, result.getPrice(), 0.001);
    }

    @Test
    public void testUTCID02_GetAnotherExistingProduct() {
        Cart result = getCart(productId2);

        assertNotNull(result);
        assertEquals(productId2, result.getProductID());
        assertEquals(productName2, result.getProductName());
        assertEquals(productPrice2, result.getPrice(), 0.001);
    }

    @Test
    public void testUTCID03_InitialQuantityIsZero() {
        Cart result = getCart(productId1);

        assertNotNull(result);
        assertEquals(0, result.getQuantity());
    }

    @Test
    public void testUTCID04_ProductDoesNotExist() {
        Cart result = getCart(missingProductId);

        assertNull(result);
    }

    @Test
    public void testUTCID05_ProductIdZero() throws SQLException {
        Cart result = getCart(0);

        if (productExists(0)) {
            assertNotNull(result);
            assertEquals(0, result.getProductID());
        } else {
            assertNull(result);
        }
    }

    @Test
    public void testUTCID06_NegativeProductId() throws SQLException {
        Cart result = getCart(-1);

        if (productExists(-1)) {
            assertNotNull(result);
            assertEquals(-1, result.getProductID());
        } else {
            assertNull(result);
        }
    }

    @Test
    public void testUTCID07_VeryLargeProductId() throws SQLException {
        Cart result = getCart(Integer.MAX_VALUE);

        if (productExists(Integer.MAX_VALUE)) {
            assertNotNull(result);
            assertEquals(Integer.MAX_VALUE, result.getProductID());
        } else {
            assertNull(result);
        }
    }

    @Test
    public void testUTCID08_RepeatedCallsReturnCorrectData() {
        Cart first = getCart(productId1);
        Cart second = getCart(productId1);

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

        Cart result = getCart(productId1);

        assertNull(result);
    }

    private static final class CartTestSupport {

        static final String CONFIG_RESOURCE = "../ConnectDB.properties";

        static final Set<String> ISOLATED = Set.of(
                "dao.DBContext",
                "dao.CartDAO");

        static byte[] realConfig() {
            String path = System.getProperty("configFile", "web/WEB-INF/ConnectDB.properties");
            try {
                return Files.readAllBytes(Path.of(path));
            } catch (IOException ex) {
                throw new UncheckedIOException("Cannot read DB config " + Path.of(path).toAbsolutePath(), ex);
            }
        }

        static ClassLoader newLoader() {
            return new IsolatedLoader(CartDAOTest.class.getClassLoader(), realConfig());
        }

        static Object newCartDao(ClassLoader loader) throws Exception {
            return create(loader, "dao.CartDAO");
        }

        static Connection connectionOf(ClassLoader loader, Object dbContext) throws Exception {
            Field field = Class.forName("dao.DBContext", true, loader).getDeclaredField("connection");
            field.setAccessible(true);
            return (Connection) field.get(dbContext);
        }

        private static Object create(ClassLoader loader, String className) throws Exception {
            try {
                return Class.forName(className, true, loader).getDeclaredConstructor().newInstance();
            } catch (InvocationTargetException ex) {
                Throwable cause = ex.getCause();
                if (cause instanceof Exception) {
                    throw (Exception) cause;
                }
                if (cause instanceof Error) {
                    throw (Error) cause;
                }
                throw ex;
            }
        }

        private static boolean isIsolated(String name) {
            for (String isolated : ISOLATED) {
                if (name.equals(isolated) || name.startsWith(isolated + "$")) {
                    return true;
                }
            }
            return false;
        }

        private static final class IsolatedLoader extends ClassLoader {

            private final byte[] config;

            IsolatedLoader(ClassLoader parent, byte[] config) {
                super(parent);
                this.config = config;
            }

            @Override
            protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
                if (!isIsolated(name)) {
                    return super.loadClass(name, resolve);
                }
                synchronized (getClassLoadingLock(name)) {
                    Class<?> c = findLoadedClass(name);
                    if (c == null) {
                        try (InputStream in = getParent().getResourceAsStream(name.replace('.', '/') + ".class")) {
                            if (in == null) {
                                throw new ClassNotFoundException(name);
                            }
                            byte[] bytes = in.readAllBytes();
                            c = defineClass(name, bytes, 0, bytes.length);
                        } catch (IOException ex) {
                            throw new ClassNotFoundException(name, ex);
                        }
                    }
                    if (resolve) {
                        resolveClass(c);
                    }
                    return c;
                }
            }

            @Override
            public InputStream getResourceAsStream(String name) {
                if (CONFIG_RESOURCE.equals(name)) {
                    return new ByteArrayInputStream(config);
                }
                return super.getResourceAsStream(name);
            }
        }
    }
}
