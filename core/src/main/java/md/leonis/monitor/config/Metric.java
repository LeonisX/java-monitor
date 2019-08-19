package md.leonis.monitor.config;

public class Metric {

    private String name;
    private String chartId;
    private boolean logAnyChange = false;
    private Integer increment;
    private Double multiplier;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getChartId() {
        return chartId;
    }

    public void setChartId(String chartId) {
        this.chartId = chartId;
    }

    public boolean isLogAnyChange() {
        return logAnyChange;
    }

    public void setLogAnyChange(boolean logAnyChange) {
        this.logAnyChange = logAnyChange;
    }

    public Integer getIncrement() {
        return increment;
    }

    public void setIncrement(Integer increment) {
        this.increment = increment;
    }

    public Double getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(Double multiplier) {
        this.multiplier = multiplier;
    }
}
