var GDP = GDP || {};

// JSLint fixes
/*global $ */
/*jslint plusplus: true */

GDP.UI = function (args) {
	"use strict";
	args = args || {};

	// Initialization
	this.init = function (args) {
		args = args || {};

		// Fix the header of the application by removing unused elements in the 
		// right container
		$('#ccsa-area').children().slice(0, 2).remove();

		var me = this,
			updateOfferingMaps = function () {
				GDP.CONFIG.cswClient.getDomain({
					propertyName : 'keyword',
					callbacks : {
						success : [
							function (cswGetDomainResponseObject) {
								var dvIdx,
									value,
									domainValues = cswGetDomainResponseObject.DomainValues[0].ListOfValues;

								for (dvIdx = 0; dvIdx < domainValues.length; dvIdx++) {
									value = domainValues[dvIdx].Value.value;
									if (value.toLowerCase().contains('gov.usgs.cida.gdp.wps')) {
										GDP.CONFIG.offeringMaps.wpsToCsw[value] = {};
									}
								}

								this.getRecordsByKeywords({
									scope : me,
									keywords : Object.keys(GDP.CONFIG.offeringMaps.wpsToCsw),
									callbacks : {
										success : [
											function (cswGetRecRespObj) {
												var records = cswGetRecRespObj.records,
													wpsToCsw = GDP.CONFIG.offeringMaps.wpsToCsw,
													cswToWps = GDP.CONFIG.offeringMaps.cswToWps,
													rIdx,
													sIdx,
													subjectArray,
													subject,
													record,
													algName,
													ident;

												for (rIdx = 0; rIdx < records.length; rIdx++) {
													record = records[rIdx];
													ident = record.identifier[0].value;
													if (!GDP.CONFIG.offeringMaps.cswIdentToRecord[ident]) {
														GDP.CONFIG.offeringMaps.cswIdentToRecord[ident] = record;
													}
													subjectArray = record.subject;
													for (sIdx = 0; sIdx < subjectArray.length; sIdx++) {
														subject = subjectArray[sIdx].value;
														if (subject.toLowerCase().contains('gov.usgs.cida.gdp.wps')) {
															if (!wpsToCsw[subject][record.identifier[0].value]) {
																wpsToCsw[subject][record.identifier[0].value] = record;
															}
														}
													}

													if (!cswToWps[record.identifier[0].value]) {
														cswToWps[record.identifier[0].value] = [];
													}
												}

												for (algName in wpsToCsw) {
													if (wpsToCsw.hasOwnProperty(algName)) {
														for (ident in wpsToCsw[algName]) {
															if (wpsToCsw[algName].hasOwnProperty(ident)) {
																if (!cswToWps[ident]) {
																	cswToWps[ident] = [];
																}
																cswToWps[ident].push(algName);
															}
														}
													}
												}
												this.initializationCompleted();
											}
										],
										error : [
											function (error) {
												throw error;
											}
										]
									}
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
			};
		this.cswDropdownUpdated = function (event) {
			var value = event.target.value,
				validAlgorithms = GDP.CONFIG.offeringMaps.cswToWps[value],
				algInd,
				offeringsObj = {};
		
			if (!value) {
				GDP.CONFIG.ui.updateWpsDropdown();
			} else {
				for (algInd = 0; algInd < validAlgorithms.length; algInd++) {
					offeringsObj[validAlgorithms[algInd]] = '';
				}
				GDP.CONFIG.ui.updateWpsDropdown({
					wpsOfferings : offeringsObj
				});
			}
		};
		
		this.wpsDropdownUpdated = function (event) {
			var value = event.target.value,
				validOfferings = GDP.CONFIG.offeringMaps.wpsToCsw[value],
				offering,
				offeringsObj = {};
		
			if (!value) {
				GDP.CONFIG.ui.updateCswDropdown();
			} else {
				for (offering in validOfferings) {
					if (validOfferings.hasOwnProperty(offering)) {
						offeringsObj[offering] = '';
					}
				}
				GDP.CONFIG.ui.updateCswDropdown({
					cswOfferings : offeringsObj
				});
			}
			
		};

		this.updateCswDropdown = function (args) {
			args = args || {};
			var cswOfferings =  args.cswOfferings || GDP.CONFIG.offeringMaps.cswToWps,
				dropdown = $('#form-control-select-csw'),
				ident;

			dropdown.empty();
			dropdown.append(
				$('<option />')
					.attr({
						name : '',
						value : '',
						label : '',
						selected : 'selected'
					}).html('')
			);
			for (ident in cswOfferings) {
				if (cswOfferings.hasOwnProperty(ident)) {
					dropdown.append(
						$('<option />')
							.attr({
								value : ident
							}).html(GDP.CONFIG.offeringMaps.cswIdentToRecord[ident].title[0].value)
					);
				}
			}
			dropdown.off('change', this.cswDropdownUpdated);
			dropdown.on('change', this.cswDropdownUpdated);
		};
		this.updateWpsDropdown = function (args) {
			args = args || {};
			var wpsOfferings = args.wpsOfferings || GDP.CONFIG.offeringMaps.wpsToCsw,
				dropdown = $('#form-control-select-wps'),
				ident;

			dropdown.empty();
			dropdown.append(
				$('<option />')
					.attr({
						name : '',
						value : '',
						label : '',
						selected : 'selected'
					}).html('')
			);

			for (ident in wpsOfferings) {
				if (wpsOfferings.hasOwnProperty(ident)) {
					dropdown.append(
						$('<option />')
							.attr({
								value : ident
							}).html(ident)
					);
				}
			}
			
			dropdown.off('change', this.wpsDropdownUpdated);
			dropdown.on('change', this.wpsDropdownUpdated);
		};
		this.initializationCompleted = function () {
			this.updateCswDropdown();
			this.updateWpsDropdown();
		};

		GDP.CONFIG.cswClient.requestGetCapabilities({
			callbacks : {
				success : [
					function () {
						if (GDP.CONFIG.wpsClient.capabilitiesDocument) {
							updateOfferingMaps();
						}
					}
				],
				error : [
					function (error) {
						throw error;
					}
				]
			}
		});

		GDP.CONFIG.wpsClient.requestGetCapabilities({
			callbacks : {
				success : [
					function (capabilities) {
						if (GDP.CONFIG.cswClient.capabilitiesDocument) {
							updateOfferingMaps();
						}
					}
				],
				error : [
					function (error) {
						throw error;
					}
				]
			}
		});
	};

	this.init();

	return {
		updateCswDropdown : this.updateCswDropdown,
		updateWpsDropdown : this.updateWpsDropdown,
		cswDropdownUpdated : this.cswDropdownUpdated,
		wpsDropdownUpdated : this.wpsDropdownUpdated
	};
};
