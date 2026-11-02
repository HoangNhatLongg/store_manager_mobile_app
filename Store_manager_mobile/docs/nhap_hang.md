# Nhập hàng (phiếu nhập)

> Hạng mục trong plan: P8 — Nhập hàng
> Ngày hoàn thành: (đang cập nhật)
> Trạng thái: Chưa bắt đầu

## 1. Mục đích

Cho phép quản lý ghi nhận hàng nhập từ nhà cung cấp: chọn nhà cung cấp, nhập
từng dòng hàng với số lượng và đơn giá, và lưu phiếu nhập. Khi lưu phiếu, tồn
kho sản phẩm tăng lên và giá nhập được cập nhật. Bám sát giao diện web `/import`.

## 2. Luồng hoạt động

### 2.1. Luồng nhập phiếu

```text
Mở màn hình Nhập hàng
  → Gọi GET /api/v1/suppliers, GET /api/v1/products, GET /api/v1/categories
  → Chọn nhà cung cấp (bắt buộc)
  → Bấm "Thêm dòng hàng" → nhập: tên sản phẩm, số lượng, đơn giá, danh mục
  → Nhập tên sản phẩm khớp hoàn toàn với sản phẩm có sẵn → dòng gắn productId
  → Nhập tên không khớp → hệ thống tìm sản phẩm tương tự:
      ├─ Có sản phẩm gần giống → mở hộp thoại gợi ý để người dùng chọn
      └─ Không có → đánh dấu dòng sẽ tạo sản phẩm mới
  → Bấm "Lưu phiếu"
      → POST /api/v1/imports
          ├─ Thành công → xoá form, tải lại sản phẩm (tồn kho đã tăng), sang màn hình chi tiết phiếu
          ├─ 400 VALIDATION_ERROR → báo lỗi cụ thể dòng hàng
          └─ 401 UNAUTHORIZED → xoá token, về đăng nhập
```

### 2.2. Quy tắc nghiệp vụ

```text
số lượng     > 0
đơn giá      > 0
tạm tính     = số lượng × đơn giá
tổng phiếu   = Σ tạm tính của các dòng
```

Khi lưu phiếu, trong một transaction:

```text
1. Với mỗi dòng chưa có productId → tạo Product mới
      (SKU tự sinh, slug tự sinh, giá nhập = đơn giá, tồn kho = số lượng)
2. Với mỗi dòng đã có productId → tăng stock, cập nhật importPrice = đơn giá
   và lưu previousImportPrice để hiển thị thay đổi giá
3. Tạo ImportInvoice (invoiceNumber tự sinh, status = COMPLETED) và các dòng hàng
```

### 2.3. Hộp thoại gợi ý sản phẩm tương tự

```text
Khi người dùng nhập tên sản phẩm không có sản phẩm nào khớp chính xác
  → Tìm các sản phẩm có tên chứa chuỗi đã nhập (không phân biệt hoa thường)
  → Nếu có kết quả → mở hộp thoại danh sách gợi ý
      ├─ Chọn một sản phẩm → gắn productId cho dòng đó
      └─ Bấm "Tạo sản phẩm mới" → đánh dấu dòng sẽ tạo mới
  → Nếu không có kết quả → đánh dấu dòng sẽ tạo mới ngay
```

### 2.4. Nhánh xử lý lỗi

| Tình huống | Mã lỗi | Xử lý |
|---|---|---|
| Chưa chọn nhà cung cấp | 400 VALIDATION_ERROR | Báo nhắc chọn nhà cung cấp |
| Số lượng hoặc đơn giá không hợp lệ | 400 VALIDATION_ERROR | Đánh dấu dòng lỗi, không gửi request |
| Trùng tên hoặc SKU khi tạo sản phẩm mới | 400 DUPLICATE | Báo dòng lỗi, cho sửa tên |
| Token hết hạn | 401 UNAUTHORIZED | Xoá token, về màn hình đăng nhập |
| Lỗi server | 500 SERVER_ERROR | Giữ nguyên nội dung form để thử lại |

## 3. Các file liên quan

