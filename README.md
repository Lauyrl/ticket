# Ticket Rush: Hệ thống đặt vé sự kiện trực tuyến

REST API đặt vé sự kiện, xây dựng bằng Spring Boot và PostgreSQL. 

## 1. Bài toán

Khách hàng cần mua vé cho các sự kiện (hòa nhạc, triển lãm, hội thảo...) với sơ đồ ghế cụ thể. Bài toán có ba khó khăn chính:

- **Tranh chấp ghế:** nhiều người có thể cùng chọn một ghế trong cùng thời điểm. Hệ thống phải đảm bảo mỗi ghế chỉ thuộc về một người.
- **Giữ ghế tạm thời:** người dùng chọn ghế nhưng chưa thanh toán ngay. Ghế phải được giữ trong một khoảng thời gian, rồi tự trả về nếu không thanh toán.
- **Vé điện tử:** sau khi thanh toán, mỗi ghế phải có một vé riêng, có mã để đối soát khi vào sự kiện.

Hệ thống giải quyết ba vấn đề này bằng ràng buộc trong database, thời hạn giữ ghế và luồng đơn hàng tách biệt với vé.

## 2. Tác nhân và nghiệp vụ

Hệ thống có hai tác nhân:

- **Khách hàng:** tìm sự kiện, chọn ghế, giữ ghế, tạo đơn, thanh toán, xem vé.
- **Quản trị viên (ADMIN):** tạo và cập nhật sự kiện, cấu hình khu vực ghế, xem toàn bộ đơn hàng.

### Luồng mua vé

1. **Xác thực:** người dùng đăng ký hoặc đăng nhập bằng email và mật khẩu, nhận Access Token và Refresh Token (JWT). Mọi thao tác sau đó lấy `userId` từ token, không nhận từ body hay query.
2. **Tìm sự kiện:** tìm công khai theo tên, thành phố, thời gian bắt đầu, có phân trang.
3. **Chọn và giữ ghế:** xem sơ đồ ghế theo khu vực (zone, hàng, ghế) với trạng thái `AVAILABLE`, `HELD`, `SOLD`. Người dùng giữ ghế tạm thời, mỗi ghế chỉ có một hold `ACTIVE` hoặc `CONVERTED` tại một thời điểm.
4. **Tạo đơn:** từ các hold còn hiệu lực, hệ thống tạo đơn hàng ở trạng thái `PENDING`.
5. **Thanh toán giả lập:** xác nhận thanh toán chuyển đơn sang `PAID`, hold sang `CONVERTED`, và sinh vé cho từng ghế.
6. **Xem vé:** người dùng xem danh sách và chi tiết vé của chính mình.

### Quy tắc nghiệp vụ

- Một ghế chỉ có tối đa một hold `ACTIVE` hoặc `CONVERTED`. Ràng buộc này nằm ở UNIQUE INDEX `ux_seat_holds_taken_seat` trong database, nên không phụ thuộc vào thứ tự xử lý trong code.
- Hold có thời hạn theo `holdDurationMinutes` của sự kiện (mặc định 10 phút). Hold `ACTIVE` đã quá hạn được coi là ghế trống.
- Mỗi người dùng giữ tối đa `maxSeatsPerUser` ghế trong một sự kiện.
- Trạng thái ghế không lưu trực tiếp trên bảng `event_seats`, mà suy ra từ bảng `seat_holds`.
- Truy cập vé hoặc đơn không thuộc người dùng trả `404 Not Found`, không trả `403`, để không lộ việc tài nguyên có tồn tại.

### Vé điện tử

- Vé được sinh khi đơn hàng chuyển sang `PAID`, mỗi ghế trong đơn một vé.
- Mã vé có dạng `{orderCode}-T{itemId}`, duy nhất trong hệ thống, dùng để đối soát khi vào sự kiện.
- Danh sách vé chỉ liệt kê vé của đơn đã thanh toán.
- Trạng thái `USED` (đã check-in) được khai báo trong đặc tả nhưng hệ thống chưa có chức năng check-in. Vì vậy lọc `status=USED` hiện trả danh sách rỗng.

## 3. Đặc tả API

Đặc tả đầy đủ nằm trong file `openapi.yaml` ở thư mục gốc, có thể mở bằng extension Swagger Preview của VS Code hoặc dán lên editor.swagger.io. Bảng dưới đây là tóm tắt.

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

Đường dẫn `/api/v1/admin/**` yêu cầu quyền ADMIN. Các endpoint khác yêu cầu Access Token trong header `Authorization: Bearer <token>`, nếu không có token trả `401`.

### Mã lỗi

| Tình huống | Mã |
|---|---|
| Sai email hoặc mật khẩu, token không hợp lệ | 401 |
| Tài nguyên không tồn tại hoặc không thuộc người dùng | 404 |
| Email đã tồn tại, ghế đã được giữ hoặc đã bán, đơn đã tồn tại | 409 |
| Vượt quá số ghế tối đa, hold hết hạn, đơn không ở trạng thái cho phép, tham số sai | 400 |

## 4. Kiến trúc

Hệ thống theo kiến trúc phân tầng. Mỗi tầng chỉ gọi tầng ngay bên dưới:

```
Client
  -> Security (JwtAuthenticationFilter: xác thực token, đặt người dùng vào SecurityContext)
  -> Controller (nhận request, khai báo đường dẫn, chuyển cho service)
  -> Service (quy tắc nghiệp vụ, không import Spring Web hay JPA)
  -> Repository (Spring Data JPA, truy vấn database)
  -> PostgreSQL (schema do Flyway quản lý)
```

