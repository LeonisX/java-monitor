package md.leonis.monitor.source;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import md.leonis.monitor.FileUtils;
import md.leonis.monitor.config.Authentication;
import md.leonis.monitor.config.Task;
import org.apache.http.HttpResponse;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class HttpSource {

    private static final Logger LOGGER = LoggerFactory.getLogger(HttpSource.class);

    private static final String STATUS_CODE = "StatusCode";

    private static int retryCount = 0;

    public static Map<String, Long> executeTask(Task task) {
        LOGGER.debug("Run task: {}", task.getName());
        Authentication auth = task.getAuthentication();

        CredentialsProvider provider = null;
        UsernamePasswordCredentials credentials;
        HttpGet httpget = new HttpGet(task.getUrl());

        switch (auth.getType()) {
            case NONE:
                break;
            case BASIC:
                // Bug in Apache Commons Http 4???
                String usernameColonPassword = auth.getUserName() + ":" + auth.getPassword();
                String basicAuthPayload = "Basic " + new String(java.util.Base64.getEncoder().encode(usernameColonPassword.getBytes(StandardCharsets.UTF_8)), StandardCharsets.UTF_8);
                httpget.addHeader("Authorization", basicAuthPayload);

            case PASSWORD:
                provider = new BasicCredentialsProvider();
                credentials = new UsernamePasswordCredentials(auth.getUserName(), auth.getPassword());
                provider.setCredentials(AuthScope.ANY, credentials);
                break;

            case DIGEST:
                provider = new BasicCredentialsProvider();
                credentials = new UsernamePasswordCredentials(auth.getUserName(), auth.getPassword());
                AuthScope authScope = new AuthScope((String) auth.getParams().get("host"), (int) auth.getParams().get("port"), (String) auth.getParams().get("realm"));
                provider.setCredentials(authScope, credentials);
                break;
        }

        try {
            HttpClientBuilder clientBuilder = HttpClientBuilder.create();
            if (provider != null) {
                clientBuilder.setDefaultCredentialsProvider(provider);
            }
            HttpClient client = clientBuilder.build();

            HttpResponse response = client.execute(httpget);

            switch (task.getResponseFormat()) {
                case JSON:
                    return readJson(response);
                case STATUS_CODE_ONLY:
                    return readStatusOnly(response, task.getName());
                default:
                    throw new RuntimeException(String.format("Can't process %s data type :(", task.getResponseFormat()));
            }
        } catch (Exception e) {
            if (retryCount > 3) {
                throw new RuntimeException("retryCount > " + retryCount, e);
            } else {
                retryCount++;
            }
            return executeTask(task);
        }
    }

    private static Map<String, Long> readJson(HttpResponse response) throws IOException {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8))) {
            String responseBody = br.lines().collect(Collectors.joining());
            if (response.getStatusLine().getStatusCode() != 200) {
                throw new RuntimeException("status: " + response.getStatusLine() + "; responseBody: " + responseBody);
            }

            Map<String, Long> map = FileUtils.secureMap(new ObjectMapper().readValue(responseBody, new TypeReference<Map<String, String>>() {
            }));

            EntityUtils.consume(response.getEntity());
            retryCount = 0;
            return map;
        }
    }

    private static Map<String, Long> readStatusOnly(HttpResponse response, String name) throws IOException {
        Map<String, Long> map = new HashMap<>();
        map.put(String.format("%s_%s", name, STATUS_CODE), (long) response.getStatusLine().getStatusCode());
        EntityUtils.consume(response.getEntity());
        retryCount = 0;
        return map;
    }
}
