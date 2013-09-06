/*
 * Entry point for the application's JavaScript
 * Author: Ivan Suftin (isuftin@usgs.gov)
 */

// JSLint fixes
/*global document */
/*global $ */
/*global GDP */
/*global CSW */

$(document).ready(function () {
	"use strict";
	var UI = new GDP.UI(),
		csw = new GDP.CSW({
			proxy: 'proxy/',
			url: GDP.CONFIG.hosts.csw
		});

	csw.sendCSWGetCapabilitiesRequest({
		callbacks : {
			success : [
				function (data, textStatus, jqXHR) {
					var domDoc = Sarissa.getDomDocument();
					var test = (new DOMParser()).parseFromString(data, "text/xml");
					var a = 1;
				}
			],
			error : []
		}
	});
});
