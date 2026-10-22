# Verification

- Backend: 20 automated tests pass on Windows/Python 3.14. They cover LAN-address validation, malformed settings, service protection, preserving other accessibility services, refusing incomplete split-APK backups, refusing cross-device/tampered restores, job serialization, CRLF getprop output, Xiaomi screenshot-driver prefixes, session token, Host/Origin, CSP and path traversal.
- JavaScript syntax checked with Node. Python modules compile. Android APK compiled and signature verified before packaging.
- Live Xiaomi MiTV-MOOR2 / Android 11: connect/read compatibility, install with a pre-install backup, export current settings, switch to Vista, back up APK + managed settings, switch to macOS, restore Vista from backup, then return TV to macOS. Existing sounds and triple-Home settings retained.
- Browser QA uses the actual localhost interface: connection status, compatibility, theme gallery, illustrated instructions, settings controls and running-app view. The responsive narrow layout is checked in the app browser.
- Linux/macOS startup files and standard-library backend are provided, but these operating systems were not physically exercised. Wireless pairing and first-time Platform Tools downloads on those systems need real-device/host verification.
- Snapshots cover the APK and settings exposed by the Studio bridge (theme, sounds, weather, crypto, Home shortcut, city, temperature, language, appearance and shelf). They are not full-device images and do not export credentials or other apps' private data.

Run: `python -m unittest discover -s tests -v`.

