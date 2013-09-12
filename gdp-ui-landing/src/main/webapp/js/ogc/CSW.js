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
			scope = args.scope || this,
			me = this;

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
					me.capabilitiesDocument = responseXML;

					if (callbacks.success && callbacks.success.length) {
						for (scbInd = 0; scbInd < callbacks.success.length; scbInd++) {
							callbacks.success[scbInd].call(scope, me.capabilitiesDocument);
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
				maxRecords = args.maxRecords || 100,
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
				type: OpenLayers.Filter.Logical.OR,
				filters: filters
			});

			getRecRequest = cswGetRecFormat.write({
				resultType: "results",
				maxRecords: String(maxRecords),
				outputSchema : "http://www.isotc211.org/2005/gmd",
				Query: {
					ElementSetName: {
						value: "full"
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
		},
		getDomain = function (args) {
			args = args || {};

			var cswGetDomainFormat = new OpenLayers.Format.CSWGetDomain(),
				propertyName = args.propertyName || '',
				scope = args.scope || this,
				callbacks = args.callbacks || {
					success : [],
					error : []
				},
				getDomainReqData = cswGetDomainFormat.write({
					PropertyName: propertyName
				});

			OpenLayers.Request.POST({
				url: this.proxy + this.url,
				data: getDomainReqData,
				success: function (request) {
					var cswGetDomainResponseObject = cswGetDomainFormat.read(request.responseXML || request.responseText),
						scbInd;

					if (callbacks.success && callbacks.success.length) {
						for (scbInd = 0; scbInd < callbacks.success.length; scbInd++) {
							callbacks.success[scbInd].call(scope, cswGetDomainResponseObject);
						}
					}
				},
				failure : function (response) {
					var scbInd;
					if (callbacks.error) {
						for (scbInd = 0; scbInd < callbacks.error.length; scbInd++) {
							callbacks.error[scbInd].call(scope, response);
						}
					}
				}
			});
		},
		getAlgorithmArrayFromRecord = function (args) {
			args = args || {};
			if (!args.record) {
				throw "undefined record passed in";
			}
			var record = args.record,
				idInfoIdx,
				idInfoElement,
				dkIdx,
				kwArray,
				kwIdx,
				keyword,
				algorithmArray = [];

			if (record.hasOwnProperty('identificationInfo')) {
				for (idInfoIdx = 0; idInfoIdx < record.identificationInfo.length; idInfoIdx++) {
					idInfoElement = record.identificationInfo[idInfoIdx];
					if (idInfoElement.hasOwnProperty('descriptiveKeywords')) {
						for (dkIdx = 0; dkIdx < idInfoElement.descriptiveKeywords.length; dkIdx++) {
							kwArray = idInfoElement.descriptiveKeywords[dkIdx].keyword;
							for (kwIdx = 0; kwIdx < kwArray.length; kwIdx++) {
								keyword = kwArray[kwIdx].CharacterString.value;
								if (keyword.toLowerCase().indexOf('gov.usgs.cida.gdp.wps') !== -1) {
									algorithmArray.push(keyword);
								}
							}
						}
					}
				}
			}
			return algorithmArray;
		},
		getTitleFromRecord = function (args) {
			args = args || {};
			if (!args.record) {
				throw "undefined record passed in";
			}
			var record = args.record,
				title = '',
				idInfoIdx,
				citation,
				idInfoElement;

			if (record.hasOwnProperty('identificationInfo')) {
				for (idInfoIdx = 0; idInfoIdx < record.identificationInfo.length && title === ''; idInfoIdx++) {
					idInfoElement = record.identificationInfo[idInfoIdx];
					if (idInfoElement.hasOwnProperty('citation')) {
						citation = idInfoElement.citation;
						title = citation.title.CharacterString.value;
					}
				}
			}

			return title;
		},
		getAbstractFromRecord = function (args) {
			args = args || {};
			if (!args.record) {
				throw "undefined record passed in";
			}
			var record = args.record,
				abstract = '',
				idInfoIdx,
				idInfoElement;

			if (record.hasOwnProperty('identificationInfo')) {
				for (idInfoIdx = 0; idInfoIdx < record.identificationInfo.length && abstract === ''; idInfoIdx++) {
					idInfoElement = record.identificationInfo[idInfoIdx];
					if (idInfoElement.hasOwnProperty('abstract')) {
						abstract = idInfoElement.abstract.CharacterString.value;
					}
				}
			}

			return abstract;
		},
		getUrlToIdentifierFromRecords = function (args) {
			args = args || {};
			if (!args.records) {
				throw "undefined record passed in";
			}
			var records = args.records,
				urlTocswIdentifier = {},
				rIdx,
				record,
				ident,
				url,
				toIdx,
				dtoIdx,
				idiIdx,
				distributor,
				distributionFormat,
				distributorTransferOptions,
				distributionTransferOption,
				distributionTransferOptionName,
				identificationInfos,
				identificationInfo,
				serviceIdentification,
				operationMetadataName,
				transferOptions,
				transferOption,
				transferOptionName;
		
			for (rIdx = 0; rIdx < records.length; rIdx++) {
				record = records[rIdx];
				ident = record.fileIdentifier.CharacterString.value;
				url = '';

				if (record.hasOwnProperty('identificationInfo')) {
					identificationInfos = record.identificationInfo;
					for (idiIdx = 0; idiIdx < identificationInfos.length; idiIdx++) {
						identificationInfo = identificationInfos[idiIdx];
						if (identificationInfo.hasOwnProperty('serviceIdentification')) {
							serviceIdentification = identificationInfo.serviceIdentification;
							operationMetadataName = serviceIdentification.operationMetadata.name.CharacterString.value.toLowerCase();
							if (operationMetadataName.indexOf('thredds') !== -1 || 
									operationMetadataName === 'opendap') {
								url = serviceIdentification.operationMetadata.linkage.URL;
								urlTocswIdentifier[url] = ident;
							}
						}
					}
				} else if (record.hasOwnProperty('distributionInfo')) {
					if (record.distributionInfo.hasOwnProperty('distributor')) {
						distributor = record.distributionInfo.distributor[0];
						distributionFormat = distributor.distributorFormat[0].name.CharacterString.value;
						if (distributionFormat.toLowerCase() === 'opendap') {
							distributorTransferOptions = distributor.distributorTransferOptions;
							for (dtoIdx = 0; dtoIdx < distributorTransferOptions.length; dtoIdx++) {
								distributionTransferOption = distributorTransferOptions[dtoIdx];
								distributionTransferOptionName = distributionTransferOption.onLine[0].name.CharacterString.value;
								if (distributionTransferOptionName.toLowerCase() === 'file information') {
									url = distributionTransferOption.onLine[0].linkage.URL;
									urlTocswIdentifier[url] = ident;
								}
							}
						}
					} else if (record.distributionInfo.hasOwnProperty('transferOptions')) {
						transferOptions = record.distributionInfo.transferOptions;
						for (toIdx = 0; toIdx < transferOptions.length; toIdx++) {
							transferOption = transferOptions[toIdx].onLine[0];
							transferOptionName = transferOption.name.CharacterString.value.toLowerCase();
							if (transferOptionName === 'opendap' ||
									transferOptionName.indexOf('wcs') !== -1) {
								url = transferOption.linkage.URL;
								urlTocswIdentifier[url] = ident;
							}
						}
					}
				}
			}
			return urlTocswIdentifier;
		},
		getEndpointFromRecord = function (args) {
			args = args || {};
			if (!args.record) {
				throw "undefined record passed in";
			}
			var record = args.record,
				distributionInfo,
				transferOption,
				transferOptions,
				url,
				protocol,
				toIndex,
				endpoint = '';

			if (record.hasOwnProperty('distributionInfo')) {
				distributionInfo  = record.distributionInfo;
				if (distributionInfo.hasOwnProperty('transferOptions')) {
					transferOptions = distributionInfo.transferOptions;
					for (toIndex = 0; toIndex < transferOptions.length && endpoint === ''; toIndex++) {
						transferOption = transferOptions[toIndex];
						protocol = transferOption.onLine[0].name.CharacterString.value.toLowerCase();
						url = transferOption.onLine[0].linkage.URL;
						if (protocol === 'opendap' || url.toLowerCase().indexOf('wcs')) {
							endpoint = url;
						}
					}
				}
			}

			return endpoint;
		};

	return {
		requestGetCapabilities: requestGetCapabilities,
		getCapabilitiesKeywords : getCapabilitiesKeywords,
		getRecordsByKeywords : getRecordsByKeywords,
		getDomain : getDomain,
		getAlgorithmArrayFromRecord : getAlgorithmArrayFromRecord,
		getTitleFromRecord : getTitleFromRecord,
		getAbstractFromRecord : getAbstractFromRecord,
		getEndpointFromRecord : getEndpointFromRecord,
		getUrlToIdentifierFromRecords : getUrlToIdentifierFromRecords,
		url : this.url,
		proxy : this.proxy,
		capabilitiesDocument : this.capabilitiesDocument
	};
};