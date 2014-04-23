$(document).ready(function() {
    $('#loadProcessesButton').click(function() {
        $.post('dashboard', { action: 'loadProcesses', startAt: 0}, function(dashboardDataJSON) {
            $('#processTableContainer').empty();
            var $json = $.parseJSON(dashboardDataJSON);
            var $table = $('<table>').appendTo($('#processTableContainer'));
            var $headerRow = $('<tr>').appendTo($table);
            $headerRow.append($('<th>').text('Identifier'));
            $headerRow.append($('<th>').text('Status'));
            $headerRow.append($('<th>').text('Created'));
            $headerRow.append($('<th>').text('Runtime'));
            $headerRow.append($('<th>').text('Output'));
            $.each($json, function(idx, data) {
                var $rowClass = "odd";
                if (idx % 2 === 0) {
                    $rowClass = "even";
                }
                var $dataRow = $('<tr>').appendTo($table);
                $dataRow.addClass($rowClass);
                $dataRow.append($('<td>').text(data.identifier));
                $dataRow.append($('<td>').text(data.status));
                $dataRow.append($('<td>').text(data.creationTime));
                $dataRow.append($('<td>').text(data.elapsedTime));
                $dataRow.append($('<td>').text(data.output));
            });
        });
    });
    
    $('#reportButton').click(function() {
        $.post('dashboard', { action: 'report' }, function(reportJSON) {
            $('#reportContainer').empty();
            var $json = $.parseJSON(reportJSON);
            var $table = $('<table>').appendTo($('#reportContainer'));
            var $headerRow = $('<tr>').appendTo($table);
            $headerRow.append($('<th>').text('Data'));
            $headerRow.append($('<th>').text('Count'));
            $.each($json.algorithms, function(idx, data) {
                var $algorithmRow = $('<tr>').appendTo($table);
                $algorithmRow.addClass('reportHeader');
                $algorithmRow.append($('<td>').text(data.identifier));
                $algorithmRow.append($('<td>').text(data.count));
                $.each($(data.dataSets), function(idx, dataset) {
                    var $rowClass = "odd";
                    if (idx % 2 === 0) {
                        $rowClass = "even";
                    }
                    var $dataSetRow = $('<tr>').appendTo($table);
                    $dataSetRow.addClass($rowClass);
                    $dataSetRow.append($('<td>').addClass('indented').text(dataset.dataSetURI));
                    $dataSetRow.append($('<td>').text(dataset.count));
                });
            });
        });
    });
});