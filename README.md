## 1. Overview

Hệ thống đặt vé sự kiện trực tuyến gồm 2 tác nhân chính. **Khách hàng** tìm kiếm sự kiện, chọn ghế, giữ ghế tạm thời, đặt đơn, thanh toán và quản lý vé điện tử. **Quản trị viên** tạo và quản lý sự kiện, thiết kế khu vực và sơ đồ ghế, giám sát đơn hàng toàn hệ thống.

### Nghiệp vụ cốt lõi

**Luồng đặt vé của khách hàng:**
1. **Xác thực:** Người dùng đăng ký hoặc đăng nhập bằng email và mật khẩu, nhận Access Token và Refresh Token (JWT). Mọi thao tác sau đó lấy `userId` từ token, không nhận từ body hay query.
2. **Tìm sự kiện:** Người dùng tìm kiếm sự kiện công khai với thanh tìm kiếm và bộ lọc (tên sự kiện, thành phố, khoảng thời gian). Các sự kiện được hiển thị dưới dạng phân trang.
3. **Chọn và giữ ghế:** Sau khi chọn sự kiện, người dùng xem sơ đồ ghế (nhóm theo zone → row → seat) với 3 trạng thái `AVAILABLE`, `HELD`, `SOLD`. Người dùng chọn và giữ ghế (hold) tạm thời (hết hạn sau 10 phút), mỗi ghế chỉ có một hold `ACTIVE` hoặc `CONVERTED` tại một thời điểm. Mỗi người dùng có thể sở hữu tối đa 2 ghế/1 sự kiện.
4. **Tạo đơn:** Từ các hold còn `ACTIVE`, hệ thống tạo đơn hàng ở trạng thái `PENDING` và có thời hạn thanh toán tương ứng với thời gian còn lại.
5. **Thanh toán giả lập:** Xác nhận thanh toán chuyển đơn sang `PAID`, hold sang `CONVERTED`, và sinh vé cho từng ghế.
6. **Xem vé:** Người dùng xem danh sách và chi tiết vé của chính mình.

**Admin quản lý hệ thống:**
- Tạo và quản lý sự kiện: thiết lập thông tin, danh mục, thời gian, địa điểm, sức chứa tối đa và trạng thái vòng đời sự kiện (`UPCOMING` → `ON_SALE` → `ENDED` / `CANCELLED`).
- Thiết kế sơ đồ ghế: cấu hình các khu vực ghế (`seat-zones`) với tên, giá vé, số hàng và số ghế mỗi hàng. Hệ thống tự động sinh bản ghi ghế tương ứng.
- Quản lý đơn hàng toàn hệ thống: xem danh sách đơn hàng, lọc theo sự kiện và trạng thái đơn hàng (`PENDING`, `PAID`, `EXPIRED`, `CANCELLED`).

## 2. Danh sách API endpoints

Bảng dưới đây tóm tắt các API endpoints của hệ thống. Đặc tả đầy đủ nằm trong file `openapi.yaml` ở thư mục `docs`, có thể mở bằng extension Swagger Preview của VS Code hoặc dán lên editor.swagger.io.

| Nhóm | Endpoint | Quyền |
|---|---|---|
| Auth | `POST /api/v1/auth/register`, `POST /api/v1/auth/login`, `POST /api/v1/auth/refresh`, `POST /api/v1/auth/logout` | Public |
| Auth | `GET /api/v1/auth/me` | Authenticated |
| Events | `GET /api/v1/events` (`search`, `city`, `startsAfter`, `startsBefore`, `page`, `size` mặc định 12) | Public |
| Events | `GET /api/v1/events/{eventId}` | Public |
| Seats | `GET /api/v1/events/{eventId}/seats` | Public / Authenticated |
| Seats | `POST` và `DELETE /api/v1/events/{eventId}/seats/{seatId}/hold` | Authenticated |
| Seats | `GET /api/v1/events/{eventId}/holds` | Authenticated |
| Orders | `POST /api/v1/orders`, `POST /api/v1/orders/{orderId}/pay` | Authenticated |
| Orders | `GET /api/v1/orders/my` (`status`, `page`, `size` mặc định 10) | Authenticated |
| Orders | `GET` và `DELETE /api/v1/orders/{orderId}` | Authenticated |
| Tickets | `GET /api/v1/tickets/my` (`eventId`, `status` là `VALID` hoặc `USED`, `page`, `size` mặc định 10) | Authenticated |
| Tickets | `GET /api/v1/tickets/{ticketId}` | Authenticated, chỉ chủ vé |
| Admin | `GET` và `POST /api/v1/admin/events` | ADMIN |
| Admin | `PATCH /api/v1/admin/events/{eventId}` | ADMIN |
| Admin | `POST /api/v1/admin/events/{eventId}/seat-zones` | ADMIN |
| Admin | `GET /api/v1/admin/orders` (`eventId`, `status`, `page`, `size` mặc định 10) | ADMIN |

