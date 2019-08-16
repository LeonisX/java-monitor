package md.leonis.monitor.config;

import java.util.Map;

public class Authentication {

    private AuthenticationType type;
    private String userName;
    private String password;
    private Map<String, Object> params;

    public AuthenticationType getType() {
        return type;
    }

    public void setType(AuthenticationType type) {
        this.type = type;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Map<String, Object> getParams() {
        return params;
    }

    public void setParams(Map<String, Object> params) {
        this.params = params;
    }
}
