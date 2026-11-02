package com.example.store_manager.du_lieu

import com.google.gson.annotations.SerializedName

/**
 * Thong tin tai khoan hien tai do server tra ve.
 *
 * @param id    Dinh danh tai khoan.
 * @param email Dia chi email dang nhap.
 * @param name  Ten hien thi, co the rong.
 * @param role  Vai tro: ADMIN, MANAGER hoac STAFF.
 */
data class ThongTinTaiKhoan(
    val id: String = "",
    val email: String = "",
    val name: String? = null,
    val role: String = ""
)

/**
 * Response cua GET /api/v1/me.
 */
data class PhanHoiTaiKhoan(
    @SerializedName("user")
    val taiKhoan: ThongTinTaiKhoan? = null
)

/**
 * Ket qua kiem tra token.
 */
sealed interface KetQuaKiemTraToken {
    /** Token con hieu luc, tai khoan la nguoi dung dang dang nhap. */
    data class HieuLuc(val taiKhoan: ThongTinTaiKhoan) : KetQuaKiemTraToken

    /** Token khong con hieu luc, phai dang nhap lai. */
    data class HetHan(val maLoi: String) : KetQuaKiemTraToken

    /** Khong ket noi duoc may chu, nguoi dung van co the thu lai. */
    data class LoiKetNoi(val thongBao: String) : KetQuaKiemTraToken
}
