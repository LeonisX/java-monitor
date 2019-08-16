package md.leonis.monitor.config;

import java.util.ArrayList;
import java.util.List;

public class Config {

    private int requestIntervalInSeconds = 30;
    private int saveStateIntervalInSeconds = 600;

    private GuiConfig gui = new GuiConfig();
    private List<Task> tasks = new ArrayList<>();

    public int getRequestIntervalInSeconds() {
        return requestIntervalInSeconds;
    }

    public void setRequestIntervalInSeconds(int requestIntervalInSeconds) {
        this.requestIntervalInSeconds = requestIntervalInSeconds;
    }

    public int getSaveStateIntervalInSeconds() {
        return saveStateIntervalInSeconds;
    }

    public void setSaveStateIntervalInSeconds(int saveStateIntervalInSeconds) {
        this.saveStateIntervalInSeconds = saveStateIntervalInSeconds;
    }

    public GuiConfig getGui() {
        return gui;
    }

    public void setGui(GuiConfig gui) {
        this.gui = gui;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }
}
