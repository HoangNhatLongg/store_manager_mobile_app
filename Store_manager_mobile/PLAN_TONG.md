# Kế hoạch phát triển ứng dụng Android Store Manager

Tài liệu này là **todo list chính** của dự án. Sau mỗi tính năng hoàn thành
phải cập nhật lại trạng thái tương ứng (đổi `- [ ]` thành `- [x]`) và ghi
thêm ngày hoàn thành.

Quy tắc viết code áp dụng cho toàn bộ dự án: xem
[`QUI_TAC_VIET_CODE.md`](./QUI_TAC_VIET_CODE.md).

Danh mục tài liệu từng tính năng: xem [`docs/README.md`](./docs/README.md).

---

## 0. Quy ước trạng thái

| Ký hiệu | Ý nghĩa |
|---|---|
| `- [ ]` | Chưa làm |
| `- [x]` | Đã hoàn thành |
| `- [~]` | Đang làm |
| `⛔` | Bị chặn bởi vấn đề khác |

Nguyên tắc: **mỗi tính năng hoàn thành phải kèm 1 file `.md` trong
`docs/`** mô tả luồng hoạt động, file liên quan, bảng database liên quan.

---

## 1. Bảng ưu tiên tổng quan

| # | Hạng mục | Ưu tiên | Phụ thuộc | Tài liệu |
|---|---|---|---|---|
| P0 | Sửa lỗi hiện có | Cao | — | [`docs/sua_loi.md`](./docs/sua_loi.md) |
| P1 | Bán hàng POS (tạo hoá đơn thật) | Cao | P0 | [`docs/ban_hang_pos.md`](./docs/ban_hang_pos.md) |
| P2 | Màn hình tồn kho | Cao | P0 | [`docs/ton_kho.md`](./docs/ton_kho.md) |
| P3 | Báo cáo doanh thu | Cao | P0 | [`docs/doanh_thu.md`](./docs/doanh_thu.md) |
| P4 | Quản lý danh mục (CRUD) | Trung bình | P0 + backend | [`docs/danh_muc.md`](./docs/danh_muc.md) |
| P5 | Quản lý đơn vị tính (CRUD) | Trung bình | P0 + backend | [`docs/don_vi_tinh.md`](./docs/don_vi_tinh.md) |
| P6 | Quản lý khách hàng | Trung bình | P0 + backend | [`docs/khach_hang.md`](./docs/khach_hang.md) |
| P7 | Quản lý nhà cung cấp | Trung bình | P0 + backend | [`docs/nha_cung_cap.md`](./docs/nha_cung_cap.md) |
| P8 | Nhập hàng (phiếu nhập) | Trung bình | P1 + P6 + P7 | [`docs/nhap_hang.md`](./docs/nhap_hang.md) |
| P9 | OCR hoá đơn | Trung bình | P8 | [`docs/ocr_hoa_don.md`](./docs/ocr_hoa_don.md) |
| P10 | Lịch sử nhập + huỷ phiếu nhập | Thấp | P8 | [`docs/lich_su_nhap.md`](./docs/lich_su_nhap.md) |
| P11 | Lịch sử bán hàng | Thấp | P1 | [`docs/lich_su_ban.md`](./docs/lich_su_ban.md) |
| P12 | Kiểm tra token + refresh | Thấp | — | [`docs/xac_thuc.md`](./docs/xac_thuc.md) |
| P13 | Màn hình cài đặt | Thấp | — | [`docs/cai_dat.md`](./docs/cai_dat.md) |
| P14 | Offline-first (Room + sync queue) | Rất thấp | P1, P12 | [`docs/offline_dong_bo.md`](./docs/offline_dong_bo.md) |

---

## 2. P0 — Sửa lỗi hiện có

Mục tiêu: xoá các lỗi đã phát hiện trong lần rà soát đầu tiên, để các tính
năng sau không phải gánh lỗi của người trước.

