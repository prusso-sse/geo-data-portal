var ScienceBase = function () {
    var _SB_SEARCH_TEXT = '#sbSearch';
    var _SB_FEATURE_BUTTON = '#sbFeatureButton';
    var _SB_ENDPOINTS = {};
    var _USE_SB = false;
    var _ITEM_ID;
    return {
        endpoints : _SB_ENDPOINTS,
        useSB : _USE_SB,
        itemId : _ITEM_ID,
        init : function() {
            this.endpoints = incomingParams;
            if (!$.isEmptyObject(this.endpoints) && this.endpoints.caller === 'sciencebase') {
                this.useSB = true;
            }
            
            this.itemId = incomingParams['item_id'] || '';
            
            // By this point, the ScienceBase object has initialized and 
            // may have incoming parameters. Use those to set our params 
            // here.
            $.each(ScienceBase.endpoints, function(key, value) {
                if (key === 'feature_wms') {
                    Constant.endpoint.wms = value;
                }
                
                if (key === 'feature_wfs') {
                    Constant.endpoint.wfs = value;
                }
                
                if (key === 'redirect_url') {
                    Constant.endpoint['redirect_url'] = value;
                }
                
                if (key === 'coverage_wcs' && value) {
                    Constant.ui.default_dataset_url = value;
                    Constant.ui.default_wms_url = ScienceBase.endpoints['coverage_wms'];
                } else if (key === 'coverage_opendap' && value) {
                    Constant.ui.default_dataset_url = value;
                    Constant.ui.default_wms_url = ScienceBase.endpoints['coverage_wms'];
                }
            });
        },
        searchSB: function() {
            var oldVal = document.theForm.query.value,
				query = $(_SB_SEARCH_TEXT).val(),
				sbEndpoint = Constant.endpoint['sciencebase-csw'];

            document.theForm.query.value = query;
            GDPCSWClient.currentSBFeatureSearch = document.theForm.query.value;
            GDPCSWClient.setCSWHost(sbEndpoint);
            GDPCSWClient.setContext('wfs');
            GDPCSWClient.getRecords();
			Dataset.createCSWResponseDialog();

            document.theForm.query.value = oldVal;

            $(_SB_FEATURE_BUTTON).trigger('click');
        },
		selectFeatureById: function (id) {
			var csw_response = GDPCSWClient.getRecordById(id),
				selectedFeature,
					// We are doing this because we don't know which format the data might be in, if we can tell, we shouldn't iterate
				datasetSelectors = [
					'[nodeName="csw:GetRecordByIdResponse"] > [nodeName="csw:Record"] [nodeName="dc:URI"]',
					'[nodeName="csw:GetRecordByIdResponse"] > [nodeName="gmd:MD_Metadata"] > [nodeName="gmd:identificationInfo"] > ' +
							'[nodeName="srv:SV_ServiceIdentification"] > [nodeName="srv:containsOperations"] > [nodeName="srv:SV_OperationMetadata"] > ' +
							'[nodeName="srv:connectPoint"] > [nodeName="gmd:CI_OnlineResource"] > [nodeName="gmd:linkage"] > [nodeName="gmd:URL"]',
					'[nodeName="csw:GetRecordByIdResponse"] > [nodeName="gmd:MD_Metadata"] > [nodeName="gmd:distributionInfo"] > ' +
							'[nodeName="gmd:MD_Distribution"] > [nodeName="gmd:transferOptions"] > [nodeName="gmd:MD_DigitalTransferOptions"] > ' +
							'[nodeName="gmd:onLine"] > [nodeName="gmd:CI_OnlineResource"] > [nodeName="gmd:linkage"] > [nodeName="gmd:URL"]'
				];

			for (var i = 0;i < datasetSelectors.length; i++) {
				$(csw_response).find(datasetSelectors[i]).each(function(index, elem) {
					var text = $(elem).text();

					if (text.toLowerCase().contains("wfs") || text.toLowerCase().contains("ows")) {
						selectedFeature = text.indexOf('?') !== -1 ? text.substring(0, text.indexOf('?')) : text;
					}
				});
			}

			if (!selectedFeature) {
				showErrorNotification("No feature found for this CSW Record");
			} else {
				Constant.endpoint.wfs = selectedFeature;
				Constant.endpoint.wms = selectedFeature;
				ScienceBase.itemId = id;
				ScienceBase.useSB = true;
				GDPCSWClient.sbConstraintFeature = false;
				GDPCSWClient.sbConstraintCoverage = false;

				AOI.updateFeatureTypesList(function() {
					var options = $(AOI.areasOfInterestSelectbox)[0].options;

					if (options.length === 1) {
						// The return only came back with one item. Pick it
						$(AOI.areasOfInterestSelectbox).val(options[0].label);
						$(AOI.areasOfInterestSelectbox).trigger('change');
					} else if (options.length === 2) {
						// If the return only has two items, pick the one that isn't
						// footprint. Footprint feature seems to selectFeaturebe included in all 
						// sciencebase features
						$.each(options, function(index, item) {
							if (item.label.toLowerCase() !== 'sb:footprint') {
								$(AOI.areasOfInterestSelectbox).val(item.label);
								$(AOI.areasOfInterestSelectbox).trigger('change');
							}
						});
					} else if (options.length > 2) {
						// If we have more thwn 2 options, don't pick any because
						// there's no way of knowing which one they picked from the 
						// feature list returned from ScienceBase. Just get rid of 
						// the available attributes and attribute values selectboxes
						// and wipe the last geometry from the map, if it's there
						var overlay = map.getLayersByName('Geometry Overlay')[0],
								hlOverlay = map.getLayersByName('Geometry Highlight Overlay')[0];

						$(AOI.availableAttributesSelectbox).fadeOut(Constant.ui.fadeSpeed);
						$(AOI.availableAttributeValsSelectbox).fadeOut(Constant.ui.fadeSpeed);

						if (overlay) {
							map.removeLayer(overlay);
						}

						if (hlOverlay) {
							map.removeLayer(hlOverlay);
						}
					}

				});

				$(this.cswOutput).dialog('close');
			}
		}
    };
};
