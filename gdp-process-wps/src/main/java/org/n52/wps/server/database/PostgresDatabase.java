package org.n52.wps.server.database;

import com.google.common.base.Joiner;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Properties;
import java.util.Timer;
import java.util.TimerTask;
import java.util.zip.GZIPInputStream;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;
import org.apache.commons.io.IOUtils;
import org.n52.wps.ServerDocument;
import org.n52.wps.commons.PropertyUtil;
import org.n52.wps.commons.WPSConfig;
import static org.n52.wps.server.database.AbstractDatabase.getDatabasePath;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author isuftin
 */
public class PostgresDatabase extends AbstractDatabase {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresDatabase.class);
    private static PostgresDatabase db;
    private static String connectionURL = null;
    private static Connection conn = null;
    private final static String KEY_DATABASE_ROOT = "org.n52.wps.server.database";
    private final static String KEY_DATABASE_PATH = "path";
    private final static String KEY_DATABASE_WIPE_ENABLED = "wipe.enabled";
    private final static String KEY_DATABASE_WIPE_PERIOD = "wipe.period";
    private final static String KEY_DATABASE_WIPE_THRESHOLD = "wipe.threshold";
    private final static boolean DEFAULT_DATABASE_WIPE_ENABLED = true;
    private final static long DEFAULT_DATABASE_WIPE_PERIOD = 1000 * 60 * 60;
    private final static long DEFAULT_DATABASE_WIPE_THRESHOLD = 1000 * 60 * 60 * 24 * 7;
    private final static int DATA_BUFFER_SIZE = 8192;
    private final static String SUFFIX_GZIP = "gz";
    private final static String DEFAULT_BASE_DIRECTORY
            = Joiner.on(File.separator).join(
                    System.getProperty("java.io.tmpdir", "."),
                    "Database",
                    "Results");
    private static Path BASE_DIRECTORY;
    private static final ServerDocument.Server server = WPSConfig.getInstance().getWPSConfig().getServer();
    private static final String baseResultURL = String.format("http://%s:%s/%s/RetrieveResultServlet?id=",
            server.getHostname(), server.getHostport(), server.getWebappPath());
    public static final String pgCreationString = "CREATE TABLE RESULTS ("
            + "REQUEST_ID VARCHAR(100) NOT NULL PRIMARY KEY, "
            + "REQUEST_DATE TIMESTAMP, "
            + "RESPONSE_TYPE VARCHAR(100), "
            + "RESPONSE TEXT, "
            + "RESPONSE_MIMETYPE VARCHAR(100))";
    protected final Object storeResponseSerialNumberLock;
    protected final Timer wipeTimer;

    private PostgresDatabase() {
        try {
            Class.forName("org.postgresql.Driver");
            PostgresDatabase.connectionURL = "jdbc:postgresql:" + getDatabasePath() + "/" + getDatabaseName();
            LOGGER.debug("Database connection URL is: " + PostgresDatabase.connectionURL);

            // Create lock object
            storeResponseSerialNumberLock = new Object();

            // Create database wiper task
            PropertyUtil propertyUtil = new PropertyUtil(server.getDatabase().getPropertyArray(), KEY_DATABASE_ROOT);
            if (propertyUtil.extractBoolean(KEY_DATABASE_WIPE_ENABLED, DEFAULT_DATABASE_WIPE_ENABLED)) {
                long periodMillis = propertyUtil.extractPeriodAsMillis(KEY_DATABASE_WIPE_PERIOD, DEFAULT_DATABASE_WIPE_PERIOD);
                long thresholdMillis = propertyUtil.extractPeriodAsMillis(KEY_DATABASE_WIPE_THRESHOLD, DEFAULT_DATABASE_WIPE_THRESHOLD);

                wipeTimer = new Timer(getClass().getSimpleName() + " Postgres Wiper", true);
                wipeTimer.scheduleAtFixedRate(new PostgresDatabase.WipeTimerTask(thresholdMillis), 15000, periodMillis);
                LOGGER.info("Started {} Postgres wiper timer; period {} ms, threshold {} ms",
                        new Object[]{getDatabaseName(), periodMillis, thresholdMillis});
            } else {
                wipeTimer = null;
            }
        } catch (ClassNotFoundException cnf_ex) {
            LOGGER.error("Database class could not be loaded", cnf_ex);
            throw new UnsupportedDatabaseException("The database class could not be loaded.");
        }
    }

    public static synchronized PostgresDatabase getInstance() {
        if (db == null) {
            db = new PostgresDatabase();
        }

        if (db.getConnection() == null) {
            if (!PostgresDatabase.createConnection()) {
                throw new RuntimeException("Creating database connection failed.");
            }
            if (!PostgresDatabase.createResultTable()) {
                throw new RuntimeException("Creating result table failed.");
            }
            if (!PostgresDatabase.createPreparedStatements()) {
                throw new RuntimeException("Creating prepared statements failed.");
            }
        }

        PropertyUtil propertyUtil = new PropertyUtil(server.getDatabase().getPropertyArray(), KEY_DATABASE_ROOT);
        String baseDirectoryPath = propertyUtil.extractString(KEY_DATABASE_PATH, DEFAULT_BASE_DIRECTORY);
        BASE_DIRECTORY = Paths.get(baseDirectoryPath);
        LOGGER.info("Using \"{}\" as base directory for results database", baseDirectoryPath);
        try {
            Files.createDirectories(BASE_DIRECTORY);
        } catch (IOException ex) {
            LOGGER.error("Error ensuring base directory exists", ex);
        }
        return PostgresDatabase.db;
    }

    private static boolean createConnection() {
        Properties props = new Properties();
        DataSource dataSource;
        String jndiName = getDatabaseProperties("jndiName");
        String username = getDatabaseProperties("username");
        String password = getDatabaseProperties("password");

        if (jndiName != null) {
            InitialContext context;
            try {
                context = new InitialContext();
                dataSource = (DataSource) context.lookup("java:comp/env/jdbc/" + jndiName);
                conn = dataSource.getConnection();
                PostgresDatabase.conn.setAutoCommit(false);
                LOGGER.info("Connected to WPS database.");
            } catch (NamingException e) {
                LOGGER.error("Could not connect to or create the database.", e);
                return false;
            } catch (SQLException e) {
                LOGGER.error("Could not connect to or create the database.", e);
                return false;
            }
        } else {
            props.setProperty("create", "true");
            props.setProperty("user", username);
            props.setProperty("password", password);
            PostgresDatabase.conn = null;
            try {
                PostgresDatabase.conn = DriverManager.getConnection(
                        PostgresDatabase.connectionURL, props);
                PostgresDatabase.conn.setAutoCommit(false);
                LOGGER.info("Connected to WPS database.");
            } catch (SQLException e) {
                LOGGER.error("Could not connect to or create the database.", e);
                return false;
            }
        }
        return true;
    }

    private static boolean createResultTable() {
        try {
            ResultSet rs;
            DatabaseMetaData meta = PostgresDatabase.conn.getMetaData();
            rs = meta.getTables(null, null, "results", new String[]{"TABLE"});
            if (!rs.next()) {
                LOGGER.info("Table RESULTS does not yet exist.");
                Statement st = PostgresDatabase.conn.createStatement();
                st.executeUpdate(PostgresDatabase.pgCreationString);

                PostgresDatabase.conn.commit();

                meta = PostgresDatabase.conn.getMetaData();

                rs = meta.getTables(null, null, "RESULTS", new String[]{"TABLE"});
                if (rs.next()) {
                    LOGGER.info("Succesfully created table RESULTS.");
                } else {
                    LOGGER.error("Could not create table RESULTS.");
                    return false;
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Connection to the Postgres database failed: " + e.getMessage());
            return false;
        }
        return true;
    }

    private static boolean createPreparedStatements() {
        try {
            PostgresDatabase.closePreparedStatements();
            PostgresDatabase.insertSQL = PostgresDatabase.conn.prepareStatement(insertionString);
            PostgresDatabase.selectSQL = PostgresDatabase.conn.prepareStatement(selectionString);
            PostgresDatabase.updateSQL = PostgresDatabase.conn.prepareStatement(updateString);
        } catch (SQLException e) {
            LOGGER.error("Could not create the prepared statements.", e);
            return false;
        }
        return true;
    }

    private static boolean closePreparedStatements() {
        try {
            if (PostgresDatabase.insertSQL != null) {
                PostgresDatabase.insertSQL.close();
                PostgresDatabase.insertSQL = null;
            }
            if (PostgresDatabase.selectSQL != null) {
                PostgresDatabase.selectSQL.close();
                PostgresDatabase.selectSQL = null;
            }
            if (PostgresDatabase.updateSQL != null) {
                PostgresDatabase.updateSQL.close();
                PostgresDatabase.updateSQL = null;
            }
        } catch (SQLException e) {
            LOGGER.error("Prepared statements could not be closed.", e);
            return false;
        }
        return true;
    }

    @Override
    public synchronized void insertRequest(String id, InputStream inputStream, boolean xml) {
        insertResultEntity(inputStream, "REQ_" + id, "ExecuteRequest", xml ? "text/xml" : "text/plain");
    }

    @Override
    public synchronized String insertResponse(String id, InputStream inputStream) {
        return insertResultEntity(inputStream, id, "ExecuteResponse", "text/xml");
    }

    @Override
    protected synchronized String insertResultEntity(InputStream stream, String id, String type, String mimeType) {
        BufferedInputStream dataStream = new BufferedInputStream(stream, DATA_BUFFER_SIZE);
        boolean isOutput = null != id && id.toLowerCase().contains("output");

        if (isOutput && !Boolean.parseBoolean(getDatabaseProperties("saveResultsToDB"))) {
            try {
                dataStream = writeDataToDisk(id, stream);
            } catch (Exception ex) {
                LOGGER.error("Failed to write output data to disk", ex);
            }
        }

        try {
            insertSQL.setString(INSERT_COLUMN_REQUEST_ID, id);
            insertSQL.setTimestamp(INSERT_COLUMN_REQUEST_DATE, new Timestamp(Calendar.getInstance().getTimeInMillis()));
            insertSQL.setString(INSERT_COLUMN_RESPONSE_TYPE, type);
            insertSQL.setString(INSERT_COLUMN_MIME_TYPE, mimeType);
            insertSQL.setAsciiStream(INSERT_COLUMN_RESPONSE, dataStream, DATA_BUFFER_SIZE);
            insertSQL.executeUpdate();
            getConnection().commit();
            LOGGER.debug("inserted request {} into database", id);
        } catch (SQLException ex) {
            LOGGER.error("Failed to insert result data into the database", ex);
        }

        return generateRetrieveResultURL(id);
    }

    /**
     *
     * @param id
     * @param stream
     * @return a stream of the file URI pointing where the data was written
     * @throws IOException
     */
    private BufferedInputStream writeDataToDisk(String id, InputStream stream) throws Exception {
        Files.createDirectories(BASE_DIRECTORY);
        Path filePath = Paths.get(BASE_DIRECTORY.toString(), id);
        filePath = Files.createFile(filePath);
        Files.copy(stream, filePath, StandardCopyOption.REPLACE_EXISTING);
        byte[] filePathByteArray = filePath.toUri().toString().getBytes();
        return new BufferedInputStream(new ByteArrayInputStream(filePathByteArray));
    }

    @Override
    public synchronized void updateResponse(String id, InputStream stream) {
        BufferedInputStream dataStream = new BufferedInputStream(stream, DATA_BUFFER_SIZE);
        try {
            updateSQL.setString(UPDATE_COLUMN_REQUEST_ID, id);
            updateSQL.setAsciiStream(UPDATE_COLUMN_RESPONSE, dataStream, DATA_BUFFER_SIZE);
            updateSQL.executeUpdate();
            getConnection().commit();
        } catch (SQLException ex) {
            LOGGER.error("Could not update response in database", ex);
        }
    }

    @Override
    public void shutdown() {
        boolean isClosedPreparedStatements = false;
        boolean isClosedConnection = false;

        try {
            if (PostgresDatabase.conn != null) {
                isClosedPreparedStatements = closePreparedStatements();
                Properties props = new Properties();
                props.setProperty("shutdown", "true");
                PostgresDatabase.conn = DriverManager.getConnection(PostgresDatabase.connectionURL, props);
                PostgresDatabase.conn.close();
                PostgresDatabase.conn = null;
                isClosedConnection = true;
                PostgresDatabase.db = null;
            }
        } catch (SQLException e) {
            LOGGER.error("Error occured while closing Postgres database connection: "
                    + "closed prepared statements?" + isClosedPreparedStatements
                    + ";closed connection?" + isClosedConnection, e);
            return;
        } finally {
            try {
                if (PostgresDatabase.conn != null) {
                    try {
                        PostgresDatabase.conn.close();
                    } catch (SQLException e) {
                        LOGGER.warn("Postgres database connection was not closed successfully during shutdown", e);
                    }
                    PostgresDatabase.conn = null;
                }
            } finally {
                System.gc();
            }
        }
        LOGGER.info("Postgres database connection is closed succesfully");
    }

    @Override
    public InputStream lookupResponse(String id) {
        if (null == id) {
            LOGGER.warn("tried to look up response for null id, returned null");
            return null;
        }
        InputStream result = super.lookupResponse(id);
        if (id.toLowerCase().contains("output") && !Boolean.parseBoolean(getDatabaseProperties("saveResultsToDB"))) {
            LOGGER.debug("ID {} is output and saved to disk instead of database");
            FileInputStream fis = null;
            try {
                String outputFileLocation = IOUtils.toString(result);
                if (Files.exists(Paths.get(outputFileLocation))) {
                    fis = new FileInputStream(outputFileLocation);
                    result = outputFileLocation.endsWith(SUFFIX_GZIP) ? new GZIPInputStream(fis) : fis;
                }
            } catch (FileNotFoundException ex) {
                LOGGER.warn("Response not found for id " + id, ex);
            } catch (IOException ex) {
                LOGGER.warn("Error processing response for id " + id, ex);
            } finally {
                try {
                    fis.close();
                } catch (IOException ex) {
                    LOGGER.warn("failed to close file input stream", ex);
                }
            }
        }
        return result;
    }

    @Override
    public File lookupResponseAsFile(String id) {
        if (id.toLowerCase().contains("output") && !Boolean.parseBoolean(getDatabaseProperties("saveResultsToDB"))) {
            try {
                String outputFileLocation = IOUtils.toString(lookupResponse(id));
                return new File(new URI(outputFileLocation));
            } catch (URISyntaxException ex) {
                LOGGER.warn("Could not get file location for response file for id " + id, ex);
            } catch (IOException ex) {
                LOGGER.warn("Could not get file location for response file for id " + id, ex);
            }
        }
        LOGGER.warn("requested response as file for a response stored in the database, returning null");
        return null;
    }

    private class WipeTimerTask extends TimerTask {

        public final long thresholdMillis;

        WipeTimerTask(long thresholdMillis) {
            this.thresholdMillis = thresholdMillis;
        }

        @Override
        public void run() {
            Boolean savingResultsToDB = Boolean.parseBoolean(getDatabaseProperties("saveResultsToDB"));
            wipe(thresholdMillis, savingResultsToDB);
        }

        private void wipe(long thresholdMillis, Boolean saveResultsToDB) {
            // SimpleDataFormat is not thread-safe.
            long currentTimeMillis = System.currentTimeMillis();
            LOGGER.info(getDatabaseName() + " Postgres wiper, checking for records older than {} ms",
                    thresholdMillis);

            List<String> oldRecords = findOldRecords(currentTimeMillis, thresholdMillis);
            if (oldRecords.size() > 0) {
                // Clean up files on disk if needed
                if (!saveResultsToDB) {
                    for (String recordId : oldRecords) {
                        if (recordId.toLowerCase().contains("output")) {
                            deleteFileOnDisk(recordId);
                        }
                    }
                }

                // Clean up records in database
                Integer recordsDeleted = deleteRecords(oldRecords);
                LOGGER.info("Cleaned {} records from database", recordsDeleted);

            }
        }

        private void deleteFileOnDisk(String id) {
            try {
                Files.deleteIfExists(Paths.get(BASE_DIRECTORY.toString(), id));
            } catch (IOException ex) {
                LOGGER.warn("Failed to delete file", ex);
            }
        }

        private Integer deleteRecords(List<String> recordIds) {
            Integer deletedRecordsCount = 0;
            PreparedStatement deleteStatement = null;

            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < recordIds.size(); i++) {
                builder.append("?,");
            }

            try {

                String deleteStatementString = "DELETE FROM RESULTS "
                        + "WHERE RESULTS.REQUEST_ID IN (" + builder.deleteCharAt(builder.length() - 1).toString() + ")";
                deleteStatement = PostgresDatabase.conn.prepareStatement(deleteStatementString);

                int idIdx = 1;
                for (String id : recordIds) {
                    deleteStatement.setString(idIdx, id);
                    idIdx++;
                }
                deletedRecordsCount = deleteStatement.executeUpdate();
                PostgresDatabase.conn.commit();
            } catch (SQLException ex) {
                LOGGER.warn("Could not delete rows from Postgres database", ex);
            } finally {
                if (null != deleteStatement) {
                    try {
                        deleteStatement.close();
                    } catch (SQLException e) {
                        LOGGER.warn("Postgres Wiper: Could not close prepared statement", e);
                    }
                }
            }

            return deletedRecordsCount;
        }

        private List<String> findOldRecords(long currentTimeMillis, long threshold) {
            PreparedStatement lookupStatement = null;
            ResultSet rs = null;
            List<String> matchingRecords = new ArrayList<String>();
            try {
                long ageMillis = currentTimeMillis - thresholdMillis;
                String lookupStatementString = "SELECT * FROM "
                        + "(SELECT REQUEST_ID, EXTRACT(EPOCH FROM REQUEST_DATE) * 1000 AS TIMESTAMP "
                        + "FROM RESULTS) items "
                        + "WHERE TIMESTAMP < ?";
                lookupStatement = PostgresDatabase.conn.prepareStatement(lookupStatementString);
                lookupStatement.setLong(1, ageMillis);
                rs = lookupStatement.executeQuery();

                while (rs.next()) {
                    matchingRecords.add(rs.getString(1));
                }

            } catch (SQLException ex) {
                LOGGER.warn("");
            } finally {
                if (null != rs) {
                    try {
                        rs.close();
                    } catch (SQLException e) {
                        LOGGER.warn("Postgres Wiper: Could not close result set", e);
                    }
                }

                if (null != lookupStatement) {
                    try {
                        lookupStatement.close();
                    } catch (SQLException e) {
                        LOGGER.warn("Postgres Wiper: Could not close prepared statement", e);
                    }
                }
            }
            return matchingRecords;
        }
    }

    @Override
    public String generateRetrieveResultURL(String id) {
        return baseResultURL + id;
    }

    @Override
    public Connection getConnection() {
        return PostgresDatabase.conn;
    }

    @Override
    public String getConnectionURL() {
        return PostgresDatabase.connectionURL;
    }

}
