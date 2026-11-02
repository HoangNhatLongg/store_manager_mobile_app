# Quản lý tồn kho

> Hạng mục trong plan: P2 — Màn hình tồn kho
> Ngày hoàn thành: 02/11/2026
> Trạng thái: ✅ Hoàn thành

## 1. Mục đích

Cho phép quản lý theo dõi số lượng tồn của toàn bộ sản phẩm, lọc theo danh mục
và trạng thái tồn kho, nhanh chóng phát hiện hàng sắp hết hoặc đã hết hàng.
Bám sát giao diện web `/inventory`.

## 2. Luồng hoạt động

### 2.1. Luồng xem danh sách tồn kho

```text
Mở tab Tồn kho
  → Gọi GET /api/v1/categories để đổ vào bộ lọc danh mục
  → Gọi GET /api/v1/products?page=&limit=&search=&categoryId=
  → Gọi GET /api/v1/inventory/stats để lấy số liệu 4 thẻ thống kê
  → Hiển thị danh sách dạng RecyclerView
  → Người dùng nhập tìm kiếm (debounce 350ms) hoặc đổi bộ lọc → tải lại
```

### 2.2. Quy tắc phân loại trạng thái

```text
Hết hàng   : stock == 0
Sắp hết    : 0 < stock <= minStock
Còn hàng   : stock > minStock
```

Quy tắc này được đặt tập trung trong enum `TrangThaiTonKho` nên cả giao diện
hiển thị, bộ lọc và báo cáo CSV đều dùng chung một tiêu chuẩn.

### 2.3. Luồng bộ lọc trạng thái

```text
Bấm một trong 4 nút trạng thái (Tất cả / Còn hàng / Sắp hết / Hết hàng)
  → Đổi màu nút được chọn
  → Lọc danh sách đang tải ở client
  → Quay lại trang 1
```

Bộ lọc trạng thái áp dụng trên danh sách của trang hiện tại, giống hệt cách
web lọc trên bảng. Số liệu 4 thẻ thống kê **không** bị ảnh hưởng bởi bộ lọc
này vì thống kê được đếm riêng ở server trên toàn bộ sản phẩm.

### 2.4. Luồng xuất báo cáo

```text
Bấm "Xuất báo cáo"
  → Sinh nội dung CSV từ danh sách đang lọc
  → Ghi file vào cacheDir/bao_cao/ bao_cao_ton_kho_yyyyMMdd_HHmmss.csv
  → Chia sẻ qua Intent ACTION_SEND với FileProvider
  → Người dùng chọn ứng dụng nhận (Google Sheets, email, Drive…)
```

Không dùng quyền ghi bộ nhớ trực tiếp; dùng `FileProvider` với `cacheDir`.

### 2.5. Nhánh xử lý lỗi

| Tình huống | Mã lỗi | Xử lý |
|---|---|---|
| Token hết hạn | 401 UNAUTHORIZED | Xoá token, về màn hình đăng nhập |
| Lỗi tải danh sách | 500 SERVER_ERROR | Hiện thông báo, giữ danh sách cũ, cho thử lại |
| Mất mạng | IOException | Đọc từ database cục bộ, hiện toast báo đang dùng dữ liệu tải về lần trước |

## 3. Các file liên quan

| Loại | Đường dẫn | Vai trò |
|---|---|---|
| Activity | `app/src/main/java/com/example/store_manager/ManHinhTonKho.kt` | Màn hình tồn kho (đã thay khung rỗng) |
| Adapter | `app/src/main/java/com/example/store_manager/TrinhBayTongKho.kt` | Một dòng trong bảng tồn kho |
| Model | `app/src/main/java/com/example/store_manager/du_lieu/TrangThaiTonKho.kt` | Enum trạng thái + thống kê tồn kho |
| Dịch vụ | `app/src/main/java/com/example/store_manager/du_lieu/DichVuSanPham.kt` | Lấy sản phẩm, danh mục và thống kê tồn kho |
| Dịch vụ | `app/src/main/java/com/example/store_manager/du_lieu/BaoCaoCsv.kt` | Tạo và chia sẻ file CSV |
| Kho dữ liệu | `app/src/main/java/com/example/store_manager/du_lieu/KhoDuLieuSanPham.kt` | Ưu tiên server, fallback database cục bộ |
| Model | `app/src/main/java/com/example/store_manager/du_lieu/DoiTuongSanPham.kt` | `SanPham`, `DanhMuc`, `PhanTrang`, `SoLieuThongKeTonKho` |
| Giao diện | `app/src/main/res/layout/ton_kho.xml` | Bố cục màn hình tồn kho |
| Giao diện | `app/src/main/res/layout/dong_tong_kho.xml` | Một dòng tồn kho |
| Giao diện | `app/src/main/res/xml/duong_dan_file_chia_se.xml` | Đường dẫn FileProvider cho báo cáo CSV |
| Hằng số | `app/src/main/res/values/strings.xml` | Chuỗi hiển thị |
| Màu | `app/src/main/res/values/colors.xml` | Màu trạng thái tồn kho (đỏ / vàng / xanh) |

