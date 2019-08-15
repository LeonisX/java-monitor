package md.leonis.monitor.model;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

public class Metric implements Serializable {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");

    private LocalDateTime localDateTime;
    private Map<String, Long> metrics;

    public Metric(LocalDateTime localDateTime, Map<String, Long> metrics) {
        this.localDateTime = localDateTime;
        this.metrics = metrics;
    }

    public LocalDateTime getLocalDateTime() {
        return localDateTime;
    }

    public void setLocalDateTime(LocalDateTime localDateTime) {
        this.localDateTime = localDateTime;
    }

    public Map<String, Long> getMetrics() {
        return metrics;
    }

    public void setMetrics(Map<String, Long> metrics) {
        this.metrics = metrics;
    }

    @Override
    public String toString() {
        return localDateTime.format(FORMATTER);
    }
}
