package md.leonis.monitor.config;

public class GuiConfig {

    private Window window = new Window();

    private Charts charts = new Charts();

    public Window getWindow() {
        return window;
    }

    public void setWindow(Window window) {
        this.window = window;
    }

    public Charts getCharts() {
        return charts;
    }

    public void setCharts(Charts charts) {
        this.charts = charts;
    }
}
