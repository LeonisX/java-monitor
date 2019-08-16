package md.leonis.monitor.config;

import java.util.ArrayList;
import java.util.List;

public class GuiConfig {

    private double horizontalScale = 1;
    private int pageSize = 200;
    private List<Chart> charts = new ArrayList<>();

    public double getHorizontalScale() {
        return horizontalScale;
    }

    public void setHorizontalScale(double horizontalScale) {
        this.horizontalScale = horizontalScale;
    }

    public int getPageSize() {
        return pageSize;
    }

    public void setPageSize(int pageSize) {
        this.pageSize = pageSize;
    }

    public List<Chart> getCharts() {
        return charts;
    }

    public void setCharts(List<Chart> charts) {
        this.charts = charts;
    }
}
