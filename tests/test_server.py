import http.client
import base64
import json
from pathlib import Path
import tempfile
import threading
import time
import unittest
from unittest.mock import patch
import sys

sys.path.insert(0, str(Path(__file__).resolve().parents[1]))
from server import Studio, StudioError, validate_endpoint, validated_config, make_handler, ThreadingHTTPServer


class ValidationTests(unittest.TestCase):
    def test_xp_widgets_theme(self):
        for theme in ('xp', 'vista', 'macos', 'xp-widgets', 'vista-widgets', 'macos-widgets', 'default'):
            with self.subTest(theme=theme):
                self.assertEqual(validated_config({'theme': theme}), {'theme': theme})

    def test_private_endpoints(self):
        self.assertEqual(validate_endpoint('192.168.1.211'), '192.168.1.211:5555')
        self.assertEqual(validate_endpoint('10.0.0.4:37123'), '10.0.0.4:37123')
        self.assertEqual(validate_endpoint('172.31.0.2:5555'), '172.31.0.2:5555')

    def test_reject_public_loopback_and_injection(self):
        for value in ('8.8.8.8', '127.0.0.1', 'localhost', '169.254.1.2', '192.168.1.3;reboot',
                      '192.168.1.3:0', '192.168.1.3:65536', '172.32.1.1', '-s', '::1', '10.1.1.1:5555:9'):
            with self.subTest(value=value), self.assertRaises(StudioError):
                validate_endpoint(value)

    def test_config_boolean_and_enum_validation(self):
        for raw in ({'sounds': 'false'}, {'quick_close': 1}, {'theme': 'macos;reboot'},
                    {'language': 'shell'}, {'appearance': 'unknown'}):
            with self.subTest(raw=raw), self.assertRaises(StudioError):
                validated_config(raw)
        self.assertEqual(validated_config({'theme': 'xp', 'quick_close': True}), {'theme': 'xp', 'quick_close': True})

    def test_city_and_shelf_validation(self):
        for raw in ({'city': 'X', 'latitude': float('nan'), 'longitude': 1},
                    {'city': 'X', 'latitude': 91, 'longitude': 1},
                    {'shelf': ['pkg.app;reboot']}, {'shelf': ['pkg.app'] * 9}):
            with self.subTest(raw=raw), self.assertRaises(StudioError):
                validated_config(raw)
        self.assertEqual(validated_config({'city': 'HCM', 'latitude': 10.8, 'longitude': 106.6})['latitude'], 10.8)


