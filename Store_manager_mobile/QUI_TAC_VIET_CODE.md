# Quy tắc viết code dự án Store Manager

Tài liệu này là quy tắc bắt buộc khi viết hoặc sửa code trong dự án
`Store_manager_mobile` (Android) và `store-manager_web` (Next.js).

---

## 1. Đặt tên tiếng Việt không dấu

Mọi tên định danh (file, class, hàm, biến, hằng số, resource id, tên truy vấn
Prisma) viết bằng **tiếng Việt không dấu**, viết liền hoặc `camelCase` để không
vi phạm quy tắc identifier của Kotlin/Android.

### 1.1. Quy tắc bắt buộc

- Tên phải **sát nghĩa với công việc** của nó, không dùng tên chung chung.
  - ✅ `taiDanhSachSanPhamTheoDanhMuc()` — tải danh sách sản phẩm theo danh mục
  - ❌ `xửLy()` / `handleData()` / `doSomething()` — không nói lên được việc gì
- Không dùng viết tắt tiếng Anh lan man trong cùng một nhóm tên. Nếu đang
  dùng `api` / `id` / `url` thì giữ nhất quán trong nhóm đó.
- Chữ cái đầu tên file: **PascalCase** cho class, **camelCase** cho file khác.

### 1.2. Ví dụ chuyển đổi

| Nghĩa | Tên sai (tiếng Anh) | Tên đúng (tiếng Việt không dấu) |
|---|---|---|
| Màn hình tổng quan | `DashboardActivity` | `ManHinhTongQuan` |
| Màn hình bán hàng | `SalesActivity` | `ManHinhBanHang` |
| Màn hình tồn kho | `InventoryActivity` | `ManHinhTonKho` |
| Màn hình doanh thu | `RevenueActivity` | `ManHinhDoanhThu` |
| Màn hình sản phẩm | `ProductActivity` | `ManHinhSanPham` |
| Màn hình chi tiết hoá đơn | `InvoiceDetailActivity` | `ManHinhChiTietHoaDon` |
| Màn hình nhập hàng | `ImportActivity` | `ManHinhNhapHang` |
| Màn hình danh mục | `CategoryActivity` | `ManHinhDanhMuc` |
| Màn hình đơn vị tính | `UnitActivity` | `ManHinhDonViTinh` |
| Màn hình khách hàng | `CustomerActivity` | `ManHinhKhachHang` |
| Màn hình nhà cung cấp | `SupplierActivity` | `ManHinhNhaCungCap` |
| Màn hình OCR | `OcrActivity` | `ManHinhOcrHoaDon` |
| Màn hình cài đặt | `SettingsActivity` | `ManHinhCaiDat` |
| Màn hình lịch sử nhập | `ImportHistoryActivity` | `ManHinhLichSuNhap` |
| Màn hình lịch sử bán | `SalesHistoryActivity` | `ManHinhLichSuBan` |
| Lớp gọi API sản phẩm | `ProductService` | `DichVuSanPham` |
| Lớp gọi API tổng quan | `DashboardService` | `DichVuTongQuan` |
| Lớp gọi API đăng nhập | `AuthService` | `DichVuDangNhap` |
| Model dữ liệu sản phẩm | `ProductDto` | `DoiTuongSanPham` / `SanPham` |
| Model yêu cầu đăng nhập | `LoginRequest` | `YeuCauDangNhap` |
| Adapter danh sách sản phẩm | `ProductAdapter` | `TrinhBaySanPham` |
| Adapter giỏ hàng | `CartAdapter` | `TrinhBayGioHang` |
| Bộ nhớ phiên đăng nhập | `SessionManager` | `BoNhoPhienDangNhap` |
| Nơi tạo Retrofit | `ApiProvider` | `BoTaoApi` |
| Kết quả trả về | `ApiResult` | `KetQuaSanPham` / `KetQuaTongQuan` |
| Layout màn hình tồn kho | `inventory.xml` | `ton_kho.xml` |
| Layout dòng sản phẩm | `item_product.xml` | `dong_san_pham.xml` |
| Layout hộp thoại sản phẩm | `dialog_product.xml` | `hop_thoai_san_pham.xml` |

### 1.3. Quy tắc riêng cho mã nguồn Web (`store-manager_web`)

- Tên file trang: `snake_case` hoặc `kebab-case` theo quy ước Next.js App Router,
  giữ nguyên vì cấu trúc thư mục là đường dẫn URL.
