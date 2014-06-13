package gov.usgs.cida.gdp.wps.service.report;

import java.util.HashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author abramhall
*/
public class Report {

    private static final Logger log = LoggerFactory.getLogger(ReportAlgorithm.class);
    private final Map<String, ReportAlgorithm> algorithms = new HashMap<>();

    public void addAlgorithm(ReportAlgorithm algorithm, ReportDataSet dataSet) {
        ReportAlgorithm ra = algorithms.get(algorithm.getIdentifier());
        if (null == ra) {
            ra = algorithm;
        }
        ra.addDataSet(dataSet);
        algorithms.put(ra.getIdentifier(), ra.incrementCount());
    }
    
    public String toJSon() {
        StringBuilder json = new StringBuilder("{\"algorithms\":[");
        String prefix = "";
        for (ReportAlgorithm algorithm : algorithms.values()) {
            json.append(prefix);
            prefix = ",";
            json.append(algorithm.toJSON());
        }
        json.append("]}");
        return json.toString();
    }
}