## 3. Kiến trúc

Hệ thống theo kiến trúc phân tầng, mỗi tầng chỉ tương tác với các tầng liền kề nó. Cụ thể các tầng của hệ thống như sau:

```
Client
  -> Security (JwtAuthenticationFilter: xác thực token, đặt người dùng vào SecurityContext)
  -> Controller (nhận request, khai báo đường dẫn, chuyển cho service)
  -> Service (quy tắc nghiệp vụ, không import Spring Web hay JPA)
  -> Repository (Spring Data JPA, truy vấn database)
  -> PostgreSQL (schema do Flyway quản lý)
```

Dữ liệu trả về được đi qua DTO (`dto/`), không trả trực tiếp để không lộ cấu trúc bảng và các trường nội bộ.

### Tổ chức mã nguồn

Mã nguồn chính của dự án nằm trong `src/main/java/com/example/ticket/`, gồm các thư mục sau:

- `config/`: cấu hình chung, gồm `SecurityConfig` (quy định endpoint nào public, endpoint nào cần đăng nhập).
- `security/`: xác thực JWT. Gồm `JwtAuthenticationFilter` (đọc và kiểm tra token), `JwtTokenProvider` (tạo và xác thực token), `CustomUserDetailsService` và `UserPrincipal` (thông tin người dùng đã xác thực).
- `controller/`: tầng API.
- `service/`: tầng nghiệp vụ.
- `repository/`: truy cập dữ liệu, chia theo nghiệp vụ: `event`, `order`, `user`.
- `entity/`: ánh xạ các bảng database.
- `dto/`: đối tượng vào và ra của API, chia theo nghiệp vụ: `auth`, `event`, `order`, `ticket`.
- `enums/`: các kiểu liệt kê nghiệp vụ.
- `exceptions/`: exception nghiệp vụ và `GlobalExceptionHandler`, nơi đổi lỗi thành mã HTTP.

## 4. Công nghệ

| Thành phần | Công nghệ |
|---|---|
| Ngôn ngữ, framework | Java, Spring Boot 4.1.1, Spring Web, Spring Security |
| Truy cập dữ liệu | Spring Data JPA, Hibernate |
| Cơ sở dữ liệu | PostgreSQL 18 |
| Migration | Flyway |
| Xác thực | JWT (Access Token và Refresh Token) |
| Tài liệu API | springdoc-openapi (Swagger UI), file `openapi.yaml` |
| Đóng gói | Docker, Docker Compose |
| CI | GitHub Actions (`.github/workflows/ci.yml`) |

## 5. Mô hình dữ liệu

Các bảng chính:

- `users`: tài khoản người dùng, có hai role là `CUSTOMER` và `ADMIN`.
- `events`: thông tin sự kiện, gồm tên, địa điểm, thời gian, trạng thái, `max_seats_per_user`, `hold_duration_minutes`.
- `seat_zones`: khu vực ghế của một sự kiện, gồm tên, giá, duy nhất theo `(event_id, name)`.
- `event_seats`: ghế, thuộc một zone và một event, duy nhất theo `(event_id, row_index, seat_number)`.
- `seat_holds`: giữ ghế, có thời hạn `expires_at`, có UNIQUE INDEX chống tranh chấp.
- `orders`, `order_items`: đơn hàng và chi tiết từng ghế trong đơn.
- `tickets`: vé, gắn với `order_item` (mỗi item tối đa một vé).

