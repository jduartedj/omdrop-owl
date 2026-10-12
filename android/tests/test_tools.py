# SPDX-License-Identifier: GPL-3.0-or-later
import importlib.util
import pathlib
import tempfile
import unittest
from unittest.mock import patch
ROOT = pathlib.Path(__file__).resolve().parents[1]
def load(name):
    spec=importlib.util.spec_from_file_location(name, ROOT/'tools'/f'{name}.py')
    m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m
compare=load('compare_receipts'); inspect=load('inspect_devices')
class ToolsTest(unittest.TestCase):
    def test_matching_files_never_claim_transport(self):
        with tempfile.TemporaryDirectory() as d:
            a=pathlib.Path(d)/'a'; b=pathlib.Path(d)/'b'
            a.write_bytes(b'abc');b.write_bytes(b'abc')
            r=compare.compare(a,b,'S22','android-to-iphone')
            self.assertTrue(r['fileEqualityVerified'])
            self.assertFalse(r['accepted']);self.assertFalse(r['nativeAirDropTransportProven'])
            self.assertFalse(r['wifiPreservationProven']);self.assertNotIn(str(a),str(r))
    def test_mismatch(self):
        with tempfile.TemporaryDirectory() as d:
            a=pathlib.Path(d)/'a';b=pathlib.Path(d)/'b';a.write_bytes(b'a');b.write_bytes(b'b')
            self.assertFalse(compare.compare(a,b,'S24','iphone-to-android')['fileEqualityVerified'])
    def test_missing_file(self):
        with self.assertRaises(OSError): compare.digest('/nonexistent/fixture')
    def test_no_devices_is_not_hardware_unsupported(self):
        with patch.object(inspect,'run',return_value='List of devices attached\n'):
            r=inspect.inspect(); self.assertEqual(r['status'],'no_authorized_device');self.assertEqual(r['devices'],[])
    def test_serial_never_output_and_only_whitelisted_reads(self):
        calls=[]
        def fake(args):
            calls.append(args)
            if args==['devices']: return 'List of devices attached\nSECRET_FIXTURE_SERIAL\tdevice\n'
            if 'getprop' in args: return 'fixture-property'
            if 'readlink' in args: return '/sys/bus/platform/drivers/fixture-wifi'
            return 'fixture'
        with patch.object(inspect,'run',side_effect=fake): r=inspect.inspect()
        self.assertNotIn('SECRET_FIXTURE_SERIAL',str(r))
        self.assertEqual(r['devices'][0]['wlan0Driver'],'fixture-wifi')
        self.assertTrue(all(c==['devices'] or c[2:4]==['shell','getprop'] or c[3] in ['uname','getenforce','readlink'] for c in calls))
        self.assertFalse(r['settingsChanged']);self.assertFalse(r['acceptance'])
    def test_unauthorized_device_not_read(self):
        with patch.object(inspect,'run',return_value='List of devices attached\nsecret\tunauthorized\n') as call:
            r=inspect.inspect();self.assertEqual(call.call_count,1);self.assertEqual(r['devices'],[])
            self.assertNotIn('secret',str(r))
    def test_adb_unavailable(self):
        with patch.object(inspect,'run',return_value=None): self.assertEqual(inspect.inspect()['status'],'adb_unavailable')
if __name__=='__main__': unittest.main()