### 3.1. File backend

| Loại | Đường dẫn | Vai trò |
|---|---|---|
| API | `store-manager_web/src/app/api/v1/inventory/stats/route.ts` | Thống kê tồn kho toàn cửa hàng (mới) |
| API | `store-manager_web/src/app/api/v1/products/route.ts` | Danh sách sản phẩm có phân trang (đã có sẵn) |
| API | `store-manager_web/src/app/api/v1/categories/route.ts` | Bộ lọc danh mục (đã có sẵn) |

## 4. Bảng database liên quan

| Bảng | Trường dùng | Ghi chú |
|---|---|---|
| `products` | `id`, `name`, `sku`, `barcode`, `stock`, `minStock`, `isActive`, `importPrice`, `sellPrice`, `categoryId` | Dữ liệu chính của bảng tồn kho |
| `categories` | `id`, `name` | Bộ lọc danh mục |
| `product_units` | `productId`, `unitId`, `isDefault` | Hiển thị đơn vị bán |
| `units` | `id`, `name`, `abbreviation` | Tên đơn vị hiển thị |

Bảng cục bộ (Room) dùng cho chế độ offline:

| Bảng | Trường dùng | Ghi chú |
|---|---|---|
| `san_pham` | `ten`, `maSku`, `tonKho`, `tonToiThieu`, `maDanhMuc`, `dangHoatDong` | Nguồn dữ liệu khi mất mạng |
| `danh_muc` | `id`, `ten` | Tên danh mục hiển thị offline |
| `don_vi_tinh`, `don_vi_san_pham` | `maSanPham`, `maDonViTinh` | Đơn vị bán hiển thị offline |

## 5. Endpoint API được dùng

| Method | Đường dẫn | Mục đích |
|---|---|---|
| GET | `/api/v1/products` | Danh sách sản phẩm, hỗ trợ `search`, `categoryId`, `page`, `limit` |
| GET | `/api/v1/categories` | Bộ lọc danh mục |
| GET | `/api/v1/inventory/stats` | Thống kê tổng / sắp hết / hết hàng / còn hàng |

## 6. Đối chiếu với Web

| Chức năng | Vị trí trên Web | Trạng thái mobile |
|---|---|---|
| Bảng tồn kho | `/inventory` | ✅ Đã làm |
| Tìm kiếm sản phẩm | `/inventory` | ✅ Đã làm (debounce 350ms, tìm theo tên / SKU / mã vạch) |
| Lọc theo danh mục | `/inventory` | ✅ Đã làm |
| Lọc theo trạng thái tồn | `/inventory` | ✅ Đã làm (4 nút) |
| 4 thẻ thống kê tồn kho | `/inventory` | ✅ Đã làm (đếm ở server, không phụ thuộc phân trang) |
| Phân trang | `/inventory` | ✅ Đã làm (20 sản phẩm mỗi trang) |
| Nút "Làm mới" | `/inventory` | ✅ Đã làm (xoá bộ lọc và tải lại) |
| Cảnh báo tồn thấp | `/inventory` | ✅ Đã có ở màn hình Tổng quan |
| Xuất báo cáo | `/inventory` | ✅ Đã làm (chia sẻ file CSV, web mới chỉ có UI) |
| Chi tiết sản phẩm khi bấm vào dòng | `/products/{id}` | ⚠️ Tạm chuyển sang màn hình Quản lý sản phẩm (màn hình chi tiết riêng sẽ làm ở P8) |
