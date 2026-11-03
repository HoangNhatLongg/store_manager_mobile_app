# Danh mục tài liệu tính năng

Mỗi tính năng của ứng dụng Android có một file tài liệu riêng trong thư mục
này. File mô tả mục đích, luồng hoạt động, các file liên quan, bảng database
liên quan và đối chiếu với bản web.

Tài liệu tuân theo mẫu trong [`MHUNG_TAI_LIEU_TINH_NANG.md`](./MHUNG_TAI_LIEU_TINH_NANG.md).

## Danh sách

| Hạng mục | Tính năng | Tài liệu | Trạng thái |
|---|---|---|---|
| P0 | Sửa lỗi hiện có | [`sua_loi.md`](./sua_loi.md) | ✅ Hoàn thành 02/11/2026 |
| P1 | Bán hàng POS | [`ban_hang_pos.md`](./ban_hang_pos.md) | ✅ Hoàn thành 02/11/2026 |
| P2 | Quản lý tồn kho | [`ton_kho.md`](./ton_kho.md) | Chưa bắt đầu |
| P3 | Báo cáo doanh thu | [`doanh_thu.md`](./doanh_thu.md) | Chưa bắt đầu |
| P4 | Quản lý danh mục | [`danh_muc.md`](./danh_muc.md) | ✅ Hoàn thành 02/11/2026 |
| P5 | Quản lý đơn vị tính | [`don_vi_tinh.md`](./don_vi_tinh.md) | ✅ Hoàn thành 02/11/2026 |
| P6 | Quản lý khách hàng | [`khach_hang.md`](./khach_hang.md) | ✅ Hoàn thành 02/11/2026 |
| P7 | Quản lý nhà cung cấp | [`nha_cung_cap.md`](./nha_cung_cap.md) | ✅ Hoàn thành 02/11/2026 |
| P8 | Nhập hàng | [`nhap_hang.md`](./nhap_hang.md) | ✅ Hoàn thành 03/11/2026 |
| - | Đơn vị sản phẩm (mặc định & gốc) | [`don_vi_san_pham.md`](./don_vi_san_pham.md) | ✅ Hoàn thành 03/11/2026 |
| P9 | OCR hoá đơn | [`ocr_hoa_don.md`](./ocr_hoa_don.md) | Chưa bắt đầu |
| P10 | Lịch sử nhập hàng | [`lich_su_nhap.md`](./lich_su_nhap.md) | Chưa bắt đầu |
| P11 | Lịch sử bán hàng | [`lich_su_ban.md`](./lich_su_ban.md) | Chưa bắt đầu |
| P12 | Xác thực nâng cao | [`xac_thuc.md`](./xac_thuc.md) | Chưa bắt đầu |
| P13 | Màn hình cài đặt | [`cai_dat.md`](./cai_dat.md) | Chưa bắt đầu |
| P14 | Offline và đồng bộ | [`offline_dong_bo.md`](./offline_dong_bo.md) | Chưa bắt đầu |

## Tài liệu liên quan

| Tài liệu | Nội dung |
|---|---|
| [`../PLAN_TONG.md`](../PLAN_TONG.md) | Todo list chính của dự án |
| [`../QUI_TAC_VIET_CODE.md`](../QUI_TAC_VIET_CODE.md) | Quy tắc đặt tên, comment, kiến trúc, giao diện |
| [`../HUONG_DAN_HE_THONG.md`](../HUONG_DAN_HE_THONG.md) | Mô tả tổng quan cách app hoạt động |

## Quy tắc cập nhật

1. Làm xong tính năng nào thì cập nhật trạng thái trong bảng trên.
2. Cập nhật ô trạng thái tương ứng trong [`../PLAN_TONG.md`](../PLAN_TONG.md).
3. Ghi thêm một dòng vào bảng "Nhật ký cập nhật" của plan.
4. Nếu luồng hoạt động thay đổi trong quá trình làm, cập nhật lại file tài liệu
   của tính năng đó cho khớp với code thực tế.
