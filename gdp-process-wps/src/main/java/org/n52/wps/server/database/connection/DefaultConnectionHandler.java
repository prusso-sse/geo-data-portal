package org.n52.wps.server.database.connection;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 *
 * @author isuftin
 */
public class DefaultConnectionHandler implements ConnectionHandler{
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
