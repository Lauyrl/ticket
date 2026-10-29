# BÁO CÁO KIỂM THỬ HIỆU NĂNG HỆ THỐNG ĐẶT VÉ SỰ KIỆN TRỰC TUYẾN

**Môn học:** Kiểm thử phần mềm  
**Dự án:** Ticket Booking System – Backend REST API  
**Công cụ kiểm thử:** Locust (kiểm thử tải)  
**Ngày kiểm thử:** 29/10/2026  
**Sinh viên thực hiện:** *(Điền tên sinh viên)*

---

## 1. Giới thiệu

### 1.1. Mô tả hệ thống

Hệ thống đặt vé sự kiện trực tuyến là một ứng dụng backend RESTful API xây dựng bằng **Java Spring Boot 4.1.1**, phục vụ hai nhóm người dùng chính:

- **Khách hàng (Customer):** Tìm kiếm sự kiện → chọn ghế → giữ ghế tạm thời (hold, hết hạn sau 10 phút) → tạo đơn hàng → thanh toán → nhận vé điện tử.
- **Quản trị viên (Admin):** Tạo và quản lý sự kiện, cấu hình sơ đồ ghế, theo dõi đơn hàng toàn hệ thống.

**Công nghệ sử dụng:**

| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ / Framework | Java, Spring Boot 4.1.1, Spring Web, Spring Security |
| Cơ sở dữ liệu | PostgreSQL 18 |
| Xác thực | JWT (Access Token + Refresh Token) |
| Triển khai | Docker, Docker Compose |
| CI | GitHub Actions |

### 1.2. Mục tiêu kiểm thử

Đánh giá hiệu năng (Performance Testing) của hệ thống dưới các mức tải người dùng đồng thời khác nhau, cụ thể:

- Đo thời gian phản hồi (Response Time) của từng endpoint.
- Đánh giá thông lượng (Throughput) tổng thể.
- Xác định điểm nghẽn cổ chai (Bottleneck) khi hệ thống chịu tải cao.
- Kiểm tra tỷ lệ lỗi (Failure Rate) ở các mức tải khác nhau.

---

## 2. Phạm vi kiểm thử

Các API endpoint được đưa vào kiểm thử tải bao gồm:

| Nhóm | Method | Endpoint | Quyền |
|---|---|---|---|
| Auth | POST | `/api/v1/auth/login` | Public |
| Auth | GET | `/api/v1/auth/me` | Authenticated |
| Events | GET | `/api/v1/events` | Public |
| Events | GET | `/api/v1/events?city=Hanoi` | Public |
| Events | GET | `/api/v1/events/{eventId}` | Public |
| Seats | GET | `/api/v1/events/{eventId}/seats` | Public |
| Seats | GET | `/api/v1/events/{eventId}/holds` | Authenticated |
| Seats | POST | `/seats/{id}/hold` | Authenticated |
| Seats | DELETE | `/seats/{id}/hold` | Authenticated |
| Orders | POST | `/api/v1/orders` | Authenticated |
| Orders | POST | `/api/v1/orders/{id}/pay` | Authenticated |
| Orders | GET | `/api/v1/orders/my` | Authenticated |

---

## 3. Kịch bản kiểm thử

### 3.1. Mô tả kịch bản

Kịch bản mô phỏng luồng sử dụng thực tế của một khách hàng:

1. **Đăng nhập** vào hệ thống.
2. **Duyệt danh sách sự kiện** (có và không có bộ lọc thành phố).
3. **Xem chi tiết** một sự kiện.
4. **Xem sơ đồ ghế** của sự kiện.
5. **Giữ ghế** (Hold seat).
6. **Xem danh sách holds** hiện tại.
7. **Tạo đơn hàng** từ các ghế đang giữ.
8. **Thanh toán** đơn hàng.
9. **Xem đơn hàng** của bản thân.
10. **Giải phóng ghế** (Delete hold, nếu cần).

### 3.2. Cấu hình kiểm thử

Ba kịch bản tải được thực hiện với số người dùng ảo đồng thời (Virtual Users – VU) khác nhau:

| Kịch bản | Số người dùng ảo (VU) | Tên file kết quả |
|---|---|---|
| Tải thấp | 100 VU | `results_100_stats.csv` |
| Tải trung bình | 500 VU | `results_500_stats.csv` |
| Tải cao | 1000 VU | `results_1000_stats.csv` |

---

## 4. Kết quả kiểm thử

### 4.1. Kịch bản 100 người dùng đồng thời (Tải thấp)

**Tổng quan:**

| Chỉ số | Giá trị |
|---|---|
| Tổng số requests | 11,615 |
| Số lỗi | 0 |
| Tỷ lệ lỗi | **0%** |
| Thông lượng (RPS) | 196.46 req/s |
| Median response time | 5 ms |
| Average response time | 18.94 ms |
| Max response time | 1,651 ms |

**Chi tiết từng endpoint:**

| Endpoint | Count | Median (ms) | Avg (ms) | Min (ms) | Max (ms) | p95 (ms) | p99 (ms) | RPS |
|---|---|---|---|---|---|---|---|---|
| DELETE /seats/{id}/hold | 252 | 5 | 6 | 4 | 38 | 14 | 23 | 4.26 |
| GET /api/v1/auth/me | 739 | 3 | 13 | 2 | 429 | 31 | 300 | 12.50 |
| GET /api/v1/events | 2,155 | 3 | 13 | 2 | 432 | 25 | 290 | 36.45 |
| GET /api/v1/events/{eventId} | 1,435 | 3 | 13 | 1 | 439 | 25 | 290 | 24.27 |
| GET /api/v1/events/{eventId}/holds | 738 | 3 | 11 | 2 | 439 | 16 | 250 | 12.48 |
| GET /api/v1/events/{eventId}/seats | 1,837 | 7 | 18 | 2 | 716 | 39 | 300 | 31.07 |
| GET /api/v1/events?city=Hanoi | 1,128 | 3 | 13 | 2 | 453 | 19 | 290 | 19.08 |
| GET /api/v1/orders/my | 741 | 4 | 12 | 2 | 429 | 19 | 300 | 12.53 |
| POST /api/v1/auth/login | 100 | 460 | **602** | 161 | **1,651** | 1,600 | 1,700 | 1.69 |
| POST /api/v1/orders | 389 | 8 | 10 | 5 | 107 | 18 | 32 | 6.58 |
| POST /api/v1/orders/{id}/pay | 389 | 7 | 8 | 5 | 44 | 15 | 25 | 6.58 |
| POST /seats/{id}/hold | 1,712 | 7 | 17 | 3 | 775 | 31 | 310 | 28.96 |
| **Aggregated** | **11,615** | **5** | **19** | **1** | **1,651** | **36** | **360** | **196.46** |

### 4.2. Kịch bản 500 người dùng đồng thời (Tải trung bình)

**Tổng quan:**

| Chỉ số | Giá trị |
|---|---|
| Tổng số requests | 30,836 |
| Số lỗi | 0 |
| Tỷ lệ lỗi | **0%** |
| Thông lượng (RPS) | 519.01 req/s |
| Median response time | 210 ms |
| Average response time | 373.20 ms |
| Max response time | 12,645 ms |

**Chi tiết từng endpoint:**

| Endpoint | Count | Median (ms) | Avg (ms) | Min (ms) | Max (ms) | p95 (ms) | p99 (ms) | RPS |
|---|---|---|---|---|---|---|---|---|
| DELETE /seats/{id}/hold | 351 | 240 | 495 | 119 | 10,922 | 1,500 | 6,800 | 5.91 |
| GET /api/v1/auth/me | 2,019 | 200 | 319 | 7 | 9,159 | 430 | 3,800 | 33.98 |
| GET /api/v1/events | 6,066 | 200 | 310 | 3 | 11,822 | 370 | 3,700 | 102.10 |
| GET /api/v1/events/{eventId} | 3,969 | 190 | 305 | 3 | 9,297 | 380 | 3,600 | 66.80 |
| GET /api/v1/events/{eventId}/holds | 2,024 | 200 | 332 | 10 | 10,058 | 430 | 4,500 | 34.07 |
| GET /api/v1/events/{eventId}/seats | 4,939 | 220 | 339 | 6 | 9,966 | 410 | 4,400 | 83.13 |
| GET /api/v1/events?city=Hanoi | 2,891 | 190 | 288 | 6 | 7,900 | 360 | 3,200 | 48.66 |
| GET /api/v1/orders/my | 2,064 | 210 | 330 | 8 | 9,716 | 450 | 4,100 | 34.74 |
| POST /api/v1/auth/login | 500 | 2,400 | **3,398** | 244 | **12,645** | 9,000 | 11,000 | 8.42 |
| POST /api/v1/orders | 550 | 230 | 411 | 40 | 7,008 | 1,300 | 5,200 | 9.26 |
| POST /api/v1/orders/{id}/pay | 550 | 230 | 328 | 123 | 6,433 | 440 | 3,100 | 9.26 |
| POST /seats/{id}/hold | 4,913 | 220 | 332 | 9 | 9,412 | 410 | 3,800 | 82.69 |
| **Aggregated** | **30,836** | **210** | **373** | **3** | **12,645** | **1,100** | **5,300** | **519.01** |

