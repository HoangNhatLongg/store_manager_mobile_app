# Hướng dẫn hệ thống Store Manager Mobile

Tài liệu này mô tả cách ứng dụng Android hoạt động, cách các lớp liên kết với
backend và quy ước đặt tên trong source code.

## 1. Mục tiêu ứng dụng

Ứng dụng mobile là client cho hệ thống quản lý cửa hàng tạp hóa. Backend dùng
Next.js và Prisma; database chính có thể là PostgreSQL cục bộ hoặc PostgreSQL
trên Supabase. Android không kết nối trực tiếp đến PostgreSQL.

```text
Android
   |
   | HTTPS + Bearer access token
   v
Next.js Mobile API (/api/v1)
   |
   | Prisma
   v
PostgreSQL / Supabase
```

SQLite/Room sẽ được thêm làm database local. Khi đó luồng đầy đủ là:

```text
UI -> DichVu/Repository -> Room SQLite
                         |
                         -> Sync Queue -> Mobile API -> PostgreSQL
```

## 1.1. Tài liệu liên quan

| Tài liệu | Nội dung |
|---|---|
| `QUI_TAC_VIET_CODE.md` | Quy tắc đặt tên tiếng Việt không dấu, comment theo khối lệnh, kiến trúc và giao diện |
| `PLAN_TONG.md` | Todo list phát triển theo thứ tự ưu tiên |
| `docs/README.md` | Danh mục tài liệu của từng tính năng |

Mỗi tính năng mới phải kèm một file `.md` trong `docs/` mô tả luồng hoạt động,
các file liên quan và bảng database liên quan. Xem mẫu tại
`docs/MHUNG_TAI_LIEU_TINH_NANG.md`.

## 2. Quy ước tên

Tên file, class, hàm và biến trong Android dùng tiếng Việt không dấu, viết
liền hoặc camelCase để không vi phạm quy tắc identifier của Kotlin/Android.

Ví dụ:

```text
ManHinhTongQuan.kt
DichVuTongQuan.kt
TrinhBaySanPham.kt
taiDuLieuTongQuan()
hienThiSanPhamSapHet()
giaTriTongTienBan
```

Nội dung hiển thị cho người dùng phải viết tiếng Việt có dấu và đặt trong:

```text
app/src/main/res/values/strings.xml
```

Không đặt chuỗi hiển thị trực tiếp trong Kotlin hoặc XML nếu chuỗi đó có thể
tái sử dụng.

## 2.1. Quyền hiển thị thanh tab

Thanh tab dưới gồm các khu vực thường dùng:

```text
Tổng quan | Bán hàng | Tồn kho | Sản phẩm | Menu
```

`Tổng quan` chỉ hiển thị khi vai trò trong phiên đăng nhập là `ADMIN`. Tài
khoản nhân viên không thấy tab này và sau khi đăng nhập sẽ đi thẳng tới màn hình
`Bán hàng`.

Tab đang mở được đánh dấu bằng nền xanh nhạt và màu chữ/icon xanh đậm. Các tab
chưa chọn dùng nền trong suốt và màu xám.

## 3. Cấu trúc source Android

```text
app/src/main/
├── java/com/example/store_manager/
│   ├── ManHinhKhoiDong.kt
│   ├── ManHinhChinh.kt
│   ├── ManHinhTongQuan.kt
│   ├── ManHinhCoThanhTab.kt
│   ├── ManHinhBanHang.kt
│   ├── ManHinhTonKho.kt
│   ├── ManHinhDoanhThu.kt
│   ├── ManHinhSanPham.kt
│   ├── TrinhBaySanPham.kt
│   └── du_lieu/
│       ├── DichVuApi.kt
│       ├── DichVuDangNhap.kt
│       ├── DichVuTongQuan.kt
│       ├── DichVuSanPham.kt
│       ├── BoNhoPhienDangNhap.kt
│       ├── DoiTuongDangNhap.kt
│       ├── DoiTuongTongQuan.kt
│       └── DoiTuongSanPham.kt
├── res/layout/
│   ├── khoi_dong.xml
│   ├── dang_nhap.xml
│   ├── tong_quan.xml
│   ├── khung_man_hinh.xml
│   ├── thanh_tab_duoi.xml
│   ├── menu_man_hinh.xml
│   ├── ban_hang.xml
│   ├── ton_kho.xml
│   ├── doanh_thu.xml
│   ├── san_pham.xml
│   └── dong_san_pham.xml
└── res/values/
    ├── strings.xml
    ├── colors.xml
    └── themes.xml
```

