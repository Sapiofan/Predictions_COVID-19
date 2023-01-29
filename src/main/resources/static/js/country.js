function chartLine(container, data) {
    // anychart.onDocumentReady(function () {
    // create data set on our data
    var dataSet = anychart.data.set(data);

    // map data for the first series, take x from the zero column and value from the first column of data set
    var firstSeriesData = dataSet.mapAs({x: 0, value: 1});

    var secondSeriesData = dataSet.mapAs({x: 0, value: 2});

    var thirdSeriesData = dataSet.mapAs({x: 0, value: 3});

    // create line chart
    var chart = anychart.line();

    // turn on chart animation
    chart.animation(true);

    // set chart padding
    chart.padding([10, 20, 5, 20]);

    // set tooltip mode to point
    chart.tooltip().positionMode('point');

    // set chart title text settings
    chart.title(
        'Covid-19'
    );

    // set yAxis title
    chart.yAxis().title('New cases of Covid-19');
    chart.xAxis().labels().padding(5);

    // create first series with mapped data
    var firstSeries = chart.line(firstSeriesData);
    firstSeries.name('Existed data and prediction');
    firstSeries.color("black");
    firstSeries.hovered().markers().enabled(true).type('circle').size(4);
    firstSeries
        .tooltip()
        .position('right')
        .anchor('left-center')
        .offsetX(5)
        .offsetY(5);

    var secondSeries = chart.line(secondSeriesData);
    secondSeries.name('Low bound');
    secondSeries.color("blue");
    secondSeries.hovered().markers().enabled(true).type('circle').size(4);
    secondSeries
        .tooltip()
        .position('right')
        .anchor('left-center')
        .offsetX(5)
        .offsetY(5);

    var thirdSeries = chart.line(thirdSeriesData);
    thirdSeries.name('High bound');
    thirdSeries.color("blue");
    thirdSeries.hovered().markers().enabled(true).type('circle').size(4);
    thirdSeries
        .tooltip()
        .position('right')
        .anchor('left-center')
        .offsetX(5)
        .offsetY(5);

    // turn the legend on
    chart.legend().enabled(true).fontSize(13).padding([0, 0, 10, 0]);

    // set container id for the chart
    chart.container(container);
    // initiate chart drawing
    chart.draw();

    var trial = document.getElementsByClassName("anychart-credits")
    for (let i = 0; i < trial.length; i++) {
        trial[i].remove();
    }
}

function chartColumn(container, data) {
        // create data set on our data
        var dataSet = anychart.data.set(data);

        // map data for the first series, take x from the zero column and value from the first column of data set
        var firstSeriesData = dataSet.mapAs({x: 0, value: 1});

        // map data for the second series, take x from the zero column and value from the second column of data set
        var secondSeriesData = dataSet.mapAs({x: 0, value: 2});

        // map data for the second series, take x from the zero column and value from the third column of data set
        var thirdSeriesData = dataSet.mapAs({x: 0, value: 3});

        // create bar chart
        var chart = anychart.column();

        // turn on chart animation
        chart.animation(true);

        // force chart to stack values by Y scale.
        chart.yScale().stackMode('value');

        // set chart title text settings
        chart.title('COVID-19');
        chart.title().padding([0, 0, 5, 0]);

        // helper function to setup label settings for all series
        var setupSeriesLabels = function (series, name) {
            series.name(name).stroke('3 #fff 1');
            series.hovered().stroke('3 #fff 1');
        };

        // temp variable to store series instance
        var series;

        // create first series with mapped data
        series = chart.column(firstSeriesData);
        setupSeriesLabels(series, 'Prediction');

        // create second series with mapped data
        series = chart.column(secondSeriesData);
        setupSeriesLabels(series, 'Low bound');

        // create third series with mapped data
        series = chart.column(thirdSeriesData);
        setupSeriesLabels(series, 'High bound');

        // turn on legend
        chart.legend().enabled(true).fontSize(13).padding([0, 0, 20, 0]);
        // set yAxis labels formatter
        chart.yAxis().labels().format('{%Value}{groupsSeparator: }');

        // set titles for axes
        chart.xAxis().title('Date');
        chart.yAxis().title('Cases');

        // set interactivity hover
        chart.interactivity().hoverMode('by-x');

        // chart.tooltip().valuePrefix('$').displayMode('union');

        chart.barsPadding(0);
        chart.barGroupsPadding(0);

        // set container id for the chart
        chart.container(container);

        // initiate chart drawing
        chart.draw();

        var trial = document.getElementsByClassName("anychart-credits")
        for (let i = 0; i < trial.length; i++) {
            trial[i].remove();
        }
}



const type = document.getElementById('type')
const quantity = document.getElementById('quantity')
const chartType = document.getElementById('chart-type')

type.addEventListener('change', (event) => {
    if (event.currentTarget.checked) {
        if(quantity.checked) {
            changeChartMode(true, true);
        } else {
            changeChartMode(true, false);
        }
    } else {
        if(quantity.checked) {
            changeChartMode(false, true);
        } else {
            changeChartMode(false, false);
        }
    }
})

quantity.addEventListener('change', (event) => {
    if (event.currentTarget.checked) {
        if(type.checked) {
            changeChartMode(true, true);
        } else {
            changeChartMode(false, true);
        }
    } else {
        if(type.checked) {
            changeChartMode(true, false);
        } else {
            changeChartMode(false, false);
        }
    }
})

chartType.addEventListener('change', (event) => {
    document.getElementById("line-chart").remove();
    document.getElementById("column-chart").remove();
    document.getElementById("line-chart-container").innerHTML += "<div id='line-chart'></div>";
    document.getElementById("column-chart-container").innerHTML += "<div id='column-chart'></div>";
    if (event.currentTarget.checked) {
        chartColumn("line-chart", getCases());
        chartColumn("column-chart", getDeaths());
        typeOfChartLinear = false;
    } else {
        chartLine("line-chart", getCases());
        chartLine("column-chart", getDeaths());
        typeOfChartLinear = true;
    }
})