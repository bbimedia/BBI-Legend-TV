"""BBI Legend TV: localhost UI and a narrow, authenticated ADB API. Python 3.10+."""
from __future__ import annotations

import argparse
import hashlib
import ipaddress
import json
import mimetypes
import os
from pathlib import Path
import platform
import re
import secrets
import shutil
import subprocess
import threading
import time
from datetime import datetime, timezone
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import unquote, urlsplit
from urllib.request import urlopen
import uuid
import webbrowser
import zipfile

ROOT = Path(__file__).resolve().parent
PKG = 'io.freemytv'
EXPORT = '/sdcard/Android/data/io.freemytv/files/portable-export.json'
IMPORT = '/sdcard/Android/data/io.freemytv/files/portable-import.json'
SERVICE = 'io.freemytv/io.freemytv.QuickCloseService'
THEMES = {'default', 'xp', 'xp-widgets', 'vista', 'macos'}
SAFE_SYSTEM_APPS = {'com.google.android.youtube.tv', 'com.android.vending', 'com.netflix.ninja',
                    'com.amazon.amazonvideo.livingroom', 'com.google.android.videos', 'com.spotify.tv.android'}
PKG_RE = re.compile(r'[A-Za-z0-9_]+(?:\.[A-Za-z0-9_]+)+\Z')
COMPONENT_RE = re.compile(r'[A-Za-z0-9_.]+/[A-Za-z0-9_.$]+\Z')


class StudioError(Exception):
    pass


def validate_endpoint(value):
    value = str(value).strip()
    parts = value.split(':')
    if len(parts) > 2:
        raise StudioError('Nhập IPv4 nội bộ, ví dụ 192.168.1.211:5555.')
    try:
        ip = ipaddress.IPv4Address(parts[0])
        port = int(parts[1]) if len(parts) == 2 else 5555
    except ValueError:
        raise StudioError('Địa chỉ IP hoặc cổng không hợp lệ.') from None
    networks = ('10.0.0.0/8', '172.16.0.0/12', '192.168.0.0/16')
    if not any(ip in ipaddress.ip_network(n) for n in networks) or not 1 <= port <= 65535:
        raise StudioError('Chỉ kết nối TV trong mạng nội bộ (10.x, 172.16–31.x, 192.168.x).')
    return f'{ip}:{port}'


def validated_config(raw):
    if not isinstance(raw, dict):
        raise StudioError('Cài đặt không hợp lệ.')
    out = {}
    for key in ('sounds', 'weather', 'quotes', 'quick_close'):
        if key in raw:
            if type(raw[key]) is not bool:
                raise StudioError(f'{key} cần giá trị bật/tắt.')
            out[key] = raw[key]
    for key, allowed in {'theme': THEMES, 'temperature': {'C', 'F', 'auto'},
                         'appearance': {'auto', 'light', 'dark'},
                         'language': {'auto', 'en', 'pt', 'es', 'de', 'ru'}}.items():
        if key in raw:
            if raw[key] not in allowed:
                raise StudioError(f'Giá trị {key} không hợp lệ.')
            out[key] = raw[key]
    if 'city' in raw:
        name = str(raw['city']).strip()
        try:
            lat, lon = float(raw['latitude']), float(raw['longitude'])
        except (KeyError, TypeError, ValueError):
            raise StudioError('Thiếu tọa độ thành phố.') from None
        if not name or len(name) > 80 or not -90 <= lat <= 90 or not -180 <= lon <= 180:
            raise StudioError('Thành phố hoặc tọa độ không hợp lệ.')
        out.update(city=name, latitude=lat, longitude=lon)
    if 'shelf' in raw:
        shelf = raw['shelf']
        if not isinstance(shelf, list) or len(shelf) > 8 or any(not isinstance(p, str) or not PKG_RE.fullmatch(p) for p in shelf):
            raise StudioError('Danh sách ứng dụng không hợp lệ.')
        out['shelf'] = shelf
    return out


