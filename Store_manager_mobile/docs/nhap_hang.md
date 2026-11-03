# Nhập hàng (phiếu nhập)

> Hạng mục trong plan: P8 — Nhập hàng
> Ngày hoàn thành: 03/11/2026
> Trạng thái: ✅ Hoàn thành

## 1. Mục đích

Cho phép quản lý ghi nhận hàng nhập từ nhà cung cấp: chọn nhà cung cấp, nhập
từng dòng hàng với số lượng và đơn giá, và lưu phiếu nhập. Khi lưu phiếu, tồn
kho sản phẩm tăng lên và giá nhập được cập nhật. Bám sát giao diện web `/import`.

## 2. Luồng hoạt động

### 2.1. Luồng nhập phiếu

```text
Mở màn hình Nhập hàng (Menu → Nhập hàng)
  → Gọi GET /api/v1/suppliers, GET /api/v1/categories, GET /api/v1/products
    → Chọn nhà cung cấp (bắt buộc)
  → Bấm "Thêm hàng" → nhập: tên sản phẩm, số lượng, đơn giá, danh mục
  → Nhập tên sản phẩm khớp hoàn toàn với sản phẩm có sẵn → dòng gắn productId
  → Nhập tên không khớp → hệ thống tìm sản phẩm tương tự:
      ├─ Có sản phẩm gần giống → mở hộp thoại gợi ý để người dùng chọn
      └─ Không có → đánh dấu dòng sẽ tạo sản phẩm mới
  → Bấm "Lưu phiếu nhập"
      → POST /api/v1/imports
          ├─ Thành công → hiện số phiếu + tổng tiền, xoá form để nhập phiếu mới
          ├─ 400 VALIDATION_ERROR → báo lỗi cụ thể
          ├─ 404 PRODUCT_NOT_FOUND / SUPPLIER_NOT_FOUND → báo lỗi tương ứng
          └─ 401 UNAUTHORIZED → tự refresh token, không đăng xuất oan
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
1. Với mỗi dòng chưa có productId:
      ├─ Tìm sản phẩm trùng tên (không phân biệt hoa thường) → dùng sản phẩm đó
      └─ Không có → tạo Product mới
           (SKU tự sinh, slug tự sinh, giá nhập = đơn giá,
            giá bán = đơn giá × 1.3, tồn kho = 0)
2. Với mỗi dòng: tăng stock, cập nhật importPrice = đơn giá
   và lưu previousImportPrice để hiển thị thay đổi giá
3. Tạo ImportInvoice (invoiceNumber tự sinh `PN-yyyyMMdd-xxxxxx`,
   status = COMPLETED) và các dòng hàng
```

### 2.3. Hộp thoại gợi ý sản phẩm tương tự

```text
Khi người dùng nhập tên sản phẩm không có sản phẩm nào khớp chính xác
  → Tên được cập nhật ngay khi gõ, dòng hàng hiển thị "sẽ tạo sản phẩm mới"
  → Sau khoảng 600ms ngừng gõ, tìm các sản phẩm có tên hoặc SKU chứa
    chuỗi đã nhập (không phân biệt hoa thường), lấy tối đa 5 sản phẩm
  → Nếu có kết quả → mở hộp thoại danh sách gợi ý
      ├─ Chọn một sản phẩm → gắn productId + giá nhập cho dòng đó
      └─ Bấm "Tạo sản phẩm mới" → giữ maSanPham = null để server tạo mới
  → Nếu không có kết quả → đánh dấu dòng sẽ tạo mới ngay
```

### 2.4. Nhánh xử lý lỗi

| Tình huống | Mã lỗi | Xử lý |
|---|---|---|
| Chưa chọn nhà cung cấp | — (kiểm tra client) | Báo nhắc chọn nhà cung cấp |
| Chưa có dòng hàng nào | — (kiểm tra client) | Báo nhắc nhập ít nhất một dòng hàng |
| Số lượng hoặc đơn giá ≤ 0 | — (kiểm tra client) | Báo lỗi, không gửi request |
| Danh sách dòng hàng sai định dạng | 400 VALIDATION_ERROR | Báo lỗi từ server |
| Sản phẩm không tồn tại | 404 PRODUCT_NOT_FOUND | Báo lỗi từ server |
| Nhà cung cấp không tồn tại | 404 SUPPLIER_NOT_FOUND | Báo lỗi từ server |
| Token hết hạn | 401 UNAUTHORIZED | Tự refresh token rồi thử lại |
| Mất mạng | IOException | Báo "Không thể kết nối đến máy chủ" |

## 3. Các file liên quan

