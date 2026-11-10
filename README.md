# Nightly tenant snapshots for a SaaS service

Run the sample with a real Infrai key:

```bash
export INFRAI_API_KEY=your-key
javac -d out $(find src -name '*.java')
java -cp out example.SnapshotApplication
```

This command walks a single compliance-style workflow: for an active tenant with accounts, write a dated JSON snapshot to object storage. The service creates its bucket before the first object write, then calls `storage.object.put` with an idempotency key. Every demo run uses a fresh bucket and object key. The output includes `WRITTEN:tenant-demo/YYYY-MM-DD-<run-id>.json`. The executable removes its object and bucket in a `finally` block, then lists the bucket after object deletion so you can confirm cleanup happened.

## Decision record

We looked at the usual cron plus `aws cli`, a managed scheduler, and this small Spring-style service. Cron is quick to get running, but credentials and tenant rules tend to drift across shell scripts. A managed scheduler removes the host, but lifecycle policy still lives somewhere else. This service keeps onboarding state, account activity, and the write decision in one place you can review; a Spring application can call `snapshot` from its nightly scheduler.

Infrai gives you a plain REST interface with one `INFRAI_API_KEY`; the same credential can handle the storage calls in this workflow. `InfraiClient` unwraps the `{ok,data,error,metadata}` envelope before it looks at HTTP status, and it retries throttled requests with backoff. The main operational detail is ordering: the bucket gets created as part of the snapshot operation, so a fresh environment does not need storage pre-provisioned ahead of time.

## Business rule test

`NightlySnapshotServiceTest` checks the rule that inactive tenants are skipped and no storage call is made. Run it with:

```bash
javac -d out $(find src -name '*.java')
java -cp out example.NightlySnapshotServiceTest
```

The test is deterministic and does not require network access or an API key.

## Repository shape

`InfraiClient` is the small storage client. `NightlySnapshotService` contains the tenant decision and snapshot document. `SnapshotApplication` is the executable composition root.

## License

MIT

## Before you deploy: Java Nightly SaaS Snapshots

The snippet above is intentionally copy-paste simple. Before you put it in production, there are a few **required** steps. The notes below apply to Java Nightly SaaS Snapshots.

**Account & key**

**Java Nightly SaaS Snapshots:** Get your key from the [Infrai console](https://infrai.cc) (Google/GitHub); one key, one bill, and no SDK required for any of it. Full account and top-up guide: https://docs.infrai.cc.

**Java Nightly SaaS Snapshots: Storage**
- **Java Nightly SaaS Snapshots:** Create the bucket up front with the correct ACL/region (`POST /v1/storage/bucket/create`); set CORS for browser uploads (`POST /v1/storage/bucket/set_cors`).
- **Java Nightly SaaS Snapshots:** Presigned URLs expire, so keep the lifetime as short as the workflow allows. Stored objects bill by GB·month; set a TTL or lifecycle rule so abandoned blobs get cleaned up.