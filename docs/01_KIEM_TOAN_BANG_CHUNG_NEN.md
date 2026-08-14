# Kiểm toán bằng chứng nền

## Kết luận hiện tại

SafeDrive cung cấp một giả thuyết kỹ thuật có triển vọng: EAR, MAR và độ lệch đầu
từ landmark khuôn mặt có thể nhận biết các sự kiện kéo dài. Tuy nhiên, workspace
chưa đủ bằng chứng để coi các số liệu trong bản thảo là kết quả đã tái lập.

## Kết quả được bản thảo SafeDrive báo cáo

- 30 người, 1.620 sự kiện có kiểm soát.
- Precision: 98,41%, 99,05%, 99,42%.
- Recall: 91,85%, 96,67%, 95,93%.
- Macro-F1: 96,84%.
- Kiểm tra chức năng trên xe: recall 88,3%–93,3%.

Nguồn văn bản:

- `../01_SafeDrive_Mobile/source_code/from_icta_Dat_DMS_pipeline/revise_manuscript.py`
- `../01_SafeDrive_Mobile/source_code/from_icta_Dat_DMS_pipeline/ICTA_DMS_Doan_Ngoc_Phuong_Vu_Dinh_Dat_ICTA_IMRAD_SCOPUS_WOS_FINAL.pdf`

## Mâu thuẫn và thiếu hụt

1. Phiên bản cũ trong `from_bai_bao_Dat_DMS_prisma_review/create_docx.py` báo
   12 người và F1 87,1%, khác với bản thảo mới.
2. Chưa tìm thấy CSV/JSON sự kiện gốc để tính lại TP/FP/FN.
3. Không có dữ liệu theo người để kiểm tra khả năng tổng quát hóa.
4. Raw video không được giữ, nên không thể audit độc lập nhãn.
5. Mã Android hiện có trong SafeDrive là app cảm xúc tham khảo, không khớp trực
   tiếp pipeline được mô tả trong bản thảo.
6. Tài liệu `LUU_Y_TAC_GIA.md` yêu cầu xác nhận protocol, nguồn ngưỡng và đối
   chiếu phép biến đổi tọa độ với mã Android.

## Quy tắc sử dụng

Cho tới khi giải quyết các điểm trên, chỉ được viết:

> FaceAccess AI kế thừa giả thuyết và thiết kế đặc trưng từ một bản thảo
> SafeDrive đang báo cáo kết quả thăm dò tích cực; FaceAccess sẽ đánh giá lại
> độc lập trên bài toán cử chỉ có chủ ý.

Không được viết:

> FaceAccess AI đạt độ chính xác 96,84%.

## Bằng chứng cần tạo mới

- Protocol và biểu mẫu đồng thuận.
- Schema sự kiện và mã người tham gia ẩn danh.
- Dữ liệu đặc trưng theo cửa sổ, không lưu ảnh nếu không cần thiết.
- Chia tập theo người hoặc LOSO.
- Script huấn luyện/đánh giá tái lập.
- Confusion matrix, macro-F1, false activation/phút và latency.
- Model card và manifest chứa hash của dữ liệu/model/mã nguồn.

