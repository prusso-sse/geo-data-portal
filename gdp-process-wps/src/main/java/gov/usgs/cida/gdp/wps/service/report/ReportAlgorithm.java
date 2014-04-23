package gov.usgs.cida.gdp.wps.service.report;

import java.util.ArrayList;
import java.util.List;

/**
 * @author abramhall
 */
public class ReportAlgorithm {

    private String identifier;
    private int count = 1;
    List<ReportDataSet> dataSets = new ArrayList<ReportDataSet>();

    public ReportAlgorithm(String identifier) {
        this.identifier = identifier;
    }

    public void incrementCount() {
        count++;
    }

    public String getIdentifier() {
        return identifier;
    }

    public ReportDataSet addDataSet(String dataSetURI) {
        ReportDataSet dataSet = null;
        boolean found = false;
        for (int i = 0; i < dataSets.size() && !found; i++) {
            dataSet = dataSets.get(i);
            if (dataSet.getDataSetURI().equals(dataSetURI)) {
                dataSet.incrementCount();
                found = true;
            }
        }
        if (!found) {
            dataSet = new ReportDataSet(dataSetURI);
            dataSets.add(dataSet);
        }
        return dataSet;
    }
}
