var CSW = function () {
	function sendCSWGetCapabilitiesRequest(url, callback, cache, proxy) {
		var cacheGetCaps = parseInt(cache.getcaps, 10),
			capMap = GDPCSWClient.capabilitiesMap;

		// Check to see if there's a capabilities document available in cache
		if (cacheGetCaps && capMap[url]) {
			callback(capMap[url]);
		} else {
			$.ajax({
				url: proxy ? proxy + url : url,
				type: 'get',
				contentType: 'text/xml',
				data: {
					'request': 'GetCapabilities',
					'service': 'CSW',
					'version': '2.0.2'
				},
				success: function (data) {
					if (cacheGetCaps) {
						capMap[url] = data;
					}
					callback(data);
				}
			});
		}
	};

	return {
		sendCSWGetCapabilitiesRequest : sendCSWGetCapabilitiesRequest
	};
};