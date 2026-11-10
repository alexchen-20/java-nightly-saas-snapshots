package example;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

public final class InfraiClient {
    private final HttpClient http = HttpClient.newHttpClient();
    private final String key;
    private final String base;

    public InfraiClient(String key, String base) { this.key = key; this.base = base; }

    public String createBucket(String name) throws IOException, InterruptedException {
        return call("POST", "/v1/storage/bucket/create", "{\"name\":\"" + esc(name) + "\"}");
    }

    public String putObject(String bucket, String objectKey, byte[] data, String idempotencyKey)
            throws IOException, InterruptedException {
        // Canonical capability: storage.object.put
        String body = "{\"data_base64\":\"" + Base64.getEncoder().encodeToString(data)
                + "\",\"content_type\":\"application/json\",\"idempotency_key\":\""
                + esc(idempotencyKey) + "\"}";
        return call("PUT", "/v1/storage/object/put/" + segment(bucket) + "/" + segment(objectKey), body);
    }

    public String listObjects(String bucket) throws IOException, InterruptedException {
        return call("GET", "/v1/storage/object/list/" + segment(bucket), "");
    }

    public void deleteObject(String bucket, String objectKey) throws IOException, InterruptedException {
        call("DELETE", "/v1/storage/object/delete/" + segment(bucket) + "/" + segment(objectKey), "");
    }

    public void deleteBucket(String bucket) throws IOException, InterruptedException {
        call("DELETE", "/v1/storage/bucket/delete/" + segment(bucket), "");
    }

    private String call(String method, String path, String body) throws IOException, InterruptedException {
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest req = HttpRequest.newBuilder(URI.create(base + path)).timeout(Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + key).header("Content-Type", "application/json")
                    .method(method, HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
            String env = res.body();
            if (res.statusCode() == 429 && attempt < 3) {
                Thread.sleep((long) Math.pow(2, attempt) * 500L); continue;
            }
            if (!env.contains("\"ok\":true")) throw new IOException("Infrai error: " + env);
            if (res.statusCode() >= 500) throw new IOException("Infrai transport status " + res.statusCode());
            return env;
        }
        throw new IOException("retry budget exhausted");
    }

    private static String segment(String s) { return s.replace("/", "%2F"); }
    private static String esc(String s) { return s.replace("\\", "\\\\").replace("\"", "\\\""); }
}
