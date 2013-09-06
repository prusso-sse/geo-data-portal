<%@page import="org.slf4j.LoggerFactory"%>
<%@page import="gov.usgs.cida.config.DynamicReadOnlyProperties"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<%!	protected DynamicReadOnlyProperties props = null;

	{
		try {
			props = new DynamicReadOnlyProperties();
			props = props.addJNDIContexts(new String[0]);
		} catch (Exception e) {
			LoggerFactory.getLogger("WEB-INF/jsp/scripts.jsp").error("Could not find JNDI - Application will probably not function correctly");
		}
	}
%>
<% Boolean dev = Boolean.parseBoolean(request.getParameter("development")); %>
<script type="text/javascript" src="webjars/jquery/1.9.0/jquery<%= dev ? "" : ".min"%>.js"></script>

<%-- Bootstrap --%>
<link type="text/css" rel="stylesheet" href="webjars/bootstrap/3.0.0/css/bootstrap<%= dev ? "" : ".min"%>.css" />
<link type="text/css" rel="stylesheet" href="webjars/bootstrap/3.0.0/css/bootstrap-theme<%= dev ? "" : ".min"%>.css" />
<script type="text/javascript" src="webjars/bootstrap/3.0.0/js/bootstrap<%= dev ? "" : ".min"%>.js"></script>

<%-- OpenLayers --%>
<script type="text/javascript" src="webjars/openlayers/2.13.1/OpenLayers<%= dev ? ".debug" : ""%>.js"></script>

<%-- Sarissa XML parsing library --%>
<script type="text/javascript" src="js/sarissa/sarissa.js"></script>
<script type="text/javascript" src="js/sarissa/sarissa_ieemu_xpath.js"></script>

<%-- CSW Client --%>
<script type="text/javascript" src="lib/scripts/cswclient.js"></script>
<script type="text/javascript" src="lib/scripts/gdp-cswclient-update.js"></script>
<link rel="stylesheet" href="lib/css/cswclient.css" type="text/css" />
<link rel="stylesheet" href="css/excat/cswclient-overlay.css" type="text/css" />

<%-- App-specific JS --%>
<script type="text/javascript" src="js/ui.js"></script>
<script type="text/javascript" src="js/onready.js"></script>
<script type="text/javascript" src="js/ogc/CSW.js"></script>
<script type="text/javascript" src="js/ogc/WPS.js"></script>

<%-- Local configuration --%>
<script type="text/javascript">
	GDP = GDP || {};
	GDP.CONFIG = {
		hosts : {
			csw : '<%= props.getProperty("gdp.endpoint.csw.url") %>',
			wps : '<%= props.getProperty("gdp.endpoint.wps.process.url") %>'
		}
	};
</script>