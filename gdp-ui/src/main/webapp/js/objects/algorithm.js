// JSLint fixes
/*jslint plusplus: true */
/*jslint nomen: true */
/*global $ */
/*global logger */
/*global Constant */
/*global WPS */
var Algorithm = function () {
    var _algorithms,
		_USER_CONFIGURABLES = ['FEATURE_COLLECTION', 'FEATURE_ATTRIBUTE_NAME', 'DATASET_URI', 'DATASET_ID', 'TIME_START', 'TIME_END'],
		_WPS_NS = 'ns',
		_OWS_NS = 'ns1';

    function _needsConfiguration(algorithm) {
        var inputs = [];
        $.each(algorithm.inputs, function (k, v) {
            if ($.inArray(k, _USER_CONFIGURABLES) === -1) {
				inputs.push(k);
			}
        });
        if (inputs.length) {
			return true;
		}
        return false;
    }

    return {
        algorithms : _algorithms,
        userConfigurables : _USER_CONFIGURABLES,
        init : function () {
            logger.debug("GDP:algorithm.js::Getting algorithms from server.");
            this.algorithms = {};
            var wpsURL = Constant.endpoint.proxy + Constant.endpoint.processwps,
				splitAlgorithm,
				singleAlgorithm = (function () {
					splitAlgorithm = Constant.ui.view_algorithm_list.split(',');
					if (splitAlgorithm.length !== 1) {
						return '';
					}
					return splitAlgorithm[0];
				}()),
				scope = this;

            // We first create a getcapabilities request, and then we send a describe process 
            // request for each algorithm in that request, creating an algorithm array from that
            WPS.sendWPSGetRequest(wpsURL, WPS.getCapabilitiesParams, false, function (getCapsXML) {
                $(getCapsXML).find(_WPS_NS + '|Process').each(function () {
                    var processID = $(this).find(_OWS_NS + '|Identifier').text();
                    if (singleAlgorithm && processID !== singleAlgorithm) {
                        return true;
                    }

                    WPS.sendWPSGetRequest(wpsURL, WPS.describeProcessParams(processID), false, function (describeProcessXML) {
                        Algorithm.learnWPSProcess(describeProcessXML, scope.algorithms);
                    });

                });
            });
        },
        learnWPSProcess : function (describeProcessXML, algorithms) {
            var identifier = $(describeProcessXML).find(_OWS_NS + '|Identifier:first').text(),
				title = $(describeProcessXML).find(_OWS_NS + '|Title:first').text(),
				abstrakt = $(describeProcessXML).find(_OWS_NS + '|Abstract:first').text(),
				inputs = {};

            WPS.processDescriptions[identifier] = describeProcessXML;

            // Create the inputs
            $(describeProcessXML).find(_WPS_NS + '|ProcessDescriptions').find('Input').each(function (i, v) {
                var inputIdentifier = $(v).find(_OWS_NS + '|Identifier').text(),
					title = $(v).find(_OWS_NS + '|Title').text(),
					minOccurs = $(v).attr('minOccurs'),
					maxOccurs = $(v).attr('maxOccurs'),
					reference = '',
					allowedValues = '',
					type = (function () {
						if ($(v).find('ComplexData').length > 0) {
							return 'complex';
						}
						if ($(v).find('LiteralData').length > 0) {
							return 'literal';
						}
						if ($(v).find('BoundingBox').length > 0) {
							return 'bbox';
						}
						return '';
					}()),
					complexFormats;

                if (type === 'literal') { // TODO- We're not using the xmlns plugin to pull attribute. Figure out why it doesn't work with it
                    reference = $(v).find('LiteralData').find(_OWS_NS + '|DataType').attr(_OWS_NS + ':reference');
                    allowedValues = (function () {
                        if ($(v).find('LiteralData').find(_OWS_NS + '|AnyValue').length > 0) {
                            return ['*'];
                        } else {
                            return (function () {
                                var valArr = [];
                                $(v).find('LiteralData').find(_OWS_NS + '|AllowedValues').find(_OWS_NS + '|Value').each(function (valI, valV) {
                                    valArr.push($(valV).text());
                                });
                                return valArr;
                            }());
                        }
                    }());
                } else if (type === 'complex') {
                    complexFormats = [];
                    $(v).find('ComplexData Format').each(function (i, v) {
                        var format = {};
                        format['type'] = $(v).parent()[0].nodeName.toLowerCase();
                        format.mimeType = $(v).find('MimeType').text();
                        format.encoding = $(v).find('Encoding').text();
                        format.schema = $(v).find('Schema').text();
                        complexFormats.push(format);
                    });
                }
				//else if (type === 'bbox') {
                //TODO- Once we have BBOX algorithms, parse those. Leave out for now
                //}

                inputs[inputIdentifier] = {
                    'maxOccurs' : maxOccurs,
                    'minOccurs' : minOccurs,
                    'title' : title,
                    'type' : type,
                    'reference' : reference,
                    'allowedValues' : allowedValues
                };
            });

            algorithms[identifier] = {
                'title' : title,
                'abstrakt' : abstrakt,
                'xml' : describeProcessXML,
                'inputs' : inputs
            };
            algorithms[identifier].needsConfiguration = _needsConfiguration(algorithms[identifier]);
            logger.debug("GDP:algorithm.js::Created " + title + " algorithm.");
        },
        isPopulated : function () {
			var member;
            for (member in this.algorithms) {
				if (this.algorithms.hasOwnProperty(member)) {
					return true;
				}
			}
            return false;
        }
    };
};