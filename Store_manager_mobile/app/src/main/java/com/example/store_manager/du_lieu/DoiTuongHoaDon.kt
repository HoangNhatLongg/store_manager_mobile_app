package com.example.store_manager.du_lieu

import com.google.gson.annotations.SerializedName

/**
 * Phuong thuc thanh toan duoc server chap nhan.
 *
 * Ten khai bao dung tieng Viet khong dau, gia tri gui len server giu nguyen
 * theo contract cua backend.
 */
enum class PhuongThucThanhToan(val maGuiLenServer: String) {
    TIEN_MAT("CASH"),
    CHUYEN_KHOAN("BANK_TRANSFER"),
    THE("CARD"),
    VI_DIEN_TU("E_WALLET");

    companion object {
        /**
         * Chuyen ma cua server thanh enum tuong ung.
         *
         * @param ma Ma phuong thuc tra ve tu server.
         * @return Enum tuong ung, mac dinh la TIEN_MAT neu khong khop.
         */
        fun tuMa(ma: String): PhuongThucThanhToan {
            return entries.firstOrNull { it.maGuiLenServer == ma } ?: TIEN_MAT
        }
    }
}

/**
 * Mot dong hang trong yeu cau tao hoa don.
 *
 * @param productId      Id san pham.
 * @param quantity       So luong theo don vi ban.
 * @param unitPrice      Gia cua mot don vi ban.
 * @param conversionQty  So luong don vi co ban trong mot don vi ban.
 */
data class DongHangHoaDon(
    val productId: String,
    val quantity: Int,
    val unitPrice: Double,
    val conversionQty: Int = 1
)

/**
 * Yeu cau tao hoa don ban gui len POST /api/v1/sales.
 *
 * @param operationId   Idempotency key. Phai giu nguyen khi retry.
 * @param customerId    Khach hang, null neu khong chon.
 * @param items         Cac dong hang trong hoa don.
 * @param paymentMethod Phuong thuc thanh toan.
 * @param discount      Tong tien giam.
 * @param notes         Ghi chu hoa don.
 */
data class YeuCauTaoHoaDon(
    val operationId: String,
    val customerId: String? = null,
    val items: List<DongHangHoaDon>,
    val paymentMethod: String,
    val discount: Double = 0.0,
    val notes: String? = null
)

/**
 * Mot dong hang trong hoa don da duoc server tao.
 */
data class DongHoaDonDaTao(
    val id: String = "",
    val productId: String = "",
    val quantity: Int = 0,
    val unitPrice: Double = 0.0,
    val importPrice: Double = 0.0,
    val totalPrice: Double = 0.0
)

/**
 * Hoa don ban da duoc server tao thanh cong.
 *
 * @param invoiceNumber So hoa don sinh tu server.
 * @param totalAmount   Tong cong sau giam gia.
 * @param items         Cac dong hang cua hoa don.
 */
data class HoaDonDaTao(
    val id: String = "",
    val invoiceNumber: String = "",
    val subtotal: Double = 0.0,
    val discount: Double = 0.0,
    val totalAmount: Double = 0.0,
    val paymentMethod: String = "CASH",
    val notes: String? = null,
    val items: List<DongHoaDonDaTao> = emptyList()
)

/**
 * Response thanh cong cua POST /api/v1/sales.
 *
 * @param status Trang thai, "SYNCED" la thanh cong moi, "ALREADY_PROCESSED" la
 *               request da duoc xu ly truoc do do retry.
 */
data class PhanHoiTaoHoaDon(
    val status: String = "",
    val invoice: HoaDonDaTao? = null
)

/**
 * Ket qua tao hoa don de Activity chi xu ly trang thai giao dien.
 */
sealed interface KetQuaTaoHoaDon {
    /**
     * Tao hoa don thanh cong.
     *
     * @param hoaDon       Hoa don server tra ve.
     * @param daTonTai     True neu hoa don da duoc tao truoc do (retry).
     */
    data class ThanhCong(
        val hoaDon: HoaDonDaTao,
        val daTonTai: Boolean = false
    ) : KetQuaTaoHoaDon

    /**
     * Tao hoa don that bai.
     *
     * @param maLoi    Ma loi chuan tu server.
     * @param thongBao Thong bao cho nguoi dung.
     */
    data class ThatBai(
        val maLoi: String,
        val thongBao: String
    ) : KetQuaTaoHoaDon
}
