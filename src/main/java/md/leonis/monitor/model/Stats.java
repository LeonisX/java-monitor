package md.leonis.monitor.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Stats implements Serializable {

    private List<MetricsWithDate> metrics = new ArrayList<>();

    public List<MetricsWithDate> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<MetricsWithDate> metrics) {
        this.metrics = metrics;
    }
}
