"""Build a portable archive without device backups, credentials or developer state."""
from pathlib import Path
import hashlib
import json
import zipfile

root = Path(__file__).resolve().parents[1]
output = root.parent / 'BBI-Legend-TV-v1.2.0.zip'
allowed = {'public', 'packages', 'launcher-source', 'tests', 'tools'}
root_files = {'server.py', 'start-windows.cmd', 'start.sh', 'start-macos.command',
              'README.md', 'DISCLAIMER.md', 'LICENSE', 'LICENSE-FreeMyTV.txt', 'TESTING.md'}
files = []
for file in sorted(root.rglob('*')):
    if not file.is_file():
        continue
    rel = file.relative_to(root)
    if '__pycache__' in rel.parts or file.suffix in ('.pyc', '.zip'):
        continue
    if rel.parts[0] not in allowed and rel.as_posix() not in root_files:
        continue
    if file.suffix in ('.keystore', '.pk8', '.pem') or file.name in ('adbkey', 'adbkey.pub', 'config.properties'):
        raise RuntimeError('Refusing to include a private configuration or key: ' + str(rel))
    files.append(file)

with zipfile.ZipFile(output, 'w', zipfile.ZIP_DEFLATED, compresslevel=6) as archive:
    for file in files:
        rel = file.relative_to(root)
        entry = zipfile.ZipInfo('BBI-Legend-TV/' + rel.as_posix())
        entry.create_system = 3
        executable = file.suffix in ('.sh', '.command')
        entry.external_attr = (0o100755 if executable else 0o100644) << 16
        entry.compress_type = zipfile.ZIP_DEFLATED
        payload = file.read_bytes()
        if file.suffix.lower() in ('.cmd', '.bat'):
            payload = payload.replace(b'\r\n', b'\n').replace(b'\n', b'\r\n')
        archive.writestr(entry, payload)
    archive.writestr('BBI-Legend-TV/data/.gitkeep', '')

with zipfile.ZipFile(output) as archive:
    bad = archive.testzip()
    assert bad is None, bad
    assert 'BBI-Legend-TV/packages/FreeMyTV-Studio.apk' in archive.namelist()
    assert not any('/backups/' in name or '/.test-data/' in name for name in archive.namelist())
digest = hashlib.sha256(output.read_bytes()).hexdigest()
(output.with_suffix('.sha256')).write_text(digest + '  ' + output.name + '\n', encoding='utf-8')
print(json.dumps({'file': str(output), 'files': len(files), 'bytes': output.stat().st_size,
                  'sha256': digest}, ensure_ascii=False, indent=2))