## 4. Luồng khởi động

Activity launcher là `ManHinhKhoiDong`.

```text
Mở app
  ↓
ManHinhKhoiDong
  ↓
Đọc access token từ BoNhoPhienDangNhap
  ├── Không có token -> ManHinhChinh
  └── Có token -> ManHinhTongQuan
```

Hiện tại app kiểm tra sự tồn tại của token. Ở bước hoàn thiện authentication,
`ManHinhKhoiDong` cần gọi thêm `GET /api/v1/me` để kiểm tra token đã hết hạn
hay chưa.

## 5. Luồng đăng nhập

```text
ManHinhChinh
  ↓ xuLyDangNhap()
kiemTraDuLieuDangNhap()
  ↓
DichVuDangNhap.dangNhap()
  ↓
ApiCuaHang.dangNhap()
  ↓
POST /api/v1/auth/login
  ↓
BoNhoPhienDangNhap.luuPhien()
  ↓
ManHinhTongQuan
```

`DichVuDangNhap` chịu trách nhiệm gọi mạng, phân loại lỗi và lưu token. Activity
chỉ điều khiển giao diện và chuyển màn hình.

## 6. Gọi API và access token

`BoTaoApi` tạo Retrofit và OkHttpClient. Interceptor đọc token từ
`BoNhoPhienDangNhap` rồi tự thêm header:

```http
Authorization: Bearer <access-token>
```

Android Emulator truy cập backend trên máy tính bằng:

```text
http://10.0.2.2:3000/
```

Địa chỉ này được cấu hình trong `app/build.gradle.kts`:

```kotlin
buildConfigField("String", "DIA_CHI_API", "\"http://10.0.2.2:3000/\"")
```

Điện thoại thật phải dùng IP LAN của máy tính và hai thiết bị phải cùng mạng.

## 7. Dashboard

Màn hình `ManHinhTongQuan` gọi:

```http
GET /api/v1/dashboard
```

API trả về:

- `stats`: các số liệu trên thẻ thống kê.
- `recentActivities`: hoạt động nhập/bán gần đây.
- `lowStockProducts`: sản phẩm dưới mức tồn tối thiểu.
- `topProducts`: sản phẩm bán chạy.

Giao diện mobile giữ màu sắc và nội dung chính của dashboard web, nhưng chuyển
thành danh sách dọc để phù hợp với màn hình nhỏ.

## 8. Sản phẩm

Màn hình `ManHinhSanPham` gọi:

```http
GET /api/v1/products?search=<tu-khoa>&page=1&limit=100
```

Các sự kiện đã có:

- Nhập từ khóa tìm kiếm.
- Debounce 350ms trước khi gọi API.
- Hiển thị danh sách bằng `RecyclerView`.
- Chọn một sản phẩm.
- Bấm nút thêm sản phẩm.

Nút thêm và chọn sản phẩm hiện mới hiển thị thông báo. Luồng CRUD sản phẩm sẽ
được nối sau khi Room schema và quyền thao tác đã được chốt.

## 8.1. Điều hướng mobile

Mobile dùng thanh tab dưới cho các màn hình thường xuyên sử dụng:

```text
Tổng quan | Bán hàng | Tồn kho | Sản phẩm | Menu
```

Thanh tab nằm trong `khung_man_hinh.xml` và được dùng chung thông qua class
`ManHinhCoThanhTab`.

Các Activity chính kế thừa class này:

```text
ManHinhTongQuan
ManHinhBanHang
ManHinhTonKho
ManHinhSanPham
ManHinhSanPham
```

