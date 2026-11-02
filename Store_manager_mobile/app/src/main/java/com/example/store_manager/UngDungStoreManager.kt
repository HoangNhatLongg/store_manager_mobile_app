package com.example.store_manager

import android.app.Application
import com.example.store_manager.du_lieu.WorkerDongBoHoaDon
import com.example.store_manager.du_lieu.WorkerDongBoSanPham

/**
 * Lop Application cua ung dung.
 *
 * Khi app khoi dong, hai worker duoc len lich ngay:
 * - Worker dong bo hoa don: gui cac hoa don tao offline len may chu.
 * - Worker dong bo san pham: tai san pham va du lieu nen moi tu may chu.
 *
 * Worker chi chay khi thiet bi co ket noi mang nen khong ton tai nham nho
 * khi app dang dung.
 */
class UngDungStoreManager : Application() {

    /**
     * Khoi dong ung dung va len lich cac cong viec dong bo nen.
     */
    override fun onCreate() {
        super.onCreate()

        // Len lich dong bo hoa don offline khi co ket noi.
        WorkerDongBoHoaDon.lenLich(this)

        // Len lich dong bo san pham dinh ky de du lieu cuc bo luon moi.
        WorkerDongBoSanPham.lenLichDinhKy(this)

        // Lan chay dau tien tai ngay du lieu de may co san pham san sang.
        WorkerDongBoSanPham.lenLichChayNgay(this)
    }
}
