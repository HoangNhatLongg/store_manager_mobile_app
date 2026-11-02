package com.example.store_manager.du_lieu

import com.google.gson.annotations.SerializedName

/**
 * Du lieu gui len API dang nhap.
 * Ten thuoc tinh Kotlin dung tieng Viet khong dau, con ten JSON duoc giu dung
 * theo contract ma backend Next.js da cong bo.
 */
data class YeuCauDangNhap(
    @SerializedName("email")
    val diaChiEmail: String,

    @SerializedName("password")
    val matKhau: String
)

/**
 * Thong tin tai khoan server tra ve sau khi dang nhap thanh cong.
 */
data class ThongTinNguoiDung(
    val id: String,
    val email: String,
    val name: String?,
    val role: String
)

/**
 * Response thanh cong cua POST /api/v1/auth/login.
 */
data class PhanHoiDangNhap(
    @SerializedName("accessToken")
    val maTruyCap: String,

    /**
     * Token dung de lai access token moi khi het han.
     *
     * Co the rong neu backend chua duoc cap nhat len phien ban refresh.
     */
    @SerializedName("refreshToken")
    val tokenLamMoi: String? = null,

    val tokenType: String,
    val expiresIn: Int,
    val user: ThongTinNguoiDung
)

/**
 * Du lieu gui len API lam moi token.
 */
data class YeuCauLamMoiToken(
    @SerializedName("refreshToken")
    val tokenLamMoi: String
)

/**
 * Response thanh cong cua POST /api/v1/auth/refresh.
 */
data class PhanHoiLamMoiToken(
    @SerializedName("accessToken")
    val maTruyCap: String,

    @SerializedName("refreshToken")
    val tokenLamMoi: String,

    val tokenType: String,
    val expiresIn: Int
)

/**
 * Du lieu gui len API dang xuat.
 */
data class YeuCauDangXuat(
    @SerializedName("refreshToken")
    val tokenLamMoi: String
)

/**
 * Response loi co dang chung tu mobile API.
 */
data class PhanHoiLoi(
    val code: String? = null,
    val message: String? = null
)

/**
 * Ket qua nghiep vu de Activity khong phai tu xu ly HTTP/JSON.
 */
sealed interface KetQuaDangNhap {
    data class ThanhCong(
        val phanHoi: PhanHoiDangNhap
    ) : KetQuaDangNhap

    data class ThatBai(
        val maLoi: String,
        val thongBao: String
    ) : KetQuaDangNhap
}
