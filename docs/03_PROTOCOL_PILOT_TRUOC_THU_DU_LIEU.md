# Protocol pilot trước khi thu dữ liệu nghiên cứu

Tài liệu này dùng cho kiểm chứng kỹ thuật nội bộ. Kết quả pilot không được
diễn giải là hiệu quả trên người hạn chế vận động chi trên.

## 1. Điều kiện bắt đầu pilot

- Bản build và model được ghi bằng commit/hash cụ thể.
- Emergency Stop không phát hành động Select trước khi dừng.
- Không nhận lệnh khi mất khuôn mặt; sau khi thấy lại phải giữ tư thế trung
  tính ít nhất 1 giây.
- Profile hiệu chỉnh hợp lệ là điều kiện bắt buộc để khởi động service.
- Log không chứa ảnh, video, tên, email hoặc định danh trực tiếp.
- Người thử nghe được hướng dẫn TTS, tên mục đang chọn và nhận rung xác nhận mà
  không cần nhìn overlay.
- Đồng hồ thiết bị, mức pin và độ sáng màn hình được ghi trước mỗi phiên.

## 2. Ma trận thiết bị và điều kiện

Pilot tối thiểu trên hai điện thoại Android của hai mức cấu hình. Với mỗi máy,
chạy các điều kiện:

1. Ánh sáng trong nhà ổn định.
2. Ánh sáng yếu nhưng vẫn nhìn rõ mặt.
3. Điện thoại đặt chính diện.
4. Điện thoại lệch nhẹ sang trái/phải.
5. Người thử đeo kính nếu bình thường có sử dụng.

Không thay đổi ngưỡng giữa các tác vụ trong cùng một phiên. Nếu phải hiệu chỉnh
lại, kết thúc phiên cũ và tạo session mới.

## 3. Tác vụ pilot

Sau một phút trung tính không ra lệnh, thực hiện mỗi tác vụ 10 lần:

- Quay trái để chuyển về mục trước.
- Quay phải để chuyển tới mục sau.
- Nhắm mắt ngắn rồi mở để chọn.
- Nhắm mắt dài để dừng khẩn cấp.
- Mất khuôn mặt giữa một cử chỉ đang thực hiện.
- Nghiêng đầu nhưng không quay để kiểm tra gate roll.

Thực hiện thêm một luồng end-to-end: mở ứng dụng đã ghim, phát/tạm dừng media,
Back, Home và dừng service mà không chạm màn hình (ngoại trừ bước khởi tạo).

## 4. Quy tắc gán nhãn

Người quan sát ghi trước cho mỗi trial:

- `trial_id` duy nhất;
- `session_id` tương ứng;
- cử chỉ/tác vụ được yêu cầu;
- thời điểm bắt đầu và kết thúc theo elapsed time của phiên;
- kết quả mong đợi;
- ghi chú lỗi protocol nếu người thử làm sai hướng dẫn.

Một dự đoán chỉ được ghép với một trial. Sự kiện ngoài mọi cửa sổ trial được
tính là kích hoạt nhầm. Trial vi phạm protocol phải được đánh dấu riêng, không
âm thầm xóa sau khi xem kết quả.

## 5. Chỉ số phải xuất

- Precision, recall và F1 cho từng cử chỉ; macro-F1 chỉ là chỉ số phụ.
- Kích hoạt nhầm trong một phút trung tính và trên tổng thời gian phiên.
- Tỷ lệ hoàn thành, thời gian hoàn thành và số lệnh nhầm cho từng tác vụ.
- Latency p50/p95 từ timestamp frame đến lúc dispatch hành động.
- FPS trung bình/p10, RAM đỉnh, pin tiêu thụ và nhiệt độ nếu thiết bị cung cấp.
- Kết quả tách theo người thử, thiết bị và điều kiện; không chỉ báo trung bình
  gộp.

## 6. Tiêu chí qua vòng pilot kỹ thuật

- Không có trường hợp Emergency Stop thực thi Select trước.
- Không có hành động nào trong thời gian mất mặt hoặc thời gian phục hồi.
- Hoàn thành luồng end-to-end trên ít nhất hai thiết bị.
- Log của mọi sự kiện cử chỉ có `latency_ms`; có `fps` sau cửa sổ khởi động đầu
  tiên.
- Không crash/ANR trong phiên liên tục 30 phút.
- Có thể lần ngược mọi bảng kết quả tới file JSONL và phiên bản build.

Các ngưỡng hiệu năng/chính xác để tuyên bố đạt phải được chốt trước nghiên cứu
người dùng, sau pilot kỹ thuật và trước khi xem dữ liệu đánh giá chính thức.

## 7. Cổng chuyển sang nghiên cứu người dùng mục tiêu

Chỉ tuyển người dùng mục tiêu sau khi có protocol nghiên cứu được duyệt, biểu
mẫu đồng thuận, quy trình dừng an toàn, chính sách lưu/xóa dữ liệu và người phụ
trách xử lý sự cố. Pilot với người không khuyết tật chỉ chứng minh hệ thống đủ
ổn định để bắt đầu đánh giá, không chứng minh lợi ích trợ năng.
