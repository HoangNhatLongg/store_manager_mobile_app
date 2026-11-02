package com.example.store_manager.du_lieu

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.store_manager.du_lieu.the_the_phong.TrangThaiHoaDon
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker dong bo hoa don ban tao offline len server.
 *
 * Worker chay khi co ket noi mang. No doc cac phan tu trong hang doi, gui len
 * server va cap nhat trang thai hoa don cuc lo theo ket qua.
 *
 * Xu ly xung dot ton kho: khi hai thiet bi cung ban mot san pham chi con mot
 * mon, thiet bi nao dong bo truoc se thang. Thiet bi con lai nhan ma loi
 * 409 STOCK_CONFLICT, hoa don duoc danh dau XUNG_DOT va nguoi dung duoc
 * canh bao de quyet dinh hoan tien hoac dieu chinh ton kho.
 */
class WorkerDongBoHoaDon(
    boiCanh: Context,
    thamSoCongViec: WorkerParameters
) : CoroutineWorker(boiCanh, thamSoCongViec) {

    /**
     * Thuc hien dong bo.
     *
     * @return Ket qua cua worker. Tra ve that bai khi con phan tu chua xu ly
     *         duoc de WorkerManager thu lai lan sau.
     */
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val kho = KhoDuLieuSanPham(applicationContext)
        val dichVu = DichVuBanHang(applicationContext)
        val daoHangDongBo = kho.layDaoHangDongBo()

        val phanTuHangDoi = daoHangDongBo.layHangDoiChoGui()
        if (phanTuHangDoi.isEmpty()) {
            return@withContext Result.success()
        }

        // Cac phan tu duoc xu ly thanh cong, khong can thu lai.
        var coPhanTuChuaXuLy = false

        for (phanTu in phanTuHangDoi) {
            // Ghi trang thai dang gui truoc khi goi mang de tranh hai worker
            // cung xu ly mot phan tu.
            daoHangDongBo.capNhatHangDongBo(
                phanTu.copy(
                    trangThai = KhoDuLieuSanPham.TRANG_THAI_DANG_GUI,
                    soLanThu = phanTu.soLanThu + 1
                )
            )

            val yeuCau = kho.docYeuCauTuHangDongBo(phanTu)
            if (yeuCau == null) {
                // Du lieu JSON khong doc duoc, khong the gui lai. Danh dau that bai.
                daoHangDongBo.capNhatHangDongBo(
                    phanTu.copy(
                        trangThai = KhoDuLieuSanPham.TRANG_THAI_THAT_BAI,
                        thongBaoLoi = "Du lieu hoa don khong hop le"
                    )
                )
                continue
            }

            when (val ketQua = dichVu.taoHoaDon(yeuCau)) {
                is KetQuaTaoHoaDon.ThanhCong -> {
                    // Server da nhan hoa don, xoa phan tu khoi hang doi.
                    daoHangDongBo.xoaPhanTu(phanTu.id)
                    kho.capNhatTrangThaiHoaDon(
                        operationId = phanTu.operationId,
                        trangThai = TrangThaiHoaDon.DA_DONG_BO,
                        soHoaDonServer = ketQua.hoaDon.invoiceNumber
                    )
                }

                is KetQuaTaoHoaDon.ThatBai -> {
                    when (ketQua.maLoi) {
                        // Xung dot ton kho: khong the tu dong gui lai, can nguoi
                        // dung quyet dinh.
                        "STOCK_CONFLICT" -> {
                            daoHangDongBo.capNhatHangDongBo(
                                phanTu.copy(
                                    trangThai = KhoDuLieuSanPham.TRANG_THAI_XUNG_DOT,
                                    thongBaoLoi = ketQua.thongBao
                                )
                            )
                            kho.capNhatTrangThaiHoaDon(
                                operationId = phanTu.operationId,
                                trangThai = TrangThaiHoaDon.XUNG_DOT,
                                thongBaoLoi = ketQua.thongBao
                            )
                        }

                        // Loi du lieu khong hop le: gui lai cung khong duoc.
                        "VALIDATION_ERROR" -> {
                            daoHangDongBo.capNhatHangDongBo(
                                phanTu.copy(
                                    trangThai = KhoDuLieuSanPham.TRANG_THAI_THAT_BAI,
                                    thongBaoLoi = ketQua.thongBao
                                )
                            )
                            kho.capNhatTrangThaiHoaDon(
                                operationId = phanTu.operationId,
                                trangThai = TrangThaiHoaDon.THAT_BAI,
                                thongBaoLoi = ketQua.thongBao
                            )
                        }

                        // Mat mang: tra phan tu ve hang doi cho gui lai.
                        else -> {
                            daoHangDongBo.capNhatHangDongBo(
                                phanTu.copy(
                                    trangThai = KhoDuLieuSanPham.TRANG_THAI_CHO_GUI,
                                    thongBaoLoi = ketQua.thongBao
                                )
                            )
                            coPhanTuChuaXuLy = true
                        }
                    }
                }
            }
        }

        if (coPhanTuChuaXuLy) {
            Result.retry()
        } else {
            Result.success()
        }
    }

    companion object {
        /** Ten cong viec duy nhat, tranh chay trung nhieu worker. */
        const val TEN_CONG_VIEC = "dong_bo_hoa_don_offline"

        /**
         * Len lich dong bo hoa don khi co ket noi mang.
         *
         * @param boiCanh Context cua ung dung.
         */
        fun lenLich(boiCanh: Context) {
            val yeuCau = OneTimeWorkRequestBuilder<WorkerDongBoHoaDon>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()

            WorkManager.getInstance(boiCanh).enqueueUniqueWork(
                TEN_CONG_VIEC,
                ExistingWorkPolicy.REPLACE,
                yeuCau
            )
        }
    }
}
