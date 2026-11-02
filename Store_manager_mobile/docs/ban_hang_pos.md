# Bán hàng POS

> Hạng mục trong plan: P1 — Bán hàng POS (tạo hoá đơn thật)
> Ngày hoàn thành: 02/11/2026
> Trạng thái: ✅ Hoàn thành

## 1. Mục đích

Cho phép nhân viên bán hàng tại cửa hàng: chọn sản phẩm theo đơn vị bán, tính
tiền, thu tiền và tạo hoá đơn thật trên server. Đây là màn hình dùng nhiều
nhất trong ngày nên ưu tiên bám sát giao diện web `/sales`.

## 2. Luồng hoạt động

### 2.1. Luồng chọn sản phẩm vào giỏ

```text
Mở tab Bán hàng
  → Gọi GET /api/v1/products (tìm kiếm + lọc danh mục)
    → Bấm một sản phẩm
      ├─ Sản phẩm KHÔNG có đơn vị → thêm thẳng vào giỏ theo đơn vị cơ bản
      ├─ Sản phẩm CHỈ có 1 đơn vị → thêm thẳng vào giỏ kèm giá của đơn vị đó
      └─ Sản phẩm CÓ nhiều đơn vị → mở hộp thoại chọn đơn vị
           → Hiển thị: tên đơn vị, giá, hệ số quy đổi, số lượng tối đa
           → Đơn vị không đủ tồn kho bị làm mờ và không bấm được
           → Chọn đơn vị → thêm vào giỏ kèm giá của đơn vị đó
  → Giỏ cập nhật tổng tiền, tồn kho còn lại hiển thị ngay trên danh sách
```

Ghi chú: cùng một sản phẩm nhưng khác đơn vị bán sẽ nằm thành **hai dòng
riêng** trong giỏ, vì giá bán và số lượng quy đổi khác nhau. Khoá duy nhất của
một dòng là `productId + unitId` (xem `MatHangGioHang.khoaDong`).

### 2.2. Luồng thanh toán

```text
Bấm "Tính tiền"
  → Màn hình chi tiết hoá đơn (ManHinhChiTietHoaDon)
  → Hiển thị danh sách hàng hoá đang bán (tên, đơn vị, số lượng, thành tiền)
  → Chọn khách hàng (tuỳ chọn, có mục "Khách lẻ")
  → Chọn phương thức thanh toán: tiền mặt / chuyển khoản / thẻ / ví điện tử
  → Nhập ghi chú hoá đơn (tuỳ chọn)
  → Nhập chiết khấu (%) và giảm giá (VNĐ)
  → Nếu thanh toán tiền mặt: nhập tiền khách đưa → hiện tiền thừa / thiếu
  → Nếu tiền đưa < tổng cộng → chặn, yêu cầu nhập lại
  → Bấm "Xác nhận thanh toán"
      → Sinh operationId (UUID)
      → POST /api/v1/sales
          ├─ 201 SYNCED → màn hình thành công (số hoá đơn, tổng tiền, tiền thừa)
          ├─ 409 STOCK_CONFLICT → báo thiếu tồn kho, giữ nguyên giỏ
          ├─ 400 VALIDATION_ERROR → hiện lỗi ngay trên màn hình
          └─ 401 UNAUTHORIZED → xoá token, về đăng nhập
  → Bấm "Hoàn tất và bán tiếp"
      → Màn hình thành công trả kết quả RESULT_OK về ManHinhBanHang
      → ManHinhBanHang xoá giỏ hàng và tải lại tồn kho từ server
```

### 2.3. Công thức quy đổi đơn vị

```text
tồn kho theo đơn vị bán = product.stock ÷ productUnit.conversionQty
số lượng tối đa           = tồn kho theo đơn vị bán − số lượng đã có trong giỏ
giá một đơn vị bán        = productUnit.price
tạm tính                  = Σ (số lượng × đơn giá)
tiền giảm                 = min(tạm tính, tạm tính × chiết khấu% ÷ 100 + giảm giá)
tổng cộng                 = tạm tính − tiền giảm
tiền thừa                 = tiền khách đưa − tổng cộng
số lượng gửi lên server   = số lượng theo đơn vị bán
conversionQty gửi lên     = số đơn vị cơ bản trong 1 đơn vị bán
```

