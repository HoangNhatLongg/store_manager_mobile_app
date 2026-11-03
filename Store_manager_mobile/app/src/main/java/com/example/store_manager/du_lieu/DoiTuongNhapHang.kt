package com.example.store_manager.du_lieu

/**
 * Mot dong hang trong phieu nhap dang soan.
 *
 * @param maDong       Khoa duy nhat cua dong hang, dung de dinh danh khi
 *                     nguoi dung sua so luong hoac don gia.
 * @param maSanPham    Id san pham da khop. Null nghia la dong hang nay se
 *                     tao san pham moi khi luu phieu.
 * @param tenSanPham   Ten san pham nguoi dung nhap.
 * @param maDanhMuc    Id danh muc, chi dung khi tao san pham moi.
 * @param soLuong      So luong nhap, tinh theo don vi goc.
 * @param donGia       Don gia nhap cho mot don vi goc.
 * @param maDonViGoc   Id don vi goc dung de nhap hang.
 * @param heSoQuyDoi   So luong don vi ban le co ban trong mot don vi goc.
 * @param tenDonViGoc  Ten don vi goc, dung de hien thi tren man hinh.
 */
data class DongHangNhap(
    val maDong: String = "",
    val maSanPham: String? = null,
    val tenSanPham: String = "",
    val maDanhMuc: String? = null,
    val soLuong: Int = 1,
    val donGia: Double = 0.0,
    val maDonViGoc: String? = null,
    val heSoQuyDoi: Int = 1,
    val tenDonViGoc: String = ""
) {
    /** Thanh tien cua dong hang = so luong x don gia. */
    val thanhTien: Double
        get() = soLuong * donGia

    /** Ten dong hang co duoc dung de hien thi hay khong. */
    val coTen: Boolean
        get() = tenSanPham.isNotBlank()

    /**
     * So luong quy doi ve don vi ban le co ban.
     *
     * Ton kho luu theo don vi ban le co ban nen so luong nhap theo don vi goc
     * phai nhan voi he so quy doi.
     */
    val soLuongDonViCoBan: Int
        get() = soLuong * heSoQuyDoi

    /**
     * Gia nhap quy doi ve don vi ban le co ban.
     *
     * Gia nhap luu tren san pham luon tinh theo don vi ban le co ban de bien
     * loi nhuan o POS luon dung.
     */
    val donGiaDonViCoBan: Double
        get() = if (heSoQuyDoi > 0) donGia / heSoQuyDoi else donGia
}

/**
 * Gán đơn vị gốc cho dòng hàng nhập.
 *
 * @param donVi Đơn vị gốc được chọn.
 * @return Bản sao dòng hàng đã cập nhật đơn vị gốc.
 */
fun DongHangNhap.ganDonViGoc(donVi: DonViSanPham): DongHangNhap {
    return copy(
        maDonViGoc = donVi.unitId,
        heSoQuyDoi = donVi.conversionQty,
        tenDonViGoc = donVi.unit?.name.orEmpty()
    )
}

/**
 * Dong hang gui len server, ten truong giong hop dong cua backend.
 *
 * @param productId   Id san pham, null neu can tao san pham moi.
 * @param productName Ten san pham.
 * @param categoryId  Id danh muc, null neu khong chon.
 * @param quantity    So luong nhap theo don vi goc.
 * @param unitPrice   Don gia nhap cua mot don vi goc.
 * @param unitId      Id don vi goc dung de nhap hang.
 * @param baseUnitId  Id don vi ban le co ban cua san pham.
 * @param conversionQty He so quy doi cua don vi goc.
 */
data class DongHangNhapGui(
    val productId: String? = null,
    val productName: String = "",
    val categoryId: String? = null,
    val quantity: Int = 0,
    val unitPrice: Double = 0.0,
    val unitId: String? = null,
    val baseUnitId: String? = null,
    val conversionQty: Int = 1
)

/**
 * Yeu cau tao phieu nhap gui len POST /api/v1/imports.
 *
 * @param supplierId Id nha cung cap, null neu khong chon.
 * @param notes      Ghi chu phieu nhap.
 * @param items      Cac dong hang trong phieu.
 */
data class YeuCauTaoPhieuNhap(
    val supplierId: String? = null,
    val notes: String? = null,
    val items: List<DongHangNhapGui> = emptyList()
)

/**
 * San pham rut gon di kem trong phieu nhap da tao.
 */
data class SanPhamPhieuNhap(
    val id: String = "",
    val name: String = "",
    val sku: String = ""
)

/**
 * Dong hang cua phieu nhap da duoc server tao.
 *
 * @param previousUnitPrice Gia nhap cu truoc khi cap nhat, dung de canh bao
 *                           thay doi gia nhap.
 * @param unitName        Ten don vi goc dung khi nhap.
 * @param conversionQty   He so quy doi cua don vi goc.
 * @param quantityBase    So luong quy doi ve don vi ban le co ban.
 */
data class DongHangPhieuNhap(
    val id: String = "",
    val productId: String = "",
    val quantity: Int = 0,
    val unitPrice: Double = 0.0,
    val previousUnitPrice: Double? = null,
    val totalPrice: Double = 0.0,
    val unitId: String? = null,
    val unitName: String? = null,
    val conversionQty: Int = 1,
    val quantityBase: Int = 0,
    val product: SanPhamPhieuNhap? = null
)

/**
 * Phieu nhap da duoc server tao thanh cong.
 */
data class PhieuNhapDaTao(
    val id: String = "",
    val invoiceNumber: String = "",
    val totalAmount: Double = 0.0,
    val status: String = "",
    val notes: String? = null,
    val supplier: NhaCungCap? = null,
    val items: List<DongHangPhieuNhap> = emptyList()
)

/** Phan hoi chua phieu nhap vua tao. */
data class PhanHoiPhieuNhap(
    val data: PhieuNhapDaTao? = null
)

/**
 * Ket qua tao phieu nhap de Activity chi xu ly trang thai giao dien.
 */
sealed interface KetQuaTaoPhieuNhap {
    /** Tao phieu nhap thanh cong. */
    data class ThanhCong(val phieu: PhieuNhapDaTao) : KetQuaTaoPhieuNhap

    /**
     * Tao phieu nhap that bai.
     *
     * @param maLoi    Ma loi chuan tu server.
     * @param thongBao Thong bao cho nguoi dung.
     */
    data class ThatBai(
        val maLoi: String,
        val thongBao: String
    ) : KetQuaTaoPhieuNhap
}
