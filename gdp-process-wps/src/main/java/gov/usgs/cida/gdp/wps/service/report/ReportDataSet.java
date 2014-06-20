package gov.usgs.cida.gdp.wps.service.report;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
* @author abramhall
*/
public class ReportDataSet {

    private static final Logger log = LoggerFactory.getLogger(ReportAlgorithm.class);
    private int count = 0;
    private final String dataSetURI;

    public ReportDataSet(String dataSetURI) {
        this.dataSetURI = dataSetURI;
    }
    
    public ReportDataSet incrementCount() {
        count++;
        return this;
    }

    public String getDataSetURI() {
        return dataSetURI;
    }

    public String toJSON() {
        StringBuilder json = new StringBuilder();
        json.append("{");
        json.append("\"dataSetURI\":").append("\"").append(dataSetURI).append("\"");
        json.append(",");
        json.append("\"count\":").append("\"").append(count).append("\"");
        json.append("}");
        return json.toString();
    }
}