- Tên hàm, biến, component, hằng số: **tiếng Việt không dấu**, `camelCase`
  (component: `PascalCase`). Ví dụ `taiDanhSachSanPham()`, `soLuongTong()`,
  `bangDoanhThu`.
- Nhãn hiển thị cho người dùng (chuỗi trong JSX) **phải có dấu** và nằm ở
  nơi tập trung nếu có thể tái sử dụng.

### 1.4. Quy tắc riêng cho chuỗi hiển thị trên Android

- Mọi chuỗi hiển thị cho người dùng **phải có dấu** và đặt trong
  `app/src/main/res/values/strings.xml`.
- Không đặt chuỗi hiển thị trực tiếp trong Kotlin hoặc XML.
- Trong Kotlin, chỉ dùng `getString(R.string....)` với khoá đã khai báo.
- Trong XML dùng `@string/...`.

---

## 2. Comment theo từng khối lệnh

Mỗi khối lệnh có ý nghĩa phải có comment giải thích **vì sao làm** và **làm
gì**, không phải chỉ lặp lại dòng code.

### 2.1. Mức độ yêu cầu

| Vị trí | Yêu cầu |
|---|---|
| Hàm public/private | Bắt buộc: KDoc mô tả mục đích hàm, tham số, giá trị trả về, các trường hợp lỗi đáng chú ý |
| Đầu class | Bắt buộc: KDoc mô tả trách nhiệm của class |
| Khối lệnh nhiều dòng trong hàm | Bắt buộc: comment tiếng Việt không dấu giải thích ý nghĩa khối đó |
| Khối lệnh 1–2 dòng, dễ hiểu | Không cần comment |
| Logic phức tạp (transaction, quy đổi đơn vị, tính tiền) | Bắt buộc: comment giải thích công thức / điều kiện biên |

### 2.2. Ví dụ đúng

```kotlin
/**
 * Tính số lượng tối đa có thể bán theo một đơn vị quy đổi.
 *
 * Tồn kho lưu theo đơn vị cơ bản, còn người bán có thể bán theo thùng hoặc
 * lon. Vì vậy số lượng theo đơn vị bán phải chia cho conversionQty.
 *
 * @param sanPham      Sản phẩm cần tính.
 * @param donViBan     Đơn vị bán người dùng chọn.
 * @param soLuongTrongGio Số lượng đã nằm trong giỏ hàng.
 * @return Số lượng tối đa theo đơn vị bán, không âm.
 */
private fun tinhSoLuongToiDaTheoDonVi(
    sanPham: SanPham,
    donViBan: DonViSanPham,
    soLuongTrongGio: Int
): Int {
    // Tồn kho luôn tính theo đơn vị cơ bản nên phải chia cho hệ số quy đổi.
    val tonKhoTheoDonViBan = sanPham.stock / donViBan.conversionQty

    // Trừ phần đã nằm trong giỏ để không cho vượt quá tồn kho thực tế.
    val conLai = tonKhoTheoDonViBan - soLuongTrongGio

    // Giá trị âm nghĩa là đã vượt tồn, chặn lại bằng 0.
    return conLai.coerceAtLeast(0)
}
```

### 2.3. Ví dụ sai

```kotlin
// Khai báo biến
val tong = 0

// Cộng vào tong
tong += giaTri

// Trả về kết quả
return tong
```

Comment kiểu này không mang thông tin, làm file rối mà không giúp ai hiểu code.

---

## 3. Quy tắc kiến trúc Android

### 3.1. Phân tầng bắt buộc

```text
UI (Activity / Adapter)
  ↓ gọi
DichVu (lớp gọi API, xử lý lỗi)
  ↓
ApiCuaHang (Retrofit interface)
```

- **Không** gọi API trực tiếp từ Adapter.
- **Không** đặt logic transaction hay logic nghiệp vụ phức tạp trong Activity.
  Activity chỉ điều khiển giao diện và điều hướng.
- Mọi lời gọi mạng chạy trong `Dispatchers.IO` bằng `withContext`.
- Lỗi phải được phân loại theo `maLoi` hoặc HTTP status, **không** kiểm tra
  nội dung message để quyết định logic.

### 3.2. Xử lý lỗi theo mã lỗi

| Mã lỗi | Xử lý trên app |
|---|---|
| `401 UNAUTHORIZED` | Xoá token, chuyển về màn hình đăng nhập |
| `409 STOCK_CONFLICT` | Đưa hóa đơn vào trạng thái xung đột, cho phép kiểm tra lại tồn kho |
| `400 VALIDATION_ERROR` | Hiển thị lỗi nhập liệu ngay tại ô nhập |
| `500 SERVER_ERROR` | Cho phép thử lại |
| `IOException` | Hiển thị trạng thái mất mạng |

