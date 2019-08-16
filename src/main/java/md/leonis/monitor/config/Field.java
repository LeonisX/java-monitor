package md.leonis.monitor.config;

public class Field {

    private String name;
    private String chartId;
    private boolean logAnyChange = false;

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
}
