package gov.usgs.cida.gdp.wps.service;

import gov.usgs.cida.gdp.wps.service.report.Report;
import gov.usgs.cida.gdp.wps.service.report.ReportAlgorithm;
import gov.usgs.cida.gdp.wps.service.report.ReportDataSet;
import java.io.IOException;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import net.opengis.wps.v_1_0_0.ExecuteResponse;
import net.opengis.wps.v_1_0_0.InputType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author abramhall
*/
public class ProcessReportService extends BaseProcessServlet {
    
    private static final Logger LOGGER = LoggerFactory.getLogger(ProcessReportService.class);
    private static final String RESPONSE_QUERY = "select response from results where request_id = ANY ( ? );";
    private static final int RESPONSE_QUERY_RESPONSE_COLUMN_INDEX = 1;
    private static final int RESPONSE_QUERY_REQUEST_ID_LIST_PARAM_INDEX = 1;
    
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        try (Connection conn = getConnection(); PreparedStatement pst = conn.prepareStatement(RESPONSE_QUERY)) {
            Report report = new Report();
            Unmarshaller unmarshaller = null;
            pst.setArray(RESPONSE_QUERY_REQUEST_ID_LIST_PARAM_INDEX, conn.createArrayOf("varchar", getRequestIds().toArray()));
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    String dataSetURI = null;
                    String xml = removeUTF8BOM(rs.getString(RESPONSE_QUERY_RESPONSE_COLUMN_INDEX));
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
                    String identifier = getIdentifier(xml);
                    ReportDataSet dataSet = new ReportDataSet(dataSetURI);
                    ReportAlgorithm algorithm = new ReportAlgorithm(identifier);
                    report.addAlgorithm(algorithm, dataSet);
                }
                resp.setContentType("application/json");
                resp.getWriter().write(report.toJSon());
            }
        } catch (SQLException | JAXBException ex) {
            LOGGER.error("Failed to retrieve or unmarshall data", ex);
            resp.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to retrieve or unmarshall data: " + ex);
        }
    }
    
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        resp.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "This servlet is read only. Try using Get.");
    }
}