### 4.3. Kịch bản 1000 người dùng đồng thời (Tải cao)

**Tổng quan:**

| Chỉ số | Giá trị |
|---|---|
| Tổng số requests | 23,815 |
| Số lỗi | 0 |
| Tỷ lệ lỗi | **0%** |
| Thông lượng (RPS) | 383.19 req/s |
| Median response time | 740 ms |
| Average response time | 1,729 ms |
| Max response time | 24,659 ms (~25 giây) |

> [!NOTE]
> Thông lượng giảm từ 519 xuống 383 RPS khi tăng từ 500 lên 1000 VU, cho thấy hệ thống đã bắt đầu bão hòa (saturation) và không thể scale tuyến tính.

**Chi tiết từng endpoint:**

| Endpoint | Count | Median (ms) | Avg (ms) | Min (ms) | Max (ms) | p95 (ms) | p99 (ms) | RPS |
|---|---|---|---|---|---|---|---|---|
| DELETE /seats/{id}/hold | 38 | 910 | 2,561 | 638 | 15,731 | 12,000 | 16,000 | 0.61 |
| GET /api/v1/auth/me | 1,535 | 710 | 1,171 | 277 | 19,873 | 2,400 | 14,000 | 24.70 |
| GET /api/v1/events | 4,662 | 710 | 1,362 | 2 | 21,600 | 4,800 | 16,000 | 75.01 |
| GET /api/v1/events/{eventId} | 3,125 | 720 | 1,373 | 7 | 23,269 | 5,700 | 16,000 | 50.28 |
| GET /api/v1/events/{eventId}/holds | 1,620 | 720 | 1,359 | 4 | 19,414 | 6,400 | 14,000 | 26.07 |
| GET /api/v1/events/{eventId}/seats | 3,925 | 830 | 1,436 | 6 | 22,628 | 6,000 | 16,000 | 63.15 |
| GET /api/v1/events?city=Hanoi | 2,421 | 710 | 1,330 | 3 | 21,609 | 5,100 | 15,000 | 38.95 |
| GET /api/v1/orders/my | 1,540 | 720 | 1,282 | 5 | 20,211 | 3,300 | 16,000 | 24.78 |
| POST /api/v1/auth/login | 1,000 | 11,000 | **10,342** | 220 | **24,659** | 20,000 | 23,000 | 16.09 |
| POST /api/v1/orders | 46 | 880 | 951 | 502 | 3,883 | 1,600 | 3,900 | 0.74 |
| POST /api/v1/orders/{id}/pay | 46 | 850 | 958 | 549 | 5,988 | 1,100 | 6,000 | 0.74 |
| POST /seats/{id}/hold | 3,857 | 740 | 1,345 | 5 | 20,978 | 4,300 | 15,000 | 62.06 |
| **Aggregated** | **23,815** | **740** | **1,729** | **2** | **24,659** | **10,000** | **18,000** | **383.19** |

---

## 5. Phân tích và đánh giá

### 5.1. Tổng hợp so sánh qua các mức tải

| Mức tải | VU | Tổng Requests | RPS | Median (ms) | Avg (ms) | Max (ms) | Failure |
|---|---|---|---|---|---|---|---|
| Thấp | 100 | 11,615 | 196 | 5 | 19 | 1,651 | 0 |
| Trung bình | 500 | 30,836 | 519 | 210 | 373 | 12,645 | 0 |
| Cao | 1,000 | 23,815 | 383 | 740 | 1,729 | 24,659 | 0 |

