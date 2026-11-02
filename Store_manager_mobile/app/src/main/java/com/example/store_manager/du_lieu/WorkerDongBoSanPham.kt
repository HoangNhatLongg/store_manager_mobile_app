package com.example.store_manager.du_lieu

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Worker dong bo san pham va du lieu nen tu may chu.
 *
 * Worker nay giu du lieu cuc bo cua may luon khop voi may chu. Nho co worker
 * nay, khi nhan vien khac them, sua hoac xoa san pham tren may chu, thiet bi
 * cua nhan vien hien tai se tu tai du lieu moi o lan dong bo tiep theo.
 */
class WorkerDongBoSanPham(
    boiCanh: Context,
    thamSoCongViec: WorkerParameters
) : CoroutineWorker(boiCanh, thamSoCongViec) {

    /**
     * Thuc hien dong bo.
     *
     * @return Ket qua cua worker.
     */
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val kho = KhoDuLieuSanPham(applicationContext)

        // Tai du lieu nen: danh muc, don vi tinh, khach hang.
        val thanhCong = kho.dongBoDuLieuNen()
        if (!thanhCong) {
            // Mat mang thi thu lai o lan chay ke tiep.
            return@withContext Result.retry()
        }

        // Tai san pham va ghi xuong database cuc lo.
        val ketQua = kho.taiSanPham()
        if (ketQua is KetQuaTaiSanPham.ThatBai) {
            return@withContext Result.retry()
        }

        Result.success()
    }

    companion object {
        /** Ten cong viec dinh ky. */
        const val TEN_CONG_VIEC_LAP_LICH = "dong_bo_san_pham_dinh_ky"

        /** Ten cong viec chay ngay. */
        const val TEN_CONG_VIEC_CHAY_NGAY = "dong_bo_san_pham_ngay"

        /** Chu ky dong ky dinh ky, tinh bang phut. */
        private const val CHU_KY_PHUT = 15L

        /**
         * Len lich dong bo san pham dinh ky.
         *
         * Worker chi chay khi may co ket noi mang va dang sac.
         *
         * @param boiCanh Context cua ung dung.
         */
        fun lenLichDinhKy(boiCanh: Context) {
            constraintsKetNoi()?.let { yeuCau ->
                val congViec = PeriodicWorkRequestBuilder<WorkerDongBoSanPham>(
                    CHU_KY_PHUT,
                    TimeUnit.MINUTES
                )
                    .setConstraints(yeuCau)
                    .build()

                WorkManager.getInstance(boiCanh).enqueueUniquePeriodicWork(
                    TEN_CONG_VIEC_LAP_LICH,
                    ExistingPeriodicWorkPolicy.KEEP,
                    congViec
                )
            }
        }

        /**
         * Len lich dong bo san pham chay ngay mot lan.
         *
         * @param boiCanh Context cua ung dung.
         */
        fun lenLichChayNgay(boiCanh: Context) {
            constraintsKetNoi()?.let { yeuCau ->
                val congViec = OneTimeWorkRequestBuilder<WorkerDongBoSanPham>()
                    .setConstraints(yeuCau)
                    .build()

                WorkManager.getInstance(boiCanh).enqueueUniqueWork(
                    TEN_CONG_VIEC_CHAY_NGAY,
                    ExistingWorkPolicy.REPLACE,
                    congViec
                )
            }
        }

        /**
         * Tao dieu kien chi chay khi co ket noi mang.
         *
         * @return Dieu kien cong viec, null neu khong tao duoc.
         */
        private fun constraintsKetNoi(): Constraints? {
            return Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        }
    }
}
