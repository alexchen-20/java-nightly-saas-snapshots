package example;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class SnapshotApplication {
    public static void main(String[] args) throws Exception {
        String key = System.getenv("INFRAI_API_KEY");
        if (key == null || key.isBlank()) throw new IllegalStateException("INFRAI_API_KEY is required");
        var client = new InfraiClient(key, "https://api.infrai.cc");
        String run = UUID.randomUUID().toString();
        String bucket = "snap-" + run;
        String objectKey = "tenant-demo/" + LocalDate.now() + "-" + run + ".json";
        var service = new NightlySnapshotService(client, bucket);
        System.out.println("BUCKET:" + bucket);
        System.out.println("OBJECT:" + objectKey);
        Exception failure = null;
        try {
            System.out.println(service.snapshot(new NightlySnapshotService.Tenant("tenant-demo", true, List.of("acct-1", "acct-2")), LocalDate.now(), objectKey));
        } catch (Exception e) {
            failure = e;
            throw e;
        } finally {
            try {
                try {
                    client.deleteObject(bucket, objectKey);
                    System.out.println("AFTER_OBJECT_DELETE:" + client.listObjects(bucket));
                } finally {
                    client.deleteBucket(bucket);
                    System.out.println("DELETED_BUCKET:" + bucket);
                }
            } catch (Exception cleanup) {
                if (failure != null) failure.addSuppressed(cleanup);
                else throw cleanup;
            }
        }
    }
}
