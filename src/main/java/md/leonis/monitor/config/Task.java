package md.leonis.monitor.config;

import java.util.List;

public class Task {

    private String name;
    private TaskType request;
    private Integer timeOffsetInSeconds = 0;
    private String url;
    private Authentication authentication;
    private ResponseFormat responseFormat;
    private List<Field> fields;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getTimeOffsetInSeconds() {
        return timeOffsetInSeconds;
    }

    public void setTimeOffsetInSeconds(Integer timeOffsetInSeconds) {
        this.timeOffsetInSeconds = timeOffsetInSeconds;
    }

    public TaskType getRequest() {
        return request;
    }

    public void setRequest(TaskType request) {
        this.request = request;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public void setAuthentication(Authentication authentication) {
        this.authentication = authentication;
    }

    public ResponseFormat getResponseFormat() {
        return responseFormat;
    }

    public void setResponseFormat(ResponseFormat responseFormat) {
        this.responseFormat = responseFormat;
    }

    public List<Field> getFields() {
        return fields;
    }

    public void setFields(List<Field> fields) {
        this.fields = fields;
    }
}
