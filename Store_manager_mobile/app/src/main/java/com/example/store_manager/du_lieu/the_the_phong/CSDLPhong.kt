package com.example.store_manager.du_lieu.the_the_phong

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Lop co so du lieu cuc bo dung Room.
 *
 * Database nay luu san pham, danh muc, don vi, khach hang, nha cung cap, hoa don
 * ban va hang doi dong bo. Nho co database cuc lo, ung dung van ban hang duoc
 * khi mat mang va tu dong dong bo lai khi co ket noi.
 */
@Database(
    entities = [
        TheSanPham::class,
        TheDanhMuc::class,
        TheDonViTinh::class,
        TheDonViSanPham::class,
        TheKhachHang::class,
        TheNhaCungCap::class,
        TheHoaDonBan::class,
        TheDongHoaDonBan::class,
        TheHangDongBo::class
    ],
    version = 1,
    exportSchema = true
)
abstract class CSDLPhong : RoomDatabase() {

    /** Dao san pham. */
    abstract fun daoSanPham(): DaoSanPham

    /** Dao danh muc. */
    abstract fun daoDanhMuc(): DaoDanhMuc

    /** Dao don vi tinh. */
    abstract fun daoDonViTinh(): DaoDonViTinh

    /** Dao don vi ban cua san pham. */
    abstract fun daoDonViSanPham(): DaoDonViSanPham

    /** Dao khach hang. */
    abstract fun daoKhachHang(): DaoKhachHang

    /** Dao nha cung cap. */
    abstract fun daoNhaCungCap(): DaoNhaCungCap

    /** Dao hoa don ban va dong hang. */
    abstract fun daoHoaDonBan(): DaoHoaDonBan

    /** Dao hang doi dong bo. */
    abstract fun daoHangDongBo(): DaoHangDongBo

    companion object {
        /** Ten file database trong bo nho may. */
        private const val TEN_DATABASE = "cua_hang_cuc_bo.db"

        @Volatile
        private var viDuyNhat: CSDLPhong? = null

        /**
         * Lay database cuc lo, tao moi neu chua co.
         *
         * Dung mau singleton de toan bo app dung chung mot database, tranh
         * truong hop mo nhieu ket noi cung luc lam hong du lieu.
         *
         * @param boiCanh Context cua ung dung.
         * @return Database cuc lo.
         */
        fun layDatabase(boiCanh: Context): CSDLPhong {
            return viDuyNhat ?: synchronized(this) {
                viDuyNhat ?: Room.databaseBuilder(
                    boiCanh.applicationContext,
                    CSDLPhong::class.java,
                    TEN_DATABASE
                )
                    // Cho phep thay doi schema khi phat trien ma khong can
                    // xoa lai du lieu cu.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { viDuyNhat = it }
            }
        }
    }
}
