<%@ page contentType="text/html;charset=UTF-8"  %>
<% response.setHeader("Pragma", "no-cache");%>
<% response.setHeader("Cache-Control", "no-store");%>
<% response.setDateHeader("Expires", -1);%> 

<%@ page import="java.util.List"%>
<%@ page import="org.n52.wps.server.database.*" %>

<%! 
    protected PostgresDashboard dashboard = new PostgresDashboard();
    List<DashboardData> dataset = dashboard.getDashboardData();
%>
<%--
--%>
<!DOCTYPE html>
<html>
    <body>
        <table border="1">
            <tr>
                <td>Request</td>
                <td>Response</td>
                <td>Output</td>
            </tr>
            <%for (DashboardData data : dataset) { %>
            <tr>
                <td><%= dashboard.parseResponse(data.getRequestId()) %></td>
                <td><%= dashboard.parseResponse(data.getResponseId()) %></td>
                <td><%= dashboard.parseResponse(data.getOutputId()) %></td>
            </tr>
            <% } %>
        </ul>
    </body>
</html>
