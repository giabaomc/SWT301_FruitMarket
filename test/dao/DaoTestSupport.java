package dao;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Runs the real {@link UserDAO} (and its {@link DBContext} constructor) outside Tomcat.
 *
 * DBContext loads its settings with getClassLoader().getResourceAsStream("../ConnectDB.properties").
 * Under Tomcat that resolves from WEB-INF/classes to WEB-INF/ConnectDB.properties, but a plain JVM
 * refuses "../" paths and DBContext must not be edited. So each test loads the dao classes through
 * a small class loader that answers that one resource with the configuration the test chooses.
 */
final class DaoTestSupport {

    static final String CONFIG_RESOURCE = "../ConnectDB.properties";

    private DaoTestSupport() {
    }

    /** The webapp's own web/WEB-INF/ConnectDB.properties (tests run from the project folder). */
    static byte[] realConfig() {
        String path = System.getProperty("configFile", "web/WEB-INF/ConnectDB.properties");
        try {
            return Files.readAllBytes(Path.of(path));
        } catch (IOException ex) {
            throw new UncheckedIOException("Cannot read DB config " + Path.of(path).toAbsolutePath(), ex);
        }
    }

    /** A configuration whose server cannot be reached, so DriverManager.getConnection fails. */
    static byte[] unreachableConfig() {
        String props = "url=jdbc:sqlserver://localhost:1;databaseName=Assignment;loginTimeout=2;trustServerCertificate=true\n"
                + "userID=sa\n"
                + "password=123\n";
        return props.getBytes(StandardCharsets.ISO_8859_1);
    }

    /**
     * Creates a new UserDAO with the given configuration and calls checkLogin(email, pass).
     * Any exception thrown by checkLogin itself is rethrown unwrapped.
     */
    static String checkLogin(byte[] config, String email, String pass) throws Throwable {
        ConfigClassLoader loader = new ConfigClassLoader(DaoTestSupport.class.getClassLoader(), config);
        Class<?> daoClass = Class.forName("dao.UserDAO", true, loader);
        Object dao = daoClass.getDeclaredConstructor().newInstance();
        Method method = daoClass.getMethod("checkLogin", String.class, String.class);
        try {
            return (String) method.invoke(dao, email, pass);
        } catch (InvocationTargetException ex) {
            throw ex.getCause();
        }
    }

    /** Defines dao.* classes itself (child-first) and serves ../ConnectDB.properties from memory. */
    private static final class ConfigClassLoader extends ClassLoader {

        private final byte[] config;

        ConfigClassLoader(ClassLoader parent, byte[] config) {
            super(parent);
            this.config = config;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            if (!name.startsWith("dao.") || name.startsWith(DaoTestSupport.class.getName())) {
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
