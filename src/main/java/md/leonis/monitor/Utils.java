package md.leonis.monitor;

import md.leonis.monitor.model.Stats;
import org.apache.commons.lang3.StringUtils;

import java.io.*;
import java.util.Map;
import java.util.stream.Collectors;

public class Utils {

    private static final String EXT = ".dmp";

    public static Map<String, Long> secureMap(Map<String, String> safeMap) {
        return safeMap.entrySet().stream()
                .filter(e -> StringUtils.isNumeric(e.getValue()))
                .collect(Collectors.toMap(Map.Entry::getKey, e -> Long.parseLong(e.getValue())));
    }

    static void save(String filename, Stats stats) {
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(filename + EXT))) {
            oos.writeObject(stats);
        } catch (FileNotFoundException ex) {
            // ignore
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    static Stats load(String filename) {
        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(filename + EXT))) {
            return (Stats) ois.readObject();
        } catch (FileNotFoundException ex) {
            // ignore
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return new Stats();
    }

    static void disableApacheHttpLogs() {
        System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.SimpleLog");
        System.setProperty("org.apache.commons.logging.simplelog.showdatetime", "true");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.wire", "warn");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http", "warn");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.impl.conn", "warn");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.impl.client", "warn");
        System.setProperty("org.apache.commons.logging.simplelog.log.org.apache.http.client", "warn");
    }
}