Lưu ý: tồn kho trong database luôn tính theo **đơn vị cơ bản**. Vì vậy backend
nhận thêm trường `conversionQty` trong mỗi dòng hàng và tự quy đổi:

```text
số lượng đơn vị cơ bản = quantity × conversionQty
tồn kho bị trừ         = số lượng đơn vị cơ bản
đơn giá lưu trong DB   = tổng tiền dòng ÷ số lượng đơn vị cơ bản
```

Nhờ vậy công thức lợi nhuận trên web vẫn đúng:
`lợi nhuận = tổng tiền − (giá nhập đơn vị cơ bản × số lượng đơn vị cơ bản)`.

### 2.4. Nhánh xử lý lỗi

| Tình huống | Mã lỗi | Xử lý |
|---|---|---|
| Token hết hạn | 401 UNAUTHORIZED | Xoá token, chuyển về `DangNhap` |
| Không đủ tồn kho | 409 STOCK_CONFLICT | Hiện thông báo ngay trên màn hình thanh toán, giữ nguyên giỏ |
| Dữ liệu không hợp lệ | 400 VALIDATION_ERROR | Hiện thông báo ngay trên màn hình thanh toán |
| Lỗi server | 500 SERVER_ERROR | Cho phép bấm lại nút xác nhận, giữ nguyên `operationId` |
| Mất mạng | IOException | Báo mất mạng, giữ nguyên giỏ để thử lại |

Điểm quan trọng: khi retry phải dùng **cùng một `operationId`** để server không
tạo hoá đơn trùng. `ManHinhChiTietHoaDon` sinh `operationId` một lần khi mở
màn hình, lưu vào `savedInstanceState` để không đổi khi xoay máy, và dùng lại
cho mọi lần bấm "Xác nhận thanh toán".

### 2.5. Nhập số lượng trực tiếp trong giỏ hàng

Mỗi dòng trong giỏ có một ô nhập số lượng nhanh, bên cạnh hai nút `-` và `+`.

```text
Bấm vào ô số lượng
  → Ô nhập được focus, toàn bộ nội dung được bôi đen để gõ đè (selectAllOnFocus)
  → Người dùng gõ số lượng mới
  → Bấm nút `-` / `+`  → tăng giảm theo bước 1
  → Rời khỏi ô nhập (chạm chỗ khác) → lấy giá trị đã gõ
  → Bấm phím Enter trên bàn phím  → lấy giá trị đã gõ
```

Quy tắc xử lý giá trị nhập:

| Trường hợp | Xử lý |
|---|---|
| Số lượng hợp lệ và ≤ tồn kho | Cập nhật giỏ, tổng tiền và tồn kho còn lại |
| Số lượng vượt tồn kho | Giới hạn lại về tối đa, hiện thông báo cho người dùng |
| Ô nhập trống | Giữ nguyên số lượng cũ, **không** xoá dòng khỏi giỏ |
| Nhập số không hợp lệ | Giữ nguyên số lượng cũ |
| Số lượng bằng 0 | Xoá dòng khỏi giỏ hàng |

### 2.6. Lưu ý kỹ thuật của ô nhập số lượng

Ô nhập nằm trong `RecyclerView`, nên có hai lỗi thường gặp đã được xử lý:

1. **Không được gọi `notifyDataSetChanged()` khi người dùng đang nhập.**
   Lệnh này huỷ toàn bộ ViewHolder đang hiển thị, tạo lại `EditText` mới và
   làm mất nội dung vừa gõ cùng con trỏ nhấp nháy. Vì vậy `TrinhBayGioHang`
   dùng `notifyItemChanged` / `notifyItemRangeInserted` / `notifyItemRangeRemoved`
   thay vì `notifyDataSetChanged`.

2. **Không gán lại giá trị ô nhập khi đang sửa.** Adapter chỉ gọi `setText`
   khi giá trị hiển thị khác giá trị trong dữ liệu, và bỏ qua khi người dùng
   đang giữ focus trong ô nhập.

Ngoài ra `số lượng tối đa` được tính bằng công thức:

