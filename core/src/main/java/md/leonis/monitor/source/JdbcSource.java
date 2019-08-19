package md.leonis.monitor.source;

import md.leonis.monitor.FileUtils;
import md.leonis.monitor.config.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class JdbcSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(JdbcSource.class);

    public static Map<String, Long> executeTask(Task task) {
        LOGGER.debug("Run task: {}", task.getName());

        Map<String, String> safeMap = new HashMap<>();
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            Connection connection = DriverManager.getConnection(task.getUrl(), task.getAuthentication().getUserName(), task.getAuthentication().getPassword());
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("SHOW GLOBAL STATUS LIKE '%%'");

            while (resultSet.next()) {
                safeMap.put(resultSet.getString(1), resultSet.getString(2));
            }
            resultSet.close();
            statement.close();
            connection.close();
        } catch (SQLException | ClassNotFoundException e) {
            e.printStackTrace();
        }
        return FileUtils.secureMap(safeMap);
    }
}
