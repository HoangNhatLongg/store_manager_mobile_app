package com.example.store_manager.du_lieu.the_the_phong

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

/**
 * Dao doc va ghi du lieu danh muc, don vi tinh, khach hang, nha cung cap.
 *
 * Cac thao tac ghi dung co che REPLACE de du lieu cua server ghi de du lieu
 * cu, tranh nhin thay doi cu trong database.
 */
@Dao
interface DaoDanhMuc {

    /** Luu danh muc, ghi de neu id da ton tai. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun luuDanhMuc(danhSach: List<TheDanhMuc>)

    /** Xoa toan bo danh muc truoc khi ghi lai du lieu moi. */
    @Query("DELETE FROM danh_muc")
    suspend fun xoaTatCaDanhMuc()

    /** Lay danh muc dang hoat dong. */
    @Query("SELECT * FROM danh_muc ORDER BY ten ASC")
    suspend fun layDanhMuc(): List<TheDanhMuc>

    /** Ghi lai toan bo danh muc trong mot lan. */
    @Transaction
    suspend fun thayTheDanhMuc(danhSach: List<TheDanhMuc>) {
        xoaTatCaDanhMuc()
        luuDanhMuc(danhSach)
    }
}

/**
 * Dao doc va ghi don vi tinh.
 */
@Dao
interface DaoDonViTinh {

    /** Luu don vi tinh, ghi de neu id da ton tai. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun luuDonViTinh(danhSach: List<TheDonViTinh>)

    /** Xoa toan bo don vi tinh truoc khi ghi lai du lieu moi. */
    @Query("DELETE FROM don_vi_tinh")
    suspend fun xoaTatCaDonViTinh()

    /** Lay tat ca don vi tinh. */
    @Query("SELECT * FROM don_vi_tinh ORDER BY laDonViCoBan DESC, ten ASC")
    suspend fun layDonViTinh(): List<TheDonViTinh>

    /** Ghi lai toan bo don vi tinh trong mot lan. */
    @Transaction
    suspend fun thayTheDonViTinh(danhSach: List<TheDonViTinh>) {
        xoaTatCaDonViTinh()
        luuDonViTinh(danhSach)
    }
}

/**
 * Dao doc va ghi don vi ban cua san pham.
 */
@Dao
interface DaoDonViSanPham {

    /** Luu don vi ban, ghi de neu id da ton tai. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun luuDonViSanPham(danhSach: List<TheDonViSanPham>)

    /** Xoa don vi ban cua mot san pham. */
    @Query("DELETE FROM don_vi_san_pham WHERE maSanPham = :maSanPham")
    suspend fun xoaDonViCuaSanPham(maSanPham: String)

    /** Xoa toan bo don vi ban. */
    @Query("DELETE FROM don_vi_san_pham")
    suspend fun xoaTatCaDonViSanPham()

    /** Lay don vi ban cua mot san pham. */
    @Query("SELECT * FROM don_vi_san_pham WHERE maSanPham = :maSanPham")
    suspend fun layDonViCuaSanPham(maSanPham: String): List<TheDonViSanPham>

    /** Lay tat ca don vi ban. */
    @Query("SELECT * FROM don_vi_san_pham")
    suspend fun layTatCaDonViSanPham(): List<TheDonViSanPham>

    /** Ghi lai toan bo don vi ban trong mot lan. */
    @Transaction
    suspend fun thayTheDonViSanPham(danhSach: List<TheDonViSanPham>) {
        xoaTatCaDonViSanPham()
        luuDonViSanPham(danhSach)
    }
}

/**
 * Dao doc va ghi khach hang.
 */
@Dao
interface DaoKhachHang {

    /** Luu khach hang, ghi de neu id da ton tai. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun luuKhachHang(danhSach: List<TheKhachHang>)

    /** Xoa toan bo khach hang truoc khi ghi lai du lieu moi. */
    @Query("DELETE FROM khach_hang")
    suspend fun xoaTatCaKhachHang()

    /** Lay tat ca khach hang. */
    @Query("SELECT * FROM khach_hang ORDER BY ten ASC")
    suspend fun layKhachHang(): List<TheKhachHang>

    /** Ghi lai toan bo khach hang trong mot lan. */
    @Transaction
    suspend fun thayTheKhachHang(danhSach: List<TheKhachHang>) {
        xoaTatCaKhachHang()
        luuKhachHang(danhSach)
    }
}

/**
 * Dao doc va ghi nha cung cap.
 */
@Dao
interface DaoNhaCungCap {

    /** Luu nha cung cap, ghi de neu id da ton tai. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun luuNhaCungCap(danhSach: List<TheNhaCungCap>)

    /** Xoa toan bo nha cung cap truoc khi ghi lai du lieu moi. */
    @Query("DELETE FROM nha_cung_cap")
    suspend fun xoaTatCaNhaCungCap()

    /** Lay tat ca nha cung cap. */
    @Query("SELECT * FROM nha_cung_cap ORDER BY ten ASC")
    suspend fun layNhaCungCap(): List<TheNhaCungCap>

    /** Ghi lai toan bo nha cung cap trong mot lan. */
    @Transaction
    suspend fun thayTheNhaCungCap(danhSach: List<TheNhaCungCap>) {
        xoaTatCaNhaCungCap()
        luuNhaCungCap(danhSach)
    }
}
