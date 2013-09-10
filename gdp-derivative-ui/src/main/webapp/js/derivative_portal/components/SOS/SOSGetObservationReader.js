Ext.ns("GDP");

GDP.SOSGetObservationReader = function(meta, recordType) {
    meta = meta || {};
    if(!meta.format) {
        meta.format = new OpenLayers.Format.SOSGetObservation();
    }
    if(typeof recordType !== "function") {
        recordType = Ext.data.Record.create(meta.fields || [
            {name: "id", type: "string"},
            {name: "name", type: "string"},
            {name: "bounds"}, // Object
            {name: "fois"}, // Array of objects
            {name: "dataRecord"}, // Array of objects
            {name: "values"} // Array of objects
        ]
        );
    }
    GDP.SOSGetObservationReader.superclass.constructor.call(
        this, meta, recordType
    );
};

Ext.extend(GDP.SOSGetObservationReader, Ext.data.DataReader, {


    /** api: config[attributionCls]
     *  ``String`` CSS class name for the attribution DOM elements.
     *  Element class names append "-link", "-image", and "-title" as
     *  appropriate.  Default is "gx-attribution".
     */
    attributionCls: "gx-attribution",

    /** private: method[read]
     *  :param request: ``Object`` The XHR object which contains the parsed XML
     *      document.
     *  :return: ``Object`` A data block which is used by an ``Ext.data.Store``
     *      as a cache of ``Ext.data.Record`` objects.
     */
    read: function(request) {
        var data = request.responseXML;
        if(!data || !data.documentElement) {
            data = request.responseText;
        }
        return this.readRecords(data);
    },
    

    /** private: method[readRecords]
     *  :param data: ``DOMElement | String | Object`` A document element or XHR
     *      response string.  As an alternative to fetching capabilities data
     *      from a remote source, an object representing the capabilities can
     *      be provided given that the structure mirrors that returned from the
     *      capabilities parser.
     *  :return: ``Object`` A data block which is used by an ``Ext.data.Store``
     *      as a cache of ``Ext.data.Record`` objects.
     *  
     *  Create a data block containing Ext.data.Records from an XML document.
     */
    readRecords: function(data) {
        if(typeof data === "string" || data.nodeType) {
            data = this.meta.format.read(data);
        }
        
        var records = [];

        Ext.iterate(data.observations, function (item) {
            var values = {};
            values.name = item.attributes.name;
            values.bounds = item.bounds;
            values.fois = item.fois;
            values.dataRecord = item.result.dataArray.dataRecord;
            values.values = item.result.dataArray.values;
            
            records.push(new this.recordType(values));
        }, this);
        
        return {
            totalRecords: records.length,
            success: true,
            records: records
        };

    }

});