| Loại | Đường dẫn | Vai trò |
|---|---|---|
| Backend | `store-manager_web/src/app/api/v1/imports/route.ts` | `POST /api/v1/imports` tạo phiếu nhập (mới) |
| Backend | `store-manager_web/src/app/api/v1/imports/[id]/route.ts` | `GET /api/v1/imports/{id}` chi tiết phiếu (mới) |
| Activity | `app/src/main/java/com/example/store_manager/ManHinhNhapHang.kt` | Màn hình nhập hàng (mới) |
| Adapter | `app/src/main/java/com/example/store_manager/TrinhBayDongHangNhap.kt` | Một dòng hàng trong phiếu (mới) |
| Adapter | `app/src/main/java/com/example/store_manager/TrinhBaySanPhamGoiY.kt` | Danh sách gợi ý sản phẩm (mới) |
| Dịch vụ | `app/src/main/java/com/example/store_manager/du_lieu/DichVuNhapHang.kt` | Gọi API nhập hàng (mới) |
| Model | `app/src/main/java/com/example/store_manager/du_lieu/DoiTuongNhapHang.kt` | `DongHangNhap`, `YeuCauTaoPhieuNhap` (mới) |
| Giao diện | `app/src/main/res/layout/nhap_hang.xml` | Màn hình nhập hàng (mới) |
| Giao diện | `app/src/main/res/layout/dong_hang_nhap.xml` | Một dòng hàng (mới) |
| Giao diện | `app/src/main/res/layout/hop_thoai_goi_y_san_pham.xml` | Hộp thoại gợi ý (mới) |
| Giao diện | `app/src/main/res/layout/dong_goi_y_san_pham.xml` | Một sản phẩm gợi ý (mới) |
| Hằng số | `app/src/main/res/values/strings.xml` | Chuỗi hiển thị |
| Menu | `app/src/main/java/com/example/store_manager/ManHinhCoThanhTab.kt` | Gắn handler `menu_nhap_hang` |
| Manifest | `app/src/main/AndroidManifest.xml` | Khai báo Activity mới |

## 4. Bảng database liên quan

| Bảng | Trường dùng | Ghi chú |
|---|---|---|
| `import_invoices` | `id`, `invoiceNumber`, `supplierId`, `totalAmount`, `notes`, `status`, `createdById` | `status = COMPLETED` khi tạo từ mobile |
| `import_invoice_items` | `importInvoiceId`, `productId`, `quantity`, `unitPrice`, `previousUnitPrice`, `totalPrice` | `previousUnitPrice` lưu giá nhập cũ để hiện thay đổi |
| `products` | `stock`, `importPrice`, `previousImportPrice` | Tăng tồn kho, cập nhật giá nhập |
| `suppliers` | `id`, `name`, `isActive` | Nhà cung cấp của phiếu |
| `categories` | `id`, `name` | Danh mục khi tạo sản phẩm mới |
| `users` | `id` | `createdById` của phiếu |

## 5. Endpoint API được dùng

| Method | Đường dẫn | Mục đích |
|---|---|---|
| GET | `/api/v1/suppliers` | Danh sách nhà cung cấp (đã có) |
| GET | `/api/v1/products` | Danh sách sản phẩm để đối chiếu tên (đã có) |
| GET | `/api/v1/categories` | Danh sách danh mục (đã có) |
| POST | `/api/v1/imports` | Tạo phiếu nhập (mới) |
| GET | `/api/v1/imports/{id}` | Chi tiết phiếu nhập (mới, dùng cho P10) |

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

## 6. Ghi chú kỹ thuật

- Tên file và id trong Android dùng tiếng Việt không dấu. Vì Android không cho
  phép ký tự `ý` trong tên tài nguyên, các id liên quan gợi ý dùng `goi_y`.
- `TrinhBayDongHangNhap` không gọi `notifyDataSetChanged()` khi người dùng đang
  nhập, tránh mất focus và mất dữ liệu đang gõ (giống `TrinhBayGioHang`).
  Dòng đang được sửa được đánh dấu bằng `maDongDangSua` và bỏ qua hoàn toàn
  trong bước đánh dấu lại danh sách.
- Số lượng, đơn giá và tên sản phẩm được ghi vào danh sách ngay khi người
  dùng gõ (`doAfterTextChanged`), không đợi mất focus. Nhờ vậy khi người dùng
  nhập xong bấm thẳng "Lưu phiếu nhập" thì dữ liệu không bị bỏ sót.
- Thành tiền của dòng được cập nhật trực tiếp trên giao diện khi số lượng
  hoặc đơn giá thay đổi, không phụ thuộc vào vòng render lại danh sách.
- Hộp thoại gợi ý chỉ mở sau 600ms ngừng gõ, tránh popup lặp liên tục khi
  người dùng đang gõ tên sản phẩm. Tác vụ hẹn giờ được huỷ khi dòng hàng bị
  tái sử dụng lại (`onViewRecycled`).
- `Spinner` danh mục chỉ gán listener sau khi set selection, tránh sự kiện
  chọn phát sinh từ việc gán dữ liệu bị hiểu nhầm là thao tác của người dùng.
- Số phiếu nhập sinh ở server dạng `PN-yyyyMMdd-xxxxxx`, có vòng lặp kiểm tra
  trùng theo ràng buộc unique của database.

## 7. Đối chiếu với Web

| Chức năng | Vị trí trên Web | Trạng thái mobile |
|---|---|---|
| Chọn nhà cung cấp | `/import` | ✅ Đã làm |
| Bảng dòng hàng nhập | `/import` | ✅ Đã làm |
| Thêm / xoá dòng hàng | `/import` | ✅ Đã làm |
| Gợi ý sản phẩm tương tự | `/import` | ✅ Đã làm |
| Tự tạo sản phẩm mới | `/import` | ✅ Đã làm |
| Lưu phiếu, tăng tồn kho, cập nhật giá nhập | `/import` | ✅ Đã làm |
| Nút OCR đính kèm | `/import` | ❌ Để dành P9 |
| Lịch sử nhập, huỷ phiếu | `/import/history` | ❌ Để dành P10 |
