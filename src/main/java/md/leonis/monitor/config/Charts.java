package md.leonis.monitor.config;

import java.util.ArrayList;
import java.util.List;

public class Charts {

    private double horizontalScale = 1;
    private int pageSize = 200;
    private List<Chart> items = new ArrayList<>();

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

    public List<Chart> getItems() {
        return items;
    }

    public void setItems(List<Chart> items) {
        this.items = items;
    }
}
