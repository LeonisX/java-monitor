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
import javafx.scene.image.Image;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import md.leonis.monitor.config.*;
import md.leonis.monitor.model.MetricsWithDate;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

public class JavaFXMonitor extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaFXMonitor.class);

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

    private TextField upperBoundTextField = new TextField();
    private TextField tickUnitTextField = new TextField();

    private Label offsetLabel = new Label();
    private Label pageLabel = new Label();

    private boolean canDisplay;

    @Override
    public void init() {
        engine = new MonitorEngine();

        canDisplay = !tasks.isEmpty() && !charts.isEmpty();

        // List of LineChart
        for (Chart chart : charts) {
            chartList.add(createLineChart(chart.getLowerBound(), chart.getUpperBound(), chart.getTickUnit()));
        }

        if (canDisplay) {
            chartOffset = Math.max(0, engine.getStatsList().get(0).getMetrics().size() - (int) (chartPageSize * chartScale));
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
    }

    @Override
    public void start(Stage stage) {
        String version = JavaFXMonitor.class.getPackage().getImplementationVersion();
        boolean isDebug = (version == null);
        LOGGER.info(String.format("JavaFX Monitor %s starts...%n", isDebug ? "Dev" : version));

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
            pane.setCenter(new Label(MonitorEngine.NO_TASKS));
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
            chartOffset = engine.getStatsList().get(0).getMetrics().size() - (int) (chartPageSize * chartScale);
            chartOffset = Math.max(chartOffset, 0);
            fillCharts();
        });
        Button forward = new Button(" >> ");
        forward.setOnAction(e -> {
            chartOffset = Math.min(chartOffset + (int) (chartPageSize * chartScale), engine.getStatsList().get(0).getMetrics().size() - (int) (chartPageSize * chartScale));
            chartOffset = Math.max(chartOffset, 0);
            fillCharts();
        });
        Button plus = new Button(" + ");
        plus.setOnAction(e -> {
            chartScale *= 2;
            gui.getCharts().setHorizontalScale(chartScale);
            chartOffset = Math.min(chartOffset + (int) (chartPageSize * chartScale), engine.getStatsList().get(0).getMetrics().size()) - (int) (chartPageSize * chartScale);
            chartOffset = Math.max(chartOffset, 0);
            fillCharts();
        });
        Button minus = new Button(" - ");
        minus.setOnAction(e -> {
            chartScale /= 2;
            gui.getCharts().setHorizontalScale(chartScale);
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

        Label separator = new Label();
        separator.setPadding(new Insets(0, 0, 0, 30));

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

        HBox hBox = new HBox(tickUnitLabel, tickUnitTextField, upperBoundLabel, upperBoundTextField, separator,
                fastBackward, backward, forward, fastForward, offsetLabel, plus, minus, pageLabel);

        hBox.getChildren().forEach(c -> c.setDisable(!canDisplay));

        hBox.setAlignment(Pos.CENTER);
        hBox.setSpacing(5);
        hBox.setPadding(new Insets(0, 0, 5, 0));

        pane.setBottom(hBox);
        pane.getStylesheets().add("modena.css");

        stage.setTitle("Java Monitor" + (isDebug ? "" : " " + version));
        stage.setScene(new Scene(pane, gui.getWindow().getWidth(), gui.getWindow().getHeight()));
        stage.setWidth(gui.getWindow().getWidth());
        stage.setHeight(gui.getWindow().getHeight());
        stage.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (!stage.isMaximized()) {
                gui.getWindow().setWidth(newVal.intValue());
            }
        });
        stage.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (!stage.isMaximized()) {
                gui.getWindow().setHeight(newVal.intValue());
            }
        });
        stage.setMaximized(gui.getWindow().isMaximized());
        stage.maximizedProperty().addListener((obs, oldVal, newVal) -> gui.getWindow().setMaximized(newVal));

        stage.getIcons().addAll(
                new Image(FileUtils.getResourceAsStream("icon256x256.png", isDebug, this)),
                new Image(FileUtils.getResourceAsStream("icon128x128.png", isDebug, this)),
                new Image(FileUtils.getResourceAsStream("icon96x96.png", isDebug, this)),
                new Image(FileUtils.getResourceAsStream("icon64x64.png", isDebug, this)),
                new Image(FileUtils.getResourceAsStream("icon48x48.png", isDebug, this)),
                new Image(FileUtils.getResourceAsStream("icon32x32.png", isDebug, this)),
                new Image(FileUtils.getResourceAsStream("icon24x24.png", isDebug, this)),
                new Image(FileUtils.getResourceAsStream("icon16x16.png", isDebug, this))
        );
        stage.show();
        start();
    }

    private void showTabStats(int tabId) {
        if (canDisplay) {
            tickUnitTextField.setText(String.valueOf(charts.get(tabId).getTickUnit()));
            upperBoundTextField.setText(String.valueOf(charts.get(tabId).getUpperBound()));
        }
    }

    private void fillCharts() {
        if (canDisplay) {
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

    private void start() {
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
        boolean isLast = chartOffset + (int) (chartPageSize * chartScale) >= engine.getStatsList().get(0).getMetrics().size();

        engine.getStats(task);

        if (isLast) {
            chartOffset = Math.max(0, engine.getStatsList().get(0).getMetrics().size() - (int) (chartPageSize * chartScale));
        }

        fillCharts();
    }

    @Override
    public void stop() {
        engine.saveStats();
    }
}
