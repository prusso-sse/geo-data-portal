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
        <input id="loadProcessesButton" type="button" value="Load Processes" />
        <div id="processTableContainer"></div>
        <br />
        <br />
        <input id="reportButton" type="button" value="Get Report" />
        <div id="reportContainer"></div>
    </body>
</html>