```text
tồn kho gốc − Σ(số lượng quy đổi của các dòng khác cùng sản phẩm)
──────────────────────────────────────────────────────────────────
                    hệ số quy đổi của đơn vị bán
```

Dòng đang sửa **không** bị trừ khỏi tồn kho, vì số lượng mới sẽ thay thế cho
số lượng cũ. Nếu trừ luôn dòng đang sửa thì người dùng nhập số lượng hợp lệ
vẫn bị giới hạn thấp hơn thực tế.


## 3. Các file liên quan

| Loại | Đường dẫn | Vai trò |
|---|---|---|
| Activity | `app/src/main/java/com/example/store_manager/ManHinhBanHang.kt` | Màn hình POS, danh sách sản phẩm và giỏ hàng |
| Activity | `app/src/main/java/com/example/store_manager/ManHinhChiTietHoaDon.kt` | Màn hình thanh toán và xác nhận |
| Activity | `app/src/main/java/com/example/store_manager/ManHinhHoaDonThanhCong.kt` | Màn hình kết quả thanh toán (mới) |
| Adapter | `app/src/main/java/com/example/store_manager/TrinhBaySanPhamBan.kt` | Danh sách sản phẩm bên dưới |
| Adapter | `app/src/main/java/com/example/store_manager/TrinhBayGioHang.kt` | Danh sách dòng trong giỏ |
| Adapter | `app/src/main/java/com/example/store_manager/TrinhBayDonViBan.kt` | Danh sách đơn vị trong hộp thoại chọn (mới) |
| Adapter | `app/src/main/java/com/example/store_manager/TrinhBayDongHoaDon.kt` | Danh sách dòng hàng trên màn hình thanh toán (mới) |
| Model | `app/src/main/java/com/example/store_manager/du_lieu/DoiTuongSanPham.kt` | `MatHangGioHang` (có `donViBan`, `heSoQuyDoi`, `khoaDong`) |
| Model | `app/src/main/java/com/example/store_manager/du_lieu/DoiTuongHoaDon.kt` | `YeuCauTaoHoaDon`, `DongHangHoaDon`, `HoaDonDaTao`, `PhuongThucThanhToan` (mới) |
| Dịch vụ | `app/src/main/java/com/example/store_manager/du_lieu/DichVuBanHang.kt` | Gọi `POST /api/v1/sales`, `GET /api/v1/customers` (mới) |
| Dịch vụ | `app/src/main/java/com/example/store_manager/du_lieu/DichVuSanPham.kt` | Lấy sản phẩm và đơn vị |
| Giao diện | `app/src/main/res/layout/ban_hang.xml` | Khung màn hình POS |
| Giao diện | `app/src/main/res/layout/chi_tiet_hoa_don.xml` | Màn hình thanh toán |
| Giao diện | `app/src/main/res/layout/hop_thoai_don_vi_ban.xml` | Hộp thoại chọn đơn vị bán (mới) |
| Giao diện | `app/src/main/res/layout/dong_don_vi_ban.xml` | Một dòng đơn vị trong hộp thoại (mới) |
| Giao diện | `app/src/main/res/layout/hoa_don_thanh_cong.xml` | Màn hình thành công (mới) |
| Giao diện | `app/src/main/res/layout/dong_dong_hoa_don.xml` | Một dòng hàng trên màn hình thanh toán (mới) |
| Giao diện | `app/src/main/res/layout/dong_gio_hang.xml` | Một dòng trong giỏ (có cột đơn vị bán) |
| Hằng số | `app/src/main/res/values/strings.xml` | Chuỗi hiển thị |
| Manifest | `app/src/main/AndroidManifest.xml` | Khai báo Activity mới |

### 3.1. File phía web

| Loại | Đường dẫn | Vai trò |
|---|---|---|
| API | `store-manager_web/src/app/api/v1/sales/route.ts` | Tạo hoá đơn cho mobile, hỗ trợ `conversionQty` |
| API | `store-manager_web/src/app/api/customers/route.ts` | Tạo hoá đơn từ web, hỗ trợ `conversionQty` |
| API | `store-manager_web/src/app/api/v1/customers/route.ts` | Danh sách khách hàng cho mobile |
| API | `store-manager_web/src/app/api/v1/products/route.ts` | Danh sách sản phẩm kèm đơn vị bán |
| Trang | `store-manager_web/src/app/sales/page.tsx` | Màn hình POS trên web (tham chiếu giao diện) |

