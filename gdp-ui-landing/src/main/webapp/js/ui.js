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
			chosenStartPath,
			removeOverlay = function () {
				$('#overlay').fadeOut(
					function () {
						$('#overlay').remove();
					}
				);
			},
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
			};

		this.cswDropdownChanged = function (event) {
			var value = event.target.value,
				validAlgorithms = GDP.CONFIG.offeringMaps.cswToWps[value],
				algInd,
				alg,
				currentValue,
				record,
				isParentRecord,
				offeringsObj = {};

			if (!value) {
				$('#row-proceed').fadeOut();
				$('#p-csw-information-title').html('');
				$('#p-csw-information-content').html('');
				if (me.chosenStartPath === 'dataset') {
					$('#row-wps-select').fadeOut();
					$('#form-control-select-wps option[value=""]').val('').change();
				}
			} else {
				record = GDP.CONFIG.offeringMaps.cswIdentToRecord[value];
//				isParentRecord = GDP.CONFIG.cswClient.isParentRecord({
//					record : record
//				});
				$('#p-csw-information-title').html(GDP.CONFIG.cswClient.getTitleFromRecord({
					record : record
				}));
				$('#p-csw-information-content').html(GDP.CONFIG.cswClient.getAbstractFromRecord({
					record : record
				}));
				if (me.chosenStartPath === 'dataset') {
					for (algInd = 0; algInd < validAlgorithms.length; algInd++) {
						alg = validAlgorithms[algInd];
						offeringsObj[alg] = alg;
					}

					currentValue = $('#form-control-select-wps').val();

					me.updateWpsDropdown({
						offerings: offeringsObj
					});

					if ($('#form-control-select-wps option[value="' + currentValue + '"]').length) {
						$('#form-control-select-wps').val(currentValue).change();
					} else {
						$('#form-control-select-wps option[value=""]').val('').change();
					}
					$('#row-wps-select').fadeIn();
				} else {
					if ($('#form-control-select-csw').val()) {
						me.bindProceedButton();
						$('#row-proceed').fadeIn();
					}
				}
			}
		};

		this.wpsDropdownChanged = function (event) {
			var value = event.target.value,
				validOfferings = GDP.CONFIG.offeringMaps.wpsToCsw[value],
				currentValue,
				me = this;

			if (!value) {
				$('#row-proceed').fadeOut();
				$('#p-wps-information-title').html('');
				$('#p-wps-information-content').html('');
				if (me.chosenStartPath === 'algorithm') {
					$('#row-csw-select').fadeOut();
					$('#form-control-select-csw option[value=""]').val('').change();
				}
			} else {
				GDP.CONFIG.wpsClient.getProcessDescription({
					process : value,
					callbacks : {
						success : [
							function (processResponse) {
								$('#p-wps-information-title').html(processResponse.title);
								$('#p-wps-information-content').html(processResponse.abstract);
								if (me.chosenStartPath === 'algorithm') {
									currentValue = $('#form-control-select-wps').val();

									me.updateCswDropdown({
										offerings: validOfferings
									});

									if ($('#form-control-select-csw option[value="' + currentValue + '"]').length) {
										$('#form-control-select-csw').val(currentValue).change();
									} else {
										$('#form-control-select-csw option[value=""]').val('').change();
									}
									$('#row-csw-select').fadeIn();
								} else {
									if ($('#form-control-select-wps').val()) {
										me.bindProceedButton();
										$('#row-proceed').fadeIn();
									}
								}
							}
						],
						error : [
							function (response) {
								var msg = 'Unable to get description for this process';
								$('#p-wps-information-content').html(msg);
							}
						]
					}
				});
			}
		};

		this.updateCswDropdown = function (args) {
			args = args || {};
			var offerings =  args.offerings || GDP.CONFIG.offeringMaps.cswToWps,
				dropdown = $('#form-control-select-csw'),
				ident,
				option;

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
			for (ident in offerings) {
				if (offerings.hasOwnProperty(ident)) {
					option = GDP.CONFIG.cswClient.createOptionFromRecord({
						record : GDP.CONFIG.offeringMaps.cswIdentToRecord[ident]
					});
					dropdown.append(
						$('<option />')
							.attr({
								value : ident
							}).html(GDP.CONFIG.cswClient.getTitleFromRecord({
								record : GDP.CONFIG.offeringMaps.cswIdentToRecord[ident]
							}))
					);
				}
			}
			dropdown.off('change', this.cswDropdownChanged);
			dropdown.on('change', this.cswDropdownChanged);
		};

		this.updateWpsDropdown = function (args) {
			args = args || {};
			var offerings = args.offerings || GDP.CONFIG.offeringMaps.wpsToCsw,
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

			for (ident in offerings) {
				if (offerings.hasOwnProperty(ident)) {
					dropdown.append(
						$('<option />')
							.attr({
								value : ident
							}).html(ident)
					);
				}
			}

			dropdown.off('change', $.proxy(this.wpsDropdownChanged, this));
			dropdown.on('change', $.proxy(this.wpsDropdownChanged, this));
		};

		this.errorEncountered = function (args) {

		};

		this.updateStartInstructions = function (args) {
			args = args || {};
			var title = args.title,
				content = args.content;
			$('#div-start-instructions').fadeOut(function () {
				$('#p-start-instructions-title').html(title);
				$('#p-start-instructions-content').html(content);
				$('#row-start-instructions').css('visibility', 'visible').removeClass('hidden').fadeIn();
				$('#div-start-instructions').fadeIn();
			});
		};

		this.algorithmStartButtonSelected = function (event) {
			var me = this;
			this.chosenStartPath = 'algorithm';

			me.updateWpsDropdown();
			me.updateCswDropdown();
			$('#row-proceed').fadeOut();

			this.updateStartInstructions({
				title : 'Begin By Selecting An Algorithm',
				content : 'Lorem ipsum dolor sit amet, consectetur adipisicing elit, ' +
						'sed do eiusmod tempor incididunt ut labore et dolore magna ' +
						'aliqua. Ut enim ad minim veniam, quis nostrud exercitation ' +
						'ullamco laboris nisi ut aliquip ex ea commodo consequat. ' +
						'Duis aute irure dolor in reprehenderit in voluptate velit ' +
						'esse cillum dolore eu fugiat nulla pariatur. Excepteur sint ' +
						'occaecat cupidatat non proident, sunt in culpa qui officia ' +
						'deserunt mollit anim id est laborum.'
			});

			$('#row-csw-select').fadeOut(function () {
				$('#row-wps-select').fadeOut(function () {
					$('#row-wps-select').insertBefore($('#row-csw-select'));
					$('#row-wps-select').fadeIn();
				});
			});
		};

		this.datasetStartButtonSelected = function (event) {
			var me = this;
			me.chosenStartPath = 'dataset';

			me.updateWpsDropdown();
			me.updateCswDropdown();
			$('#row-proceed').fadeOut();

			this.updateStartInstructions({
				title : 'Begin By Selecting A Dataset',
				content : 'Sed ut perspiciatis unde omnis iste natus error sit ' +
						'voluptatem accusantium doloremque laudantium, totam rem ' +
						'aperiam, eaque ipsa quae ab illo inventore veritatis et ' +
						'quasi architecto beatae vitae dicta sunt explicabo. Nemo ' +
						'enim ipsam voluptatem quia voluptas sit aspernatur aut odit ' +
						'aut fugit, sed quia consequuntur magni dolores eos qui ' +
						'ratione voluptatem sequi nesciunt.'
			});

			$('#row-wps-select').fadeOut(function () {
				$('#row-csw-select').fadeOut(function () {
					$('#row-csw-select').insertBefore($('#row-wps-select'));
					$('#row-csw-select').fadeIn();
				});
			});
		};

		this.bindProceedButton = function () {
			$('#btn-proceed').off('click', this.bindProceedButton);
			$('#btn-proceed').on('click', function () {
				var csw,
					record,
					wps = $('#form-control-select-wps').val(),
					win;
				record = GDP.CONFIG.offeringMaps.cswIdentToRecord[$('#form-control-select-csw').val()];
				csw = encodeURIComponent(GDP.CONFIG.cswClient.getEndpointFromRecord({
					record : record
				}));
				win = window.open(GDP.CONFIG.hosts.gdp + '?csw=' + csw + '&wps=' + wps, '_gdp');
				win.focus();
			});
		};

		this.initializationCompleted = function () {
			me.updateWpsDropdown();
			me.updateCswDropdown();
			removeOverlay();
			$('#btn-choice-algorithm').on('change', $.proxy(this.algorithmStartButtonSelected, this));
			$('#btn-choice-dataset').on('change', $.proxy(this.datasetStartButtonSelected, this));
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
		updateWpsDropdown : this.updateWpsDropdown,
		cswDropdownUpdated : this.cswDropdownChanged,
		wpsDropdownUpdated : this.wpsDropdownChanged,
		errorEncountered : this.errorEncountered,
		chosenStartPath : this.chosenStartPath
	};
};
