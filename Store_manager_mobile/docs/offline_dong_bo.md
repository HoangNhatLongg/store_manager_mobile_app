# Offline và đồng bộ dữ liệu

> Hạng mục trong plan: P14 — Offline-first (Room + sync queue)
> Ngày hoàn thành: 02/11/2026
> Trạng thái: ✅ Hoàn thành

## 1. Mục đích

Cho phép ứng dụng vẫn dùng được khi mất mạng: dữ liệu được lưu cục bộ trong
SQLite, các thao tác bán hàng được xếp vào hàng đợi đồng bộ và tự gửi lại khi
có mạng.

## 2. Luồng hoạt động

### 2.1. Kiến trúc tổng thể

```text
UI (Activity / Adapter)
  ↓
KhoDuLieuSanPham  ← đọc ưu tiên server, fallback Room khi mất mạng
  ↓            ↘ ghi thao tác vào HangDongBo
  ↓
WorkerDongBoHoaDon + WorkerDongBoSanPham  → Mobile API  → PostgreSQL
```

### 2.2. Luồng đồng bộ gia tăng

```text
App mở ứng dụng / mỗi 15 phút (WorkerDongBoSanPham)
  → Gọi GET /api/v1/categories, /units, /customers
  → Gọi GET /api/v1/products để lấy sản phẩm và tồn kho mới
  → Ghi vào Room (ghi đè theo id)
  → Tồn kho trên máy luôn khớp với máy chủ
```

`WorkerDongBoSanPham` được lên lịch trong `UngDungStoreManager.onCreate()`:

- `lenLichDinhKy()` — chạy mỗi 15 phút, chỉ khi có mạng.
- `lenLichChayNgay()` — chạy ngay lần đầu để máy có sẵn dữ liệu bán hàng.

### 2.3. Luồng bán hàng offline

```text
Nhân viên bán hàng khi mất mạng
  → POST /api/v1/sales thất bại với mã lỗi LOI_KET_NOI
  → KhoDuLieuSanPham.luuHoaDonOffline():
      ├─ Ghi hoá đơn vào bảng hoa_don_ban (trạng thái CHUA_DONG_BO)
      ├─ Ghi các dòng hàng vào dong_hoa_don_ban
      ├─ Ghi một bản ghi vào hang_dong_bo với operationId (UUID)
      └─ Trừ tồn kho trong bảng san_pham cục bộ
  → Hiển thị màn hình thành công kèm nhãn "đã lưu offline"
  → WorkerDongBoHoaDon chạy khi có mạng:
      ├─ Thành công → xoá bản ghi hàng đợi, đánh dấu hoá đơn ĐA_DONG_BO
      ├─ 409 STOCK_CONFLICT → đánh dấu XUNG_DOT, hiện cảnh báo cho người dùng
      ├─ 400 VALIDATION_ERROR → đánh dấu THAT_BAI, hiện lỗi
      └─ Lỗi mạng → trả về hàng đợi, WorkerManager thử lại
```

Quy tắc bắt buộc: retry phải dùng **cùng `operationId`** để không tạo hoá đơn
trùng trên server. Bản ghi hàng đợi lưu nguyên payload JSON của
`YeuCauTaoHoaDon`, nên giá trị `operationId` luôn giữ nguyên qua mọi lần thử.

### 2.4. Bảng SQLite cục bộ

| Bảng Room | Thực thể | Trường chính | Ghi chú |
|---|---|---|---|
| `san_pham` | `TheSanPham` | `id`, `ten`, `maSku`, `maVach`, `giaNhap`, `giaBan`, `tonKho`, `tonToiThieu`, `maDanhMuc`, `dangHoatDong`, `capNhatLuc` | Nguồn dữ liệu cho UI |
| `danh_muc` | `TheDanhMuc` | `id`, `ten`, `slug` | Bộ lọc |
| `don_vi_tinh` | `TheDonViTinh` | `id`, `ten`, `vietTat`, `laDonViCoBan` | |
| `don_vi_san_pham` | `TheDonViSanPham` | `id`, `maSanPham`, `maDonViTinh`, `soLuongQuyDoi`, `gia`, `laMacDinh` | Quy đổi và giá bán |
| `khach_hang` | `TheKhachHang` | `id`, `ten`, `soDienThoai` | |
| `nha_cung_cap` | `TheNhaCungCap` | `id`, `ten` | |
| `hoa_don_ban` | `TheHoaDonBan` | `id`, `operationId`, `soHoaDon`, `maKhachHang`, `tamTinh`, `giamGia`, `tongCong`, `phuongThucThanhToan`, `trangThai` | `trangThai`: CHUA_DONG_BO / DA_DONG_BO / XUNG_DOT / THAT_BAI |
| `dong_hoa_don_ban` | `TheDongHoaDonBan` | `id`, `maHoaDon`, `maSanPham`, `soLuong`, `heSoQuyDoi`, `donGia`, `thanhTien` | |
| `hang_dong_bo` | `TheHangDongBo` | `id`, `operationId`, `loaiThaoTac`, `duLieuJson`, `soLanThu`, `thongBaoLoi`, `trangThai` | Hàng đợi gửi lên server |

