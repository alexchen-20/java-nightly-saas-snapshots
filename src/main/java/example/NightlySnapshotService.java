package example;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class NightlySnapshotService {
    public record Tenant(String id, boolean active, List<String> accounts) {}
    private final InfraiClient storage;
    private final String bucket;

    public NightlySnapshotService(InfraiClient storage, String bucket) { this.storage = storage; this.bucket = bucket; }

    public String snapshot(Tenant tenant, LocalDate date) throws Exception {
        return snapshot(tenant, date, tenant.id() + "/" + date + "-" + UUID.randomUUID() + ".json");
    }

    public String snapshot(Tenant tenant, LocalDate date, String key) throws Exception {
        if (!tenant.active() || tenant.accounts().isEmpty()) return "SKIPPED";
        storage.createBucket(bucket);
        String json = "{\"tenant\":\"" + tenant.id() + "\",\"date\":\"" + date
                + "\",\"accounts\":" + tenant.accounts().size() + "}";
        storage.putObject(bucket, key, json.getBytes(StandardCharsets.UTF_8), "snapshot-" + key);
        return "WRITTEN:" + key;
    }
}
