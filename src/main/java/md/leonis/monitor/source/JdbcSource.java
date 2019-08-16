package md.leonis.monitor.source;

import md.leonis.monitor.FileUtils;
import md.leonis.monitor.config.Task;

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class JdbcSource {

    public static Map<String, Long> executeTask(Task task) {
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
