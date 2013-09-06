/*
 * Entry point for the application's JavaScript
 * Author: Ivan Suftin (isuftin@usgs.gov)
 */

// JSLint fixes
/*global document */
/*global $ */
/*global GDP */
/*global CSW */
/*global Sarissa */

$(document).ready(function () {
	"use strict";
	var proxyEndpoint = 'proxy/',
		UI = new GDP.UI(),
		csw = new GDP.CSW({
			proxy: proxyEndpoint,
			url: GDP.CONFIG.hosts.csw
		}),
		wps = new GDP.WPS({
			proxy: proxyEndpoint,
			url: GDP.CONFIG.hosts.wps
		});

	csw.sendCSWGetCapabilitiesRequest({
		callbacks : {
			success : [
				function (data, textStatus, jqXHR) {
					var oDomDoc = Sarissa.getDomDocument(),
						keywords;
					oDomDoc.setProperty("SelectionLanguage", "XPath");
					oDomDoc.setProperty("SelectionNamespaces",
						"xmlns:xhtml='http://www.w3.org/1999/xhtml' " +
						"xmlns:xsl='http://www.w3.org/1999/XSL/Transform' " +
						"xmlns:ows='http://www.opengis.net/ows' " +
						"xmlns:csw='http://www.opengis.net/cat/csw/2.0.2'");
					keywords = data.selectNodes('//ows:Keywords/ows:Keyword');
				}
			],
			error : []
		}
	});
});
