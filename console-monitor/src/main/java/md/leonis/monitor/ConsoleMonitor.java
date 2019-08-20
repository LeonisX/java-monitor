package md.leonis.monitor;

import md.leonis.monitor.config.Config;
import md.leonis.monitor.config.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

class ConsoleMonitor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsoleMonitor.class);

    private final MonitorEngine engine;

    public static void main(String[] args) {
        new ConsoleMonitor();
    }

    private ConsoleMonitor() {
        String version = ConsoleMonitor.class.getPackage().getImplementationVersion();
        boolean isDebug = (version == null);
        LOGGER.info(String.format("Java Console Monitor %s starts...%n", isDebug ? "Dev" : version));

        engine = new MonitorEngine();
        Runtime.getRuntime().addShutdownHook(new Thread(engine::saveStats));

        start();

        while(true) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    private void start() {
        Config config = ConfigHolder.getInstance();
        ScheduledExecutorService pool = Executors.newScheduledThreadPool(2);
        pool.scheduleAtFixedRate(() -> engine.setCurrentLocalDateTime(LocalDateTime.now()), 0, config.getRequestIntervalInSeconds(), TimeUnit.SECONDS); // set time for other tasks
        pool.scheduleAtFixedRate(() -> engine.setCurrentLocalDateTime(LocalDateTime.now()), 0, config.getSaveStateIntervalInSeconds(), TimeUnit.SECONDS); // save metrics and config

        for (Task task : config.getTasks()) {
            pool.scheduleAtFixedRate(() -> engine.getStats(task), task.getTimeOffsetInSeconds(), config.getRequestIntervalInSeconds(), TimeUnit.SECONDS); // regular tasks
        }
    }
}
