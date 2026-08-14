# Schema dữ liệu FaceAccess AI

Ba schema trong thư mục này là thiết kế **mới, độc lập** cho FaceAccess AI —
không sao chép từ `data/inherited_safedrive/`. Chúng dùng lại tên trường quen
thuộc (`ear_left`, `ear_right`, `mar`, `yaw_deg`, `pitch_deg`, `roll_deg`) để
nhất quán thuật ngữ với SafeDrive, nhưng có ngữ nghĩa và ràng buộc khác:

- `gesture_event.schema.json` — một bản ghi mỗi khung hình/sự kiện do
  `EventLogger` trong app ghi ra (JSON Lines), không chứa ảnh hay đặc trưng
  sinh trắc có thể định danh khuôn mặt.
- `calibration_profile.schema.json` — ngưỡng hiệu chỉnh riêng theo người
  dùng/thiết bị, sinh từ màn hình hiệu chỉnh, lưu cục bộ.
- `session_summary.schema.json` — số liệu tổng hợp một phiên (macro-F1, tỷ lệ
  hoàn thành lệnh, kích hoạt nhầm/phút, latency p50/p95) tính từ chuỗi
  `gesture_event`, phục vụ đúng tiêu chí nghiệm thu MVP trong
  `../../docs/00_Y_TUONG_VA_PHAM_VI.md`.

## Khác biệt quan trọng với schema SafeDrive

- `yaw_deg` ở đây suy ra từ `facialTransformationMatrixes` của MediaPipe Face
  Landmarker (Tasks Vision). Trường `yaw` trong
  `../inherited_safedrive/sample_driver_events.csv` là góc minh hoạ theo mô tả
  cũ của SafeDrive — **không được coi hai đại lượng là một** (đúng cảnh báo
  trong `../inherited_safedrive/README.md`).
- `gesture_type` ở đây là cử chỉ điều khiển (head_turn_left/right,
  eye_close_hold, eye_close_long), khác hoàn toàn `label`/`alert_type` cảnh
  báo buồn ngủ khi lái xe của SafeDrive.
- Mọi trường liên quan tới người dùng chỉ chấp nhận `participant_code` ẩn
  danh; không có trường tên, email hay định danh trực tiếp.

## Dùng khi báo cáo

Mỗi file trong `../../reports/` build từ `gesture_event` phải trỏ được tới
`source_events_path` và `computed_by_script` cụ thể (đã bắt buộc trong
`session_summary.schema.json`), đúng nguyên tắc "mỗi chỉ số công bố phải trỏ
được tới dữ liệu đầu vào, script tính và phiên bản mã" trong `../../README.md`.
