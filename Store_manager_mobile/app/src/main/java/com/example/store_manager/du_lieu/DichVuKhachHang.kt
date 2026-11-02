package com.example.store_manager.du_lieu

import android.content.Context
import com.example.store_manager.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lop xu ly tai khach hang tu mobile API.
 *
 * Loi tra ve kem ma loi chuan de Activity hien thi thong bao phu hop.
 */
class DichVuKhachHang(boiCanh: Context) {
    private val boiCanh = boiCanh.applicationContext
    private val api = BoTaoApi.taoApi(this.boiCanh)

    /**
     * Tai danh sach khach hang.
     *
     * @param tuKhoa    Tu khoa tim kiem theo ten, dien thoai hoac email.
     * @param baoGomAn True neu muon lay ca khach hang dang an.
     * @return Danh sach khach hang rong neu khong tai duoc.
     */
    suspend fun taiDanhSach(
        tuKhoa: String? = null,
        baoGomAn: Boolean = false
    ): Result<List<KhachHang>> = withContext(Dispatchers.IO) {
        runCatching {
            val phanHoi = api.layDanhSachKhachHangQuanLy(
                tuKhoa = tuKhoa?.takeIf { it.isNotBlank() },
                baoGomAn = baoGomAn
            )
            check(phanHoi.isSuccessful) { PhanTichLoi.layMaLoi(phanHoi) }
            phanHoi.body()?.data.orEmpty()
        }
    }

    /**
     * Tao khach hang moi tren server.
     *
     * @param duLieu Thong tin khach hang can tao.
     * @return Khach hang vua tao hoac loi theo ma loi chuan.
     */
    suspend fun taoKhachHang(duLieu: DuLieuKhachHang): Result<KhachHang> =
        withContext(Dispatchers.IO) {
            runCatching {
                val phanHoi = api.taoKhachHang(duLieu)
                check(phanHoi.isSuccessful) { PhanTichLoi.layMaLoi(phanHoi) }
                requireNotNull(phanHoi.body()?.data)
            }
        }

    /**
     * Cap nhat khach hang tren server.
     *
     * @param maKhachHang Id khach hang can sua.
     * @param duLieu      Thong tin moi cua khach hang.
     * @return Khach hang vua cap nhat hoac loi theo ma loi chuan.
     */
    suspend fun suaKhachHang(
        maKhachHang: String,
        duLieu: DuLieuKhachHang
    ): Result<KhachHang> = withContext(Dispatchers.IO) {
        runCatching {
            val phanHoi = api.suaKhachHang(maKhachHang, duLieu)
            check(phanHoi.isSuccessful) { PhanTichLoi.layMaLoi(phanHoi) }
            requireNotNull(phanHoi.body()?.data)
        }
    }

    /**
     * Xoa hoac an khach hang tren server.
     *
     * @param maKhachHang Id khach hang can xoa.
     * @return Ket qua rong neu xoa thanh cong.
     */
    suspend fun xoaKhachHang(maKhachHang: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val phanHoi = api.xoaKhachHang(maKhachHang)
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
