package com.example.store_manager.du_lieu

/**
 * Trang thai ton kho cua mot san pham.
 *
 * Dung de gan nhan trang thai va tinh cac the thong ke tren man hinh ton kho.
 * Quy tac phan loai giong het voi man hinh web `/inventory`.
 */
enum class TrangThaiTonKho {
    /** San pham het hang, ton kho bang 0. */
    HET_HANG,

    /** San pham sap het, ton kho khong vuot ngay mo ngay lon nhat. */
    SAP_HET,

    /** San pham van con hang. */
    CON_HANG;

    companion object {
        /**
         * Xep loai trang thai ton kho cua mot san pham.
         *
         * @param sanPham San pham can xep loai.
         * @return Trang thai ton kho tuong ung.
         */
        fun cuaSanPham(sanPham: SanPham): TrangThaiTonKho {
            return when {
                sanPham.stock <= 0 -> HET_HANG
                sanPham.stock <= sanPham.minStock -> SAP_HET
                else -> CON_HANG
            }
        }
    }
}

/**
 * So lieu thong ke ton kho cua mot danh sach san pham.
 *
 * @param tongSanPham  Tong so san pham.
 * @param sapHet       So san pham sap het hang.
 * @param hetHang      So san pham het hang.
 * @param conHang      So san pham van con hang.
 */
data class ThongKeTonKho(
    val tongSanPham: Int = 0,
    val sapHet: Int = 0,
    val hetHang: Int = 0,
    val conHang: Int = 0
) {
    companion object {
        /**
         * Tinh thong ke ton kho tu danh sach san pham.
         *
         * @param danhSach Danh sach san pham can thong ke.
         * @return So lieu thong ke.
         */
        fun tuDanhSach(danhSach: List<SanPham>): ThongKeTonKho {
            var sapHet = 0
            var hetHang = 0

            danhSach.forEach { sanPham ->
                when (TrangThaiTonKho.cuaSanPham(sanPham)) {
                    TrangThaiTonKho.HET_HANG -> hetHang++
                    TrangThaiTonKho.SAP_HET -> sapHet++
                    TrangThaiTonKho.CON_HANG -> Unit
                }
            }

            return ThongKeTonKho(
                tongSanPham = danhSach.size,
                sapHet = sapHet,
                hetHang = hetHang,
                conHang = danhSach.size - sapHet - hetHang
            )
        }
    }
}