Các thay đổi schema trong quá trình phát triển dự án được viết thành script đánh số trong `src/main/resources/db/migration`.

## 6. Triển khai và chạy

### Yêu cầu

Docker Desktop. Không cần cài Java hay PostgreSQL trên máy.

### Các lệnh

```bash
# Build lại image và chạy toàn bộ (backend và PostgreSQL)
docker compose up --build -d

# Xem log backend
docker compose logs -f backend

# Dừng và xóa cả dữ liệu database
docker compose down -v
```

Khi backend khởi động thành công, log có dòng `Started TicketApplication`.

### Địa chỉ

- Backend: `http://localhost:8080`
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- JSON API docs: `http://localhost:8080/v3/api-docs`
- PostgreSQL: cổng `5432`, thông tin kết nối trong `docker-compose.yml`

### Quy trình build trong Docker

`Dockerfile` gồm hai giai đoạn: Giai đoạn build dùng JDK để chạy `./mvnw clean package -DskipTests` và tạo file JAR. Giai đoạn chạy chỉ dùng JRE, copy JAR và chạy `java -jar app.jar`.

## 7. Database migration

- Hệ thống sử dụng Flyway, chạy tự động khi backend khởi động và kết nối tới container PostgreSQL.
- Script nằm tại `src/main/resources/db/migration`, format `V<số>__<mô tả>.sql`.
- Không sửa script đã chạy. Nếu muốn thay đổi thì phải thêm file `V<số tiếp theo>__...sql`, vì Flyway kiểm tra checksum và sẽ báo lỗi nếu file cũ bị đổi.
- Migration cũng nạp dữ liệu mẫu (sự kiện và ghế) để có thể test ngay.

## 8. Kiểm thử và CI

### Kiểm thử

```bash
./mvnw test
```

Test gồm kiểm thử xác thực (`AuthSecurityTests`), kiểm thử nghiệp vụ đơn hàng (`OrderServiceTest`), kiểm thử nghiệp vụ vé (`TicketServiceTest`) và kiểm thử khởi động ứng dụng (`TicketApplicationTests`). Test khởi động ứng dụng cần PostgreSQL đang chạy.

### CI

Workflow `.github/workflows/ci.yml` chạy khi có push và pull request vào `main`:

1. Khởi động PostgreSQL làm service.
2. Build và test bằng `./mvnw clean verify`.
3. Build Docker image.

Pull request chỉ nên được merge khi CI xanh.

## 9. Vấn đề để cải tiến (Phase 2)

Nhóm cải tiến ở Phase 2 có thể cân nhắc các vấn đề sau để cải tiến hệ thống:

- **Chịu tải:** Khi có các sự kiện hot, hệ thống có thể chịu lượng truy cập lớn đột xuất. Nhóm có thể đưa ra các giải pháp giúp đảm bảo trải nghiệm người dùng cũng như tính sẵn sàng của hệ thống.
- **Đọc sơ đồ ghế và danh sách sự kiện:** Đây là các truy vấn đọc nhiều nhất, số lượng bản ghi lớn. Nhóm có thể thực hiện các cải tiến để hiệu năng tốt hơn.
- **Giải phóng hold hết hạn:** Hiện tại, ngoài việc người dùng tự huỷ, hệ thống áp dụng giải phóng hold quá hạn bằng cách lazy update (hold quá hạn chỉ được dọn khi có người khác bấm hold lại ghế đó), nên trạng thái ghế có thể không chính xác trong thời gian dài. Nhóm có thể áp dụng các giải pháp tốt hơn.
- **Thanh toán và tạo vé:** Hiện không có cổng thanh toán thật nên chưa có độ trễ. Nhóm có thể tích hợp cổng thanh toán thật để hệ thống thực tế hơn.
- **Giới hạn vé 1 người dùng/1 sự kiện**: Nhằm tránh tình trạng mua vé đầu cơ tích trữ, hệ thống hiện áp dụng khoá cứng 2 vé/1 sự kiện/1 người dùng. Nhóm có thể áp dụng các giải pháp tốt hơn. 