- [x] **P0.1** `POST /api/v1/products` bỏ qua `barcode` khi tạo sản phẩm mới
  → sản phẩm mất mã vạch nhưng API lại tìm kiếm theo `barcode`.
  - File: `store-manager_web/src/app/api/v1/products/route.ts`
  - Sửa: đọc `body.barcode` và gán vào `prisma.product.create`.
  - Kiểm tra: tạo sản phẩm có barcode → tìm kiếm bằng barcode → ra đúng 1 kết quả.
  - **Hoàn thành ngày 02/11/2026** ✅

- [x] **P0.2** Chuẩn hoá trường tiền tệ (`importPrice`, `sellPrice`)
  - Backend: dùng `decimalToString` cho mọi response của `/api/v1/products`.
  - Android: `DuLieuSanPham` dùng `Double` thay vì `String`, thêm hàm
    `chuyenDongTienSangChuoi()` dùng `DecimalFormat("#0.##")`.
  - Loại bỏ trường hợp `""` biến thành chuỗi `"null"`.
  - **Hoàn thành ngày 02/11/2026** ✅
    - Tạo `du_lieu/DinhDangTien.kt` làm nguồn chuẩn duy nhất cho tiền tệ.
    - `SanPham.importPrice/sellPrice`, `DonViSanPham.price`,
      `SanPhamSapHet.sellPrice` đổi sang `Double`.

- [x] **P0.3** Kiểm tra tồn kho của giỏ hàng khi bán
  - `ManHinhBanHang.themSanPhamVaoGio()` hiện chỉ so với tồn lúc tải trang.
  - Phải lấy lại tồn kho mới nhất trước khi gửi `POST /api/v1/sales`.
  - **Hoàn thành ngày 02/11/2026** ✅
    - `moChiTietHoaDon()` gọi lại `taiDanhSach()` để lấy tồn kho mới nhất,
      tự điều chỉnh số lượng các mặt hàng vượt tồn kho.

- [x] **P0.4** `ManHinhSanPham.xacNhanXoaSanPham()` kiểm tra lỗi bằng
  `contains("HTTP_409")` trên message → chuyển sang so sánh `maLoi`.
  - Sửa: `DichVuSanPham.xoaSanPham()` trả về `Result` kèm mã lỗi, không kiểm
    tra chuỗi message.
  - **Hoàn thành ngày 02/11/2026** ✅
    - Tạo `du_lieu/PhanTichLoi.kt` đọc mã lỗi chuẩn từ body lỗi.
    - Mọi lời gọi API trong `DichVuSanPham` dùng `Result` với mã lỗi chuẩn.

- [x] **P0.5** `ManHinhKhoiDong` không kiểm tra token còn hạn
  → gọi `GET /api/v1/me`, xoá token nếu trả 401.
  - **Hoàn thành ngày 02/11/2026** ✅
    - Tạo `du_lieu/DichVuTaiKhoan.kt` và `du_lieu/DoiTuongTaiKhoan.kt`.
    - Lỗi mạng không xoá phiên, chỉ báo lỗi để người dùng thử lại.

- [x] **P0.6** `ManHinhCoThanhTab.dangXuat()` gọi `finishAffinity()` khiến app
  thoát hẳn → đổi sang quay lại `DangNhap` bằng `startActivity` + `finish()`.
  - **Hoàn thành ngày 02/11/2026** ✅

- [x] **P0.7** Kiểm tra rò rỉ Activity do `moManHinh()` dùng
  `FLAG_ACTIVITY_REORDER_TO_FRONT` không `clearTask` → bảo đảm back stack gọn.
  - **Hoàn thành ngày 02/11/2026** ✅
    - `moManHinh()` bổ sung `FLAG_ACTIVITY_CLEAR_TOP`.

- [x] **P0.8** Rà lại toàn bộ chuỗi hiển thị đang đặt trực tiếp trong XML
  (`ton_kho.xml`, `doanh_thu.xml` còn chuỗi cứng) → chuyển hết sang
  `strings.xml`.
  - **Hoàn thành ngày 02/11/2026** ✅

---

