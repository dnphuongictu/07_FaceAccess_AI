# Kiểm thử

Unit test thật nằm theo đúng quy ước Gradle/Android, bên trong module app, để
`gradlew :app:testDebugUnitTest` tự phát hiện — không đặt file test ở đây:

- `../src/app/src/test/java/com/faceaccess/app/metrics/FaceMetricsExtractorTest.kt`
  — công thức EAR/MAR trên landmark giả lập, và giải mã góc Euler
  yaw/pitch/roll từ ma trận xoay (bao gồm chuyển đổi column-major của
  MediaPipe sang row-major nội bộ).
- `../src/app/src/test/java/com/faceaccess/app/gesture/GestureStateMachineTest.kt`
  — cổng chống kích hoạt nhầm: chớp mắt tự nhiên không kích hoạt, giữ đủ lâu
  mới kích hoạt, không lặp lại khi giữ liên tục, dừng khẩn cấp khi giữ quá
  ngưỡng, mất khuôn mặt huỷ cử chỉ dở dang.
- `../src/app/src/test/java/com/faceaccess/app/action/ScanControllerTest.kt`
  — thứ tự quét (quay vòng hai chiều) và ánh xạ ScanAction sang giá trị
  `action_mapped` của `../data/schema/gesture_event.schema.json`.

Còn thiếu (ngoài phạm vi lượt viết pipeline ban đầu): kịch bản an toàn end-to-end
trên thiết bị thật (instrumented test dùng UiAutomator/Accessibility, đo
latency/FPS/pin thật), và kiểm thử màn hình Compose (CalibrationScreen,
OnboardingScreen).

Chạy test: xem hướng dẫn build trong `../src/README.md`.
