import AdminDashboardState from './adminDashboardState.js';

const ChartHelper = (() => {
    function renderParticipationCharts(eventStats, clubStats, handlers = {}) {
        const chartLib = globalThis.Chart;
        if (!chartLib) {
            console.warn('Chart.js not available.');
            return;
        }

        const eventLabels = eventStats.map(stat => `${stat.eventTitle} (${stat.clubName})`);
        const eventRanks = eventLabels.map((_, index) => String(index + 1));
        const eventCounts = eventStats.map(stat => stat.registrationCount);
        const clubLabels = clubStats.map(stat => stat.clubName);
        const clubRanks = clubLabels.map((_, index) => String(index + 1));
        const clubCounts = clubStats.map(stat => stat.registrationCount);
        const eventColor = resolveCssVar('--accent');
        const clubColor = resolveCssVar('--accent-2');
        const gridColor = resolveCssVar('--stroke');
        const textColor = resolveChartTextColor();
        const axisTitleStyle = { color: textColor, font: { size: 12, weight: '600' } };
        const gridStyle = {
            color: withAlpha(gridColor, 0.35),
            borderDash: [4, 4]
        };
        const interaction = { mode: 'nearest', intersect: false };

        const eventCanvas = document.getElementById('adminEventRegistrationsChart');
        const clubCanvas = document.getElementById('adminTopClubsChart');

        AdminDashboardState.getEventChart()?.destroy?.();
        AdminDashboardState.getClubChart()?.destroy?.();
        AdminDashboardState.setEventChart(null);
        AdminDashboardState.setClubChart(null);

        if (eventCanvas) {
            applyChartHeight(eventCanvas, eventLabels.length);
            const chart = new chartLib(eventCanvas, {
                type: 'line',
                data: {
                    labels: eventRanks,
                    datasets: [{
                        label: 'Registrations',
                        data: eventCounts,
                        borderColor: eventColor,
                        backgroundColor: (context) => createAreaGradient(context, eventColor),
                        fill: true,
                        tension: 0.35,
                        pointRadius: 3,
                        pointHoverRadius: 5,
                        pointBackgroundColor: eventColor,
                        pointBorderColor: eventColor,
                        pointHitRadius: 8,
                        borderWidth: 2
                    }]
                },
                options: {
                    interaction,
                    responsive: true,
                    maintainAspectRatio: false,
                    layout: { padding: { right: 8 } },
                    onClick: (event, elements, chartInstance) => {
                        const index = resolveChartIndex(chartInstance, event);
                        if (index === null) {
                            return;
                        }
                        const stat = eventStats[index];
                        if (stat && typeof handlers.onEventSelected === 'function') {
                            handlers.onEventSelected(stat);
                        }
                    },
                    plugins: {
                        legend: { display: false },
                        tooltip: {
                            callbacks: {
                                title: (items) => items.map(item => eventLabels[item.dataIndex] || ''),
                                label: (item) => {
                                    const rank = item.dataIndex + 1;
                                    const count = item.parsed.y ?? 0;
                                    return `Rank: ${rank} · Registrations: ${count}`;
                                }
                            }
                        }
                    },
                    scales: {
                        x: {
                            ticks: {
                                color: textColor,
                                maxTicksLimit: 6,
                                autoSkip: true,
                                callback: (_, index) => eventRanks[index]
                            },
                            grid: { display: false },
                            title: { display: true, text: 'Rank (1 = most registrations)', ...axisTitleStyle }
                        },
                        y: {
                            beginAtZero: true,
                            ticks: { precision: 0, color: textColor },
                            grid: gridStyle,
                            title: { display: true, text: 'Registrations', ...axisTitleStyle }
                        }
                    }
                }
            });
            AdminDashboardState.setEventChart(chart);
        }

        if (clubCanvas) {
            applyChartHeight(clubCanvas, clubLabels.length);
            const chart = new chartLib(clubCanvas, {
                type: 'line',
                data: {
                    labels: clubRanks,
                    datasets: [{
                        label: 'Registrations',
                        data: clubCounts,
                        borderColor: clubColor,
                        backgroundColor: (context) => createAreaGradient(context, clubColor),
                        fill: true,
                        tension: 0.35,
                        pointRadius: 3,
                        pointHoverRadius: 5,
                        pointBackgroundColor: clubColor,
                        pointBorderColor: clubColor,
                        pointHitRadius: 8,
                        borderWidth: 2
                    }]
                },
                options: {
                    interaction,
                    responsive: true,
                    maintainAspectRatio: false,
                    layout: { padding: { right: 8 } },
                    onClick: (event, elements, chartInstance) => {
                        const index = resolveChartIndex(chartInstance, event);
                        if (index === null) {
                            return;
                        }
                        const stat = clubStats[index];
                        if (stat && typeof handlers.onClubSelected === 'function') {
                            handlers.onClubSelected(stat);
                        }
                    },
                    plugins: {
                        legend: { display: false },
                        tooltip: {
                            callbacks: {
                                title: (items) => items.map(item => clubLabels[item.dataIndex] || ''),
                                label: (item) => {
                                    const rank = item.dataIndex + 1;
                                    const count = item.parsed.y ?? 0;
                                    return `Rank: ${rank} · Registrations: ${count}`;
                                }
                            }
                        }
                    },
                    scales: {
                        x: {
                            ticks: {
                                color: textColor,
                                maxTicksLimit: 6,
                                autoSkip: true,
                                callback: (_, index) => clubRanks[index]
                            },
                            grid: { display: false },
                            title: { display: true, text: 'Rank (1 = most registrations)', ...axisTitleStyle }
                        },
                        y: {
                            beginAtZero: true,
                            ticks: { precision: 0, color: textColor },
                            grid: gridStyle,
                            title: { display: true, text: 'Registrations', ...axisTitleStyle }
                        }
                    }
                }
            });
            AdminDashboardState.setClubChart(chart);
        }
    }

    function resolveChartIndex(chart, event) {
        if (!chart) {
            return null;
        }
        const points = chart.getElementsAtEventForMode(
            event,
            'nearest',
            { intersect: false },
            true
        );
        if (!points.length) {
            return null;
        }
        return points[0].index ?? null;
    }

    function applyChartHeight(canvas, itemCount) {
        if (!canvas) {
            return;
        }
        const minHeight = 240;
        const maxHeight = 320;
        const perItem = 6;
        const height = Math.min(Math.max(minHeight + itemCount * perItem, minHeight), maxHeight);
        canvas.height = height;
        canvas.style.height = `${height}px`;
    }

    function resolveCssVar(name) {
        const value = getComputedStyle(document.documentElement)
            .getPropertyValue(name)
            .trim();
        if (!value) {
            throw new Error(`Missing CSS variable ${name}`);
        }
        return value;
    }

    function resolveChartTextColor() {
        const theme = document.documentElement.dataset.bsTheme || 'light';
        if (theme === 'dark') {
            return resolveCssVar('--muted');
        }
        return resolveCssVar('--ink');
    }

    function createAreaGradient(context, color) {
        const { chart } = context;
        if (!chart) {
            return color;
        }
        const { ctx, chartArea } = chart;
        if (!chartArea) {
            return color;
        }
        const gradient = ctx.createLinearGradient(0, chartArea.top, 0, chartArea.bottom);
        gradient.addColorStop(0, withAlpha(color, 0.35));
        gradient.addColorStop(1, withAlpha(color, 0.05));
        return gradient;
    }

    function withAlpha(color, alpha) {
        const rgb = toRgb(color);
        if (!rgb) {
            return color;
        }
        return `rgba(${rgb.r}, ${rgb.g}, ${rgb.b}, ${alpha})`;
    }

    function toRgb(color) {
        if (!color) {
            return null;
        }
        const value = color.trim();
        if (value.startsWith('rgb')) {
            const parts = value.replaceAll(/rgba?\(|\)/g, '').split(',').map(part => part.trim());
            if (parts.length >= 3) {
                return {
                    r: Number(parts[0]),
                    g: Number(parts[1]),
                    b: Number(parts[2])
                };
            }
            return null;
        }
        if (value.startsWith('#')) {
            const hex = value.slice(1);
            if (hex.length === 3) {
                const r = Number.parseInt(hex[0] + hex[0], 16);
                const g = Number.parseInt(hex[1] + hex[1], 16);
                const b = Number.parseInt(hex[2] + hex[2], 16);
                return { r, g, b };
            }
            if (hex.length === 6) {
                const r = Number.parseInt(hex.slice(0, 2), 16);
                const g = Number.parseInt(hex.slice(2, 4), 16);
                const b = Number.parseInt(hex.slice(4, 6), 16);
                return { r, g, b };
            }
        }
        return null;
    }

    return {
        renderParticipationCharts
    };
})();

export default ChartHelper;
