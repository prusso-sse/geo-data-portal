/*
 * Entry point for the application's JavaScript
 * Author: Ivan Suftin (isuftin@usgs.gov)
 */

// JSLint fixes
/*global document */
/*global $ */

$(document).ready(function () {
	"use strict";

	// Fix the header of the application by removing unused elements in the 
	// right container
	$('#ccsa-area').children().slice(0, 2).remove();
});