def sha256(path):
    h = hashlib.sha256()
    with open(path, 'rb') as f:
        for block in iter(lambda: f.read(1024 * 1024), b''):
            h.update(block)
    return h.hexdigest()


class Studio:
    def __init__(self, data=None, adb=None):
        self.data = Path(data or ROOT / 'data').resolve()
        self.data.mkdir(parents=True, exist_ok=True)
        (self.data / 'backups').mkdir(exist_ok=True)
        self.adb_override = adb
        self.endpoint = None
        self.device = None
        self.config = None
        self.jobs = {}
        self.operation_lock = threading.Lock()

    def adb_path(self, required=True):
        suffix = 'adb.exe' if os.name == 'nt' else 'adb'
        candidates = [self.adb_override, os.environ.get('TVMOD_ADB'),
                      self.data / 'platform-tools' / suffix, ROOT / 'tools' / 'platform-tools' / suffix,
                      shutil.which('adb')]
        for candidate in candidates:
            if candidate and Path(candidate).is_file():
                return str(Path(candidate).resolve())
        if required:
            raise StudioError('Chưa có ADB. Bấm “Chuẩn bị công cụ” để tải từ Google.')
        return None

    def adb(self, args, *, device=True, timeout=30, binary=False, check=True):
        cmd = [self.adb_path()]
        if device:
            if not self.endpoint:
                raise StudioError('Kết nối TV trước khi thực hiện.')
            cmd += ['-s', self.endpoint]
        cmd += list(args)
        try:
            result = subprocess.run(cmd, capture_output=True, timeout=timeout,
                                    creationflags=subprocess.CREATE_NO_WINDOW if os.name == 'nt' else 0)
        except subprocess.TimeoutExpired:
            raise StudioError('TV phản hồi quá lâu. Kiểm tra kết nối và thử lại.') from None
        except OSError as e:
            raise StudioError(f'Không chạy được ADB: {e}') from None
        if binary:
            if check and result.returncode:
                raise StudioError(result.stderr.decode('utf-8', 'replace')[:1000])
            return result.stdout
        text = (result.stdout + result.stderr).decode('utf-8', 'replace').replace('\r\n', '\n').strip()
        if check and result.returncode:
            if 'unauthorized' in text:
                raise StudioError('Chờ cấp quyền trên TV: tick “Luôn cho phép” rồi chọn OK.')
            raise StudioError(text[:1800] or 'ADB không thực hiện được thao tác.')
        return text

    def shell(self, *args, **kwargs):
        return self.adb(['shell', *args], **kwargs)

    def require_device(self):
        if not self.device or not self.endpoint:
            raise StudioError('Kết nối và kiểm tra TV trước.')
        if self.adb(['get-state']) != 'device':
            raise StudioError('TV đã ngắt kết nối. Vui lòng kết nối lại.')

    def prepare(self, payload, log):
        if self.adb_path(False):
            log('ADB đã sẵn sàng. Không cần cài Java hay Android Studio.')
            return {'ready': True}
        os_name = {'Windows': 'windows', 'Linux': 'linux', 'Darwin': 'darwin'}.get(platform.system())
        if not os_name:
            raise StudioError('Hệ điều hành máy tính chưa được hỗ trợ.')
        if os_name == 'linux' and platform.machine().lower() not in ('x86_64', 'amd64'):
            raise StudioError('Linux ARM cần ADB tương thích từ máy của bạn. Đặt ADB trên PATH hoặc dùng TVMOD_ADB.')
        log('Đang tải Android Platform Tools chính thức từ Google…')
        archive = self.data / 'platform-tools-download.zip'
        try:
            with urlopen(f'https://dl.google.com/android/repository/platform-tools-latest-{os_name}.zip', timeout=60) as response, open(archive, 'wb') as out:
                shutil.copyfileobj(response, out)
            with zipfile.ZipFile(archive) as z:
                for member in z.infolist():
                    target = (self.data / member.filename).resolve()
                    if not target.is_relative_to(self.data) or not member.filename.startswith('platform-tools/'):
                        raise StudioError('Gói công cụ có đường dẫn không an toàn.')
                    z.extract(member, self.data)
                    if os.name != 'nt' and target.is_file():
                        target.chmod((member.external_attr >> 16) & 0o777 or 0o755)
            log('Đã chuẩn bị ADB cho ' + platform.system() + '.')
            return {'ready': bool(self.adb_path())}
        finally:
            archive.unlink(missing_ok=True)

    def pair(self, payload, log):
        address = validate_endpoint(payload.get('endpoint', ''))
        code = str(payload.get('code', '')).strip()
        if not re.fullmatch(r'\d{6}', code):
            raise StudioError('Mã ghép nối cần đúng 6 chữ số trên TV.')
        # Pairing code is transient; never included in job logs or persistent state.
        result = self.adb(['pair', address, code], device=False, timeout=40)
        if 'Successfully paired' not in result:
            raise StudioError('Ghép nối chưa thành công. Mở mã mới trên TV và thử lại.')
        log('Đã ghép nối. Nhập cổng kết nối (khác cổng ghép nối) để kết nối TV.')
        return {'paired': True}

    def connect(self, payload, log):
        endpoint = validate_endpoint(payload.get('endpoint', ''))
        self.device = None
        self.config = None
        self.endpoint = endpoint
        log('Đang kết nối TV tại ' + endpoint + '…')
        result = self.adb(['connect', endpoint], device=False, timeout=20)
        if 'failed to authenticate' in result or 'unauthorized' in result:
            raise StudioError('TV đang chờ cấp quyền. Chọn “Luôn cho phép” → OK trên TV, rồi kết nối lại.')
        if 'connected to' not in result:
            raise StudioError('Không kết nối được TV. Kiểm tra IP, cùng mạng và USB debugging. ' + result[:250])
        self.device = self.inspect()
        if self.device['bridge']:
            try:
                self.config = self.bridge('export')
            except StudioError as e:
                log('Đã kết nối; chưa đọc được cài đặt: ' + str(e))
        log(f"Đã đọc {self.device['model']} • Android {self.device['android']}.")
        return {'device': self.device, 'config': self.config}

    def inspect(self):
        raw = self.shell('getprop')
        props = dict(re.findall(r'^\[([^\]]+)\]: \[(.*)\]\r?$', raw, re.M))
        features = self.shell('pm', 'list', 'features')
        sdk = int(props.get('ro.build.version.sdk', '0'))
        tv = 'android.software.leanback' in features or 'android.hardware.type.television' in features
        port = int(self.endpoint.rsplit(':', 1)[1])
        reasons = []
        if sdk < 28:
            reasons.append('Cần Android 9 trở lên (API 28).')
        if not tv:
            reasons.append('Thiết bị không khai báo Android TV / Google TV.')
        if port != 5555:
            reasons.append('Các chức năng launcher cần ADB cổng 5555. Bật chế độ tương thích trước.')
        home = self.shell('cmd', 'package', 'resolve-activity', '--brief', '-a', 'android.intent.action.MAIN', '-c', 'android.intent.category.HOME', check=False)
        package = self.shell('dumpsys', 'package', PKG, check=False)
        installed = 'versionName=' in package
        aid = self.shell('settings', 'get', 'secure', 'android_id')
        identity = hashlib.sha256((props.get('ro.product.manufacturer', '') + ':' + aid).encode()).hexdigest()
        disabled = self.shell('pm', 'list', 'packages', '-d').splitlines()
        bridge = installed and '.PortableControl' in self.shell('cmd', 'package', 'resolve-activity', '--brief', '-n', PKG + '/.PortableControl', check=False)
        return {'endpoint': self.endpoint, 'brand': props.get('ro.product.manufacturer', ''),
                'model': props.get('ro.product.model', 'TV'), 'android': props.get('ro.build.version.release', '?'),
                'sdk': sdk, 'tv': tv, 'compatible': not reasons, 'reasons': reasons,
                'identity': identity, 'installed': installed, 'bridge': bool(bridge),
                'home': next((x.strip() for x in home.splitlines() if '/' in x), home),
                'date': self.shell('date'), 'auto_time': self.shell('settings', 'get', 'global', 'auto_time') == '1',
                'disabled': [x.removeprefix('package:') for x in disabled],
                'tested': 'MOOR2' in props.get('ro.product.model', ''),
                'version': (re.search(r'versionName=(\S+)', package).group(1) if installed else None)}

    def compat(self, payload, log):
        self.require_device()
        host = self.endpoint.split(':')[0]
        log('Chuyển ADB của TV về cổng 5555 để launcher có thể tự kết nối…')
        self.adb(['tcpip', '5555'], timeout=20)
        time.sleep(1)
        return self.connect({'endpoint': host + ':5555'}, log)

    def bridge(self, action, config=None):
        request = uuid.uuid4().hex
        if config is not None:
            config = validated_config(config)
            local = self.data / ('config-' + request + '.json')
            try:
                local.write_text(json.dumps(config, ensure_ascii=False), encoding='utf-8')
                self.shell('mkdir', '-p', '/sdcard/Android/data/io.freemytv/files')
                self.adb(['push', str(local), IMPORT])
            finally:
                local.unlink(missing_ok=True)
        # The bridge finishes in onCreate; -W can wait forever for a window that never appears.
        self.adb(['shell', 'am', 'start', '-n', PKG + '/.PortableControl', '--es', 'action', action,
                  '--es', 'request', request], timeout=30)
        for _ in range(20):
            raw = self.shell('cat', EXPORT, check=False)
            try:
                result = json.loads(raw)
            except ValueError:
                result = {}
            if result.get('request') == request:
                if not result.get('ok'):
                    raise StudioError(result.get('error', 'Không đọc được cài đặt ứng dụng.'))
                result.pop('request', None)
                result.pop('ok', None)
                return result
            time.sleep(0.15)
        raise StudioError('Bản cài chưa hỗ trợ đọc/ghi cài đặt hoặc TV chưa phản hồi. Cài bản BBI Legend TV trước.')

    def backup(self, payload, log):
        self.require_device()
        self.device = self.inspect()
        backup_id = datetime.now().strftime('%Y%m%d-%H%M%S-') + uuid.uuid4().hex[:8]
        folder = self.data / 'backups' / backup_id
        folder.mkdir()
        metadata = {'id': backup_id, 'created': datetime.now(timezone.utc).isoformat(),
                    'device': self.device, 'label': str(payload.get('label', 'Sao lưu thủ công'))[:80],
                    'scope': 'Bản cài Free My TV và cài đặt ứng dụng nếu bản cài hỗ trợ. Không phải bản sao firmware/tài khoản/Wi-Fi.'}
        try:
            if self.device['installed']:
                log('Đang sao lưu bản cài hiện tại…')
                paths = self.shell('pm', 'path', PKG).splitlines()
                apks = [p[8:] for p in paths if p.startswith('package:')]
                if len(apks) != 1:
                    raise StudioError('Bản cài dạng split APK chưa hỗ trợ sao lưu; dừng để không tạo bản backup thiếu.')
                self.adb(['pull', apks[0], str(folder / 'launcher.apk')], timeout=60)
                metadata['sha256'] = sha256(folder / 'launcher.apk')
                if self.device['bridge']:
                    metadata['config'] = self.bridge('export')
                else:
                    metadata['note'] = 'Bản cũ không có cầu nối cài đặt: lưu APK, giữ dữ liệu khi cài đè; không snapshot cài đặt riêng.'
            else:
                metadata['note'] = 'TV chưa có Free My TV: chỉ lưu thông tin ban đầu. Quay về launcher gốc bằng Original trên TV.'
            (folder / 'backup.json').write_text(json.dumps(metadata, ensure_ascii=False, indent=2), encoding='utf-8')
            log('Đã lưu backup: ' + backup_id)
            return metadata
        except Exception:
            # Retain incomplete files for recovery, but do not expose them as valid backups.
            (folder / 'INCOMPLETE.txt').write_text('Backup chưa hoàn tất. Không dùng để khôi phục.', encoding='utf-8')
            raise

    def list_backups(self):
        result = []
        for file in sorted((self.data / 'backups').glob('*/backup.json'), reverse=True):
            try:
                info = json.loads(file.read_text(encoding='utf-8'))
                info['restorable'] = (file.parent / 'launcher.apk').is_file()
                result.append(info)
            except (ValueError, OSError):
                continue
        return result

    def set_quick_close_service(self, enabled):
        raw = self.shell('settings', 'get', 'secure', 'enabled_accessibility_services')
        services = [] if raw in ('', 'null') else raw.split(':')
        if any(not COMPONENT_RE.fullmatch(s) for s in services):
            raise StudioError('Danh sách Trợ năng có định dạng lạ; chưa thay đổi quyền. Bật thủ công trên TV.')
        services = [s for s in services if s not in (SERVICE, PKG + '/.QuickCloseService')]
        if enabled:
            services.append(SERVICE)
        if services:
            self.shell('settings', 'put', 'secure', 'enabled_accessibility_services', ':'.join(services))
            self.shell('settings', 'put', 'secure', 'accessibility_enabled', '1')
        else:
            self.shell('settings', 'delete', 'secure', 'enabled_accessibility_services')
            self.shell('settings', 'put', 'secure', 'accessibility_enabled', '0')

    def install(self, payload, log):
        self.require_device()
        self.device = self.inspect()
        if not self.device['compatible']:
            raise StudioError('Chưa thể cài: ' + ' '.join(self.device['reasons']))
        config = validated_config(payload.get('config', {}))
        if 'theme' not in config:
            raise StudioError('Chọn giao diện trước khi cài.')
        apk = ROOT / 'packages' / 'FreeMyTV-Studio.apk'
        manifest = json.loads((ROOT / 'packages' / 'manifest.json').read_text(encoding='utf-8'))
        if sha256(apk) != manifest['sha256']:
            raise StudioError('Bản cài bị thay đổi hoặc hỏng. Dừng cài đặt.')
        old = self.backup({'label': 'Tự động trước khi cài ' + config['theme']}, log)
        log('Cài bản BBI Legend TV, giữ dữ liệu ứng dụng hiện có…')
        result = self.adb(['install', '-r', str(apk)], timeout=120)
        if 'Success' not in result:
            raise StudioError('Cài chưa thành công. Không gỡ bản cũ: ' + result)
        self.device = self.inspect()
        self.config = self.bridge('apply', config)
        self.set_quick_close_service(self.config.get('quick_close', False))
        active = PKG in self.device['home']
        self.shell('am', 'start', '-n', PKG + ('/.HomeActivity' if active else '/.SettingsActivity'))
        log('Đã cài. ' + ('Theme mới đã sẵn sàng.' if active else 'Trên TV chọn Home Screen → Free My TV và chấp nhận yêu cầu gỡ lỗi.'))
        return {'config': self.config, 'backup': old['id'], 'needs_activation': not active}

    def apply(self, payload, log):
        self.require_device()
        if not self.inspect()['bridge']:
            raise StudioError('Cài bản BBI Legend TV trước để dùng cài đặt nhanh và đổi theme.')
        config = validated_config(payload.get('config', {}))
        if not config:
            raise StudioError('Chưa chọn thay đổi.')
        saved = self.backup({'label': 'Tự động trước khi đổi cài đặt'}, log)
        log('Áp dụng cài đặt đã chọn…')
        self.config = self.bridge('apply', config)
        if 'quick_close' in config:
            self.set_quick_close_service(config['quick_close'])
        self.shell('am', 'start', '-n', PKG + '/.HomeActivity')
        return {'config': self.config, 'backup': saved['id']}

    def restore(self, payload, log):
        self.require_device()
        backup_id = str(payload.get('id', ''))
        if not re.fullmatch(r'\d{8}-\d{6}-[a-f0-9]{8}', backup_id):
            raise StudioError('Mã backup không hợp lệ.')
        folder = self.data / 'backups' / backup_id
        info = json.loads((folder / 'backup.json').read_text(encoding='utf-8'))
        current = self.inspect()
        if info['device']['identity'] != current['identity']:
            raise StudioError('Backup này thuộc TV khác. Dừng để không áp dụng sai cấu hình.')
        apk = folder / 'launcher.apk'
        if not apk.is_file() or sha256(apk) != info.get('sha256'):
            raise StudioError('Không có APK hoặc bản sao không còn nguyên vẹn.')
        safety = self.backup({'label': 'Tự động trước khi khôi phục'}, log)
        log('Khôi phục bản cài bằng cách cài đè, giữ dữ liệu…')
        self.adb(['install', '-r', str(apk)], timeout=120)
        if 'config' in info:
            self.config = self.bridge('import', info['config'])
            self.set_quick_close_service(self.config.get('quick_close', False))
        else:
            self.config = None
        self.device = self.inspect()
        self.shell('am', 'start', '-n', PKG + '/.HomeActivity')
        return {'restored': backup_id, 'safety_backup': safety['id'], 'config': self.config}

    def apps(self, payload, log):
        self.require_device()
        raw = self.shell('dumpsys', 'activity', 'processes', timeout=25)
        third = set(re.findall(r'package:([^\s]+)', self.shell('pm', 'list', 'packages', '-3')))
        processes = []
        for match in re.finditer(r'ProcessRecord\{\S+ (\d+):([^/\s]+)[^\n]*\}\s*([\s\S]*?)(?=\n  \*|\Z)', raw):
            pid, name, block = match.groups()
            package = name.split(':')[0]
            if '.' not in package:
                continue
            pss = re.search(r'lastPss=([^\s]+)', block)
            procstate = re.search(r'curProcState=(\d+)', block)
            state = int(procstate.group(1)) if procstate else 99
            # Only explicitly selected, launchable consumer apps can be force-stopped here.
            allowed = package in third or package in SAFE_SYSTEM_APPS
            if package == PKG or package.endswith('.inputmethod'):
                allowed = False
            processes.append({'pid': int(pid), 'name': name, 'package': package,
                              'memory': pss.group(1) if pss else '—', 'state': 'Đang mở' if state == 2 else 'Bộ nhớ đệm' if state >= 15 else 'Chạy nền',
                              'can_stop': allowed})
        memory = self.shell('dumpsys', 'meminfo', '--oom', timeout=30)
        summary = [x.strip() for x in memory.splitlines() if x.startswith(('Total RAM:', ' Free RAM:', ' Used RAM:'))]
        return {'apps': sorted(processes, key=lambda x: (not x['can_stop'], x['name'])), 'memory': summary}

    def stop_app(self, payload, log):
        self.require_device()
        pkg = str(payload.get('package', ''))
        if not PKG_RE.fullmatch(pkg):
            raise StudioError('Tên ứng dụng không hợp lệ.')
        third = set(re.findall(r'package:([^\s]+)', self.shell('pm', 'list', 'packages', '-3')))
        protected = {PKG, 'com.google.android.gms', 'com.google.android.gsf', 'com.android.systemui', 'com.android.tv.settings'}
        if pkg in protected or pkg not in third | SAFE_SYSTEM_APPS:
            raise StudioError('Ứng dụng hệ thống được bảo vệ.')
        # Exclude third-party launchers and keyboards, too.
        homes = self.shell('cmd', 'package', 'query-activities', '--brief', '-a', 'android.intent.action.MAIN', '-c', 'android.intent.category.HOME', check=False)
        keyboards = self.shell('settings', 'get', 'secure', 'enabled_input_methods')
        if pkg + '/' in homes or pkg + '/' in keyboards:
            raise StudioError('Launcher hoặc bàn phím được bảo vệ.')
        self.shell('am', 'force-stop', '--user', '0', pkg)
        log('Đã buộc dừng ' + pkg + '. Dữ liệu được giữ nguyên.')
        return {'stopped': pkg}

    def screenshot(self, payload, log):
        self.require_device()
        raw = self.adb(['exec-out', 'screencap', '-p'], binary=True, timeout=30)
        # Some Xiaomi firmware writes a small driver diagnostic before the PNG bytes.
        offset = raw.find(b'\x89PNG\r\n\x1a\n', 0, 4096)
        if offset < 0 or raw[offset + 12:offset + 16] != b'IHDR':
            raise StudioError('TV chưa trả về ảnh chụp hợp lệ.')
        raw = raw[offset:]
        (self.data / 'screen.png').write_bytes(raw)
        return {'url': '/api/screenshot.png?t=' + str(time.time_ns())}

    def quick(self, payload, log):
        self.require_device()
        action = payload.get('action')
        intents = {'network': 'android.settings.WIFI_SETTINGS', 'accessibility': 'android.settings.ACCESSIBILITY_SETTINGS',
                   'date': 'android.settings.DATE_SETTINGS', 'tv-settings': 'android.settings.SETTINGS'}
        if action == 'auto-time':
            self.shell('settings', 'put', 'global', 'auto_time', '1')
            log('Đã bật ngày giờ tự động. TV cần truy cập được máy chủ thời gian để đồng bộ.')
        elif action in intents:
            self.shell('am', 'start', '-a', intents[action])
        elif action == 'home':
            self.shell('input', 'keyevent', '3')
        elif action == 'launcher-settings':
            self.shell('am', 'start', '-n', PKG + '/.SettingsActivity')
        else:
            raise StudioError('Thao tác không hợp lệ.')
        self.device = self.inspect()
        return {'device': self.device}

    def start_job(self, action, payload):
        handlers = {'prepare': self.prepare, 'connect': self.connect, 'pair': self.pair, 'compat': self.compat,
                    'backup': self.backup, 'install': self.install, 'apply': self.apply, 'restore': self.restore,
                    'apps': self.apps, 'stop-app': self.stop_app, 'screenshot': self.screenshot, 'quick': self.quick}
        if action not in handlers:
            raise StudioError('Không có thao tác này.')
        if not self.operation_lock.acquire(blocking=False):
            raise StudioError('Đang có thao tác khác. Chờ hoàn tất trước.')
        job_id = uuid.uuid4().hex
        job = {'id': job_id, 'action': action, 'status': 'running', 'logs': [], 'result': None}
        self.jobs[job_id] = job
        def log(message):
            job['logs'].append({'time': datetime.now().strftime('%H:%M:%S'), 'text': message})
        def run():
            try:
                job['result'] = handlers[action](payload, log)
                job['status'] = 'done'
                log('Hoàn tất.')
            except Exception as e:
                job['status'] = 'error'
                job['error'] = str(e)
                log('Dừng: ' + str(e))
            finally:
                self.operation_lock.release()
        threading.Thread(target=run, daemon=True).start()
        return job_id


