# BBI Legend TV

Ứng dụng web chạy trên máy tính để hướng dẫn, kiểm tra, cài giao diện và quản lý Free My TV qua ADB nội bộ. Giao diện tiếng Việt; không phụ thuộc dịch vụ web bên ngoài để vận hành. Đây là công cụ cục bộ, không phải website công khai.

## Mở ứng dụng

1. Giải nén toàn bộ thư mục. Cần **Python 3.10+** ([Python chính thức](https://www.python.org/downloads/)). Không cần cài thư viện Python, Node, Java hoặc Android Studio.
2. **Windows:** mở `start-windows.cmd`.
3. **macOS:** mở `start-macos.command`. Nếu hệ thống chưa cho thực thi, mở Terminal tại thư mục này và chạy `bash start.sh`.
4. **Linux:** mở Terminal tại thư mục và chạy `bash start.sh`.
5. Trình duyệt mở tại **http://127.0.0.1:8765**. Giữ cửa sổ khởi chạy mở trong lúc dùng. Ctrl+C để dừng.

Nếu cổng đã được dùng: `python3 server.py --port 8766` (Windows có thể dùng `py -3`). Nếu muốn tự mở trình duyệt: `python3 server.py --no-browser`.

Windows đã kèm Android Platform Tools chính thức. Trên Linux/macOS, bấm **Chuẩn bị công cụ** để tải đúng gói từ Google (cần Internet cho lần này). Nếu đã có ADB, BBI Legend TV tự dùng bản trên PATH. Có thể chỉ định `--adb /đường/dẫn/adb` hoặc biến môi trường `TVMOD_ADB`. Linux ARM cần ADB tương thích kiến trúc do người dùng cung cấp qua PATH/`TVMOD_ADB`; gói Google Linux dành cho x86_64.

## Quy trình

- **Hướng dẫn bằng hình:** bật Developer options, USB debugging, lấy IP và cho phép máy tính trên TV.
- **Kết nối & kiểm tra:** nhập IP/cổng, hỗ trợ ghép nối mã 6 số. Kiểm tra TV/Google TV, Android 9+ và cổng ADB 5555. Chỉ các IP LAN 10.x, 172.16–31.x, 192.168.x được chấp nhận.
- **Giao diện:** Free My TV mặc định, Windows XP, Vista hoặc macOS. Cả bốn trong một APK đã ký; không cần build trên máy khác.
- **Cài đặt:** âm thanh điều hướng, Home 3 lần, thời tiết/crypto trên theme mặc định, thành phố/nhiệt độ. Thao tác nhanh mở cài đặt mạng/Trợ năng, giờ tự động, chụp màn hình và xem tiến trình.
- **Sao lưu:** tự động trước khi cài, đổi theme, lưu cài đặt và khôi phục. Có nút sao lưu thủ công.

Lần đầu cài trên TV: chọn **Free My TV → Home Screen → Free My TV** và chấp nhận gỡ lỗi. BBI Legend TV không tự chuyển chế độ hệ thống từ máy tính. Muốn quay về giao diện gốc của TV, chọn **Home Screen → Original** trước khi gỡ Free My TV.

**Home 3 lần:** cần USB debugging/ADB 5555 và dịch vụ Trợ năng “Free My TV — Đóng ứng dụng”. Khi người dùng bật công tắc và xác nhận Lưu/Cài, BBI Legend TV bật dịch vụ này nhưng giữ các dịch vụ Trợ năng khác. Dịch vụ chỉ nhận tên gói của cửa sổ; không đọc nội dung, chụp ảnh hoặc lưu mật khẩu. Buộc dừng làm app ngừng phát/tác vụ nền đến khi mở lại. Google Play Services, launcher, bàn phím và phần hệ thống được bảo vệ.

Các theme là giao diện lấy cảm hứng; không cài Windows hay macOS lên TV. macOS lấy phong cách macOS 27/Liquid Glass tại thời điểm làm. Theme mặc định không có gợi ý phim nếu chưa build với khóa TMDB riêng; công cụ không yêu cầu khóa trong giao diện hay thu thập tài khoản streaming.

## Backup và mang sang máy khác

Giữ toàn bộ thư mục **data/backups** khi di chuyển công cụ. Backup chứa APK và các tùy chọn do BBI Legend TV quản lý khi bản cài có cầu nối: theme, âm thanh, thời tiết/crypto, Home 3 lần, thành phố/nhiệt độ, ngôn ngữ, sáng/tối và danh sách app trên shelf. Không snapshot các ánh xạ nút remote riêng hay toàn bộ dữ liệu riêng của launcher. Bản cũ không xuất được dữ liệu riêng: chỉ lưu APK và giữ cài đặt hiện có bằng cách cài đè. Bản sao không bao gồm firmware, tài khoản, dữ liệu các app khác hay Wi-Fi. Chỉ cho phép khôi phục đúng TV đã tạo backup, kiểm tra mã SHA-256 và tự backup trạng thái hiện tại trước khi restore.

Không xóa dữ liệu hay gỡ Free My TV trước khi restore. Nếu báo khác chữ ký, BBI Legend TV dừng; không tự gỡ bản đang có. Các backup XP/Vista trước dự án này vẫn nằm nguyên trong `free-my-tv-setup/backups` của máy đã tạo chúng; không trộn với backup mới.

## Phạm vi hỗ trợ và kiểm tra

Đã có TV tham chiếu Xiaomi MiTV-MOOR2 / Android 11. Máy tính Windows được kiểm tra trong phiên phát triển; Linux/macOS có launcher và backend dùng thư viện chuẩn, chưa được chạy thực tế trên từng hệ điều hành. Kiểm tra kỹ thuật thành công không đảm bảo mọi firmware/remote tương thích. ADB ghép nối cổng động cần chuyển sang cổng 5555 để tính năng tự kết nối của Free My TV hoạt động. TV chỉ có webOS/Tizen không thuộc bản công cụ này.

Chạy kiểm tra backend: `python -m unittest discover -s tests -v`.

## Bảo mật và dữ liệu

Máy chủ chỉ lắng nghe 127.0.0.1. API có token phiên, kiểm tra Host/Origin, không bật CORS, không nhận lệnh shell tùy ý. Mã ghép nối chỉ ở bộ nhớ trong thời gian ghép nối; không ghi vào nhật ký. Ảnh màn hình và backup được lưu cục bộ. Không đóng gói khóa ký APK, khóa ADB hoặc tài khoản của máy đã phát triển.

Nguồn launcher Android trong `launcher-source/`, giấy phép MIT ở `LICENSE-FreeMyTV.txt`. Android Platform Tools có giấy phép kèm theo trong gói Google. Font Inter có OFL trong `public/assets/OFL.txt`.

## Giới thiệu và nhật ký cập nhật

Trang Giới thiệu sử dụng thiết kế nhận diện BBI MEDIA, logo, liên hệ, cộng đồng Tôi là Vibe’r và thông tin ủng hộ do người dùng cung cấp. Tính năng và lời cảm ơn dành riêng cho BBI Legend TV.

Trang Nhật ký cập nhật chỉ ghi các phiên bản BBI Legend TV, kèm tìm kiếm. Thông tin BBI MEDIA và donate lưu tại `public/assets/bbi-content.json`.
