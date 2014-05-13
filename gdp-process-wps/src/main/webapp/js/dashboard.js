$(document).ready(function() {
    $('#loadProcessesButton').click(function() {
        $.get('list', function(processJSON) {
            $('#lastProcessLoad').text(new Date().timeNow());
            $('#processData').empty();
            if (processJSON.length) {
                var $table = $('<table>').appendTo($('#processData'));
                var $headerRow = $('<tr>').appendTo($table);
                $headerRow.append($('<th>').text('Identifier'));
                $headerRow.append($('<th>').text('Status'));
                $headerRow.append($('<th>').text('Created'));
                $headerRow.append($('<th>').text('Runtime'));
                $headerRow.append($('<th>').text('Output'));
                $.each(processJSON, function(idx, data) {
                    var $dataRow = $('<tr>').appendTo($table);
                    $dataRow.append($('<td>').text(data.identifier));
                    $dataRow.append($('<td>').text(data.status));
                    $dataRow.append($('<td>').text(data.creationTime));
                    $dataRow.append($('<td>').text(data.elapsedTime));
                    $dataRow.append($('<td>').text(data.output));
                });
            } else {
                $('#processData').text("No processes found");
            }
        });
    });
    
    $('#reportButton').click(function() {
        $.get('report', function(reportJSON) {
            $('#lastReportRun').text(new Date().timeNow());
            $('#reportData').empty();
            if (reportJSON.algorithms.length) {
                var $table = $('<table></table>').appendTo($('#reportData'));
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
            } else {
                $('#reportData').text("Nothing to report on");
            }
        });
    });
});

Date.prototype.timeNow = function () {
    var year = this.getFullYear();
    var month = (this.getMonth()+1 < 10 ? "0" : "") + (this.getMonth()+1);
    var day = (this.getDate() < 10 ? "0" : "") + this.getDate();
    var hours = (this.getHours() < 10 ? "0" : "") + this.getHours();
    var minutes = (this.getMinutes() < 10 ? "0" : "") + this.getMinutes();
    var seconds = this.getSeconds();
    return year + "-" + month + "-" + day + " " + hours + ":" + minutes + ":" + seconds;
};