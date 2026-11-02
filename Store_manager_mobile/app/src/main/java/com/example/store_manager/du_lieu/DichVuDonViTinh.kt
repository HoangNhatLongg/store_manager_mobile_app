package com.example.store_manager.du_lieu

import android.content.Context
import com.example.store_manager.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Lop xu ly tai don vi tinh tu mobile API.
 *
 * Loi tra ve kem ma loi chuan, vi du "DUPLICATE" khi viet tat bi trung hoac
 * "UNIT_IN_USE" khi don vi dang duoc gan cho san pham.
 */
class DichVuDonViTinh(boiCanh: Context) {
    private val boiCanh = boiCanh.applicationContext
    private val api = BoTaoApi.taoApi(this.boiCanh)

    /**
     * Tai danh sach don vi tinh kem so san pham dung don vi do.
     *
     * @param tuKhoa Tu khoa tim kiem theo ten hoac viet tat, null la tai tat ca.
     * @return Danh sach don vi tinh rong neu khong tai duoc.
     */
    suspend fun taiDanhSach(tuKhoa: String? = null): Result<List<DonViTinh>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val phanHoi = api.layDanhSachDonViTinh(
                    tuKhoa = tuKhoa?.takeIf { it.isNotBlank() }
                )
                check(phanHoi.isSuccessful) { PhanTichLoi.layMaLoi(phanHoi) }
                phanHoi.body()?.data.orEmpty()
            }
        }

    /**
     * Tao don vi tinh moi tren server.
     *
     * @param duLieu Thong tin don vi can tao.
     * @return Don vi vua tao hoac loi theo ma loi chuan.
     */
    suspend fun taoDonViTinh(duLieu: DuLieuDonViTinh): Result<DonViTinh> =
        withContext(Dispatchers.IO) {
            runCatching {
                val phanHoi = api.taoDonViTinh(duLieu)
                check(phanHoi.isSuccessful) { PhanTichLoi.layMaLoi(phanHoi) }
                requireNotNull(phanHoi.body()?.data)
            }
        }

    /**
     * Cap nhat don vi tinh tren server.
     *
     * @param maDonVi Id don vi can sua.
     * @param duLieu  Thong tin moi cua don vi.
     * @return Don vi vua cap nhat hoac loi theo ma loi chuan.
     */
    suspend fun suaDonViTinh(maDonVi: String, duLieu: DuLieuDonViTinh): Result<DonViTinh> =
        withContext(Dispatchers.IO) {
            runCatching {
                val phanHoi = api.suaDonViTinh(maDonVi, duLieu)
                check(phanHoi.isSuccessful) { PhanTichLoi.layMaLoi(phanHoi) }
                requireNotNull(phanHoi.body()?.data)
            }
        }

    /**
     * Xoa don vi tinh tren server.
     *
     * @param maDonVi Id don vi can xoa.
     * @return Ket qua rong neu xoa thanh cong.
     */
    suspend fun xoaDonViTinh(maDonVi: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val phanHoi = api.xoaDonViTinh(maDonVi)
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
            "DUPLICATE" -> boiCanh.getString(R.string.loi_trung_viet_tat_don_vi)
            "UNIT_IN_USE" -> boiCanh.getString(R.string.loi_xoa_don_vi_dang_dung)
            "VALIDATION_ERROR" -> boiCanh.getString(R.string.loi_du_lieu_bat_buoc)
            "NOT_FOUND" -> boiCanh.getString(R.string.loi_khong_tim_thay_du_lieu)
            else -> ketQua.exceptionOrNull()?.message
                ?: boiCanh.getString(R.string.khong_the_tai_du_lieu)
        }
    }
}
