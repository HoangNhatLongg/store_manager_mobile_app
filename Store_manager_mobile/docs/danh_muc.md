# Quản lý Danh mục (P4)

## 1. Mục đích

Cho phép người dùng quản lý danh mục sản phẩm ngay trên điện thoại, thay vì
phải mở trên web. Danh mục là dữ liệu nền dùng để lọc sản phẩm ở màn hình
bán hàng, màn hình tồn kho và khi tạo phiếu nhập.

Bám sát trang web `/categories`:

- Danh sách danh mục kèm số sản phẩm thuộc danh mục.
- Thêm mới, sửa, xoá danh mục.
- Không cho xoá danh mục còn sản phẩm.

## 2. Luồng hoạt động

```text
Menu màn hình chính -> Danh mục
        |
        v
ManHinhDanhMuc.onStart()
        |
        +-> DichVuDanhMuc.taiDanhSach(tuKhoa, baoGomAn)
        |       GET /api/v1/categories?search=&includeHidden=
        |
        +-> TrinhBayBanGhi (danh sach card)
        |
        +-> Nut "Thêm danh mục" / Nut sửa
                |
                v
        moBottomSheetForm(...)  -> BottomSheetDialog (92% man hinh)
                |
                v
        DichVuDanhMuc.taoDanhMuc() / suaDanhMuc()
                POST /api/v1/categories
                PUT  /api/v1/categories/:id
                |
                v
        taiDanhSach()  -> cap nhat danh sach
```

Xoá danh mục:

```text
Nut xoa -> xacNhanXoaBanGhi()
        |
        +-> lyDoKhongTheXoa(): soSanPham > 0 -> thong bao, khong cho xoa
        |
        v
MaterialAlertDialogBuilder -> xac nhan
        |
        v
DichVuDanhMuc.xoaDanhMuc()  -> DELETE /api/v1/categories/:id
```

## 3. Các file liên quan

| File | Vai trò |
|---|---|
| `du_lieu/DoiTuongDanhMuc.kt` | Model `DanhMuc`, `DuLieuDanhMuc` và các response |
| `du_lieu/DichVuDanhMuc.kt` | Lớp gọi API, trả `Result` kèm mã lỗi chuẩn |
| `ManHinhDanhMuc.kt` | Activity màn hình danh mục |
| `ManHinhQuanLyDuLieu.kt` | Activity base dùng chung cho 4 màn hình CRUD master |
| `TrinhBayBanGhi.kt` | Adapter dùng chung hiển thị danh sách card |
| `res/layout/khung_quan_ly_du_lieu.xml` | Bố cục chung của màn hình |
| `res/layout/dong_ban_ghi_quan_ly.xml` | Bố cục chung của một dòng dữ liệu |
| `res/layout/bottom_sheet_ban_ghi.xml` | Form thêm/sửa dùng chung |
| `store-manager_web/src/app/api/v1/categories/route.ts` | API mobile GET/POST |
| `store-manager_web/src/app/api/v1/categories/[id]/route.ts` | API mobile PUT/DELETE |

## 4. Bảng database liên quan

| Bảng | Trường dùng |
|---|---|
| `categories` | `id`, `name`, `slug`, `description`, `isActive` |
| `products` | `categoryId` — dùng để đếm số sản phẩm của danh mục |

## 5. Endpoint API được dùng

| Method | Endpoint | Mô tả |
|---|---|---|
| GET | `/api/v1/categories?search=&includeHidden=` | Danh sách danh mục kèm số sản phẩm |
| POST | `/api/v1/categories` | Tạo danh mục mới (tự sinh slug) |
| PUT | `/api/v1/categories/:id` | Cập nhật danh mục |
| DELETE | `/api/v1/categories/:id` | Xoá danh mục |

## 6. Mã lỗi chuẩn

| Mã lỗi | HTTP | Ý nghĩa |
|---|---|---|
| `VALIDATION_ERROR` | 400 | Thiếu tên danh mục |
| `DUPLICATE` | 400 | Tên danh mục đã tồn tại |
| `CATEGORY_HAS_PRODUCTS` | 409 | Danh mục còn sản phẩm, không xoá được |
| `NOT_FOUND` | 404 | Danh mục không còn tồn tại |
| `SERVER_ERROR` | 500 | Lỗi máy chủ |

## 7. Ghi chú

- Trường `isActive` được trả kèm để hiển thị nhãn "ĐANG ẨN" trên bản ghi bị ẩn.
- App không có API riêng để bật/tắt danh mục, vì vậy hiện chỉ hỗ trợ xoá.
