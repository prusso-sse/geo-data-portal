/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
 *
 * @author abramhall
 */
public class PostgresDashboard implements Dashboard {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresDashboard.class);
    private static final PostgresDatabase db = PostgresDatabase.getInstance();
    private static final String ALL_REQUESTS_QUERY = "select request_id from results where request_id like 'REQ_%';";

    public String parseResponse(String requestId) {
        String response = null;
        try {
            PreparedStatement st = db.getConnection().prepareStatement("select response from results where request_id = ?");
            st.setString(1, requestId);
            ResultSet rs = st.executeQuery();
            if (rs.next()) {
                response = rs.getString(1);
            }
        } catch (SQLException ex) {
            LOGGER.error("failed to select response for request_id " + requestId, ex);
        }
        return response;
    }

    public List<DashboardData> getDashboardData() {
        List<DashboardData> data = new ArrayList<DashboardData>();
        for (String request : getRequestIds()) {
            String baseRequestId = request.substring(4);
            data.add(new DashboardData(request, baseRequestId, baseRequestId + "OUTPUT"));
        }
        return data;
    }

    @Override
    public List<String> getRequestIds() {
        List<String> request_ids = new ArrayList<String>();
        try {
            Statement st = db.getConnection().createStatement();
            ResultSet rs = st.executeQuery(ALL_REQUESTS_QUERY);
            while (rs.next()) {
                String id = rs.getString(1);
                request_ids.add(id);
            }
        } catch (SQLException ex) {
            LOGGER.error("failed to retrieve processes", ex);
        }
        return request_ids;
    }
}
