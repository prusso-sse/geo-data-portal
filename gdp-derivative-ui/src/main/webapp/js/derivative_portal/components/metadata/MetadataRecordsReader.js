/*
 * Application now reads metadata information from json files rather than a CSW call.
 * Kept the record store structure to limit the changes needed in the rest of the application
 */

Ext.ns("GDP");

GDP.MetadataRecordsReader = function(meta, recordType) {
    meta = meta || {};

    if(typeof recordType !== "function") {
        recordType = Ext.data.Record.create(meta.fields || [
            {name: "identifier", type: "string"},
            {name: "derivatives"}, // Array of objects
            {name: "scenarios"}, // Array of objects
            {name: "gcms"}, // Array of objects
            {name: "opendap", type: "string"},
            {name: "wms", type: "string"},
            {name: "sos", type: "string"},
            {name: "fieldLabels"},
            {name: "helptext"}
        ]
        );
    }
    GDP.MetadataRecordsReader.superclass.constructor.call(
        this, meta, recordType
    );
};

Ext.extend(GDP.MetadataRecordsReader, Ext.data.DataReader, {

    /** private: method[read]
     *  @param request: ``Object`` The XHR object which contains the json string
     *
     *  @return: ``Object`` A data block which is used by an ``Ext.data.Store``
     *      as a cache of ``Ext.data.Record`` objects.
     */
    read: function(request) {
        var data = Ext.util.JSON.decode(request.responseText);
        return this.readRecords(data);
    },


    /** private: method[readRecords]
     *  @param data: json Object
     *  @return: ``Object`` A data block which is used by an ``Ext.data.Store``
     *      as a cache of ``Ext.data.Record`` objects.
     *
     *  Create a data block containing Ext.data.Records from a json object.
     */
    readRecords: function(data) {
        if(typeof data === "string" || data.nodeType) {
            data = this.meta.format.read(data);
        }

        var records = [];

        Ext.iterate(data, function (item) {
            var values = {};
            values.identifier = item.identifier;
            values.derivatives = [];
            values.scenarios = [];
            values.gcms = [];

			var keywordTypes = item.descriptiveKeywords;
			var srvId = item.serviceIdentification;
			var abstrakt = item.hasOwnProperty('abstract') ? item["abstract"] : null;

			if (keywordTypes) {
				if (keywordTypes.hasOwnProperty('derivatives')) {
					Ext.iterate(keywordTypes.derivatives, function(key) {
						var derivArr = [];
						derivArr.push(key);
						if (abstrakt !== null) {
							derivArr.push(abstrakt.quicktips.derivatives[key]);
						}
						values.derivatives.push(derivArr);
					}, this);
				}
				if (keywordTypes.hasOwnProperty('scenarios')) {
					Ext.iterate(keywordTypes.scenarios, function(key) {
						var scenarioArr = [];
						scenarioArr.push(key);
						if (abstrakt !== null) {
							scenarioArr.push(abstrakt.quicktips.scenarios[key]);
						}
						values.scenarios.push(scenarioArr);
					}, this);
				}
				if (keywordTypes.hasOwnProperty('gcm')) {
					Ext.iterate(keywordTypes.gcm, function(key) {
						var gcmArr = [];
						gcmArr.push(key);
						if (abstrakt !== null) {
							gcmArr.push(abstrakt.quicktips.gcms[key]);
						}
						values.gcms.push(gcmArr);
					}, this);
				}
			}
			if (srvId) {
				if (srvId.hasOwnProperty('sos')) {
					values.sos = srvId.sos;
				}
				if (srvId.hasOwnProperty('opendap')) {
					values.opendap = srvId.opendap;
				}
				if (srvId.hasOwnProperty('wms')) {
					values.wms = srvId.wms;
				}
			}
			if (abstrakt !== null) {
				values.fieldLabels = abstrakt.fieldlabels;
				values.helptext = abstrakt.helptext;
			}
            records.push(new this.recordType(values));
        }, this);

        return {
            totalRecords: records.length,
            success: true,
            records: records
        };

    }

});