### 5.2. Phân tích thời gian phản hồi

**✅ Ưu điểm:**
- Ở mức tải **100 VU**: hầu hết endpoint đạt median < 10 ms, trải nghiệm người dùng rất tốt (dưới ngưỡng "tức thì" 100 ms theo tiêu chuẩn Jakob Nielsen).
- **Tỷ lệ lỗi 0%** ở cả ba mức tải – hệ thống không trả về bất kỳ lỗi 5xx hay 4xx không mong muốn nào, chứng tỏ độ ổn định tốt.
- Các endpoint đọc (GET) như `/events`, `/events/{id}`, `/orders/my` scale tương đối tốt từ 100 → 500 VU.

**⚠️ Điểm cần cải thiện:**

1. **`POST /api/v1/auth/login` là nút cổ chai rõ rệt nhất:**

   | VU | Median login (ms) | p95 login (ms) | Max login (ms) |
   |---|---|---|---|
   | 100 | 460 | 1,600 | 1,651 |
   | 500 | 2,400 | 9,000 | 12,645 |
   | 1,000 | 11,000 | 20,000 | 24,659 |

   Thời gian đăng nhập tăng đột biến theo tải. Nguyên nhân chính rất có thể là do hàm **BCrypt hash password** có cost factor cao, là thao tác CPU-intensive và không thể song song hóa dễ dàng. Ở 1000 VU, median lên tới **11 giây**, hoàn toàn không chấp nhận được với người dùng thực tế.

2. **Thông lượng giảm khi vượt 500 VU:**
   
   RPS giảm từ 519 (500 VU) xuống 383 (1000 VU), cho thấy hệ thống đã đạt ngưỡng bão hòa. Connection pool database hoặc thread pool của Spring Boot nhiều khả năng là nút thắt.

3. **`GET /api/v1/events/{eventId}/seats` có median cao nhất trong nhóm GET:**
   
   Ở 100 VU: median 7 ms (bình thường), nhưng ở 1000 VU lên tới 830 ms với max 22,628 ms. Truy vấn này trả về danh sách ghế (kích thước content ~23 KB/response ở 100 VU) và có độ phức tạp truy vấn cao hơn các endpoint khác.

4. **`DELETE /seats/{id}/hold` giảm mạnh số lần gọi ở 1000 VU** (từ 351 lần → 38 lần), phản ánh rằng ít người dùng hoàn thành được toàn bộ luồng ở mức tải cao vì bị nghẽn ở các bước trước.

### 5.3. Nhận xét về khả năng mở rộng

```
100 VU  → 500 VU: Avg response time tăng ~20x (19 ms → 373 ms), RPS tăng ~2.6x
500 VU  → 1000 VU: Avg response time tăng ~4.6x (373 ms → 1729 ms), RPS giảm ~26%
```

Hệ thống scale **không tuyến tính** – dấu hiệu điển hình của tình trạng resource contention (tranh chấp tài nguyên), thường gặp ở kết nối database hoặc CPU.

---

## 6. Phát hiện vấn đề và khuyến nghị cải thiện

### 6.1. Vấn đề: Xác thực (Login) quá chậm dưới tải cao

**Mức độ:** 🔴 Nghiêm trọng

**Biểu hiện:** Median 11 giây ở 1000 VU.

**Nguyên nhân có thể:** BCrypt với work factor mặc định (10–12) mất ~100–400 ms/request ngay cả khi nhàn. Khi hàng trăm request đồng thời, thread pool bị kiệt sức.

**Khuyến nghị:**
- Xem xét điều chỉnh BCrypt cost factor (ví dụ giảm xuống 10 nếu đang dùng cao hơn), cân bằng giữa bảo mật và hiệu năng.
- Cache thông tin người dùng sau đăng nhập (sử dụng Redis hoặc in-memory cache) để giảm tải database.
- Tăng kích thước thread pool của Tomcat và connection pool của HikariCP trong cấu hình Spring Boot.

### 6.2. Vấn đề: API lấy sơ đồ ghế chậm ở tải cao

