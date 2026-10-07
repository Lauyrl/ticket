# Overview

REST API dùng để đặt vé sự kiện.

Feature: event search, seat zone, giữ ghế tạm thời.

# Endpoints

## Events
| Method | Endpoint | Mô tả | Auth | Query params |
|--------|----------|-------|------|--------------|
| `GET` | `/api/v1/events` | Danh sách sự kiện (filter, pagination) | Public | `search`: tìm theo tên<br>`city`: lọc theo thành phố<br>`startsAfter`, `startsBefore`: lọc theo thời điểm bắt đầu<br>`endsAfter`, `endsBefore`: lọc theo thời điểm kết thúc<br>`page`: (default: 0, min: 0) |
| `GET` | `/api/v1/events/suggest` | Dùng keywords user đang viết để show gợi ý nhanh trong lúc search | Public | `search`: suggest theo tên |
| `GET` | `/api/v1/events/{eventId}` | Chi tiết một sự kiện | Public | — |

**Notes:**
- `GET .../events`: Kích thước trang cố định là 12
- `GET .../events/{eventId}`: Sự kiện không tồn tại → `404`

<br>

---

<br>

## Seats, seat holds
| Method | Endpoint | Mô tả | Auth | Query params |
|--------|----------|-------|------|--------------|
| `GET` | `/api/v1/events/{eventId}/seats` | Lấy sơ đồ ghế (nhóm zone → seat), thông tin về row có ở trong metadata | Customer | — |
| `POST` | `/api/v1/events/{eventId}/seats/{seatId}/hold` | Tạo một seat hold, chỉ được phép có 1 seat hold ACTIVE hay CONVERTED, seat hold sẽ expire sau một khoảng thời gian | Customer | — |
| `DELETE` | `/api/v1/events/{eventId}/seats/{seatId}/hold` | Bỏ seat hold thủ công | Customer | — |
| `GET` | `/api/v1/events/{eventId}/holds` | Lấy các ghế đang giữ (ACTIVE, chưa hết hạn) của user trong event | Customer | — |

**Notes:**
- Trạng thái khả dụng của ghế được rút ra từ bảng `seat_holds`, không lưu trực tiếp trong `event_seats`. Hold ACTIVE mà quá `expires_at` được coi là ghế trống
- `POST .../hold`: Việc chống nhiều hold trên 1 ghế dựa trên UNIQUE INDEX `ux_seat_holds_taken_seat` trong DB - mỗi ghế chỉ được phép có 1 seat hold *ACTIVE* hay *CONVERTED* (đã bán), seat hold sẽ expire sau một khoảng thời gian (tùy vào event) và được release dựa trên lazy check khi có user muốn tạo hold mới trên cùng seat đó. Insert vi phạm index → `409`
- `POST .../hold`: Ghế không tồn tại hoặc không thuộc event này → `404`. Ghế đã bị giữ hoặc đã bán → `409`
- `DELETE .../hold`: thành công → `204`. Không có hold ACTIVE (chưa hết hạn) của user trên ghế đó → `409` 
- `userId` sẽ lấy từ token xác thực

<br>

## EXAMPLE
| Method | Endpoint | Mô tả | Auth | Query params |
|--------|----------|-------|------|--------------|
| `GET/POST/DELETE` | `...` | ... | Public/Customer/Admin | ... |

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
- Flyway chạy bên trong backend container và kết nối tới PostgreSQL container để chạy các migration khi backend container chạy

<br>

## Tài liệu OpenAPI: http://localhost:8080/swagger-ui/index.html
