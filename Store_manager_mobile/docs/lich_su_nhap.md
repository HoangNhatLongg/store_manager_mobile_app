# Lịch sử nhập hàng

> Hạng mục trong plan: P10 — Lịch sử nhập hàng
> Ngày hoàn thành: (đang cập nhật)
> Trạng thái: Chưa bắt đầu

## 1. Mục đích

Cho phép tra cứu các phiếu nhập đã tạo, xem chi tiết từng phiếu và huỷ phiếu
khi nhập sai để hệ thống hoàn trả tồn kho. Bám sát giao diện web
`/import/history` và `/import/[id]`.

## 2. Luồng hoạt động

### 2.1. Luồng xem lịch sử

```text
Mở màn hình Lịch sử nhập hàng
  → Gọi GET /api/v1/imports/history?page=&limit=
  → Hiển thị danh sách phiếu: số phiếu | nhà cung cấp | tổng tiền | trạng thái | ngày tạo
  → Lọc theo trạng thái: tất cả / PENDING / COMPLETED / CANCELLED
  → Bấm một phiếu → màn hình chi tiết
```

### 2.2. Luồng xem chi tiết phiếu

```text
GET /api/v1/imports/:id
  → Hiển thị: số phiếu, nhà cung cấp, ngày tạo, tổng tiền, trạng thái, ghi chú
  → Bảng dòng hàng: tên sản phẩm | số lượng | đơn giá | thành tiền
  → Nếu đơn giá khác giá nhập trước đó → hiển thị dòng cảnh báo thay đổi giá
```

### 2.3. Luồng huỷ phiếu nhập

```text
Trên phiếu có trạng thái PENDING → hiện nút "Huỷ phiếu"
  → Bấm → hộp thoại xác nhận nêu rõ sẽ hoàn trả tồn kho
  → Bấm "Huỷ"
      → POST /api/v1/imports/:id/cancel
          ├─ Thành công → trạng thái thành CANCELLED, tồn kho đã hoàn trả
          ├─ 400 → phiếu đã COMPLETED hoặc CANCELLED, không thể huỷ
          └─ 401 → xoá token, về đăng nhập
```

Quy tắc quan trọng: chỉ phiếu `PENDING` mới huỷ được. Phiếu `COMPLETED` đã
làm thay đổi tồn kho nên phải xử lý bằng cách khác (bán hàng bù trừ, hoặc sửa
tồn kho trực tiếp).

### 2.4. Nhánh xử lý lỗi

| Tình huống | Mã lỗi | Xử lý |
|---|---|---|
| Phiếu không tồn tại | 404 NOT_FOUND | Báo không tìm thấy phiếu, quay lại danh sách |
| Không huỷ được phiếu | 400 VALIDATION_ERROR | Báo chỉ huỷ được phiếu đang chờ |
| Token hết hạn | 401 UNAUTHORIZED | Xoá token, về màn hình đăng nhập |
| Lỗi server | 500 SERVER_ERROR | Giữ nguyên màn hình, cho thử lại |

## 3. Các file liên quan

| Loại | Đường dẫn | Vai trò |
|---|---|---|
| Activity | `app/src/main/java/com/example/store_manager/ManHinhLichSuNhap.kt` | Danh sách phiếu nhập (mới) |
| Activity | `app/src/main/java/com/example/store_manager/ManHinhChiTietPhieuNhap.kt` | Chi tiết phiếu + huỷ phiếu (mới, dùng chung với P8) |
| Adapter | `app/src/main/java/com/example/store_manager/TrinhBayPhieuNhap.kt` | Một dòng phiếu nhập (mới) |
| Adapter | `app/src/main/java/com/example/store_manager/TrinhBayDongHangNhap.kt` | Một dòng hàng trong chi tiết phiếu (mới, dùng chung với P8) |
| Dịch vụ | `app/src/main/java/com/example/store_manager/du_lieu/DichVuNhapHang.kt` | Gọi API lịch sử và huỷ phiếu (mới) |
| Model | `app/src/main/java/com/example/store_manager/du_lieu/DoiTuongNhapHang.kt` | `PhieuNhap`, `DongHangNhap` (mới) |
| Giao diện | `app/src/main/res/layout/lich_su_nhap.xml` | Danh sách phiếu (mới) |
| Giao diện | `app/src/main/res/layout/chi_tiet_phieu_nhap.xml` | Chi tiết phiếu (mới) |
| Giao diện | `app/src/main/res/layout/dong_phieu_nhap.xml` | Một dòng phiếu (mới) |
| Hằng số | `app/src/main/res/values/strings.xml` | Chuỗi hiển thị |
| Menu | `app/src/main/java/com/example/store_manager/ManHinhCoThanhTab.kt` | Gắn handler `menu_lich_su_nhap` |
| Manifest | `app/src/main/AndroidManifest.xml` | Khai báo Activity mới |

## 4. Bảng database liên quan

| Bảng | Trường dùng | Ghi chú |
|---|---|---|
| `import_invoices` | `id`, `invoiceNumber`, `supplierId`, `totalAmount`, `notes`, `status`, `createdAt` | Lọc theo `status` |
| `import_invoice_items` | `importInvoiceId`, `productId`, `quantity`, `unitPrice`, `previousUnitPrice`, `totalPrice` | Chi tiết dòng hàng |
| `suppliers` | `id`, `name` | Tên nhà cung cấp hiển thị |
| `products` | `id`, `name`, `stock` | Tên sản phẩm và tồn kho sau khi huỷ |

## 5. Endpoint API được dùng

| Method | Đường dẫn | Mục đích |
|---|---|---|
| GET | `/api/v1/imports/history` | Danh sách phiếu nhập (đã có) |
| GET | `/api/v1/imports/:id` | Chi tiết một phiếu (cần tạo mới) |
| POST | `/api/v1/imports/:id/cancel` | Huỷ phiếu và hoàn trả tồn kho (cần tạo mới) |

## 6. Đối chiếu với Web

| Chức năng | Vị trí trên Web | Trạng thái mobile |
|---|---|---|
| Danh sách phiếu nhập | `/import/history` | ❌ Cần làm |
| Lọc theo trạng thái | `/import/history` | ❌ Cần làm |
| Tìm kiếm, phân trang | `/import/history` | ❌ Cần làm |
| Chi tiết phiếu | `/import/[id]` | ❌ Cần làm |
| Cảnh báo thay đổi giá nhập | `/import/[id]` | ❌ Cần làm |
| Huỷ phiếu và hoàn trả tồn kho | `/import/[id]` | ❌ Cần làm |
