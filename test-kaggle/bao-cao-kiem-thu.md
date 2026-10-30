# Kết quả kiểm thử tải trên Kaggle

**Công cụ kiểm thử:** Locust (kiểm thử tải)

**Mục tiêu kiểm thử:** Đánh giả hiệu năng và khả năng chịu tải của hệ thống dưới các mức tải người dùng đồng thời khác nhau, cụ thể:

- Đo thời gian phản hồi (Response Time) của từng endpoint.
- Đánh giá thông lượng (Throughput) tổng thể.
- Xác định điểm nghẽn cổ chai (Bottleneck) khi hệ thống chịu tải cao.
- Kiểm tra tỷ lệ lỗi (Failure Rate) ở các mức tải khác nhau.

## Phạm vi kiểm thử

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

Các endpoint này đều là các API quan trọng, thường xuyên được người dùng sử dụng.

## Kịch bản kiểm thử

Nhóm chạy 3 kịch bản kiểm thử khác nhau, mỗi kịch bản chạy trong 1 phút với số lượng người dùng ảo như sau:

| Kịch bản | Số người dùng ảo (VU) |Tốc độ sinh người dùng/s| Tên file kết quả |
|---|---|---|---|
| Tải thấp | 100 VU | 10 users/s | `results_100_stats.csv` |
| Tải trung bình | 500 VU | 50 users/s | `results_500_stats.csv` |
| Tải cao | 1000 VU | 1000 users/s |`results_1000_stats.csv` |

## Kết quả kiểm thử

### 1. 100 người dùng đồng thời (Tải thấp)

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
| **Tổng hợp** | **11,615** | **5** | **19** | **1** | **1,651** | **36** | **360** | **196.46** |

### 2. 500 người dùng đồng thời (Tải trung bình)

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
| **Tổng hợp** | **30,836** | **210** | **373** | **3** | **12,645** | **1,100** | **5,300** | **519.01** |

### 3. 1000 người dùng đồng thời (Tải cao)

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
| **Tổng hợp** | **23,815** | **740** | **1,729** | **2** | **24,659** | **10,000** | **18,000** | **383.19** |

## Phân tích và đánh giá

### 1. Tổng hợp so sánh các mức tải

| Mức tải | VU | Tổng Requests | RPS | Median (ms) | Avg (ms) | Max (ms) | Failure |
|---|---|---|---|---|---|---|---|
| Thấp | 100 | 11,615 | 196 | 5 | 19 | 1,651 | 0 |
| Trung bình | 500 | 30,836 | 519 | 210 | 373 | 12,645 | 0 |
| Cao | 1,000 | 23,815 | 383 | 740 | 1,729 | 24,659 | 0 |

### 2. Phân tích thời gian phản hồi

**Ưu điểm:**
- Ở mức tải **100 VU** hầu hết endpoint đạt median < 10 ms, trải nghiệm người dùng tốt.
- **Tỷ lệ lỗi 0%** cho thấy độ ổn định của hệ thống tốt.
- Các endpoint đọc (GET) như `/events`, `/events/{id}`, `/orders/my` scale tương đối tốt từ 100 → 500 VU.

**Điểm cần cải thiện:**

1. **`POST /api/v1/auth/login` là Bottleneck rõ ràng nhất:**

   | VU | Median login (ms) | p95 login (ms) | Max login (ms) |
   |---|---|---|---|
   | 100 | 460 | 1,600 | 1,651 |
   | 500 | 2,400 | 9,000 | 12,645 |
   | 1,000 | 11,000 | 20,000 | 24,659 |

   Thời gian đăng nhập tăng đột biến theo tải. Nguyên nhân chính rất có thể là do hàm **BCrypt hash password** có cost factor cao. Ở 1000 VU, median lên tới **11 giây**, không thể chấp nhận được với người dùng thực tế.

2. **Thông lượng giảm khi vượt 500 VU:**
   
   RPS giảm từ 519 (500 VU) xuống 383 (1000 VU) cho thấy hệ thống đã đạt ngưỡng bão hòa. Connection pool database hoặc thread pool của Spring Boot khả năng cao là vấn đề.

3. **`GET /api/v1/events/{eventId}/seats` có median cao nhất trong nhóm GET:**
   
   Ở 100 VU thời gian median lấy được danh sách ghế là 7 ms nhưng ở 1000 VU lên tới 830 ms với max 22,628 ms. Đây là truy vấn trả về nội dung tương đối nặng và có độ phức tạp cao hơn các endpoint khác.
## Kết luận

### 1. Đánh giá tổng thể

| Tiêu chí | 100 VU | 500 VU | 1000 VU |
|---|---|---|---|
| Tỷ lệ lỗi | 0% | 0% | 0% |
| Thời gian phản hồi tổng thể | Tốt | Chấp nhận được | Kém |
| Thời gian đăng nhập | Chấp nhận | Chậm |Rất chậm |
| Thông lượng | Tốt | Tốt | Giảm sút |

### 2. Nhận xét chung

- Hệ thống **hoạt động ổn định** và **không có lỗi** ở cả ba mức tải.
- Ở mức tải **100 VU**, hệ thống vận hành tốt.
- Ở mức tải **500–1000 VU**, hiệu năng suy giảm đáng kể, nhất là ở chức năng đăng nhập.
- Các điểm cần ưu tiên cải thiện: **(1) Tối ưu login**, **(2) Xử lý dữ liệu đọc nhiều**, **(3) Horizontal scaling**.

---

### Giải thích các chỉ số

| Chỉ số | Giải thích |
|---|---|
| **Median (p50)** | 50% requests có thời gian phản hồi ≤ giá trị này |
| **p95** | 95% requests có thời gian phản hồi ≤ giá trị này |
| **p99** | 99% requests có thời gian phản hồi ≤ giá trị này |
| **RPS** | Requests per second – số request được xử lý mỗi giây |
| **Avg Response Time** | Thời gian phản hồi trung bình |
| **Max Response Time** | Thời gian phản hồi lâu nhất ghi nhận được |
| **Failure Count** | Số request trả về lỗi (HTTP 5xx hoặc timeout) |