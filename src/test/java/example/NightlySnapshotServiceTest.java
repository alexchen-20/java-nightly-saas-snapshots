package example;

import java.time.LocalDate;
import java.util.List;

public final class NightlySnapshotServiceTest {
    public static void main(String[] args) throws Exception {
        var service = new NightlySnapshotService(new InfraiClient("test", "http://127.0.0.1"), "bucket");
        String result = service.snapshot(new NightlySnapshotService.Tenant("inactive", false, List.of("a")), LocalDate.of(2026, 1, 1));
        if (!result.equals("SKIPPED")) throw new AssertionError(result);
        System.out.println("NightlySnapshotServiceTest passed");
    }
}