## 3. P1 — Bán hàng POS (ưu tiên cao nhất)

Mục tiêu: thay thế màn hình thanh toán "đang phát triển" bằng luồng thật,
bám sát web `/sales`.

- [x] **P1.1** Backend: đã sẵn sàng `POST /api/v1/sales`.
  - Thêm `GET /api/v1/sales/history` cho P11 (đã có, chỉ cần dùng).
  - Bổ sung trường `conversionQty` cho từng dòng hàng để quy đổi đơn vị bán
    về đơn vị cơ bản (áp dụng cho cả `/api/v1/sales` và `/api/sales`).
- [x] **P1.2** Android model:
  - Thêm `MatHangGioHang` trường `donViBan: DonViSanPham?`, `heSoQuyDoi`,
    `soLuongDonViCoBan`, `khoaDong`.
  - Thêm `YeuCauTaoHoaDon(operationId, customerId, items, paymentMethod, discount, notes)`.
  - Thêm `HoaDonDaTao(invoiceNumber, totalAmount, changeAmount, items)`.
- [x] **P1.3** Hộp thoại chọn đơn vị bán (khi sản phẩm có nhiều đơn vị)
  - Tính số lượng tối đa theo `conversionQty`.
  - Lấy giá theo đơn vị từ `productUnit.price`.
- [x] **P1.4** Chọn phương thức thanh toán: `CASH / BANK_TRANSFER / CARD / E_WALLET`.
- [x] **P1.5** Ô "tiền khách đưa" + hiển thị tiền thừa / tiền thiếu, chặn khi thiếu.
- [x] **P1.6** Chọn khách hàng (dùng `GET /api/v1/customers`).
- [x] **P1.7** Ô ghi chú hoá đơn (`notes`).
- [x] **P1.8** Sinh `operationId` (UUID) và gọi `POST /api/v1/sales`.
  - Xử lý lỗi: 409 `STOCK_CONFLICT` → báo tồn kho không đủ, 401 → đăng nhập lại.
- [x] **P1.9** Màn hình thanh toán thành công: số hoá đơn, tổng tiền, tiền thừa.
- [x] **P1.10** Làm mới tồn kho và xoá giỏ hàng sau khi bán thành công.
- [x] **P1.11** Viết `docs/ban_hang_pos.md`.
- [x] **P1.12** Cập nhật `PLAN_TONG.md` và `docs/README.md` sau khi xong.
  - **Hoàn thành ngày 02/11/2026** ✅
- [x] **P1.13** Rà soát lỗi nghiệp vụ sau khi hoàn thành P1.
  - Sửa 11 lỗi nghiệp vụ mà build/typecheck không phát hiện được, xem mục 7
    trong `docs/ban_hang_pos.md`.
  - Nghiêm trọng nhất: `operationId` bị khoá vĩnh viễn sau một lần lỗi tồn
    kho, và sản phẩm nằm ở trang 2+ bị xoá khỏi giỏ khi kiểm tra tồn kho.
  - **Hoàn thành ngày 02/11/2026** ✅

---

## 4. P2 — Màn hình tồn kho

Mục tiêu: thay khung rỗng `ManHinhTonKho.kt` bằng màn hình thật, bám sát
web `/inventory`.

- [x] **P2.1** Layout `ton_kho.xml`: 4 thẻ thống kê (tổng sản phẩm / sắp hết /
  hết hàng / còn hàng) + ô tìm kiếm + bộ lọc danh mục + bộ lọc trạng thái +
  `RecyclerView` + nút phân trang.
- [x] **P2.2** Lấy dữ liệu từ `GET /api/v1/products` (đã có sẵn, hỗ trợ
  `search`, `categoryId`, phân trang).
- [x] **P2.3** Adapter `TrinhBayTongKho` hiển thị: tên, SKU, danh mục, đơn vị,
  tồn kho, tối thiểu, cảnh báo sắp hết.
