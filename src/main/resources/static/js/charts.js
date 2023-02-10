function chartLine(container, data) {
    // create data set on our data
    var dataSet = anychart.data.set(data);

    // map data for the first series, take x from the zero column and value from the first column of data set
    var firstSeriesData = dataSet.mapAs({x: 0, value: 3});

    var secondSeriesData = dataSet.mapAs({x: 0, value: 1});

    var thirdSeriesData = dataSet.mapAs({x: 0, value: 2});

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
    firstSeries.name('High bound');
    // firstSeries.color("#EF476F");
    firstSeries.normal().stroke("#EF476F", 2);
    firstSeries.hovered().markers().enabled(true).type('circle').size(4);
    firstSeries
        .tooltip()
        .position('right')
        .anchor('left-center')
        .offsetX(5)
        .offsetY(5);

    var secondSeries = chart.line(secondSeriesData);
    secondSeries.name('Existed data and prediction');
    secondSeries.normal().stroke("#118AB2", 2);
    secondSeries.hovered().markers().enabled(true).type('circle').size(4);
    secondSeries
        .tooltip()
        .position('right')
        .anchor('left-center')
        .offsetX(5)
        .offsetY(5);

    var thirdSeries = chart.line(thirdSeriesData);
    thirdSeries.name('Low bound');
    thirdSeries.normal().stroke("#EF476F", 2);
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

    // map data for the first series, take x from the zero area and value from the first area of data set
    var firstSeriesData = dataSet.mapAs({ x: 0, value: 3 });

    // map data for the second series, take x from the zero area and value from the second area of data set
    var secondSeriesData = dataSet.mapAs({ x: 0, value: 1 });

    // map data for the second series, take x from the zero area and value from the third area of data set
    var thirdSeriesData = dataSet.mapAs({ x: 0, value: 2 });

    // create bar chart
    var chart = anychart.area();

    // turn on chart animation
    chart.animation(true);

    // turn on the crosshair
    var crosshair = chart.crosshair();
    crosshair.enabled(true).yStroke(null).xStroke('#fff').zIndex(39);
    crosshair.yLabel().enabled(false);

    // force chart to stack values by Y scale.
    chart.yScale().stackMode('value');

    // set chart title text settings
    chart.title('COVID-19');

    // helper function to setup label settings for all series
    var setupSeriesLabels = function (series, name) {
        series
            .name(name)
            .stroke('3 #fff 1')
            .fill(function () {
                return this.sourceColor + ' 0.8';
            });
        series.hovered().stroke('3 #fff 1');
        series
            .hovered()
            .markers()
            .enabled(true)
            .type('circle')
            .size(4)
            .stroke('1.5 #fff');
        series.markers().zIndex(100);
    };

    // temp variable to store series instance
    var series;

    // create first series with mapped data
    series = chart.area(firstSeriesData);
    setupSeriesLabels(series, 'High bound');

    // create second series with mapped data
    series = chart.area(secondSeriesData);
    setupSeriesLabels(series, 'Existed data and prediction');

    // create third series with mapped data
    series = chart.area(thirdSeriesData);
    setupSeriesLabels(series, 'Low bound');

    // turn on legend
    chart.legend().enabled(true).fontSize(13).padding([0, 0, 20, 0]);

    // set titles for axises
    chart.xAxis().title("Date");
    chart.yAxis().title('Cases');

    // interactivity and tooltip settings
    chart.interactivity().hoverMode('by-x');
    chart
        .tooltip()
        .displayMode('union');

    // chart.tooltip().valuePrefix('$').displayMode('union');

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

const type_r = document.getElementById('type-r')
const quantity_r = document.getElementById('quantity-r')
const chartType_r = document.getElementById('chart-type-r')
const dataType_r = document.getElementById('data-r')

type.addEventListener('change', (event) => {
    if (event.currentTarget.checked) {
        if(quantity.checked) {
            changeWorldChartMode(true, true);
        } else {
            changeWorldChartMode(true, false);
        }
    } else {
        if(quantity.checked) {
            changeWorldChartMode(false, true);
        } else {
            changeWorldChartMode(false, false);
        }
    }
})

quantity.addEventListener('change', (event) => {
    if (event.currentTarget.checked) {
        if(type.checked) {
            changeWorldChartMode(true, true);
        } else {
            changeWorldChartMode(false, true);
        }
    } else {
        if(type.checked) {
            changeWorldChartMode(true, false);
        } else {
            changeWorldChartMode(false, false);
        }
    }
})

chartType.addEventListener('change', (event) => {
    document.getElementById("line-chart").remove();
    document.getElementById("column-chart").remove();
    document.getElementById("line-chart-container").innerHTML += "<div id='line-chart'></div>";
    document.getElementById("column-chart-container").innerHTML += "<div id='column-chart'></div>";
    if (event.currentTarget.checked) {
        chartColumn("line-chart", getWorldCases("World"));
        chartColumn("column-chart", getWorldDeaths("World"));
        typeOfChartLinear = false;
    } else {
        chartLine("line-chart", getWorldCases("World"));
        chartLine("column-chart", getWorldDeaths("World"));
        typeOfChartLinear = true;
    }
})


type_r.addEventListener('change', (event) => {
    if (event.currentTarget.checked) {
        if(quantity_r.checked) {
            if(dataType_r.checked) {
                changeRegionChartMode("deaths", true, true);
            } else {
                changeRegionChartMode("cases", true, true);
            }
        } else {
            if(dataType_r.checked) {
                changeRegionChartMode("deaths", true, false);
            } else {
                changeRegionChartMode("cases", true, false);
            }
        }
    } else {
        if(quantity.checked) {
            if(dataType_r.checked) {
                changeRegionChartMode("deaths", false, true);
            } else {
                changeRegionChartMode("cases", false, true);
            }
        } else {
            if(dataType_r.checked) {
                changeRegionChartMode("deaths", false, false);
            } else {
                changeRegionChartMode("cases", false, false);
            }
        }
    }
})

quantity_r.addEventListener('change', (event) => {
    if (event.currentTarget.checked) {
        if(type.checked) {
            if(dataType_r.checked) {
                changeRegionChartMode("deaths", true, true);
            } else {
                changeRegionChartMode("cases", true, true);
            }
        } else {
            if(dataType_r.checked) {
                changeRegionChartMode("deaths", false, true);
            } else {
                changeRegionChartMode("cases", false, true);
            }
        }
    } else {
        if(type.checked) {
            if(dataType_r.checked) {
                changeRegionChartMode("deaths", true, false);
            } else {
                changeRegionChartMode("cases", true, false);
            }
        } else {
            if(dataType_r.checked) {
                changeRegionChartMode("deaths", false, false);
            } else {
                changeRegionChartMode("cases", false, false);
            }
        }
    }
})

chartType_r.addEventListener('change', (event) => {
    document.getElementById("container1").remove();
    document.getElementById("container2").remove();
    document.getElementById("container3").remove();
    document.getElementById("container4").remove();
    document.getElementById("container5").remove();
    document.getElementById("europe").innerHTML += "<div id='container1'></div>";
    document.getElementById("america").innerHTML += "<div id='container2'></div>";
    document.getElementById("asia").innerHTML += "<div id='container3'></div>";
    document.getElementById("oceania").innerHTML += "<div id='container4'></div>";
    document.getElementById("africa").innerHTML += "<div id='container5'></div>";

    if (event.currentTarget.checked) {
        if(dataType_r.checked) {
            chartColumn("container1", getWorldCases("Europe"));
            chartColumn("container2", getWorldCases("Americas"));
            chartColumn("container3", getWorldCases("Asia"));
            chartColumn("container4", getWorldCases("Oceania"));
            chartColumn("container5", getWorldCases("Africa"));
        } else {
            chartColumn("container1", getWorldDeaths("Europe"));
            chartColumn("container2", getWorldDeaths("Americas"));
            chartColumn("container3", getWorldDeaths("Asia"));
            chartColumn("container4", getWorldDeaths("Oceania"));
            chartColumn("container5", getWorldDeaths("Africa"));
        }
        typeOfChartLinearRegion = false;
    } else {
        if(dataType_r.checked) {
            chartLine("container1", getWorldCases("Europe"));
            chartLine("container2", getWorldCases("Americas"));
            chartLine("container3", getWorldCases("Asia"));
            chartLine("container4", getWorldCases("Oceania"));
            chartLine("container5", getWorldCases("Africa"));
        } else {
            chartLine("container1", getWorldDeaths("Europe"));
            chartLine("container2", getWorldDeaths("Americas"));
            chartLine("container3", getWorldDeaths("Asia"));
            chartLine("container4", getWorldDeaths("Oceania"));
            chartLine("container5", getWorldDeaths("Africa"));
        }
        typeOfChartLinearRegion = true;
    }
})

dataType_r.addEventListener('change', (event) => {
    if (event.currentTarget.checked) {
        if(type.checked) {
            if(quantity_r.checked) {
                changeRegionChartMode("deaths", true, true);
            } else {
                changeRegionChartMode("deaths", true, false);
            }
        } else {
            if(quantity_r.checked) {
                changeRegionChartMode("deaths", false, true);
            } else {
                changeRegionChartMode("deaths", false, false);
            }
        }
    } else {
        if(type.checked) {
            if(quantity_r.checked) {
                changeRegionChartMode("cases", true, true);
            } else {
                changeRegionChartMode("cases", true, false);
            }
        } else {
            if(quantity_r.checked) {
                changeRegionChartMode("cases", false, true);
            } else {
                changeRegionChartMode("cases", false, false);
            }
        }
    }
})