package md.leonis.monitor;

import md.leonis.monitor.config.Config;
import md.leonis.monitor.model.Stats;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.introspector.BeanAccess;
import org.yaml.snakeyaml.introspector.Property;
import org.yaml.snakeyaml.nodes.NodeTuple;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.representer.Representer;

import java.io.*;
import java.util.Map;
import java.util.stream.Collectors;

public class FileUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(FileUtils.class);

    private static final Yaml YAML = createYaml();

    private static final String CONFIG_FILE = "config.yml";
    private static final String EXT = ".dmp";

    public static Map<String, Long> secureMap(Map<String, String> safeMap) {
        return safeMap.entrySet().stream()
                .filter(e -> StringUtils.isNumeric(e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, e -> Long.parseLong(e.getValue())));
    }

    static void saveStats(String filename, Stats stats) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename + EXT))) {
            oos.writeObject(stats);
        } catch (FileNotFoundException ex) {
            // ignore
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    static Stats loadStats(String filename) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename + EXT))) {
            return (Stats) ois.readObject();
        } catch (FileNotFoundException ex) {
            // ignore
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new Stats();
    }

    static Config loadConfig() {
        try {
            InputStream ios = new FileInputStream(new File(CONFIG_FILE));
            Constructor constructor = new Constructor(Config.class);
            Yaml yaml = new Yaml(constructor);
            Config config = yaml.load(ios);
            if (null == config) {
                LOGGER.warn(String.format("Can't load config: %s. It's empty. Use default.", CONFIG_FILE));
                return new Config();
            }
            return config;
        } catch (FileNotFoundException e) {
            LOGGER.warn(String.format("Can't load config: %s. File not found. Use default.", CONFIG_FILE));
            return new Config();
        }
    }

    static void saveConfig(Config config) {
        String configString = YAML.dump(config).replace("!!" + config.getClass().getName() + "\n", "");

        try (PrintWriter out = new PrintWriter(CONFIG_FILE)) {
            out.print(configString);
        } catch (FileNotFoundException ex) {
            // ignore
            ex.printStackTrace();
        }
    }

    private static Yaml createYaml() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);

        Representer representer = new Representer() {
            @Override
            protected NodeTuple representJavaBeanProperty(Object javaBean, Property property, Object propertyValue, Tag customTag) {
                if (propertyValue == null) {
                    return null; // if value of property is null, ignore it.
                }
                else {
                    return super.representJavaBeanProperty(javaBean, property, propertyValue, customTag);
                }
            }
        };

        OrderedPropertyUtils propertyUtils = new OrderedPropertyUtils();
        propertyUtils.setBeanAccess(BeanAccess.FIELD);
        representer.setPropertyUtils(propertyUtils);

        return new Yaml(representer, options);
    }

    static InputStream getResourceAsStream(String path, boolean isDebug) {
        if (isDebug) {
            try {
                return new BufferedInputStream(new FileInputStream(FileUtils.class.getProtectionDomain().getCodeSource().getLocation().getPath() + path));
            } catch (FileNotFoundException e) {
                throw new RuntimeException(e);
            }
        } else {
            return new BufferedInputStream(FileUtils.class.getClassLoader().getResourceAsStream(path));
        }
    }
}