- [x] **P2.4** Bộ lọc trạng thái: tất cả / còn hàng / sắp hết / hết hàng.
- [x] **P2.5** Nút "Xuất báo cáo" — trên mobile dùng chia sẻ file CSV.
- [x] **P2.6** Viết `docs/ton_kho.md`.
- [x] **P2.7** Backend `GET /api/v1/inventory/stats` để 4 thẻ thống kê đếm
  trên toàn bộ sản phẩm, không phụ thuộc phân trang (mới).
- [x] **P2.8** Hỗ trợ offline: đọc tồn kho từ Room khi mất mạng, có thông báo
  "đang dùng dữ liệu tải về lần trước".

  - **Hoàn thành ngày 02/11/2026** ✅

---

## 5. P3 — Báo cáo doanh thu

Mục tiêu: thay khung rỗng `ManHinhDoanhThu.kt`, bám sát web `/revenue`.

- [x] **P3.1** Backend: thêm `GET /api/v1/analytics/revenue` và
  `GET /api/v1/analytics/revenue-daily` cho mobile (web đang dùng session
  NextAuth nên mobile không gọi được).
- [x] **P3.2** Chọn năm / tháng / ngày.
- [x] **P3.3** Thẻ thống kê: doanh thu, giá vốn, lợi nhuận, biên lợi nhuận,
  số hoá đơn, tổng lượng bán.
- [x] **P3.4** Drill-down: tháng → danh sách ngày → chi tiết sản phẩm trong ngày.
- [x] **P3.5** Biểu đồ cột đơn giản vẽ bằng `View` tùy chỉnh (tránh thêm thư viện).
- [x] **P3.6** Viết `docs/doanh_thu.md`.
- [x] **P3.7** Biểu đồ cột vẽ bằng Canvas trong `BieuDoCot.kt`, có callback
  chạm để drill-down, không thêm thư viện bieu do.
- [x] **P3.8** Thêm mục "Quản lý doanh thu" vào menu màn hình chính.

  - **Hoàn thành ngày 02/11/2026** ✅

---

## 6. P4–P7 — CRUD dữ liệu danh mục

Mỗi nhóm gồm 3 phần: **backend mobile API → model + `DichVu` → màn hình**.

- [x] **P4.1** Backend `POST/PUT/DELETE /api/v1/categories`
  (tự sinh slug, chặn xoá khi còn sản phẩm).
- [x] **P4.2** Android `ManHinhDanhMuc` + `DichVuDanhMuc` + `TrinhBayDanhMuc`.
- [x] **P4.3** Gắn handler menu `menu_danh_muc` trong `ManHinhCoThanhTab`.
- [x] **P4.4** Viết `docs/danh_muc.md`.
  - **Hoàn thành ngày 02/11/2026** ✅
- [x] **P5.1** Backend `POST/PUT/DELETE /api/v1/units`
  (viết tắt unique, đơn vị cơ bản, chặn xoá khi đang được dùng).
- [x] **P5.2** Android `ManHinhDonViTinh` + `DichVuDonViTinh` + `TrinhBayDonViTinh`.
- [x] **P5.3** Gắn handler menu `menu_don_vi`.
- [x] **P5.4** Viết `docs/don_vi_tinh.md`.
  - **Hoàn thành ngày 02/11/2026** ✅
- [x] **P6.1** Backend `POST/PUT/DELETE /api/v1/customers`.
- [x] **P6.2** Android `ManHinhKhachHang` + `DichVuKhachHang` + `TrinhBayKhachHang`.
- [x] **P6.3** Gắn handler menu `menu_khach_hang`.
- [x] **P6.4** Viết `docs/khach_hang.md`.
  - **Hoàn thành ngày 02/11/2026** ✅
  - Khách hàng đã có hoá đơn chỉ bị ẩn (`isActive = false`) chứ không xoá hẳn.
- [x] **P7.1** Backend `POST/PUT/DELETE /api/v1/suppliers`
  (chặn xoá khi đã có phiếu nhập).
