## Overview

Hệ thống đặt vé sự kiện trực tuyến gồm 2 tác nhân. **Khách hàng** tìm kiếm sự kiện, chọn ghế và mua vé tham dự các sự kiện. **Quản trị viên** tạo và quản lý sự kiện, khu vực ghế, đơn hàng và phân tích doanh thu theo thời gian thực.

### Nghiệp vụ cốt lõi

**Luồng đặt vé của khách hàng:**
1. **Xác thực** — Người dùng đăng ký hoặc đăng nhập bằng email-password. Sau khi xác thực, `userId` luôn được lấy từ JWT token, không nhận từ client.
2. **Tìm kiếm sự kiện** — Người dùng tìm kiếm sự kiện công khai với thanh tìm kiếm và bộ lọc (danh mục, thành phố, ngày). Các sự kiện được hiển thị dưới dạng phân trang. Trang chủ hiển thị sự kiện nổi bật, thanh tìm kiếm hỗ trợ gợi ý từ khóa theo thời gian thực.
3. **Chọn ghế (Seat Hold)** — Sau khi chọn sự kiện, người dùng xem sơ đồ ghế (nhóm theo zone → row → seat) và giữ ghế tạm thời (`SeatHold`). Mỗi ghế chỉ cho phép **1 hold ACTIVE hoặc CONVERTED** tại một thời điểm (đảm bảo bằng UNIQUE INDEX trong DB, chống race condition bằng `SELECT FOR UPDATE`). Hold tự động hết hạn sau thời gian `holdDurationMinutes` của sự kiện (khoảng 5 phút); khi có người hold mới thì lazy-check và release hold cũ.
4. **Tạo đơn hàng (Order)** — Người dùng xác nhận giỏ hàng, hệ thống tạo `Order` từ `SeatHold` đang ACTIVE. Đơn hàng ở trạng thái `PENDING` và cũng có thời hạn thanh toán (khoảng 10 phút).
5. **Thanh toán giả lập (Checkout)** — Người dùng xác nhận thanh toán, hệ thống chuyển Order sang `PAID`, sinh `Ticket` kèm mã QR cho từng ghế.
6. **Quản lý vé** — Người dùng xem danh sách vé của mình, chi tiết từng vé (bao gồm QR code để check-in tại sự kiện).

**Nghiệp vụ Admin:**
- Tạo và quản lý sự kiện (thông tin, hình ảnh, danh mục, trạng thái vòng đời: `DRAFT` → `ON_SALE` → `CANCELLED`/`COMPLETED`).
- Thiết kế sơ đồ ghế: tạo seat zone với tên, màu sắc, giá vé, số hàng và số ghế mỗi hàng. Hệ thống tự động sinh ghế.
- Quản lý đơn hàng toàn hệ thống: xem, lọc, cập nhật trạng thái.
- Xem dashboard analytics theo từng sự kiện: doanh thu, tỷ lệ lấp đầy (fill rate), phân bổ theo độ tuổi và giới tính.
- Upload ảnh cho sự kiện.

**Thông báo & Real-time:**
- Người dùng nhận thông báo trong ứng dụng đối với các sự kiện như: đặt vé thành công, hủy đơn, sự kiện sắp diễn ra.
- Hệ thống sử dụng **WebSocket (STOMP over SockJS)** để đẩy cập nhật real-time: trạng thái ghế, đơn hàng mới, analytics dashboard — không polling.

**Notes:**
- `userId` luôn lấy từ token xác thực, không nhận từ request body/query
- Trạng thái ghế được suy ra lazy từ bảng `seat_holds`, không lưu trực tiếp trên `event_seats`

# Endpoints

## Auth

| Method | Endpoint | Mô tả | Auth | Query params |
|--------|----------|-------|------|--------------|
| `POST` | `/api/v1/auth/register` | Đăng ký tài khoản mới | Public | — |
| `POST` | `/api/v1/auth/login` | Đăng nhập và nhận access token, refresh token | Public | — |
| `POST` | `/api/v1/auth/refresh` | Làm mới access token và refresh token bằng refresh token hiện tại | Public | — |
| `POST` | `/api/v1/auth/logout` | Đăng xuất | Public | — |
| `GET` | `/api/v1/auth/me` | Lấy thông tin user hiện tại | Authenticated | — |
| `POST` | `/api/v1/auth/seed-admin` | Tạo tài khoản admin nếu email chưa tồn tại; nếu đã tồn tại thì trả token cho tài khoản đó | Public | — |

