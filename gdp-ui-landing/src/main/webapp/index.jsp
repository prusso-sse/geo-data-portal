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
						The increasing availability of downscaled climate projections 
						and other large data products that summarize or predict climate 
						and land use conditions, is making use of these data more common 
						in research and management. Scientists and decisionmakers often 
						need to construct ensembles and compare climate hindcasts and future 
						projections for particular spatial areas. These tasks generally 
						require an investigator to procure all datasets of interest en masse, 
						integrate the various data formats and representations into commonly 
						accessible and comparable formats, and then extract the subsets of the 
						datasets that are actually of interest.
						This process can be challenging 
						and time intensive due to data-transfer, -storage, and(or) -processing 
						limits, or unfamiliarity with methods of accessing climate and land use 
						data. Data management for modeling and assessing the impacts of future 
						climate conditions is also becoming increasingly expensive due to the 
						size of the datasets. The Geo Data Portal addresses these limitations, 
						making access to numerous climate datasets for particular areas of 
						interest a simple and efficient task.</p>

					<p>
						This page is a catalog of the datasets that have been tested to 
						work well for access with the Geo Data Portal. Select one of the 
						buttons below to see a list of these datasets. At its core, the 
						Geo Data Portal is an advanced Open Geospatial Consortium Web 
						Processing Service that can be used in a wide variety of 
						applications against any web-accessible standards-compliant 
						dataset. If you'd like to see all the datasets that are 
						compatible with one of the processing types the Geo Data 
						Portal can perform, select one of those buttons below.
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
					<div id="col-csw-information-title" class="text-center col-md-12">
						<p id="p-csw-information-title" class="lead"></p>
					</div>
					<div id="col-csw-information-content" class="col-md-12">
						<p id="p-csw-information-content"></p>
					</div>
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
					<div id="col-wps-information-content" class="col-md-12">
						<p id="p-wps-information-content"></p>
					</div>
				</div>
			</div>

			<div id="row-proceed" class="row text-center">
				<button id="btn-proceed" class="btn btn-success">Process Data with the Geo Data Portal</button>
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