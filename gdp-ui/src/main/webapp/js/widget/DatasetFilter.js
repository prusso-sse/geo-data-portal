var GDP = GDP || {};
GDP.widget = GDP.widget || {};

/**
 * Creates a dataset filter modal window which allows the user to choose datasets from the Dataset selectbox
 * @type _L7.Anonym$0|Function
 */
GDP.widget.DatasetFilter = (function () {
	"use strict";
	var DATASET_FILTER_SELECTOR_PREPEND = '#dataset-filter-',
		create = function (args) {
			args = args || {};

			var selectControlSelector = args.selectControlSelector,
				$selectbox = $(selectControlSelector),
				createColumnsBind = function () {
					createColumns(selectControlSelector);
				};

			if (!selectControlSelector) {
				throw "Missing argument selectControlSelector";
			}



			// Uncheck the radio buttons and re-bind their event handling
			$(DATASET_FILTER_SELECTOR_PREPEND + 'delim-selector-form input').
				prop('checked', false).
				off('change', createColumnsBind).
				on('change', createColumnsBind);

			// Clear the checkbox columns
			$(DATASET_FILTER_SELECTOR_PREPEND + 'listing').empty();
			// Clear the X Datasets Selected text
			$(DATASET_FILTER_SELECTOR_PREPEND + 'select-count').html('');

			// Create a modal window
			$(DATASET_FILTER_SELECTOR_PREPEND + 'modal').dialog({
				width: Math.floor($(window).innerWidth() / 2),
				maxHeight: Math.floor($(window).innerHeight() / 1.25),
				position: 'top',
				buttons: [{
						text: 'Done',
						click: function () {
							$(this).dialog("close");
						}
					}, {
						text: 'Select All',
						click: function () {
							// Select everything in the dropdown list and select all the checkboxes
							var $checkboxes = $(this).find('input[type="checkbox"]'),
								$selectBoxes = $selectbox.find('option');
							$selectBoxes.prop('selected', true);
							$checkboxes.prop('checked', true);
							$(this).find('#dataset-filter-select-count').html($selectBoxes.length + ' Datasets Selected');
						}
					}, {
						text: 'Select None',
						click: function () {
							// Clear the dropdown list and unselect all the checkboxes
							$selectbox.find('option').prop('selected', false);
							$(this).find('input[type="checkbox"]').prop('checked', false);
							$(this).find('#dataset-filter-select-count').html('0 Datasets Selected');
						}
					}]
			});
		},
		createColumns = function (selectControlSelector) {
			var delim = $(DATASET_FILTER_SELECTOR_PREPEND + 'delim-selector-form input:checked').val(),
				$selectbox = $(selectControlSelector),
				$options = $selectbox.find('option'),
				columns = [],
				$dialog;

			$(DATASET_FILTER_SELECTOR_PREPEND + 'listing').empty();
			$(DATASET_FILTER_SELECTOR_PREPEND + 'select-count').html('0 Datasets Selected');

			// Create the column groupings using the options in the select box
			$options.each(function (i, $option) {
				var optVal = $option.value,
					optGroups = optVal.split(delim);

				// Check to make sure that the delimited split properly 
				if (optGroups.length > 1) {
					for (var ogIdx = 0; ogIdx < optGroups.length; ogIdx++) {
						var group = optGroups[ogIdx];

						if (columns.length === ogIdx) {
							columns.push([]);
						}

						if (columns[ogIdx].indexOf(group) === -1) {
							columns[ogIdx].push(group);
						}
					}
				}
			});

			// Create the table using the groupings I just created
			var $table = $('<table />').attr('id', 'dataset-filter-table'),
				$tr = $('<tr />');

			$table.append($tr);

			$.each(columns, function (i, column) {
				var $td = $('<td />');
				for (var colIdx = 0; colIdx < column.length; colIdx++) {
					var group = column[colIdx],
						$div = $('<div />'),
						$input = $('<input />').attr({
						type: 'checkbox',
						name: 'dataset-filter-column-' + i,
						value: group
					});
					$div.append($input, group);
					$td.append($div);
				}
				$tr.append($td);
			});
			$(DATASET_FILTER_SELECTOR_PREPEND + 'listing').append($table);

			// A small hack to get the height auto-sizing correctly
			$dialog = $('#dataset-filter-modal');
			$dialog.dialog("option", "height", 'auto');
			if ($dialog.dialog().height() > $dialog.dialog("option", "maxHeight")) {
				$dialog.dialog("option", "height", $dialog.dialog("option", "maxHeight"));
			}
			bindCheckboxes();
		},
		bindCheckboxes = function () {
			$(DATASET_FILTER_SELECTOR_PREPEND + 'table input').on('change', function () {
				var $checkboxTable = $(DATASET_FILTER_SELECTOR_PREPEND + 'table'),
					columnCount = $checkboxTable.find('td').length,
					columnGroups = [],
					optionsSelector = '#dataset-id-selectbox option',
					$options = $(optionsSelector),
					regexStr = '',
					regex,
					ccIdx;

				// Unselect everything in the datatype listbox
				$options.prop('selected', false);

				for (ccIdx = 0; ccIdx < columnCount; ccIdx++) {
					var $checkedColumnBoxes = $('#dataset-filter-table td:nth-child(' + (ccIdx + 1) + ') input:checked');

					if ($checkedColumnBoxes.length === 0) {
						regexStr += '(.*)-?';
						columnGroups.push(['(.*)']);
					} else {
						var cbVals = [];
						$checkedColumnBoxes.each(function () {
							cbVals.push($(this).val());
						});
						regexStr += '(' + cbVals.join('|') + ')-?';
						columnGroups.push(['(.*)']);
					}
				}

				regexStr = regexStr.substring(0, regexStr.length - 2);
				regex = new RegExp(regexStr);

				for (var optIdx = 0; optIdx < $options.length; optIdx++) {
					var $option = $($options[optIdx]),
						name = $option.attr('name');

					if (regex.test(name)) {
						$option.prop('selected', true);
					}
				}

				// Update the select count text
				$(DATASET_FILTER_SELECTOR_PREPEND + 'select-count').html($(optionsSelector + ':selected').length + ' Datasets Selected');
			});
		};

	return {
		create: create
	};
}());
