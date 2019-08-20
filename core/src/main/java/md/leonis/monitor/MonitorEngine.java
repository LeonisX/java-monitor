package md.leonis.monitor;

import md.leonis.monitor.config.*;
import md.leonis.monitor.model.LogMetric;
import md.leonis.monitor.model.MetricsWithDate;
import md.leonis.monitor.model.Stats;
import md.leonis.monitor.source.HttpSource;
import md.leonis.monitor.source.JdbcSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class MonitorEngine {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitorEngine.class);

    private static Config config = ConfigHolder.getInstance();
    private static GuiConfig gui = config.getGui();
    private static List<Chart> charts = gui.getCharts().getItems();
    private static List<Task> tasks = config.getTasks();

    static final String NO_TASKS = "No monitoring tasks. Please, setup config.yml file.";

    private List<Stats> statsList;
    private Map<String, Integer> chartsByIdMap = new HashMap<>();

    private Map<String, Metric> metricsByNameMap;
    private Map<String, Metric> chartMetricsByNameMap; // Only metrics for charts

    private Map<String, Integer> taskIdByMetricNameMap = new HashMap<>();

    private List<LogMetric> logMetricsList = new ArrayList<>();

    private volatile LocalDateTime currentLocalDateTime;

    MonitorEngine() {
        statsList = tasks.stream().map(task -> FileUtils.loadStats(task.getName())).collect(Collectors.toList());

        // List of LineChart
        for (int i = 0; i < charts.size(); i++) {
            Chart chart = charts.get(i);
            chartsByIdMap.put(chart.getId(), i);
        }

        if (tasks.isEmpty()) {
            LOGGER.warn(NO_TASKS);
        }

        // List of lists of Series data
        tasks.forEach(task ->
                task.getMetrics().forEach(metric -> {
                    if (metric.isLogAnyChange()) {
                        logMetricsList.add(new LogMetric(metric.getName(), null));
                    }
                })
        );

        for (int i = 0; i < tasks.size(); i++) {
            for (Metric metric : tasks.get(i).getMetrics()) {
                taskIdByMetricNameMap.put(metric.getName(), i);
            }
        }

        metricsByNameMap = tasks.stream().flatMap(t -> t.getMetrics().stream()).collect(Collectors.toMap(Metric::getName, f -> f));

        chartMetricsByNameMap = metricsByNameMap.entrySet().stream().filter(e -> e.getValue().getChartId() != null).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    void getStats(Task task) {
        Map<String, Long> map = null;
        switch (task.getRequest()) {
            case HTTP:
                map = HttpSource.executeTask(task);
                break;

            case JDBC:
                map = JdbcSource.executeTask(task);
                break;
        }

        map = map.entrySet().stream().filter(e -> metricsByNameMap.containsKey(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        MetricsWithDate metricsWithDate = new MetricsWithDate(currentLocalDateTime, map);
        statsList.get(tasks.indexOf(task)).getMetrics().add(metricsWithDate);

        logMetricsList.forEach(logMetric -> {
            if (metricsByNameMap.get(logMetric.getName()).isLogAnyChange()) {
                Long val = metricsWithDate.getMetrics().get(logMetric.getName());
                if (null == logMetric.getValue()) {
                    logMetric.setValue(val);
                } else if (!logMetric.getValue().equals(val) && val != null) {
                    LOGGER.warn(String.format("%s::%s: %s -> %s", task.getName(), logMetric.getName(), logMetric.getValue(), val));
                    logMetric.setValue(val);
                }
            }
        });
    }

    void saveStats() {
        for (int i = 0; i < statsList.size(); i++) {
            FileUtils.saveStats(tasks.get(i).getName(), statsList.get(i));
        }

        FileUtils.saveConfig(config);
        LOGGER.info("Stats and config saved.");
    }

    List<Stats> getStatsList() {
        return statsList;
    }

    Map<String, Integer> getChartsByIdMap() {
        return chartsByIdMap;
    }

    Map<String, Metric> getChartMetricsByNameMap() {
        return chartMetricsByNameMap;
    }

    Map<String, Integer> getTaskIdByMetricNameMap() {
        return taskIdByMetricNameMap;
    }

    public void setCurrentLocalDateTime(LocalDateTime currentLocalDateTime) {
        this.currentLocalDateTime = currentLocalDateTime;
    }
}
