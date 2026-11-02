package com.example.store_manager.du_lieu

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

/**
 * Lop kiem tra thiet bi co ket noi mang hay khong.
 *
 * Dung de biet truoc khi goi API. Neu thiet bi dang offline thi goi API se
 * phai cho het thoi gian timeout moi tra ve loi, gay treo giao dien. Kiem tra
 * truoc giup ung dung phan hoi ngay lap tuc.
 */
object KiemTraKetNoi {

    /**
     * Kiem tra thiet bi co duoc ket noi mang khong.
     *
     * @param boiCanh Context cua ung dung.
     * @return True neu co ket noi mang.
     */
    fun coKetNoi(boiCanh: Context): Boolean {
        val quanLyKetNoi = boiCanh.getSystemService(Context.CONNECTIVITY_SERVICE)
            as? ConnectivityManager
            ?: return false

        val mangHienTai = quanLyKetNoi.activeNetwork ?: return false
        val nangLuc = quanLyKetNoi.getNetworkCapabilities(mangHienTai) ?: return false

        // Mang dang co va duoc phep truy cap Internet moi co dung de goi API.
        return nangLuc.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            nangLuc.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
