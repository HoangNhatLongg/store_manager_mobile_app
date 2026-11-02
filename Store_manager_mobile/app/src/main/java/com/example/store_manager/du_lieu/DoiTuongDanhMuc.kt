package com.example.store_manager.du_lieu

/**
 * Model danh muc dung chung cho man hinh quan ly danh muc va bo loc san pham.
 *
 * @param soSanPham So san pham dang thuoc danh muc nay.
 */
data class DanhMuc(
    val id: String = "",
    val name: String = "",
    val slug: String? = null,
    val description: String? = null,
    val isActive: Boolean = true,
    val soSanPham: Int = 0
)

/**
 * Model don vi tinh dung cho man hinh quan ly don vi va chon don vi ban.
 *
 * @param laDonViCoBan Don vi co ban la don vi tinh ton kho theo.
 * @param soSanPham   So san pham dang dung don vi nay.
 */
data class DonViTinh(
    val id: String = "",
    val name: String = "",
    val abbreviation: String = "",
    val laDonViCoBan: Boolean = true,
    val soSanPham: Int = 0
) {
    /** Chon ten co nghia nhat de hien thi tren POS. */
    fun layTenHienThi(): String {
        return name.trim().takeIf { it.isNotEmpty() }
            ?: abbreviation.trim()
    }
}

/**
 * Model khach hang cho man hinh quan ly khach hang va chon khach khi ban hang.
 *
 * @param soHoaDon So hoa don ban da thanh toan cua khach hang nay.
 * @param diem     Diem thuong con lai cua khach hang.
 */
data class KhachHang(
    val id: String = "",
    val name: String = "",
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val diem: Int = 0,
    val isActive: Boolean = true,
    val soHoaDon: Int = 0
)

/**
 * Model nha cung cap cho man hinh quan ly nha cung cap va chon khi nhap hang.
 *
 * @param soPhieuNhap So phieu nhap da phat sinh voi nha cung cap nay.
 */
data class NhaCungCap(
    val id: String = "",
    val name: String = "",
    val phone: String? = null,
    val email: String? = null,
    val address: String? = null,
    val isActive: Boolean = true,
    val soPhieuNhap: Int = 0
)

/** Du lieu gui len server khi tao hoac sua danh muc. */
data class DuLieuDanhMuc(
    val name: String = "",
    val description: String = ""
)

/** Du lieu gui len server khi tao hoac sua don vi tinh. */
data class DuLieuDonViTinh(
    val name: String = "",
    val abbreviation: String = "",
    val isBaseUnit: Boolean = false
)

/** Du lieu gui len server khi tao hoac sua khach hang. */
data class DuLieuKhachHang(
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = ""
)

/** Du lieu gui len server khi tao hoac sua nha cung cap. */
data class DuLieuNhaCungCap(
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = ""
)

/** Phan hoi chua danh sach danh muc. */
data class PhanHoiDanhMuc(
    val data: List<DanhMuc> = emptyList()
)

/** Phan hoi chua danh sach don vi tinh. */
data class PhanHoiDonVi(
    val data: List<DonViTinh> = emptyList()
)

/** Phan hoi chua danh sach khach hang. */
data class PhanHoiKhachHang(
    val data: List<KhachHang> = emptyList()
)

/** Phan hoi chua danh sach nha cung cap. */
data class PhanHoiNhaCungCap(
    val data: List<NhaCungCap> = emptyList()
)

/** Phan hoi chua mot danh muc. */
data class PhanHoiDanhMucDon(
    val data: DanhMuc? = null
)

/** Phan hoi chua mot don vi tinh. */
data class PhanHoiDonViTinhDon(
    val data: DonViTinh? = null
)

/** Phan hoi chua mot khach hang. */
data class PhanHoiKhachHangDon(
    val data: KhachHang? = null
)

/** Phan hoi chua mot nha cung cap. */
data class PhanHoiNhaCungCapDon(
    val data: NhaCungCap? = null
)
