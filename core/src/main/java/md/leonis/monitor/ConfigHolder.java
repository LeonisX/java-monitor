package md.leonis.monitor;

import md.leonis.monitor.config.Config;

class ConfigHolder {

    private static Config config;

    private ConfigHolder() {
        // Singleton
    }

    static Config getInstance() {
        if (null == config) {
            config = FileUtils.loadConfig();
        }
        return config;
    }
}
