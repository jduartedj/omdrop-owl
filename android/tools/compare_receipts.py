#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-or-later
"""Local file equality receipt. It deliberately NEVER certifies transport or AirDrop acceptance."""
import argparse
import hashlib
import json
import pathlib

def digest(path):
    md = hashlib.sha256()
    size = 0
    with pathlib.Path(path).open('rb') as f:
        for block in iter(lambda: f.read(65536), b''):
            md.update(block); size += len(block)
    return {'bytes': size, 'sha256': md.hexdigest()}

def compare(original, received, device, direction):
    a, b = digest(original), digest(received)
    return {'schemaVersion': 1, 'deviceLabel': device, 'direction': direction,
            'original': a, 'received': b, 'fileEqualityVerified': a == b,
            'nativeAirDropTransportProven': False, 'wifiPreservationProven': False,
            'accepted': False, 'note': 'Local hash comparison only; separately witnessed native transport evidence is required.'}
if __name__ == '__main__':
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument('original'); p.add_argument('received')
    p.add_argument('--device', required=True, choices=['S22', 'S24'])
    p.add_argument('--direction', required=True, choices=['android-to-iphone', 'iphone-to-android'])
    a = p.parse_args()
    try: result = compare(a.original, a.received, a.device, a.direction)
    except OSError: p.exit(2, 'Input unavailable; no receipt produced.\n')
    print(json.dumps(result, indent=2))
    raise SystemExit(0 if result['fileEqualityVerified'] else 1)