**Notes:**
- `POST /api/v1/auth/register`: Trả về `201 Created`; email đã tồn tại → `409 Conflict`
- `POST /api/v1/auth/login`: Email hoặc mật khẩu không hợp lệ → lỗi xác thực
- `POST /api/v1/auth/refresh`: Refresh token không hợp lệ hoặc hết hạn → `401 Unauthorized`
- `POST /api/v1/auth/logout`: Browser tự xóa token ở local. Endpoint chỉ trả về thông báo thành công, chưa vô hiệu hóa token
- `GET /api/v1/auth/me`: Trả về thông tin tài khoản đang đăng nhập; user không xác thực → lỗi xác thực
- `POST /api/v1/auth/seed-admin`: Endpoint hiện đang public và có thể cấp token cho tài khoản đã tồn tại với email được gửi lên; cần bảo vệ hoặc vô hiệu hóa sau khi tạo admin ban đầu

<br>

## Events
| Method | Endpoint | Mô tả | Auth | Query params |
|--------|----------|-------|------|--------------|
| `GET` | `/api/v1/events` | Danh sách sự kiện (filter, pagination) | Public | `search`: tìm theo tên<br>`city`: lọc theo thành phố<br>`startsAfter`, `startsBefore`: lọc theo thời điểm bắt đầu<br>`endsAfter`, `endsBefore`: lọc theo thời điểm kết thúc<br>`page`: (default: 0) |
| `GET` | `/api/v1/events/suggest` | Dùng để gợi ý nhanh tên sự kiện theo từ khóa đang nhập khi user đang search | Public | `search`: suggest theo tên |
| `GET` | `/api/v1/events/{eventId}` | Chi tiết một sự kiện | Public | — |
| `POST` | `/api/v1/admin/events` | Tạo sự kiện mới, kèm seat zone | Admin | — |
| `PATCH` | `/api/v1/admin/events/{eventId}` | Cập nhật thông tin sự kiện | Admin | — |
| `POST` | `/api/v1/admin/events/{eventId}/seat-zones` | Tạo/cập nhật seat zones | Admin | — |
| `DELETE` | `/api/v1/admin/events/{eventId}/cancel` | CANCEL sự kiện | Admin | — |

**Notes:**
- `GET .../events`: Kích thước trang cố định là 12
- `GET .../events/{eventId}`: Sự kiện không tồn tại → `404`

<br>

## Seats, seat holds
| Method | Endpoint | Mô tả | Auth | Query params |
|--------|----------|-------|------|--------------|
| `GET` | `/api/v1/events/{eventId}/seats` | Lấy sơ đồ ghế (nhóm zone → seat), thông tin về row có ở trong metadata | Authenticated | — |
| `POST` | `/api/v1/events/{eventId}/seats/{seatId}/hold` | Tạo một seat hold, seat hold sẽ expire sau một khoảng thời gian | Authenticated | — |
| `DELETE` | `/api/v1/events/{eventId}/seats/{seatId}/hold` | Bỏ seat hold thủ công | Authenticated | — |
| `GET` | `/api/v1/events/{eventId}/holds` | Lấy các ghế đang giữ (ACTIVE, chưa hết hạn) của user trong event | Authenticated | — |

**Notes:**
- Trạng thái khả dụng của ghế được suy ra từ bảng `seat_holds`, không lưu trực tiếp trong `event_seats`. Hold ACTIVE mà quá `expires_at` được coi là ghế trống
- `POST .../hold`: Việc chống nhiều hold trên 1 ghế dựa trên UNIQUE INDEX `ux_seat_holds_taken_seat` trong DB - mỗi ghế chỉ được phép có 1 seat hold *ACTIVE* hay *CONVERTED* (đã bán), seat hold sẽ expire sau một khoảng thời gian (tùy vào event: `holdDurationMinutes`) và được release dựa trên lazy check khi có user muốn tạo hold mới trên cùng seat đó. Insert vi phạm index (ghế đã bị giữ hoặc đã bán) → `409`. Ghế không tồn tại hoặc không thuộc event này → `404`
- `DELETE .../hold`: thành công → `204`. Không có hold ACTIVE (chưa hết hạn) của user trên ghế đó → `409` 

