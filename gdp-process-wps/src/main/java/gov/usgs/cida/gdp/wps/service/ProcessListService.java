package gov.usgs.cida.gdp.wps.service;

import com.google.gson.Gson;
import java.io.IOException;
import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.DatatypeConverter;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import net.opengis.wps.v_1_0_0.ExecuteResponse;
import net.opengis.wps.v_1_0_0.StatusType;
import org.n52.wps.server.database.PostgresDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author abramhall
 */
public class ProcessListService extends BaseProcessServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessListService.class);
    private static final String DATA_QUERY = "select request_id, request_date, response from results where request_id like ?;";
    private static final String REQUEST_PREFIX = "REQ_";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try {
            String json = new Gson().toJson(getDashboardData());
            resp.setContentType("application/json");
            resp.getWriter().write(json);
        } catch (SQLException | JAXBException ex) {
            LOGGER.error("Failed to retrieve or unmarshall data", ex);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to retrieve or unmarshall data: " + ex);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "This servlet is read only. Try using Get.");
    }

    private List<DashboardData> getDashboardData() throws SQLException, JAXBException {
        List<DashboardData> dataset = new ArrayList<>();
        for (String request : getRequestIds()) {
            String baseRequestId = request.substring(REQUEST_PREFIX.length());
            DashboardData dashboardData = buildDashboardData(baseRequestId);
            dataset.add(dashboardData);
        }
        return dataset;
    }

    private DashboardData buildDashboardData(String baseRequestId) throws SQLException, JAXBException {
        DashboardData data = new DashboardData();
        long startTime = -1;
        long endTime = System.currentTimeMillis();
        try (PreparedStatement pst = PostgresDatabase.getInstance().getConnection().prepareStatement(DATA_QUERY)) {
            pst.setString(1, "%" + baseRequestId + "%");
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    String requestId = rs.getString(1);
                    String requestDate = rs.getString(2);
                    String xml = rs.getString(3);
                    if (requestId.toUpperCase().endsWith("OUTPUT")) {
                        endTime = Timestamp.valueOf(requestDate).getTime();
                        data.setOutput(xml);
                    } else if (requestId.startsWith(REQUEST_PREFIX)) {
                        String identifier = getIdentifier(xml);
                        data.setIdentifier(identifier);
                    } else {
                        String status = getStatus(xml);
                        startTime = getStartTime(xml);
                        data.setStatus(status);
                        data.setCreationTime(startTime);
                    }
                }
                if (startTime != -1) {
                    data.setElapsedTime(endTime - startTime);
                }
            }
        }
        return data;
    }

    private String getStatus(String xml) throws JAXBException {
        StringBuilder status = new StringBuilder();
        StatusType statusElement = getStatusElement(xml);
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
}
