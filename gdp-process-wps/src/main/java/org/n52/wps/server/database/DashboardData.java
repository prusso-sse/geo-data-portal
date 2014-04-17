package org.n52.wps.server.database;

/**
 * @author abramhall
 */
public class DashboardData {

    private String requestXML = null;
    private String responseXML = null;
    private String outputXML = null;
    private String completedTimestamp = null;

    public String getRequestXML() {
        return requestXML;
    }

    public void setRequestXML(String requestXML) {
        this.requestXML = requestXML;
    }

    public String getResponseXML() {
        return responseXML;
    }

    public void setResponseXML(String responseXML) {
        this.responseXML = responseXML;
    }

    public String getOutputXML() {
        return outputXML;
    }

    public void setOutputXML(String outputXML) {
        this.outputXML = outputXML;
    }

    public String getCompletedTimestamp() {
        return completedTimestamp;
    }

    public void setCompletedTimestamp(String completedTimestamp) {
        this.completedTimestamp = completedTimestamp;
    }

}
