## Overview

Hệ thống đặt vé sự kiện trực tuyến gồm 2 tác nhân chính. **Khách hàng** tìm kiếm sự kiện, chọn ghế, giữ ghế tạm thời, đặt đơn, thanh toán và quản lý vé điện tử. **Quản trị viên** tạo và quản lý sự kiện, thiết kế khu vực và sơ đồ ghế, giám sát đơn hàng toàn hệ thống.

### Nghiệp vụ cốt lõi

**Luồng đặt vé của khách hàng:**
1. **Xác thực** - Người dùng đăng ký hoặc đăng nhập bằng email-password để nhận cặp JWT Access Token và Refresh Token. Sau khi xác thực, `userId` luôn được trích xuất từ token, không nhận từ client body/query.
2. **Tìm kiếm sự kiện** - Người dùng tìm kiếm sự kiện công khai với thanh tìm kiếm và bộ lọc (tên sự kiện, thành phố, khoảng thời gian). Các sự kiện được hiển thị dưới dạng phân trang.
3. **Chọn ghế (Seat Hold)** - Sau khi chọn sự kiện, người dùng xem sơ đồ ghế (nhóm theo zone → row → seat) và giữ ghế tạm thời (`SeatHold`). Mỗi ghế chỉ cho phép 1 hold `ACTIVE` hoặc `CONVERTED` tại một thời điểm (đảm bảo bằng `UNIQUE INDEX` trong DB, chống race condition). Hold tự động hết hạn sau thời gian `holdDurationMinutes` của sự kiện (khoảng 5-10 phút); khi có người hold mới thì lazy-check và release hold cũ.
4. **Tạo đơn hàng (Order)** - Người dùng xác nhận giỏ hàng, hệ thống tạo `Order` từ `SeatHold` đang `ACTIVE`. Đơn hàng ở trạng thái `PENDING` và có thời hạn thanh toán tương ứng.
5. **Thanh toán giả lập (Checkout)** - Người dùng xác nhận thanh toán (`POST /api/v1/orders/{orderId}/pay`), hệ thống chuyển Order sang `PAID`, chuyển trạng thái `SeatHold` sang `CONVERTED` (đã bán), sinh `Ticket` kèm **mã vé điện tử (`ticketCode`)** duy nhất cho từng ghế.
6. **Quản lý vé** - Người dùng xem danh sách vé điện tử đã mua và chi tiết từng vé (thông tin sự kiện, khu vực, hàng, số ghế và mã vé điện tử `ticketCode`).

**Nghiệp vụ Admin:**
- Tạo và quản lý sự kiện: thiết lập thông tin, danh mục, thời gian, địa điểm, sức chứa tối đa và trạng thái vòng đời sự kiện (`UPCOMING` → `ON_SALE` → `ENDED` / `CANCELLED`).
- Thiết kế sơ đồ ghế: cấu hình các khu vực ghế (`seat-zones`) với tên, giá vé, số hàng và số ghế mỗi hàng. Hệ thống tự động sinh bản ghi ghế tương ứng.
- Quản lý đơn hàng toàn hệ thống: xem danh sách đơn hàng, lọc theo sự kiện và trạng thái đơn hàng (`PENDING`, `PAID`, `EXPIRED`, `CANCELLED`).

**Notes:**
- `userId` luôn lấy từ token xác thực, không nhận từ request body/query
- Trạng thái ghế được suy ra từ bảng `seat_holds`, không lưu cứng trên `event_seats`

---

# Endpoints

## 1. Auth & Users (5 Endpoints)
| Method | Endpoint | Mô tả | Auth | Query params |
|:---:|---|---|:---:|---|
| `POST` | `/api/v1/auth/register` | Đăng ký tài khoản khách hàng mới | Public | — |
| `POST` | `/api/v1/auth/login` | Đăng nhập bằng Email/Password, cấp Access & Refresh Token | Public | — |
| `POST` | `/api/v1/auth/refresh` | Làm mới Access Token bằng Refresh Token | Public | — |
| `POST` | `/api/v1/auth/logout` | Đăng xuất (xóa phiên làm việc) | Public | — |
| `GET` | `/api/v1/auth/me` | Lấy thông tin tài khoản người dùng đang đăng nhập | Authenticated | — |