## 4. Bảng database liên quan

| Bảng | Trường dùng | Ghi chú |
|---|---|---|
| `products` | `id`, `name`, `stock`, `sellPrice`, `importPrice`, `isActive` | Tồn kho theo đơn vị cơ bản |
| `product_units` | `productId`, `unitId`, `conversionQty`, `price`, `isDefault` | Quy đổi và giá theo đơn vị bán |
| `units` | `id`, `name`, `abbreviation`, `isBaseUnit` | Thông tin hiển thị đơn vị |
| `sale_invoices` | `invoiceNumber`, `customerId`, `subtotal`, `discount`, `totalAmount`, `paymentMethod`, `notes`, `status` | Hoá đơn bán |
| `sale_invoice_items` | `productId`, `quantity`, `unitPrice`, `importPrice`, `totalPrice` | Dòng hàng, lưu theo đơn vị cơ bản |
| `customers` | `id`, `name`, `phone` | Khách hàng tuỳ chọn |
| `sync_operations` | `operationId`, `entityType`, `status`, `result`, `errorCode` | Idempotency của thao tác bán |

## 5. Endpoint API được dùng

| Method | Đường dẫn | Mục đích |
|---|---|---|
| GET | `/api/v1/products` | Danh sách sản phẩm kèm đơn vị bán (`units` có sẵn trong response) |
| GET | `/api/v1/customers` | Danh sách khách hàng đang hoạt động |
| POST | `/api/v1/sales` | Tạo hoá đơn bán (bắt buộc `operationId`) |

Payload mẫu của `POST /api/v1/sales`:

```json
{
  "operationId": "uuid-do-mobile-tao",
  "customerId": null,
  "items": [
    {
      "productId": "product-id",
      "quantity": 2,
      "unitPrice": 7000,
      "conversionQty": 24
    }
  ],
  "paymentMethod": "CASH",
  "discount": 0,
  "notes": null
}
```

Response thành công:

```json
{
  "status": "SYNCED",
  "invoice": {
    "invoiceNumber": "BH-20261102-123456",
    "subtotal": "14000.00",
    "discount": "0.00",
    "totalAmount": "14000.00",
    "paymentMethod": "CASH",
    "items": []
  }
}
```

Response lỗi (chuẩn hoá bởi `mobileErrorResponse`):

```json
{
  "code": "STOCK_CONFLICT",
  "message": "Sản phẩm \"Nước\" không đủ tồn kho",
  "details": { "productId": "...", "requested": 48, "available": 12 }
}
```

## 6. Đối chiếu với Web

| Chức năng | Vị trí trên Web | Trạng thái mobile |
|---|---|---|
| Danh sách sản phẩm + tìm kiếm | `/sales` | ✅ Đã có |
| Lọc theo danh mục | `/sales` | ✅ Đã có |
| Giỏ hàng, tăng giảm số lượng | `/sales` | ✅ Đã có |
| Hộp thoại chọn đơn vị bán | `/sales` | ✅ Đã làm (P1.3) |
| Chọn phương thức thanh toán | `/sales` | ✅ Đã làm (P1.4) |
| Tiền khách đưa / tiền thừa | `/sales` | ✅ Đã làm (P1.5) |
| Chọn khách hàng | `/sales` | ✅ Đã làm (P1.6) |
| Ghi chú hoá đơn | `/sales` | ✅ Đã làm (P1.7) |
| Tạo hoá đơn trên server | `/sales` | ✅ Đã làm (P1.8) |
| Màn hình thanh toán thành công | `/sales` | ✅ Đã làm (P1.9) |
| Mã giảm giá (voucher) | `/sales` | ❌ Không có trên web, đã bỏ ô nhập ở mobile để khớp giao diện |

## 7. Các lỗi đã phát hiện và sửa trong đợt rà soát 02/11/2026

