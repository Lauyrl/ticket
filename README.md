## Overview

Hệ thống đặt vé sự kiện trực tuyến gồm 2 tác nhân chính. **Khách hàng** tìm kiếm sự kiện, chọn ghế, giữ ghế tạm thời, đặt đơn, thanh toán và quản lý vé điện tử. **Quản trị viên** tạo và quản lý sự kiện, thiết kế khu vực và sơ đồ ghế, giám sát đơn hàng toàn hệ thống.

### Nghiệp vụ cốt lõi

**Luồng đặt vé của khách hàng:**
1. **Xác thực** - Người dùng đăng ký hoặc đăng nhập bằng email-password để nhận cặp JWT Access Token và Refresh Token. Sau khi xác thực, `userId` luôn được trích xuất từ token, không nhận từ client body/query.
2. **Tìm kiếm sự kiện** - Người dùng tìm kiếm sự kiện công khai với thanh tìm kiếm và bộ lọc (tên sự kiện, thành phố, khoảng thời gian). Các sự kiện được hiển thị dưới dạng phân trang. Hỗ trợ endpoint gợi ý tên sự kiện nhanh theo từ khóa.
3. **Chọn ghế (Seat Hold)** - Sau khi chọn sự kiện, người dùng xem sơ đồ ghế (nhóm theo zone → row → seat) và giữ ghế tạm thời (`SeatHold`). Mỗi ghế chỉ cho phép 1 hold `ACTIVE` hoặc `CONVERTED` tại một thời điểm (đảm bảo bằng `UNIQUE INDEX` trong DB, chống race condition). Hold tự động hết hạn sau thời gian `holdDurationMinutes` của sự kiện (khoảng 5-10 phút); khi có người hold mới thì lazy-check và release hold cũ.
4. **Tạo đơn hàng (Order)** - Người dùng xác nhận giỏ hàng, hệ thống tạo `Order` từ `SeatHold` đang `ACTIVE`. Đơn hàng ở trạng thái `PENDING` và có thời hạn thanh toán tương ứng.
5. **Thanh toán giả lập (Checkout)** - Người dùng xác nhận thanh toán (`POST /api/v1/orders/{orderId}/pay`), hệ thống chuyển Order sang `PAID`, chuyển trạng thái `SeatHold` sang `CONVERTED` (đã bán), sinh `Ticket` kèm **mã vé điện tử (`ticketCode`)** duy nhất cho từng ghế.
6. **Quản lý vé** - Người dùng xem danh sách vé điện tử đã mua và chi tiết từng vé (thông tin sự kiện, khu vực, hàng, số ghế và mã vé điện tử `ticketCode`).

**Nghiệp vụ Admin:**
- Tạo và quản lý sự kiện: thiết lập thông tin, danh mục, thời gian, địa điểm, sức chứa tối đa và trạng thái vòng đời sự kiện (`UPCOMING` → `ON_SALE` → `ENDED` / `CANCELLED`).
- Thiết kế sơ đồ ghế: cấu hình các khu vực ghế (`seat-zones`) với tên, giá vé, số hàng và số ghế mỗi hàng. Hệ thống tự động sinh bản ghi ghế tương ứng.
- Quản lý đơn hàng toàn hệ thống: xem danh sách đơn hàng, lọc theo sự kiện, trạng thái đơn hàng (`PENDING`, `PAID`, `EXPIRED`, `CANCELLED`) và xem chi tiết đơn hàng.

**Notes:**
- `userId` luôn lấy từ token xác thực, không nhận từ request body/query
- Trạng thái ghế được suy ra từ bảng `seat_holds`, không lưu cứng trên `event_seats`

---

# Endpoints

## 1. Auth & Users

| Method | Endpoint | Mô tả | Auth | Query params |
|--------|----------|-------|------|--------------|
| `POST` | `/api/v1/auth/register` | Đăng ký tài khoản khách hàng mới | Public | — |
| `POST` | `/api/v1/auth/login` | Đăng nhập và nhận cặp Access Token & Refresh Token | Public | — |
| `POST` | `/api/v1/auth/refresh` | Làm mới Access Token bằng Refresh Token | Public | — |
| `POST` | `/api/v1/auth/logout` | Đăng xuất (xóa phiên làm việc) | Public | — |
| `GET` | `/api/v1/auth/me` | Lấy thông tin tài khoản người dùng đang đăng nhập | Authenticated | — |
| `POST` | `/api/v1/auth/seed-admin` | Tạo tài khoản admin mặc định (Dev helper) | Public | — |

**Notes:**
- `POST /api/v1/auth/register`: Trả về `201 Created`; email đã tồn tại → `409 Conflict`
- `POST /api/v1/auth/login`: Email hoặc mật khẩu không hợp lệ → `401 Unauthorized`
- `POST /api/v1/auth/refresh`: Refresh token không hợp lệ hoặc hết hạn → `401 Unauthorized`
- `POST /api/v1/auth/logout`: JWT stateless, client xóa token ở local. Trả về thông báo thành công
- `GET /api/v1/auth/me`: User chưa xác thực → `401 Unauthorized`