**Notes:**
- `POST /api/v1/auth/register`: Trả về `201 Created`; email đã tồn tại → `409 Conflict`
- `POST /api/v1/auth/login`: Email hoặc mật khẩu không hợp lệ → `401 Unauthorized`
- `POST /api/v1/auth/refresh`: Refresh token không hợp lệ hoặc hết hạn → `401 Unauthorized`
- `POST /api/v1/auth/logout`: JWT stateless, client xóa token ở local. Trả về thông báo thành công
- `GET /api/v1/auth/me`: User chưa xác thực → `401 Unauthorized`

---

## 2. Events (2 Endpoints)
| Method | Endpoint | Mô tả | Auth | Query params |
|:---:|---|---|:---:|---|
| `GET` | `/api/v1/events` | Danh sách sự kiện (tìm kiếm, phân trang) | Public | `search`: từ khóa tên sự kiện<br>`city`: thành phố<br>`startsAfter`, `startsBefore`: lọc thời điểm bắt đầu<br>`page`: số trang (default: 0)<br>`size`: số phần tử (default: 12) |
| `GET` | `/api/v1/events/{eventId}` | Chi tiết thông tin sự kiện (thời gian, địa điểm, mô tả, chính sách giữ ghế) | Public | — |

**Notes:**
- `GET /api/v1/events`: Kích thước trang mặc định là 12, hỗ trợ tìm kiếm theo tên và lọc theo thành phố / thời gian bắt đầu
- `GET /api/v1/events/{eventId}`: Sự kiện không tồn tại → `404 Not Found`

---

## 3. Seats & Seat Holds (4 Endpoints)
| Method | Endpoint | Mô tả | Auth | Query params |
|:---:|---|---|:---:|---|
| `GET` | `/api/v1/events/{eventId}/seats` | Lấy sơ đồ ghế theo khu vực (zone → hàng → ghế kèm trạng thái `AVAILABLE`, `HELD`, `SOLD`) | Public / Authenticated | — |
| `POST` | `/api/v1/events/{eventId}/seats/{seatId}/hold` | Giữ ghế tạm thời (chống tranh chấp với UNIQUE INDEX, tự hết hạn sau `holdDurationMinutes`) | Authenticated | — |
| `DELETE` | `/api/v1/events/{eventId}/seats/{seatId}/hold` | Hủy giữ ghế thủ công (giải phóng ghế ngay lập tức) | Authenticated | — |
| `GET` | `/api/v1/events/{eventId}/holds` | Danh sách ghế đang giữ còn hiệu lực (`ACTIVE`) của user trong sự kiện | Authenticated | — |

**Notes:**
- Trạng thái khả dụng của ghế được suy ra từ bảng `seat_holds`. Hold `ACTIVE` mà quá `expires_at` được coi là ghế trống.
- `POST .../hold`: Chống tranh chấp bằng UNIQUE INDEX `ux_seat_holds_taken_seat` (chỉ cho phép 1 hold `ACTIVE` hoặc `CONVERTED` trên mỗi ghế). Ghế đã có người giữ hoặc đã bán → `409 Conflict`. Vượt quá số ghế tối đa/user (`maxSeatsPerUser`) → `400 Bad Request`.
- `DELETE .../hold`: Thành công → `204 No Content`. Không có hold `ACTIVE` của user trên ghế đó → `409 Conflict`.

---

## 4. Orders & Checkout (5 Endpoints)
| Method | Endpoint | Mô tả | Auth | Query params |
|:---:|---|---|:---:|---|
| `POST` | `/api/v1/orders` | Tạo đơn hàng mới từ các ghế đang hold (chuyển sang `PENDING`) | Authenticated | — |
| `POST` | `/api/v1/orders/{orderId}/pay` | Xác nhận thanh toán giả lập (chuyển sang `PAID`, sinh danh sách vé kèm `ticketCode`) | Authenticated | — |
| `GET` | `/api/v1/orders/my` | Lịch sử đơn hàng của người dùng hiện tại (sắp xếp mới nhất trước) | Authenticated | `status`: lọc theo trạng thái (`PENDING`, `PAID`, `CANCELLED`)<br>`page`: (default: 0)<br>`size`: (default: 10) |
| `GET` | `/api/v1/orders/{orderId}` | Chi tiết đơn hàng (thông tin ghế, giá vé, trạng thái thanh toán, hạn thanh toán) | Authenticated | — |
| `DELETE` | `/api/v1/orders/{orderId}` | Hủy đơn hàng `PENDING` và tự động giải phóng ghế tương ứng | Authenticated | — |

