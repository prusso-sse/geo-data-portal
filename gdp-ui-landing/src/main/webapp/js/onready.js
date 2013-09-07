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

	csw.requestGetCapabilities({
		callbacks : {
			success : [
				function (capablitiesXmlDoc) {
					var keywords = this.getCapabilitiesKeywords();
					this.getRecordsByKeywords({
						keywords : [
							'gov.usgs.cida.gdp.wps.algorithm.FeatureCoverageOPeNDAPIntersectionAlgorithm'
						]
					});
				}
			],
			error : [
				function (error) {
					throw error;
				}
			]
		}
	});

	wps.requestGetCapabilities({
		callbacks : {
			success : [
				function (capabilities) {
					var a = 1;
				}
			],
			error : [
				function (error) {
					throw error;
				}
			]
		}
	});
});
