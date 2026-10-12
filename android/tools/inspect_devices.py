#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Read-only ADB inventory. Serial values stay inside subprocess arguments, never output."""
import json
import pathlib
import subprocess
import sys
from datetime import datetime, timezone
PROPS = {
    'manufacturer': 'ro.product.manufacturer', 'model': 'ro.product.model',
    'deviceVariant': 'ro.product.device', 'socManufacturer': 'ro.soc.manufacturer',
    'socModel': 'ro.soc.model', 'hardware': 'ro.hardware', 'boardPlatform': 'ro.board.platform',
    'androidRelease': 'ro.build.version.release', 'api': 'ro.build.version.sdk',
    'securityPatch': 'ro.build.version.security_patch', 'buildType': 'ro.build.type',
}
def run(args):
    try:
        p = subprocess.run(['adb', *args], stdout=subprocess.PIPE, stderr=subprocess.DEVNULL,
                           text=True, timeout=15, check=False)
        return p.stdout.strip() if p.returncode == 0 else None
    except (OSError, subprocess.TimeoutExpired):
        return None

def inspect():
    raw = run(['devices'])
    if raw is None:
        return {'schemaVersion': 1, 'status': 'adb_unavailable', 'devices': []}
    devices = []
    states = {}
    for line in raw.splitlines()[1:]:
        fields = line.split()
        if len(fields) < 2: continue
        serial, state = fields[:2]
        states[state] = states.get(state, 0) + 1
        if state != 'device': continue
        # Fixed whitelist only; no full getprop, SSID, MAC, IMEI, build fingerprint, accounts, or logs.
        record = {key: run(['-s', serial, 'shell', 'getprop', prop]) for key, prop in PROPS.items()}
        record['kernelRelease'] = run(['-s', serial, 'shell', 'uname', '-r'])
        record['selinuxMode'] = run(['-s', serial, 'shell', 'getenforce'])
        driver = run(['-s', serial, 'shell', 'readlink', '-f', '/sys/class/net/wlan0/device/driver'])
        record['wlan0Driver'] = pathlib.PurePosixPath(driver).name if driver else None
        record['driverEvidenceScope'] = 'wlan0 sysfs read only; empty is unknown, not unsupported'
        record['appCapabilityProbeExecuted'] = False
        record['monitorInjectionTestExecuted'] = False
        devices.append(record)
    return {'schemaVersion': 1, 'observedAt': datetime.now(timezone.utc).isoformat(),
            'status': 'devices_observed' if devices else 'no_authorized_device',
            'connectionStateCounts': states, 'devices': devices,
            'acceptance': False, 'settingsChanged': False}
if __name__ == '__main__':
    json.dump(inspect(), sys.stdout, indent=2); print()
