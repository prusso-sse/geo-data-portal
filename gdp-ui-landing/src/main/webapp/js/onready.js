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
	var proxyEndpoint = 'proxy/';

	GDP.CONFIG.cswClient = new GDP.CSW({
		proxy: proxyEndpoint,
		url: GDP.CONFIG.hosts.csw
	});

	GDP.CONFIG.wpsClient = new GDP.WPS({
		proxy: proxyEndpoint,
		url: GDP.CONFIG.hosts.wps
	});

	GDP.CONFIG.ui = new GDP.UI();
});
