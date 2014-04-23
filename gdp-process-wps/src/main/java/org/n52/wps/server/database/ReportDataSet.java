package org.n52.wps.server.database;

/**
 * @author abramhall
 */
public class ReportDataSet {

    private String dataSetURI;
    private int count = 1;

    public ReportDataSet(String dataSetURI) {
        this.dataSetURI = dataSetURI;
    }

    public void incrementCount() {
        count++;
    }

    public String getDataSetURI() {
        return dataSetURI;
    }

}