- [x] **P7.2** Android `ManHinhNhaCungCap` + `DichVuNhaCungCap` + `TrinhBayNhaCungCap`.
- [x] **P7.3** Gắn handler menu `menu_nha_cung_cap`.
- [x] **P7.4** Viết `docs/nha_cung_cap.md`.
  - **Hoàn thành ngày 02/11/2026** ✅
  - Nhà cung cấp đã có phiếu nhập chỉ bị ẩn (`isActive = false`) chứ không xoá hẳn.

### Ghi chú chung cho P4 – P7

Cả bốn màn hình dùng chung một bộ khung giao diện nên không lặp lại code:

- `ManHinhQuanLyDuLieu` — Activity base chứa tìm kiếm (debounce 350ms), trạng
  thái loading, thông báo lỗi, xác nhận xoá và mở bottom sheet form.
- `TrinhBayBanGhi` — Adapter dùng chung, mỗi màn hình chỉ cần hàm chuyển dữ
  liệu sang `ThongTinHienThi`.
- `bottom_sheet_ban_ghi.xml` — form thêm/sửa dùng chung, cao 92% màn hình,
  tiêu đề cố định, phần giữa cuộn được, nút hành động ghim dưới cùng.

---

## 7. P8 — Nhập hàng (phiếu nhập)

Mục tiêu: bám sát web `/import`.

- [ ] **P8.1** Backend `POST /api/v1/imports` (transaction: tăng tồn kho, cập
  nhật giá nhập, tạo phiếu + dòng hàng, tự tạo sản phẩm mới nếu cần).
- [ ] **P8.2** Màn hình `ManHinhNhapHang`:
  - Chọn nhà cung cấp.
  - Bảng dòng hàng: tên, số lượng, đơn giá, danh mục.
  - Thêm / xoá dòng.
  - Tổng tiền.
- [ ] **P8.3** Hộp thoại gợi ý sản phẩm tương tự khi tên chưa khớp sản phẩm nào.
- [ ] **P8.4** Gắn handler menu `menu_nhap_hang`.
- [ ] **P8.5** Viết `docs/nhap_hang.md`.

---

## 8. P9 — OCR hoá đơn

- [ ] **P9.1** Backend: tạo `POST /api/v1/ocr/process` dùng Bearer token
  (bản web `/api/ocr/process` dùng session NextAuth nên mobile không gọi được).
- [ ] **P9.2** Android: chọn ảnh (dùng `ActivityResultContracts.GetContent`),
  xem trước, kiểm tra định dạng và dung lượng (JPG/PNG/WebP, tối đa 10MB).
- [ ] **P9.3** Gửi ảnh base64 lên server, nhận về danh sách sản phẩm.
- [ ] **P9.4** Đổ dữ liệu OCR vào form nhập hàng, người dùng sửa trước khi lưu.
- [ ] **P9.5** Gắn handler menu `menu_ocr`.
- [ ] **P9.6** Viết `docs/ocr_hoa_don.md`.

---

## 9. P10 — Lịch sử nhập hàng

- [ ] **P10.1** Backend `GET /api/v1/imports/:id` và `POST /api/v1/imports/:id/cancel`
  (chỉ huỷ được phiếu `PENDING`, hoàn trả tồn kho trong transaction).
- [ ] **P10.2** Android `ManHinhLichSuNhap`: danh sách, lọc trạng thái, phân trang.
- [ ] **P10.3** Android `ManHinhChiTietPhieuNhap`: chi tiết phiếu, cảnh báo thay
  đổi giá nhập, nút huỷ phiếu.
- [ ] **P10.4** Gắn handler menu `menu_lich_su_nhap`.
- [ ] **P10.5** Viết `docs/lich_su_nhap.md`.

---

## 10. P11 — Lịch sử bán hàng

- [ ] **P11.1** Android `ManHinhLichSuBan`: dùng `GET /api/v1/sales/history`.
- [ ] **P11.2** Màn hình chi tiết hoá đơn bán.
- [ ] **P11.3** Gắn handler menu `menu_lich_su_ban`.
- [ ] **P11.4** Viết `docs/lich_su_ban.md`.

