
package controllers;

import dao.DBContext;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import models.Cart;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class CartServletTest {

    private DBAccess db;
    private int productId1;
    private int productId2;
    private int missingProductId;

    private static class DBAccess extends DBContext {
        Connection getConnection() {
            return connection;
        }
    }

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
        db = new DBAccess();

        assertNotNull(
                "Database connection unavailable. Check ConnectDB.properties.",
                db.getConnection()
        );

        List<Integer> ids = new ArrayList<>();

        String sql = "SELECT TOP 2 productID "
                + "FROM tblProducts ORDER BY productID";

        try (PreparedStatement ps =
                     db.getConnection().prepareStatement(sql);
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
        if (db != null && db.getConnection() != null) {
            db.getConnection().close();
        }
    }

    private int findMissingProductId() throws SQLException {
        int candidate = 999999;

        String sql = "SELECT productID FROM tblProducts "
                + "WHERE productID = ?";

        while (true) {
            boolean exists;

            try (PreparedStatement ps =
                         db.getConnection().prepareStatement(sql)) {

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

        TestOutcome outcome = new TestOutcome();

        Map<String, String> parameters = new HashMap<>();

        if (service != null) {
            parameters.put("service", service);
        }

        if (productID != null) {
            parameters.put("productID", productID);
        }

        HttpSession httpSession = createSession(session);
        HttpServletRequest request =
                createRequest(parameters, httpSession, outcome);
        HttpServletResponse response = createResponse(outcome);

        TestableCartServlet servlet = new TestableCartServlet();

        try {
            if (usePost) {
                servlet.callPost(request, response);
            } else {
                servlet.callGet(request, response);
            }
        } catch (Throwable ex) {
            outcome.exception = ex;
        }

        return outcome;
    }

    private static class TestableCartServlet extends CartServlet {

        void callGet(
                HttpServletRequest request,
                HttpServletResponse response)
                throws ServletException, IOException {
            super.doGet(request, response);
        }

        void callPost(
                HttpServletRequest request,
                HttpServletResponse response)
                throws ServletException, IOException {
            super.doPost(request, response);
        }
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
            TestOutcome outcome) {

        return (HttpServletRequest) Proxy.newProxyInstance(
                HttpServletRequest.class.getClassLoader(),
                new Class<?>[]{HttpServletRequest.class},
                (proxy, method, args) -> {
                    switch (method.getName()) {
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

    // UTCID01: Add a valid product for the first time.
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

    // UTCID02: Add the same product again; quantity increases.
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

    // UTCID03: Different products use different session keys.
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

    // UTCID04: A product ID that does not exist in the database.
    @Test
    public void UTCID04_addNonExistingProduct() {
        FakeSession session = new FakeSession();

        TestOutcome outcome = execute(
                "add2cart",
                String.valueOf(missingProductId),
                session,
                false
        );

        assertTrue(
                "Current code is expected to throw NullPointerException "
                + "when DAO returns null.",
                outcome.exception instanceof NullPointerException
        );
    }

    // UTCID05: Missing productID parameter.
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
                "Current code is expected to throw NumberFormatException.",
                outcome.exception instanceof NumberFormatException
        );
    }

    // UTCID06: Non-numeric productID.
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
                "Current code is expected to throw NumberFormatException.",
                outcome.exception instanceof NumberFormatException
        );
    }

    // UTCID07: Boundary value productID = 0.
    @Test
    public void UTCID07_zeroProductID() {
        FakeSession session = new FakeSession();

        TestOutcome outcome = execute(
                "add2cart",
                "0",
                session,
                false
        );

        assertTrue(
                "Expected NullPointerException if product ID 0 is absent.",
                outcome.exception instanceof NullPointerException
        );
    }

    // UTCID08: Negative productID.
    @Test
    public void UTCID08_negativeProductID() {
        FakeSession session = new FakeSession();

        TestOutcome outcome = execute(
                "add2cart",
                "-1",
                session,
                false
        );

        assertTrue(
                "Expected NullPointerException if product ID -1 is absent.",
                outcome.exception instanceof NullPointerException
        );
    }

    // UTCID09: Add a valid product through an HTTP POST request.
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
}
