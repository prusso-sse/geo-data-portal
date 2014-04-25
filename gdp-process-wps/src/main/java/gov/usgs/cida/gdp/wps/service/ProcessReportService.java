package gov.usgs.cida.gdp.wps.service;

import com.google.gson.Gson;
import gov.usgs.cida.gdp.wps.service.report.Report;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import net.opengis.wps.v_1_0_0.Execute;
import net.opengis.wps.v_1_0_0.ExecuteResponse;
import net.opengis.wps.v_1_0_0.InputType;
import org.n52.wps.server.database.PostgresDatabase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author abramhall
 */
public class ProcessReportService extends HttpServlet {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessReportService.class);
    private static final String ALL_REQUESTS_QUERY = "select request_id from results where response_type = 'ExecuteRequest' order by request_date;";
    private static final String RESPONSE_QUERY = "select response from results where request_id = ANY ( ? );";
    private static final String WPS_NAMESPACE = "net.opengis.wps.v_1_0_0";

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try (Connection conn = PostgresDatabase.getInstance().getConnection(); PreparedStatement pst = conn.prepareStatement(RESPONSE_QUERY)) {
            Report report = new Report();
            Unmarshaller unmarshaller = null;
            pst.setArray(1, conn.createArrayOf("varchar", getRequestIds().toArray()));
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    String dataSetURI = null;
                    String xml = rs.getString(1);
                    StreamSource source = new StreamSource(new StringReader(xml));
                    if (null == unmarshaller) {
                        unmarshaller = JAXBContext.newInstance(WPS_NAMESPACE).createUnmarshaller();
                    }
                    JAXBElement<ExecuteResponse> wpsExecuteResponseElement = unmarshaller.unmarshal(source, ExecuteResponse.class);
                    for (InputType inputType : wpsExecuteResponseElement.getValue().getDataInputs().getInput()) {
                        if ("DATASET_URI".equals(inputType.getIdentifier().getValue())) {
                            dataSetURI = inputType.getData().getLiteralData().getValue();
                        }
                    }
                    report.addAlgorithm(getIdentifier(xml)).addDataSet(dataSetURI);
                }
                String json = new Gson().toJson(report);
                json = json.replaceFirst("\\{", "{\"dateExcecuted\" : 5,");
                resp.setContentType("application/json");
                resp.getWriter().write(json);
            }
        } catch (SQLException | JAXBException ex) {
            LOGGER.error("Failed to retrieve or unmarshall data", ex);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to retrieve or unmarshall data.");
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "This servlet is read only. Try using Get.");
    }

    private List<String> getRequestIds() throws SQLException {
        List<String> request_ids = new ArrayList<>();
        try (Statement st = PostgresDatabase.getInstance().getConnection().createStatement(); ResultSet rs = st.executeQuery(ALL_REQUESTS_QUERY)) {
            while (rs.next()) {
                String id = rs.getString(1);
                request_ids.add(id);
            }
        }

        return request_ids;
    }

    private String getIdentifier(String xml) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(WPS_NAMESPACE);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        StreamSource source = new StreamSource(new StringReader(xml));
        JAXBElement<Execute> wpsExecuteElement = unmarshaller.unmarshal(source, Execute.class
        );
        Execute execute = wpsExecuteElement.getValue();
        String identifier = execute.getIdentifier().getValue();

        return identifier.substring(identifier.lastIndexOf(".") + 1);
    }
}