Tên file database: `cua_hang_cuc_bo.db`.

### 2.5. Nhánh xử lý lỗi

| Tình huống | Xử lý |
|---|---|
| Mất mạng khi đọc dữ liệu | Đọc từ Room, hiện thông báo "không có mạng, đang dùng dữ liệu tải về lần trước" |
| Mất mạng khi ghi thao tác | Ghi vào hàng đợi cục bộ, vẫn hiển thị thành công cho nhân viên |
| Xung đột tồn kho khi đồng bộ | Đánh dấu XUNG_DOT, hoá đơn không được gửi lại tự động |
| Hoá đơn bị từ chối | Đánh dấu THAT_BAI, lưu thông báo lỗi |

### 2.6. Xung đột tồn kho khi hai thiết bị cùng bán sản phẩm cuối cùng

Tình huống: tồn kho còn 1, hai nhân viên trên hai máy cùng bán cùng một món.

```text
Máy A (offline)              Máy B (offline)
  → tạo hoá đơn offline        → tạo hoá đơn offline
  → trừ tồn kho cục bộ: 1→0    → trừ tồn kho cục bộ: 1→0
                              ↓
                    Có mạng, cả hai gửi lên server
                              ↓
        Server (nguồn chuẩn duy nhất, transaction + updateMany có điều kiện
        stock >= soLuong) → chỉ một máy thắng
                              ↓
        Máy thắng:  201 SYNCED  → hoá đơn ĐA_DONG_BO, xoá khỏi hàng đợi
        Máy thua:   409 STOCK_CONFLICT → hoá đơn XUNG_DOT, yêu cầu người dùng
                                          quyết định hoàn tiền hoặc điều chỉnh tồn kho
```

Cơ chế an toàn phía server nằm trong `store-manager_web/src/app/api/v1/sales/route.ts`:

1. Kiểm tra tồn kho trong transaction.
2. `tx.product.updateMany({ where: { id, stock: { gte: soLuong } }, data: { stock: { decrement } } })`
   — nếu `count !== 1` thì tồn kho vừa thay đổi, trả `409 STOCK_CONFLICT`.
3. Bảng `sync_operations` giữ vai trò idempotency: gửi lại cùng `operationId`
   trả về `200 ALREADY_PROCESSED` thay vì tạo hoá đơn trùng.

## 3. Các file liên quan

