package md.leonis.monitor;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.ParallelTransition;
import javafx.animation.Timeline;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import md.leonis.monitor.config.*;
import md.leonis.monitor.model.MetricsWithDate;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class MonitorController {

    private static Config config = ConfigHolder.getInstance();
    private static GuiConfig gui = config.getGui();
    private static List<Chart> charts = gui.getCharts().getItems();
    private static List<Task> tasks = config.getTasks();

    private MonitorEngine engine;

    private List<LineChart> chartList = new ArrayList<>();
    private List<List<XYChart.Series<String, Long>>> chartsDataList;

    private double chartScale = gui.getCharts().getHorizontalScale();
    private int chartPageSize = gui.getCharts().getPageSize();
    private int chartOffset = 0;

    @FXML public BorderPane pane;

    @FXML private HBox hBox;
    @FXML private Label offsetLabel;
    @FXML private Label pageLabel;
    @FXML private TextField upperBoundTextField;
    @FXML private TextField tickUnitTextField;

    @FXML public Button fastBackward;
    @FXML public Button backward;
    @FXML public Button fastForward;
    @FXML public Button forward;
    @FXML public Button plus;
    @FXML public Button minus;

    private TabPane tabPane = new TabPane();

    private boolean canDisplay;

    void init() {
        engine = new MonitorEngine();

        canDisplay = !tasks.isEmpty() && !charts.isEmpty();

        if (!canDisplay) {
            pane.setCenter(new Label(MonitorEngine.NO_TASKS));
            hBox.setVisible(false);
            return;
        }

        // List of LineChart
        for (Chart chart : charts) {
            chartList.add(createLineChart(chart.getLowerBound(), chart.getUpperBound(), chart.getTickUnit()));
        }

        // List of lists of Series data
        chartsDataList = chartList.stream().map(c -> new ArrayList<XYChart.Series<String, Long>>()).collect(Collectors.toList());
        tasks.forEach(task ->
                task.getMetrics().forEach(metric -> {
                    if (metric.getChartId() != null) {
                        Integer chartId = engine.getChartsByIdMap().get(metric.getChartId());
                        if (chartId != null) {
                            chartsDataList.get(chartId).add(new XYChart.Series<>(metric.getName(), FXCollections.observableArrayList()));
                        }
                    }
                })
        );

        // Create tabs
        for (int i = 0; i < chartList.size(); i++) {
            Tab tab = newTab(charts.get(i).getName(), chartList.get(i));
            tab.setId(String.valueOf(i));
            tabPane.getTabs().add(tab);
        }

        tabPane.getSelectionModel().selectedItemProperty().addListener(
                (ov, prevTab, newTab) -> showTabStats(Integer.parseInt(newTab.getId()))
        );

        showTabStats(0);

        pane.setCenter(tabPane);

        chartOffset = Math.max(0, engine.getStatsList().get(0).getMetrics().size() - (int) (chartPageSize * chartScale));

        fillCharts();

        customizeCharts(pane);

        UnaryOperator<TextFormatter.Change> integerFilter = change -> {
            String input = change.getText();
            if (input.matches("[0-9]*")) {
                return change;
            }
            return null;
        };

        upperBoundTextField.setTextFormatter(new TextFormatter<String>(integerFilter));
        tickUnitTextField.setTextFormatter(new TextFormatter<String>(integerFilter));

        pane.getStylesheets().add("modena.css");
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

    private void showTabStats(int tabId) {
        tickUnitTextField.setText(String.valueOf(charts.get(tabId).getTickUnit()));
        upperBoundTextField.setText(String.valueOf(charts.get(tabId).getUpperBound()));
    }

    private void fillCharts() {
        int toIndex = Math.min(chartOffset + (int) (chartPageSize * chartScale), engine.getStatsList().get(0).getMetrics().size());
        List<List<MetricsWithDate>> subLists = engine.getStatsList().stream().map(stats -> {
            if (toIndex > stats.getMetrics().size()) {
                return new ArrayList<MetricsWithDate>();
            } else {
                return stats.getMetrics().subList(chartOffset, toIndex);
            }
        }).collect(Collectors.toList());

        for (List<XYChart.Series<String, Long>> series : chartsDataList) {
            for (XYChart.Series<String, Long> d : series) {
                String name = d.getName();

                List<XYChart.Data<String, Long>> dataList = subLists.get(engine.getTaskIdByMetricNameMap().get(name)).stream().map(s -> {
                    Metric metric = engine.getChartMetricsByNameMap().get(name);
                    Long value = s.getMetrics().get(name);
                    if (null == value) {
                        return new XYChart.Data<>(s.toString(), -1L);
                    } else {
                        if (metric.getIncrement() != null) {
                            value += metric.getIncrement();
                        }
                        if (metric.getMultiplier() != null) {
                            value = (long) (value * metric.getMultiplier());
                        }
                        return new XYChart.Data<>(s.toString(), value);
                    }
                }).collect(Collectors.toList());

                d.getData().clear();
                d.getData().addAll(dataList);
            }
        }

        offsetLabel.setText(String.format("[%s - %s] of %s", chartOffset, toIndex, engine.getStatsList().get(0).getMetrics().size()));
        pageLabel.setText(String.format("Page size: %s", (int) (chartPageSize * chartScale)));
    }

    private Tab newTab(String name, LineChart chart) {
        Tab tab = new Tab();
        tab.setText(name);
        VBox root = new VBox(chart);
        tab.setContent(root);
        return tab;
    }

    @SuppressWarnings("unchecked")
    private void customizeCharts(Pane pane) {
        for (int i = 0; i < chartList.size(); i++) {
            LineChart chart = chartList.get(i);
            chart.getData().addAll(chartsDataList.get(i));
            chart.setCreateSymbols(false);
            chart.setScaleShape(true);
            chart.setVerticalGridLinesVisible(false);
            chart.prefHeightProperty().bind(pane.heightProperty());
        }
    }

    void start() {
        tabPane.requestFocus();
        Timeline etalonTimeLine = new Timeline(new KeyFrame(Duration.seconds(config.getRequestIntervalInSeconds()), ae -> engine.setCurrentLocalDateTime(LocalDateTime.now())));
        etalonTimeLine.setCycleCount(Animation.INDEFINITE);
        etalonTimeLine.play();

        List<Timeline> timelineList = tasks.stream().map(task -> {
            Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(config.getRequestIntervalInSeconds()), ae -> getStats(task)));
            timeline.setCycleCount(Animation.INDEFINITE);
            timeline.setDelay(Duration.seconds(task.getTimeOffsetInSeconds()));
            return timeline;
        }).collect(Collectors.toList());

        ParallelTransition pt = new ParallelTransition();
        pt.getChildren().addAll(timelineList);
        pt.play();

        Timeline saveTimeline = new Timeline(new KeyFrame(Duration.minutes(config.getSaveStateIntervalInSeconds()), ae -> engine.saveStats()));
        saveTimeline.setCycleCount(Animation.INDEFINITE);
        saveTimeline.play();
    }

    private void getStats(Task task) {
        if (canDisplay) {
            boolean isLast = chartOffset + (int) (chartPageSize * chartScale) >= engine.getStatsList().get(0).getMetrics().size();

            engine.getStats(task);

            if (isLast) {
                chartOffset = Math.max(0, engine.getStatsList().get(0).getMetrics().size() - (int) (chartPageSize * chartScale));
            }

            fillCharts();
        }
    }

    void saveStats() {
        engine.saveStats();
    }

    public void tickUnitTextFieldKeyReleased() {
        if (StringUtils.isNotBlank(tickUnitTextField.getText())) {
            int newValue = Integer.parseInt(tickUnitTextField.getText());
            int chartId = Integer.parseInt(tabPane.getSelectionModel().getSelectedItem().getId());
            ((NumberAxis) chartList.get(chartId).getYAxis()).setTickUnit(newValue);
            charts.get(chartId).setTickUnit(newValue);
        }
    }

    public void upperBoundTextFieldKeyReleased() {
        if (StringUtils.isNotBlank(upperBoundTextField.getText())) {
            int newValue = Integer.parseInt(upperBoundTextField.getText());
            int chartId = Integer.parseInt(tabPane.getSelectionModel().getSelectedItem().getId());
            ((NumberAxis) chartList.get(chartId).getYAxis()).setUpperBound(newValue);
            charts.get(chartId).setUpperBound(newValue);
        }
    }

    public void fastBackwardOnAction() {
        chartOffset = 0;
        fillCharts();
    }

    public void backwardOnAction() {
        chartOffset -= (int) (chartPageSize * chartScale);
        chartOffset = Math.max(chartOffset, 0);
        fillCharts();
    }

    public void fastForwardOnAction() {
        chartOffset = engine.getStatsList().get(0).getMetrics().size() - (int) (chartPageSize * chartScale);
        chartOffset = Math.max(chartOffset, 0);
        fillCharts();
    }

    public void forwardOnAction() {
        chartOffset = Math.min(chartOffset + (int) (chartPageSize * chartScale), engine.getStatsList().get(0).getMetrics().size() - (int) (chartPageSize * chartScale));
        chartOffset = Math.max(chartOffset, 0);
        fillCharts();
    }

    public void plusOnAction() {
        chartScale *= 2;
        gui.getCharts().setHorizontalScale(chartScale);
        chartOffset = Math.min(chartOffset + (int) (chartPageSize * chartScale), engine.getStatsList().get(0).getMetrics().size()) - (int) (chartPageSize * chartScale);
        chartOffset = Math.max(chartOffset, 0);
        fillCharts();
    }

    public void minusOnAction() {
        chartScale /= 2;
        gui.getCharts().setHorizontalScale(chartScale);
        fillCharts();
    }
}