<br>

## 2. Events

| Method | Endpoint | Mô tả | Auth | Query params |
|--------|----------|-------|------|--------------|
| `GET` | `/api/v1/events` | Danh sách sự kiện công khai (filter, pagination) | Public | `search`: tìm theo tên<br>`city`: lọc theo thành phố<br>`startsAfter`, `startsBefore`: lọc thời điểm bắt đầu<br>`endsAfter`, `endsBefore`: lọc thời điểm kết thúc<br>`page`: (default: 0) |
| `GET` | `/api/v1/events/suggest` | Gợi ý nhanh tên sự kiện theo từ khóa người dùng đang nhập | Public | `search`: từ khóa tìm kiếm |
| `GET` | `/api/v1/events/{eventId}` | Chi tiết thông tin một sự kiện | Public | — |

**Notes:**
- `GET /api/v1/events`: Kích thước trang mặc định là 12
- `GET /api/v1/events/{eventId}`: Sự kiện không tồn tại → `404 Not Found`

<br>

## 3. Seats & Seat Holds

| Method | Endpoint | Mô tả | Auth | Query params |
|--------|----------|-------|------|--------------|
| `GET` | `/api/v1/events/{eventId}/seats` | Lấy sơ đồ ghế theo khu vực (zone → seat kèm trạng thái `AVAILABLE`, `HELD`, `HELD_BY_ME`, `SOLD`) | Public / Authenticated | — |
| `POST` | `/api/v1/events/{eventId}/seats/{seatId}/hold` | Giữ ghế tạm thời (tự động hết hạn sau `holdDurationMinutes`) | Authenticated | — |
| `DELETE` | `/api/v1/events/{eventId}/seats/{seatId}/hold` | Hủy giữ ghế thủ công (giải phóng ghế ngay lập tức) | Authenticated | — |
| `GET` | `/api/v1/events/{eventId}/holds` | Lấy danh sách các ghế đang giữ còn hiệu lực (`ACTIVE`) của user trong sự kiện | Authenticated | — |

**Notes:**
- Trạng thái khả dụng của ghế được suy ra từ bảng `seat_holds`. Hold `ACTIVE` mà quá `expires_at` được coi là ghế trống.
- `POST .../hold`: Chống tranh chấp bằng UNIQUE INDEX `ux_seat_holds_taken_seat` (chỉ cho phép 1 hold `ACTIVE` hoặc `CONVERTED` trên mỗi ghế). Ghế đã có người giữ hoặc đã bán → `409 Conflict`. Vượt quá số ghế tối đa/user (`maxSeatsPerUser`) → `400 Bad Request`.
- `DELETE .../hold`: Thành công → `204 No Content`. Không có hold `ACTIVE` của user trên ghế đó → `409 Conflict`.

<br>

## 4. Orders & Checkout

| Method | Endpoint | Mô tả | Auth | Query params |
|--------|----------|-------|------|--------------|
| `POST` | `/api/v1/orders` | Tạo đơn hàng mới từ seat hold đang `ACTIVE` (trạng thái `PENDING`) | Authenticated | — |
| `POST` | `/api/v1/orders/{orderId}/pay` | Xác nhận thanh toán giả lập (chuyển sang `PAID`, xác nhận ghế `CONVERTED`) | Authenticated | — |
| `GET` | `/api/v1/orders/my` | Lịch sử đơn hàng của người dùng hiện tại | Authenticated | `status`: lọc theo trạng thái (`PENDING`, `PAID`, `EXPIRED`, `CANCELLED`)<br>`page`: (default: 0)<br>`size`: (default: 10) |
| `GET` | `/api/v1/orders/{orderId}` | Chi tiết đơn hàng của người dùng hiện tại | Authenticated | — |
| `DELETE` | `/api/v1/orders/{orderId}` | Hủy đơn hàng PENDING và giải phóng ghế tương ứng | Authenticated | — |

**Notes:**
- `POST /api/v1/orders`: Trả về `201 Created`. Seat hold phải thuộc về user hiện tại, đang `ACTIVE` và chưa hết hạn.
- `POST /api/v1/orders/{orderId}/pay`: Chuyển Order sang `PAID`, cập nhật `paidAt` và chuyển `SeatHold` sang `CONVERTED`. Đơn hàng không ở trạng thái `PENDING` hoặc đã quá hạn → `400 Bad Request` / `410 Gone`.
- `GET /api/v1/orders/my`: Sắp xếp theo `createdAt` giảm dần.
- `GET /api/v1/orders/{orderId}`: Đơn hàng không tồn tại hoặc không thuộc user hiện tại → `404 Not Found`. Nếu đơn hàng `PENDING` đã hết hạn, lazy update trạng thái thành `EXPIRED`.
- `DELETE /api/v1/orders/{orderId}`: Chỉ hủy được đơn hàng `PENDING`. Thành công trả về `204 No Content`.

