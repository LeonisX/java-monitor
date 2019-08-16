package md.leonis.monitor;

import md.leonis.monitor.config.Config;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

// Singleton
class ConfigHolder {

    private static Log LOGGER = LogFactory.getLog(ConfigHolder.class);

    public static final String CONFIG_FILE = "config.yml";

    private static Config config;

    private ConfigHolder() {
        // ignore
    }

    static Config getInstance() {

        if (null == config) {
            try {
                InputStream ios = new FileInputStream(new File(CONFIG_FILE));
                Constructor constructor = new Constructor(Config.class);
                Yaml yaml = new Yaml(constructor);
                config = yaml.load(ios);
                if (null == config) {
                    LOGGER.warn(String.format("Can't load config: %s. It's empty. Use default.", CONFIG_FILE));
                    config = new Config();
                }
            } catch (FileNotFoundException e) {
                LOGGER.warn(String.format("Can't load config: %s. File not found. Use default.", CONFIG_FILE));
                config = new Config();
            }
        }
        return config;
    }
}
