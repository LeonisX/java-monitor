package md.leonis.monitor;

import org.apache.http.StatusLine;

import java.text.MessageFormat;

public class HttpException extends RuntimeException {

    public HttpException(StatusLine statusLine, String responseBody) {
        super(formatMessage(statusLine, responseBody));
    }

    private static String formatMessage(StatusLine statusLine, String responseBody) {
        String status = (statusLine == null) ? "null" : statusLine.toString();
        String body = (responseBody == null) ? "null" : responseBody;
        body = (body.length() > 120) ? body.substring(0, 120) + "..." : body;
        return MessageFormat.format("Status: {0}; Response Body: {1}", status, body);
    }
}
