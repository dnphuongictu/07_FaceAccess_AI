# Mã nguồn

Mã Android và pipeline nhận dạng mới của FaceAccess AI được phát triển tại
đây. Không sao chép trực tiếp các snapshot `from_*` chưa được kiểm toán giấy phép.

## Kiến trúc

Xem sơ đồ pipeline đầy đủ trong `../docs/00_Y_TUONG_VA_PHAM_VI.md`. Tóm tắt:

- `app/src/main/java/com/faceaccess/app/camera/` — CameraX + MediaPipe Face
  Landmarker (`FaceLandmarkerHelper`, `CameraSourceManager`).
- `app/src/main/java/com/faceaccess/app/metrics/` — tính EAR/MAR/head pose từ
  landmark (`FaceMetricsExtractor`), thuần Kotlin, có unit test.
- `app/src/main/java/com/faceaccess/app/gesture/` — cổng chống kích hoạt nhầm
  (`GestureStateMachine`), thuần Kotlin, có unit test.
- `app/src/main/java/com/faceaccess/app/overlay/` — Foreground Service hiển
  thị bảng điều khiển nổi (`FaceAccessOverlayService`, `OverlayContent`,
  `ScanController`).
- `app/src/main/java/com/faceaccess/app/access/` — `AccessibilityService`
  chỉ dùng để phát Back/Home hệ thống.
- `app/src/main/java/com/faceaccess/app/action/` — `ActionDispatcher` thực
  thi thao tác.
- `app/src/main/java/com/faceaccess/app/logging/` — ghi JSON Lines theo
  `../data/schema/gesture_event.schema.json`, không bao giờ ghi ảnh.
- `app/src/main/java/com/faceaccess/app/ui/` — màn hình Compose (onboarding,
  hiệu chỉnh, ghim ứng dụng).

## Build từ mã nguồn

Yêu cầu: JDK 17+ (khuyến nghị JDK 21), Android SDK có platform 35/36 và
build-tools 35, kết nối mạng cho lần build đầu tiên (tải dependency AndroidX/
CameraX/MediaPipe từ Maven).

```bash
# Trong thư mục src/ (đây chính là Gradle project root)
export JAVA_HOME=/duong/dan/toi/jdk17+
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest
```

Trên Windows, nếu project nằm trong đường dẫn có ký tự đặc biệt (dấu `&`,
khoảng trắng — ví dụ khi để trong OneDrive), Gradle/AGP có thể lỗi path hoặc
lỗi khoá file do đồng bộ đám mây. Khi đó hãy copy toàn bộ `src/` (trừ
`.gradle/`, `build/`, `.idea/`) ra một đường dẫn thường ngoài OneDrive rồi
build từ đó.

APK debug sau khi build nằm ở `app/build/outputs/apk/debug/app-debug.apk`.
