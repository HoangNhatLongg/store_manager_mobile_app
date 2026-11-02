package com.example.store_manager.du_lieu

import android.content.Context
import com.example.store_manager.BuildConfig
import com.google.gson.Gson
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Tu dong lam moi access token khi server tra ve loi 401.
 *
 * Access token chi song 15 phut. Neu nguoi dung mat mang lau roi ket noi lai,
 * token da het han va moi API deu tra 401, app se bi dang xuat oan.
 *
 * Authenticator cua OkHttp se duoc goi khi nhan duoc 401. Khi do, ta goi API
 * lay access token moi bang refresh token roi thu lai request cu. Nguoi dung
 * khong phai dang nhap lai.
 *
 * Chi mot request duoc thu lai mot lan. Neu van 401 sau khi refresh thi
 * return null de OkHttp khong lap vo han, va phien dang nhap se bi xoa.
 */
class LamMoiTokenTuDong(
    private val boiCanh: Context
) : Authenticator {

    private val boNhoPhien = BoNhoPhienDangNhap(boiCanh)
    private val boPhanTichJson = Gson()

    /**
     * Client rieng cho API lam moi token.
     *
     * Client nay khong dung interceptor gan token va khong co authenticator,
     * tranh viec goi lai chinh no va gay vo han.
     */
    private val clientLamMoi = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    override fun authenticate(route: Route?, response: Response): Request? {
        // Chi thu lai mot lan. Neu van that bai thi dung phien dang nhap.
        if (responseCount(response) >= 2) {
            return null
        }

        val tokenLamMoi = boNhoPhien.layTokenLamMoi()
        if (tokenLamMoi.isNullOrBlank()) {
            return null
        }

        // Khong the lam moi token khi dang offline, vi khi do khong co ket noi
        // de goi API va request cu cung khong the thanh cong.
        if (!KiemTraKetNoi.coKetNoi(boiCanh)) {
            return null
        }

        return try {
            val accessTokenMoi = goiApiLamMoiToken(tokenLamMoi) ?: return null

            // Luu token moi vao bo nho de cac request sau dung dung token.
            boNhoPhien.luuMaTruyCap(accessTokenMoi.maTruyCap)
            boNhoPhien.luuTokenLamMoi(accessTokenMoi.tokenLamMoi)

            // Tao lai request voi access token moi.
            response.request.newBuilder()
                .header("Authorization", "Bearer ${accessTokenMoi.maTruyCap}")
                .build()
        } catch (_: IOException) {
            // Mat mang trong luc lam moi token, khong thu lai duoc.
            null
        }
    }

    /**
     * Goi API lam moi token va tra ve token moi.
     *
     * @param tokenLamMoi Refresh token hien tai.
     * @return Token moi, null neu server tu choi refresh token.
     */
    private fun goiApiLamMoiToken(tokenLamMoi: String): PhanHoiLamMoiToken? {
        val boDoi = Gson().toJson(YeuCauLamMoiToken(tokenLamMoi))
        val yeuCau = Request.Builder()
            .url("${BuildConfig.DIA_CHI_API}api/v1/auth/refresh")
            .post(boDoi.toRequestBody("application/json".toMediaType()))
            .build()

        clientLamMoi.newCall(yeuCau).execute().use { phanHoi ->
            if (!phanHoi.isSuccessful) {
                // Server tu choi refresh token. Phien dang nhap khong con gia
                // tri, phai dang nhap lai.
                if (phanHoi.code == 401) {
                    boNhoPhien.xoaPhien()
                }
                return null
            }

            val noiDung = phanHoi.body?.string() ?: return null

            return boPhanTichJson.fromJson(noiDung, PhanHoiLamMoiToken::class.java)
        }
    }

    /**
     * Dem so lan da thu lai request trong chuoi interceptor.
     *
     * @param response Phan hoi hien tai.
     * @return So lan thu lai.
     */
    private fun responseCount(response: Response): Int {
        var soLan = 1
        var phanHoiTruoc: Response? = response.priorResponse

        while (phanHoiTruoc != null) {
            soLan++
            phanHoiTruoc = phanHoiTruoc.priorResponse
        }

        return soLan
    }
}
