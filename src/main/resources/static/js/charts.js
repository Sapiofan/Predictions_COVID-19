anychart.onDocumentReady(function () {
    // create data set on our data
    var dataSet = anychart.data.set(getData());

    // map data for the first series, take x from the zero column and value from the first column of data set
    var firstSeriesData = dataSet.mapAs({ x: 0, value: 1 });

    // create line chart
    var chart = anychart.line();

    // turn on chart animation
    chart.animation(true);

    // set chart padding
    chart.padding([10, 20, 5, 20]);

    // turn on the crosshair
    // chart.crosshair().enabled(true).yLabel(false).yStroke(null);

    // set tooltip mode to point
    chart.tooltip().positionMode('point');

    // set chart title text settings
    chart.title(
        'Covid-19 new cases for last 4 months'
    );

    // set yAxis title
    chart.yAxis().title('New cases of Covid-19');
    chart.xAxis().labels().padding(5);

    // create first series with mapped data
    var firstSeries = chart.line(firstSeriesData);
    // firstSeries.name('Brandy');
    firstSeries.color("black");
    firstSeries.hovered().markers().enabled(true).type('circle').size(4);
    firstSeries
        .tooltip()
        .position('right')
        .anchor('left-center')
        .offsetX(5)
        .offsetY(5);

    // turn the legend on
    chart.legend().enabled(true).fontSize(13).padding([0, 0, 10, 0]);

    // set container id for the chart
    chart.container('line-chart');
    // initiate chart drawing
    chart.draw();

    var trial = document.getElementsByClassName("anychart-credits")
    for (let i = 0; i < trial.length; i++) {
        trial[i].remove();
    }
});

anychart.onDocumentReady(function () {
    // create column chart
    var chart = anychart.column();

    // turn on chart animation
    chart.animation(true);

    // set chart title text settings
    chart.title('Covid-19 new cases for last 4 months');

    // create area series with passed data
    // var series = chart.column([
    //     ['Rouge', '80540'],
    //     ['Foundation', '94190'],
    //     ['Mascara', '102610'],
    //     ['Lip gloss', '110430'],
    //     ['Lipstick', '128000'],
    //     ['Nail polish', '143760'],
    //     ['Eyebrow pencil', '170670'],
    //     ['Eyeliner', '213210'],
    //     ['Eyeshadows', '249980']
    // ]);

    // console.log([
    //     ['Rouge', '80540'],
    //     ['Foundation', '94190'],
    //     ['Mascara', '102610'],
    //     ['Lip gloss', '110430'],
    //     ['Lipstick', '128000'],
    //     ['Nail polish', '143760'],
    //     ['Eyebrow pencil', '170670'],
    //     ['Eyeliner', '213210'],
    //     ['Eyeshadows', '249980']
    // ]);

    // console.log(getData());

    var series = chart.column(getData());

    // set series tooltip settings
    series.tooltip().titleFormat('{%X}');

    series
        .tooltip()
        .position('center-top')
        .anchor('center-bottom')
        .offsetX(0)
        .offsetY(5)
        .format('{%Value}{groupsSeparator: } cases');

    // set scale minimum
    chart.yScale().minimum(0);

    // set yAxis labels formatter
    chart.yAxis().labels().format('{%Value}{groupsSeparator: }');

    // tooltips position and interactivity settings
    chart.tooltip().positionMode('point');
    chart.interactivity().hoverMode('by-x');

    // axes titles
    chart.xAxis().title('Date');
    chart.yAxis().title('New cases of Covid-19');

    // set container id for the chart
    chart.container('column-chart');

    // initiate chart drawing
    chart.draw();

    var trial = document.getElementsByClassName("anychart-credits")
    for (let i = 0; i < trial.length; i++) {
        trial[i].remove();
    }
});