---

## 11. P12 — Xác thực nâng cao

- [x] **P12.1** Backend: thêm refresh token (`POST /api/v1/auth/refresh`).
  - Thêm model `MobileRefreshToken` vào `prisma/schema.prisma` + migration
    `20261102180000_add_mobile_refresh_tokens`.
  - Thêm `createMobileRefreshToken` / `verifyMobileRefreshToken` vào
    `src/lib/mobile-jwt.ts`, refresh token sống 30 ngày.
  - `POST /api/v1/auth/login` trả kèm `refreshToken`.
  - `POST /api/v1/auth/refresh` xoay vòng token: thu hồi token cũ, cấp token mới.
  - `POST /api/v1/auth/logout` thu hồi refresh token.
  - **Hoàn thành ngày 02/11/2026** ✅
- [x] **P12.2** Android: lưu access + refresh token, tự refresh khi 401.
  - `BoNhoPhienDangNhap` lưu thêm refresh token.
  - `du_lieu/LamMoiTokenTuDong.kt` là `okhttp3.Authenticator`: khi server trả 401
    thì gọi API refresh rồi thử lại request, người dùng không bị đăng xuất oan.
  - `DichVuTaiKhoan.kiemTraToken()` thử refresh trước khi kết luận token hết hạn.
  - `DichVuDangNhap.dangXuat()` gọi API logout để thu hồi refresh token.
  - **Hoàn thành ngày 02/11/2026** ✅
- [ ] **P12.3** Mã hoá token bằng Android Keystore thay vì SharedPreferences.
- [ ] **P12.4** Dùng `GET /api/v1/bootstrap` khi vào app để đồng bộ dữ liệu nền.
- [ ] **P12.5** Viết `docs/xac_thuc.md`.

---

## 12. P13 — Màn hình cài đặt

- [ ] **P13.1** Gắn handler menu `menu_cai_dat`.
- [ ] **P13.2** Nội dung bám sát web `/settings`: tab Chung / AI OCR / Cơ sở dữ liệu
  / Thông báo, bật tắt dark mode.
- [ ] **P13.3** Viết `docs/cai_dat.md`.

---

## 13. P14 — Offline-first (khối lượng lớn nhất, để cuối)

- [x] **P14.1** Thêm dependency Room + WorkManager.
- [x] **P14.2** Entity SQLite: `SanPham`, `DanhMuc`, `DonViTinh`, `KhachHang`,
  `NhaCungCap`, `HoaDon`, `DongHoaDon`, `HangDongBo`.
- [x] **P14.3** Repository đọc ưu tiên local, đồng bộ gia tăng bằng
  `updatedAfter`.
- [x] **P14.4** Bảng `sync_queue` lưu thao tác chờ gửi, có `operationId`.
- [x] **P14.5** `WorkerDongBoHoaDon` chạy định kỳ, retry khi mất mạng.
- [x] **P14.6** Xử lý xung đột tồn kho khi đồng bộ hoá đơn offline.
- [x] **P14.7** Viết `docs/offline_dong_bo.md`.

---

## 14. Nhật ký cập nhật

