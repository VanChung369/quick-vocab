LEXIS APP DESIGN — MÃ NGUỒN HTML
================================

Cách mở:
  1. Giải nén file .zip.
  2. Mở file index.html bằng Chrome, Edge hoặc Firefox.
  3. Bấm "Lexis App Design" để xem tất cả màn hình.

Cần có mạng để tải font chữ (Google Fonts) và icon.

Nếu trang hiện trắng, chạy server trong thư mục này rồi mở http://localhost:8000:
  python3 -m http.server

Các file:
  Lexis App Design.dc.html  Trang chính: mọi màn hình app (prototype bấm được), widget, design system
  Lexis Logo.dc.html        Logo và bộ icon
  LexisPhone.dc.html        Component điện thoại (tất cả màn hình app nằm trong file này)
  LexisWidget*.dc.html      Các component widget
  logo/                     Logo SVG và PNG 512px cho Play Store
  support.js, image-slot.js, vendor/   Phần chạy để hiển thị trên trình duyệt, không thuộc thiết kế

Lưu ý: đây là mockup tham khảo. Màu, cỡ chữ, khoảng cách, bo góc... nằm trong
thuộc tính style="..." của từng file .dc.html; khi dựng app thật hãy làm lại theo
các giá trị đó.
