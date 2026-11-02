package com.example.store_manager.du_lieu

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

/**
 * Tien ich chuan hoa gia tien dung chung cho ca ung dung.
 *
 * Backend tra gia tien duoi dang chuoi de tranh loi lam tron so thuc. Android
 * chuyen sang so thuc de tinh toan, sau do moi dinh dang lai khi hien thi.
 * Moi noi deu di qua lop nay de moi dau co mot quy tac lam tron thong nhat.
 */
object DinhDangTien {
    /**
     * Dinh dang gia tien theo chuan Viet Nam, vi du 12000 -> "12.000 ₫".
     *
     * @param gia Gia tri can dinh dang.
     * @return Chuoi tien da dinh dang de hien thi.
     */
    fun dinhDangTien(gia: Double): String {
        return NumberFormat.getCurrencyInstance(Locale("vi", "VN")).format(gia)
    }

    /**
     * Chuyen mot chuoi gia nhap cua nguoi dung thanh so thuc.
     *
     * Nguoi dung co the nhap "12000", "12.000" hoac "12000,5". Ham nay chuan
     * hoa dau phay thanh dau cham truoc khi chuyen doi de khong sinh NaN.
     *
     * @param chuoi Chuoi do nguoi dung nhap.
     * @return So thuc tuong ung, tra ve 0.0 neu chuoi khong hop le.
     */
    fun chuyenChuoiSangTien(chuoi: String?): Double {
        if (chuoi.isNullOrBlank()) {
            return 0.0
        }

        // Loai bo khoang trang thua va doi dau phay thanh dau cham de phan so
        // thập phan khong lam sai gia tri.
        val chuoiChuanHoa = chuoi.trim().replace(" ", "").replace(",", ".")

        return chuoiChuanHoa.toDoubleOrNull() ?: 0.0
    }

    /**
     * Kiem tra chuoi gia nhap co phai la so hop le hay khong.
     *
     * @param chuoi Chuoi do nguoi dung nhap.
     * @return True neu chuoi rong hoac khong parse duoc thanh so.
     */
    fun laGiaKhongHopLe(chuoi: String?): Boolean {
        if (chuoi.isNullOrBlank()) {
            return true
        }

        val chuoiChuanHoa = chuoi.trim().replace(" ", "").replace(",", ".")
        return chuoiChuanHoa.toDoubleOrNull() == null
    }

    /**
     * Chuyen so thuc thanh chuoi de gui len backend.
     *
     * Backend nhan gia tien qua JSON, nen phai dam bao chuoi khong chua dau
     * phay theo chuan locale Viet Nam.
     *
     * @param gia Gia tri can chuyen doi.
     * @return Chuoi so san bay, vi du 12000.0 -> "12000".
     */
    fun chuyenTienSangChuoi(gia: Double): String {
        val dauPhay = DecimalFormatSymbols(Locale.US)
        val dinhDang = DecimalFormat("#.##", dauPhay)
        return dinhDang.format(gia)
    }

    /**
     * Tao chuoi so tiền de nhung vao o nhap cua hop thoai.
     *
     * @param gia Gia tri can dua vao o nhap.
     * @return Chuoi khong co dau phay, vi du 12000.0 -> "12000".
     */
    fun taoChuoiChoOCoNhap(gia: Double): String {
        val dinhDang = DecimalFormat("#.##", DecimalFormatSymbols(Locale.US))
        return dinhDang.format(gia)
    }
}
