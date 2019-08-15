package md.leonis.monitor;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import md.leonis.monitor.config.Config;
import md.leonis.monitor.config.Field;
import md.leonis.monitor.config.Task;
import md.leonis.monitor.model.LogField;
import md.leonis.monitor.model.Metric;
import md.leonis.monitor.model.Stats;
import md.leonis.monitor.source.HttpSource;
import md.leonis.monitor.source.JdbcSource;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class Monitor extends Application {

    private static Config config = ConfigHolder.getInstance();

    private List<Stats> statsList;
    private List<LineChart> chartList;

    private Map<String, Field> fieldsByNameMap = new HashMap<>();
    private Map<String, Field> chartFieldsByNameMap = new HashMap<>();

    private List<List<XYChart.Series<String, Long>>> chartsDataList;

    private List<LogField> logFieldsList = new ArrayList<>();

    private double chartScale = config.getUi().getHorizontalScale();
    private int chartPageSize = config.getUi().getPageSize();
    private int chartOffset;

    private Label offsetLabel = new Label();
    private Label pageLabel = new Label();

    @Override
    public void start(Stage stage) {
        Utils.disableApacheHttpLogs();

        statsList = config.getTasks().stream().map(task -> Utils.load(task.getName())).collect(Collectors.toList());

        // List of LineChart
        chartList = config.getUi().getCharts().stream().map(chart ->
                createLineChart(chart.getLowerBound(), chart.getUpperBound(), chart.getTickCount())
        ).collect(Collectors.toList());

        chartOffset = Math.max(0, statsList.get(0).getMetrics().size() - (int) (chartPageSize * chartScale));

        // List of lists of Series data
        chartsDataList = chartList.stream().map(c -> new ArrayList<XYChart.Series<String, Long>>()).collect(Collectors.toList());
        config.getTasks().forEach(task ->
                task.getFields().forEach(field -> {
                    if (field.getChartId() != null) {
                        chartsDataList.get(field.getChartId()).add(new XYChart.Series<>(field.getName(), FXCollections.observableArrayList()));
                    }
                    if (field.isLogAnyChange()) {
                        logFieldsList.add(new LogField(field.getName(), null));
                    }
                })
        );

        fieldsByNameMap = config.getTasks().stream().flatMap(t -> t.getFields().stream()).collect(Collectors.toMap(Field::getName, f -> f));

        chartFieldsByNameMap = fieldsByNameMap.entrySet().stream().filter(e -> e.getValue().getChartId() != null).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        fillCharts();

        customizeCharts(stage);

        TabPane tabPane = new TabPane();
        for (int i = 0; i < chartList.size(); i++) {
            tabPane.getTabs().add(newTab(config.getUi().getCharts().get(i).getName(), chartList.get(i)));
        }

        BorderPane pane = new BorderPane();
        pane.setCenter(tabPane);

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
        Button plus = new Button(" + ");
        plus.setOnAction(e -> {
            chartScale /= 2;
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
        Button minus = new Button(" - ");
        minus.setOnAction(e -> {
            chartScale *= 2;
            chartOffset = Math.min(chartOffset + (int) (chartPageSize * chartScale), statsList.get(0).getMetrics().size()) - (int) (chartPageSize * chartScale);
            chartOffset = Math.max(chartOffset, 0);
            fillCharts();
        });

        HBox hBox = new HBox(fastBackward, backward, forward, fastForward, offsetLabel, plus, minus, pageLabel);
        hBox.setAlignment(Pos.CENTER);
        hBox.setSpacing(5);
        hBox.setPadding(new Insets(0,0,5,0));

        pane.setBottom(hBox);
        //pane.getStylesheets().add("modena.css");
        stage.setTitle("Java Monitor");
        stage.setScene(new Scene(pane, 1280, 1024));
        stage.show();

        Timeline requestTimeline = new Timeline(new KeyFrame(Duration.seconds(config.getRequestIntervalInSeconds()), ae -> getStats()));
        requestTimeline.setCycleCount(Animation.INDEFINITE);
        requestTimeline.play();

        Timeline saveTimeline = new Timeline(new KeyFrame(Duration.minutes(10), ae -> saveStats()));
        saveTimeline.setCycleCount(Animation.INDEFINITE);
        saveTimeline.play();
    }

    private void fillCharts() {
        int toIndex = Math.min(chartOffset + (int) (chartPageSize * chartScale), statsList.get(0).getMetrics().size());
        List<List<Metric>> subLists = statsList.stream().map(stats -> stats.getMetrics().subList(chartOffset, toIndex)).collect(Collectors.toList());

        for (List<XYChart.Series<String, Long>> series : chartsDataList) {
            for (XYChart.Series<String, Long> d : series) {
                String name = d.getName();
                d.getData().clear();
                d.getData().addAll(subLists.get(chartFieldsByNameMap.get(name).getChartId()).stream().map(s -> new XYChart.Data<>(s.toString(), s.getMetrics().get(name))).collect(Collectors.toList()));

            }
        }

        offsetLabel.setText(String.format("[%s - %s] of %s", chartOffset, toIndex, statsList.get(0).getMetrics().size()));
        pageLabel.setText(String.format("Page size: %s", (int) (chartPageSize * chartScale)));
    }

    private Tab newTab(String name, LineChart chart) {
        Tab tab = new Tab();
        tab.setText(name);
        VBox root = new VBox(chart);
        tab.setContent(root);
        return tab;
    }

    @SuppressWarnings("all")
    private LineChart createLineChart(int lowerBound, int upperBound, int tickCount) {
        CategoryAxis xAxis = new CategoryAxis();
        //xAxis.setLabel("Time");

        NumberAxis yAxis = new NumberAxis(lowerBound, upperBound, tickCount);
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

    private void getStats() {
        LocalDateTime currentLocalDateTime = LocalDateTime.now();
        boolean isLast = chartOffset + (int) (chartPageSize * chartScale) >= statsList.get(0).getMetrics().size();

        for (int i = 0; i < config.getTasks().size(); i++) {
            Task task = config.getTasks().get(i);

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
            statsList.get(i).getMetrics().add(metric);

            logFieldsList.forEach(logField -> {
                if (fieldsByNameMap.get(logField.getName()).isLogAnyChange()) {
                    Long val = metric.getMetrics().get(logField.getName());
                    if (null == logField.getValue()) {
                        logField.setValue(val);
                    } else if (!logField.getValue().equals(val) && val != null) {
                        System.out.println(task.getName() + ". " + logField.getName() + ": " + val);
                        logField.setValue(val);
                    }
                }
            });
        }

        if (isLast) {
            chartOffset = Math.max(0, statsList.get(0).getMetrics().size() - (int) (chartPageSize * chartScale));
        }

        fillCharts();
    }

    @Override
    public void stop(){
        saveStats();
    }

    private void saveStats() {
        for (int i = 0; i < statsList.size(); i++) {
            Utils.save(config.getTasks().get(i).getName(), statsList.get(i));
        }
        System.out.println("Stats saved.");
    }
}