Dữ liệu trả về đi qua DTO (`dto/`), không trả trực tiếp entity, để không lộ cấu trúc bảng và các trường nội bộ.

### Tổ chức mã nguồn

Mã nguồn nằm ở `src/main/java/com/example/ticket/`:

- `config/`: cấu hình chung, gồm `SecurityConfig` (quy định endpoint nào public, endpoint nào cần đăng nhập).
- `security/`: xác thực JWT. Gồm `JwtAuthenticationFilter` (đọc và kiểm tra token), `JwtTokenProvider` (tạo và xác thực token), `CustomUserDetailsService` và `UserPrincipal` (thông tin người dùng đã xác thực).
- `controller/`: tầng API.
- `service/`: tầng nghiệp vụ.
- `repository/`: truy cập dữ liệu, chia theo nghiệp vụ: `event`, `order`, `user`.
- `entity/`: ánh xạ các bảng database.
- `dto/`: đối tượng vào và ra của API, chia theo nghiệp vụ: `auth`, `event`, `order`, `ticket`.
- `enums/`: các kiểu liệt kê nghiệp vụ.
- `exceptions/`: exception nghiệp vụ và `GlobalExceptionHandler`, nơi đổi lỗi thành mã HTTP.

Tầng service không import `org.springframework.web` hay JPA. Quy tắc này giúp đổi công nghệ ở tầng dưới mà không phải sửa logic nghiệp vụ.

## 5. Công nghệ

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

## 6. Mô hình dữ liệu

Các bảng chính, theo thứ tự migration:

- `users`: tài khoản người dùng và vai trò (`CUSTOMER`, `ADMIN`).
- `events`: sự kiện, gồm tên, địa điểm, thời gian, trạng thái, `max_seats_per_user`, `hold_duration_minutes`.
- `seat_zones`: khu vực ghế của một sự kiện, gồm tên, giá, duy nhất theo `(event_id, name)`.
- `event_seats`: ghế, thuộc một zone và một event, duy nhất theo `(event_id, row_index, seat_number)`.
- `seat_holds`: giữ ghế, có thời hạn `expires_at`, có UNIQUE INDEX chống tranh chấp.
- `orders`, `order_items`: đơn hàng và chi tiết từng ghế trong đơn.
- `tickets`: vé, gắn với `order_item` (mỗi item tối đa một vé).

Các thay đổi schema được viết thành script đánh số trong `src/main/resources/db/migration` (V1 đến V6).

## 7. Triển khai và chạy

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

`Dockerfile` gồm hai giai đoạn. Giai đoạn build dùng JDK để chạy `./mvnw clean package -DskipTests` và tạo file JAR. Giai đoạn chạy chỉ dùng JRE, copy JAR và chạy `java -jar app.jar`.

## 8. Database migration

- Flyway chạy tự động khi backend khởi động, kết nối tới container PostgreSQL.
- Script nằm tại `src/main/resources/db/migration`, đặt tên `V<số>__<mô tả>.sql`.
- Không sửa script đã chạy. Thay đổi mới phải thêm file `V<số tiếp theo>__...sql`, vì Flyway kiểm tra checksum và sẽ báo lỗi nếu file cũ bị đổi.
- Migration cũng nạp dữ liệu mẫu (sự kiện và ghế) để có thể test ngay.

## 9. Kiểm thử và CI

### Kiểm thử

```bash
./mvnw test
```

Test gồm kiểm thử xác thực (`AuthSecurityTests`), kiểm thử nghiệp vụ đơn hàng (`OrderServiceTest`), kiểm thử nghiệp vụ vé (`TicketServiceTest`) và kiểm thử ứng dụng khởi động (`TicketApplicationTests`). Test khởi động ứng dụng cần PostgreSQL đang chạy.

### CI

Workflow `.github/workflows/ci.yml` chạy khi có push và pull request vào `main`:

1. Khởi động PostgreSQL làm service.
2. Build và test bằng `./mvnw clean verify`.
3. Build Docker image.

Pull request chỉ nên được merge khi CI xanh.

## 10. Vấn đề dễ thấy và hướng cải tiến (Phase 2)

Các điểm dưới đây là cơ sở để đánh giá và cải tiến ở Phase 2, theo thuộc tính chất lượng:

- **Hiệu năng khi nhiều người cùng chọn ghế:** nhiều request giữ cùng một ghế sẽ nhận `409`. Cần đo tỉ lệ `409` và độ trễ trong kiểm thử tải.
- **Đọc sơ đồ ghế và danh sách sự kiện:** là các truy vấn đọc nhiều nhất. Cần đo thời gian khi số sự kiện và số ghế lớn. Hướng cải tiến: cache kết quả đọc.
- **Giải phóng hold hết hạn theo kiểu lười:** hold quá hạn chỉ được dọn khi có người giữ ghế đó lần sau, nên trạng thái ghế có thể không chính xác trong thời gian dài. Hướng cải tiến: job định kỳ hoặc tính trạng thái khi đọc.
- **Tạo vé đồng bộ trong request thanh toán:** hiện không có cổng thanh toán thật nên chưa có độ trễ. Khi tích hợp cổng thật, việc sinh vé nên chuyển sang xử lý bất đồng bộ qua hàng đợi.
- **Truy vấn vé:** `findPaidTickets` dùng `EntityGraph` để tránh truy vấn lặp khi lấy thông tin sự kiện. Đây là điểm để so sánh trước và sau khi tối ưu.



