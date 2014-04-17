<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>

<%@ page contentType="text/html;charset=UTF-8"  %>
<% response.setHeader("Pragma", "no-cache");%>
<% response.setHeader("Cache-Control", "no-store");%>
<% response.setDateHeader("Expires", -1);%> 

<jsp:useBean id="dashboard" class="org.n52.wps.server.database.PostgresDashboard" />

<!DOCTYPE html>
<html>
    <body>
    <script type="text/javascript">
        function toggleData(id) {
            var showLink = document.getElementById("showLinkDiv" + id);
            var hideLink = document.getElementById("hideLinkDiv" + id);
            var dataDiv = document.getElementById("dataDiv" + id);
            if(showLink.style.display === "") {
                showLink.style.display = "none";
                hideLink.style.display = "";
                dataDiv.style.display = "";
            } else {
                showLink.style.display = "";
                hideLink.style.display = "none";
                dataDiv.style.display = "none";
            }
        }
    </script>
    <table border="1" cellpadding="1" width="100%">
            <tr>
                <th>Details</th>
                <th>Identifier</th>
                <th>Status</th>
                <th>Start Time</th>
                <th>Elapsed Time</th>
                <th>Output</th>
            </tr>
            <c:forEach items="${dashboard.dashboardData}" var="data" varStatus="theCount">
            <tr>
                <td>
                    <div id="showLinkDiv${theCount.index}">
                        <a href="#" onClick="javascript:toggleData(${theCount.index});">Show</a>
                    </div>
                    <div id="hideLinkDiv${theCount.index}" style="display:none;">
                        <a href="#" onClick="javascript:toggleData(${theCount.index});">Hide</a>
                    </div>
                </td>
                <td><c:catch>${dashboard.getIdentifier(data)}</c:catch></td>
                <td><c:catch>${dashboard.getStatus(data)}</c:catch></td>
                <td><c:catch>${dashboard.getStartTime(data)}</c:catch></td>
                <td><c:catch>${dashboard.getElapsedTime(data)}</c:catch></td>
                <td>${data.outputXML}</td>
            </tr>
            <tr id="dataDiv${theCount.index}" style="display:none">
                <td colspan="6"><pre>${dashboard.formatXMLForWebDisplay(data.requestXML)}</pre></td>
            </tr>
            </c:forEach>
        </ul>
    </body>
</html>