def make_handler(studio, token):
    class Handler(BaseHTTPRequestHandler):
        server_version = 'BBILegendTV/1.3.1'
        def log_message(self, *_args):
            pass
        def valid_host(self):
            return self.headers.get('Host') in {f'127.0.0.1:{self.server.server_port}', f'localhost:{self.server.server_port}'}
        def authorized(self):
            return self.valid_host() and secrets.compare_digest(self.headers.get('X-Studio-Token', ''), token)
        def send(self, body, content_type='application/json; charset=utf-8', status=200):
            if not isinstance(body, bytes):
                body = json.dumps(body, ensure_ascii=False).encode('utf-8')
            self.send_response(status)
            self.send_header('Content-Type', content_type)
            self.send_header('Content-Length', str(len(body)))
            self.send_header('Cache-Control', 'no-store' if '/api/' in self.path else 'no-cache')
            self.send_header('X-Content-Type-Options', 'nosniff')
            self.send_header('Referrer-Policy', 'no-referrer')
            self.send_header('Content-Security-Policy', "default-src 'self'; img-src 'self' blob: data:; style-src 'self'; script-src 'self'; connect-src 'self' https://api.github.com; frame-ancestors 'none'; base-uri 'none'")
            self.end_headers()
            self.wfile.write(body)
        def do_GET(self):
            if not self.valid_host():
                return self.send({'error': 'Host không hợp lệ.'}, status=403)
            path = unquote(urlsplit(self.path).path)
            if path.startswith('/api/'):
                if not self.authorized():
                    return self.send({'error': 'Phiên không hợp lệ. Tải lại trang.'}, status=403)
                if path == '/api/state':
                    return self.send({'os': platform.system(), 'adb': bool(studio.adb_path(False)),
                                      'device': studio.device, 'config': studio.config,
                                      'backups': studio.list_backups(), 'busy': studio.operation_lock.locked()})
                if path.startswith('/api/jobs/'):
                    job = studio.jobs.get(path.rsplit('/', 1)[1])
                    return self.send(job or {'error': 'Không tìm thấy tác vụ.'}, status=200 if job else 404)
                if path == '/api/screenshot.png' and (studio.data / 'screen.png').is_file():
                    return self.send((studio.data / 'screen.png').read_bytes(), 'image/png')
                return self.send({'error': 'Không tìm thấy.'}, status=404)
            public = ROOT / 'public'
            file = (public / (path.lstrip('/') or 'index.html')).resolve()
            if not file.is_relative_to(public) or not file.is_file():
                return self.send({'error': 'Không tìm thấy trang.'}, status=404)
            body = file.read_bytes()
            if file.name == 'index.html':
                body = body.replace(b'__STUDIO_TOKEN__', token.encode())
            return self.send(body, mimetypes.guess_type(file)[0] or 'application/octet-stream')
        def do_POST(self):
            self.connection.settimeout(10)
            try:
                length = int(self.headers.get('Content-Length', '0'))
            except ValueError:
                return self.send({'error': 'Độ dài yêu cầu không hợp lệ.'}, status=400)
            if not 0 < length <= 65536:
                return self.send({'error': 'Dữ liệu yêu cầu quá lớn hoặc rỗng.'}, status=413)
            # Drain a bounded body before returning errors, avoiding TCP resets on Windows.
            raw_body = self.rfile.read(length)
            origin = self.headers.get('Origin')
            allowed = {f'http://127.0.0.1:{self.server.server_port}', f'http://localhost:{self.server.server_port}'}
            if not self.authorized() or origin not in allowed:
                return self.send({'error': 'Yêu cầu phải đến từ giao diện Studio trên máy này.'}, status=403)
            if self.headers.get('Content-Type', '').split(';')[0] != 'application/json':
                return self.send({'error': 'Cần JSON.'}, status=415)
            try:
                payload = json.loads(raw_body)
                if not isinstance(payload, dict):
                    raise StudioError('Dữ liệu không hợp lệ.')
                path = urlsplit(self.path).path
                if not path.startswith('/api/actions/'):
                    return self.send({'error': 'Không tìm thấy.'}, status=404)
                job = studio.start_job(path.rsplit('/', 1)[1], payload)
                return self.send({'job': job}, status=202)
            except (StudioError, ValueError) as e:
                return self.send({'error': str(e)}, status=400)
    return Handler


def main():
    parser = argparse.ArgumentParser(description='BBI Legend TV — local, portable web app')
    parser.add_argument('--port', type=int, default=8765)
    parser.add_argument('--adb', help='Optional path to ADB')
    parser.add_argument('--data-dir', type=Path)
    parser.add_argument('--no-browser', action='store_true')
    args = parser.parse_args()
    studio = Studio(args.data_dir, args.adb)
    server = ThreadingHTTPServer(('127.0.0.1', args.port), make_handler(studio, secrets.token_urlsafe(32)))
    url = f'http://127.0.0.1:{server.server_port}'
    print('BBI Legend TV: ' + url, flush=True)
    print('Giữ cửa sổ này mở. Ctrl+C để dừng. Không cần Java/Android Studio.', flush=True)
    if not args.no_browser:
        threading.Timer(0.7, lambda: webbrowser.open(url)).start()
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()


if __name__ == '__main__':
    main()
