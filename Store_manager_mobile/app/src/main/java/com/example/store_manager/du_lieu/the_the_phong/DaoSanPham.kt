package com.example.store_manager.du_lieu.the_the_phong

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update

/**
 * Dao doc va ghi san pham trong database cuc lo.
 */
@Dao
interface DaoSanPham {

    /** Luu san pham, ghi de neu id da ton tai. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun luuSanPham(danhSach: List<TheSanPham>)

    /** Luu mot san pham. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun luuSanPham(sanPham: TheSanPham)

    /** Cap nhat ton kho cua san pham. */
    @Query("UPDATE san_pham SET tonKho = :tonKho WHERE id = :maSanPham")
    suspend fun capNhatTonKho(maSanPham: String, tonKho: Int)

    /** Xoa toan bo san pham truoc khi ghi lai du lieu moi. */
    @Query("DELETE FROM san_pham")
    suspend fun xoaTatCaSanPham()

    /** Lay san pham dang hoat dong. */
    @Query("SELECT * FROM san_pham WHERE dangHoatDong = 1 ORDER BY ten ASC")
    suspend fun laySanPhamDangHoatDong(): List<TheSanPham>

    /** Lay san pham theo tu khoa tim kiem. */
    @Query(
        "SELECT * FROM san_pham " +
            "WHERE dangHoatDong = 1 AND (ten LIKE '%' || :tuKhoa || '%' OR maSku LIKE '%' || :tuKhoa || '%') " +
            "ORDER BY ten ASC"
    )
    suspend fun timSanPham(tuKhoa: String): List<TheSanPham>

    /** Lay san pham theo id, dung khi can tra cuu rieng. */
    @Query("SELECT * FROM san_pham WHERE id = :maSanPham")
    suspend fun laySanPham(maSanPham: String): TheSanPham?

    /**
     * Lay san pham theo danh muc.
     *
     * @param maDanhMuc Id danh muc, null la lay tat ca.
     */
    @Query("SELECT * FROM san_pham WHERE dangHoatDong = 1 AND maDanhMuc = :maDanhMuc ORDER BY ten ASC")
    suspend fun laySanPhamTheoDanhMuc(maDanhMuc: String): List<TheSanPham>

    /**
     * Dem san pham theo muc ton kho, dung cho thong ke offline.
     *
     * @param maDanhMuc Id danh muc, null la dem tat ca.
     * @param tuKhoa    Tu khoa tim kiem, null la khong loc.
     */
    @Query(
        "SELECT COUNT(*) FROM san_pham " +
            "WHERE dangHoatDong = 1 " +
            "AND (:maDanhMuc IS NULL OR maDanhMuc = :maDanhMuc) " +
            "AND (:tuKhoa IS NULL OR ten LIKE '%' || :tuKhoa || '%' OR maSku LIKE '%' || :tuKhoa || '%')"
    )
    suspend fun demSanPhamTheoBocLoc(maDanhMuc: String?, tuKhoa: String?): Int

    /**
     * Dem so san pham het hang theo boc loc.
     *
     * @param maDanhMuc Id danh muc, null la dem tat ca.
     * @param tuKhoa    Tu khoa tim kiem, null la khong loc.
     */
    @Query(
        "SELECT COUNT(*) FROM san_pham " +
            "WHERE dangHoatDong = 1 AND tonKho = 0 " +
            "AND (:maDanhMuc IS NULL OR maDanhMuc = :maDanhMuc) " +
            "AND (:tuKhoa IS NULL OR ten LIKE '%' || :tuKhoa || '%' OR maSku LIKE '%' || :tuKhoa || '%')"
    )
    suspend fun demSanPhamHetHang(maDanhMuc: String?, tuKhoa: String?): Int

