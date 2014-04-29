$(document).ready(function() {
    $('#loadProcessesButton').click(function() {
        $.get('list', function(dashboardDataJSON) {
            $('#processTableContainer').empty();
            var $table = $('<table>').appendTo($('#processTableContainer'));
            var $headerRow = $('<tr>').appendTo($table);
            $headerRow.append($('<th>').text('Identifier'));
            $headerRow.append($('<th>').text('Status'));
            $headerRow.append($('<th>').text('Created'));
            $headerRow.append($('<th>').text('Runtime'));
            $headerRow.append($('<th>').text('Output'));
            $.each(dashboardDataJSON, function(idx, data) {
                var $dataRow = $('<tr>').appendTo($table);
                $dataRow.append($('<td>').text(data.identifier));
                $dataRow.append($('<td>').text(data.status));
                $dataRow.append($('<td>').text(data.creationTime));
                $dataRow.append($('<td>').text(data.elapsedTime));
                $dataRow.append($('<td>').text(data.output));
            });
        });
    });
    
    $('#reportButton').click(function() {
        $.get('report', function(reportJSON) {
            $('#reportContainer').empty();
            var $table = $('<table></table>').appendTo($('#reportContainer'));
            var $headerRow = $('<tr>').appendTo($table);
            $headerRow.append($('<th>').text('Data'));
            $headerRow.append($('<th>').text('Count'));
            $.each(reportJSON.algorithms, function(idx, data) {
                var $algorithmRow = $('<tr>').appendTo($table);
                $algorithmRow.addClass('reportHeader');
                $algorithmRow.append($('<td>').text(data.identifier));
                $algorithmRow.append($('<td>').text(data.count));
                $.each($(data.dataSets), function(idx, dataset) {
                    var $dataSetRow = $('<tr>').appendTo($table);
                    $dataSetRow.append($('<td>').addClass('indented').text(dataset.dataSetURI));
                    $dataSetRow.append($('<td>').text(dataset.count));
                });
            });
        });
    });
});