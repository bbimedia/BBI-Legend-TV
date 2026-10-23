# Verification

## v1.3.6 — Thumbnail và skin thanh menu

- Tạo lại đủ bảy SVG minh họa từ công cụ Python chuẩn: nền, app, Dock/Sidebar/widget và thanh menu theo từng phong cách; không có SSID/số liệu riêng. Kiểm tra XML/viewBox của bảy ảnh, xem thực tế trong trình duyệt cả phần trên và phần dưới kho giao diện.
- Số lượng kho tự tính từ danh sách theme (07), cả Tổng quan và Kho giao diện dùng ảnh mới có mã cache 1.3.6. Cú pháp JavaScript và bộ kiểm thử cập nhật Node qua.
- Bao gồm skin thanh menu native đã biên dịch và xem trên TV trong lượt sửa trước; đóng lại APK với metadata 1.3.6, cùng bytecode đã kiểm tra và chữ ký được xác minh. Không thêm quyền hoặc thay đổi chức năng thống kê mạng.

## Local sau v1.3.5 — Thanh trên theo theme

- Đổi màu gradient, viền, bo góc, kiểu chữ, độ trong và trạng thái focus cho XP/Vista/macOS, chọn cùng skin cho bản cổ điển/widget. Vista/macOS có lề và thanh kính bo góc, XP giữ thanh xanh sát cạnh.
- Biên dịch APK và xác minh chữ ký; sao lưu trước cài. Đã xem ảnh thật của cả ba bản widget trên Xiaomi Android 11: chữ và nền đúng tương phản, không chồng lên widget; TV đang ở macOS Widgets. Không thay đổi hành vi mạng/nguồn/Video.
- Cập nhật sáu SVG minh họa trong web theo skin mới. Chỉ lưu local và cài trên TV; chưa commit/push hoặc cập nhật file ZIP đã phát hành.

## v1.3.5 — Màn hình chính gọn và truy cập nhanh

- Biên dịch/xác minh chữ ký APK; 21 kiểm thử backend, Node update tests và Java AppMetricsTest đều qua. Thêm kiểm tra tốc độ theo thời gian trôi thực tế, bộ đếm không hỗ trợ/reset và khoảng thời gian không hợp lệ.
- Cài trên Xiaomi MiTV-MOOR2 Android 11, backup trước nâng cấp. Kiểm tra UI thật cho XP/Vista/macOS cổ điển, ba bản widget và Free My TV mặc định: không còn chữ BBI/tên theme/cửa sổ giới thiệu; có nút mạng, đổi nguồn và Video. Xem ảnh thực Vista cổ điển và macOS Widgets.
- Xác nhận tên Wi-Fi đang dùng và up/down cập nhật từ bộ đếm toàn TV. Không chạy bài đo băng thông hoặc suy ra tốc độ gói mạng. Nhấn Mạng mở NetworkActivity; Video mở partnercustomizer.picture.PictureActivity. Chỉ mở trang, không thay đổi kết nối/thông số ảnh.
- Nút đổi nguồn hiển thị TV/Cáp/Ăng-ten, AV và HDMI 1–3 do TV cung cấp. Không chuyển sang nguồn đang trống khi kiểm tra. Firmware khác có thể trả loại đầu vào/trang cài đặt khác.
- Ảnh public dùng SVG minh họa, không đưa tên Wi-Fi/ảnh riêng lên repo. Backup, cấu hình và khóa ký không nằm trong gói portable.

## v1.3.4 local — Danh sách ứng dụng và lưu lượng mạng

- Biên dịch và xác minh chữ ký APK; 21 kiểm thử backend qua (bao gồm cả bảy mã theme); AppMetricsTest qua với nhóm process, nhiều process, PSS table/summary, từ chối quyền/không có dữ liệu và đơn vị byte. Kiểm tra cú pháp JS và bộ kiểm thử cập nhật Node qua.
- Cài thực tế trên Xiaomi MiTV-MOOR2 Android 11, backup trước nâng cấp. Mở bằng phím Menu trên cả XP/Vista/macOS cổ điển và XP/Vista/macOS Widgets: đều vào AppCatalogActivity có icon/tên, trạng thái, RAM và lịch sử mạng; bộ lọc đầu vào không dùng số RAM/mạng của ứng dụng. Kiểm tra thêm mở thẳng HDMI Inputs từ Vista vào bộ lọc đầu vào ngay lúc tạo activity.
- Bật GET_USAGE_STATS bằng nút trên TV. NetworkStatsManager trả số liệu Wi-Fi tháng hiện tại theo UID: tổng/tải xuống/tải lên; ảnh chụp có số liệu thực và mốc thời gian, không có giá trị mẫu. Các app không có bản ghi được ghi rõ; UI chú thích số liệu UID dùng chung và không bao gồm LAN/di động.
- RAM đọc dumpsys meminfo từng PID qua ADB nội bộ; ps cung cấp snapshot tiến trình. Đã kiểm tra trạng thái có tiến trình và không có tiến trình; chữ trạng thái không tràn cột. Mạng và RAM có thể trễ/thay đổi; chỉ kiểm tra đầy đủ trên model TV này.
- Free My TV mặc định cũng dùng trình duyệt app chung qua All Apps (đã nối route và biên dịch, chưa thử trực tiếp lần này). Không thử mở app trả phí, xóa data, đóng app hoặc giao dịch. Giữ TV ở AppCatalog của macOS Widgets để xem thử. Chỉ lưu local; không commit/push/release GitHub.

