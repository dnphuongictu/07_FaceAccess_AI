# Dữ liệu kế thừa từ SafeDrive

Ba tệp trong thư mục này là bản sao nguyên vẹn từ
`01_SafeDrive_Mobile/data/`, được đưa vào FaceAccess để thiết kế schema, kiểm
thử parser và dựng baseline. Tất cả các dòng CSV đều là **synthetic**, không
phải dữ liệu của 12 hoặc 30 người được mô tả trong các bản thảo SafeDrive.

## Manifest nguồn

| Tệp | SHA-256 nguồn |
|---|---|
| `driver_clip.schema.json` | `7CEDCC0DD487590E7D363E7A20ABC28F1B8E2CCB85356872AE34C9831A6BFDAE` |
| `sample_driver_events.csv` | `9E091AFC4CE15265E498D5574D3BBB7035E15E749EB07D3003DCBFAB96B6F686` |
| `student_practice_clips.csv` | `2ADC5875B558B216E8BC62840114B9353A2759D0D33B98E19606AA2A8C3F20B6` |

Nguồn cục bộ tại thời điểm sao chép:

```text
../01_SafeDrive_Mobile/data/driver_clip.schema.json
../01_SafeDrive_Mobile/data/sample_driver_events.csv
../01_SafeDrive_Mobile/data/student_practice_clips.csv
```

## Giới hạn sử dụng

- Schema SafeDrive gộp mọi `head_turn`; FaceAccess cần tách trái/phải/gật đầu.
- Trường `alert_type` là hành động cảnh báo lái xe, không phải thao tác trợ năng.
- `yaw` trong CSV mẫu là góc minh họa, trong khi bản thảo mới mô tả một chỉ số
  Z-depth tương đối; không được coi hai đại lượng là một.
- Không dùng 16 clip synthetic để báo độ chính xác của FaceAccess.
- Không dùng các tệp này để huấn luyện model triển khai thực tế.

