/*
 * Application now reads metadata information from json files rather than a CSW call.
 * Kept the record store structure to limit the changes needed in the rest of the application
 */

Ext.ns("GDP");

GDP.MetadataRecordsStore = function(meta) {
    meta = meta || {};

    meta.fields = [
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
    GDP.MetadataRecordsStore.superclass.constructor.call(
        this,
        Ext.apply(meta, {
            proxy: meta.proxy || (!meta.data ? new Ext.data.HttpProxy({url: meta.url, disableCaching: false, method: "GET"}) : undefined),
            baseParams : meta.opts,
            reader: new GDP.MetadataRecordsReader(meta)
        })
    );
};

Ext.extend(GDP.MetadataRecordsStore, Ext.data.Store);

