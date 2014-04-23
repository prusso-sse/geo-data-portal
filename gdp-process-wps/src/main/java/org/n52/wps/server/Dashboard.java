package org.n52.wps.server;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import net.opengis.wps.v_1_0_0.Execute;
import net.opengis.wps.v_1_0_0.ExecuteResponse;
import net.opengis.wps.v_1_0_0.InputType;
import net.opengis.wps.v_1_0_0.StatusType;
import org.n52.wps.server.database.DashboardData;
import org.n52.wps.server.database.PostgresDatabase;
import org.n52.wps.server.database.Report;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author abramhall
 */
public class Dashboard extends HttpServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(Dashboard.class);
    private static final PostgresDatabase db = PostgresDatabase.getInstance();
    private static final String ALL_REQUESTS_QUERY = "select request_id from results where response_type = 'ExecuteRequest' order by request_date;";
    private static final String DATA_QUERY = "select request_id, request_date, response from results where request_id like ?;";
    private static final String RESPONSE_QUERY = "select response from results where request_id = ?;";
    private static final String WPS_NAMESPACE = "net.opengis.wps.v_1_0_0";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("called do get");
        resp.getWriter().write("called do get");
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("do post");
        String action = req.getParameter("action");
        StringBuilder out = new StringBuilder();
        if ("loadProcesses".equalsIgnoreCase(action)) {
            String startAt = req.getParameter("startAt");
            System.out.println("load processes, param startAt = " + startAt);
            String json = new Gson().toJson(getDashboardData());
            out.append(json);
        } else if ("report".equalsIgnoreCase(action)) {
            System.out.println("in post, report action");

            Report report = new Report();

            for (String requestId : getRequestIds()) {
                PreparedStatement pst = null;
                ResultSet rs = null;
                String identifier = null;
                String dataSetURI = null;
                try {
                    pst = db.getConnection().prepareStatement(RESPONSE_QUERY);
                    pst.setString(1, requestId);
                    rs = pst.executeQuery();
                    while (rs.next()) {
                        String xml = rs.getString(1);

                        JAXBContext context = JAXBContext.newInstance(WPS_NAMESPACE);
                        Unmarshaller unmarshaller = context.createUnmarshaller();
                        StreamSource source = new StreamSource(new StringReader(xml));
                        JAXBElement<ExecuteResponse> wpsExecuteResponseElement = unmarshaller.unmarshal(source, ExecuteResponse.class);

                        for (InputType inputType : wpsExecuteResponseElement.getValue().getDataInputs().getInput()) {
                            if ("DATASET_URI".equals(inputType.getIdentifier().getValue())) {
                                dataSetURI = inputType.getData().getLiteralData().getValue();
                            }
                        }
                        identifier = getIdentifier(xml);
                    }
                } catch (SQLException ex) {
                    LOGGER.error("failed to retrieve response", ex);
                } catch (JAXBException ex) {
                    LOGGER.error("failed to retrieve response", ex);
                } finally {
                    closeResultSet(rs);
                    closeStatement(pst);
                }
                report.addAlgorithm(identifier).addDataSet(dataSetURI);
            }
            final String toJson = new Gson().toJson(report);
            System.out.println(toJson);
            out.append(toJson);
        }
        resp.getWriter().write(out.toString());
    }

    private List<DashboardData> getDashboardData() {
        List<DashboardData> dataset = new ArrayList<DashboardData>();
        for (String request : getRequestIds()) {
            String baseRequestId = request.substring(4);
            DashboardData dashboardData = buildDashboardData(baseRequestId);
            dataset.add(dashboardData);
        }
        return dataset;
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

    private DashboardData buildDashboardData(String baseRequestId) {
        LOGGER.debug("build dashboard data for {}", baseRequestId);
        DashboardData data = new DashboardData();
        Long startTime = null;
        long endTime = System.currentTimeMillis();
        PreparedStatement pst = null;
        ResultSet rs = null;
        try {
            pst = db.getConnection().prepareStatement(DATA_QUERY);
            pst.setString(1, "%" + baseRequestId + "%");
            rs = pst.executeQuery();
            while (rs.next()) {
                String requestId = rs.getString(1);
                String requestDate = rs.getString(2);
                String xml = rs.getString(3);
                if (requestId.endsWith("OUTPUT")) {
                    endTime = Timestamp.valueOf(requestDate).getTime();
                } else if (requestId.startsWith("REQ_")) {
                    final String identifier = getIdentifier(xml);
                    data.setIdentifier(identifier);
                } else {
                    final String status = getStatus(xml);
                    startTime = getStartTime(xml);
                    data.setStatus(status);
                    data.setCreationTime(startTime);
                }
            }
            if (null != startTime) {
                data.setElapsedTime(endTime - startTime);
            }
        } catch (SQLException ex) {
            LOGGER.error("failed to select responses for request ids like " + baseRequestId, ex);
        } catch (JAXBException ex) {
            LOGGER.error("failed to select responses for request ids like " + baseRequestId, ex);
        } finally {
            closeStatement(pst);
            closeResultSet(rs);
        }
        return data;
    }

    private String getIdentifier(String xml) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(WPS_NAMESPACE);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        StreamSource source = new StreamSource(new StringReader(xml));
        JAXBElement<Execute> wpsExecuteElement = unmarshaller.unmarshal(source, Execute.class);
        Execute execute = wpsExecuteElement.getValue();
        String identifier = execute.getIdentifier().getValue();

        return identifier.substring(identifier.lastIndexOf(".") + 1);
    }

    private String getStatus(String xml) throws JAXBException {
        StringBuilder status = new StringBuilder();
        final StatusType statusElement = getStatusElement(xml);
        if (statusElement.isSetProcessAccepted()) {
            status.append("Accepted");
        } else if (statusElement.isSetProcessFailed()) {
            status.append("Failed");
        } else if (statusElement.isSetProcessPaused()) {
            status.append("Paused");
        } else if (statusElement.isSetProcessStarted()) {
            status.append("Started");
        } else if (statusElement.isSetProcessSucceeded()) {
            status.append("Succeeded");
        }
        return status.toString();
    }

    private StatusType getStatusElement(String xml) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(WPS_NAMESPACE);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        StreamSource source = new StreamSource(new StringReader(xml));
        JAXBElement<ExecuteResponse> executeResponseElement = unmarshaller.unmarshal(source, ExecuteResponse.class);
        ExecuteResponse executeResponse = executeResponseElement.getValue();
        return executeResponse.getStatus();
    }

    private long getStartTime(String xml) throws JAXBException {
        final StatusType statusElement = getStatusElement(xml);
        Calendar start = DatatypeConverter.parseDateTime(statusElement.getCreationTime().toString());
        return start.getTimeInMillis();
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

    private String formatXMLForWebDisplay(String xml) {
        String formattedXML = xml.replaceAll(">\\s+<", "><");
        formattedXML = formattedXML.replaceAll("><", ">" + System.lineSeparator() + "<");
        formattedXML = formattedXML.replaceAll(">", "&gt;");
        formattedXML = formattedXML.replaceAll("<", "&lt;");
        return formattedXML;
    }
}
