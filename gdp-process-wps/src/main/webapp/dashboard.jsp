<%@ page contentType="text/html;charset=UTF-8"  %>
<% response.setHeader("Pragma", "no-cache");%>
<% response.setHeader("Cache-Control", "no-store");%>
<% response.setDateHeader("Expires", -1);%> 

<!DOCTYPE html>
<html>
    <head>
        <link rel="stylesheet" type="text/css" href="css/dashboard.css" />
        <jsp:include page="scripts.jsp" />
        <script src="js/dashboard.js"></script>
        <title>GDP Web Processing Service Dashboard</title>
    </head>
    <body>
        <p>Geo Data Portal Web Processing Service Dashboard</p>
        <input id="loadProcessesButton" type="button" value="Load Processes" />Last loaded: <span id="lastProcessLoad">never</span>
        <br />
        <div id="processData">Load processes...</div>
        <br />
        <br />
        <input id="reportButton" type="button" value="Run Report" />Last run: <span id="lastReportRun">never</span>
        <br />
        <div id="reportData">Run a report...</div>
    </body>
</html>