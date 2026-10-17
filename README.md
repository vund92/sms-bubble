# SMS Bubble

App Android tạo và xem đoạn hội thoại tin nhắn dạng bong bóng chat. Dữ liệu chỉ lưu bên trong app, không gửi SMS thật và không ghi vào hộp thư tin nhắn của máy.

## Tính năng

- **Hai giao diện**, đổi bất cứ lúc nào qua biểu tượng bảng màu hoặc menu `⋯`:
  - *iPhone*: bong bóng xanh/xám có đuôi cong, header avatar ở giữa, tiêu đề lớn "Tin nhắn".
  - *Android*: Material 3, màu theo hình nền máy (Android 12+), bong bóng bo góc kiểu Google Messages, giờ dưới mỗi nhóm tin.
- **Người gửi / người nhận**: chọn "Tôi gửi" (bên phải) hoặc "<Tên> gửi" (bên trái) trước khi gửi từng tin.
- **Ngày giờ của tin**: nhấn chip thời gian để
  - gõ tay `dd/MM/yyyy HH:mm` (cũng nhận `17-10-2026 16:24`, `17.10.2026 16:24`, `16:24 17/10/2026`, hoặc chỉ ngày), ngày không hợp lệ như 31/02 sẽ báo lỗi;
  - hoặc bấm *Chọn ngày* / *Chọn giờ* để dùng lịch và đồng hồ.
  - Khi đã đặt giờ tự chọn, mỗi tin gửi tiếp theo tự cộng thêm 1 phút để giữ đúng thứ tự.
- **Danh bạ**: *Chọn từ danh bạ* (không cần cấp quyền), hoặc nhập tay số rồi bấm *Lấy tên trong danh bạ theo số này* (xin quyền đọc danh bạ). Nếu để trống tên, app tự hiển thị tên trong danh bạ khớp với số.
- **Nhãn thời gian** giữa khung chat: "Hôm nay 16:24", "Hôm qua …", "Thứ Hai …", "12/03/2025 lúc …" — tự chèn khi sang ngày mới hoặc cách nhau hơn 1 giờ.
- **Nhấn giữ bong bóng** để sửa nội dung, đổi người gửi, đổi ngày giờ hoặc xoá. Nhấn giữ cuộc trò chuyện ở danh sách để xoá.
- Hỗ trợ chế độ tối.

## Build

Yêu cầu: Android Studio Ladybug (2024.2) trở lên, JDK 17.

1. Giải nén, mở thư mục `SmsBubble` bằng Android Studio (*File → Open*).
2. Chờ Gradle sync (lần đầu sẽ tải Gradle 8.11.1, AGP 8.7.3, Compose).
3. Cắm điện thoại (bật USB debugging) hoặc mở emulator → bấm **Run ▶**.

Hoặc dòng lệnh: `./gradlew assembleDebug` → APK ở `app/build/outputs/apk/debug/app-debug.apk`.

minSdk 26 (Android 8.0), targetSdk 35.

## Cấu trúc

```
app/src/main/java/com/smsbubble/app/
├─ MainActivity.kt            Điều hướng: danh sách ↔ màn hình chat
├─ ChatViewModel.kt
├─ data/
│  ├─ Models.kt               Conversation, Message, BubbleStyle
│  ├─ ChatRepository.kt       Lưu JSON trong filesDir/chats.json
│  └─ ContactUtils.kt         Chọn liên hệ, tra tên theo số
└─ ui/
   ├─ Theme.kt                Màu iPhone / Android, bảng màu bong bóng
   ├─ Formatting.kt           Định dạng ngày giờ tiếng Việt, đọc ngày gõ tay
   ├─ Components.kt           Avatar, hình bong bóng có đuôi, menu giao diện
   ├─ Dialogs.kt              Chọn/nhập ngày giờ, liên hệ, sửa tin nhắn
   ├─ ConversationListScreen.kt
   └─ ChatScreen.kt           Header, bong bóng, ô soạn tin
```

## Lấy file APK không cần Android Studio (GitHub Actions)

1. Đưa toàn bộ thư mục `SmsBubble` lên một repo GitHub (giữ cả thư mục ẩn `.github`).
2. Vào tab **Actions** → workflow **Build APK** tự chạy (hoặc bấm *Run workflow*). Mất khoảng 3–5 phút.
3. Mở lần chạy đã xong → mục **Artifacts** → tải `SmsBubble-apk` (file zip chứa `app-debug.apk`).
4. Chép APK vào điện thoại, mở để cài (cho phép "Cài ứng dụng không rõ nguồn gốc" nếu máy hỏi).
