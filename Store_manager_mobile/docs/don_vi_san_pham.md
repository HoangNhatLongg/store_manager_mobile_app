# Luồng gán đơn vị mặc định & đơn vị gốc cho sản phẩm

Tài liệu này mô tả luồng dùng đơn vị của sản phẩm trên cả web lẫn ứng dụng
Android, tập trung vào hai khái niệm:

- **Đơn vị bán lẻ mặc định** (`isDefault = true`, `conversionQty = 1`): đơn vị
  dùng để bán lẻ và **đếm tồn kho**.
- **Đơn vị gốc nhập hàng** (`isPurchase = true`): đơn vị dùng khi nhập hàng từ
  nhà cung cấp (thùng, lốc…). Tồn kho vẫn được cộng theo đơn vị bán lẻ mặc định.

## 1. Quy tắc bắt buộc

| Quy tắc | Mã lỗi / HTTP |
|---|---|
| Sản phẩm phải có đơn vị bán lẻ mặc định | `UNIT_REQUIRED` / 400 |
| Đơn vị bán lẻ mặc định luôn có `conversionQty = 1` | tự động ép |
| Chỉ một đơn vị bán lẻ mặc định cho mỗi sản phẩm | tự động bỏ cờ cũ |
| Chỉ một đơn vị gốc nhập hàng cho mỗi sản phẩm | tự động bỏ cờ cũ |
| Không bỏ được cờ mặc định của đơn vị mặc định duy nhất | `CANNOT_REMOVE_LAST_DEFAULT` / 409 |
| Không xoá được đơn vị mặc định duy nhất của sản phẩm | `CANNOT_DELETE_LAST_DEFAULT` / 409 |

## 2. Luồng trên web

### 2.1. Thêm / sửa sản phẩm

```text
Trang /products -> Nut "Thêm sản phẩm" / Nut sửa
        |
        v
ChonDonViSanPham (components/chon-don-vi-san-pham.tsx)
        |
        +-> Chọn "Đơn vị bán lẻ mặc định"   (bắt buộc)
        +-> Chọn "Đơn vị gốc (nhập hàng)"   (tuỳ chọn)
        +-> Nhập hệ số quy đổi              (chỉ hiện khi đơn vị gốc khác đơn vị mặc định)
        |
        v
POST /api/products  hoặc  PUT /api/products/:id
        body: { ..., defaultUnit: { unitId, price },
                purchaseUnit: { unitId, conversionQty, price } | null }
        |
        v
capNhatDonViChoSanPham() trong transaction
```

Cùng logic này dùng cho hộp thoại thêm nhanh (`components/quick-add-product-dialog.tsx`).

### 2.2. Màn hình đơn vị của sản phẩm

Trang `/products/[id]/units` cho phép:

- Thêm / sửa / xoá từng đơn vị.
- Nút "Đặt làm mặc định" (ngôi sao) để chuyển nhanh đơn vị bán lẻ mặc định.
- Cảnh báo vàng nếu sản phẩm chưa có đơn vị bán lẻ mặc định.

### 2.3. Luồng nhập hàng

Trang `/import` cho phép chọn đơn vị gốc cho từng dòng hàng:

- Mặc định chọn đơn vị có `isPurchase = true`, sau đó tới đơn vị `conversionQty = 1`.
- Số lượng nhập được hiểu theo đơn vị gốc, hiển thị "= X đơn vị lẻ" bên dưới.
- Sản phẩm đã tồn tại mà chưa chọn đơn vị gốc sẽ bị chặn khi lưu phiếu.

## 3. Luồng trên Android

### 3.1. Thêm / sửa sản phẩm

```text
ManHinhSanPham -> Bottom sheet sản phẩm (bottom_sheet_san_pham.xml)
        |
        +-> Spinner "Đơn vị bán lẻ mặc định"  (bắt buộc)
        +-> Spinner "Đơn vị gốc (nhập hàng)"   (tuỳ chọn)
        +-> Ô "Hệ số quy đổi"                (chỉ hiện khi đơn vị gốc khác đơn vị mặc định)
        |
        v
DuLieuSanPham (defaultUnit, purchaseUnit)
        |
        v
POST/PUT /api/v1/products
```

Model liên quan trong `du_lieu/DoiTuongSanPham.kt`:

- `DuLieuDonViMacDinh(unitId, price)`
- `DuLieuDonViGoc(unitId, conversionQty, price)`

### 3.2. Màn hình đơn vị của sản phẩm

`ManHinhDonViSanPham` cho phép thêm / sửa / xoá từng đơn vị:

- Bật "Đơn vị mặc định" sẽ khoá ô hệ số quy đổi ở giá trị 1.
- Nếu đơn vị đang là mặc định duy nhất thì không cho tắt công tắc.
- Lỗi từ server (ví dụ không bỏ được mặc định duy nhất) được hiển thị nguyên văn.

### 3.3. Đồng bộ offline

Bảng cục bộ `don_vi_san_pham` lưu thêm cột `laDonViGoc` để biết đơn vị nào là
đơn vị gốc khi app mất mạng. Xem `docs/offline_dong_bo.md`.

## 4. File quan trọng

| Nền tảng | File | Vai trò |
|---|---|---|
| Web | `src/lib/don-vi-san-pham.ts` | Logic dùng chung: `chuyenThamSoDonVi`, `ganDonViChoSanPham`, `capNhatDonViChoSanPham` |
| Web | `src/components/chon-don-vi-san-pham.tsx` | Form chọn đơn vị mặc định + đơn vị gốc |
| Web | `src/app/api/products/route.ts` | Tạo sản phẩm kèm đơn vị (transaction) |
| Web | `src/app/api/products/[id]/route.ts` | Sửa sản phẩm kèm đơn vị (transaction) |
| Web | `src/app/api/product-units/route.ts` | CRUD đơn vị, chặn xoá mặc định duy nhất |
| Web | `src/app/api/v1/products/route.ts` | API mobile tạo sản phẩm kèm đơn vị |
| Web | `src/app/api/v1/products/[id]/route.ts` | API mobile sửa sản phẩm kèm đơn vị |
| Web | `src/app/api/v1/product-units/route.ts` | API mobile CRUD đơn vị |
| Web | `src/lib/nhap-hang.ts` | Quy đổi đơn vị gốc khi nhập hàng, tăng tồn kho |
| Android | `du_lieu/DoiTuongSanPham.kt` | Model sản phẩm + đơn vị |
| Android | `ManHinhSanPham.kt` | Form thêm/sửa sản phẩm có chọn đơn vị |
| Android | `ManHinhDonViSanPham.kt` | Màn hình quản lý đơn vị của sản phẩm |
| Android | `du_lieu/DichVuSanPham.kt` | Gọi API, ném `LoiApiException` kèm mã lỗi |

## 5. Script kiểm tra

| Script | Mục đích |
|---|---|
| `scripts/kiem-tra-don-vi.ts` | Rà soát dữ liệu hiện có: thiếu đơn vị, nhiều đơn vị mặc định/gốc |
| `scripts/kiem-tra-luong-don-vi.ts` | Kiểm thử luồng: tạo, đổi mặc định, bỏ đơn vị gốc, chặn lỗi |
| `scripts/gan-don-vi-mac-dinh.ts` | Gán đơn vị mặc định cho sản phẩm cũ còn thiếu |
| `scripts/chuan-hoa-don-vi-mac-dinh.ts` | Chuẩn hoá `conversionQty = 1` cho đơn vị mặc định |

Chạy: `npx tsx scripts/kiem-tra-luong-don-vi.ts`
