package com.example.store_manager.du_lieu

import com.example.store_manager.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import java.util.concurrent.TimeUnit

/**
 * Khai bao cac endpoint cua backend mobile.
 * Retrofit se tu dong chuyen ham Kotlin nay thanh HTTP request.
 */
interface ApiCuaHang {
    @POST("api/v1/auth/login")
    suspend fun dangNhap(
        @Body yeuCau: YeuCauDangNhap
    ): Response<PhanHoiDangNhap>
}

/**
 * Noi tao Retrofit dung chung cho toan bo ung dung.
 */
object BoTaoApi {
    fun taoApi(): ApiCuaHang {
        // Interceptor nay chi de xem request/response khi debug, khong bat body
        // log trong production vi response co the chua access token.
        val boGhiLog = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        // Cau hinh timeout de app khong bi treo vo han neu may chu khong phan hoi.
        val khachHttp = OkHttpClient.Builder()
            .addInterceptor(boGhiLog)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        // DIA_CHI_API duoc dat trong build.gradle.kts de de thay doi theo moi truong.
        return Retrofit.Builder()
            .baseUrl(BuildConfig.DIA_CHI_API)
            .client(khachHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiCuaHang::class.java)
    }
}
