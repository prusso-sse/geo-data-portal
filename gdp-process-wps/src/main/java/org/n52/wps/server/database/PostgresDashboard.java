package org.n52.wps.server.database;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import javax.xml.bind.DatatypeConverter;
import javax.xml.xpath.XPathExpressionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;

/**
 * @author abramhall
 */
public class PostgresDashboard {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresDashboard.class);
    private static final PostgresDatabase db = PostgresDatabase.getInstance();
    private static final String ALL_REQUESTS_QUERY = "select request_id from results where response_type = 'ExecuteRequest';";
    private static final String RESPONSE_QUERY = "select request_id, request_date, response from results where request_id like ?;";

    private static final String OWS_NAMESPACE_URI = "http://www.opengis.net/ows/1.1";
    private static final String WPS_NAMESPACE_URI = "http://www.opengis.net/wps/1.0.0";

    private static final long MILLIS_PER_DAY = 1000 * 60 * 60 * 24;
    private static final long MILLIS_PER_HOUR = 1000 * 60 * 60;
    private static final long MILLIS_PER_MIN = 1000 * 60;

    public List<DashboardData> getDashboardData() {
        List<DashboardData> dataset = new ArrayList<DashboardData>();
        for (String request : getRequestIds()) {
            String baseRequestId = request.substring(4);
            dataset.add(buildDashboardData(baseRequestId));
        }
        return dataset;
    }

    public String getIdentifier(DashboardData data) {
        XPathXMLParser parser = getParser(data.getRequestXML());
        if (null == parser) {
            return "";
        }

        String wpsPrefix = parser.getContext().getPrefix(WPS_NAMESPACE_URI);
        String owsPrefix = parser.getContext().getPrefix(OWS_NAMESPACE_URI);

        StringBuilder xpath = new StringBuilder();
        xpath.append("/").append(wpsPrefix).append(":Execute/").append(owsPrefix).append(":Identifier");

        return parser.getString(xpath.toString());
    }

    public String getStatus(DashboardData data) {
        XPathXMLParser parser = getParser(data.getResponseXML());
        if (null == parser) {
            return "";
        }

        String wpsPrefix = parser.getContext().getPrefix(WPS_NAMESPACE_URI);
        StringBuilder xpath = new StringBuilder();
        xpath.append("/").append(wpsPrefix).append(":ExecuteResponse/").append(wpsPrefix).append(":Status//*");

        Node node = parser.getNode(xpath.toString());

        return node.getLocalName();
    }

    public String getStartTime(DashboardData data) {
        XPathXMLParser parser = getParser(data.getResponseXML());
        if (null == parser) {
            return "";
        }

        String startTime = parser.getString("//@creationTime");
        return DatatypeConverter.parseDateTime(startTime).getTime().toString();
    }

    public String getElapsedTime(DashboardData data) {
        XPathXMLParser parser = getParser(data.getResponseXML());
        if (null == parser) {
            return "";
        }

        long elapsed = 0;
        String startTime = parser.getString("//@creationTime");
        final long startTimeInMillis = DatatypeConverter.parseDateTime(startTime).getTimeInMillis();
        if (data.getOutputXML() == null) {
            elapsed = System.currentTimeMillis() - startTimeInMillis;
        } else {
            elapsed = Timestamp.valueOf(data.getCompletedTimestamp()).getTime() - startTimeInMillis;
        }

        StringBuilder elapsedTime = new StringBuilder();
        long days = elapsed / MILLIS_PER_DAY;
        if (days > 0) {
            elapsed = elapsed - days * MILLIS_PER_DAY;
            elapsedTime.append(days).append("d ");
        }
        long hours = elapsed / MILLIS_PER_HOUR;
        if (hours > 0) {
            elapsed = elapsed - hours * MILLIS_PER_HOUR;
            elapsedTime.append(hours).append("h ");
        }
        long minutes = elapsed / MILLIS_PER_MIN;
        if (minutes > 0) {
            elapsed = elapsed - minutes * MILLIS_PER_MIN;
            elapsedTime.append(minutes).append("m ");
        }
        long seconds = elapsed / 1000;
        elapsedTime.append(seconds).append("s");

        return elapsedTime.toString();
    }

    private XPathXMLParser getParser(String xml) {
        try {
            return new XPathXMLParser(xml);
        } catch (XPathExpressionException ex) {
            LOGGER.error("Failed to parse XML data", ex);
            return null;
        }
    }

    private DashboardData buildDashboardData(String baseRequestId) {
        LOGGER.info("build dashboard data for {}", baseRequestId);
        DashboardData data = new DashboardData();
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = db.getConnection().prepareStatement(RESPONSE_QUERY);
            pst.setString(1, "%" + baseRequestId + "%");
            rs = pst.executeQuery();
            while (rs.next()) {
                String requestId = rs.getString(1);
                String requestDate = rs.getString(2);
                String response = naivelyFormatXML(rs.getString(3));
                if (requestId.endsWith("OUTPUT")) {
                    LOGGER.info("setting outputXML {}", response);
                    data.setOutputXML(response);
                    data.setCompletedTimestamp(requestDate);
                } else if (requestId.startsWith("REQ_")) {
                    LOGGER.info("setting requestXML {}", response);
                    data.setRequestXML(response);
                } else {
                    LOGGER.info("setting responseXML {}", response);
                    data.setResponseXML(response);
                }
            }
        } catch (SQLException ex) {
            LOGGER.error("failed to select responses for request ids like " + baseRequestId, ex);
        } finally {
            closeStatement(pst);
            closeResultSet(rs);
        }
        return data;
    }

    private String naivelyFormatXML(String xml) {
        xml = xml.replaceAll("> *<", "><");
        xml = xml.replaceAll("><", ">" + System.lineSeparator() + "<");
        return xml;
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
