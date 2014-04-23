$(document).ready(function() {
    $('#loadProcessesButton').click(function() {
        $.post('dashboard', { action: 'loadProcesses', startAt: 0}, function(dashboardDataJSON) {
            $('#processTableContainer').empty();
            var json = $.parseJSON(dashboardDataJSON);
            var $table = $('<table>').appendTo($('#processTableContainer'));
            var $headerRow = $('<tr>').appendTo($table);
            $headerRow.append($('<th>')).text('Identifier');
            $headerRow.append($('<th>')).text('Status');
            $headerRow.append($('<th>')).text('Created');
            $headerRow.append($('<th>')).text('Runtime');
            $headerRow.append($('<th>')).text('Output');
            $.each($(json), function(idx, data) {
                var $dataRow = $('<tr>').appendTo($table);
                $dataRow.append($('<td>').text(data.identifier));
                $dataRow.append($('<td>').text(data.status));
                $dataRow.append($('<td>').text(data.creationTime));
                $dataRow.append($('<td>').text(data.elapsedTime));
                $dataRow.append($('<td>').text(''));
            });
        });
    });
    
    $('#reportButton').click(function() {
        $.post('dashboard', { action: 'report' }, function(reportJSON) {
            $('#reportContainer').empty();
            var json = $.parseJSON(reportJSON);
            var $table = $('<table>').appendTo($('#reportContainer'));
            $.each($(json.algorithms), function(idx, data) {
                var $algorithmRow = $('<tr>').appendTo($table);
                $algorithmRow.append($('<td>').text(data.identifier));
                $algorithmRow.append($('<td>').text(data.count));
                var $dataSetRow = $('<tr>').appendTo($table);
                var $innerTable = $dataSetRow.append($('<td>').append($('<table>')));
                $.each($(data.dataSets), function(idx, dataset) {
                    var $innerRow = $('<tr>').appendTo($innerTable);
                    $innerRow.append($('<td>').text(dataset.dataSetURI));
                    $innerRow.append($('<td>').text(dataset.count));
                });
            });
        });
    });
});
