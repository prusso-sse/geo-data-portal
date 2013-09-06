describe('Verify Openlayers dependency', function() {
	it('tests for an OpenLayers definition', function() {
		expect(OpenLayers).not.toBe(undefined);
	});

	it('tests for existence of OpenLayers version', function() {
		expect(OpenLayers.VERSION_NUMBER).not.toBe(undefined);
	});

	it('expects OpenLayers to define browser as FireFox', function() {
		expect(OpenLayers.BROWSER_NAME).toEqual("firefox");
	});
});

describe('Verify Openlayers CSW Parsing', function() {
	it('tests OpenLayers ability to pull down CSW GetRecords', function() {

//		sendRequest("gdp-geonetwork-csw-getrecords-all.xml", function(req) {
//			expect(req.responseText).toBe('awesome');
//			expect(0).toBe(2);
//		});

//		var test = new OpenLayers.Request.GET({
//			url: "src/test/resources/gdp-geonetwork-csw-getrecords-all.xml",
//			async: false,
//			callback: function(r) {
//				expect(r).toBe('awesome');
//				expect(0).toBe(2);
//			}
//		});
////		
//		try {
//			test.read();
//		} catch (a){}
//
//		var protocol = new OpenLayers.Protocol.CSW({
//			url: "gdp-geonetwork-csw-getrecords-all.xml",
//			async : false,
//			parseData: function(request) {
//				expect(0).toBe(2);
//			},
//		},
//		{
//			callback: function(response) {
//				expect(response).toBe(1);
//				expect(response.code).toEqual(OpenLayers.Protocol.Response.SUCCESS);
//			}
//		});
		
//		try {
//		protocol.read();
//		} catch (a) {
//			
//		}
	});
});