| Ngày | Hạng mục | Nội dung | Người thực hiện |
|---|---|---|---|
| 2026-11-02 | — | Tạo plan tổng + quy tắc viết code | AI |
| 2026-11-02 | P0 | Hoàn thành P0.1–P0.8: lưu barcode, chuẩn hoá tiền tệ, kiểm tra tồn kho trước khi bán, xử lý lỗi theo mã lỗi, kiểm tra token khi khởi động, sửa đăng xuất + back stack, gom chuỗi vào `strings.xml` | AI |
| 2026-11-02 | P1 | Hoàn thành P1.1–P1.12: hộp thoại chọn đơn vị bán, chọn khách hàng, phương thức thanh toán, tiền khách đưa/tiền thừa, ghi chú, tạo hoá đơn thật qua `POST /api/v1/sales` có `operationId`, màn hình thanh công, xoá giỏ + làm mới tồn kho; backend bổ sung `conversionQty` để quy đổi đơn vị bán về đơn vị cơ bản | AI |
| 2026-11-02 | P1.13 | Rà soát lỗi nghiệp vụ: sửa 11 lỗi (khoá vĩnh viễn `operationId` sau lỗi tồn kho, xoá nhầm sản phẩm trang 2+ khỏi giỏ, POS chỉ tải 15 sản phẩm, hiển thị sai giá theo đơn vị, ô mã giảm giá chết, trừ kho âm trên web) | AI |
| 2026-11-02 | P14 | Hoàn thành P14.1–P14.7: thêm Room + WorkManager + KSP, 9 entity SQLite, repository đọc ưu tiên local, tạo hoá đơn offline vào hàng đợi đồng bộ, `WorkerDongBoHoaDon` retry bằng cùng `operationId`, `WorkerDongBoSanPham` đồng bộ sản phẩm định kỳ 15 phút, xử lý `STOCK_CONFLICT` 409 | AI |
| 2026-11-02 | Sửa lỗi | Thiếu bảng `sync_operations` gây 500 khi tạo hoá đơn: chạy `prisma migrate resolve --applied` + áp dụng SQL migration; tiền khách đưa không còn bắt buộc (bỏ trống = khách đưa đủ tiền, tiền thừa = 0) | AI |
| 2026-11-02 | Sửa lỗi | Sửa lỗi nhập số lượng trong giỏ hàng bị "văng ra": `TrinhBayGioHang` không dùng `notifyDataSetChanged()` (làm mất focus ô nhập), không ghi đè nội dung đang gõ, ô trống/không hợp lệ giữ số lượng cũ thay vì xoá dòng; `tinhSoLuongToiDa` không trừ dòng đang sửa; vượt tồn kho có thông báo | AI |
| 2026-11-02 | P4 – P7 | Hoàn thành P4.1–P7.4: bổ sung API mobile CRUD cho danh mục, đơn vị tính, khách hàng, nhà cung cấp (tự sinh slug, kiểm tra trùng, chặn xoá khi còn dữ liệu liên quan, ẩn thay vì xoá khi đã phát sinh hoá đơn/phiếu nhập); Android dùng chung `ManHinhQuanLyDuLieu` + `TrinhBayBanGhi` + `bottom_sheet_ban_ghi.xml` cho cả 4 màn hình; gắn handler menu | AI |

---

## 15. Bảng theo dõi nhanh

| Mục | Trạng thái | Ghi chú |
|---|---|---|
| P0 Sửa lỗi | ✅ Hoàn thành | 02/11/2026 — build & typecheck đều sạch |
| P1 Bán hàng POS | ✅ Hoàn thành | 02/11/2026 — build & typecheck đều sạch |
| P2 Tồn kho | ⬜ Chưa bắt đầu | API đã có |
| P3 Doanh thu | ⬜ Chưa bắt đầu | Cần thêm API mobile |
| P4 Danh mục | ✅ Hoàn thành | 02/11/2026 — build & typecheck đều sạch |
| P5 Đơn vị tính | ✅ Hoàn thành | 02/11/2026 — build & typecheck đều sạch |
| P6 Khách hàng | ✅ Hoàn thành | 02/11/2026 — build & typecheck đều sạch |
| P7 Nhà cung cấp | ✅ Hoàn thành | 02/11/2026 — build & typecheck đều sạch |
| P8 Nhập hàng | ⬜ Chưa bắt đầu | |
| P9 OCR | ⬜ Chưa bắt đầu | |
| P10 Lịch sử nhập | ⬜ Chưa bắt đầu | |
| P11 Lịch sử bán | ⬜ Chưa bắt đầu | |
| P12 Xác thực | ⬜ Chưa bắt đầu | |
| P13 Cài đặt | ⬜ Chưa bắt đầu | |
| P14 Offline | ✅ Hoàn thành | 02/11/2026 — Room + WorkManager, build sạch |
