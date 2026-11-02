package com.example.store_manager.du_lieu

import android.content.Context
import com.example.store_manager.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Lop xu ly tai bao cao doanh thu tu mobile API.
 */
class DichVuDoanhThu(boiCanh: Context) {
    private val boiCanh = boiCanh.applicationContext
    private val api = BoTaoApi.taoApi(this.boiCanh)

    /**
     * Tai bao cao doanh thu theo khoang thoi gian.
     *
     * @param ngayBatDau Ngay bat dau, dinh dang `yyyy-MM-dd`, null lay mac dinh.
     * @param ngayKetThuc Ngay ket thuc, dinh dang `yyyy-MM-dd`, null lay mac dinh.
     * @param ky         Ky gom nhom: `year`, `month` hoac `day`.
     * @return Ket qua chua bao cao doanh thu hoac loi theo ma loi chuan.
     */
    suspend fun taiBaoCaoDoanhThu(
        ngayBatDau: String? = null,
        ngayKetThuc: String? = null,
        ky: String = KY_THANG
    ): KetQuaDoanhThu = withContext(Dispatchers.IO) {
        try {
            val phanHoi = api.layBaoCaoDoanhThu(
                ngayBatDau = ngayBatDau,
                ngayKetThuc = ngayKetThuc,
                ky = ky
            )

            if (phanHoi.isSuccessful) {
                val duLieu = phanHoi.body()?.data
                if (duLieu != null) {
                    return@withContext KetQuaDoanhThu.ThanhCong(
                        PhanHoiDoanhThu(
                            summary = duLieu.summary,
                            byPeriod = duLieu.byPeriod
                        )
                    )
                }
            }

            KetQuaDoanhThu.ThatBai(
                maLoi = PhanTichLoi.layMaLoi(phanHoi),
                thongBao = boiCanh.getString(R.string.khong_the_tai_du_lieu)
            )
        } catch (_: IOException) {
            KetQuaDoanhThu.ThatBai(
                maLoi = "LOI_KET_NOI",
                thongBao = boiCanh.getString(R.string.loi_ket_noi)
            )
        } catch (_: Exception) {
            KetQuaDoanhThu.ThatBai(
                maLoi = "LOI_KHONG_XAC_DINH",
                thongBao = boiCanh.getString(R.string.khong_the_tai_du_lieu)
            )
        }
    }

    /**
     * Tai chi tiet doanh thu cua mot ngay.
     *
     * @param ngay Ngay can xem, dinh dang `yyyy-MM-dd`.
     * @return Ket qua chua chi tiet doanh thu hoac loi theo ma loi chuan.
     */
    suspend fun taiChiTietNgay(ngay: String): KetQuaDoanhThu = withContext(Dispatchers.IO) {
        try {
            val phanHoi = api.layChiTietDoanhThuNgay(ngay)

            if (phanHoi.isSuccessful) {
                val duLieu = phanHoi.body()?.data
                if (duLieu != null) {
                    return@withContext KetQuaDoanhThu.ThanhCongNgay(
                        PhanHoiDoanhThuNgay(
                            date = duLieu.date,
                            summary = duLieu.summary,
                            productsSold = duLieu.productsSold
                        )
                    )
                }
            }

            KetQuaDoanhThu.ThatBai(
                maLoi = PhanTichLoi.layMaLoi(phanHoi),
                thongBao = boiCanh.getString(R.string.khong_the_tai_du_lieu)
            )
        } catch (_: IOException) {
            KetQuaDoanhThu.ThatBai(
                maLoi = "LOI_KET_NOI",
                thongBao = boiCanh.getString(R.string.loi_ket_noi)
            )
        } catch (_: Exception) {
            KetQuaDoanhThu.ThatBai(
                maLoi = "LOI_KHONG_XAC_DINH",
                thongBao = boiCanh.getString(R.string.khong_the_tai_du_lieu)
            )
        }
    }

    companion object {
        /** Gom doanh thu theo thang, dung cho bieu do ca nam. */
        const val KY_NAM = "year"

        /** Gom doanh thu theo ngay, dung cho bieu do trong thang. */
        const val KY_THANG = "month"

        /** Gom doanh thu theo gio, dung cho chi tiet mot ngay. */
        const val KY_NGAY = "day"
    }
}