| Loại | Đường dẫn | Vai trò |
|---|---|---|
| Activity | `app/src/main/java/com/example/store_manager/ManHinhNhapHang.kt` | Màn hình nhập hàng (mới) |
| Activity | `app/src/main/java/com/example/store_manager/ManHinhChiTietPhieuNhap.kt` | Chi tiết phiếu nhập sau khi lưu (mới) |
| Adapter | `app/src/main/java/com/example/store_manager/TrinhBayDongHangNhap.kt` | Một dòng hàng trong phiếu (mới) |
| Adapter | `app/src/main/java/com/example/store_manager/TrinhBaySanPhamGoiY.kt` | Danh sách gợi ý sản phẩm (mới) |
| Dịch vụ | `app/src/main/java/com/example/store_manager/du_lieu/DichVuNhapHang.kt` | Gọi API nhập hàng (mới) |
| Model | `app/src/main/java/com/example/store_manager/du_lieu/DoiTuongNhapHang.kt` | `DongHangNhap`, `YeuCauTaoPhieuNhap` (mới) |
| Giao diện | `app/src/main/res/layout/nhap_hang.xml` | Màn hình nhập hàng (mới) |
| Giao diện | `app/src/main/res/layout/dong_hang_nhap.xml` | Một dòng hàng (mới) |
| Giao diện | `app/src/main/res/layout/hop_thoai_goi_ý_san_pham.xml` | Hộp thoại gợi ý (mới) |
| Hằng số | `app/src/main/res/values/strings.xml` | Chuỗi hiển thị |
| Menu | `app/src/main/java/com/example/store_manager/ManHinhCoThanhTab.kt` | Gắn handler `menu_nhap_hang` |
| Manifest | `app/src/main/AndroidManifest.xml` | Khai báo Activity mới |

## 4. Bảng database liên quan

| Bảng | Trường dùng | Ghi chú |
|---|---|---|
| `import_invoices` | `id`, `invoiceNumber`, `supplierId`, `totalAmount`, `notes`, `status`, `createdById` | `status` mặc định `PENDING` |
| `import_invoice_items` | `importInvoiceId`, `productId`, `quantity`, `unitPrice`, `previousUnitPrice`, `totalPrice` | `previousUnitPrice` lưu giá nhập cũ để hiện thay đổi |
| `products` | `stock`, `importPrice`, `previousImportPrice` | Tăng tồn kho, cập nhật giá nhập |
| `suppliers` | `id`, `name` | Nhà cung cấp của phiếu |
| `categories` | `id`, `name` | Danh mục khi tạo sản phẩm mới |
| `users` | `id` | `createdById` của phiếu |

## 5. Endpoint API được dùng

| Method | Đường dẫn | Mục đích |
|---|---|---|
| GET | `/api/v1/suppliers` | Danh sách nhà cung cấp (đã có) |
| GET | `/api/v1/products` | Danh sách sản phẩm để đối chiếu tên (đã có) |
| GET | `/api/v1/categories` | Danh sách danh mục (đã có) |
| POST | `/api/v1/imports` | Tạo phiếu nhập (cần tạo mới) |

Payload mẫu của `POST /api/v1/imports`:

```json
{
  "supplierId": "supplier-id",
  "notes": null,
  "items": [
    {
      "productId": null,
      "productName": "Coca Cola",
      "categoryId": "category-id",
      "quantity": 24,
      "unitPrice": 12000
    }
  ]
}
```

## 6. Đối chiếu với Web

| Chức năng | Vị trí trên Web | Trạng thái mobile |
|---|---|---|
| Chọn nhà cung cấp | `/import` | ❌ Cần làm |
| Bảng dòng hàng nhập | `/import` | ❌ Cần làm |
| Thêm / xoá dòng hàng | `/import` | ❌ Cần làm |
| Gợi ý sản phẩm tương tự | `/import` | ❌ Cần làm |
| Tự tạo sản phẩm mới | `/import` | ❌ Cần làm |
| Lưu phiếu, tăng tồn kho, cập nhật giá nhập | `/import` | ❌ Cần làm |
| Nút OCR đính kèm | `/import` | ❌ Cần làm (P9) |
