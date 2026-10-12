# Stock Android / native AirDrop compatibility gate

**Verdict: blocked, not native-AirDrop acceptance.** A normal unprivileged APK cannot satisfy the pinned OWL Linux backend's required capability operations under the cited AOSP security model. This is a source-proven *app privilege/API limitation*, not a claim that every Samsung Wi-Fi chipset is physically incapable of AWDL. The actual S22/S24 variants, Android builds, Wi-Fi chip/driver/firmware, and vendor policy remain unobserved: no authorized ADB devices are attached to the build host, and the two known Android nodes are disconnected without model metadata. There has been no on-device runtime test, installation, packet capture/injection, or phone/router modification.

## Pinned upstreams and licensing

* omdrop-owl: [`ee6a57d971c207c933a424c0d705044a02985b0a`](https://github.com/t4t5/omdrop-owl/tree/ee6a57d971c207c933a424c0d705044a02985b0a).
* Its `pins:7,10`: OWL `832d70f815c3d4a06a02117bf0fc5e868daa1ff0`; omdrop-awdl `534f91a525337951ca19c311e53d333bd6d55100`.
* Receiver inspected separately: omdrop-plugin [`b2f9a537b150c26402c5daad8125fb7e61c214ec`](https://github.com/brentkearney/omdrop-plugin/tree/b2f9a537b150c26402c5daad8125fb7e61c214ec), MIT.
* GitHub API reports omdrop-owl license=null and there is no top-level LICENSE. `PKGBUILD:10-12` documents component licenses (OWL/patches GPL3-or-later, tools GPL2-only, radiotap ISC); it is not an unambiguous blanket license for all new shell helpers. OWL `daemon/io.c:1-18` explicitly grants GPL3-or-later and its COPYING contains GPL3. omdrop-awdl LICENSE declares GPL2-only. Existing helper licensing must be clarified before redistributing those helpers as Android payloads. No relicensing was performed.
* The APK contains only original diagnostic code, **not OWL or any inherited radio/transfer helper**. See `android/NOTICE.md`. Original Android additions are GPL3-or-later; the separately licensed official Gradle wrapper remains Apache2.

## Requirements demonstrated in source

| Layer | Evidence | Consequence |
|---|---|---|
| Physical monitor interface | [omdrop helper:437-454](https://github.com/t4t5/omdrop-owl/blob/ee6a57d971c207c933a424c0d705044a02985b0a/userspace/omdrop-discoverable#L437-L454) creates `mon0`, sets its MAC and link up. `need_root:78`; `cmd_start:377`. | This is not an ordinary Android network/socket API. Monitor creation and interface administration need privileged kernel access and actual driver support. |
| Monitor reception | [OWL io.c:41-84](https://github.com/jedbillyb/owl/blob/832d70f815c3d4a06a02117bf0fc5e868daa1ff0/daemon/io.c#L41-L84) activates libpcap and rejects any datalink other than `DLT_IEEE802_11_RADIO`. | Ethernet/IP connectivity is insufficient: raw radiotap/802.11 reception is required. |
| Frame injection | [OWL io.c:381-389](https://github.com/jedbillyb/owl/blob/832d70f815c3d4a06a02117bf0fc5e868daa1ff0/daemon/io.c#L381-L389) calls `pcap_inject`. | A TCP/UDP or Wi-Fi Direct connection is not an OWL transport. |
| nl80211 administration | [OWL netutils.c:234-268](https://github.com/jedbillyb/owl/blob/832d70f815c3d4a06a02117bf0fc5e868daa1ff0/daemon/netutils.c#L234-L268) sends `NL80211_CMD_SET_INTERFACE`, monitor type and active monitor flags. | NDK does not grant permission to execute these commands. |
| TAP / IPv6 integration | [OWL io.c:127-185](https://github.com/jedbillyb/owl/blob/832d70f815c3d4a06a02117bf0fc5e868daa1ff0/daemon/io.c#L127-L185) opens `/dev/net/tun`, uses `IFF_TAP`, `TUNSETIFF`, hardware address/link/MTU ioctls; `io.c:341-351` wires it into host I/O. | This route needs interface privileges. A VPN IP tunnel alone does not supply the physical AWDL monitor/injection path. |
| Shared radio channel | [omdrop helper:425-430](https://github.com/t4t5/omdrop-owl/blob/ee6a57d971c207c933a424c0d705044a02985b0a/userspace/omdrop-discoverable#L425-L430) accepts only 2437/5220/5745 MHz (channels 6/44/149). | AP association on another channel does not satisfy this particular shared-mode backend. No router setting was changed. Regulatory permission and actual AP frequency must be measured, not guessed. |
| AirDrop above AWDL | [sender:4-8,54-65](https://github.com/brentkearney/omdrop-awdl/blob/534f91a525337951ca19c311e53d333bd6d55100/userspace/airdrop-send.py#L4-L8), [receiver:640-655](https://github.com/brentkearney/omdrop-plugin/blob/b2f9a537b150c26402c5daad8125fb7e61c214ec/bin/airdrop-serve.py#L640-L655), [receiver:1088-1119](https://github.com/brentkearney/omdrop-plugin/blob/b2f9a537b150c26402c5daad8125fb7e61c214ec/bin/airdrop-serve.py#L1088-L1119). | `_airdrop._tcp`, IPv6 scope, TLS, binary-plist Discover/Ask and framed Upload handling are needed in addition to OWL. The current APK does not implement these. |

## Why an ordinary stock app cannot supply this backend

These are **reference source constraints**, not measurements of a particular Samsung firmware:

1. Linux v6.6 [`net/packet/af_packet.c:3341-3353`](https://github.com/torvalds/linux/blob/v6.6/net/packet/af_packet.c#L3341-L3353): `packet_create` fails `-EPERM` unless `ns_capable(net->user_ns, CAP_NET_RAW)` is true.
2. Linux v6.6 [`net/wireless/nl80211.c:16711-16726`](https://github.com/torvalds/linux/blob/v6.6/net/wireless/nl80211.c#L16711-L16726): SET_INTERFACE and NEW_INTERFACE require `GENL_UNS_ADMIN_PERM`. [`net/netlink/genetlink.c:1039-1045`](https://github.com/torvalds/linux/blob/v6.6/net/netlink/genetlink.c#L1039-L1045) enforces `CAP_NET_ADMIN` for that flag. Opening a generic netlink socket is not authorization for those operations.
3. Linux v6.6 [`drivers/net/tun.c:2794-2804`](https://github.com/torvalds/linux/blob/v6.6/drivers/net/tun.c#L2794-L2804): new TUN/TAP creation requires `CAP_NET_ADMIN` plus the LSM security hook. This is the pinned backend's route; alternatives still need actual radio access.
4. AOSP Android 15 tag `android-15.0.0_r1`, [`system/sepolicy/private/app.te:513-518`](https://android.googlesource.com/platform/system/sepolicy/+/android-15.0.0_r1/private/app.te): `neverallow { appdomain -bluetooth -network_stack } self:capability_class_set *;`. Ordinary apps are not the Bluetooth or network-stack platform processes.
5. AOSP same tag, [`frameworks/base/core/jni/com_android_internal_os_Zygote.cpp:2210-2237`](https://android.googlesource.com/platform/frameworks/base/+/android-15.0.0_r1/core/jni/com_android_internal_os_Zygote.cpp): initial capabilities are zero; CAP_NET_ADMIN/RAW are specifically assigned to platform Bluetooth/network-stack UIDs, not arbitrary app UIDs. Native/JNI code runs in the same process/security context: translating OWL to arm64 does not change this calculation.
6. `WifiManager.java` at the same AOSP tag was inspected for public monitor/raw injection facilities. No public radiotap monitor/frame-injection API was found. Absence of a text match is **not** the principal proof; the kernel checks and app policy above are. Hidden-API reflection would not change capabilities or the driver. No reflection, exploit or policy bypass was attempted.

A platform/vendor-sanctioned broker could theoretically provide narrowly scoped radio operations from a suitably privileged process. No such broker, contract, permission, Samsung driver evidence, or approved hardware was supplied. Do not ask the user to disable SELinux or unlock/root phones as an implicit workaround; those actions are outside authorization and are not established solutions.

## Updated hardware evidence vs the October 9 article

The article said only MT7925 had been tested at ingest. The pinned October 11 README now also lists tested Intel AX201/AX211/Panther Lake in **exclusive** mode (`README:21-40`, `owl-profiles:21-23`). Exclusive disconnects Wi-Fi and therefore cannot satisfy the requested Wi-Fi preservation. Qualcomm entries are *untested Linux PCI drivers*, not proof about any Samsung phone or its vendor full-MAC firmware. Device model or application processor alone is not proof of the installed Wi-Fi chip.

MT7925 shared mode is reported upstream, not independently tested here. Upstream also deliberately reconnects after monitor teardown (`README:159-165`, helper:330-343) because the station otherwise wedges. Thus “concurrent Wi-Fi during transfer” must not be silently equated with uninterrupted connectivity across setup/teardown. The user's preservation requirement still needs real-device evidence throughout the entire window and afterward.

## Bounded approaches actually completed

* Source/license/protocol/driver review at the exact SHAs above. No unsafe radio commands were executed.
* Local authorized ADB inventory twice (no devices), plus sanitized available-node metadata (Android nodes disconnected, no model/SoC/OS evidence). Missing hardware is distinct from the source privilege blocker.
* Original NDK probe cross-compiled for `arm64-v8a`; same JNI C source also exercised on the unprivileged Linux host. Host packet socket was denied, but this host observation is **not** a Samsung/Android measurement.
* Gate exhaustively tested for all 3^8 = 6,561 unknown/absent/verified condition combinations. Unknown and missing conditions always block; even an all-verified fixture never claims end-to-end acceptance.
* Public IP/LAN/P2P/VPN substitutions were excluded rather than mislabeled AirDrop. No root/driver/SELinux/config experiments occurred because there is no hardware access or permission for them.

## What unlocks the next bounded step

Connect S22 and S24 with their already authorized read-only ADB sessions (or a connected node exposing equivalent nonunique metadata). Run `python3 android/tools/inspect_devices.py`; it reads only whitelisted model/SoC/OS properties, kernel release, SELinux mode and the `wlan0` driver symlink. It never outputs a serial, IMEI, SSID, MAC, fingerprint, account or logcat. Empty driver/SoC fields stay unknown.

Separately obtain explicit permission before installing the diagnostic APK; its in-app NDK report is necessary to measure the app's own actual sandbox, unlike an ADB shell process. That measurement alone does **not** remove the stock privilege barrier. A permitted vendor/system radio access path with independently verified monitor RX/injection/concurrent Wi-Fi is needed before implementing the real OWL adapter. Clarify helper licensing before packaging upstream tools. Finally require witnessed native iPhone transfers both ways on each phone, original/received byte counts and SHA-256, and continuous Wi-Fi connectivity evidence through start, transfer, stop and at least five minutes afterward. See `ACCEPTANCE.md`.
