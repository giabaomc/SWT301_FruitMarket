
package dao;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import models.Cart;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class CartDAOTest {

    private CartDAO dao;

    @Before
    public void setUp() throws Exception {
        dao = createCartDAO();
    }

    private CartDAO createCartDAO() throws Exception {
        Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
        unsafeField.setAccessible(true);

        sun.misc.Unsafe unsafe =
                (sun.misc.Unsafe) unsafeField.get(null);

        CartDAO cartDAO =
                (CartDAO) unsafe.allocateInstance(CartDAO.class);

        Field connectionField =
                DBContext.class.getDeclaredField("connection");
        connectionField.setAccessible(true);
        connectionField.set(cartDAO, null);

        return cartDAO;
    }

    private void setConnection(Connection connection)
            throws Exception {
        Field field = DBContext.class.getDeclaredField("connection");
        field.setAccessible(true);
        field.set(dao, connection);
    }

    private Connection createMockConnection(
            final boolean productExists) {

        return (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class[]{Connection.class},
                (proxy, method, args) -> {

                    if (method.getName().equals("prepareStatement")) {
                        return createMockPreparedStatement(productExists);
                    }

                    return null;
                }
        );
    }

    private PreparedStatement createMockPreparedStatement(
            final boolean productExists) {

        return (PreparedStatement) Proxy.newProxyInstance(
                PreparedStatement.class.getClassLoader(),
                new Class[]{PreparedStatement.class},
                (proxy, method, args) -> {

                    if (method.getName().equals("executeQuery")) {
                        return createMockResultSet(productExists);
                    }

                    return null;
                }
        );
    }

    private ResultSet createMockResultSet(
            final boolean productExists) {

        final boolean[] read = {false};

        return (ResultSet) Proxy.newProxyInstance(
                ResultSet.class.getClassLoader(),
                new Class[]{ResultSet.class},
                (proxy, method, args) -> {

                    String methodName = method.getName();

                    if (methodName.equals("next")) {
                        if (productExists && !read[0]) {
                            read[0] = true;
                            return true;
                        }
                        return false;
                    }

                    if (methodName.equals("getInt")) {
                        return 1;
                    }

                    if (methodName.equals("getString")) {
                        return "Apple";
                    }

                    if (methodName.equals("getDouble")) {
                        return 10.0;
                    }

                    return null;
                }
        );
    }

    @Test
    public void testGetCart_ProductExists() throws Exception {
        setConnection(createMockConnection(true));

        Cart result = dao.getCart(1);

        assertNotNull(result);
        assertEquals(1, result.getProductID());
        assertEquals("Apple", result.getProductName());
        assertEquals(10.0, result.getPrice(), 0.001);
        assertEquals(0, result.getQuantity());
    }

    @Test
    public void testGetCart_ProductDoesNotExist() throws Exception {
        setConnection(createMockConnection(false));

        Cart result = dao.getCart(999);

        assertNull(result);
    }

    @Test
    public void testGetCart_DatabaseError() throws Exception {
        Connection connection = (Connection) Proxy.newProxyInstance(
                Connection.class.getClassLoader(),
                new Class[]{Connection.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("prepareStatement")) {
                        throw new SQLException("Simulated database error");
                    }
                    return null;
                }
        );

        setConnection(connection);

        Cart result = dao.getCart(1);

        assertNull(result);
    }
}
