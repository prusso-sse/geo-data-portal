// JSLint fixes
/*jslint plusplus: true */
/*global $ */
/*global OpenLayers */
/*global Sarissa */

var GDP = GDP || {};
GDP.CSW = function (args) {
	"use strict";
	args = args || {};
	this.url = args.url;
	this.proxy = args.proxy;
	this.capabilitiesDocument = args.capabilitiesDocument;

	/**
	 * Create a CSW GetCapabilities request
	 * @param args {Object}
	 */
	var requestGetCapabilities = function (args) {
		args = args || {};

		var callbacks = args.callbacks || {
			success: [],
			error: []
		},
			url = args.url || this.url,
			scbInd,
			proxy = args.proxy || this.proxy,
			capabilitiesDocument = args.capabilitiesDocument || this.capabilitiesDocument,
			scope = args.scope || this;

		// Check to see if there's a capabilities document available in cache
		if (capabilitiesDocument) {
			for (scbInd = 0; scbInd < callbacks.success.length; scbInd++) {
				callbacks.success[scbInd].call(this, capabilitiesDocument);
			}
		} else {
			OpenLayers.Request.GET({
				url: proxy ? proxy + url : url,
				params: {
					request: "GetCapabilities",
					service: "CSW",
					version: "2.0.2"
				},
				success: function (response) {
					var responseXML = response.responseXML;

					// Add the getCapabilities response to cache
					scope.capabilitiesDocument = responseXML;

					if (callbacks.success && callbacks.success.length) {
						for (scbInd = 0; scbInd < callbacks.success.length; scbInd++) {
							callbacks.success[scbInd].call(scope, scope.capabilitiesDocument);
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
	},
		getCapabilitiesKeywords = function (capablitiesXmlDoc) {
			if (!capablitiesXmlDoc && this.capabilitiesDocument) {
				capablitiesXmlDoc = this.capabilitiesDocument;
			} else if (!capablitiesXmlDoc && !this.capabilitiesDocument) {
				throw "missing capabilities xml document";
			}

			var oDomDoc = Sarissa.getDomDocument(),
				keywordNodes,
				keywords = [],
				kwInd;

			oDomDoc.setProperty("SelectionLanguage", "XPath");
			oDomDoc.setProperty("SelectionNamespaces",
					"xmlns:xhtml='http://www.w3.org/1999/xhtml' " +
					"xmlns:xsl='http://www.w3.org/1999/XSL/Transform' " +
					"xmlns:ows='http://www.opengis.net/ows' " +
					"xmlns:csw='http://www.opengis.net/cat/csw/2.0.2'");
			keywordNodes = capablitiesXmlDoc.selectNodes('//ows:Keywords/ows:Keyword');

			for (kwInd = 0; kwInd < keywordNodes.length; kwInd++) {
				keywords.push(keywordNodes[kwInd].innerHTML);
			}

			return keywords;
		},
		getRecordsByKeywords = function (args) {
			args = args || {};
			var keywords = args.keywords || [],
				callbacks = args.callbacks || {
					success : [],
					error : []
				},
				scope = args.scope || this,
				fInd,
				cswGetRecFormat = new OpenLayers.Format.CSWGetRecords(),
				filters = [],
				filter,
				getRecRequest,
				getRecordsResponse = new OpenLayers.Protocol.Response({
					requestType: "read"
				});

			if (!keywords.length) {
				filters.push(
					new OpenLayers.Filter.Comparison({
						type: OpenLayers.Filter.Comparison.LIKE,
						property: "keyword",
						value: '*'
					})
				);
			} else {
				for (fInd = 0; fInd < keywords.length; fInd++) {
					filters.push(
						new OpenLayers.Filter.Comparison({
							type: OpenLayers.Filter.Comparison.LIKE,
							property: "keyword",
							value: keywords[fInd]
						})
					);
				}
			}
			
			filter = new OpenLayers.Filter.Logical({
					type: OpenLayers.Filter.Logical.AND,
					filters: filters
			});
			
			getRecRequest = cswGetRecFormat.write({
				resultType: "results",
//					maxRecords: "20",
				Query: {
					ElementSetName: {
						value: "summary"
					},
					Constraint: {
						"version": "1.1.0",
						"Filter": filter
					}
				}
			});

			getRecordsResponse.priv = OpenLayers.Request.POST({
				url: this.proxy + this.url,
				data: getRecRequest,
				success: function (response) {
					var cswGetRecRespObj = cswGetRecFormat.read(response.responseXML || response.responseText),
						scbInd;

					if (callbacks.success && callbacks.success.length) {
						for (scbInd = 0; scbInd < callbacks.success.length; scbInd++) {
							callbacks.success[scbInd].call(scope, cswGetRecRespObj);
						}
					}
				},
				failure: function (response) {
					var scbInd;
					if (callbacks.error) {
						for (scbInd = 0; scbInd < callbacks.error.length; scbInd++) {
							callbacks.error[scbInd].call(scope, response);
						}
					}
				}
			});
		};

	return {
		requestGetCapabilities: requestGetCapabilities,
		getCapabilitiesKeywords : getCapabilitiesKeywords,
		getRecordsByKeywords : getRecordsByKeywords,
		url : this.url,
		proxy : this.proxy,
		capabilitiesDocument : this.capabilitiesDocument
	};
};