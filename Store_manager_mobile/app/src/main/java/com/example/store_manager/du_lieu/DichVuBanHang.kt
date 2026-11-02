package com.example.store_manager.du_lieu

import android.content.Context
import com.example.store_manager.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

/**
 * Lop xu ly tao hoa don ban va tai danh sach khach hang.
 *
 * Moi yeu cau tao hoa don deu kem theo operationId de server co the phat
 * hien request trung. Khi nguoi dung bam lai nut xac nhan sau khi loi,
 * app phai dung lai cung mot operationId de khong tao hai hoa don.
 */
class DichVuBanHang(boiCanh: Context) {
    private val boiCanh = boiCanh.applicationContext
    private val api = BoTaoApi.taoApi(this.boiCanh)
    /**
     * Tai danh sach khach hang de nguoi dung chon trong man hinh thanh toan.
     *
     * @return Danh sach khach dang hoat dong, danh sach rong neu loi.
     */
    suspend fun taiKhachHang(): Result<List<KhachHang>> = withContext(Dispatchers.IO) {
        try {
            // Kiem tra ket noi truoc de khong phai cho het timeout khi offline.
            if (!KiemTraKetNoi.coKetNoi(this@DichVuBanHang.boiCanh)) {
                return@withContext Result.failure(IOException("Khong co ket noi mang"))
            }

            val phanHoi = api.layDanhSachKhachHang()
            check(phanHoi.isSuccessful) { "HTTP_${phanHoi.code()}" }
            Result.success(phanHoi.body()?.data.orEmpty())
        } catch (loiHuy: kotlinx.coroutines.CancellationException) {
            // Man hinh bi dong giua chung, khong phai loi de bao nguoi dung.
            throw loiHuy
        } catch (loi: Exception) {
            Result.failure(loi)
        }
    }

    /**
     * Tao hoa don ban tren server.
     *
     * @param yeuCau Yeu cau tao hoa don kem operationId.
     * @return Ket qua phan loai theo ma loi chuan cua backend.
     */
    suspend fun taoHoaDon(yeuCau: YeuCauTaoHoaDon): KetQuaTaoHoaDon =
        withContext(Dispatchers.IO) {
            try {
                // Kiem tra ket noi truoc. Neu thiet bi dang offline thi goi API
                // se phai cho het thoi gian timeout moi bao loi, gay treo
                // giao dien khi nhan vien bam xac nhan thanh toan.
                if (!KiemTraKetNoi.coKetNoi(this@DichVuBanHang.boiCanh)) {
                    return@withContext KetQuaTaoHoaDon.ThatBai(
                        maLoi = MA_LOI_KET_NOI,
                        thongBao = boiCanh.getString(R.string.loi_ket_noi)
                    )
                }

                val phanHoi = api.taoHoaDon(yeuCau)

                if (phanHoi.isSuccessful) {
                    val thanhHoi = phanHoi.body()
                    val hoaDon = thanhHoi?.invoice

                    // Server tra ve "ALREADY_PROCESSED" khi operationId da duoc
                    // xu ly truoc do, nghia la hoa don da ton tai va khong can tao lai.
                    if (hoaDon == null) {
                        return@withContext KetQuaTaoHoaDon.ThatBai(
                            maLoi = "PHAN_HOI_KHONG_HOP_LE",
                            thongBao = boiCanh.getString(R.string.loi_phan_hoi_khong_hop_le)
                        )
                    }

                    return@withContext KetQuaTaoHoaDon.ThanhCong(
                        hoaDon = hoaDon,
                        daTonTai = thanhHoi.status == "ALREADY_PROCESSED"
                    )
                }

                // Doc ma loi chuan tu body loi de quyet dinh cach xu ly.
                val loi = PhanTichLoi.layLoi(phanHoi)
                KetQuaTaoHoaDon.ThatBai(
                    maLoi = loi.maLoi,
                    thongBao = loi.thongBao.ifBlank {
                        boiCanh.getString(R.string.loi_may_chu)
                    }
                )
            } catch (loiHuy: kotlinx.coroutines.CancellationException) {
                // Man hinh bi dong giua chung, khong phai loi de bao nguoi dung.
                throw loiHuy
            } catch (_: IOException) {
                // Mat mang: giu nguyen operationId de nguoi dung bam lai duoc.
                KetQuaTaoHoaDon.ThatBai(
                    maLoi = MA_LOI_KET_NOI,
                    thongBao = boiCanh.getString(R.string.loi_ket_noi)
                )
            } catch (_: Exception) {
                KetQuaTaoHoaDon.ThatBai(
                    maLoi = "LOI_KHONG_XAC_DINH",
                    thongBao = boiCanh.getString(R.string.loi_may_chu)
                )
            }
        }

    /**
     * Sinh operationId moi cho mot lan thanh toan.
     *
     * @return Chuoi UUID duy nhat.
     */
    fun sinhMaThaoTac(): String = UUID.randomUUID().toString()

    companion object {
        /**
         * Ma loi dung khi khong ket noi duoc voi may chu.
         *
         * Trong truong hop nay hoa don duoc luu cuc bo thay vi bao loi.
         */
        const val MA_LOI_KET_NOI = "LOI_KET_NOI"
    }
}
