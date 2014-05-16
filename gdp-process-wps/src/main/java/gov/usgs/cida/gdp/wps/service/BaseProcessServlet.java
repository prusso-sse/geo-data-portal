package gov.usgs.cida.gdp.wps.service;

import java.io.StringReader;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.servlet.http.HttpServlet;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import net.opengis.wps.v_1_0_0.Execute;
import org.n52.wps.server.database.PostgresDatabase;

/**
 * @author abramhall
 */
public abstract class BaseProcessServlet extends HttpServlet {

    protected static final String WPS_NAMESPACE = "net.opengis.wps.v_1_0_0";
    private static final int DEFAULT_OFFSET = 0;
    private static final int DEFAULT_LIMIT = 50;
    private static final int LIMIT_PARAM_INDEX = 1;
    private static final int OFFSET_PARAM_INDEX = 2;
    private static final String REQUESTS_QUERY = "select request_id from results where response_type = 'ExecuteRequest' order by request_date desc limit ? offset ?;";

    /**
     * @return The latest
     * {@value gov.usgs.cida.gdp.wps.service.BaseProcessServlet#DEFAULT_LIMIT}
     * ExecuteRequest request ids
     * @throws SQLException
     */
    protected final List<String> getRequestIds() throws SQLException {
        return getRequestIds(DEFAULT_LIMIT, DEFAULT_OFFSET);
    }

    /**
     *
     * @param limit the max number of results to return
     * @param offset which row of the query results to start returning at
     * @return a list of ExecuteRequest request ids
     * @throws SQLException
     */
    protected final List<String> getRequestIds(int limit, int offset) throws SQLException {
        List<String> request_ids = new ArrayList<>();
        try (Connection conn = PostgresDatabase.getInstance().getConnection(); PreparedStatement pst = conn.prepareStatement(REQUESTS_QUERY)) {
            pst.setInt(LIMIT_PARAM_INDEX, limit);
            pst.setInt(OFFSET_PARAM_INDEX, offset);
            try (ResultSet rs = pst.executeQuery()) {
                while (rs.next()) {
                    String id = rs.getString(1);
                    request_ids.add(id);
                }
            }
        }
        return request_ids;
    }

    protected final String getIdentifier(String xml) throws JAXBException {
        JAXBContext context = JAXBContext.newInstance(WPS_NAMESPACE);
        Unmarshaller unmarshaller = context.createUnmarshaller();
        StreamSource source = new StreamSource(new StringReader(xml));
        JAXBElement<Execute> wpsExecuteElement = unmarshaller.unmarshal(source, Execute.class);
        Execute execute = wpsExecuteElement.getValue();
        String identifier = execute.getIdentifier().getValue();
        return identifier.substring(identifier.lastIndexOf(".") + 1);
    }
}
