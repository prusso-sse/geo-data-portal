<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%
	Boolean dev = Boolean.parseBoolean(request.getParameter("development"));
%>
<script type="text/javascript" src="webjars/jquery/1.9.0/jquery<%= dev ? "" : ".min"%>.js"></script>
<link type="text/css" rel="stylesheet" href="webjars/bootstrap/3.0.0/css/bootstrap<%= dev ? "" : ".min"%>.css" />
<link type="text/css" rel="stylesheet" href="webjars/bootstrap/3.0.0/css/bootstrap-theme<%= dev ? "" : ".min"%>.css" />
<script type="text/javascript" src="webjars/bootstrap/3.0.0/js/bootstrap<%= dev ? "" : ".min"%>.js"></script>
<script type="text/javascript" src="webjars/openlayers/2.13.1/OpenLayers<%= dev ? ".debug" : ""%>.js"></script>
<script type="text/javascript" src="js/ui.js"></script>
<script type="text/javascript" src="js/onready.js"></script>