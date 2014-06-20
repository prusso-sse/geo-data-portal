
package org.n52.wps.server.database.connection;

import java.sql.Connection;
import java.sql.SQLException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

/**
 * @author abramhall
 */
public class JNDIConnectionHandler implements ConnectionHandler {
     
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
