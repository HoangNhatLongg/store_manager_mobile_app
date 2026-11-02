package com.example.store_manager.du_lieu

import android.content.Context
import com.example.store_manager.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Lop xu ly tai danh muc tu mobile API.
 *
 * Moi loi tra ve deu kem ma loi chuan de Activity quyet dinh hien thi thong bao,
 * vi du "DUPLICATE" khi trung ten hoac "CATEGORY_HAS_PRODUCTS" khi danh muc
 * con san pham.
 */
class DichVuDanhMuc(boiCanh: Context) {
    private val boiCanh = boiCanh.applicationContext
    private val api = BoTaoApi.taoApi(this.boiCanh)

    /**
     * Tai danh sach danh muc kem so san pham thuoc tung danh muc.
     *
     * @param tuKhoa    Tu khoa tim kiem theo ten, null la tai tat ca.
     * @param baoGomAn True neu muon lay ca danh muc dang an.
     * @return Danh sach danh muc rong neu khong tai duoc.
     */
    suspend fun taiDanhSach(
        tuKhoa: String? = null,
        baoGomAn: Boolean = false
    ): Result<List<DanhMuc>> = withContext(Dispatchers.IO) {
        runCatching {
            val phanHoi = api.layDanhSachDanhMucQuanLy(
                tuKhoa = tuKhoa?.takeIf { it.isNotBlank() },
                baoGomAn = baoGomAn
            )
            check(phanHoi.isSuccessful) { PhanTichLoi.layMaLoi(phanHoi) }
            phanHoi.body()?.data.orEmpty()
        }
    }

    /**
     * Tao danh muc moi tren server.
     *
     * @param duLieu Thong tin danh muc can tao.
     * @return Danh muc vua tao hoac loi theo ma loi chuan.
     */
    suspend fun taoDanhMuc(duLieu: DuLieuDanhMuc): Result<DanhMuc> =
        withContext(Dispatchers.IO) {
            runCatching {
                val phanHoi = api.taoDanhMuc(duLieu)
                check(phanHoi.isSuccessful) { PhanTichLoi.layMaLoi(phanHoi) }
                requireNotNull(phanHoi.body()?.data)
            }
        }

    /**
     * Cap nhat danh muc tren server.
     *
     * @param maDanhMuc Id danh muc can sua.
     * @param duLieu    Thong tin moi cua danh muc.
     * @return Danh muc vua cap nhat hoac loi theo ma loi chuan.
     */
    suspend fun suaDanhMuc(maDanhMuc: String, duLieu: DuLieuDanhMuc): Result<DanhMuc> =
        withContext(Dispatchers.IO) {
            runCatching {
                val phanHoi = api.suaDanhMuc(maDanhMuc, duLieu)
                check(phanHoi.isSuccessful) { PhanTichLoi.layMaLoi(phanHoi) }
                requireNotNull(phanHoi.body()?.data)
            }
        }

    /**
     * Xoa danh muc tren server.
     *
     * @param maDanhMuc Id danh muc can xoa.
     * @return Ket qua rong neu xoa thanh cong.
     */
    suspend fun xoaDanhMuc(maDanhMuc: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val phanHoi = api.xoaDanhMuc(maDanhMuc)
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
            "DUPLICATE" -> boiCanh.getString(R.string.loi_trung_ten_danh_muc)
            "CATEGORY_HAS_PRODUCTS" ->
                boiCanh.getString(R.string.loi_xoa_danh_muc_con_san_pham)
            "VALIDATION_ERROR" -> boiCanh.getString(R.string.loi_du_lieu_bat_buoc)
            "NOT_FOUND" -> boiCanh.getString(R.string.loi_khong_tim_thay_du_lieu)
            else -> ketQua.exceptionOrNull()?.message
                ?: boiCanh.getString(R.string.khong_the_tai_du_lieu)
        }
    }
}
