# Sửa lỗi hiện có

> Hạng mục trong plan: P0 — Sửa lỗi hiện có
> Ngày hoàn thành: 02/11/2026
> Trạng thái: ✅ Hoàn thành

## 1. Mục đích

Xoá các lỗi đã phát hiện trong lần rà soát đầu tiên giữa Web và Android, để
các tính năng mới không phải gánh lỗi của người trước và để dữ liệu không bị
sai lệch.

## 2. Luồng hoạt động

Mỗi lỗi được sửa theo cùng một trình tự:

```text
Xác định lỗi → Viết lại theo quy tắc trong QUI_TAC_VIET_CODE.md
  → Build thành công → Kiểm tra thủ công → Ghi vào bảng bên dưới
```

### 2.1. Danh sách lỗi và cách sửa

| Mã | Lỗi | File liên quan | Cách sửa | Trạng thái |
|---|---|---|---|---|
| P0.1 | `POST /api/v1/products` bỏ qua `barcode` | `store-manager_web/src/app/api/v1/products/route.ts` | Đọc `body.barcode` và gán vào `prisma.product.create` | ✅ |
| P0.2 | Trường tiền tệ là `String`, có thể sinh chuỗi `"null"` | `du_lieu/DoiTuongSanPham.kt`, `du_lieu/DinhDangTien.kt`, `api/v1/products/route.ts` | Backend dùng `decimalToString`; Android đổi sang `Double` + `DecimalFormat` | ✅ |
| P0.3 | Tồn kho giỏ hàng lấy từ lúc tải trang, có thể sai | `ManHinhBanHang.kt` | Lấy lại tồn kho mới nhất trước khi sang màn hình thanh toán | ✅ |
| P0.4 | Kiểm tra lỗi xoá bằng `contains("HTTP_409")` | `ManHinhSanPham.kt`, `du_lieu/DichVuSanPham.kt`, `du_lieu/PhanTichLoi.kt` | Trả mã lỗi có kiểu, so sánh bằng mã chứ không kiểm tra message | ✅ |
| P0.5 | Không kiểm tra token còn hạn khi khởi động | `ManHinhKhoiDong.kt`, `du_lieu/DichVuTaiKhoan.kt` | Gọi `GET /api/v1/me`, xoá token nếu trả 401 | ✅ |
| P0.6 | `finishAffinity()` khiến app thoát hẳn | `ManHinhCoThanhTab.kt` | `startActivity(DangNhap)` + `finish()` | ✅ |
| P0.7 | Back stack có thể phình to | `ManHinhCoThanhTab.kt` | Dùng `FLAG_ACTIVITY_CLEAR_TOP` khi chuyển tab | ✅ |
| P0.8 | Chuỗi hiển thị đặt cứng trong XML | `res/layout/ton_kho.xml`, `res/layout/doanh_thu.xml` | Chuyển hết sang `values/strings.xml` | ✅ |

## 3. Chi tiết từng thay đổi

### 3.1. P0.1 — Lưu mã vạch khi tạo sản phẩm

Luồng trước khi sửa:

```text
Mobile POST /api/v1/products { name, sku, barcode, ... }
  → Backend bỏ qua barcode
    → Prisma tạo product với barcode = NULL
      → Mobile tìm kiếm bằng barcode → không ra kết quả
```

Luồng sau khi sửa:

```text
Mobile POST /api/v1/products { name, sku, barcode, ... }
  → Backend đọc body.barcode, trim và gán vào prisma.product.create
    → Prisma tạo product với barcode đúng
      → Mobile tìm kiếm bằng barcode → ra đúng 1 kết quả
```

### 3.2. P0.2 — Chuẩn hoá tiền tệ

| Tầng | Cách làm |
|---|---|
| Backend | `chuyenSanPham()` trong `api/v1/products/route.ts` dùng `decimalToString` cho `importPrice`, `sellPrice`, `previousImportPrice`, `previousSellPrice` và `units[].price`. |
| Model | `SanPham.importPrice` / `sellPrice`, `DonViSanPham.price`, `SanPhamSapHet.sellPrice` đổi từ `String?` sang `Double`. |
| Tiện ích | `du_lieu/DinhDangTien.kt` — nguồn duy nhất cho định dạng tiền, chuyển chuỗi ↔ số và kiểm tra chuỗi nhập hợp lệ. |

