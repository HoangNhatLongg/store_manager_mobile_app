# Báo cáo doanh thu

> Hạng mục trong plan: P3 — Báo cáo doanh thu
> Ngày hoàn thành: 02/11/2026
> Trạng thái: ✅ Hoàn thành

## 1. Mục đích

Cho phép xem báo cáo doanh thu theo ba cấp độ: **năm → tháng → ngày**, kèm
biểu đồ cột vẽ bằng Canvas và bảng chi tiết sản phẩm đã bán trong ngày.
Bám sát giao diện web `/revenue`.

## 2. Luồng hoạt động

### 2.1. Luồng tổng quan

```text
Mở menu → Quản lý doanh thu
  → Chọn năm trong Spinner
  → Gọi GET /api/v1/analytics/revenue?startDate=&endDate=&period=year
  → Hiển thị 4 thẻ thống kê + biểu đồ 12 cột (T1 → T12)
```

### 2.2. Luồng drill-down ba cấp

```text
CAP NAM
  Chạm vào cột tháng (T3)
    → thangDangXem = 3, ngayDangXem = null
    → Gọi ...?period=month
    → Hiện khu vực tháng: 3 thẻ + biểu đồ ngày + lưới danh sách ngày

CAP THANG
  Chạm vào cột ngày (hoặc ô ngày trong lưới)
    → ngayDangXem = "yyyy-MM-dd"
    → Gọi GET /api/v1/analytics/revenue-daily?date=
    → Hiện khu vực ngày: 4 thẻ + bảng sản phẩm đã bán

Nút "Quay lại"
  Ở cap ngày → về cap thang
  Ở cap thang → về cap nam
```

Đổi năm sẽ đưa người dùng về lại cap năm vì tháng và ngay đang chọn không
còn hợp lệ.

### 2.3. Quy tắc tính lợi nhuận

```text
Doanh thu = tổng totalPrice của các dòng hàng trong hóa đơn
Giá vốn   = tổng (importPrice × quantity)
Lợi nhuận = Doanh thu − Giá vốn
Biên lợi nhuận = Lợi nhuận / Doanh thu × 100
```

Giá nhập được lưu theo từng dòng hàng tại thời điểm bán (`importPrice` trong
`sale_invoice_items`), nên lợi nhuận luôn phản ánh đúng giá vốn thực tế dù
giá nhập sản phẩm có thay đổi về sau.

Chỉ hóa đơn có trạng thái `COMPLETED` mới được tính vào báo cáo.

### 2.4. Nhánh xử lý lỗi

| Tình huống | Mã lỗi | Xử lý |
|---|---|---|
| Token hết hạn | 401 UNAUTHORIZED | Xoá token, về màn hình đăng nhập |
| Lỗi tải báo cáo | 500 SERVER_ERROR | Hiện thông báo, giữ dữ liệu cũ |
| Mất mạng | IOException | Hiện thông báo mất mạng, cho thử lại |

## 3. Các file liên quan

### 3.1. Android

| Loại | Đường dẫn | Vai trò |
|---|---|---|
| Activity | `app/src/main/java/com/example/store_manager/ManHinhDoanhThu.kt` | Màn hình doanh thu 3 cấp độ |
| View | `app/src/main/java/com/example/store_manager/BieuDoCot.kt` | Biểu đồ cột vẽ bằng Canvas |
| Adapter | `app/src/main/java/com/example/store_manager/TrinhBayNgayTrongThang.kt` | Lưới các ngày trong tháng |
| Adapter | `app/src/main/java/com/example/store_manager/TrinhBaySanPhamDaBan.kt` | Bảng sản phẩm đã bán trong ngày |
| Model | `app/src/main/java/com/example/store_manager/du_lieu/DoiTuongDoanhThu.kt` | `TongHopDoanhThu`, `DoanhThuTheoKy`, `SanPhamDaBan` |
| Dịch vụ | `app/src/main/java/com/example/store_manager/du_lieu/DichVuDoanhThu.kt` | Gọi API báo cáo doanh thu |
| Giao diện | `app/src/main/res/layout/doanh_thu.xml` | Bố cục màn hình (3 khu vực) |
| Giao diện | `app/src/main/res/layout/dong_ngay_trong_thang.xml` | Một ô ngày |
| Giao diện | `app/src/main/res/layout/dong_san_pham_da_ban.xml` | Một dòng sản phẩm đã bán |
| Giao diện | `app/src/main/res/layout/menu_man_hinh.xml` | Thêm mục "Quản lý doanh thu" |

### 3.2. Backend

| Loại | Đường dẫn | Vai trò |
|---|---|---|
| API | `store-manager_web/src/app/api/v1/analytics/revenue/route.ts` | Báo cáo doanh thu theo kỳ (mới) |
| API | `store-manager_web/src/app/api/v1/analytics/revenue-daily/route.ts` | Chi tiết doanh thu một ngày (mới) |

## 4. Bảng database liên quan

| Bảng | Trường dùng | Ghi chú |
|---|---|---|
| `sale_invoices` | `id`, `status`, `createdAt`, `customerId`, `subtotal`, `discount`, `totalAmount` | Hóa đơn bán, chỉ tính `COMPLETED` |
| `sale_invoice_items` | `saleInvoiceId`, `productId`, `quantity`, `unitPrice`, `importPrice`, `totalPrice` | Chi tiết dòng hàng |
| `products` | `id`, `name`, `sku`, `categoryId` | Thông tin sản phẩm khi gom theo ngày |
| `categories` | `id`, `name` | Tên danh mục hiển thị trong bảng sản phẩm |

## 5. Endpoint API được dùng

| Method | Đường dẫn | Tham số | Mục đích |
|---|---|---|---|
| GET | `/api/v1/analytics/revenue` | `startDate`, `endDate`, `period` | Báo cáo doanh thu theo kỳ (`year` / `month` / `day`) |
| GET | `/api/v1/analytics/revenue-daily` | `date` | Chi tiết doanh thu và sản phẩm đã bán của một ngày |

## 6. Đối chiếu với Web

| Chức năng | Vị trí trên Web | Trạng thái mobile |
|---|---|---|
| Chọn năm | `/revenue` | ✅ Đã làm (Spinner, 2 năm trước → 1 năm sau) |
| 4 thẻ thống kê năm | `/revenue` | ✅ Đã làm (tổng DT, tổng LN, số đơn, TB tháng) |
| Biểu đồ doanh thu theo tháng | `/revenue` | ✅ Đã làm (`BieuDoCot` vẽ Canvas, chạm để drill-down) |
| Drill-down tháng → ngày | `/revenue` | ✅ Đã làm |
| 3 thẻ thống kê tháng | `/revenue` | ✅ Đã làm |
| Biểu đồ doanh thu theo ngày | `/revenue` | ✅ Đã làm |
| Danh sách ngày trong tháng | `/revenue` | ✅ Đã làm (lưới 3 cột) |
| 4 thẻ thống kê ngày | `/revenue` | ✅ Đã làm (DT, LN, số đơn, SP đã bán) |
| Bảng sản phẩm bán trong ngày | `/revenue` | ✅ Đã làm (tên, SKU, danh mục, SL, đơn giá, thành tiền, lợi nhuận) |
| Nút "Quay lại" | `/revenue` | ✅ Đã làm |
| Biểu đồ theo tuần | `/analytics/revenue?period=week` | ❌ Không làm (web cũng chưa dùng) |
| Nút "Tạo dữ liệu mẫu" | `/revenue` | ❌ Không làm (chỉ dùng khi dev web) |
