package com.example.store_manager.du_lieu

import android.content.Context
import com.example.store_manager.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import java.io.IOException

/**
 * Lop goi API dashboard. Hien tai du lieu doc tu server; buoc Room sau se
 * luu lai phan hoi nay de hien thi khi khong co mang.
 */
class DichVuTongQuan(boiCanh: Context) {
    private val boiCanh = boiCanh.applicationContext
    private val api = BoTaoApi.taoApi(this.boiCanh)

    /** Tai cac chi so, hoat dong, canh bao va san pham ban chay. */
    suspend fun taiDuLieu(): KetQuaTongQuan = withContext(Dispatchers.IO) {
        try {
            val phanHoi = api.layDuLieuTongQuan()

            if (phanHoi.isSuccessful) {
                val duLieu = phanHoi.body()

                if (duLieu != null) {
                    return@withContext KetQuaTongQuan.ThanhCong(duLieu)
                }

                return@withContext KetQuaTongQuan.ThatBai(
                    maLoi = "PHAN_HOI_KHONG_HOP_LE",
                    thongBao = boiCanh.getString(R.string.loi_phan_hoi_khong_hop_le)
                )
            }

            if (phanHoi.code() == 401) {
                return@withContext KetQuaTongQuan.ThatBai(
                    maLoi = "UNAUTHORIZED",
                    thongBao = boiCanh.getString(R.string.phien_dang_nhap_het_han)
                )
            }

            KetQuaTongQuan.ThatBai(
                maLoi = "HTTP_${phanHoi.code()}",
                thongBao = boiCanh.getString(R.string.loi_tai_tong_quan)
            )
        } catch (_: IOException) {
            KetQuaTongQuan.ThatBai(
                maLoi = "LOI_KET_NOI",
                thongBao = boiCanh.getString(R.string.loi_ket_noi)
            )
        } catch (_: HttpException) {
            KetQuaTongQuan.ThatBai(
                maLoi = "LOI_HTTP",
                thongBao = boiCanh.getString(R.string.loi_tai_tong_quan)
            )
        } catch (_: Exception) {
            KetQuaTongQuan.ThatBai(
                maLoi = "LOI_KHONG_XAC_DINH",
                thongBao = boiCanh.getString(R.string.loi_tai_tong_quan)
            )
        }
    }
}
