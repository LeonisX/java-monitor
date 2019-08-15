package md.leonis.monitor.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class Stats implements Serializable {

    private List<Metric> metrics = new ArrayList<>();

    public List<Metric> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<Metric> metrics) {
        this.metrics = metrics;
    }
}
