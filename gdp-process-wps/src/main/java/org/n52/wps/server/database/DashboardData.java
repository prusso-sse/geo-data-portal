/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.n52.wps.server.database;

/**
 *
 * @author abramhall
 */
public class DashboardData {

    private String requestId = null;
    private String responseId = null;
    private String outputId = null;

    public DashboardData(String requestId, String responseId, String outputId) {
        this.requestId = requestId;
        this.responseId = responseId;
        this.outputId = outputId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getResponseId() {
        return responseId;
    }

    public String getOutputId() {
        return outputId;
    }

}