Tab `Menu` mở `BottomSheetDialog`, bên trong có các chức năng ít sử dụng hơn:

```text
Sản phẩm
Nhập hàng
Lịch sử nhập hàng
Lịch sử bán hàng
Danh mục
Đơn vị tính
Khách hàng
Nhà cung cấp
OCR hóa đơn
Cài đặt
Đăng xuất
```

Các mục chưa có Activity nghiệp vụ hiện hiển thị thông báo đang phát triển.
Khi triển khai module mới, tạo Activity kế thừa `ManHinhCoThanhTab`, khai báo
layout và thay handler trong `ManHinhCoThanhTab.moMenuManHinh()`.

## 9. API mobile hiện có

```text
POST /api/v1/auth/login
GET  /api/v1/me
GET  /api/v1/bootstrap
GET  /api/v1/dashboard
GET  /api/v1/products
GET  /api/v1/categories
GET  /api/v1/units
GET  /api/v1/customers
GET  /api/v1/suppliers
GET  /api/v1/sales/history
GET  /api/v1/imports/history
POST /api/v1/sales
```

`POST /api/v1/sales` dùng `operationId` để chống tạo trùng khi mobile retry.
Kiểm tra tồn kho và ghi hóa đơn được thực hiện trong transaction PostgreSQL.

## 10. Quy tắc xử lý lỗi

Android không nên kiểm tra nội dung message để quyết định logic. Hãy kiểm tra
`maLoi` hoặc HTTP status:

```text
401 UNAUTHORIZED       -> xóa token, yêu cầu đăng nhập lại
409 STOCK_CONFLICT     -> đưa hóa đơn vào trạng thái xung đột
400 VALIDATION_ERROR   -> hiển thị lỗi nhập liệu
500 SERVER_ERROR       -> cho phép thử lại
IOException            -> hiển thị trạng thái mất mạng
```

## 11. Phần chưa chuyển hoàn chỉnh

Các module sau cần triển khai tiếp:

1. Room SQLite và repository offline-first.
2. Giỏ hàng và bán hàng online.
3. Hóa đơn bán offline và `sync_queue`.
4. WorkManager đồng bộ hóa đơn.
5. Nhập hàng và hủy phiếu nhập.
6. CRUD sản phẩm, danh mục, đơn vị, nhà cung cấp.
7. OCR hóa đơn.
8. Báo cáo doanh thu chi tiết.
9. Refresh token và mã hóa token bằng Android Keystore.

Không nên đánh dấu các module này là hoàn thiện chỉ vì đã có layout. Nghiệp vụ
bán hàng phải dùng transaction và idempotency; nghiệp vụ offline phải được
kiểm thử khi mất mạng, retry và xung đột tồn kho.

## 12. Cách chạy và kiểm tra

### Backend

```powershell
cd C:\CNTT\MobileApp\DoAnCuoiKi\Store-manager\store-manager_web
npm run dev
```

### Android

```powershell
cd C:\CNTT\MobileApp\DoAnCuoiKi\Store-manager\Store_manager_mobile
.\gradlew.bat :app:assembleDebug
```

### Kiểm tra thủ công

1. Mở app và thấy splash logo.
2. Đăng nhập bằng tài khoản hợp lệ.
3. Dashboard tải được số liệu.
4. Bấm `Làm mới` và quan sát loading.
5. Mở màn hình sản phẩm từ Activity tương ứng.
6. Tìm kiếm sản phẩm.
7. Tắt backend và kiểm tra thông báo lỗi kết nối.

## 13. Quy tắc phát triển tiếp theo

Mỗi module mới nên đi theo thứ tự:

```text
Prisma/API contract
  ↓
Model Kotlin
  ↓
DichVu gọi API
  ↓
Layout
  ↓
Adapter/ViewModel
  ↓
Xử lý sự kiện
  ↓
Room local
  ↓
Test online/offline
```

Không gọi API trực tiếp từ Adapter. Không đặt logic transaction trong Activity.
Không lưu password trong Android. Không đưa database credential hoặc OpenRouter
API key vào app.
