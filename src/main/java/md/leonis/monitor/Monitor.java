package md.leonis.monitor;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import md.leonis.monitor.config.*;
import md.leonis.monitor.model.LogField;
import md.leonis.monitor.model.Metric;
import md.leonis.monitor.model.Stats;
import md.leonis.monitor.source.HttpSource;
import md.leonis.monitor.source.JdbcSource;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class Monitor extends Application {

    private static final Log LOGGER = LogFactory.getLog(FileUtils.class);

    private static Config config = ConfigHolder.getInstance();
    private static GuiConfig gui = config.getGui();
    private static List<Chart> charts = gui.getCharts();
    private static List<Task> tasks = config.getTasks();

    private static final String NO_TASKS = "No monitoring tasks. Please, setup config.yml file.";

    private List<Stats> statsList;
    private List<LineChart> chartList = new ArrayList<>();
    private Map<String, Integer> chartsByIdMap = new HashMap<>();

    private Map<String, Field> fieldsByNameMap = new HashMap<>();
    private Map<String, Field> chartFieldsByNameMap = new HashMap<>(); // Only fields for charts

    private List<List<XYChart.Series<String, Long>>> chartsDataList;

    private List<LogField> logFieldsList = new ArrayList<>();

    private double chartScale = gui.getHorizontalScale();
    private int chartPageSize = gui.getPageSize();
    private int chartOffset = 0;

    private TextField upperBoundTextField = new TextField();
    private TextField tickUnitTextField = new TextField();

    private Label offsetLabel = new Label();
    private Label pageLabel = new Label();

    private boolean canDisplay;

    @Override
    public void start(Stage stage) {
        LoggerUtils.disableApacheHttpLogs();

        statsList = tasks.stream().map(task -> FileUtils.loadStats(task.getName())).collect(Collectors.toList());

        canDisplay = !statsList.isEmpty();

        // List of LineChart
        for (int i = 0; i < charts.size(); i++) {
            Chart chart = charts.get(i);
            chartList.add(createLineChart(chart.getLowerBound(), chart.getUpperBound(), chart.getTickUnit()));
            chartsByIdMap.put(chart.getId(), i);
        }

        if (canDisplay) {
            chartOffset = Math.max(0, statsList.get(0).getMetrics().size() - (int) (chartPageSize * chartScale));
        }

        // List of lists of Series data
        chartsDataList = chartList.stream().map(c -> new ArrayList<XYChart.Series<String, Long>>()).collect(Collectors.toList());
        tasks.forEach(task ->
                task.getFields().forEach(field -> {
                    if (field.getChartId() != null) {
                        Integer chartId = chartsByIdMap.get(field.getChartId());
                        chartsDataList.get(chartId).add(new XYChart.Series<>(field.getName(), FXCollections.observableArrayList()));
                    }
                    if (field.isLogAnyChange()) {
                        logFieldsList.add(new LogField(field.getName(), null));
                    }
                })
        );

        fieldsByNameMap = tasks.stream().flatMap(t -> t.getFields().stream()).collect(Collectors.toMap(Field::getName, f -> f));

        chartFieldsByNameMap = fieldsByNameMap.entrySet().stream().filter(e -> e.getValue().getChartId() != null).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        fillCharts();

        customizeCharts(stage);

        TabPane tabPane = new TabPane();
        for (int i = 0; i < chartList.size(); i++) {
            Tab tab = newTab(charts.get(i).getName(), chartList.get(i));
            tab.setId(String.valueOf(i));
            tabPane.getTabs().add(tab);
        }

        tabPane.getSelectionModel().selectedItemProperty().addListener(
                (ov, prevTab, newTab) -> showTabStats(Integer.parseInt(newTab.getId()))
        );

        showTabStats(0);

        BorderPane pane = new BorderPane();
        if (canDisplay) {
            pane.setCenter(tabPane);
        } else {
            LOGGER.warn(NO_TASKS);
            pane.setCenter(new Label(NO_TASKS));
        }

        Button fastBackward = new Button(" <<< ");
        fastBackward.setOnAction(e -> {
            chartOffset = 0;
            fillCharts();
        });
        Button backward = new Button(" << ");
        backward.setOnAction(e -> {
            chartOffset -= (int) (chartPageSize * chartScale);
            chartOffset = Math.max(chartOffset, 0);
            fillCharts();
        });
        Button fastForward = new Button(" >>> ");
        fastForward.setOnAction(e -> {
            chartOffset = statsList.get(0).getMetrics().size() - (int) (chartPageSize * chartScale);
            chartOffset = Math.max(chartOffset, 0);
            fillCharts();
        });
        Button forward = new Button(" >> ");
        forward.setOnAction(e -> {
            chartOffset = Math.min(chartOffset + (int) (chartPageSize * chartScale), statsList.get(0).getMetrics().size() - (int) (chartPageSize * chartScale));
            chartOffset = Math.max(chartOffset, 0);
            fillCharts();
        });
        Button plus = new Button(" + ");
        plus.setOnAction(e -> {
            chartScale *= 2;
            gui.setHorizontalScale(chartScale);
            chartOffset = Math.min(chartOffset + (int) (chartPageSize * chartScale), statsList.get(0).getMetrics().size()) - (int) (chartPageSize * chartScale);
            chartOffset = Math.max(chartOffset, 0);
            fillCharts();
        });
        Button minus = new Button(" - ");
        minus.setOnAction(e -> {
            chartScale /= 2;
            gui.setHorizontalScale(chartScale);
            fillCharts();
        });

        offsetLabel.setPadding(new Insets(0, 30, 0, 0));

        Label upperBoundLabel = new Label("Chart Upper Bound:");

        UnaryOperator<TextFormatter.Change> integerFilter = change -> {
            String input = change.getText();
            if (input.matches("[0-9]*")) {
                return change;
            }
            return null;
        };

        upperBoundTextField.setTextFormatter(new TextFormatter<String>(integerFilter));
        upperBoundTextField.setPrefWidth(60);

        upperBoundTextField.setOnKeyReleased(value -> {
            if (StringUtils.isNotBlank(upperBoundTextField.getText())) {
                int newValue = Integer.parseInt(upperBoundTextField.getText());
                int chartId = Integer.parseInt(tabPane.getSelectionModel().getSelectedItem().getId());
                ((NumberAxis) chartList.get(chartId).getYAxis()).setUpperBound(newValue);
                charts.get(chartId).setUpperBound(newValue);
            }
        });

        Label separator1 = new Label();
        separator1.setPadding(new Insets(0, 0, 0, 30));

        Label tickUnitLabel = new Label("Chart Tick Unit:");

        tickUnitTextField.setTextFormatter(new TextFormatter<String>(integerFilter));
        tickUnitTextField.setPrefWidth(60);

        tickUnitTextField.setOnKeyReleased(value -> {
            if (StringUtils.isNotBlank(tickUnitTextField.getText())) {
                int newValue = Integer.parseInt(tickUnitTextField.getText());
                int chartId = Integer.parseInt(tabPane.getSelectionModel().getSelectedItem().getId());
                ((NumberAxis) chartList.get(chartId).getYAxis()).setTickUnit(newValue);
                charts.get(chartId).setTickUnit(newValue);
            }
        });

        HBox hBox = new HBox(tickUnitLabel, tickUnitTextField, upperBoundLabel, upperBoundTextField, separator1,
                fastBackward, backward, forward, fastForward, offsetLabel, plus, minus, pageLabel);

        hBox.getChildren().forEach(c -> c.setDisable(!canDisplay));

        hBox.setAlignment(Pos.CENTER);
        hBox.setSpacing(5);
        hBox.setPadding(new Insets(0, 0, 5, 0));

        pane.setBottom(hBox);
        //pane.getStylesheets().add("modena.css");

        stage.setTitle("Java Monitor");
        stage.setScene(new Scene(pane, gui.getWindow().getWidth(), gui.getWindow().getHeight()));
        stage.setWidth(gui.getWindow().getWidth());
        stage.setHeight(gui.getWindow().getHeight());
        stage.widthProperty().addListener((obs, oldVal, newVal) -> gui.getWindow().setWidth(newVal.intValue()));
        stage.heightProperty().addListener((obs, oldVal, newVal) -> gui.getWindow().setHeight(newVal.intValue()));
        stage.show();

        List<Timeline> timelineList = tasks.stream().map(task -> {
            Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(config.getRequestIntervalInSeconds()), ae -> getStats(task)));
            timeline.setCycleCount(Animation.INDEFINITE);
            timeline.setDelay(Duration.seconds(task.getTimeOffsetInSeconds()));
            return timeline;
        }).collect(Collectors.toList());

        ParallelTransition pt = new ParallelTransition();
        pt.getChildren().addAll(timelineList);
        pt.play();

        Timeline saveTimeline = new Timeline(new KeyFrame(Duration.minutes(config.getSaveStateIntervalInSeconds()), ae -> saveStats()));
        saveTimeline.setCycleCount(Animation.INDEFINITE);
        saveTimeline.play();
    }

    private void showTabStats(int tabId) {
        tickUnitTextField.setText(String.valueOf(charts.get(tabId).getTickUnit()));
        upperBoundTextField.setText(String.valueOf(charts.get(tabId).getUpperBound()));
    }

    private void fillCharts() {
        if (canDisplay) {
            int toIndex = Math.min(chartOffset + (int) (chartPageSize * chartScale), statsList.get(0).getMetrics().size());
            List<List<Metric>> subLists = statsList.stream().map(stats -> {
                if (toIndex > stats.getMetrics().size()) {
                    return new ArrayList<Metric>();
                } else {
                    return stats.getMetrics().subList(chartOffset, toIndex);
                }
            }).collect(Collectors.toList());

            for (List<XYChart.Series<String, Long>> series : chartsDataList) {
                for (XYChart.Series<String, Long> d : series) {
                    String name = d.getName();
                    d.getData().clear();
                    Integer chartId = chartsByIdMap.get(chartFieldsByNameMap.get(name).getChartId());
                    d.getData().addAll(subLists.get(chartId).stream().map(s -> new XYChart.Data<>(s.toString(), s.getMetrics().get(name))).collect(Collectors.toList()));
                }
            }

            offsetLabel.setText(String.format("[%s - %s] of %s", chartOffset, toIndex, statsList.get(0).getMetrics().size()));
            pageLabel.setText(String.format("Page size: %s", (int) (chartPageSize * chartScale)));
        }
    }

    private Tab newTab(String name, LineChart chart) {
        Tab tab = new Tab();
        tab.setText(name);
        VBox root = new VBox(chart);
        tab.setContent(root);
        return tab;
    }

    @SuppressWarnings("all")
    private LineChart createLineChart(int lowerBound, int upperBound, int tickUnit) {
        CategoryAxis xAxis = new CategoryAxis();
        //xAxis.setLabel("Time");

        NumberAxis yAxis = new NumberAxis(lowerBound, upperBound, tickUnit);
        //yAxis.setLabel("#");
        LineChart linechart = new LineChart(xAxis, yAxis);

        linechart.setPrefWidth(1200);
        linechart.setPrefHeight(1024);
        return linechart;
    }

    @SuppressWarnings("unchecked")
    private void customizeCharts(Stage stage) {
        for (int i = 0; i < chartList.size(); i++) {
            LineChart chart = chartList.get(i);
            chart.getData().addAll(chartsDataList.get(i));
            chart.setCreateSymbols(false);
            chart.setScaleShape(true);
            chart.setVerticalGridLinesVisible(false);
            chart.prefHeightProperty().bind(stage.heightProperty());
        }
    }

    private void getStats(Task task) {
        LocalDateTime currentLocalDateTime = LocalDateTime.now();
        boolean isLast = chartOffset + (int) (chartPageSize * chartScale) >= statsList.get(0).getMetrics().size();

        Map<String, Long> map = null;
        switch (task.getRequest()) {
            case HTTP:
                map = HttpSource.executeTask(task);
                break;

            case JDBC:
                map = JdbcSource.executeTask(task);
                break;
        }

        map = map.entrySet().stream().filter(e -> fieldsByNameMap.containsKey(e.getKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        Metric metric = new Metric(currentLocalDateTime, map);
        statsList.get(tasks.indexOf(task)).getMetrics().add(metric);

        logFieldsList.forEach(logField -> {
            if (fieldsByNameMap.get(logField.getName()).isLogAnyChange()) {
                Long val = metric.getMetrics().get(logField.getName());
                if (null == logField.getValue()) {
                    logField.setValue(val);
                } else if (!logField.getValue().equals(val) && val != null) {
                    LOGGER.warn(String.format("%s::%s: %s -> %s", task.getName(), logField.getName(), logField.getValue(), val));
                    logField.setValue(val);
                }
            }
        });

        if (isLast) {
            chartOffset = Math.max(0, statsList.get(0).getMetrics().size() - (int) (chartPageSize * chartScale));
        }

        fillCharts();
    }

    @Override
    public void stop() {
        saveStats();
    }

    private void saveStats() {
        for (int i = 0; i < statsList.size(); i++) {
            FileUtils.saveStats(tasks.get(i).getName(), statsList.get(i));
        }

        FileUtils.saveConfig(config);
        LOGGER.info("Stats and config saved.");
    }
}
