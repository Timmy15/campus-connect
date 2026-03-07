export function createTableChartToggle({
    tableButtonId,
    chartButtonId,
    tableViewId,
    chartViewId,
    defaultView = 'table',
    onViewChange
}) {
    const tableButton = document.getElementById(tableButtonId);
    const chartButton = document.getElementById(chartButtonId);
    const tableView = document.getElementById(tableViewId);
    const chartView = document.getElementById(chartViewId);

    if (!tableButton || !chartButton || !tableView || !chartView) {
        return {
            showTable: () => {},
            showChart: () => {},
            setView: () => {},
            getView: () => defaultView
        };
    }

    let currentView = defaultView === 'chart' ? 'chart' : 'table';

    const setView = (view) => {
        currentView = view === 'chart' ? 'chart' : 'table';
        const showTable = currentView === 'table';

        tableView.classList.toggle('d-none', !showTable);
        chartView.classList.toggle('d-none', showTable);

        tableButton.classList.toggle('active', showTable);
        chartButton.classList.toggle('active', !showTable);
        tableButton.setAttribute('aria-pressed', showTable ? 'true' : 'false');
        chartButton.setAttribute('aria-pressed', showTable ? 'false' : 'true');

        if (typeof onViewChange === 'function') {
            onViewChange(currentView);
        }
    };

    tableButton.addEventListener('click', () => setView('table'));
    chartButton.addEventListener('click', () => setView('chart'));

    setView(currentView);

    return {
        showTable: () => setView('table'),
        showChart: () => setView('chart'),
        setView,
        getView: () => currentView
    };
}

export function createHorizontalBarChart({
    canvas,
    labels,
    values,
    tooltipFormatter,
    onClick,
    axisTitles,
    tickColor,
    axisTitleColor,
    color = '#0d6efd',
    barThickness = 18,
    borderRadius = 4
}) {
    const ChartLib = globalThis.Chart;
    if (!ChartLib || !canvas) {
        return null;
    }

    const resolvedTickColor = tickColor || undefined;
    const resolvedAxisTitleColor = axisTitleColor || tickColor || undefined;

    return new ChartLib(canvas, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: 'Failures',
                data: values,
                backgroundColor: color,
                borderRadius: borderRadius,
                barThickness: barThickness
            }]
        },
        options: {
            indexAxis: 'y',
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            if (typeof tooltipFormatter === 'function') {
                                return tooltipFormatter(context);
                            }
                            return `Value: ${context.parsed.x}`;
                        }
                    }
                }
            },
            scales: {
                x: {
                    beginAtZero: true,
                    ticks: { precision: 0, color: resolvedTickColor },
                    title: {
                        display: Boolean(axisTitles?.x),
                        text: axisTitles?.x || '',
                        color: resolvedAxisTitleColor
                    }
                },
                y: {
                    ticks: { autoSkip: false, color: resolvedTickColor },
                    title: {
                        display: Boolean(axisTitles?.y),
                        text: axisTitles?.y || '',
                        color: resolvedAxisTitleColor
                    }
                }
            },
            onClick: function(event, elements) {
                if (!elements || elements.length === 0) return;
                if (typeof onClick === 'function') {
                    onClick(elements[0].index);
                }
            }
        }
    });
}

export function destroyChart(chartInstance) {
    if (chartInstance) {
        chartInstance.destroy();
    }
    return null;
}