`DinhDangTien` cung cấp:

- `dinhDangTien(gia: Double): String` — hiển thị theo chuẩn VN (`12.000 ₫`).
- `chuyenChuoiSangTien(chuoi: String?): Double` — nhận `"12000"`, `"12.000"`, `"12000,5"`.
- `laGiaKhongHopLe(chuoi: String?): Boolean` — kiểm tra trước khi gửi server.
- `taoChuoiChoOCoNhap(gia: Double): String` — đưa giá vào ô nhập không dấu phay.

### 3.3. P0.3 — Kiểm tra tồn kho trước khi bán

`ManHinhBanHang.moChiTietHoaDon()` giờ gọi lại `DichVuSanPham.taiDanhSach()`
trước khi chuyển màn hình:

```text
Bấm "Tính tiền"
  → Tải lại danh sách sản phẩm (tồn kho mới nhất)
    → So từng dòng giỏ hàng với tồn kho mới
      → Vượt tồn: điều chỉnh số lượng + thông báo "không còn đủ tồn kho"
        → Số lượng = 0: loại khỏi giỏ hàng
          → Sang màn hình chi tiết hoá đơn
```

Nếu không tải được tồn kho mới (lỗi mạng), app vẫn cho sang màn hình thanh
toán — server sẽ chặn lại nếu thực sự thiếu tồn kho.

### 3.4. P0.4 — Xử lý lỗi theo mã lỗi chuẩn

Tạo `du_lieu/PhanTichLoi.kt`:

- `layLoi(response)` → trả về `LoiApi(maLoi, thongBao)`.
- `layMaLoi(response)` → trả về chuỗi mã lỗi.

Body lỗi của Retrofit chỉ đọc được một lần nên `layLoi()` đọc và phân tích
trong cùng một lần gọi.

`DichVuSanPham` đổi toàn bộ API ghi/xoá sang `Result`, mã lỗi được ném trong
`message` của `IllegalStateException` để Activity so sánh trực tiếp:

```text
DUPLICATE                 → "Tên sản phẩm hoặc mã SKU đã tồn tại"
PRODUCT_HAS_INVOICE_HISTORY → "Sản phẩm đã có trong hóa đơn nhập hàng..."
VALIDATION_ERROR         → "Thiếu thông tin bắt buộc..."
```

### 3.5. P0.5 — Kiểm tra token khi khởi động

Tạo `du_lieu/DichVuTaiKhoan.kt` với `kiemTraToken()` trả về:

| Kết quả | Hành động |
|---|---|
| `HieuLuc` | Vào `ManHinhTongQuan` nếu vai trò ADMIN, ngược lại `ManHinhBanHang`. |
| `HetHan` | Xoá phiên, quay lại `DangNhap`. |
| `LoiKetNoi` | Giữ nguyên phiên, vẫn vào app để người dùng thử lại. |

`ManHinhKhoiDong` chỉ kiểm tra token khi có token trong bộ nhớ; nếu không có thì
chuyển thẳng tới `DangNhap`.

### 3.6. P0.6 & P0.7 — Đăng xuất và back stack

- `dangXuat()` bỏ `finishAffinity()`, dùng `startActivity(DangNhap)` kèm
  `FLAG_ACTIVITY_CLEAR_TASK` rồi `finish()`.
- `moManHinh()` bổ sung `FLAG_ACTIVITY_CLEAR_TOP` để xoá các màn hình trùng
  loại nằm trong stack.

### 3.7. P0.8 — Gom chuỗi hiển thị

`ton_kho.xml` và `doanh_thu.xml` không còn chuỗi tiếng Việt viết cứng; tất cả
trỏ tới `values/strings.xml`. Các chuỗi mới được thêm:

| Khóa | Nội dung |
|---|---|
| `mo_ta_ton_kho_man_hinh` | Mô tả màn hình tồn kho |
| `tieu_de_san_pham_can_chu_y` | Tiêu đề thẻ cảnh báo |
| `mo_ta_san_pham_can_chu_y` | Mô tả thẻ cảnh báo |
| `mo_ta_doanh_thu_man_hinh` | Mô tả màn hình doanh thu |
| `tieu_de_bao_cao_doanh_thu` | Tiêu đề báo cáo |
| `mo_ta_bieu_do_doanh_thu` | Mô tả biểu đồ |
| `loi_gia_khong_hop_le` | Báo lỗi giá không hợp lệ |
| `loi_trung_ten_hoac_sku` | Báo lỗi trùng tên/SKU |
| `loi_du_lieu_bat_buoc` | Báo thiếu trường bắt buộc |
| `loi_het_ton_kho_khi_ban` | Báo hết tồn kho lúc bán |

