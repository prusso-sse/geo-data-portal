package org.n52.wps.server.database;

import com.google.common.base.Joiner;
import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
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
import java.util.zip.GZIPOutputStream;
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

    private static final String KEY_DATABASE_ROOT = "org.n52.wps.server.database";
    private static final String KEY_DATABASE_PATH = "path";
    private static final String KEY_DATABASE_WIPE_ENABLED = "wipe.enabled";
    private static final String KEY_DATABASE_WIPE_PERIOD = "wipe.period";
    private static final String KEY_DATABASE_WIPE_THRESHOLD = "wipe.threshold";
    private static final boolean DEFAULT_DATABASE_WIPE_ENABLED = true;
    private static final long DEFAULT_DATABASE_WIPE_PERIOD = 1000 * 60 * 60;
    private static final long DEFAULT_DATABASE_WIPE_THRESHOLD = 1000 * 60 * 60 * 24 * 7;
    private static final int DATA_BUFFER_SIZE = 8192;
    private static final String SUFFIX_GZIP = "gz";
    private static final String DEFAULT_BASE_DIRECTORY
            = Joiner.on(File.separator).join(System.getProperty("java.io.tmpdir", "."), "Database", "Results");
    private static final ServerDocument.Server server = WPSConfig.getInstance().getWPSConfig().getServer();
    private static final String baseResultURL = String.format("http://%s:%s/%s/RetrieveResultServlet?id=",
            server.getHostname(), server.getHostport(), server.getWebappPath());

    private static String connectionURL;
    private static Path BASE_DIRECTORY;
    private static PostgresDatabase instance;
    private static ConnectionHandler connectionHandler;

    protected static Timer wipeTimer;

    private static final String CREATE_RESULTS_TABLE_PSQL
            = "CREATE TABLE RESULTS ("
            + "REQUEST_ID VARCHAR(100) NOT NULL PRIMARY KEY, "
            + "REQUEST_DATE TIMESTAMP, "
            + "RESPONSE_TYPE VARCHAR(100), "
            + "RESPONSE TEXT, "
            + "RESPONSE_MIMETYPE VARCHAR(100))";

    private PostgresDatabase() {
        try {
            Class.forName("org.postgresql.Driver");
            PropertyUtil propertyUtil = new PropertyUtil(server.getDatabase().getPropertyArray(), KEY_DATABASE_ROOT);
            initializeBaseDirectory(propertyUtil);
            initializeDatabaseWiper(propertyUtil);
            initializeConnectionHandler();
            initializeResultsTable();
        } catch (ClassNotFoundException cnfe) {
            LOGGER.error("Database class could not be loaded.", cnfe);
            throw new UnsupportedDatabaseException("The database class could not be loaded.", cnfe);
        } catch (NamingException | IOException | SQLException ex) {
            LOGGER.error("Error creating PostgresDatabase", ex);
            throw new RuntimeException("Error creating PostgresDatabase", ex);
        }
    }

    private void initializeBaseDirectory(PropertyUtil propertyUtil) throws IOException {
        String baseDirectoryPath = propertyUtil.extractString(KEY_DATABASE_PATH, DEFAULT_BASE_DIRECTORY);
        BASE_DIRECTORY = Paths.get(baseDirectoryPath);
        LOGGER.info("Using \"{}\" as base directory for results database", baseDirectoryPath);
        if (!Files.isDirectory(BASE_DIRECTORY)) {
            Files.createDirectories(BASE_DIRECTORY);
        }
    }

    private void initializeDatabaseWiper(PropertyUtil propertyUtil) {
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
    }

    private void initializeConnectionHandler() throws SQLException, NamingException {
        String jndiName = getDatabaseProperties("jndiName");
        if (null != jndiName) {
            connectionHandler = new JNDIConnectionHandler(jndiName);
        } else {
            connectionURL = "jdbc:postgresql:" + getDatabasePath() + "/" + getDatabaseName();
            LOGGER.debug("Database connection URL is: " + connectionURL);
            String username = getDatabaseProperties("username");
            String password = getDatabaseProperties("password");
            Properties props = new Properties();
            props.setProperty("create", "true");
            props.setProperty("user", username);
            props.setProperty("password", password);
            connectionHandler = new DefaultConnectionHandler(connectionURL, props);
        }
    }

    private void initializeResultsTable() throws SQLException {
        try (Connection connection = getConnection(); ResultSet rs = connection.getMetaData().getTables(null, null, "results", new String[]{"TABLE"})) {
            if (!rs.next()) {
                LOGGER.info("Table RESULTS does not yet exist.");
                Statement st = connection.createStatement();
                st.executeUpdate(CREATE_RESULTS_TABLE_PSQL);
            }
        }
    }

    public static synchronized PostgresDatabase getInstance() {
        if (instance == null) {
            instance = new PostgresDatabase();
        }
        return instance;
    }

    @Override
    public String getConnectionURL() {
        return connectionURL;
    }

    @Override
    public Connection getConnection() {
        try {
            return connectionHandler.getConnection();
        } catch (SQLException ex) {
            throw new RuntimeException("Unable to obtain connection to database!", ex);
        }
    }

    @Override
    public String generateRetrieveResultURL(String id) {
        return baseResultURL + id;
    }

    @Override
    public void insertRequest(String id, InputStream inputStream, boolean xml) {
        insertResultEntity(inputStream, "REQ_" + id, "ExecuteRequest", xml ? "text/xml" : "text/plain");
    }

    @Override
    public String insertResponse(String id, InputStream inputStream) {
        return insertResultEntity(inputStream, id, "ExecuteResponse", "text/xml");
    }

    @Override
    protected String insertResultEntity(InputStream stream, String id, String type, String mimeType) {
        BufferedInputStream dataStream = new BufferedInputStream(stream, DATA_BUFFER_SIZE);
        boolean isOutput = null != id && id.toLowerCase().contains("output");

        if (isOutput && !Boolean.parseBoolean(getDatabaseProperties("saveResultsToDB"))) {
            try {
                dataStream = writeDataToDiskWithGZIP(id, stream);
            } catch (Exception ex) {
                LOGGER.error("Failed to write output data to disk", ex);
            }
        }

        try (Connection connection = getConnection(); PreparedStatement myInsertSQL = connection.prepareStatement(insertionString)) {
            myInsertSQL.setString(INSERT_COLUMN_REQUEST_ID, id);
            myInsertSQL.setTimestamp(INSERT_COLUMN_REQUEST_DATE, new Timestamp(Calendar.getInstance().getTimeInMillis()));
            myInsertSQL.setString(INSERT_COLUMN_RESPONSE_TYPE, type);
            myInsertSQL.setString(INSERT_COLUMN_MIME_TYPE, mimeType);
            myInsertSQL.setAsciiStream(INSERT_COLUMN_RESPONSE, dataStream, DATA_BUFFER_SIZE);
            myInsertSQL.executeUpdate();
            LOGGER.debug("inserted request {} into database", id);
        } catch (SQLException ex) {
            LOGGER.error("Failed to insert result data into the database", ex);
        }

        return generateRetrieveResultURL(id);
    }

    @Override
    public void updateResponse(String id, InputStream stream) {
        BufferedInputStream dataStream = new BufferedInputStream(stream, DATA_BUFFER_SIZE);

        try (Connection connection = getConnection(); PreparedStatement myUpdateSQL = connection.prepareStatement(updateString)) {
            myUpdateSQL.setString(UPDATE_COLUMN_REQUEST_ID, id);
            myUpdateSQL.setAsciiStream(UPDATE_COLUMN_RESPONSE, dataStream, DATA_BUFFER_SIZE);
            myUpdateSQL.executeUpdate();
        } catch (SQLException ex) {
            LOGGER.error("Could not update response in database", ex);
        }
    }

    @Override
    public InputStream lookupResponse(String id) {
        InputStream result = null;
        if (null != id) {
            try (Connection connection = getConnection(); PreparedStatement mySelectSQL = connection.prepareStatement(selectionString)) {
                mySelectSQL.setString(SELECT_COLUMN_RESPONSE, id);
                try (ResultSet rs = mySelectSQL.executeQuery()) {
                    if (null == rs || !rs.next()) {
                        LOGGER.warn("No response found for request id " + id);
                    } else {
                        result = rs.getAsciiStream(1);
                    }
                }
            } catch (SQLException ex) {
                LOGGER.error("Could not look up response in database", ex);
            }

            if (null != result) {
                if (id.toLowerCase().contains("output") && !Boolean.parseBoolean(getDatabaseProperties("saveResultsToDB"))) {
                    try {
                        String outputFileLocation = IOUtils.toString(result);
                        LOGGER.info("ID {} is output and saved to disk instead of database. Path = " + outputFileLocation);
                        if (Files.exists(Paths.get(outputFileLocation))) {
                            result = new GZIPInputStream(new FileInputStream(outputFileLocation));
                        } else {
                            LOGGER.warn("Response not found on disk for id " + id + " at " + outputFileLocation);
                        }
                    } catch (FileNotFoundException ex) {
                        LOGGER.warn("Response not found on disk for id " + id, ex);
                    } catch (IOException ex) {
                        LOGGER.warn("Error processing response for id " + id, ex);
                    }
                }
            } else {
                LOGGER.warn("response found but returned null");
            }
        } else {
            LOGGER.warn("tried to look up response for null id, returned null");
        }
        return result;
    }

    @Override
    public String getMimeTypeForStoreResponse(String id) {
        String mimeType = null;
        try (Connection connection = getConnection(); PreparedStatement mySelectSQL = connection.prepareStatement(selectionString)) {
            mySelectSQL.setString(SELECT_COLUMN_RESPONSE, id);
            try (ResultSet rs = mySelectSQL.executeQuery()) {
                if (null == rs || !rs.next()) {
                    LOGGER.warn("No response found for request id " + id);
                } else {
                    mimeType = rs.getString(2);
                }
            }
        } catch (SQLException ex) {
            LOGGER.error("Could not look up response in database", ex);
        }
        return mimeType;
    }

    @Override
    public File lookupResponseAsFile(String id) {
        if (id.toLowerCase().contains("output") && !Boolean.parseBoolean(getDatabaseProperties("saveResultsToDB"))) {
            try {
                String outputFileLocation = IOUtils.toString(lookupResponse(id));
                return new File(new URI(outputFileLocation));
            } catch (URISyntaxException | IOException ex) {
                LOGGER.warn("Could not get file location for response file for id " + id, ex);
            }
        }
        LOGGER.warn("requested response as file for a response stored in the database, returning null");
        return null;
    }

    /**
     * @param filename base filename
     * @param stream data stream to write to disk, compressed using gzip
     * @return a stream of the file URI pointing where the data was written
     * @throws IOException
     */
    private BufferedInputStream writeDataToDiskWithGZIP(String filename, InputStream stream) throws Exception {
        Files.createDirectories(BASE_DIRECTORY);
        Path filePath = Paths.get(BASE_DIRECTORY.toString(), Joiner.on(".").join(filename, SUFFIX_GZIP));
        filePath = Files.createFile(filePath);
        OutputStream outputStream = new GZIPOutputStream(Files.newOutputStream(filePath));
        IOUtils.copy(stream, outputStream);
        IOUtils.closeQuietly(outputStream);
        byte[] filePathByteArray = filePath.toUri().toString().getBytes();
        return new BufferedInputStream(new ByteArrayInputStream(filePathByteArray));

    }

    private interface ConnectionHandler {

        public Connection getConnection() throws SQLException;
    }

    private class JNDIConnectionHandler implements ConnectionHandler {

        private final DataSource dataSource;

        public JNDIConnectionHandler(String jndiName) throws NamingException {
            InitialContext context = new InitialContext();
            dataSource = (DataSource) context.lookup("java:comp/env/jdbc/" + jndiName);
        }

        @Override
        public Connection getConnection() throws SQLException {
            Connection conn = dataSource.getConnection();
            return conn;
        }
    }

    private class DefaultConnectionHandler implements ConnectionHandler {

        private final String dbConnectionURL;
        private final Properties dbProps;

        public DefaultConnectionHandler(String dbConnectionURL, Properties dbProps) {
            this.dbConnectionURL = dbConnectionURL;
            this.dbProps = dbProps;
        }

        @Override
        public Connection getConnection() throws SQLException {
            Connection conn = DriverManager.getConnection(dbConnectionURL, dbProps);
            return conn;
        }
    }

    private class WipeTimerTask extends TimerTask {

        private final long thresholdMillis;
        private static final String DELETE_STATEMENT = "DELETE FROM RESULTS WHERE RESULTS.REQUEST_ID = ANY ( ? );";
        private static final String LOOKUP_STATEMENT = "SELECT * FROM "
                + "(SELECT REQUEST_ID, EXTRACT(EPOCH FROM REQUEST_DATE) * 1000 AS TIMESTAMP FROM RESULTS) items WHERE TIMESTAMP < ?";

        WipeTimerTask(long thresholdMillis) {
            this.thresholdMillis = thresholdMillis;
        }

        @Override
        public void run() {
            Boolean savingResultsToDB = Boolean.parseBoolean(getDatabaseProperties("saveResultsToDB"));
            try {
                wipe(thresholdMillis, savingResultsToDB);
            } catch (SQLException | IOException ex) {
                LOGGER.warn("Failed to deleted old records.", ex);
            }
        }

        private void wipe(long thresholdMillis, Boolean saveResultsToDB) throws SQLException, IOException {
            LOGGER.info(getDatabaseName() + " Postgres wiper, checking for records older than {} ms", thresholdMillis);
            List<String> oldRecords = findOldRecords();
            if (!saveResultsToDB) {
                for (String recordId : oldRecords) {
                    if (recordId.toLowerCase().contains("output")) {
                        Files.deleteIfExists(Paths.get(BASE_DIRECTORY.toString(), recordId));
                    }
                }
            }
            if (!oldRecords.isEmpty()) {
                deleteRecords(oldRecords);
            }
        }

        private void deleteRecords(List<String> recordIds) throws SQLException {
            Integer deletedRecordsCount = 0;
            try (Connection connection = getConnection(); PreparedStatement deleteStatement = connection.prepareStatement(DELETE_STATEMENT)) {
                deleteStatement.setArray(1, connection.createArrayOf("varchar", recordIds.toArray()));
                deletedRecordsCount = deleteStatement.executeUpdate();
            }
            LOGGER.info("Cleaned {} records from database", deletedRecordsCount);
        }

        private List<String> findOldRecords() throws SQLException {
            List<String> matchingRecords = new ArrayList<>();
            try (Connection connection = getConnection(); PreparedStatement lookupStatement = connection.prepareStatement(LOOKUP_STATEMENT)) {
                long ageMillis = System.currentTimeMillis() - thresholdMillis;
                lookupStatement.setLong(1, ageMillis);
                try (ResultSet rs = lookupStatement.executeQuery()) {
                    while (rs.next()) {
                        matchingRecords.add(rs.getString(1));
                    }
                }
            }
            return matchingRecords;
        }
    }
}