<br>

## Orders

| Method | Endpoint | Mô tả | Auth | Query params |
|--------|----------|-------|------|--------------|
| `POST`   | `/api/v1/orders` | Tạo đơn hàng từ seat hold đang ACTIVE và chưa hết hạn | Authenticated | — |
| `GET`    | `/api/v1/orders/my` | List đơn hàng của user hiện tại | Authenticated | `status`: lọc theo trạng thái<br>`page`: (default: 0)<br>`size`: page size (default: 10) |
| `GET`    | `/api/v1/orders/{orderId}` | Chi tiết đơn hàng của user hiện tại | Authenticated | — |
| `DELETE` | `/api/v1/orders/{orderId}` | Hủy đơn hàng PENDING và seat hold tương ứng | Authenticated | — |
| `GET`    | `/api/v1/admin/orders` | List tất cả đơn hàng | Admin | `eventId`: lọc theo sự kiện<br>`status`: lọc theo trạng thái<br>`page`: (default: 0)<br>`size`: page size (default: 10) |
| `GET`    | `/api/v1/admin/orders/{orderId}` | Chi tiết một đơn hàng bất kỳ | Admin | — |

**Notes:**
- `POST /api/v1/orders`: Trả về `201 Created`. Seat hold phải thuộc user hiện tại, đang ACTIVE và chưa hết hạn
- `GET /api/v1/orders/my`: Sắp xếp theo `createdAt` giảm dần
- `GET /api/v1/orders/{orderId}`: Đơn hàng không tồn tại hoặc không thuộc user hiện tại → `404`. Nếu đơn hàng PENDING đã hết hạn, lazy update trạng thái thành EXPIRED
- `DELETE /api/v1/orders/{orderId}`: Chỉ hủy được đơn hàng PENDING và seat hold tương ứng nếu ACTIVE. Thành công trả về `204 No Content`. Đơn hàng không tồn tại hoặc không thuộc user hiện tại → `404`
- `GET /api/v1/admin/orders`: Sắp xếp theo `createdAt` giảm dần. Lưu ý: `status` filtering không hoạt động khi chỉ truyền `status` mà không truyền `eventId`; cần sửa `getAllOrders()`
- `GET /api/v1/admin/orders/{orderId}`: Đơn hàng không tồn tại → `404`
 
<br> 

## Tickets

| Method | Endpoint | Mô tả | Auth | Query params |
|--------|----------|-------|------|--------------|
| `GET` | `/api/v1/tickets/my` | Danh sách vé của user hiện tại (filter, pagination) | Authenticated | `status`: lọc theo trạng thái<br>`page`: (default: 0)<br>`size`: page size (default: 10) |
| `GET` | `/api/v1/tickets/my/events/{eventId}` | Danh sách vé của user hiện tại cho sự kiện cụ thể | Authenticated | `status`: lọc theo trạng thái<br>`page`: (default: 0)<br>`size`: page size (default: 10) |
| `GET` | `/api/v1/tickets/{ticketId}` | Chi tiết một vé của user hiện tại | Authenticated | — |

**Notes:**

<br>

## Notifications

| Method | Endpoint | Mô tả | Auth | Query params |
|--------|----------|-------|------|--------------|
| `GET` | `/api/v1/notifications` | Danh sách thông báo (pagination) | Authenticated | `page`: (default: 0)<br>`size`: page size (default: 10) |
| `GET` | `/api/v1/notifications/unread-count` | Số lượng thông báo chưa đọc | Authenticated | — |
| `PATCH` | `/api/v1/notifications/{notificationId}/read` | Đánh dấu một thông báo đã đọc | Authenticated | — |
| `PATCH` | `/api/v1/notifications/read-all` | Đánh dấu tất cả thông báo đã đọc | Authenticated | — |
| `DELETE` | `/api/v1/notifications/{notificationId}` | Xóa một thông báo | Authenticated | — |
| `DELETE` | `/api/v1/notifications` | Xóa các thông báo được chọn | Authenticated | `ids`: danh sách id cách nhau bởi dấu phẩy (vd: `1,2,3`) |
| `DELETE` | `/api/v1/notifications/all` | Xóa tất cả thông báo của user hiện tại | Authenticated | — |