## 4. Các file liên quan

| Loại | Đường dẫn | Vai trò |
|---|---|---|
| Web API | `store-manager_web/src/app/api/v1/products/route.ts` | Tạo sản phẩm, sửa lỗi barcode và định dạng tiền |
| Model | `app/src/main/java/com/example/store_manager/du_lieu/DoiTuongSanPham.kt` | Chuẩn hoá kiểu dữ liệu tiền |
| Model | `app/src/main/java/com/example/store_manager/du_lieu/DoiTuongTaiKhoan.kt` | Model cho `GET /api/v1/me` |
| Tiện ích | `app/src/main/java/com/example/store_manager/du_lieu/DinhDangTien.kt` | Nguồn chuẩn duy nhất cho tiền tệ |
| Tiện ích | `app/src/main/java/com/example/store_manager/du_lieu/PhanTichLoi.kt` | Đọc mã lỗi chuẩn từ response lỗi |
| Dịch vụ | `app/src/main/java/com/example/store_manager/du_lieu/DichVuSanPham.kt` | Trả mã lỗi có kiểu thay vì chuỗi |
| Dịch vụ | `app/src/main/java/com/example/store_manager/du_lieu/DichVuTaiKhoan.kt` | Kiểm tra token còn hạn |
| Activity | `app/src/main/java/com/example/store_manager/ManHinhBanHang.kt` | Kiểm tra tồn kho trước khi bán |
| Activity | `app/src/main/java/com/example/store_manager/ManHinhSanPham.kt` | Xử lý lỗi xoá theo mã lỗi |
| Activity | `app/src/main/java/com/example/store_manager/ManHinhKhoiDong.kt` | Kiểm tra token |
| Activity | `app/src/main/java/com/example/store_manager/ManHinhCoThanhTab.kt` | Đăng xuất và điều hướng tab |
| Hằng số | `app/src/main/res/values/strings.xml` | Gom chuỗi hiển thị |

## 5. Bảng database liên quan

| Bảng | Trường | Ghi chú |
|---|---|---|
| `products` | `barcode` | Có unique index, giá trị `NULL` được phép |
| `products` | `importPrice`, `sellPrice` | `Decimal(12,2)` — phải chuyển qua chuỗi khi truyền JSON |
| `products` | `isActive` | Dùng cho chức năng ẩn/hiện |
| `sync_operations` | `operationId`, `status`, `errorCode` | Dùng khi xử lý lỗi bán hàng |

## 6. Endpoint API liên quan

| Method | Đường dẫn | Mục đích |
|---|---|---|
| POST | `/api/v1/products` | Tạo sản phẩm — sửa lỗi barcode |
| PUT | `/api/v1/products/:id` | Cập nhật sản phẩm — đã hỗ trợ barcode |
| GET | `/api/v1/me` | Kiểm tra token còn hạn |
| POST | `/api/v1/sales` | Kiểm tra tồn kho trước khi bán |

## 7. Đối chiếu với Web

| Chức năng | Vị trí trên Web | Trạng thái mobile |
|---|---|---|
| Tạo sản phẩm có mã vạch | `/products` | ✅ Đã sửa ở P0.1 |
| Tìm kiếm theo mã vạch | `/products` | ✅ Đã sửa ở P0.1 |
| Ghi đè giá nhập / giá bán | `/products` | ✅ Đã sửa ở P0.2 |
| Thông báo lỗi theo mã lỗi | Toàn hệ thống | ✅ Đã sửa ở P0.4 |
| Tự động đăng nhập lại khi token hết hạn | NextAuth | ✅ Đã sửa ở P0.5 |

## 8. Kiểm tra sau khi sửa

| Bước | Lệnh | Kết quả |
|---|---|---|
| Build Android | `./gradlew.bat :app:assembleDebug` | BUILD SUCCESSFUL |
| Typecheck Web | `npx tsc --noEmit` | Không có lỗi |
