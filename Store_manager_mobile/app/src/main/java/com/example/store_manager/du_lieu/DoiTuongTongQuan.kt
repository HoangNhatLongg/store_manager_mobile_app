package com.example.store_manager.du_lieu

/**
 * Nhom so lieu tren cac the thong ke cua dashboard web.
 */
data class SoLieuTongQuan(
    val totalProducts: Int = 0,
    val lowStockProducts: Int = 0,
    val totalImportInvoices: Int = 0,
    val totalSaleInvoices: Int = 0,
    val totalImportAmount: Double = 0.0,
    val totalSaleAmount: Double = 0.0,
    val totalProfit: Double = 0.0
)

/** Mot hoat dong gan day: nhap hang hoac ban hang. */
data class HoatDongGanDay(
    val id: String = "",
    val type: String = "",
    val message: String = "",
    val createdAt: String = ""
)

/** San pham dang co ton kho bang hoac duoi muc toi thieu. */
data class SanPhamSapHet(
    val id: String = "",
    val name: String = "",
    val sku: String = "",
    val stock: Int = 0,
    val minStock: Int = 0,
    val sellPrice: Double = 0.0
)

/** San pham co so luong ban cao nhat. */
data class SanPhamBanChay(
    val id: String = "",
    val name: String = "",
    val totalSold: Int = 0,
    val totalRevenue: Double = 0.0
)

/** Response tong hop cua GET /api/v1/dashboard. */
data class PhanHoiTongQuan(
    val serverTime: String = "",
    val stats: SoLieuTongQuan = SoLieuTongQuan(),
    val recentActivities: List<HoatDongGanDay> = emptyList(),
    val lowStockProducts: List<SanPhamSapHet> = emptyList(),
    val topProducts: List<SanPhamBanChay> = emptyList()
)

/** Ket qua tai dashboard de Activity chi xu ly trang thai giao dien. */
sealed interface KetQuaTongQuan {
    data class ThanhCong(val duLieu: PhanHoiTongQuan) : KetQuaTongQuan

    data class ThatBai(
        val maLoi: String,
        val thongBao: String
    ) : KetQuaTongQuan
}
