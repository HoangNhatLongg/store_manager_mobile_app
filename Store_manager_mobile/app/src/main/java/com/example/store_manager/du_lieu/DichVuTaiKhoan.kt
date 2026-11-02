package com.example.store_manager.du_lieu

import android.content.Context
import com.example.store_manager.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/**
 * Lop goi API kiem tra thong tin tai khoan hien tai.
 *
 * Dung khi mo app de xac nhan access token con hieu luc truoc khi vao man hinh
 * chinh, tranh truong hop nguoi dung thay doi thong tin tai khoan tren server.
 */
class DichVuTaiKhoan(boiCanh: Context) {
    private val boiCanh = boiCanh.applicationContext
    private val api = BoTaoApi.taoApi(this.boiCanh)
    private val boNhoPhien = BoNhoPhienDangNhap(this.boiCanh)

    /**
    20|     * Kiem tra access token hien tai co con hieu luc khong.
     *
     * @return Ket qua phan loai: hieu luc, het han hoac loi ket noi.
     */
    suspend fun kiemTraToken(): KetQuaKiemTraToken = withContext(Dispatchers.IO) {
        try {
            val phanHoi = api.layThongTinTaiKhoan()

            if (phanHoi.isSuccessful) {
                // Server tra ve thong tin tai khoan, dung chinh thong tin nay
                // de quyet dinh vao man hinh nao sau do.
                val taiKhoan = phanHoi.body()?.taiKhoan

                return@withContext if (taiKhoan == null) {
                    KetQuaKiemTraToken.HetHan(maLoi = "PHAN_HOI_KHONG_HOP_LE")
                } else {
                    KetQuaKiemTraToken.HieuLuc(taiKhoan)
                }
            }

            // Token co the chi het han chu khong phai bi thu hoi. Thu lam moi
            // token truoc khi ket luan phai dang nhap lai.
            if (phanHoi.code() == 401) {
                if (lamMoiToken()) {
                    val phanHoiMoi = api.layThongTinTaiKhoan()

                    if (phanHoiMoi.isSuccessful) {
                        val taiKhoan = phanHoiMoi.body()?.taiKhoan
                        if (taiKhoan != null) {
                            return@withContext KetQuaKiemTraToken.HieuLuc(taiKhoan)
                        }
                    }
                }
            }

            // 401 nghia la token het han hoac khong con ton tai tren server.
            val loi = PhanTichLoi.layLoi(phanHoi)
            KetQuaKiemTraToken.HetHan(maLoi = loi.maLoi)
        } catch (_: IOException) {
            // Mat mang khong dong nghia voi token invalid, nen giu nguyen phien
            // dang nhap va thong bao nguoi dung thu lai sau.
            KetQuaKiemTraToken.LoiKetNoi(
                thongBao = boiCanh.getString(R.string.loi_ket_noi)
            )
        } catch (_: Exception) {
            KetQuaKiemTraToken.LoiKetNoi(
                thongBao = boiCanh.getString(R.string.loi_may_chu)
            )
        }
    }

    /**
     * Thu lam moi access token bang refresh token.
     *
     * @return True neu lay duoc access token moi.
     */
    private suspend fun lamMoiToken(): Boolean = withContext(Dispatchers.IO) {
        val tokenLamMoi = boNhoPhien.layTokenLamMoi()
        if (tokenLamMoi.isNullOrBlank()) {
            return@withContext false
        }

        try {
            val phanHoi = api.lamMoiToken(YeuCauLamMoiToken(tokenLamMoi))

            if (!phanHoi.isSuccessful) {
                return@withContext false
            }

            val ketQua = phanHoi.body() ?: return@withContext false

            // Luu token moi vao bo nho cho cac request sau.
            boNhoPhien.luuMaTruyCap(ketQua.maTruyCap)
            boNhoPhien.luuTokenLamMoi(ketQua.tokenLamMoi)

            true
        } catch (_: IOException) {
            false
        } catch (_: Exception) {
            false
        }
    }
}
