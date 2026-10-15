## Overview

REST API dùng để đặt vé sự kiện.

Tính năng: xác thực, tìm kiếm sự kiện, seat zone, giữ ghế tạm thời, đặt vé, quản lý vé.

**Notes:**
- `userId` luôn lấy từ token xác thực

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

## Tài liệu OpenAPI: http://localhost:8080/swagger-ui/index.html
