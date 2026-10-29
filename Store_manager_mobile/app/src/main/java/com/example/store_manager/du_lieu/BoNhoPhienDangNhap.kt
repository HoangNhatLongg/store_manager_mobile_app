package com.example.store_manager.du_lieu

import android.content.Context

/**
 * Lop luu thong tin phien dang nhap tren thiet bi.
 *
 * Ban dau dung SharedPreferences de de hieu luong. Khi app chuan bi phat hanh,
 * co the thay bang DataStore + Android Keystore de bao ve token tot hon.
 */
class BoNhoPhienDangNhap(boiCanh: Context) {
    private val boNho = boiCanh.getSharedPreferences(
        TEN_BO_NHO,
        Context.MODE_PRIVATE
    )

    /** Luu access token va thong tin co ban cua nguoi dung. */
    fun luuPhien(phanHoi: PhanHoiDangNhap) {
        boNho.edit()
            .putString(KHOA_MA_TRUY_CAP, phanHoi.maTruyCap)
            .putString(KHOA_ID_NGUOI_DUNG, phanHoi.user.id)
            .putString(KHOA_EMAIL, phanHoi.user.email)
            .putString(KHOA_TEN, phanHoi.user.name)
            .putString(KHOA_VAI_TRO, phanHoi.user.role)
            .apply()
    }

    /** Lay access token de gui vao header Authorization o cac API tiep theo. */
    fun layMaTruyCap(): String? {
        return boNho.getString(KHOA_MA_TRUY_CAP, null)
    }

    /** Xoa phien khi nguoi dung dang xuat hoac token khong con hop le. */
    fun xoaPhien() {
        boNho.edit().clear().apply()
    }

    companion object {
        private const val TEN_BO_NHO = "phien_dang_nhap"
        private const val KHOA_MA_TRUY_CAP = "ma_truy_cap"
        private const val KHOA_ID_NGUOI_DUNG = "id_nguoi_dung"
        private const val KHOA_EMAIL = "email"
        private const val KHOA_TEN = "ten"
        private const val KHOA_VAI_TRO = "vai_tro"
    }
}
