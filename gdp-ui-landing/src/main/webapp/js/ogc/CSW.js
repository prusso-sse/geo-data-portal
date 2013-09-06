// JSLint fixes
/*jslint plusplus: true */
/*global $ */
var GDP = GDP || {};
GDP.CSW = function (args) {
	"use strict";
	args = args || {};
	this.url = args.url;
	this.cache = args.cache || {};
	this.proxy = args.proxy;
	this.capabilitiesMap = args.capabilitiesMap || {};

	/**
	 * Create a CSW GetCapabilities request
	 * @param args {Object}
	 */
	var sendCSWGetCapabilitiesRequest = function (args) {
		args = args || {};
		var callbacks = args.callbacks || {
			success: [],
			error: []
		},
			cachedGetCaps = parseInt(this.cache.getcaps, 10),
			scbInd,
			url = args.url || this.url,
			proxy = args.proxy || this.proxy,
			capabilitiesMap = args.capabilitiesMap || this.capabilitiesMap,
			scope = args.scope || this;

		// Check to see if there's a capabilities document available in cache
		if (cachedGetCaps && capabilitiesMap[url]) {
			for (scbInd = 0; scbInd < callbacks.success.length; scbInd++) {
				callbacks.success[scbInd].call(this, capabilitiesMap[url]);
			}
		} else {
			$.ajax({
				url: proxy ? proxy + url : url,
				type: 'get',
				contentType: 'text/xml',
				data: {
					'request': 'GetCapabilities',
					'service': 'CSW',
					'version': '2.0.2'
				},
				success: function (data, textStatus, jqXHR) {
					if (cachedGetCaps) {
						scope.capabilitiesMap[url] = data;
					}

					if (callbacks.success) {
						for (scbInd = 0; scbInd < callbacks.success.length; scbInd++) {
							callbacks.success[scbInd].call(scope, data, textStatus, jqXHR);
						}
					}
				},
				error: function (jqXHR, textStatus, errorThrown) {
					if (callbacks.error) {
						for (scbInd = 0; scbInd < callbacks.error.length; scbInd++) {
							callbacks.error[scbInd].call(scope, jqXHR, textStatus, errorThrown);
						}
					}
				}
			});
		}
	};

	return {
		sendCSWGetCapabilitiesRequest: sendCSWGetCapabilitiesRequest,
		url : this.url,
		cache : this.cache,
		proxy : this.proxy,
		capabilitiesMap : this.capabilitiesMap
	};
};