| Loại | Đường dẫn | Vai trò |
|---|---|---|
| Cấu hình | `app/build.gradle.kts` | Thêm `room-runtime`, `room-ktx`, `room-compiler`, `work-runtime-ktx`, plugin KSP |
| Cấu hình | `gradle/libs.versions.toml` | Khai báo version Room 2.7.2, KSP 2.2.10-2.0.2, WorkManager 2.10.1 |
| Cấu hình | `gradle.properties` | `android.disallowKotlinSourceSets=false` cho AGP 9 + KSP |
| Cơ sở dữ liệu | `du_lieu/the_the_phong/CSDLPhong.kt` | Lớp cơ sở dữ liệu Room (mới) |
| Thực thể | `du_lieu/the_the_phong/TheSanPham.kt` | Thực thể Room sản phẩm (mới) |
| Thực thể | `du_lieu/the_the_phong/TheDanhMuc.kt` | Thực thể Room danh mục, đơn vị, khách hàng, nhà cung cấp (mới) |
| Thực thể | `du_lieu/the_the_phong/TheHoaDonBan.kt` | Thực thể Room hoá đơn, dòng hàng, hàng đợi (mới) |
| DAO | `du_lieu/the_the_phong/DaoSanPham.kt` | Truy vấn sản phẩm, hoá đơn, hàng đợi (mới) |
| DAO | `du_lieu/the_the_phong/DaoDanhMuc.kt` | Truy vấn danh mục, đơn vị, khách hàng, nhà cung cấp (mới) |
| Chuyển đổi | `du_lieu/the_the_phong/ChuyenDoiDuLieu.kt` | Chuyển đổi model ↔ thực thể (mới) |
| Repository | `du_lieu/KhoDuLieuSanPham.kt` | Đọc/ghi qua Room, tạo hoá đơn offline (mới) |
| Worker | `du_lieu/WorkerDongBoHoaDon.kt` | Worker đồng bụ hoá đơn offline (mới) |
| Worker | `du_lieu/WorkerDongBoSanPham.kt` | Worker đồng bộ sản phẩm định kỳ (mới) |
| Ứng dụng | `UngDungStoreManager.kt` | Lên lịch worker khi app khởi động (mới) |
| Màn hình | `ManHinhChiTietHoaDon.kt` | Xử lý mã lỗi `LOI_KET_NOI` → lưu offline |
| Màn hình | `ManHinhBanHang.kt` | Đọc sản phẩm qua `KhoDuLieuSanPham` |
| Màn hình | `ManHinhHoaDonThanhCong.kt` | Hiển thị nhãn "đã lưu offline" |
| Dịch vụ | `du_lieu/DichVuBanHang.kt` | Gửi hoá đơn lên server (dùng chung với P1) |
| Model | `du_lieu/DoiTuongSanPham.kt` | Model dùng chung cho UI và Room |

## 4. Bảng database liên quan

Bảng PostgreSQL liên quan:

| Bảng | Trường dùng | Ghi chú |
|---|---|---|
| `sync_operations` | `operationId`, `entityType`, `entityId`, `status`, `result`, `errorCode`, `errorMessage` | Bảng idempotency phía server |
| `products` | `stock`, `updatedAt` | Nguồn chuẩn của tồn kho |
| `sale_invoices` | `id`, `invoiceNumber`, `totalAmount`, `status` | Đích đến của đồng bộ |
| `sale_invoice_items` | `saleInvoiceId` | Dòng hàng của hoá đơn |

## 5. Endpoint API được dùng

| Method | Đường dẫn | Mục đích |
|---|---|---|
| GET | `/api/v1/categories`, `/units`, `/customers` | Đồng bộ danh mục (đã có) |
| GET | `/api/v1/products` | Đồng bộ sản phẩm và tồn kho (đã có) |
| POST | `/api/v1/sales` | Gửi hoá đơn offline lên server (đã có) |

## 6. Đối chiếu với Web

| Chức năng | Vị trí trên Web | Trạng thái mobile |
|---|---|---|
| Dữ liệu cục bộ khi mất mạng | Không có (web cần mạng) | ✅ Đã có |
| Hàng đợi thao tác chờ gửi | Không có | ✅ Đã có |
| Đồng bộ nền bằng WorkManager | Không có | ✅ Đã có |
| Xử lý xung đột tồn kho | Có ở `POST /api/v1/sales` | ✅ Có sẵn phía server |

## 7. Ghi chú triển khai

- Đã kiểm thử bằng script `store-manager_web/kiem_tra_xung_dot.mjs`:
  hai request đồng thời với tồn kho = 1 cho kết quả đúng là một `201 SYNCED`
  và một `409 STOCK_CONFLICT`, tồn kho còn lại = 0.
- Đã kiểm thử idempotency: gửi lại cùng `operationId` trả về
  `200 ALREADY_PROCESSED` và không tạo hoá đơn trùng.
- Cần kiểm thử thực tế trên thiết bị: tắt mạng, bán hàng, bật mạng lại và
  xác nhận hoá đơn được đồng bộ, cùng trường hợp xung đột tồn kho.
- Khi thêm bảng mới vào `CSDLPhong`, tăng `version` trong `@Database`.
  Hiện dùng `fallbackToDestructiveMigration()` nên trong lúc phát triển có thể
  xoá dữ liệu cục bộ mà không báo lỗi.