class DeviceSafetyTests(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.studio = Studio(self.temp.name)
        self.studio.endpoint = '192.168.1.211:5555'
        self.studio.device = {'identity': 'current'}
        self.studio.require_device = lambda: None

    def tearDown(self):
        self.temp.cleanup()

    def test_keep_other_accessibility_services(self):
        calls = []
        def shell(*args, **kwargs):
            calls.append(args)
            if args == ('settings', 'get', 'secure', 'enabled_accessibility_services'):
                return 'org.other.app/.Reader'
            return ''
        self.studio.shell = shell
        self.studio.set_quick_close_service(True)
        value = next(call[-1] for call in calls if call[:4] == ('settings', 'put', 'secure', 'enabled_accessibility_services'))
        self.assertIn('org.other.app/.Reader', value)
        self.assertIn('QuickCloseService', value)

    def test_disabling_only_removes_our_service(self):
        calls = []
        def shell(*args, **kwargs):
            calls.append(args)
            if args == ('settings', 'get', 'secure', 'enabled_accessibility_services'):
                return 'org.other.app/.Reader:io.freemytv/.QuickCloseService'
            return ''
        self.studio.shell = shell
        self.studio.set_quick_close_service(False)
        self.assertIn(('settings', 'put', 'secure', 'enabled_accessibility_services', 'org.other.app/.Reader'), calls)
        self.assertNotIn(('settings', 'put', 'secure', 'accessibility_enabled', '0'), calls)

    def test_malformed_services_do_not_reach_mutation(self):
        calls = []
        self.studio.shell = lambda *args, **kw: calls.append(args) or 'bad.service;reboot'
        with self.assertRaises(StudioError):
            self.studio.set_quick_close_service(True)
        self.assertEqual(len(calls), 1)

    def test_protect_google_services_and_own_launcher(self):
        mutations = []
        self.studio.shell = lambda *args, **kw: mutations.append(args) or 'package:org.user.app'
        for pkg in ('com.google.android.gms', 'io.freemytv', 'com.android.tv.settings', 'org.user.app;reboot'):
            with self.subTest(pkg=pkg), self.assertRaises(StudioError):
                self.studio.stop_app({'package': pkg}, lambda _: None)
        self.assertFalse(any('force-stop' in call for call in mutations))

    def test_protect_third_party_home_and_keyboard(self):
        def shell(*args, **kw):
            if args[:3] == ('pm', 'list', 'packages'):
                return 'package:org.home.app\npackage:org.keyboard.app'
            if args[:3] == ('cmd', 'package', 'query-activities'):
                return 'org.home.app/.Home'
            if args[:3] == ('settings', 'get', 'secure'):
                return 'org.keyboard.app/.IME'
            self.fail('Unexpected mutation')
        self.studio.shell = shell
        for pkg in ('org.home.app', 'org.keyboard.app'):
            with self.assertRaises(StudioError):
                self.studio.stop_app({'package': pkg}, lambda _: None)

    def test_backup_does_not_claim_success_on_split_apk(self):
        self.studio.inspect = lambda: {'installed': True, 'bridge': False, 'identity': 'current'}
        self.studio.shell = lambda *a, **kw: 'package:/data/base.apk\npackage:/data/split.apk'
        with self.assertRaises(StudioError):
            self.studio.backup({}, lambda _: None)
        self.assertEqual(self.studio.list_backups(), [])
        self.assertTrue(list(Path(self.temp.name).glob('backups/*/INCOMPLETE.txt')))

    def test_restore_rejects_other_tv_before_install(self):
        folder = Path(self.temp.name) / 'backups' / '20261022-120000-1234abcd'
        folder.mkdir()
        (folder / 'backup.json').write_text(json.dumps({'device': {'identity': 'different'}}))
        self.studio.inspect = lambda: {'identity': 'current'}
        self.studio.adb = lambda *a, **kw: self.fail('Must not install anything')
        with self.assertRaisesRegex(StudioError, 'TV khác'):
            self.studio.restore({'id': folder.name}, lambda _: None)

    def test_restore_rejects_tampered_apk(self):
        folder = Path(self.temp.name) / 'backups' / '20261022-120000-1234abcd'
        folder.mkdir()
        (folder / 'backup.json').write_text(json.dumps({'device': {'identity': 'current'}, 'sha256': 'wrong'}))
        (folder / 'launcher.apk').write_bytes(b'tampered')
        self.studio.inspect = lambda: {'identity': 'current'}
        self.studio.adb = lambda *a, **kw: self.fail('Must not install anything')
        with self.assertRaisesRegex(StudioError, 'nguyên vẹn'):
            self.studio.restore({'id': folder.name}, lambda _: None)

    def test_unknown_action_and_busy_job(self):
        with self.assertRaises(StudioError):
            self.studio.start_job('arbitrary-command', {})
        self.studio.operation_lock.acquire()
        try:
            with self.assertRaises(StudioError):
                self.studio.start_job('connect', {})
        finally:
            self.studio.operation_lock.release()

    def test_xiaomi_screenshot_driver_prefix(self):
        png = base64.b64decode('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO+aP1UAAAAASUVORK5CYII=')
        self.studio.adb = lambda *args, **kwargs: b'Init wrapper sys mutex successful.\nopen mma dev failed\n' + png
        result = self.studio.screenshot({}, lambda _: None)
        self.assertIn('/api/screenshot.png', result['url'])
        self.assertEqual((Path(self.temp.name) / 'screen.png').read_bytes(), png)

    def test_windows_getprop_and_bridge_detection(self):
        def shell(*args, **kwargs):
            if args == ('getprop',):
                return '[ro.product.model]: [MiTV-MOOR2]\r\n[ro.product.manufacturer]: [Xiaomi]\r\n[ro.build.version.sdk]: [30]\r\n[ro.build.version.release]: [11]\r\n'
            if args == ('pm', 'list', 'features'):
                return 'feature:android.software.leanback'
            if args[:2] == ('dumpsys', 'package'):
                return 'versionName=1.1-studio'
            if args[:3] == ('cmd', 'package', 'resolve-activity'):
                return 'io.freemytv/.PortableControl' if '.PortableControl' in args[-1] else 'io.freemytv/.HomeActivity'
            if args == ('settings', 'get', 'secure', 'android_id'):
                return 'example-id'
            return '1'
        self.studio.shell = shell
        result = self.studio.inspect()
        self.assertEqual(result['model'], 'MiTV-MOOR2')
        self.assertEqual(result['sdk'], 30)
        self.assertTrue(result['compatible'])
        self.assertTrue(result['bridge'])


class HttpBoundaryTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.temp = tempfile.TemporaryDirectory()
        cls.studio = Studio(cls.temp.name)
        cls.server = ThreadingHTTPServer(('127.0.0.1', 0), make_handler(cls.studio, 'test-token'))
        cls.thread = threading.Thread(target=cls.server.serve_forever, daemon=True)
        cls.thread.start()
        cls.port = cls.server.server_port

    @classmethod
    def tearDownClass(cls):
        cls.server.shutdown()
        cls.server.server_close()
        cls.thread.join(timeout=2)
        cls.temp.cleanup()

    def request(self, method, path, body=None, **headers):
        conn = http.client.HTTPConnection('127.0.0.1', self.port, timeout=4)
        conn.request(method, path, body, headers=headers)
        response = conn.getresponse()
        result = response.status, response.read(), dict(response.getheaders())
        conn.close()
        return result

    def test_api_requires_session(self):
        self.assertEqual(self.request('GET', '/api/state')[0], 403)
        self.assertEqual(self.request('GET', '/api/state', **{'X-Studio-Token': 'test-token'})[0], 200)

    def test_index_injects_token_and_csp(self):
        code, body, headers = self.request('GET', '/')
        self.assertEqual(code, 200)
        self.assertIn(b'test-token', body)
        self.assertNotIn(b'__STUDIO_TOKEN__', body)
        self.assertIn("frame-ancestors 'none'", headers['Content-Security-Policy'])

    def test_reject_dns_rebinding_and_traversal(self):
        self.assertEqual(self.request('GET', '/', Host='attacker.example')[0], 403)
        self.assertEqual(self.request('GET', '/%2e%2e/server.py')[0], 404)

    def test_reject_cross_origin_and_missing_origin(self):
        headers = {'X-Studio-Token': 'test-token', 'Content-Type': 'application/json'}
        self.assertEqual(self.request('POST', '/api/actions/connect', '{}', **headers)[0], 403)
        self.assertEqual(self.request('POST', '/api/actions/connect', '{}', Origin='https://evil.example', **headers)[0], 403)

    def test_reject_unknown_action_and_non_json(self):
        headers = {'X-Studio-Token': 'test-token', 'Origin': f'http://127.0.0.1:{self.port}'}
        self.assertEqual(self.request('POST', '/api/actions/connect', '{}', **headers)[0], 415)
        headers['Content-Type'] = 'application/json'
        self.assertEqual(self.request('POST', '/api/actions/arbitrary-command', '{}', **headers)[0], 400)
        self.assertEqual(self.request('POST', '/api/actions/connect', '[]', **headers)[0], 400)


if __name__ == '__main__':
    unittest.main()
