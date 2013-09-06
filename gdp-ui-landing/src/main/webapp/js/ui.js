var GDP = GDP || {};

// JSLint fixes
/*global $ */

GDP.UI = function (args) {
	"use strict";
	args = args || {};

	// Initialization
	this.init = function (args) {
		args = args || {};

		// Fix the header of the application by removing unused elements in the 
		// right container
		$('#ccsa-area').children().slice(0, 2).remove();
	};

	this.init();

	return {
	};
};
