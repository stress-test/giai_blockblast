# BB Solver — Liquid Glass edition 1.1

## Những thay đổi chính
- Sửa căn chỉnh 4 điểm: dùng tọa độ nội bộ của lớp phủ thay vì `rawX/rawY`.
- Chuẩn hóa tọa độ theo kích thước view và ảnh chụp; overlay ánh xạ riêng trục X/Y.
- Không dùng lại frame cũ khi ImageReader chưa có ảnh mới.
- Làm mới màn hình chính theo phong cách Liquid Glass.
- Làm lại bảng hướng dẫn nước đi: nền kính, viền sáng, số bước rõ ràng và thanh công cụ nổi gọn hơn.
- Giữ lại chức năng sửa nhận diện, tự dò và căn chỉnh thủ công.

## Build
1. Mở thư mục dự án bằng Android Studio.
2. Chờ Gradle Sync hoàn tất.
3. Chọn Build → Build Bundle(s) / APK(s) → Build APK(s).
4. Cài APK trên thiết bị Android và cấp quyền theo hướng dẫn trong ứng dụng.

## Lưu ý kiểm thử
Mã nguồn đã được chỉnh sửa và rà soát tĩnh trong môi trường này. Môi trường hiện tại không có Android SDK/Gradle nên chưa thể chạy build Android hoặc kiểm thử trực tiếp trên máy thật. Cần kiểm tra lần cuối trên đúng thiết bị và độ phân giải đang chơi Block Blast.
