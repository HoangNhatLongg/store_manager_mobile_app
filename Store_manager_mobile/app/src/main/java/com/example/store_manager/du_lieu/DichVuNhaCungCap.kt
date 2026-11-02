package com.example.store_manager.du_lieu

import android.content.Context
import com.example.store_manager.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lop xu ly tai nha cung cap tu mobile API.
 *
 * Loi tra ve kem ma loi chuan de Activity hien thi thong bao phu hop.
 */
class DichVuNhaCungCap(boiCanh: Context) {
    private val boiCanh = boiCanh.applicationContext
    private val api = BoTaoApi.taoApi(this.boiCanh)

    /**
     * Tai danh sach nha cung cap.
     *
     * @param tuKhoa    Tu khoa tim kiem theo ten, dien thoai hoac email.
     * @param baoGomAn True neu muon lay ca nha cung cap dang an.
     * @return Danh sach nha cung cap rong neu khong tai duoc.
     */
    suspend fun taiDanhSach(
        tuKhoa: String? = null,
        baoGomAn: Boolean = false
    ): Result<List<NhaCungCap>> = withContext(Dispatchers.IO) {
        runCatching {
            val phanHoi = api.layDanhSachNhaCungCap(
                tuKhoa = tuKhoa?.takeIf { it.isNotBlank() },
                baoGomAn = baoGomAn
            )
            check(phanHoi.isSuccessful) { PhanTichLoi.layMaLoi(phanHoi) }
            phanHoi.body()?.data.orEmpty()
        }
    }

    /**
     * Tao nha cung cap moi tren server.
     *
     * @param duLieu Thong tin nha cung cap can tao.
     * @return Nha cung cap vua tao hoac loi theo ma loi chuan.
     */
    suspend fun taoNhaCungCap(duLieu: DuLieuNhaCungCap): Result<NhaCungCap> =
        withContext(Dispatchers.IO) {
            runCatching {
                val phanHoi = api.taoNhaCungCap(duLieu)
                check(phanHoi.isSuccessful) { PhanTichLoi.layMaLoi(phanHoi) }
                requireNotNull(phanHoi.body()?.data)
            }
        }

    /**
     * Cap nhat nha cung cap tren server.
     *
     * @param maNhaCungCap Id nha cung cap can sua.
     * @param duLieu        Thong tin moi cua nha cung cap.
     * @return Nha cung cap vua cap nhat hoac loi theo ma loi chuan.
     */
    suspend fun suaNhaCungCap(
        maNhaCungCap: String,
        duLieu: DuLieuNhaCungCap
    ): Result<NhaCungCap> = withContext(Dispatchers.IO) {
        runCatching {
            val phanHoi = api.suaNhaCungCap(maNhaCungCap, duLieu)
            check(phanHoi.isSuccessful) { PhanTichLoi.layMaLoi(phanHoi) }
            requireNotNull(phanHoi.body()?.data)
        }
    }

    /**
     * Xoa hoac an nha cung cap tren server.
     *
     * @param maNhaCungCap Id nha cung cap can xoa.
     * @return Ket qua rong neu xoa thanh cong.
     */
    suspend fun xoaNhaCungCap(maNhaCungCap: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val phanHoi = api.xoaNhaCungCap(maNhaCungCap)
            check(phanHoi.isSuccessful) { PhanTichLoi.layMaLoi(phanHoi) }
        }
    }

    /**
     * Chuyen loi tu server thanh thong bao tieng Viet.
     *
     * @param ketQua Ket qua tra ve tu server.
     * @return Chuoi thong bao hien thi cho nguoi dung.
     */
    fun thongBaoLoi(ketQua: Result<*>): String {
        val maLoi = ketQua.exceptionOrNull()?.message.orEmpty()

        return when (maLoi) {
            "VALIDATION_ERROR" -> boiCanh.getString(R.string.loi_du_lieu_bat_buoc)
            "NOT_FOUND" -> boiCanh.getString(R.string.loi_khong_tim_thay_du_lieu)
            else -> ketQua.exceptionOrNull()?.message
                ?: boiCanh.getString(R.string.khong_the_tai_du_lieu)
        }
    }
}
