# FaceAccess AI

FaceAccess AI là dự án Android mã nguồn mở hỗ trợ người hạn chế vận động tay
điều khiển điện thoại bằng cử chỉ khuôn mặt. Hệ thống dự kiến dùng camera trước,
MediaPipe Face Landmarker và bộ nhận dạng cử chỉ theo thời gian để chuyển các cử
chỉ có chủ ý thành thao tác chọn, quay lại, Home hoặc điều khiển media.

## Trạng thái

**Giai đoạn:** khởi tạo và kiểm chứng bằng chứng nền. Chưa có MVP và chưa có kết
quả thực nghiệm riêng của FaceAccess AI.

Kết quả SafeDrive trong workspace chỉ là bằng chứng kế thừa để hình thành giả
thuyết. Không được tuyên bố FaceAccess đạt macro-F1 96,84% cho tới khi tìm được
artifact gốc, tái lập phép tính và đánh giá lại đúng bài toán cử chỉ chủ ý.

## Mục tiêu MVP

1. Nhận dạng tối thiểu ba cử chỉ có chủ ý: quay đầu trái/phải và nhắm mắt giữ.
2. Điều khiển một launcher hỗ trợ bằng cử chỉ mà không chạm màn hình.
3. Thực hiện an toàn các thao tác Back, Home, chọn và phát/tạm dừng media.
4. Xử lý hình ảnh cục bộ; không lưu hoặc gửi ảnh khuôn mặt.
5. Đo macro-F1, tỷ lệ hoàn thành lệnh, kích hoạt nhầm/phút và latency p95.

## Cấu trúc ban đầu

- `docs/` — phạm vi sản phẩm và kiểm toán bằng chứng nền.
- `src/` — mã nguồn mới của FaceAccess AI.
- `data/` — schema và dữ liệu mới có đồng thuận; không sao chép dữ liệu chưa rõ quyền.
- `tests/` — kiểm thử đơn vị, tích hợp và kịch bản nghiệm thu.
- `reports/` — kết quả có thể tái lập của riêng FaceAccess AI.

## Nguyên tắc

- Không sửa hoặc đóng gói trực tiếp mã trong các thư mục `from_*` của dự án khác.
- Không gọi sản phẩm là thiết bị y tế hoặc tuyên bố hiệu quả với người khuyết tật
  khi chưa có đánh giá phù hợp với người dùng mục tiêu.
- Mỗi chỉ số công bố phải trỏ được tới dữ liệu đầu vào, script tính và phiên bản mã.
- Mọi thao tác nhạy cảm phải có xác nhận và cơ chế dừng nhanh.

