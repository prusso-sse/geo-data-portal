<%@page import="gov.usgs.cida.config.DynamicReadOnlyProperties"%>
<%@page import="org.slf4j.LoggerFactory"%>
<%@page import="java.io.File"%>
<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE HTML>
<%!	protected DynamicReadOnlyProperties props = null;

	{
		try {
			props = new DynamicReadOnlyProperties();
			props = props.addJNDIContexts(new String[0]);
		} catch (Exception e) {
			LoggerFactory.getLogger("index.jsp").error("Could not find JNDI - Application will probably not function correctly");
		}
	}
	boolean development = Boolean.parseBoolean(props.getProperty("development"));
%>
<html lang="en">
    <head>
        <META HTTP-EQUIV="CONTENT-TYPE" CONTENT="text/html; charset=UTF-8" />
		<meta HTTP-EQUIV="X-UA-Compatible" CONTENT="IE=edge">
        <meta NAME="viewport" CONTENT="width=device-width, initial-scale=1.0">
        <link rel="shortcut icon" href="favicon.ico" type="image/x-icon" />
        <link rel="icon" href="favicon.ico" type="image/x-icon" />
		<!-- HTML5 shim and Respond.js IE8 support of HTML5 elements and media queries -->
		<!--[if lt IE 9]>
		  <script src="../../assets/js/html5shiv.js"></script>
		  <script src="../../assets/js/respond.min.js"></script>
		<![endif]-->
        <jsp:include page="template/USGSHead.jsp">
            <jsp:param name="relPath" value="" />
            <jsp:param name="shortName" value="USGS Geo Data Portal" />
            <jsp:param name="title" value="USGS Geo Data Portal" />
            <jsp:param name="description" value="" />
            <jsp:param name="author" value="Ivan Suftin" />
            <jsp:param name="keywords" value="" />
            <jsp:param name="publisher" value="" />
            <jsp:param name="revisedDate" value="" />
            <jsp:param name="nextReview" value="" />
            <jsp:param name="expires" value="never" />
            <jsp:param name="development" value="<%= development%>" />
        </jsp:include>
    </head>
    <body>
		<div id="overlay">
			<div id="overlay-content">
				Application Loading
			</div>
		</div>
		<div class="container">
			<div class="row">
				<jsp:include page="template/USGSHeader.jsp">
                    <jsp:param name="relPath" value="" />
                    <jsp:param name="header-class" value="" />
                    <jsp:param name="site-title" value="USGS Geo Data Portal" />
                </jsp:include>
			</div>

			<div id="row-application-intro-text" class="row">
				<div class="well">
					<p>
						At vero eos et accusamus et iusto odio dignissimos ducimus 
						qui blanditiis praesentium voluptatum deleniti atque corrupti 
						quos dolores et quas molestias excepturi sint occaecati 
						cupiditate non provident, similique sunt in culpa qui officia 
						deserunt mollitia animi, id est laborum et dolorum fuga. 
					</p>
					<br /><br />
					<p>
						Et harum quidem rerum facilis est et expedita distinctio. 
						Nam libero tempore, cum soluta nobis est eligendi optio cumque 
						nihil impedit quo minus id quod maxime placeat facere possimus, 
						omnis voluptas assumenda est, omnis dolor repellendus. 
					</p>
				</div>
			</div>

			<div id="row-choice-start" class="row">
				<div id="col-choice-start" class="col-md-12 text-center">
					<div class="btn-group" data-toggle="buttons">
						<label class="btn btn-primary">
							<input type="radio" id="btn-choice-algorithm" name="btn-choice-start" />Begin With An Algorithm
						</label>
						<label class="btn btn-primary">
							<input type="radio" id="btn-choice-dataset" name="btn-choice-start" />Begin With A Dataset
						</label>
					</div>
				</div>
			</div>

			<div id="row-start-instructions" class="row">
				<div class="well">
					<div id="div-start-instructions">
						<div id="col-start-instructions-title" class="col-md-12 text-center">
							<p id="p-start-instructions-title" class="lead"></p>
						</div>
						<div id="p-start-instructions-content" class="col-md-12"></div>
					</div>
				</div>
			</div>

			<div id="row-csw-select" class="row">
				<div id="col-csw-select" class="col-md-6">
					<label>Select Dataset
						<select id="form-control-select-csw" class="form-control"></select>
					</label>
				</div>
				<div id="col-csw-information" class="col-md-6">
					<div id="col-csw-information-title" class="text-center col-md-12"></div>
					<div id="col-csw-information-content" class="text-center col-md-12"></div>
				</div>
			</div>
			<div id="row-wps-select" class="row">
				<div id="col-wps-select" class="col-md-6">
					<label>Select Algorithm
						<select id="form-control-select-wps" class="form-control"></select>
					</label>
				</div>
				<div id="col-wps-information" class="col-md-6">
					<div id="col-wps-information-title" class="text-center col-md-12">
						<p id="p-wps-information-title" class="lead"></p>
					</div>
					<div id="col-wps-information-content" class="text-center col-md-12">
						<p id="p-wps-information-content" class="lead"></p>
					</div>
				</div>
			</div>
			<div class="row">
				<jsp:include page="template/USGSFooter.jsp">
                    <jsp:param name="relPath" value="" />
                    <jsp:param name="header-class" value="" />
                    <jsp:param name="site-url" value="<script type='text/javascript'>document.write(document.location.href);</script>" />
                    <jsp:param name="contact-info" value="<a href='mailto:gdp@usgs.gov?Subject=GDP%20Derivative%20Portal%20Help%20Request'>Contact the Geo Data Portal team</a>" />
                </jsp:include>
			</div>
		</div>
		<jsp:include page="WEB-INF/jsp/scripts.jsp">
			<jsp:param name="development" value="<%= development%>" />
		</jsp:include>
    </body>
</html>