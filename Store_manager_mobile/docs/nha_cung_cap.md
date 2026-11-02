# Quản lý Nhà cung cấp (P7)

## 1. Mục đích

Cho phép quản lý thông tin nhà cung cấp trên điện thoại: thêm mới, sửa,
xoá và tra cứu nhanh theo tên, số điện thoại hoặc email.

Nhà cung cấp là dữ liệu bắt buộc cho màn hình Nhập hàng (P8) và Lịch sử nhập
(P10).

## 2. Luồng hoạt động

```text
Menu màn hình chính -> Nhà cung cấp
        |
        v
ManHinhNhaCungCap.onStart()
        |
        +-> DichVuNhaCungCap.taiDanhSach(tuKhoa, baoGomAn)
        |       GET /api/v1/suppliers?search=&includeHidden=
        |
        +-> O tim kiem (debounce 350ms)
        |
        +-> Nut "Thêm nhà cung cấp" / Nut sửa
                |
                v
        moBottomSheetForm(hienLienHe = true)
                |
                v
        DichVuNhaCungCap.taoNhaCungCap() / suaNhaCungCap()
                POST /api/v1/suppliers
                PUT  /api/v1/suppliers/:id
```

Xoá nhà cung cấp:

```text
Nut xoa -> xacNhanXoaBanGhi() -> MaterialAlertDialogBuilder
        |
        v
DichVuNhaCungCap.xoaNhaCungCap()  -> DELETE /api/v1/suppliers/:id
        |
        +-> Server: neu nha cung cap da co phieu nhap -> an (isActive = false)
        +-> Server: neu chua co phieu nhap -> xoa han
```

## 3. Các file liên quan

| File | Vai trò |
|---|---|
| `du_lieu/DoiTuongDanhMuc.kt` | Model `NhaCungCap`, `DuLieuNhaCungCap` |
| `du_lieu/DichVuNhaCungCap.kt` | Lớp gọi API cho nhà cung cấp |
| `ManHinhNhaCungCap.kt` | Activity màn hình nhà cung cấp |
| `store-manager_web/src/app/api/v1/suppliers/route.ts` | API mobile GET/POST |
| `store-manager_web/src/app/api/v1/suppliers/[id]/route.ts` | API mobile PUT/DELETE |

## 4. Bảng database liên quan

| Bảng | Trường dùng |
|---|---|
| `suppliers` | `id`, `name`, `phone`, `email`, `address`, `isActive` |
| `import_invoices` | `supplierId` — dùng để đếm số phiếu nhập |

## 5. Endpoint API được dùng

| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/v1/suppliers?search=&includeHidden=` | Danh sách nhà cung cấp kèm số phiếu nhập |
| POST | `/api/v1/suppliers` | Tạo nhà cung cấp mới |
| PUT | `/api/v1/suppliers/:id` | Cập nhật nhà cung cấp |
| DELETE | `/api/v1/suppliers/:id` | Xoá hoặc ẩn nhà cung cấp |

## 6. Mã lỗi chuẩn

| Mã lỗi | HTTP | Ý nghĩa |
|---|---|---|
| `VALIDATION_ERROR` | 400 | Thiếu tên nhà cung cấp |
| `NOT_FOUND` | 404 | Nhà cung cấp không còn tồn tại |
| `SERVER_ERROR` | 500 | Lỗi máy chủ |

## 7. Ghi chú

- Nhà cung cấp đã có phiếu nhập **không bị xoá hẳn** mà được ẩn đi, vì phiếu
  nhập cần thông tin nhà cung cấp để đối soát tồn kho.
- Bản ghi đang ẩn hiển thị nhãn "ĐANG ẨN" và làm mờ cả dòng.
