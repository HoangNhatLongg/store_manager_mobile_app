# Quản lý Đơn vị tính (P5)

## 1. Mục đích

Cho phép quản lý danh mục đơn vị tính (lon, thùng, chai, kg…) trên điện thoại.
Đơn vị tính là dữ liệu nền cho tính năng bán hàng theo nhiều đơn vị quy đổi
(`ProductUnit`).

Bám sát trang web `/units`:

- Danh sách đơn vị kèm số sản phẩm sử dụng đơn vị đó.
- Đánh dấu đơn vị cơ bản (đơn vị tồn kho được tính theo).
- Thêm mới, sửa, xoá đơn vị.

## 2. Luồng hoạt động

```text
Menu màn hình chính -> Đơn vị tính
        |
        v
ManHinhDonViTinh.onStart()
        |
        +-> DichVuDonViTinh.taiDanhSach(tuKhoa)
        |       GET /api/v1/units?search=
        |
        +-> Nut "Thêm đơn vị" / Nut sửa
                |
                v
        moBottomSheetForm(hienVietTat = true, hienDonViCoBan = true)
                |
                v
        DichVuDonViTinh.taoDonViTinh() / suaDonViTinh()
                POST /api/v1/units
                PUT  /api/v1/units/:id
```

Xoá đơn vị:

```text
Nut xoa -> xacNhanXoaBanGhi()
        |
        +-> lyDoKhongTheXoa(): soSanPham > 0 -> thong bao, khong cho xoa
        |
        v
MaterialAlertDialogBuilder -> xac nhan
        |
        v
DichVuDonViTinh.xoaDonViTinh()  -> DELETE /api/v1/units/:id
```

## 3. Các file liên quan

| File | Vai trò |
|---|---|
| `du_lieu/DoiTuongDanhMuc.kt` | Model `DonViTinh`, `DuLieuDonViTinh` |
| `du_lieu/DichVuDonViTinh.kt` | Lớp gọi API cho đơn vị tính |
| `ManHinhDonViTinh.kt` | Activity màn hình đơn vị tính |
| `ManHinhDonViSanPham.kt` | Màn hình gán đơn vị quy đổi cho một sản phẩm (đã có sẵn) |
| `TrinhBayDonViSanPham.kt` | Adapter hiển thị đơn vị của một sản phẩm |
| `store-manager_web/src/app/api/v1/units/route.ts` | API mobile GET/POST |
| `store-manager_web/src/app/api/v1/units/[id]/route.ts` | API mobile PUT/DELETE |

## 4. Bảng database liên quan

| Bảng | Trường dùng |
|---|---|
| `units` | `id`, `name`, `abbreviation` (unique), `isBaseUnit` |
| `product_units` | `unitId` — dùng để đếm số sản phẩm dùng đơn vị này |

## 5. Endpoint API được dùng

| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/v1/units?search=` | Danh sách đơn vị tính kèm số sản phẩm |
| POST | `/api/v1/units` | Tạo đơn vị tính mới |
| PUT | `/api/v1/units/:id` | Cập nhật đơn vị tính |
| DELETE | `/api/v1/units/:id` | Xoá đơn vị tính |

## 6. Mã lỗi chuẩn

| Mã lỗi | HTTP | Ý nghĩa |
|---|---|---|
| `VALIDATION_ERROR` | 400 | Thiếu tên hoặc viết tắt |
| `DUPLICATE` | 400 | Viết tắt đơn vị đã tồn tại |
| `UNIT_IN_USE` | 409 | Đơn vị đang được dùng cho sản phẩm |
| `NOT_FOUND` | 404 | Đơn vị không còn tồn tại |
| `SERVER_ERROR` | 500 | Lỗi máy chủ |

## 7. Ghi chú

- Viết tắt (`abbreviation`) là khoá duy nhất trong database, server tự chuyển
  chữ thường trước khi lưu.
- Đơn vị cơ bản được đánh dấu bằng nhãn "Đơn vị cơ bản" trên danh sách.