    /**
     * Dem so san pham sap het theo boc loc.
     *
     * Sap het la ton kho lon hon 0 va nho hon hoac bang muc toi thieu.
     *
     * @param maDanhMuc Id danh muc, null la dem tat ca.
     * @param tuKhoa    Tu khoa tim kiem, null la khong loc.
     */
    @Query(
        "SELECT COUNT(*) FROM san_pham " +
            "WHERE dangHoatDong = 1 AND tonKho > 0 AND tonKho <= tonToiThieu " +
            "AND (:maDanhMuc IS NULL OR maDanhMuc = :maDanhMuc) " +
            "AND (:tuKhoa IS NULL OR ten LIKE '%' || :tuKhoa || '%' OR maSku LIKE '%' || :tuKhoa || '%')"
    )
    suspend fun demSanPhamSapHet(maDanhMuc: String?, tuKhoa: String?): Int

    /** Dem so san pham dang co trong database. */
    @Query("SELECT COUNT(*) FROM san_pham")
    suspend fun demSoSanPham(): Int

    /** Ghi lai toan bo san pham trong mot lan. */
    @Transaction
    suspend fun thayTheSanPham(danhSach: List<TheSanPham>) {
        xoaTatCaSanPham()
        luuSanPham(danhSach)
    }
}

/**
 * Dao doc va ghi hoa don ban, dong hang va hang doi dong bo.
 */
@Dao
interface DaoHoaDonBan {

    /** Luu hoa don ban. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun luuHoaDon(hoaDon: TheHoaDonBan)

    /** Cap nhat hoa don ban. */
    @Update
    suspend fun capNhatHoaDon(hoaDon: TheHoaDonBan)

    /** Luu cac dong hang cua hoa don. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun luuDongHang(danhSach: List<TheDongHoaDonBan>)

    /** Lay hoa don theo operationId. */
    @Query("SELECT * FROM hoa_don_ban WHERE operationId = :operationId LIMIT 1")
    suspend fun layHoaDonTheoOperationId(operationId: String): TheHoaDonBan?

    /** Lay dong hang cua mot hoa don. */
    @Query("SELECT * FROM dong_hoa_don_ban WHERE maHoaDon = :maHoaDon")
    suspend fun layDongHang(maHoaDon: String): List<TheDongHoaDonBan>

    /** Lay tat ca hoa don theo trang thai. */
    @Query("SELECT * FROM hoa_don_ban WHERE trangThai = :trangThai ORDER BY taoLuc DESC")
    suspend fun layHoaDonTheoTrangThai(trangThai: TrangThaiHoaDon): List<TheHoaDonBan>

    /** Dem so hoa don chua dong bo. */
    @Query("SELECT COUNT(*) FROM hoa_don_ban WHERE trangThai = 'CHUA_DONG_BO'")
    suspend fun demSoHoaDonChuaDongBo(): Int

    /** Ghi hoa don kem dong hang trong mot lan. */
    @Transaction
    suspend fun luuHoaDonKemDongHang(
        hoaDon: TheHoaDonBan,
        dongHang: List<TheDongHoaDonBan>
    ) {
        luuHoaDon(hoaDon)
        luuDongHang(dongHang)
    }
}

/**
 * Dao doc va ghi hang doi dong bo.
 */
@Dao
interface DaoHangDongBo {

    /** Luu phan tu hang doi. */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun luuHangDongBo(phanTu: TheHangDongBo)

    /** Cap nhat phan tu hang doi. */
    @Update
    suspend fun capNhatHangDongBo(phanTu: TheHangDongBo)

    /** Lay cac phan tu cho san xep vao hang doi. */
    @Query("SELECT * FROM hang_dong_bo WHERE trangThai = 'CHO_GUI' ORDER BY taoLuc ASC")
    suspend fun layHangDoiChoGui(): List<TheHangDongBo>

    /** Dem so phan tu dang cho gui. */
    @Query("SELECT COUNT(*) FROM hang_dong_bo WHERE trangThai = 'CHO_GUI'")
    suspend fun demSoPhanTuChoGui(): Int

    /** Lay phan tu hang doi theo operationId. */
    @Query("SELECT * FROM hang_dong_bo WHERE operationId = :operationId LIMIT 1")
    suspend fun layTheoOperationId(operationId: String): TheHangDongBo?

    /** Xoa phan tu hang doi. */
    @Query("DELETE FROM hang_dong_bo WHERE id = :maPhanTu")
    suspend fun xoaPhanTu(maPhanTu: String)
}
