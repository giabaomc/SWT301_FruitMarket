package controllers;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import models.Cart;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class CartServletTest {

    private ClassLoader loader;
    private Connection connection;
    private int productId1;
    private int productId2;
    private int missingProductId;

    private static class TestOutcome {
        String redirectUrl;
        String forwardPath;
        Throwable exception;
        Map<String, Object> requestAttributes = new HashMap<>();
    }

    private static class FakeSession {
        final Map<String, Object> attributes = new HashMap<>();
    }

    @Before
    public void setUp() throws Exception {
        loader = CartTestSupport.newLoader();
        connection = CartTestSupport.connectionOf(loader, CartTestSupport.newDbContext(loader));

        assertNotNull(
                "Database connection unavailable. Check ConnectDB.properties.",
                connection
        );

        List<Integer> ids = new ArrayList<>();

        String sql = "SELECT TOP 2 productID "
                + "FROM tblProducts ORDER BY productID";

        try (PreparedStatement ps = connection.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                ids.add(rs.getInt("productID"));
            }
        }

        assertTrue(
                "The database must contain at least two products.",
                ids.size() >= 2
        );

        productId1 = ids.get(0);
        productId2 = ids.get(1);
        missingProductId = findMissingProductId();
    }

    @After
    public void tearDown() throws Exception {
        if (connection != null && !connection.isClosed()) {
            connection.close();
        }
    }

    private int findMissingProductId() throws SQLException {
        int candidate = 999999;

        String sql = "SELECT productID FROM tblProducts "
                + "WHERE productID = ?";

        while (true) {
            boolean exists;

            try (PreparedStatement ps = connection.prepareStatement(sql)) {

                ps.setInt(1, candidate);

                try (ResultSet rs = ps.executeQuery()) {
                    exists = rs.next();
                }
            }

            if (!exists) {
                return candidate;
            }

            candidate--;
        }
    }

    private TestOutcome execute(
            String service,
            String productID,
            FakeSession session,
            boolean usePost) {

        Map<String, String> parameters = new HashMap<>();

        if (service != null) {
            parameters.put("service", service);
        }

        if (productID != null) {
            parameters.put("productID", productID);
        }

        return execute(parameters, session, usePost);
    }

    private TestOutcome execute(
            Map<String, String> parameters,
            FakeSession session,
            boolean usePost) {

        TestOutcome outcome = new TestOutcome();

        HttpSession httpSession = createSession(session);
        HttpServletRequest request = createRequest(
                parameters, httpSession, outcome, usePost ? "POST" : "GET");
        HttpServletResponse response = createResponse(outcome);

        HttpServlet servlet;

        try {
            servlet = CartTestSupport.newCartServlet(loader);
        } catch (Exception ex) {
            throw new IllegalStateException("Cannot create CartServlet", ex);
        }

        try {

            servlet.service((ServletRequest) request, (ServletResponse) response);
        } catch (Throwable ex) {
            outcome.exception = ex;
        }

        return outcome;
    }

    private HttpSession createSession(FakeSession session) {
        return (HttpSession) Proxy.newProxyInstance(
                HttpSession.class.getClassLoader(),
                new Class<?>[]{HttpSession.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getAttribute":
                            return session.attributes.get(args[0]);

                        case "setAttribute":
                            session.attributes.put(
                                    (String) args[0], args[1]);
                            return null;

                        case "removeAttribute":
                            session.attributes.remove(args[0]);
                            return null;

                        case "getAttributeNames":
                            return Collections.enumeration(
                                    new ArrayList<>(
                                            session.attributes.keySet()));

                        default:
                            return defaultValue(method.getReturnType());
                    }
                }
        );
    }

    private HttpServletRequest createRequest(
            Map<String, String> parameters,
            HttpSession session,
            TestOutcome outcome,
            String httpMethod) {

        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
                        case "getMethod":
                            return httpMethod;

                        case "getParameter":
                            return parameters.get(args[0]);

                        case "getSession":
                            return session;

                        case "getRequestDispatcher":
                            outcome.forwardPath = (String) args[0];

                            return (RequestDispatcher) Proxy.newProxyInstance(
                                    RequestDispatcher.class.getClassLoader(),
                                    new Class<?>[]{RequestDispatcher.class},
                                    (p, m, a) -> defaultValue(
                                            m.getReturnType())
                            );

                        case "setAttribute":
                            outcome.requestAttributes.put(
                                    (String) args[0], args[1]);
                            return null;

                        default:
                            return defaultValue(method.getReturnType());
                    }
                }
        );
    }

    private HttpServletResponse createResponse(TestOutcome outcome) {
        return (HttpServletResponse) Proxy.newProxyInstance(
                HttpServletResponse.class.getClassLoader(),
                new Class<?>[]{HttpServletResponse.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("sendRedirect")) {
                        outcome.redirectUrl = (String) args[0];
                    }

                    return defaultValue(method.getReturnType());
                }
        );
    }

    private static Object defaultValue(Class<?> type) {
        if (type == boolean.class) return false;
        if (type == int.class) return 0;
        if (type == long.class) return 0L;
        if (type == double.class) return 0.0;
        if (type == float.class) return 0.0f;
        if (type == short.class) return (short) 0;
        if (type == byte.class) return (byte) 0;
        if (type == char.class) return '\0';
        return null;
    }

    private static void assertNullPointerFromCartServlet(TestOutcome outcome) {
        assertTrue(
                "Current code is expected to throw NullPointerException "
                + "when DAO returns null, but got: " + outcome.exception,
                outcome.exception instanceof NullPointerException
        );

        StackTraceElement[] trace = outcome.exception.getStackTrace();

        assertTrue("NullPointerException has no stack trace.", trace.length > 0);
        assertEquals(
                "NullPointerException must be thrown inside CartServlet.",
                "controllers.CartServlet",
                trace[0].getClassName()
        );
    }

    @Test
    public void UTCID01_addValidProductFirstTime() {
        FakeSession session = new FakeSession();

        TestOutcome outcome = execute(
                "add2cart",
                String.valueOf(productId1),
                session,
                false
        );

        assertNull("Unexpected exception: " + outcome.exception,
                outcome.exception);
        assertEquals("productjsp", outcome.redirectUrl);

        Cart cart = (Cart) session.attributes.get(
                String.valueOf(productId1));

        assertNotNull(cart);
        assertEquals(productId1, cart.getProductID());
        assertEquals(1, cart.getQuantity());
    }

    @Test
    public void UTCID02_addSameProductAgain() {
        FakeSession session = new FakeSession();

        Cart existing = new Cart(
                productId1, "Existing product", 10.0, 1);

        session.attributes.put(String.valueOf(productId1), existing);

        TestOutcome outcome = execute(
                "add2cart",
                String.valueOf(productId1),
                session,
                false
        );

        assertNull("Unexpected exception: " + outcome.exception,
                outcome.exception);
        assertEquals("productjsp", outcome.redirectUrl);
        assertEquals(2, existing.getQuantity());
    }

    @Test
    public void UTCID03_addDifferentProducts() {
        FakeSession session = new FakeSession();

        TestOutcome first = execute(
                "add2cart",
                String.valueOf(productId1),
                session,
                false
        );

        TestOutcome second = execute(
                "add2cart",
                String.valueOf(productId2),
                session,
                false
        );

        assertNull(first.exception);
        assertNull(second.exception);
        assertEquals("productjsp", second.redirectUrl);

        Cart cart1 = (Cart) session.attributes.get(
                String.valueOf(productId1));
        Cart cart2 = (Cart) session.attributes.get(
                String.valueOf(productId2));

        assertNotNull(cart1);
        assertNotNull(cart2);
        assertEquals(1, cart1.getQuantity());
        assertEquals(1, cart2.getQuantity());
    }

    @Test
    public void UTCID04_addNonExistingProduct() {
        FakeSession session = new FakeSession();

        TestOutcome outcome = execute(
                "add2cart",
                String.valueOf(missingProductId),
                session,
                false
        );

        assertNullPointerFromCartServlet(outcome);
    }

    @Test
    public void UTCID05_missingProductID() {
        FakeSession session = new FakeSession();

        TestOutcome outcome = execute(
                "add2cart",
                null,
                session,
                false
        );

        assertTrue(
                "Current code is expected to throw NumberFormatException, "
                + "but got: " + outcome.exception,
                outcome.exception instanceof NumberFormatException
        );
    }

    @Test
    public void UTCID06_nonNumericProductID() {
        FakeSession session = new FakeSession();

        TestOutcome outcome = execute(
                "add2cart",
                "abc",
                session,
                false
        );

        assertTrue(
                "Current code is expected to throw NumberFormatException, "
                + "but got: " + outcome.exception,
                outcome.exception instanceof NumberFormatException
        );
    }

    @Test
    public void UTCID07_zeroProductID() {
        FakeSession session = new FakeSession();

        TestOutcome outcome = execute(
                "add2cart",
                "0",
                session,
                false
        );

        assertNullPointerFromCartServlet(outcome);
    }

    @Test
    public void UTCID08_negativeProductID() {
        FakeSession session = new FakeSession();

        TestOutcome outcome = execute(
                "add2cart",
                "-1",
                session,
                false
        );

        assertNullPointerFromCartServlet(outcome);
    }

    @Test
    public void UTCID09_addValidProductUsingPost() {
        FakeSession session = new FakeSession();

        TestOutcome outcome = execute(
                "add2cart",
                String.valueOf(productId1),
                session,
                true
        );

        assertNull("Unexpected exception: " + outcome.exception,
                outcome.exception);
        assertEquals("productjsp", outcome.redirectUrl);

        Cart cart = (Cart) session.attributes.get(
                String.valueOf(productId1));

        assertNotNull(cart);
        assertEquals(productId1, cart.getProductID());
        assertEquals(1, cart.getQuantity());
    }

    @Test
    public void UTCID10_showCartWhenEmpty() {
        FakeSession session = new FakeSession();

        TestOutcome outcome = execute(
                "showCart",
                null,
                session,
                false
        );

        assertNull("Unexpected exception: " + outcome.exception,
                outcome.exception);
        assertEquals("jsp/cartjsp.jsp", outcome.forwardPath);
        assertNull(outcome.redirectUrl);

        Vector<?> vector = (Vector<?>) outcome.requestAttributes.get("vector");

        assertNotNull(vector);
        assertTrue(vector.isEmpty());
    }

    @Test
    public void UTCID11_showCartIsDefaultService() {
        FakeSession session = new FakeSession();

        Cart cart1 = new Cart(productId1, "Product 1", 10.0, 2);
        Cart cart2 = new Cart(productId2, "Product 2", 20.0, 1);

        session.attributes.put("email", "customer@vuonqua.local");
        session.attributes.put(String.valueOf(productId1), cart1);
        session.attributes.put(String.valueOf(productId2), cart2);

        TestOutcome outcome = execute(
                null,
                null,
                session,
                false
        );

        assertNull("Unexpected exception: " + outcome.exception,
                outcome.exception);
        assertEquals("jsp/cartjsp.jsp", outcome.forwardPath);

        Vector<?> vector = (Vector<?>) outcome.requestAttributes.get("vector");

        assertNotNull(vector);
        assertEquals(2, vector.size());
        assertTrue(vector.contains(cart1));
        assertTrue(vector.contains(cart2));
    }

    @Test
    public void UTCID12_removeExistingProduct() {
        FakeSession session = new FakeSession();

        session.attributes.put(String.valueOf(productId1),
                new Cart(productId1, "Product 1", 10.0, 1));
        session.attributes.put(String.valueOf(productId2),
                new Cart(productId2, "Product 2", 20.0, 1));

        Map<String, String> parameters = new HashMap<>();
        parameters.put("service", "remove");
        parameters.put("productId", String.valueOf(productId1));

        TestOutcome outcome = execute(parameters, session, false);

        assertNull("Unexpected exception: " + outcome.exception,
                outcome.exception);
        assertEquals("cart", outcome.redirectUrl);
        assertFalse(session.attributes.containsKey(String.valueOf(productId1)));
        assertTrue(session.attributes.containsKey(String.valueOf(productId2)));
    }

    @Test
    public void UTCID13_removeAllKeepsEmail() {
        FakeSession session = new FakeSession();

        session.attributes.put("email", "customer@vuonqua.local");
        session.attributes.put(String.valueOf(productId1),
                new Cart(productId1, "Product 1", 10.0, 1));
        session.attributes.put(String.valueOf(productId2),
                new Cart(productId2, "Product 2", 20.0, 1));

        TestOutcome outcome = execute(
                "removeAll",
                null,
                session,
                false
        );

        assertNull("Unexpected exception: " + outcome.exception,
                outcome.exception);
        assertEquals("cart", outcome.redirectUrl);
        assertEquals(1, session.attributes.size());
        assertTrue(session.attributes.containsKey("email"));
    }

    @Test
    public void UTCID14_showCartAfterLoginThrowsClassCastException() {
        FakeSession session = new FakeSession();

        session.attributes.put("email", "customer@vuonqua.local");
        session.attributes.put("role", "customer");
        session.attributes.put(String.valueOf(productId1),
                new Cart(productId1, "Product 1", 10.0, 1));

        TestOutcome outcome = execute(
                "showCart",
                null,
                session,
                false
        );

        assertTrue(
                "Current code is expected to throw ClassCastException "
                + "because the 'role' attribute is not a Cart, but got: "
                + outcome.exception,
                outcome.exception instanceof ClassCastException
        );
        assertNull(outcome.forwardPath);
    }

    private static final class CartTestSupport {

        static final String CONFIG_RESOURCE = "../ConnectDB.properties";

        static final Set<String> ISOLATED = Set.of(
                "dao.DBContext",
                "dao.CartDAO",
                "controllers.CartServlet");

        static byte[] realConfig() {
            String path = System.getProperty("configFile", "web/WEB-INF/ConnectDB.properties");
            try {
                return Files.readAllBytes(Path.of(path));
            } catch (IOException ex) {
                throw new UncheckedIOException("Cannot read DB config " + Path.of(path).toAbsolutePath(), ex);
            }
        }

        static ClassLoader newLoader() {
            return new IsolatedLoader(CartServletTest.class.getClassLoader(), realConfig());
        }

        static Object newDbContext(ClassLoader loader) throws Exception {
            return create(loader, "dao.DBContext");
        }

        static HttpServlet newCartServlet(ClassLoader loader) throws Exception {
            return (HttpServlet) create(loader, "controllers.CartServlet");
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
