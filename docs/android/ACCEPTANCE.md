# Native iPhone acceptance contract — currently all unfulfilled

A successful build, green unit tests, populated diagnostics, LAN transfer, peer discovery, an AirDrop tile, or matching files alone is **not success**.

| Required case | Native transfer evidence | Byte/SHA256 equality | Wi-Fi preservation |
|---|---|---|---|
| Actual S22 → iPhone | Not tested | Not tested | Not tested |
| iPhone → actual S22 | Not tested | Not tested | Not tested |
| Actual S24 → iPhone | Not tested | Not tested | Not tested |
| iPhone → actual S24 | Not tested | Not tested | Not tested |

## Permission and hardware prerequisites

* Connect the actual phones for approved read-only identification, not guessed regional chip variants. Record model suffix, nonunique SoC metadata, Android release/API/security patch, kernel, Wi-Fi driver/firmware evidence and access method. Do not publish serials, SSIDs, MACs, IMEIs, device fingerprints, credentials or private photos.
* Explicit permission is required before diagnostic-app installation or any subsequent radio/settings change. Present a narrowly scoped test and rollback/safety boundary then. No root, unlock, flash, SELinux change, router change or helper run is currently authorized.
* First establish a permitted OWL-compatible radio path; installing this diagnostic app does not establish one. A real adapter and the AirDrop transfer protocol must be implemented before these tests are possible.
* Record iPhone model and iOS version only once actually observed. The user should select/approve native AirDrop transfers and any receiving-mode changes. Do not assume Everyone/Contacts policy or change it silently.

## Witnessed procedure once feasible

1. Use nonprivate deterministic fixtures for each direction; retain source byte count and SHA256. Include multi-chunk binary data and a small file. Capture actual phone/iPhone native AirDrop UI and correlated application/OWL session evidence while keeping identifiers private.
2. Run one transfer per matrix cell over the actual AWDL radio path. Prevent IP/LAN/P2P fallback by architecture and demonstrate the selected radio transport; a UI tile or mDNS packet alone cannot prove transfer transport.
3. Obtain the received file from its real device destination via an explicitly user-approved export/read path. Independently compute full byte count/SHA256 at source and destination. If received-original bytes differ due to photo conversion, select a Files-based binary fixture rather than relaxing hash equality.
4. `android/tools/compare_receipts.py original received --device S22|S24 --direction android-to-iphone|iphone-to-android` produces *file-equality evidence only* and intentionally leaves transport/preservation/accepted=false. Correlate this with separately witnessed native UI and real radio/session evidence. Do not edit those booleans to pretend the comparison tool proved transport.
5. Independently record ongoing Wi-Fi association and usable traffic before radio start, during transfer, through stop, and for at least five minutes afterward. Use approved non-sensitive endpoints and redact IP/SSID/MAC details. A single `NET_CAPABILITY_VALIDATED` snapshot is insufficient. Record interruptions; exclusive mode and teardown-induced reconnects cannot silently count as uninterrupted preservation.
6. Repeat on both variants, test user decline/cancel, permission loss, Activity/background/service lifecycle, malformed uploads, bound enforcement and restoration behavior. Stop on connectivity regression; never reload a driver or change settings without approval.
7. Final report must contain the four witnessed cell receipts, exact hardware/OS/driver evidence, build/commit/APK hashes, and preservation results. Any missing cell or connectivity proof keeps acceptance blocked.

## Watchdog/lease behavior

Owner state is in the operator-requested absolute `STATE.json`. `lastProgressAt` advances only when evidence, a test/build, or a commit/push completes; no clock-only heartbeat updates. A human hardware/permission blocker sets ownerLease.status=blocked and autoResumeAllowed=false. The parent owns watchdog policy; this implementation session creates no scheduler or watchdog loops. Do not auto-resume the absent-hardware loop. Resume with a single writer only when concrete hardware/permission evidence changes.