**Mức độ:** 🟡 Trung bình

**Biểu hiện:** `GET /events/{eventId}/seats` có median 830 ms ở 1000 VU, content size ~23 KB.

**Nguyên nhân có thể:** Truy vấn JOIN nhiều bảng (seat_zones → event_seats), trả về lượng data lớn, không có cache.

**Khuyến nghị:**
- Áp dụng **Redis cache** cho sơ đồ ghế, vì dữ liệu này ít thay đổi.
- Thêm **index** phù hợp trên cột `event_id` của bảng `event_seats`.
- Xem xét **phân trang** hoặc lazy-load theo zone thay vì trả toàn bộ ghế trong một request.

### 6.3. Vấn đề: Hệ thống bão hòa tại ~500 VU

**Mức độ:** 🟡 Trung bình

**Biểu hiện:** RPS giảm khi tăng từ 500 → 1000 VU.

**Khuyến nghị:**
- Cấu hình **HikariCP connection pool** với `maximum-pool-size` phù hợp (thường 10–20 kết nối cho PostgreSQL đơn node).
- Tăng `server.tomcat.threads.max` (mặc định 200) và `server.tomcat.accept-count`.
- Cân nhắc triển khai **horizontal scaling** (nhiều instance backend) với load balancer.
- Tích hợp **Redis** để cache các query đọc nhiều (danh sách sự kiện, ghế).

---

## 7. Kết luận

### 7.1. Đánh giá tổng thể

| Tiêu chí | 100 VU | 500 VU | 1000 VU |
|---|---|---|---|
| Tỷ lệ lỗi | ✅ 0% | ✅ 0% | ✅ 0% |
| Thời gian phản hồi tổng thể | ✅ Tốt | ⚠️ Chấp nhận được | ❌ Kém |
| Thời gian đăng nhập | ✅ Chấp nhận | ⚠️ Chậm | ❌ Rất chậm |
| Thông lượng | ✅ Tốt | ✅ Tốt | ⚠️ Giảm sút |
| Khả năng mở rộng | ✅ | ⚠️ | ❌ |

### 7.2. Nhận xét chung

- Hệ thống **hoạt động ổn định** và **không có lỗi** ở cả ba mức tải – đây là điểm cộng lớn về độ tin cậy.
- Ở mức tải **100 VU**, hệ thống vận hành tốt, phù hợp với yêu cầu của một dự án học thuật.
- Ở mức tải **500–1000 VU**, hiệu năng suy giảm đáng kể, đặc biệt ở chức năng đăng nhập. Hệ thống cần được tối ưu trước khi đưa vào môi trường production với lưu lượng truy cập thực tế cao.
- Các điểm cần ưu tiên cải thiện theo thứ tự: **(1) Tối ưu login**, **(2) Cache dữ liệu đọc nhiều**, **(3) Horizontal scaling**.

---

## 8. Phụ lục

### 8.1. File dữ liệu kiểm thử

| File | Mô tả |
|---|---|
| [`results_100_stats.csv`](file:///e:/UET/Project/github/ticket/test-kaggle/results_100_stats.csv) | Kết quả kịch bản 100 VU |
| [`results_500_stats.csv`](file:///e:/UET/Project/github/ticket/test-kaggle/results_500_stats.csv) | Kết quả kịch bản 500 VU |
| [`results_1000_stats.csv`](file:///e:/UET/Project/github/ticket/test-kaggle/results_1000_stats.csv) | Kết quả kịch bản 1000 VU |

### 8.2. Giải thích các chỉ số

| Chỉ số | Giải thích |
|---|---|
| **Median (p50)** | 50% requests có thời gian phản hồi ≤ giá trị này |
| **p95** | 95% requests có thời gian phản hồi ≤ giá trị này |
| **p99** | 99% requests có thời gian phản hồi ≤ giá trị này |
| **RPS** | Requests per second – số request được xử lý mỗi giây |
| **Avg Response Time** | Thời gian phản hồi trung bình (có thể bị kéo lệch bởi outliers) |
| **Max Response Time** | Thời gian phản hồi lớn nhất ghi nhận được |
| **Failure Count** | Số request trả về lỗi (HTTP 5xx hoặc timeout) |
