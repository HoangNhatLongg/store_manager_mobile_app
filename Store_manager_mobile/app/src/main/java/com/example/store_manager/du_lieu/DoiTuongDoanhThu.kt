package com.example.store_manager.du_lieu

/**
 * Tong hop so lieu doanh thu cua mot khoang thoi gian.
 *
 * @param totalRevenue      Tong doanh thu.
 * @param totalImportCost   Tong gia von cua hang ban.
 * @param totalProfit       Tong loi nhuan.
 * @param grossMargin       Bien loi nhuan, tinh theo phan tram.
 * @param totalSales        So hoa don ban.
 * @param totalInvoices     So hoa don ban, dung o cap chi tiet ngay.
 * @param totalProductsSold Tong so luong san pham da ban.
 * @param totalCustomers    So khach hang da mua.
 */
data class TongHopDoanhThu(
    val totalRevenue: Double = 0.0,
    val totalImportCost: Double = 0.0,
    val totalProfit: Double = 0.0,
    val grossMargin: Double = 0.0,
    val totalSales: Int = 0,
    val totalInvoices: Int = 0,
    val totalProductsSold: Int = 0,
    val totalCustomers: Int = 0
)

/**
 * Doanh thu gom theo mot ky (nam, thang hoac gio).
 *
 * @param key       Khoa ky, dung de sap xep.
 * @param label     Nhan hien thi tren bieu do.
 * @param revenue   Doanh thu trong ky.
 * @param importCost Gia von trong ky.
 * @param profit    Loi nhuan trong ky.
 * @param count     So dong hang trong ky.
 */
data class DoanhThuTheoKy(
    val key: String = "",
    val label: String = "",
    val revenue: Double = 0.0,
    val importCost: Double = 0.0,
    val profit: Double = 0.0,
    val count: Int = 0
)

/** Phan hoi bao cao doanh thu tu API `/api/v1/analytics/revenue`. */
data class PhanHoiDoanhThu(
    val summary: TongHopDoanhThu = TongHopDoanhThu(),
    val byPeriod: List<DoanhThuTheoKy> = emptyList()
)

/**
 * Chi tiet san pham da ban trong mot ngay.
 *
 * @param productId    Id san pham.
 * @param productName  Ten san pham.
 * @param productSku   Ma SKU.
 * @param categoryName Ten danh muc, null neu khong co.
 * @param quantity     So luong da ban.
 * @param unitPrice    Don gia ban.
 * @param totalPrice   Tong tien ban.
 * @param importPrice  Gia nhap tai thoi diem ban.
 * @param profit       Loi nhuan cua san pham nay.
 */
data class SanPhamDaBan(
    val productId: String = "",
    val productName: String = "",
    val productSku: String = "",
    val categoryName: String? = null,
    val quantity: Int = 0,
    val unitPrice: Double = 0.0,
    val totalPrice: Double = 0.0,
    val importPrice: Double = 0.0,
    val profit: Double = 0.0
)

/**
 * Phan hoi chi tiet doanh thu cua mot ngay.
 *
 * @param date         Ngay dang xem, dinh dang `yyyy-MM-dd`.
 * @param summary      Tong hop so lieu cua ngay.
 * @param productsSold Danh sach san pham da ban.
 */
data class PhanHoiDoanhThuNgay(
    val date: String = "",
    val summary: TongHopDoanhThu = TongHopDoanhThu(),
    val productsSold: List<SanPhamDaBan> = emptyList()
)

/**
 * Ket qua tra ve cua man hinh doanh thu.
 */
sealed interface KetQuaDoanhThu {
    /**
     * Tai bao cao doanh thu thanh cong.
     *
     * @param duLieu Phan hoi bao cao doanh thu theo ky.
     */
    data class ThanhCong(val duLieu: PhanHoiDoanhThu) : KetQuaDoanhThu

    /**
     * Tai chi tiet ngay thanh cong.
     *
     * @param duLieu Phan hoi chi tiet doanh thu cua mot ngay.
     */
    data class ThanhCongNgay(val duLieu: PhanHoiDoanhThuNgay) : KetQuaDoanhThu

    /**
     * Tai that bai.
     *
     * @param maLoi    Ma loi chuan tu server.
     * @param thongBao Thong bao cho nguoi dung.
     */
    data class ThatBai(
        val maLoi: String,
        val thongBao: String
    ) : KetQuaDoanhThu
}