**Notes:**
- `POST /api/v1/orders`: Trả về `201 Created`. Seat hold phải thuộc về user hiện tại, đang `ACTIVE` và chưa hết hạn.
- `POST /api/v1/orders/{orderId}/pay`: Chuyển Order sang `PAID`, cập nhật `paidAt` và chuyển `SeatHold` sang `CONVERTED`. Tự động sinh danh sách vé kèm `ticketCode`. Đơn hàng không ở trạng thái `PENDING` hoặc đã quá hạn → `400 Bad Request`.
- `GET /api/v1/orders/my`: Sắp xếp theo `createdAt` giảm dần.
- `GET /api/v1/orders/{orderId}`: Đơn hàng không tồn tại hoặc không thuộc user hiện tại → `404 Not Found`. Nếu đơn hàng `PENDING` đã hết hạn, lazy update trạng thái thành `EXPIRED`.
- `DELETE /api/v1/orders/{orderId}`: Chỉ hủy được đơn hàng `PENDING`. Thành công trả về `204 No Content`.

---

## 5. Tickets (2 Endpoints)
| Method | Endpoint | Mô tả | Auth | Query params |
|:---:|---|---|:---:|---|
| `GET` | `/api/v1/tickets/my` | Danh sách vé điện tử đã mua của người dùng | Authenticated | `eventId`: lọc theo sự kiện<br>`status`: lọc theo trạng thái vé (`VALID`, `USED`)<br>`page`: (default: 0)<br>`size`: (default: 10) |
| `GET` | `/api/v1/tickets/{ticketId}` | Chi tiết vé điện tử (sự kiện, khu vực, số hàng, số ghế, mã vé `ticketCode`) | Authenticated | — |

**Notes:**
- Vé điện tử được tạo tự động khi đơn hàng được thanh toán thành công (`PAID`).
- Mã vé điện tử (`ticketCode`) là duy nhất theo mẫu `{orderCode}-T{itemId}`, dùng để định danh và đối soát vé khi tham dự sự kiện.
- Endpoint `GET /api/v1/tickets/{ticketId}` cho phép chủ sở hữu vé (hoặc quản trị viên) xem đầy đủ thông tin vé điện tử.

---

## 6. Admin Management (5 Endpoints)
> Tất cả endpoint `/api/v1/admin/**` đều yêu cầu quyền **ADMIN**.

| Method | Endpoint | Mô tả | Query params |
|:---:|---|---|---|
| `GET` | `/api/v1/admin/events` | Danh sách tất cả sự kiện hệ thống (bao gồm `DRAFT`, `ON_SALE`, `CANCELLED`) | `search`: tìm theo tên<br>`status`: lọc trạng thái<br>`page`: (default: 0)<br>`size`: (default: 12) |
| `POST` | `/api/v1/admin/events` | Tạo sự kiện mới | — |
| `PATCH` | `/api/v1/admin/events/{eventId}` | Cập nhật thông tin sự kiện hoặc thay đổi trạng thái vòng đời (`ON_SALE`, `CANCELLED`) | — |
| `POST` | `/api/v1/admin/events/{eventId}/seat-zones` | Cấu hình khu vực ghế (tên zone, giá tiền, số hàng, số ghế/hàng) và tự động sinh ghế | — |
| `GET` | `/api/v1/admin/orders` | Quản lý toàn bộ đơn hàng trong hệ thống | `eventId`: lọc theo sự kiện<br>`status`: lọc trạng thái<br>`page`: (default: 0)<br>`size`: (default: 10) |

**Notes:**
- `GET /api/v1/admin/events`: Hỗ trợ tìm kiếm theo tên, lọc theo trạng thái sự kiện và phân trang.
- `POST /api/v1/admin/events`: Trả về `201 Created` kèm thông tin sự kiện vừa tạo. Hỗ trợ tạo kèm danh sách `zones` và `seats` ngay trong một request.
- `PATCH /api/v1/admin/events/{eventId}`: Cập nhật thông tin sự kiện hoặc chuyển đổi trạng thái vòng đời (`ON_SALE`, `CANCELLED`).
- `POST /api/v1/admin/events/{eventId}/seat-zones`: Nhận danh sách các zone (tên, giá tiền, danh sách ghế với `rowIndex` và `seatNumber`). Trả về sơ đồ ghế hoàn chỉnh của sự kiện.
- `GET /api/v1/admin/orders`: Sắp xếp theo `createdAt` giảm dần. Hỗ trợ lọc theo `eventId`, theo `status` (`PENDING`, `PAID`, `CANCELLED`), hoặc kết hợp cả hai.

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
