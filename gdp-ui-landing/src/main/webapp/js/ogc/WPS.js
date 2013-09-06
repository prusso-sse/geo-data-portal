// JSLint fixes
/*jslint plusplus: true */
/*global $ */
/*global OpenLayers */

var GDP = GDP || {};
GDP.WPS = function (args) {
	"use strict";
	args = args || {};
	this.url = args.url;
	this.proxy = args.proxy;
	this.capabilitiesDocument = args.capabilitiesDocument;
	this.processOfferings = args.processOfferings || {};
	
	/**
	 * Requests a capabilities document from a WPS server
	 * @argument {Object} args 
	 */
	var sendWPSGetCapabilitiesRequest = function (args) {
		args = args || {};

		var callbacks = args.callbacks || {
			success: [],
			error: []
		},
			url = args.url || this.url,
			scbInd,
			proxy = args.proxy || this.proxy,
			capabilitiesDocument = args.capabilitiesDocument || this.capabilitiesDocument,
			cachedGetCaps = parseInt(capabilitiesDocument, 10),
			scope = args.scope || this;

		if (cachedGetCaps) {
			for (scbInd = 0; scbInd < callbacks.success.length; scbInd++) {
				callbacks.success[scbInd].call(this, capabilitiesDocument);
			}
		} else {
			OpenLayers.Request.GET({
				url: proxy ? proxy + url : url,
				params: {
					service: "WPS",
					request: "GetCapabilities",
					version: "1.0.0"
				},
				success: function (response) {
					var capabilities = new OpenLayers.Format.WPSCapabilities().read(response.responseText);
					scope.cache.capabilities = 
					scope.processOfferings = capabilities.processOfferings;
					if (callbacks.success) {
							for (scbInd = 0; scbInd < callbacks.success.length; scbInd++) {
								callbacks.success[scbInd].call(scope, capabilities);
							}
						}
				},
				failure: function(response) {
					if (callbacks.error) {
						for (scbInd = 0; scbInd < callbacks.error.length; scbInd++) {
							callbacks.error[scbInd].call(scope, response);
						}
					}
				}
			});
		}
	};
	
	// Initialization
	sendWPSGetCapabilitiesRequest.call(this, {
		proxy: this.proxy,
		url: this.url
	});
	
	return {
		sendWPSGetCapabilitiesRequest: sendWPSGetCapabilitiesRequest,
		processOfferings : this.processOfferings,
		url: this.url,
		proxy: this.proxy,
		capabilitiesDocument: this.capabilitiesDocument
	};
};