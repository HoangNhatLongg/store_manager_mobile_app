package com.example.store_manager.du_lieu

/**
 * Model san pham mobile nhan tu API products.
 *
 * Gia tien duoc chuyen sang kieu Double de tinh toan chinh xac. Backend van
 * tra gia tien duoi dang chuoi de tranh loi lam tron phia server, Gson tu
 * chuyen chuoi thanh so khi doc JSON.
 */
data class SanPham(
    val id: String = "",
    val name: String = "",
    val sku: String = "",
    val barcode: String? = null,
    val importPrice: Double = 0.0,
    val sellPrice: Double = 0.0,
    val stock: Int = 0,
    val minStock: Int = 0,
    val description: String? = null,
    val imageUrl: String? = null,
    val categoryId: String? = null,
    val isActive: Boolean = true,
    val category: DanhMuc? = null,
    val units: List<DonViSanPham> = emptyList()
)

/** Don vi ban cua san pham, vi du chai, thung hoac kg. */
data class DonViSanPham(
    val id: String = "",
    val unitId: String = "",
    val conversionQty: Int = 1,
    val price: Double = 0.0,
    val isDefault: Boolean = false,
    val unit: DonViTinh? = null
)

data class YeuCauDonViSanPham(
    val productId: String,
    val unitId: String,
    val conversionQty: Int,
    val price: Double,
    val isDefault: Boolean
)

/** Response danh sach san pham co phan trang. */
data class PhanHoiSanPham(
    val data: List<SanPham> = emptyList(),
    val pagination: PhanTrang = PhanTrang()
)

/**
 * Du lieu gửi lên server khi tạo hoặc sửa sản phẩm.
 *
 * Gia tien dùng kieu Double de tranh trang thai "null" do chuyen String sang
 * Double. Khi gui len server, cac gia tri nay duoc chuyen lai thanh chuoi
 * boi DinhDangTien.
 */
data class DuLieuSanPham(
    val name: String = "",
    val sku: String = "",
    val barcode: String? = null,
    val categoryId: String? = null,
    val importPrice: Double = 0.0,
    val sellPrice: Double = 0.0,
    val stock: Int = 0,
    val minStock: Int = 10,
    val description: String = "",
    val imageUrl: String? = null
)

data class PhanHoiSanPhamDon(
    val data: SanPham? = null
)

data class PhanHoiDonViSanPham(
    val data: List<DonViSanPham> = emptyList()
)

data class PhanTrang(
    val page: Int = 1,
    val limit: Int = 50,
    val total: Int = 0,
    val totalPages: Int = 0
)

/**
 * Phan hoi thong ke ton kho tu API `/api/v1/inventory/stats`.
 *
 * @param totalProducts Tong so san pham.
 * @param outOfStock   So san pham het hang.
 * @param lowStock     So san pham sap het hang.
 * @param inStock      So san pham van con hang.
 */
data class SoLieuThongKeTonKho(
    val totalProducts: Int = 0,
    val outOfStock: Int = 0,
    val lowStock: Int = 0,
    val inStock: Int = 0
)

/** Phan hoi chua so lieu thong ke ton kho. */
data class PhanHoiThongKeTonKho(
    val data: SoLieuThongKeTonKho = SoLieuThongKeTonKho()
)

/**
 * Phan hoi bao cao doanh thu tu API `/api/v1/analytics/revenue`.
 *
 * @param data Tong hop va doanh thu theo ky.
 */
data class PhanHoiBaoCaoDoanhThu(
    val data: PhanHoiDoanhThu = PhanHoiDoanhThu()
)

/**
 * Phan hoi chi tiet doanh thu ngay tu API `/api/v1/analytics/revenue-daily`.
 *
 * @param data Chi tiet doanh thu cua ngay.
 */
data class PhanHoiChiTietDoanhThuNgay(
    val data: PhanHoiDoanhThuNgay = PhanHoiDoanhThuNgay()
)

sealed interface KetQuaSanPham {    data class ThanhCong(
        val danhSach: List<SanPham>,
        val phanTrang: PhanTrang = PhanTrang()
    ) : KetQuaSanPham

    data class ThatBai(
        val maLoi: String,
        val thongBao: String
    ) : KetQuaSanPham
}

/**
 * Mot dong trong gio hang tam thoi.
 *
 * @param sanPham      San pham duoc chon.
 * @param soLuong      So luong theo don vi ban dang chon.
 * @param donViBan     Don vi ban duoc chon. Null nghia la ban theo don vi co ban.
 */
data class MatHangGioHang(
    val sanPham: SanPham,
    val soLuong: Int = 1,
    val donViBan: DonViSanPham? = null
) {
    /**
     * He so quy doi giua don vi ban va don vi co ban.
     *
     * Vi du 1 thung = 24 chai thi he so quy doi la 24.
     */
    val heSoQuyDoi: Int
        get() = donViBan?.conversionQty?.takeIf { it > 0 } ?: 1

    /**
     * So luong quy doi ve don vi co ban, dung de so sanh voi ton kho server.
     */
    val soLuongDonViCoBan: Int
        get() = soLuong * heSoQuyDoi

    /** Don gia cua mot don vi ban. */
    val donGia: Double
        get() = donViBan?.price ?: sanPham.sellPrice

    /** Thanh tien cua dong hang. */
    val thanhTien: Double
        get() = donGia * soLuong

    /** Ten don vi ban de hien thi tren gio hang. */
    val tenDonViBan: String
        get() = donViBan?.unit?.layTenHienThi() ?: sanPham.units
            .firstOrNull { it.isDefault }
            ?.unit
            ?.layTenHienThi()
            .orEmpty()

    /**
     * Khoa duy nhat cua mot dong hang trong gio hang.
     *
     * Cung mot san pham nhung khac don vi ban thi phai la hai dong rieng,
     * vi gia va so luong ton kho quy doi khac nhau.
     */
    val khoaDong: String
        get() = "${sanPham.id}|${donViBan?.unitId.orEmpty()}"
}