## v1.3.3 local — Vista và macOS Widgets

- Dùng chung WidgetHomeView cho XP Widgets/Vista/macOS để giữ thống nhất lịch, bộ lọc bóng đá, nguồn giá và cache hiện có. Biên dịch APK và xác minh chữ ký thành công; JavaScript syntax và bộ kiểm thử thông báo cập nhật Node qua.
- Đã backup và cài trên Xiaomi MiTV-MOOR2 Android 11: Vista hiển thị đủ sáu widget; chuyển lịch sang tháng sau, bật lịch bóng đá 7 ngày và chọn NHA cho ra danh sách trận/kênh FPT Play từ dữ liệu nguồn. Lưu ý ghi nguồn nằm cuối danh sách và cần cuộn khi có nhiều trận.
- macOS: ảnh chụp xác nhận thẻ bo góc, thanh menu, Dock và đủ sáu widget. Thử VNINDEX sang VN30, lịch tháng trước, Back mở danh sách ứng dụng/đầu vào, nút Settings mở cài đặt có nền macOS, donate và credit. Các kiểm tra tương tác dùng ADB input trên TV; đã xác nhận D-pad chuyển focus xuống nút mã chứng khoán, lên lịch và OK mở lịch. Chưa đánh giá đầy đủ mọi hướng D-pad hay chất lượng âm thanh nghe thực tế.
- Xác nhận bridge vẫn giữ sounds=true, weather=true, quick_close=true và thành phố Hồ Chí Minh; không thực hiện giao dịch, không thử đóng app bằng Home 3 lần trong lượt này. Giữ TV ở macOS để xem thử. Thay đổi và gói portable chỉ lưu local, chưa commit/push hoặc tạo GitHub Release.

## v1.3.2 — Cài đặt theo theme, donate và credit

- Biên dịch và xác minh chữ ký APK; 21 kiểm thử Python và bộ kiểm thử thông báo cập nhật Node đều qua.
- Cài trên Xiaomi MiTV-MOOR2 / Android 11, tạo backup trước nâng cấp và giữ cấu hình hiện có.
- Kiểm tra ảnh chụp thật ở theme XP Widgets: nền xanh/đồi đồng bộ, donate ở cột trái, credit và hai dòng miễn trừ không chồng lên menu. Chưa thử quét QR thanh toán; không thực hiện giao dịch.
- Dùng chung bộ vẽ nền cho XP, XP Widgets, Vista và macOS; mặc định giữ nền Scene. Các theme còn lại đã biên dịch, chưa kiểm tra ảnh trên TV trong lượt này.

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

## v1.3.1 — Bongda24h & cài đặt BBI Legend TV

- Cài thử thực tế trên Xiaomi MiTV-MOOR2 / Android 11 tại IP LAN do chủ thiết bị cung cấp. Backup trước khi nâng cấp; giữ theme, thành phố, âm thanh, Home 3 lần và shelf ứng dụng.
- Xác nhận hình thật: đồng hồ, lịch âm/dương, thời tiết Hồ Chí Minh, giá DOJI/SJC và biểu đồ VNINDEX tải dữ liệu. D-pad phải/OK mở lịch; chuyển tháng hoạt động. Đã đối chiếu lịch V.League và tên kênh với nguồn Bongda24h.
- `BongDa24hParserTest.java`: giờ Việt Nam không cộng hai lần, chọn giải và nữ Việt Nam, kênh có/không có, HTML entities, lọc ngày, chuyển năm, báo lỗi khi cấu trúc không còn nhận diện. Kiểm tra trên HTML tải thực tế ngoài repo.
- Đổi hộp chi tiết XP sang theme sáng để tương phản chữ/nền đúng trên TV. Tên/logo BBI và dòng nguồn cuối màn hình đã kiểm tra qua ảnh chụp thiết bị.
- Không gỡ launcher gốc hoặc xóa dữ liệu app. Không dùng thao tác chuyển Home Screen sang Original khi thử.

- Kiểm tra thêm trên TV: NHA 7 ngày hiển thị Ipswich–Nottingham, Aston Villa–Man City, Arsenal–Everton… cùng tên kênh FPT Play từ nguồn. Bộ lọc C1 hiển thị trạng thái chưa có trận trong phần lịch tuần nhận được; không tự tạo trận. Sửa vùng chẩn đoán trong cài đặt để không đè lên dòng Home 3 lần và footer nguồn.
