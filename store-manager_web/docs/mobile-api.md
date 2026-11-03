# Mobile API

Các endpoint mobile nằm dưới `/api/v1` để tách contract của Android khỏi API
web hiện tại.

## Cấu hình

Thêm vào `.env` thật:

```env
MOBILE_JWT_SECRET="một-chuỗi-bí-mật-dài-và-ngẫu-nhiên"
```

Nếu chưa khai báo `MOBILE_JWT_SECRET`, code tạm thời dùng `NEXTAUTH_SECRET`.
Production nên khai báo secret riêng.

## Authentication

### Đăng nhập

```http
POST /api/v1/auth/login
Content-Type: application/json
```

```json
{
  "email": "admin@example.com",
  "password": "your-password"
}
```

Response chứa `accessToken`. Các request cần xác thực gửi:

```http
Authorization: Bearer <accessToken>
```

Access token hiện có hạn 15 phút. Refresh token chưa được triển khai ở bước
này; mobile cần đăng nhập lại khi token hết hạn.

### Kiểm tra tài khoản hiện tại

```http
GET /api/v1/me
Authorization: Bearer <accessToken>
```

## Dữ liệu khởi tạo

```http
GET /api/v1/bootstrap
Authorization: Bearer <accessToken>
```

Response gồm categories, products, units, customers và suppliers đang hoạt động.
Mobile dùng response này để khởi tạo Room SQLite.

## Sản phẩm

```http
GET /api/v1/products?page=1&limit=50&search=nuoc
Authorization: Bearer <accessToken>
```

Có thể truyền `updatedAfter` dạng ISO-8601 để lấy các sản phẩm thay đổi sau
một thời điểm:

```http
GET /api/v1/products?updatedAfter=2026-10-29T10:00:00.000Z
```

Giá tiền được trả dưới dạng chuỗi để tránh lỗi floating-point khi Android xử
lý dữ liệu tiền tệ.

## Bán hàng

```http
POST /api/v1/sales
Authorization: Bearer <accessToken>
Content-Type: application/json
```

```json
{
  "operationId": "uuid-do-mobile-tao",
  "customerId": null,
  "items": [
    {
      "productId": "product-id",
      "quantity": 2,
      "unitPrice": 7000
    }
  ],
  "paymentMethod": "CASH",
  "discount": 0,
  "notes": null
}
```

`operationId` phải duy nhất cho một thao tác bán. Khi mobile retry cùng request,
server trả lại kết quả cũ thay vì tạo hóa đơn thứ hai.

Các mã lỗi quan trọng:

- `UNAUTHORIZED`: token thiếu, sai hoặc hết hạn.
- `VALIDATION_ERROR`: payload không hợp lệ.
- `PRODUCT_NOT_FOUND`: sản phẩm không tồn tại hoặc đã ngừng hoạt động.
- `CUSTOMER_NOT_FOUND`: khách hàng không tồn tại hoặc đã ngừng hoạt động.
- `STOCK_CONFLICT`: tồn kho server không đủ hoặc vừa thay đổi.
- `OPERATION_IN_PROGRESS`: operation đang được request khác xử lý.

## Database migration

API bán hàng dùng bảng `sync_operations` để lưu idempotency key và kết quả
đồng bộ. Chạy migration trước khi test `/api/v1/sales`:

```powershell
npm run db:migrate
```

Không dùng `db:push` trên database production nếu chưa kiểm tra migration.
