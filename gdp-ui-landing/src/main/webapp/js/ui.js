var GDP = GDP || {};

// JSLint fixes
/*global $ */
/*global window */
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
									if (value.toLowerCase().indexOf('gov.usgs.cida.gdp.wps') !== -1) {
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
													record,
													algIdx,
													algName,
													ident,
													url,
													algorithm,
													algorithmArray;

												
												for (rIdx = 0; rIdx < records.length; rIdx++) {
													record = records[rIdx];
													ident = record.fileIdentifier.CharacterString.value;
													url = '';
													if (!GDP.CONFIG.offeringMaps.cswIdentToRecord[ident]) {
														GDP.CONFIG.offeringMaps.cswIdentToRecord[ident] = record;
													}

													algorithmArray = GDP.CONFIG.cswClient.getAlgorithmArrayFromRecord({
														record : record
													});

													for (algIdx = 0; algIdx < algorithmArray.length; algIdx++) {
														algorithm = algorithmArray[algIdx];
														if (!wpsToCsw[algorithm][ident]) {
															wpsToCsw[algorithm][ident] = record;
														}
													}

													if (!cswToWps[ident]) {
														cswToWps[ident] = [];
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
												GDP.CONFIG.ui.errorEncountered({
													data : error,
													recoverable : false
												});
											}
										]
									}
								});
							}
						],
						error : [
							function (error) {
								GDP.CONFIG.ui.errorEncountered({
									data : error,
									recoverable : false
								});
							}
						]
					}
				});
			},
			deselectButtonGroup = function (args) {
				var group = args.group,
					labels;
				
				if (group === 'dset') {
					labels = $('#btn-choice-dataset-all,#btn-choice-dataset-climate,#btn-choice-dataset-landscape').parent();
				} else if (group === 'proc') {
					labels = $('#btn-choice-algorithm-areal,#btn-choice-algorithm-subset').parent();
				}
				labels.removeClass('active');
			},
			buttonSelected = function (event) {
				var me = this,
					button = event.target,
					buttonId = button.id,
					dropdown = $('#form-control-select-csw'),
					keywords = Object.keys(GDP.CONFIG.offeringMaps.wpsToCsw),
					records;

				dropdown.empty();

				if (buttonId === 'btn-choice-dataset-all') {
					me.updateCswDropdown({
						offerings : GDP.CONFIG.offeringMaps.cswIdentToRecord
					});
					deselectButtonGroup({
						group : 'proc'
					});
				} else if (buttonId === 'btn-choice-dataset-climate') {
					keywords = ['*climate*'];
					deselectButtonGroup({
						group : 'proc'
					});
				} else if (buttonId === 'btn-choice-dataset-landscape') {
					keywords = ['*landscape*'];
					deselectButtonGroup({
						group : 'proc'
					});
				} else if (buttonId === 'btn-choice-algorithm-areal') {
					records = GDP.CONFIG.wpsClient.getRecordsByAlgorithmArray({
						algorithms : [
							"gov.usgs.cida.gdp.wps.algorithm.FeatureWeightedGridStatisticsAlgorithm",
							"gov.usgs.cida.gdp.wps.algorithm.FeatureGridStatisticsAlgorithm",
							"gov.usgs.cida.gdp.wps.algorithm.FeatureCategoricalGridCoverageAlgorithm"
						]
					});
					deselectButtonGroup({
						group : 'dset'
					});
				} else if (buttonId === 'btn-choice-algorithm-subset') {
					records = GDP.CONFIG.wpsClient.getRecordsByAlgorithmArray({
						algorithms : [
							"gov.usgs.cida.gdp.wps.algorithm.FeatureCoverageOPeNDAPIntersectionAlgorithm",
							"gov.usgs.cida.gdp.wps.algorithm.FeatureCoverageIntersectionAlgorithm"
						]
					});
					deselectButtonGroup({
						group : 'dset'
					});
				}

				if (records) {
					me.updateCswDropdown({
						offerings : records
					});
				} else {
					GDP.CONFIG.cswClient.getRecordsByKeywords({
						scope : me,
						keywords : keywords,
						callbacks : {
							success : [
								function (cswGetRecRespObj) {
									var records;

									records = GDP.CONFIG.cswClient.getCswIdentToRecordMapFromRecordsArray({
										records : cswGetRecRespObj.records
									});

									if (Object.keys(cswGetRecRespObj).length > 0) {
										me.updateCswDropdown({
											offerings : records
										});
									} else {
										// @todo - handle where we don't have any records 
										// from request. This should never happen in 
										// our controlled environment unless the CSW
										// server is set up wrong
									}
								}
							],
							error : [
								function (error) {
									GDP.CONFIG.ui.errorEncountered({
										data : error,
										recoverable : false
									});
								}
							]
						}
					});
				}
			};

		this.removeOverlay = function () {
			$('#overlay').fadeOut(
				function () {
					$('#overlay').remove();
				}
			);
		};

		this.cswDropdownChanged = function (event) {
			var value = event.target.value,
				validAlgorithms,
				record,
				ident,
				title,
				subtitle,
				content,
				isDatasetChosen;

			if (!value) {
				$('#row-proceed').fadeOut();
				$('#p-csw-information-title').html('');
				$('#p-csw-information-content').html('');
			} else {
				ident = value.split(';')[1];
				record = GDP.CONFIG.offeringMaps.cswIdentToRecord[ident];
				validAlgorithms = GDP.CONFIG.offeringMaps.cswToWps[ident];
				title = GDP.CONFIG.cswClient.getTitleFromRecord({
					record : record
				});
				subtitle = $(event.target).find('option[value="' + value + '"]').html();
				isDatasetChosen = $('.btn-group label.active input').attr('id').indexOf('dataset') !== -1;

				if (subtitle !== title) {
					title += '<br />' + subtitle;
				}

				content = GDP.CONFIG.cswClient.getAbstractFromRecord({
					record : record
				});

				$('#p-csw-information-title').html(title);
				$('#p-csw-information-content').html(content);
				$('#p-csw-information-content').append('&nbsp;&nbsp;&nbsp;&nbsp;',
					$('<a />')
						.attr({
							'id' :  'view-full-info-link',
							'ident' : ident
						})
						.html('View Full Record')
					);

				if ($('#form-control-select-csw').val()) {
					$('#row-proceed').fadeIn();
					me.bindProceedButton();
				}
				
				$('#view-full-info-link').on('click', function() {
					var ident = this.attributes.ident.textContent;
					GDP.CONFIG.cswClient.createFullRecordView({
						identifier : ident
					});
					$('#full-record-modal').modal('show');
				});
			}
		};

		this.updateCswDropdown = function (args) {
			args = args || {};
			var offerings =  args.offerings || GDP.CONFIG.offeringMaps.cswToWps,
				dropdown = $('#form-control-select-csw'),
				row = $('#row-csw-group'),
				ident,
				option;

			dropdown.empty();
			dropdown.append(
				$('<option />')
					.attr({
						name : '',
						value : '',
						label : ''
					}).html('')
			);
			for (ident in offerings) {
				if (offerings.hasOwnProperty(ident)) {
					option = GDP.CONFIG.cswClient.createOptionFromRecord({
						record : GDP.CONFIG.offeringMaps.cswIdentToRecord[ident]
					});
					dropdown.append(option);
				}
			}

			if (row.css('display') === 'none') {
				row.fadeIn();
			}

			dropdown.off('change', this.cswDropdownChanged);
			dropdown.on('change', this.cswDropdownChanged);
		};

		this.errorEncountered = function (args) {

		};

		this.bindProceedButton = function () {
			$('#btn-proceed').off('click', this.bindProceedButton);
			$('#btn-proceed').on('click', function () {
				var csw,
					cswIdent,
					cswUrl,
					record,
					win,
					algorithms,
					buttonId = $('.btn-group label.active input').attr('id'),
					isDatasetChosen = buttonId.indexOf('dataset') !== -1;

				cswUrl = $('#form-control-select-csw').val().split(';')[0];
				cswIdent = $('#form-control-select-csw').val().split(';')[1];

				if (isDatasetChosen) {
					algorithms = GDP.CONFIG.offeringMaps.cswToWps[cswIdent].join(',');
				} else {
					if (buttonId === 'btn-choice-algorithm-areal') {
						algorithms = "gov.usgs.cida.gdp.wps.algorithm.FeatureWeightedGridStatisticsAlgorithm," +
							"gov.usgs.cida.gdp.wps.algorithm.FeatureGridStatisticsAlgorithm," +
							"gov.usgs.cida.gdp.wps.algorithm.FeatureCategoricalGridCoverageAlgorithm";
					} else if (buttonId === 'btn-choice-algorithm-subset') {
						algorithms = "gov.usgs.cida.gdp.wps.algorithm.FeatureCoverageOPeNDAPIntersectionAlgorithm," +
							"gov.usgs.cida.gdp.wps.algorithm.FeatureCoverageIntersectionAlgorithm";
					}
					
				}

				record = GDP.CONFIG.offeringMaps.cswIdentToRecord[cswIdent];
				csw = encodeURIComponent(cswUrl);
				win = window.open(GDP.CONFIG.hosts.gdp + '?dataset=' + csw + '&algorithm=' + algorithms, '_gdp');
				win.focus();
			});
		};

		this.initializationCompleted = function () {
			$('#btn-choice-dataset-all,#btn-choice-dataset-climate,#btn-choice-dataset-landscape,#btn-choice-algorithm-areal,#btn-choice-algorithm-subset').
				on('change', $.proxy(buttonSelected, this));
			this.removeOverlay();
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
						GDP.CONFIG.ui.errorEncountered({
							data : error,
							recoverable : false
						});
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
						GDP.CONFIG.ui.errorEncountered({
							data : error,
							recoverable : false
						});
					}
				]
			}
		});
	};

	this.init();

	return {
		bindProceedButton : this.bindProceedButton,
		updateCswDropdown : this.updateCswDropdown,
		cswDropdownUpdated : this.cswDropdownChanged,
		errorEncountered : this.errorEncountered,
		removeOverlay : this.removeOverlay
	};
};
