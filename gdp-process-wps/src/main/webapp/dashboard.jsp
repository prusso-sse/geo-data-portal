<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<%@ page contentType="text/html;charset=UTF-8"  %>
<% response.setHeader("Pragma", "no-cache");%>
<% response.setHeader("Cache-Control", "no-store");%>
<% response.setDateHeader("Expires", -1);%> 

<jsp:useBean id="dashboard" class="org.n52.wps.server.database.PostgresDashboard" />

<!DOCTYPE html>
<html>
    <body>
        <table border="1">
            <tr>
                <td>Request ID</td>
                <td>Request</td>
                <td>Response</td>
                <td>Output</td>
            </tr>
            <c:forEach items="${dashboard.dashboardData}" var="data">
            <tr>
                <td>${data.baseRequestId}</td>
                <td>${data.requestXML}</td>
                <td>${data.responseXML}</td>
                <td>${data.outputXML}</td>
            </tr>
            </c:forEach>
        </ul>
    </body>
</html>
