package md.leonis.monitor;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import md.leonis.monitor.config.Config;
import md.leonis.monitor.config.GuiConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class JavaFXMonitor extends Application {

    private static final Logger LOGGER = LoggerFactory.getLogger(JavaFXMonitor.class);

    private static Config config = ConfigHolder.getInstance();
    private static GuiConfig gui = config.getGui();

    private FXMLLoader loader = null;

    @Override
    public void start(Stage stage) {
        Parent root = null;
        try {
            loader = new FXMLLoader(ClassLoader.getSystemResource("Monitor.fxml"));
            root = loader.load();
        } catch (IOException e) {
            LOGGER.error("Can't load main scene!");
            System.exit(500);
        }

        loader.<MonitorController>getController().init();

        String version = JavaFXMonitor.class.getPackage().getImplementationVersion();
        boolean isDebug = (version == null);
        LOGGER.info(String.format("JavaFX Monitor %s starts...%n", isDebug ? "Dev" : version));

        stage.setTitle("Java Monitor" + (isDebug ? "" : " " + version));
        stage.setScene(new Scene(root));
        stage.setWidth(gui.getWindow().getWidth());
        stage.setHeight(gui.getWindow().getHeight());
        stage.widthProperty().addListener((obs, oldVal, newVal) -> {
            if (!stage.isMaximized()) {
                gui.getWindow().setWidth(newVal.intValue());
            }
        });
        stage.heightProperty().addListener((obs, oldVal, newVal) -> {
            if (!stage.isMaximized()) {
                gui.getWindow().setHeight(newVal.intValue());
            }
        });
        stage.setMaximized(gui.getWindow().isMaximized());
        stage.maximizedProperty().addListener((obs, oldVal, newVal) -> gui.getWindow().setMaximized(newVal));

        stage.getIcons().addAll(
                new Image(FileUtils.getResourceAsStream("icon256x256.png", isDebug, this)),
                new Image(FileUtils.getResourceAsStream("icon128x128.png", isDebug, this)),
                new Image(FileUtils.getResourceAsStream("icon96x96.png", isDebug, this)),
                new Image(FileUtils.getResourceAsStream("icon64x64.png", isDebug, this)),
                new Image(FileUtils.getResourceAsStream("icon48x48.png", isDebug, this)),
                new Image(FileUtils.getResourceAsStream("icon32x32.png", isDebug, this)),
                new Image(FileUtils.getResourceAsStream("icon24x24.png", isDebug, this)),
                new Image(FileUtils.getResourceAsStream("icon16x16.png", isDebug, this))
        );
        stage.show();
        loader.<MonitorController>getController().start();
    }


    @Override
    public void stop() {
        loader.<MonitorController>getController().saveStats();
    }
}
