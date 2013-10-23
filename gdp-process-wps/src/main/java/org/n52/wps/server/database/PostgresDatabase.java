package org.n52.wps.server.database;

import java.io.File;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;
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
    public static final String pgCreationString = "CREATE TABLE RESULTS ("
            + "REQUEST_ID VARCHAR(100) NOT NULL PRIMARY KEY, "
            + "REQUEST_DATE TIMESTAMP, "
            + "RESPONSE_TYPE VARCHAR(100), "
            + "RESPONSE TEXT, "
            + "RESPONSE_MIMETYPE VARCHAR(100))";

    private PostgresDatabase() {
        try {
            Class.forName("org.postgresql.Driver");
            PostgresDatabase.connectionURL = "jdbc:postgresql:" + getDatabasePath() + File.separator + getDatabaseName();
            LOGGER.debug("Database connection URL is: " + PostgresDatabase.connectionURL);
        } catch (ClassNotFoundException cnf_ex) {
            LOGGER.error("Database class could not be loaded: " + connectionURL);
            throw new UnsupportedDatabaseException("The database class could not be loaded.");
        }
    }
    
    public static synchronized PostgresDatabase getInstance() {
        if (PostgresDatabase.db == null) {
            PostgresDatabase.db = new PostgresDatabase();
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
        return PostgresDatabase.db;
    }

    private static boolean createConnection() {
        Properties props = new Properties();
        props.setProperty("create", "true");
        PostgresDatabase.conn = null;
        try {
            PostgresDatabase.conn = DriverManager.getConnection(
                    PostgresDatabase.connectionURL, props);
            LOGGER.info("Connected to WPS database.");
        } catch (SQLException e) {
            LOGGER.error("Could not connect to or create the database.");
            return false;
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
                PostgresDatabase.conn.setAutoCommit(false);
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
            LOGGER.error("Could not create the prepared statements.");
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
        } catch (SQLException sql_ex) {
            LOGGER.error("Prepared statements could not be closed.");
            return false;
        }
        return true;
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
        } catch (SQLException sql_ex) {
            LOGGER.error("Error occured while closing Postgres database connection: "
                    + sql_ex.getMessage() + " :: "
                    + "closed prepared statements?" + isClosedPreparedStatements
                    + ";closed connection?" + isClosedConnection);
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
    public Connection getConnection() {
        return PostgresDatabase.conn;
    }

    @Override
    public String getConnectionURL() {
        return PostgresDatabase.connectionURL;
    }

}
