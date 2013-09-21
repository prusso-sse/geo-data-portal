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
	this.capabilities = args.capabilities;
	this.processOfferings = args.processOfferings || {};
	this.processDescriptions = args.pricessDescriptions || {};

	var describeProcess = function (args) {
		var processId = args.process,
			url = args.url || this.url,
			proxy = args.proxy || this.proxy,
			scope = args.scope || this,
			cbInd = 0,
			callbacks = args.callbacks || {
				success : [],
				error : []
			};

		OpenLayers.Request.GET({
			url: proxy ? proxy + url : url,
			params: {
				service: "WPS",
				request: "DescribeProcess",
				version: "1.0.0",
				identifier: processId
			},
			success: function (response) {
				var process = new OpenLayers.Format.WPSDescribeProcess().read(
					response.responseText
				).processDescriptions[processId];

				for (cbInd; cbInd < callbacks.success.length; cbInd++) {
					callbacks.success[cbInd].call(scope, process);
				}
			},
			failure : function (response) {
				for (cbInd; cbInd < callbacks.error.length; cbInd++) {
					callbacks.error[cbInd].call(scope, response);
				}
			}
		});
	},

		getProcessDescription = function (args) {
			args = args || {};
			var cbInd = 0,
				callback,
				process = args.process,
				me = this,
				proxy = args.proxy || this.proxy,
				scope = args.scope || me,
				callbacks = args.callbacks || {
					success : [],
					error : []
				};

			callbacks.success.push(function (processResponse) {
				me.processDescriptions[process] = processResponse;
			});

			if (me.processDescriptions[process]) {
				for (cbInd; cbInd < callbacks.success.length; cbInd++) {
					callback = callbacks.success[cbInd];
					callback.call(scope, me.processDescriptions[process]);
				}
			} else {
				describeProcess.call(scope, {
					proxy : proxy,
					process: process,
					callbacks: callbacks
				});
			}
		},
				
		getRecordsByAlgorithmArray = function (args) {
			args = args || {};

			var algorithms = args.algorithms || [],
				algorithm,
				aIdx = 0,
				wpsToCsw = GDP.CONFIG.offeringMaps.wpsToCsw,
				records,
				ident,
				record,
				identToCsw = {};
		
			for (aIdx; aIdx < algorithms.length; aIdx++) {
				algorithm = algorithms[aIdx];
				records = wpsToCsw[algorithm];
				for (ident in records) {
					if (records.hasOwnProperty(ident)) {
						record = records[ident];
						if (!identToCsw[ident]) {
							identToCsw[ident] = record;
						}
					}
				}
			}
			
			return identToCsw;

		},

		/**
		 * Requests a capabilities document from a WPS server
		 * @argument {Object} args 
		 */
		requestGetCapabilities = function (args) {
			args = args || {};

			var callbacks = args.callbacks || {
				success: [],
				error: []
			},
				url = args.url || this.url,
				scbInd,
				proxy = args.proxy || this.proxy,
				capabilitiesDocument = args.capabilitiesDocument || this.capabilitiesDocument,
				scope = args.scope || this,
				me = this;

			if (capabilitiesDocument) {
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
						me.capabilities = new OpenLayers.Format.WPSCapabilities().read(response.responseText);
						me.capabilitiesDocument = response.responseXML;
						me.processOfferings = me.capabilities.processOfferings;

						if (callbacks.success && callbacks.success.length) {
							for (scbInd = 0; scbInd < callbacks.success.length; scbInd++) {
								callbacks.success[scbInd].call(scope, me.capabilities);
							}
						}
					},
					failure: function (response) {
						if (callbacks.error) {
							for (scbInd = 0; scbInd < callbacks.error.length; scbInd++) {
								callbacks.error[scbInd].call(scope, response);
							}
						}
					}
				});
			}
		};

	return {
		requestGetCapabilities: requestGetCapabilities,
		describeProcess : describeProcess,
		getProcessDescription : getProcessDescription,
		processOfferings : this.processOfferings,
		processDescriptions : this.processDescriptions,
		getRecordsByAlgorithmArray : getRecordsByAlgorithmArray,
		url: this.url,
		proxy: this.proxy,
		capabilities : this.capabilities,
		capabilitiesDocument: this.capabilitiesDocument
	};
};