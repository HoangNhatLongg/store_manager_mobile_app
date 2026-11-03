package com.example.store_manager.du_lieu

import android.content.Context
import com.example.store_manager.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Lop xu ly tao phieu nhap hang va tai du lieu phu tro cho man hinh nhap hang.
 *
 * Loi tra ve kem ma loi chuan de Activity hien thi thong bao phu hop, khong
 * phan biet theo noi dung message vi message co the bi thay doi theo ngon ngu.
 */
class DichVuNhapHang(boiCanh: Context) {
    private val boiCanh = boiCanh.applicationContext
    private val api = BoTaoApi.taoApi(this.boiCanh)

    /**
     * Tao phieu nhap hang tren server.
     *
     * @param yeuCau Yeu cau tao phieu nhap kem danh sach dong hang.
     * @return Ket qua phan loai theo ma loi chuan cua backend.
     */
    suspend fun taoPhieuNhap(yeuCau: YeuCauTaoPhieuNhap): KetQuaTaoPhieuNhap =
        withContext(Dispatchers.IO) {
            try {
                // Kiem tra ket noi truoc de khong phai cho het timeout khi offline.
                if (!KiemTraKetNoi.coKetNoi(this@DichVuNhapHang.boiCanh)) {
                    return@withContext KetQuaTaoPhieuNhap.ThatBai(
                        maLoi = MA_LOI_KET_NOI,
                        thongBao = boiCanh.getString(R.string.loi_ket_noi)
                    )
                }

                val phanHoi = api.taoPhieuNhap(yeuCau)

                if (phanHoi.isSuccessful) {
                    val phieu = phanHoi.body()?.data
                        ?: return@withContext KetQuaTaoPhieuNhap.ThatBai(
                            maLoi = "PHAN_HOI_KHONG_HOP_LE",
                            thongBao = boiCanh.getString(R.string.loi_phan_hoi_khong_hop_le)
                        )

                    return@withContext KetQuaTaoPhieuNhap.ThanhCong(phieu)
                }

                // Doc ma loi chuan tu body loi de quyet dinh cach xu ly.
                val loi = PhanTichLoi.layLoi(phanHoi)
                KetQuaTaoPhieuNhap.ThatBai(
                    maLoi = loi.maLoi,
                    thongBao = loi.thongBao.ifBlank {
                        boiCanh.getString(R.string.loi_may_chu)
                    }
                )
            } catch (loiHuy: kotlinx.coroutines.CancellationException) {
                // Man hinh bi dong giua chung, khong phai loi de bao nguoi dung.
                throw loiHuy
            } catch (_: IOException) {
                KetQuaTaoPhieuNhap.ThatBai(
                    maLoi = MA_LOI_KET_NOI,
                    thongBao = boiCanh.getString(R.string.loi_ket_noi)
                )
            } catch (_: Exception) {
                KetQuaTaoPhieuNhap.ThatBai(
                    maLoi = "LOI_KHONG_XAC_DINH",
                    thongBao = boiCanh.getString(R.string.loi_may_chu)
                )
            }
        }

    /**
     * Tai chi tiet mot phieu nhap da tao.
     *
     * @param maPhieu Id cua phieu nhap can xem.
     * @return Phieu nhap hoac loi theo ma loi chuan.
     */
    suspend fun layChiTietPhieuNhap(maPhieu: String): Result<PhieuNhapDaTao> =
        withContext(Dispatchers.IO) {
            runCatching {
                val phanHoi = api.layChiTietPhieuNhap(maPhieu)
                check(phanHoi.isSuccessful) { PhanTichLoi.layMaLoi(phanHoi) }
                requireNotNull(phanHoi.body()?.data)
            }
        }

    /**
     * Tai danh sach nha cung cap de nguoi dung chon trong phieu nhap.
     *
     * @return Danh sach nha cung cap rong neu tai that bai.
     */
    suspend fun taiNhaCungCap(): Result<List<NhaCungCap>> = withContext(Dispatchers.IO) {
        runCatching {
            val phanHoi = api.layDanhSachNhaCungCap()
            check(phanHoi.isSuccessful) { PhanTichLoi.layMaLoi(phanHoi) }
            phanHoi.body()?.data.orEmpty()
        }
    }

    /**
     * Tai danh muc de nguoi dung gan cho dong hang tao san pham moi.
     *
     * @return Danh sach danh muc rong neu tai that bai.
     */
    suspend fun taiDanhMuc(): Result<List<DanhMuc>> = withContext(Dispatchers.IO) {
        runCatching {
            val phanHoi = api.layDanhSachDanhMuc()
            check(phanHoi.isSuccessful) { PhanTichLoi.layMaLoi(phanHoi) }
            phanHoi.body()?.data.orEmpty()
        }
    }

    /**
     * Tai toan bo san pham de doi chieu ten khi nguoi dung nhap.
     *
     * API san pham co phan trang nen phai duyet het cac trang de khong bo
     * sot san pham nam o cac trang sau.
     *
     * @return Danh sach san pham rong neu khong tai duoc.
     */
    suspend fun taiToanBoSanPham(): Result<List<SanPham>> = withContext(Dispatchers.IO) {
        runCatching {
            val tatCa = mutableListOf<SanPham>()
            var trang = 1
            var tongTrang = 1

            while (trang <= tongTrang) {
                val phanHoi = api.layDanhSachSanPham(trang = trang, gioiHan = 50)
                check(phanHoi.isSuccessful) { PhanTichLoi.layMaLoi(phanHoi) }

                val thanhHoi = phanHoi.body() ?: break
                tatCa.addAll(thanhHoi.data)
                tongTrang = thanhHoi.pagination.totalPages
                trang++
            }

            tatCa
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

    companion object {
        /**
         * Ma loi dung khi khong ket noi duoc voi may chu.
         */
        const val MA_LOI_KET_NOI = "LOI_KET_NOI"
    }
}