**Notes:**
- Thông báo được tạo tự động bởi hệ thống khi có các sự kiện nghiệp vụ: đặt vé thành công, hủy đơn, sự kiện sắp diễn ra, v.v.
- Thông báo mới được đẩy real-time qua WebSocket topic `/topic/notifications/users/{userId}` — không polling.
- `PATCH .../read` và `PATCH .../read-all`: Trả về `200 OK` kèm số lượng thông báo vừa được đánh dấu
- `DELETE /api/v1/notifications`: Query param `ids` bắt buộc; trả về `204 No Content`

<br>

## Admin

> Tất cả endpoint `/api/v1/admin/**` yêu cầu role **Admin**.

### Admin — Events

| Method | Endpoint | Mô tả | Query params |
|--------|----------|-------|--------------|
| `GET` | `/api/v1/admin/events` | Danh sách tất cả sự kiện (search, filter, pagination) | `search`: tìm theo tên<br>`status`: lọc theo trạng thái<br>`page`: (default: 0)<br>`size`: page size (default: 12) |
| `POST` | `/api/v1/admin/events` | Tạo sự kiện mới | — |
| `POST` | `/api/v1/admin/events/with-seat-zones` | Tạo sự kiện kèm seat zones trong một request | — |
| `GET` | `/api/v1/admin/events/{eventId}` | Chi tiết sự kiện | — |
| `PATCH` | `/api/v1/admin/events/{eventId}` | Cập nhật thông tin sự kiện | — |
| `PATCH` | `/api/v1/admin/events/{eventId}/status` | Thay đổi trạng thái vòng đời sự kiện | `status`: giá trị mới (vd: `ON_SALE`, `CANCELLED`) |
| `DELETE` | `/api/v1/admin/events/{eventId}` | Xóa sự kiện | — |

**Notes:**
- `POST /api/v1/admin/events/with-seat-zones`: Tạo sự kiện và toàn bộ seat zone trong một transaction; thuận tiện khi setup sự kiện hoàn chỉnh ngay từ đầu
- `PATCH .../status`: Vòng đời hợp lệ: `DRAFT` → `ON_SALE` → `COMPLETED` / `CANCELLED`. Chuyển trạng thái không hợp lệ → `400`

### Admin — Seat Zones

| Method | Endpoint | Mô tả | Query params |
|--------|----------|-------|--------------|
| `GET` | `/api/v1/admin/events/{eventId}/seat-zones` | Danh sách seat zones của sự kiện | — |
| `GET` | `/api/v1/admin/events/{eventId}/seat-map` | Sơ đồ ghế đầy đủ (admin view: tất cả trạng thái) | — |
| `POST` | `/api/v1/admin/events/{eventId}/seat-zones` | Tạo hoặc cập nhật seat zones của sự kiện | — |

**Notes:**
- `POST .../seat-zones`: Body nhận danh sách zone; mỗi zone gồm tên, màu sắc, giá vé, số hàng (`rows`), số ghế mỗi hàng (`seatsPerRow`). Hệ thống tự động sinh bản ghi ghế tương ứng.
- `GET .../seat-map` (admin): Trả về toàn bộ trạng thái ghế bao gồm cả HELD và SOLD — khác với endpoint public chỉ trả về trạng thái có thể chọn

### Admin — Orders

