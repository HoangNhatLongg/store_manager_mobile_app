package com.example.store_manager.du_lieu

import android.content.Context
import com.example.store_manager.R
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

/**
 * Lop chua logic dang nhap: goi API, doc loi va luu token.
 * Activity chi cung cap email/mat khau va hien thi KetQuaDangNhap.
 */
class DichVuDangNhap(boiCanh: Context) {
    private val boiCanh = boiCanh.applicationContext
    private val api = BoTaoApi.taoApi()
    private val boNhoPhien = BoNhoPhienDangNhap(this.boiCanh)
    private val boPhanTichJson = Gson()

    /**
     * Chay request tren IO dispatcher de khong chan giao dien Android.
     */
    suspend fun dangNhap(
        diaChiEmail: String,
        matKhau: String
    ): KetQuaDangNhap = withContext(Dispatchers.IO) {
        try {
            // Gui request den endpoint mobile va cho backend kiem tra bcrypt.
            val ketQua = api.dangNhap(
                YeuCauDangNhap(
                    diaChiEmail = diaChiEmail,
                    matKhau = matKhau
                )
            )

            if (ketQua.isSuccessful) {
                // Response thanh cong co access token; token phai duoc luu lai
                // de cac man hinh sau dung khi goi bootstrap/products.
                val phanHoi = ketQua.body()

                if (phanHoi == null || phanHoi.maTruyCap.isBlank()) {
                    return@withContext KetQuaDangNhap.ThatBai(
                        maLoi = "PHAN_HOI_KHONG_HOP_LE",
                        thongBao = boiCanh.getString(R.string.loi_phan_hoi_khong_hop_le)
                    )
                }

                boNhoPhien.luuPhien(phanHoi)
                return@withContext KetQuaDangNhap.ThanhCong(phanHoi)
            }

            // Backend tra JSON loi gom code/message de mobile hien thi dung ngu canh.
            val noiDungLoi = ketQua.errorBody()?.string()
            val phanHoiLoi = noiDungLoi?.let {
                runCatching {
                    boPhanTichJson.fromJson(it, PhanHoiLoi::class.java)
                }.getOrNull()
            }

            KetQuaDangNhap.ThatBai(
                maLoi = phanHoiLoi?.code ?: "HTTP_${ketQua.code()}",
                thongBao = phanHoiLoi?.message
                    ?: "Khong the dang nhap (ma loi ${ketQua.code()})"
            )
        } catch (_: IOException) {
            // IOException thuong xay ra khi mat mang, sai dia chi server hoac
            // emulator khong truy cap duoc may tinh chay Next.js.
            KetQuaDangNhap.ThatBai(
                maLoi = "LOI_KET_NOI",
                thongBao = "Khong the ket noi den may chu"
            )
        } catch (_: HttpException) {
            // Bao ve them cho loi HTTP phat sinh o tang Retrofit/OkHttp.
            KetQuaDangNhap.ThatBai(
                maLoi = "LOI_HTTP",
                thongBao = boiCanh.getString(R.string.loi_http)
            )
        } catch (_: Exception) {
            // Khong de exception roi ra Activity lam ung dung bi crash.
            KetQuaDangNhap.ThatBai(
                maLoi = "LOI_KHONG_XAC_DINH",
                thongBao = boiCanh.getString(R.string.loi_khong_xac_dinh)
            )
        }
    }
}
