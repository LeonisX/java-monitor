package md.leonis.monitor;

class LoggerUtils {

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
