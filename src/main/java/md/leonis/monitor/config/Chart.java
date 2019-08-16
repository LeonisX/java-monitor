package md.leonis.monitor.config;

public class Chart {

    private String id;
    private String name;
    private int lowerBound = 0;
    private int upperBound = 200;
    private int tickUnit = 1;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound(int lowerBound) {
        this.lowerBound = lowerBound;
    }

    public int getUpperBound() {
        return upperBound;
    }

    public void setUpperBound(int upperBound) {
        this.upperBound = upperBound;
    }

    public int getTickUnit() {
        return tickUnit;
    }

    public void setTickUnit(int tickUnit) {
        this.tickUnit = tickUnit;
    }
}
