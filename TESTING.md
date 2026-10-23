# Verification

- Backend: 20 automated tests pass on Windows/Python 3.14. They cover LAN-address validation, malformed settings, service protection, preserving other accessibility services, refusing incomplete split-APK backups, refusing cross-device/tampered restores, job serialization, CRLF getprop output, Xiaomi screenshot-driver prefixes, session token, Host/Origin, CSP and path traversal.
- JavaScript syntax checked with Node. Python modules compile. Android APK compiled and signature verified before packaging.
- Live Xiaomi MiTV-MOOR2 / Android 11: connect/read compatibility, install with a pre-install backup, export current settings, switch to Vista, back up APK + managed settings, switch to macOS, restore Vista from backup, then return TV to macOS. Existing sounds and triple-Home settings retained.
- Browser QA uses the actual localhost interface: connection status, compatibility, theme gallery, illustrated instructions, settings controls and running-app view. The responsive narrow layout is checked in the app browser.
- Linux/macOS startup files and standard-library backend are provided, but these operating systems were not physically exercised. Wireless pairing and first-time Platform Tools downloads on those systems need real-device/host verification.
- Snapshots cover the APK and settings exposed by the Studio bridge (theme, sounds, weather, crypto, Home shortcut, city, temperature, language, appearance and shelf). They are not full-device images and do not export credentials or other apps' private data.

Run: `python -m unittest discover -s tests -v`.


## v1.2.0 — thông báo cập nhật

- `node tests/test_updates.cjs`: so sánh phiên bản số, loại prerelease/draft, cố định liên kết về repo chính thức; kiểm tra tự động/thủ công, cache 24 giờ, Để sau, tắt tự kiểm tra và lỗi mạng.
- 20 kiểm tra backend tiếp tục đạt.
- Trình duyệt tại localhost: kiểm tra thủ công gọi GitHub Releases thành công, hiển thị trạng thái phiên bản hiện tại và công tắc tự kiểm tra. CSP chỉ bổ sung kết nối đến `https://api.github.com`.
- README mới nhất của chủ repo được kéo về trước khi chỉnh. Thông tin trách nhiệm và quyền tài sản có trong README, DISCLAIMER.md và trang Giới thiệu.
- Không thay APK hoặc thao tác TV trong bản cập nhật này. Bản cũ trước v1.2.0 cần nâng cấp thủ công một lần để có tính năng thông báo.

## v1.3.0 — XP Widgets

- Java Android biên dịch với SDK 34 / min API 28; có bước DEX, căn chỉnh APK và xác minh chữ ký. Chứng thư ký phải khớp APK v1.2.0 trước khi thay gói cài.
- `tests/WidgetRulesTest.java`: các ngày Tết 2020/2024/2025/2026, rằm tháng Tám 2021, tháng Hai nhuận 2023; kiểm tra miền giá trị lịch trong 1900–2099; ưu tiên ĐT nam/nữ, V.League, danh sách giải cho phép và chỉ lấy kênh quốc gia Việt Nam.
- Đã kiểm tra trực tiếp HTTPS: giá vàng công bố có metadata nguồn/mốc giờ; DNSE trả chuỗi VNINDEX và FPT; TheSportsDB trả mã V.League 4803, C1 4480, ĐT nam 140161/nữ 144951 và lịch kênh quốc gia Vietnam. Việc có dữ liệu trong một lần kiểm tra không bảo đảm độ phủ hoặc độ ổn định về sau.
- Ảnh SVG XP Widgets là minh họa bố cục, không phải ảnh chụp từ TV. Chưa cài bản này lên TV trong lượt phát triển; cần kiểm tra thực tế remote, overscan và dữ liệu nguồn trên thiết bị đích. Linux/macOS chưa chạy thực tế.
- Theme mới không gỡ hoặc ghi đè cấu hình XP cổ điển; việc cài qua công cụ vẫn dùng cơ chế backup trước thay đổi. Widget cache không thuộc export/restore tùy chọn.
