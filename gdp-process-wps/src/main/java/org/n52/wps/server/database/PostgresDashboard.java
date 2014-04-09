package org.n52.wps.server.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author abramhall
 */
public class PostgresDashboard {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresDashboard.class);
    private static final PostgresDatabase db = PostgresDatabase.getInstance();
    private static final String ALL_REQUESTS_QUERY = "select request_id from results where response_type = 'ExecuteRequest';";
    private static final String RESPONSE_QUERY = "select request_id, response from results where request_id like ?;";

    public List<DashboardData> getDashboardData() {
        List<DashboardData> dataset = new ArrayList<DashboardData>();
        for (String request : getRequestIds()) {
            String baseRequestId = request.substring(4);
            dataset.add(buildDashboardData(baseRequestId));
        }
        return dataset;
    }

    private DashboardData buildDashboardData(String requestId) {
        LOGGER.info("build dashboard data for {}", requestId);
        DashboardData data = new DashboardData(requestId);
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = db.getConnection().prepareStatement(RESPONSE_QUERY);
            pst.setString(1, "%" + requestId + "%");
            rs = pst.executeQuery();
            while (rs.next()) {
                String request_id = rs.getString(1);
                String response = rs.getString(2);
                if (request_id.endsWith("OUTPUT")) {
                    LOGGER.info("setting outputXML {}", response);
                    data.setOutputXML(response);
                } else if (request_id.startsWith("REQ_")) {
                    LOGGER.info("setting requestXML {}", response);
                    data.setRequestXML(response);
                } else {
                    LOGGER.info("setting responseXML {}", response);
                    data.setResponseXML(response);
                }
            }
        } catch (SQLException ex) {
            LOGGER.error("failed to select responses for request ids like " + requestId, ex);
        } finally {
            closeStatement(pst);
            closeResultSet(rs);
        }
        return data;
    }

    private List<String> getRequestIds() {
        Statement st = null;
        ResultSet rs = null;
        List<String> request_ids = new ArrayList<String>();
        try {
            st = db.getConnection().createStatement();
            rs = st.executeQuery(ALL_REQUESTS_QUERY);
            while (rs.next()) {
                String id = rs.getString(1);
                request_ids.add(id);
            }
        } catch (SQLException ex) {
            LOGGER.error("failed to retrieve processes", ex);
        } finally {
            closeResultSet(rs);
            closeStatement(st);
        }
        return request_ids;
    }

    private void closeResultSet(ResultSet rs) {
        if (null != rs) {
            try {
                rs.close();
            } catch (SQLException ex) {
                LOGGER.warn("failed to close result set", ex);
            }
        }
    }

    private void closeStatement(Statement st) {
        if (null != st) {
            try {
                st.close();
            } catch (SQLException ex) {
                LOGGER.warn("failed to close statement", ex);
            }
        }
    }
}
