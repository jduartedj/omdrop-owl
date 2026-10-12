# Diagnostic artifact and future adapter boundary

Original additions: GPL-3.0-or-later. This is a compatibility prototype, not a transfer-capable AirDrop app.

## Implemented and testable now

* One Java `Activity`, API31+ (Android12+), target/compile36; Samsung arm64 ABI only. System-bar/cutout insets, scrollable/selectable diagnostics, explicit unavailable Send/Receive controls. No Wi-Fi, Bluetooth, nearby-device, storage-all-files, location, root, or VPN permission requests. The two normal permissions are INTERNET (socket-creation diagnostic) and ACCESS_NETWORK_STATE (OS connectivity metadata).
* `NativeProbe` → JNI → `native_probe.c`: read this process's `CapEff`; create and immediately close AF_PACKET/SOCK_RAW **protocol zero** and unbound NETLINK_GENERIC sockets; read SELinux enforcing if available. It never binds, transmits, receives, registers packet capture, sends netlink messages, creates a TAP, modifies a radio, requests root, or launches a shell. Kernel packet code registers a receive hook only when protocol !=0 (`af_packet.c:3396-3399`). It reports unknown on unavailable native library/proc/sysfs evidence.
* `CapabilityGate`: eight necessary evidence conditions, three evidence levels. No unknown/absent condition becomes ready. A theoretical all-verified fixture is only a hardware-test candidate, never acceptance. Production always reports OWL and AirDrop protocol implementations absent.
* `OwlBackend.Unavailable`: explicit nonimplemented adapter seam; start/send/receive always throw. There is no hidden service, LAN, Wi-Fi Direct, Bluetooth transfer, HTTP listener, peer enumeration or fake transfer-progress path.
* SAF document hashing: explicit user selection, request local-only providers, 512MiB maximum, streaming SHA256 and byte count, no original filename/URI export. Provider I/O errors, oversized inputs and interruptions produce no receipt. Per-operation generation invalidates cancelled/superseded UI callbacks; Activity destruction shuts down the worker. A stuck provider can still delay I/O cancellation, so no bounded wall-time guarantee is claimed.
* Optional user-directed SAF diagnostic JSON save. It contains nonunique model/SoC/OS metadata and booleans/errno, not serial/IMEI/MAC/SSID/accounts/fingerprint. Local file hashes may identify private content; exports stay user-controlled and are not automatically uploaded. A single active-network snapshot is never Wi-Fi preservation proof.
* Read-only ADB inventory tool and host local-receipt comparison tool. Matching files never certify AirDrop transport or acceptance. Native C parsing tests under ASan/UBSan, Linux host JNI smoke, JVM gate/digest/async-ownership tests, and Python privacy/receipt tests are distinct from Android runtime tests.

## Not implemented, and why

There is no radio-privileged daemon/broker, OWL engine build/linkage, AirDrop discovery, TLS identity, binary-plist protocol, upload/archive receiver, notification/foreground transfer service, Android share-sheet import, or contact policy. Implementing a real transfer layer before any permitted radio path is shown would not make the requested stock phones interoperable. No fake implementation is substituted.

The next integration must preserve these boundaries:

1. Identify each actual phone variant, vendor OS/kernel, Wi-Fi chip/driver/firmware, AP channel and permitted country/regulatory state using approved read-only evidence.
2. Obtain a vendor-sanctioned/system-owned radio contract (or separately approved compatible hardware path). An ordinary APK must not manufacture privileges. Any service must authenticate clients and expose only narrow AWDL radio operations; no arbitrary shell/capability delegation.
3. Prove driver support for radiotap monitor RX, permitted injection and hardware ACK behavior, plus AP coexistence. A CPU model, generic Linux driver name or successful socket-open alone is not proof. Do not infer Samsung support from MT7925 or untested Linux Qualcomm PCI cards.
4. Build the pinned, licensed OWL engine and Android-specific fd/transport adapter only once that path exists. Track OWL/GPL3 and dependency licenses/provenance; do not combine GPL2-only tools into a GPL3 binary without a valid licensing basis. The upstream shell helper additionally assumes NetworkManager, polkit, Linux filesystem/service layout and driver knobs: it is not directly Android-compatible.
5. Implement scoped IPv6 + `_airdrop._tcp` discovery/announcements and native iPhone protocol above AWDL. Receiver approval, bounded archive decoding, destination SAF ownership, cancellation, durable receipts, lifecycle/foreground restrictions and identity handling need actual tests. Never copy permissive upstream TLS settings as a production security design without review.
6. Test two-way native iPhone interoperability and Wi-Fi preservation on **both** target devices. Only this permits an acceptance claim; see ACCEPTANCE.md.

No future adapter implementation or physical compatibility is promised until those hardware/permission gates are satisfied.
