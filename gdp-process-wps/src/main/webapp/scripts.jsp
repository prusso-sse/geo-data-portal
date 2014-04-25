<%@page import="java.net.URL"%>
<%@page import="java.io.File"%>
<%@page import="org.slf4j.LoggerFactory"%>
<%@page import="gov.usgs.cida.config.DynamicReadOnlyProperties"%>
<%!	protected DynamicReadOnlyProperties props = null;
	{
		try {
            URL resource = getClass().getClassLoader().getResource("application.properties");
            File applicationProperties = new File(resource.toURI());
			props = new DynamicReadOnlyProperties(applicationProperties);
			props = props.addJNDIContexts(new String[0]);
		} catch (Exception e) {
			LoggerFactory.getLogger("WEB-INF/jsp/scripts.jsp").error("Could not find JNDI - Application will probably not function correctly", e);
		}
	}
%>
<%
    Boolean dev = Boolean.parseBoolean(request.getParameter("development"));
    String jqueryVersion = props.getProperty("version.jquery", "2.1.0");
%>

<%-- JQuery + JQuery UI --%>
<script type="text/javascript" src="webjars/jquery/<%= jqueryVersion %>/jquery<%= dev ? "" : ".min" %>.js"></script>