| Method | Endpoint | Mô tả | Query params |
|--------|----------|-------|--------------|
| `GET` | `/api/v1/admin/orders` | Danh sách đơn hàng toàn hệ thống (search, filter, pagination) | `eventId`: lọc theo sự kiện<br>`status`: lọc theo trạng thái<br>`search`: tìm theo tên/email khách<br>`page`: (default: 0)<br>`size`: page size (default: 10) |
| `GET` | `/api/v1/admin/orders/{orderId}` | Chi tiết một đơn hàng bất kỳ | — |
| `PATCH` | `/api/v1/admin/orders/{orderId}/status` | Cập nhật trạng thái đơn hàng | `status`: trạng thái mới (vd: `CANCELLED`) |

**Notes:**
- `GET /api/v1/admin/orders`: Sắp xếp theo `createdAt` giảm dần.
- `GET /api/v1/admin/orders/{orderId}`: Đơn hàng không tồn tại → `404`

### Admin — Dashboard & Analytics

| Method | Endpoint | Mô tả | Query params |
|--------|----------|-------|--------------|
| `GET` | `/api/v1/admin/dashboard/{eventId}` | Analytics của sự kiện: doanh thu, fill rate, phân bổ độ tuổi/giới tính | — |

**Notes:**
- Dashboard cũng nhận cập nhật tự động qua WebSocket topic `/topic/admin/dashboard/{eventId}` khi có đơn hàng mới

### Admin — Upload

| Method | Endpoint | Mô tả | Query params |
|--------|----------|-------|--------------|
| `POST` | `/api/v1/admin/upload` | Upload ảnh sự kiện (`multipart/form-data`), trả về URL | — |

<br>

## WebSocket (STOMP over SockJS)

> **Endpoint kết nối:** `ws://localhost:8080/ws`  
> **Protocol:** STOMP, SockJS fallback  
> Client subscribe vào topic để nhận cập nhật real-time; không cần polling REST.

| Topic | Mô tả | Dành cho |
|-------|-------|----------|
| `/topic/seats/{eventId}` | Cập nhật trạng thái ghế real-time (HELD / AVAILABLE / SOLD) | Frontend — trang chọn ghế |
| `/topic/admin/seats/{eventId}` | Cập nhật ghế real-time (admin view, đầy đủ trạng thái) | Admin — trang quản lý ghế |
| `/topic/admin/orders/{eventId}` | Đơn hàng mới / cập nhật trạng thái theo sự kiện | Admin — trang quản lý đơn hàng |
| `/topic/admin/orders/global` | Tất cả đơn hàng toàn hệ thống | Admin dashboard tổng quan |
| `/topic/admin/dashboard/{eventId}` | Cập nhật analytics dashboard tự động khi có giao dịch mới | Admin dashboard |
| `/topic/events` | Cập nhật danh sách sự kiện (sự kiện mới, thay đổi trạng thái) | Frontend — trang chủ |
| `/topic/notifications/users/{userId}` | Thông báo real-time của người dùng cụ thể | Frontend — notification bell |

**Notes:**
- Client cần authenticate trước khi subscribe vào các topic user-specific (vd: `/topic/notifications/users/{userId}`)
- Payload của mỗi message là JSON tương ứng với entity được cập nhật (SeatDto, OrderDto, NotificationDto, v.v.)

<br>

## EXAMPLE
| Method | Endpoint | Mô tả | Auth | Query params |
|--------|----------|-------|------|--------------|
| `GET/POST/DELETE` | `...` | ... | Public/Authenticated/Admin | ... |

**Notes:**
- `...`: ...

<br>

# Cách chạy
### Clean build
```
docker compose down -v
docker compose up --build
```
### Không build lại
```
docker compose up
```

<br>

# Database migration
- Database schema được quản lý bởi Flyway
- Migration scripts được lưu tại: `src/main/resources/db/migration`
- Flyway chạy bên trong backend container và kết nối tới PostgreSQL container để chạy các migration khi backend container khởi động

<br>

## Tài liệu OpenAPI
- **Swagger UI (Interactive):** `http://localhost:8080/swagger-ui/index.html` (truy cập khi server đang chạy)
- **JSON API Docs:** `http://localhost:8080/v3/api-docs`
- **File đặc tả tĩnh (Static Spec):** `openapi.yaml` tại thư mục gốc (có thể import vào Postman, Insomnia hoặc Swagger Editor)