<br>

## 5. Tickets

| Method | Endpoint | Mô tả | Auth | Query params |
|--------|----------|-------|------|--------------|
| `GET` | `/api/v1/tickets/my` | Danh sách vé điện tử đã mua của người dùng hiện tại | Authenticated | `status`: lọc theo trạng thái<br>`page`: (default: 0)<br>`size`: (default: 10) |
| `GET` | `/api/v1/tickets/{ticketId}` | Chi tiết một vé điện tử (thông tin sự kiện, khu vực, hàng, số ghế, mã vé `ticketCode`) | Authenticated | — |

**Notes:**
- Vé điện tử được tạo tự động khi đơn hàng được thanh toán thành công (`PAID`).
- Mã vé điện tử (`ticketCode`) là duy nhất và dùng để đối soát vé khi tham dự sự kiện.

<br>

## 6. Admin Management

> Tất cả endpoint `/api/v1/admin/**` yêu cầu quyền **ADMIN** (`ROLE_ADMIN`).

### Admin — Events & Sơ đồ ghế

| Method | Endpoint | Mô tả | Query params |
|--------|----------|-------|--------------|
| `GET` | `/api/v1/admin/events` | Danh sách tất cả sự kiện hệ thống (hỗ trợ search, filter, pagination) | `search`: tìm theo tên<br>`status`: lọc theo trạng thái (`UPCOMING`, `ON_SALE`, `ENDED`, `CANCELLED`)<br>`page`: (default: 0)<br>`size`: (default: 12) |
| `POST` | `/api/v1/admin/events` | Tạo sự kiện mới (hỗ trợ cấu hình kèm danh sách khu vực ghế `zones` và ghế `seats`) | — |
| `PATCH` | `/api/v1/admin/events/{eventId}` | Cập nhật thông tin sự kiện hoặc thay đổi trạng thái vòng đời (`ON_SALE`, `CANCELLED`,...) | — |
| `POST` | `/api/v1/admin/events/{eventId}/seat-zones` | Cấu hình hoặc bổ sung khu vực ghế và tự động sinh ghế cho sự kiện | — |

**Notes:**
- `POST /api/v1/admin/events`: Trả về `201 Created` kèm thông tin sự kiện vừa tạo. Hỗ trợ tạo kèm danh sách `zones` và `seats` ngay trong một request.
- `PATCH /api/v1/admin/events/{eventId}`: Vòng đời hợp lệ của sự kiện: `UPCOMING` → `ON_SALE` → `ENDED` / `CANCELLED`. Chuyển trạng thái hoặc thời gian không hợp lệ (`endsAt <= startsAt`) → `400 Bad Request`.
- `POST /api/v1/admin/events/{eventId}/seat-zones`: Nhận danh sách các zone (tên, giá tiền, danh sách ghế với `rowIndex` và `seatNumber`). Trả về sơ đồ ghế hoàn chỉnh của sự kiện.

### Admin — Orders

| Method | Endpoint | Mô tả | Query params |
|--------|----------|-------|--------------|
| `GET` | `/api/v1/admin/orders` | Danh sách đơn hàng toàn hệ thống (lọc theo sự kiện, trạng thái, phân trang) | `eventId`: lọc theo sự kiện<br>`status`: lọc theo trạng thái<br>`page`: (default: 0)<br>`size`: (default: 10) |
| `GET` | `/api/v1/admin/orders/{orderId}` | Xem chi tiết một đơn hàng bất kỳ trong hệ thống | — |

**Notes:**
- `GET /api/v1/admin/orders`: Sắp xếp theo `createdAt` giảm dần. Hỗ trợ lọc độc lập theo `eventId`, theo `status`, hoặc kết hợp cả hai.
- `GET /api/v1/admin/orders/{orderId}`: Đơn hàng không tồn tại → `404 Not Found`.

<br>

---

# Cách chạy

### Clean build
```bash
docker compose down -v
docker compose up --build
```

### Không build lại
```bash
docker compose up
```

<br>

# Database migration
- Database schema được quản lý tự động bởi Flyway
- Migration scripts được lưu tại: `src/main/resources/db/migration`
- Flyway chạy bên trong backend container và kết nối tới PostgreSQL container để tự động migrate schema và seed dữ liệu khởi tạo khi container khởi động.

<br>

## Tài liệu OpenAPI
- **Swagger UI (Interactive):** `http://localhost:8080/swagger-ui/index.html` (truy cập khi server đang chạy)
- **JSON API Docs:** `http://localhost:8080/v3/api-docs`
- **File đặc tả tĩnh (Static Spec):** `openapi.yaml` tại thư mục gốc