### 3.3. Idempotency khi bán hàng

Mỗi lần tạo hóa đơn bán **phải** sinh một `operationId` (UUID) duy nhất và
gửi kèm. Nếu request retry phải dùng lại **cùng** `operationId` để server
không tạo hóa đơn trùng.

### 3.4. Quy ước đặt tên tài nguyên Android

- Layout: `snake_case` (`ton_kho.xml`, `hop_thoai_don_vi.xml`)
- Drawable: `snake_case` có tiền tố chủ đề (`nen_`, `bieu_tuong_`, `ic_`)
- Màu: tiếng Việt không dấu (`xanh_dam`, `xam_phu`)
- Id trong layout: `snake_case` tiếng Việt không dấu (`nut_xac_nhan_thanh_toan`)

---

## 4. Quy tắc giao diện bám sát Web

Giao diện mobile phải bám sát bản web nhất có thể.

### 4.1. Bảng màu

Lấy từ `store-manager_web/src/app/globals.css`:

| Ý nghĩa | Web (HSL) | Android (`colors.xml`) |
|---|---|---|
| Màu chính | `hsl(160 60% 45%)` | `xanh_chu_dao` = `#059669` |
| Màu chính đậm | — | `xanh_dam` = `#047857` |
| Nền nhạt chính | `hsl(160 55% 45%)` mờ | `xanh_nhat` = `#ECFDF5` |
| Nền trang | `hsl(40 20% 98%)` | `nen_splash` = `#F8FAFC` |
| Chữ chính | `hsl(220 20% 25%)` | `xam_chu` = `#0F172A` |
| Chữ phụ | `hsl(220 10% 50%)` | `xam_phu` = `#64748B` |
| Bo góc | `--radius: 0.75rem` | 18–24dp |

### 4.2. Nguyên tắc bố cục

- Thẻ (card) dùng `MaterialCardView`, bo góc 18–24dp, `strokeWidth = 0dp`,
  nền trắng `#FFFFFF`, có đổ bóng nhẹ.
- Tiêu đề màn hình: 28sp, đậm, màu `xam_chu`.
- Mô tả phụ dưới tiêu đề: 14sp, màu `xam_phu`.
- Thẻ thống kê: giá trị đậm 24–28sp, nhãn 13–14sp màu `xam_phu`.
- Cảnh báo tồn kho: nền `#FFFBEB`, chữ `#92400E`, màu phụ `#A16207`.
- Bảng dữ liệu dài → dùng `RecyclerView` thay vì `TableLayout`.
- Khoảng cách chuẩn: 16dp giữa các khối, 20dp padding ngoài màn hình.

### 4.3. Hành vi

- Tìm kiếm có debounce 350ms.
- Có trạng thái loading (`ProgressBar`) và thông báo lỗi bên dưới.
- Nút thao tác chính dùng màu `xanh_chu_dao`, nút phụ dùng nền trắng viền xám.
- Xác nhận trước khi xoá: `MaterialAlertDialogBuilder` với nút tạm ưu tiên là
  `Xoá`.

---

## 5. Quy tắc tài liệu

- Mỗi tính năng mới **phải** có một file `.md` trong `Store_manager_mobile/docs/`.
- File tài liệu phải mô tả:
  1. Mục đích tính năng.
  2. Luồng hoạt động (dạng danh sách bước hoặc PlantUML).
  3. Các file liên quan (Kotlin, XML, strings, API).
  4. Các bảng database liên quan.
  5. Endpoint API được dùng.
- Tên file tài liệu dùng tiếng Việt không dấu: `ban_hang_pos.md`, `ton_kho.md`…
- Cập nhật `PLAN_TONG.md` sau mỗi tính năng hoàn thành (đổi `[ ]` thành `[x]`).

---

## 6. Kiểm tra trước khi kết thúc một tác vụ

- [ ] Build thành công: `.\gradlew.bat :app:assembleDebug`
- [ ] Không còn `TODO` bỏ dở trong code mới.
- [ ] Mọi chuỗi hiển thị mới đã nằm trong `strings.xml`.
- [ ] Mọi tên mới đã theo đúng quy tắc tiếng Việt không dấu.
- [ ] Có comment theo từng khối lệnh.
- [ ] Đã cập nhật `PLAN_TONG.md` và viết file tài liệu của tính năng.
