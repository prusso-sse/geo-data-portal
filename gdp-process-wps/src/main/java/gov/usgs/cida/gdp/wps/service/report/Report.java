package gov.usgs.cida.gdp.wps.service.report;

import java.util.ArrayList;
import java.util.List;

/**
 * @author abramhall
 */
public class Report {

    private List<ReportAlgorithm> algorithms = new ArrayList<ReportAlgorithm>();

    public ReportAlgorithm addAlgorithm(String identifier) {
        ReportAlgorithm algorithm = null;
        boolean found = false;
        for (int i = 0; i < algorithms.size() && !found; i++) {
            algorithm = algorithms.get(i);
            if (algorithm.getIdentifier().equals(identifier)) {
                algorithm.incrementCount();
                found = true;
            }
        }
        if (!found) {
            algorithm = new ReportAlgorithm(identifier);
            algorithms.add(algorithm);
        }
        return algorithm;
    }
}
