package org.n52.wps.server.database;

/**
 * @author abramhall
 */
public class DashboardData {

    private String baseRequestId = null;
    private String requestXML = null;
    private String responseXML = null;
    private String outputXML = null;

    public DashboardData(String baseRequestId) {
        this.baseRequestId = baseRequestId;
    }

    public String getBaseRequestId() {
        return baseRequestId;
    }

    public String getRequestXML() {
        return requestXML;
    }

    public String getResponseXML() {
        return responseXML;
    }

    public String getOutputXML() {
        return outputXML;
    }

    public void setBaseRequestId(String baseRequestId) {
        this.baseRequestId = baseRequestId;
    }

    public void setRequestXML(String requestXML) {
        this.requestXML = requestXML;
    }

    public void setResponseXML(String responseXML) {
        this.responseXML = responseXML;
    }

    public void setOutputXML(String outputXML) {
        this.outputXML = outputXML;
    }

}
