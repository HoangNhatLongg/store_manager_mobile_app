# Lịch sử bán hàng

> Hạng mục trong plan: P11 — Lịch sử bán hàng
> Ngày hoàn thành: (đang cập nhật)
> Trạng thái: Chưa bắt đầu

## 1. Mục đích

Cho phép tra cứu các hoá đơn bán đã tạo từ màn hình POS, xem chi tiết từng
hoá đơn để đối chiếu khi có khiếu nại. Bám sát API `/api/sales` của web (web
hiện chưa có trang lịch sử bán riêng, mobile bổ sung theo quy ước menu).

## 2. Luồng hoạt động

### 2.1. Luồng xem lịch sử

```text
Mở màn hình Lịch sử bán hàng
  → Gọi GET /api/v1/sales/history?page=&limit=
  → Hiển thị danh sách hoá đơn:
      số hoá đơn | khách hàng | tổng tiền | phương thức thanh toán | trạng thái | ngày tạo
  → Bấm một hoá đơn → màn hình chi tiết
```

### 2.2. Luồng xem chi tiết hoá đơn

```text
GET /api/v1/sales/history/:id
  → Hiển thị: số hoá đơn, khách hàng, phương thức thanh toán, ghi chú, ngày tạo
  → Bảng dòng hàng: tên sản phẩm | số lượng | đơn giá | thành tiền
  → Tạm tính | giảm giá | tổng cộng
```

### 2.3. Nhánh xử lý lỗi

| Tình huống | Mã lỗi | Xử lý |
|---|---|---|
| Hoá đơn không tồn tại | 404 NOT_FOUND | Báo không tìm thấy, quay lại danh sách |
| Token hết hạn | 401 UNAUTHORIZED | Xoá token, về màn hình đăng nhập |
| Lỗi server | 500 SERVER_ERROR | Giữ nguyên danh sách, cho thử lại |

## 3. Các file liên quan

| Loại | Đường dẫn | Vai trò |
|---|---|---|
| Activity | `app/src/main/java/com/example/store_manager/ManHinhLichSuBan.kt` | Danh sách hoá đơn bán (mới) |
| Activity | `app/src/main/java/com/example/store_manager/ManHinhChiTietHoaDon.kt` | Tái dùng cho chi tiết hoá đơn đã lưu (đã có) |
| Adapter | `app/src/main/java/com/example/store_manager/TrinhBayHoaDonBan.kt` | Một dòng hoá đơn (mới) |
| Dịch vụ | `app/src/main/java/com/example/store_manager/du_lieu/DichVuBanHang.kt` | Gọi API lịch sử bán (mới) |
| Model | `app/src/main/java/com/example/store_manager/du_lieu/DoiTuongBanHang.kt` | `HoaDonBan`, `DongHoaDonBan` (mới) |
| Giao diện | `app/src/main/res/layout/lich_su_ban.xml` | Danh sách hoá đơn (mới) |
| Giao diện | `app/src/main/res/layout/dong_hoa_don_ban.xml` | Một dòng hoá đơn (mới) |
| Hằng số | `app/src/main/res/values/strings.xml` | Chuỗi hiển thị |
| Menu | `app/src/main/java/com/example/store_manager/ManHinhCoThanhTab.kt` | Gắn handler `menu_lich_su_ban` |
| Manifest | `app/src/main/AndroidManifest.xml` | Khai báo Activity mới |

## 4. Bảng database liên quan

| Bảng | Trường dùng | Ghi chú |
|---|---|---|
| `sale_invoices` | `id`, `invoiceNumber`, `customerId`, `subtotal`, `discount`, `totalAmount`, `paymentMethod`, `notes`, `status`, `createdAt` | Danh sách hoá đơn |
| `sale_invoice_items` | `saleInvoiceId`, `productId`, `quantity`, `unitPrice`, `importPrice`, `totalPrice` | Chi tiết dòng hàng |
| `customers` | `id`, `name` | Tên khách hàng |
| `products` | `id`, `name`, `sku` | Tên sản phẩm |

## 5. Endpoint API được dùng

| Method | Đường dẫn | Mục đích |
|---|---|---|
| GET | `/api/v1/sales/history` | Danh sách hoá đơn bán (đã có) |
| GET | `/api/v1/sales/history/:id` | Chi tiết một hoá đơn (cần tạo mới) |

## 6. Đối chiếu với Web

| Chức năng | Vị trí trên Web | Trạng thái mobile |
|---|---|---|
| Danh sách hoá đơn bán | Chưa có trang riêng | 🆕 Mobile bổ sung |
| Chi tiết hoá đơn bán | `/api/sales/:id` | 🆕 Mobile bổ sung |
| Màn hình thanh toán thành công | `/sales` | ❌ Cần làm (P1.9) |
