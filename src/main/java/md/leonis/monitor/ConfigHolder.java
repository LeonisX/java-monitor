package md.leonis.monitor;

import md.leonis.monitor.config.Config;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

// Singleton
class ConfigHolder {

    private static Config config;

    private ConfigHolder() {
        // ignore
    }

    static Config getInstance() {

        if (null == config) {
            try {
                InputStream ios = new FileInputStream(new File("config.yml"));
                Constructor constructor = new Constructor(Config.class);
                Yaml yaml = new Yaml(constructor);
                config = yaml.load(ios);
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        }
        return config;
    }
}
