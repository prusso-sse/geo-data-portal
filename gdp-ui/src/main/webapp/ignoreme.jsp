<%@page contentType="text/html" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>JSP Page</title>
    </head>
    <body>
        <form action="/GDP_WEB/index.jsp" method="POST">
            <input type="hidden" name="wfs" value="http://my-beta.usgs.gov/catalogMaps/mapping/ows/502a77c2e4b01c6d34a57e81?service=wfs&request=getcapabilities&version=1.0.0" />
            <input type="submit" name="submit" value="Go!">
        </form>
    </body>
</html>