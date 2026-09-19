# MD04_SS08_Bai1: Cài đặt Circuit Breaker cơ bản với Resilience4j

Dự án Microservices mẫu minh họa cơ chế **Circuit Breaker** (Ngắt mạch khi sập nguồn) sử dụng **Spring Boot 3**, **Java 21**, **Gradle** và thư viện **Resilience4j**.

---

## 1. Kiến trúc Hệ thống

Hệ thống bao gồm 2 dịch vụ độc lập:
1. **`doctor-service`** (Cổng `8082`):
   - Quản lý và cung cấp API kiểm tra lịch trực của các bác sĩ.
   - Endpoint: `GET /doctors/{doctorId}/schedule`

2. **`appointment-service`** (Cổng `8083`):
   - Tiếp nhận yêu cầu đặt lịch hẹn từ bệnh nhân.
   - Gọi sang `doctor-service:8082` để xác thực lịch trực của bác sĩ trước khi tiến hành đặt lịch.
   - Được bảo vệ bởi **Circuit Breaker** (`doctorServiceCB`). Khi `doctor-service` gặp sự cố hoặc sập nguồn, Circuit Breaker sẽ ngắt mạch ngay lập tức (OPEN), chuyển hướng sang hàm **Fallback** để phản hồi lập tức cho người dùng, tránh treo luồng (thread starvation / cascading failure).

```
                      +-----------------------------+
                      |   Client / Người dùng       |
                      +--------------+--------------+
                                     |
                                     | (1) POST /appointments/book hoặc
                                     |     GET /appointments/check-doctor/1
                                     v
                 +---------------------------------------+
                 |       Appointment-Service (8083)      |
                 |                                       |
                 |      [Circuit Breaker: doctorServiceCB]|
                 |          - CLOSED / OPEN / HALF_OPEN  |
                 |          - Fallback Handler           |
                 +-------------------+-------------------+
                                     | (2) Gọi REST API
                     (Ngắt khi sập)  |     (Fail fast)
                                     v
                 +---------------------------------------+
                 |         Doctor-Service (8082)         |
                 |  - GET /doctors/{id}/schedule         |
                 +---------------------------------------+
```

---

## 2. Cấu hình Circuit Breaker (`application.properties`)

Cấu hình tại `appointment-service/src/main/resources/application.properties`:

```properties
spring.application.name=appointment-service
server.port=8083

# Endpoint kết nối sang Doctor-Service
doctor.service.url=http://localhost:8082

# Cấu hình Resilience4j Circuit Breaker cho instance "doctorServiceCB"
resilience4j.circuitbreaker.instances.doctorServiceCB.failure-rate-threshold=50
resilience4j.circuitbreaker.instances.doctorServiceCB.minimum-number-of-calls=5
resilience4j.circuitbreaker.instances.doctorServiceCB.wait-duration-in-open-state=10s
resilience4j.circuitbreaker.instances.doctorServiceCB.permitted-number-of-calls-in-half-open-state=3
resilience4j.circuitbreaker.instances.doctorServiceCB.sliding-window-size=5
resilience4j.circuitbreaker.instances.doctorServiceCB.sliding-window-type=COUNT_BASED

# Actuator Endpoints theo dõi trạng thái
management.endpoints.web.exposure.include=health,info,circuitbreakers,circuitbreakerevents
management.endpoint.health.show-details=always
management.health.circuitbreakers.enabled=true

# Ghi log DEBUG chi tiết của Resilience4j
logging.level.io.github.resilience4j=DEBUG
logging.level.com.example.appointmentservice=INFO
```

### Ý nghĩa các thông số:
- **`failure-rate-threshold=50`**: Mạch sẽ ngắt (`OPEN`) nếu tỷ lệ cuộc gọi thất bại đạt từ 50% trở lên.
- **`minimum-number-of-calls=5`**: Số lần gọi tối thiểu cần ghi nhận trong cửa sổ trượt trước khi tính toán tỷ lệ lỗi để quyết định ngắt mạch.
- **`wait-duration-in-open-state=10s`**: Thời gian mạch giữ trạng thái `OPEN` trước khi chuyển sang `HALF_OPEN` để thăm dò dịch vụ đích đã hồi phục hay chưa.
- **`permitted-number-of-calls-in-half-open-state=3`**: Số cuộc gọi thử nghiệm được phép đi qua khi ở trạng thái `HALF_OPEN`.
- **`logging.level.io.github.resilience4j=DEBUG`**: Bật in log chi tiết trạng thái mạch và từng request.

---

## 3. Hướng dẫn chạy & Thực hành quan sát chuyển trạng thái mạch

### Bước 1: Khởi động 2 dịch vụ

Mở 2 cửa sổ terminal riêng biệt:

- **Terminal 1: Khởi động Doctor-Service (8082)**
  ```powershell
  .\gradlew :doctor-service:bootRun
  ```

