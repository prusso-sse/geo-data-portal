
package org.n52.wps.server.database.connection;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * @author abramhall
 */
public interface ConnectionHandler {
    public Connection getConnection() throws SQLException;
}