Build và typecheck đều pass nhưng vẫn còn lỗi nghiệp vụ. Các lỗi dưới đây đã
được sửa trong đợt rà soát này:

| # | Vị trí | Lỗi | Hậu quả | Cách sửa |
|---|---|---|---|---|
| 1 | `api/v1/sales/route.ts` | `operationId` bị đánh dấu `FAILED`/`CONFLICT` khi thiếu tồn kho, và lần retry sau bị chặn vĩnh viễn | Người dùng không bao giờ bán được sau 1 lần gặp thiếu tồn kho | Xoá bản ghi `sync_operations` có trạng thái `FAILED`/`CONFLICT` trước khi xử lý lại |
| 2 | `api/sales/route.ts` (web) | `product.update` không có điều kiện tồn kho | Hai phiên bán cùng lúc có thể trừ kho xuống âm | Dùng `updateMany` kèm `stock: { gte: quantityBase }` |
| 3 | `api/sales/route.ts` (web) | Không kiểm tra `discount > subtotal` | Tổng cộng có thể âm | Thêm kiểm tra và ném lỗi |
| 4 | `ManHinhChiTietHoaDon.kt` | Tổng cộng và tiền thừa tính ở client, lệch với server khi quy đổi đơn vị | Tiền thừa hiển thị sai | Lấy `totalAmount` từ server trả về |
| 5 | `ManHinhChiTietHoaDon.kt` | Chiết khấu sai giá trị làm hỏng cả bảng tổng tiền, nhưng vẫn gửi lên server | Tổng tiền hiển thị không khớp với hoá đơn | Kẹp giá trị về khoảng hợp lệ, cảnh báo riêng |
| 6 | `ManHinhBanHang.kt` | API sản phẩm có phân trang mặc định 15, POS không có nút tải thêm | Cửa hàng có hơn 15 sản phẩm thì phần lớn không bao giờ hiện để bán | Tăng `limit` lên 50 và tự tải trang khi cuon xuống cuối |
| 7 | `ManHinhBanHang.kt` | Kiểm tra tồn kho trước thanh toán dùng `tonKhoBanDau[id] ?: 0` | Sản phẩm ở trang 2+ bị coi là hết hàng và **bị xoá khỏi giỏ** | Thêm `taiTonKhoTheoIdSanPham()` duyệt hết các trang |
| 8 | `TrinhBaySanPhamBan.kt` | Luôn hiển thị `sellPrice` (giá đơn vị cơ bản) | Giá hiển thị khác giá bán thật khi sản phẩm có đơn vị mặc định khác | Hiển thị giá của đơn vị mặc định, giống web |
| 9 | `chi_tiet_hoa_don.xml` | Có ô "mã giảm giá" nhưng code không đọc | Người dùng nhập mã xong mất khi thanh toán | Bỏ ô nhập vì web cũng không có tính năng này |
| 10 | `DichVuBanHang.kt` | Bắt cả `CancellationException` | Request không hủy khi người dùng đóng màn hình giữa chừng | Ném lại `CancellationException` để coroutine hủy đúng cách |
| 11 | `TrinhBayDonViBan.kt` | `tonKho` được truyền vào lúc tạo adapter, không cập nhật theo giỏ hàng | Số lượng tối đa hiển thị sai sau khi đã thêm hàng | Adapter dùng lambda đọc tồn kho tại thời điểm bind |

## 8. Ghi chú về idempotency

`operationId` sinh một lần khi mở màn hình thanh toán, lưu trong
`savedInstanceState` để không đổi khi xoay máy, và dùng lại cho mọi lần bấm
"Xác nhận thanh toán".

Luồng xử lý phía server:

```text
Nhận request
  ├─ Đã có sync_operations với status SYNCED → trả lại hoá đơn cũ (ALREADY_PROCESSED)
  ├─ Đã có sync_operations với status FAILED/CONFLICT → xoá bản ghi, xử lý lại
  │     (cho phép thử lại sau khi đã nhập thêm tồn kho)
  └─ Chưa có → tạo bản ghi, xử lý trong transaction
        ├─ Thành công → status SYNCED, lưu kết quả
        └─ Thất bại → status FAILED/CONFLICT, lưu mã lỗi
```