- **Terminal 2: Khởi động Appointment-Service (8083)**
  ```powershell
  .\gradlew :appointment-service:bootRun
  ```

---

### Bước 2: Kiểm tra khi hệ thống hoạt động bình thường (Mạch `CLOSED`)

Thực hiện gọi API kiểm tra lịch trực bác sĩ:
```powershell
curl http://localhost:8083/appointments/check-doctor/1
```
**Kết quả mong đợi:**
```json
{
  "success": true,
  "message": "Lay thong tin lich truc bac si thanh cong.",
  "appointmentId": null,
  "doctorSchedule": {
    "doctorId": 1,
    "doctorName": "Dr. Nguyen Van A",
    "specialty": "Tim mach",
    "availableTime": "08:00 - 11:30",
    "available": true
  },
  "circuitBreakerState": "CLOSED"
}
```

Kiểm tra trạng thái mạch hiện tại:
```powershell
curl http://localhost:8083/appointments/circuit-breaker-status
```
Kết quả hiển thị: `"state": "CLOSED"`.

---

### Bước 3: Mô phỏng Doctor-Service sập nguồn (Tắt Service 8082)

- Tại **Terminal 1** (nơi đang chạy `doctor-service`), bấm tổ hợp phím `Ctrl + C` để dừng dịch vụ.
- Lúc này dịch vụ `doctor-service` đã bị sập hoàn toàn.

---

### Bước 4: Gọi API đặt lịch 5 lần & Quan sát ngắt mạch (`CLOSED` -> `OPEN`)

Thực hiện gọi API 5 lần liên tiếp:
```powershell
1..5 | ForEach-Object {
    Write-Host "--- Lan goi $_ ---"
    curl http://localhost:8083/appointments/check-doctor/1
    Start-Sleep -Milliseconds 200
}
```

**Hiện tượng quan sát được:**
1. **Tại các lần gọi 1 đến 4**:
   - Cuộc gọi sang port 8082 bị từ chối kết nối (`ConnectException`).
   - Hàm **Fallback** lập tức kích hoạt, trả về thông báo lỗi an toàn thay vì làm ứng dụng bị crash.
2. **Sau lần gọi thứ 5**:
   - Tổng số cuộc gọi đạt `minimum-number-of-calls=5` và tỷ lệ thất bại là 100% (> 50%).
   - Terminal của `appointment-service` xuất hiện dòng log nổi bật:
     ```text
     ⚡⚡⚡ [CIRCUIT BREAKER: doctorServiceCB] CHUYEN TRANG THAI: CLOSED -> OPEN ⚡⚡⚡
     ```
3. **Từ lần gọi thứ 6 trở đi**:
   - Mạch đang ở trạng thái `OPEN`.
   - Hệ thống **ngắt kết nối ngay lập tức (fail-fast)** mà KHÔNG hề gửi request qua mạng, trả về fallback trong `0ms`.
   - Kiểm tra endpoint trạng thái:
     ```powershell
     curl http://localhost:8083/appointments/circuit-breaker-status
     ```
     Trả về: `"state": "OPEN"`.

---

### Bước 5: Chuyển sang `HALF_OPEN` và Phục hồi (`OPEN` -> `HALF_OPEN` -> `CLOSED`)

1. Chờ đủ **10 giây** (theo cấu hình `wait-duration-in-open-state=10s`).
2. Khởi động lại `doctor-service` ở Terminal 1:
   ```powershell
   .\gradlew :doctor-service:bootRun
   ```
3. Gửi 1 request từ `appointment-service`:
   - Mạch chuyển sang trạng thái `HALF_OPEN`.
4. Gửi tiếp 3 request thành công (`permitted-number-of-calls-in-half-open-state=3`):
   - Mạch ghi nhận dịch vụ đã phục hồi và chuyển lại trạng thái:
     ```text
     ⚡⚡⚡ [CIRCUIT BREAKER: doctorServiceCB] CHUYEN TRANG THAI: HALF_OPEN -> CLOSED ⚡⚡⚡
     ```

---

## 4. Chạy Kiểm Thử Tự Động (Automated Testing)

Dự án đã được tích hợp sẵn bộ kiểm thử đơn vị & tích hợp với JUnit 5 & Mockito theo chuẩn TDD:
```powershell
.\gradlew test
```
Tất cả các ca kiểm thử:
- Khởi tạo ban đầu mạch `CLOSED`.
- Giữ `CLOSED` khi dịch vụ đích phản hồi tốt.
- Tự động ngắt mạch sang `OPEN` sau đúng 5 lần lỗi liên tiếp.
- Ngắt mạng tức thì (short-circuit) khi mạch `OPEN`.
- Chuyển tiếp trạng thái sang `HALF_OPEN`.
đều vượt qua 100% thành công.
