# Ý tưởng và phạm vi FaceAccess AI

## Bài toán thực tiễn

Người bị hạn chế vận động tay có thể gặp khó khăn khi chạm chính xác vào màn hình
điện thoại. FaceAccess AI hướng tới một phương thức tương tác bổ sung bằng cử chỉ
khuôn mặt, hoạt động cục bộ và không yêu cầu thiết bị chuyên dụng.

## Người dùng mục tiêu

- Người bị hạn chế vận động chi trên, tạm thời hoặc lâu dài.
- Người hỗ trợ, người chăm sóc và nhóm phát triển công nghệ trợ năng.

Việc thiết kế cho người dùng mục tiêu phải có tham vấn và đồng thuận phù hợp.
Thử nghiệm ban đầu với người không khuyết tật chỉ chứng minh tính khả thi kỹ thuật.

## Luồng sản phẩm MVP

```text
Camera trước
  -> Face Landmarker
  -> chuỗi EAR/MAR/head index
  -> nhận dạng cử chỉ + cổng chống kích hoạt nhầm
  -> accessibility focus / thao tác hệ thống
  -> phản hồi hình ảnh, âm thanh và nhật ký không chứa ảnh mặt
```

## Trong phạm vi MVP

- Hiệu chỉnh ngưỡng nhanh theo người dùng.
- Quay đầu trái/phải để di chuyển lựa chọn.
- Nhắm mắt giữ để xác nhận.
- Launcher có các nút lớn và trạng thái nhận dạng rõ ràng.
- Back, Home và điều khiển phát/tạm dừng media.
- Nút hoặc cử chỉ dừng khẩn cấp.
- Đánh giá độ chính xác và kích hoạt nhầm.

## Ngoài phạm vi MVP

- Chẩn đoán bệnh hoặc mức độ khuyết tật.
- Điều khiển giao dịch tài chính hoặc nhập mật khẩu.
- Thu thập âm thầm, nhận dạng danh tính hay lưu video khuôn mặt.
- Tuyên bố thay thế hoàn toàn TalkBack, Switch Access hoặc thiết bị trợ năng y tế.

## Tiêu chí nghiệm thu tối thiểu

1. Chạy được trên ít nhất hai điện thoại Android.
2. Hoàn thành một luồng mở ứng dụng và điều khiển media hoàn toàn không chạm.
3. Báo macro-F1 theo người tham gia hoặc chia tập theo người.
4. Báo false activation trên thời gian sử dụng trung tính.
5. Báo latency p50/p95, FPS, RAM và ảnh hưởng pin trong kịch bản xác định.
6. Có model card, data card, giấy phép và hướng dẫn build từ mã nguồn.

