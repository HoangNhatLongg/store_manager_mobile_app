# Quản lý Khách hàng (P6)

## 1. Mục đích

Cho phép quản lý thông tin khách hàng ngay trên điện thoại: thêm mới, sửa,
xoá và tra cứu nhanh theo tên, số điện thoại hoặc email.

Khách hàng cũng là dữ liệu được gán vào hoá đơn bán (màn hình POS đã dùng
`GET /api/v1/customers`).

## 2. Luồng hoạt động

```text
Menu màn hình chính -> Khách hàng
        |
        v
ManHinhKhachHang.onStart()
        |
        +-> DichVuKhachHang.taiDanhSach(tuKhoa, baoGomAn)
        |       GET /api/v1/customers?search=&includeHidden=
        |
        +-> O tim kiem (debounce 350ms)
        |
        +-> Nut "Thêm khách hàng" / Nut sửa
                |
                v
        moBottomSheetForm(hienLienHe = true)
                |
                v
        DichVuKhachHang.taoKhachHang() / suaKhachHang()
                POST /api/v1/customers
                PUT  /api/v1/customers/:id
```

Xoá khách hàng:

```text
Nut xoa -> xacNhanXoaBanGhi() -> MaterialAlertDialogBuilder
        |
        v
DichVuKhachHang.xoaKhachHang()  -> DELETE /api/v1/customers/:id
        |
        +-> Server: neu khach da co hoa don -> an (isActive = false)
        +-> Server: neu chua co hoa don -> xoa han
```

## 3. Các file liên quan

| File | Vai trò |
|---|---|
| `du_lieu/DoiTuongDanhMuc.kt` | Model `KhachHang`, `DuLieuKhachHang` |
| `du_lieu/DichVuKhachHang.kt` | Lớp gọi API cho khách hàng |
| `ManHinhKhachHang.kt` | Activity màn hình khách hàng |
| `ManHinhBanHang.kt` | Màn hình POS dùng chung model `KhachHang` |
| `store-manager_web/src/app/api/v1/customers/route.ts` | API mobile GET/POST |
| `store-manager_web/src/app/api/v1/customers/[id]/route.ts` | API mobile PUT/DELETE |

## 4. Bảng database liên quan

| Bảng | Trường dùng |
|---|---|
| `customers` | `id`, `name`, `phone`, `email`, `address`, `points`, `isActive` |
| `sale_invoices` | `customerId` — dùng để đếm số hoá đơn của khách hàng |

## 5. Endpoint API được dùng

| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/v1/customers?search=&includeHidden=` | Danh sách khách hàng kèm số hoá đơn |
| POST | `/api/v1/customers` | Tạo khách hàng mới |
| PUT | `/api/v1/customers/:id` | Cập nhật khách hàng |
| DELETE | `/api/v1/customers/:id` | Xoá hoặc ẩn khách hàng |

## 6. Mã lỗi chuẩn

| Mã lỗi | HTTP | Ý nghĩa |
|---|---|---|
| `VALIDATION_ERROR` | 400 | Thiếu tên khách hàng |
| `NOT_FOUND` | 404 | Khách hàng không còn tồn tại |
| `SERVER_ERROR` | 500 | Lỗi máy chủ |

## 7. Ghi chú

- Khách hàng đã phát sinh hoá đơn **không bị xoá hẳn** mà được ẩn đi, vì hoá
  đơn cũ cần thông tin khách để đối soát. Server trả về `isActive = false`.
- Bản ghi đang ẩn hiển thị nhãn "ĐANG ẨN" và làm mờ cả dòng.
