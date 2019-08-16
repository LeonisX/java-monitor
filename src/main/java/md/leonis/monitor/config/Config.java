package md.leonis.monitor.config;

import java.util.ArrayList;
import java.util.List;

public class Config {

    private int requestIntervalInSeconds = 30;
    private UIConfig ui = new UIConfig();
    private List<Task> tasks = new ArrayList<>();

    public int getRequestIntervalInSeconds() {
        return requestIntervalInSeconds;
    }

    public void setRequestIntervalInSeconds(int requestIntervalInSeconds) {
        this.requestIntervalInSeconds = requestIntervalInSeconds;
    }

    public UIConfig getUi() {
        return ui;
    }

    public void setUi(UIConfig ui) {
        this.ui = ui;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }
}
