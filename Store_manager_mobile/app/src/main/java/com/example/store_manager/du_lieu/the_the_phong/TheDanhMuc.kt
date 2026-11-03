package com.example.store_manager.du_lieu.the_the_phong

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The thể danh mục trong database cuc bo.
 *
 * @param id           Id danh muc tu server.
 * @param ten          Ten danh muc.
 * @param slug         Duong dan tren web, dung de doi chieu khi can.
 * @param capNhatLuc   Thoi diem server cap nhat lan cuoi, dung cho dong bo gia tang.
 */
@Entity(tableName = "danh_muc")
data class TheDanhMuc(
    @PrimaryKey val id: String,
    val ten: String,
    val slug: String? = null,
    val capNhatLuc: Long = 0L
)

/**
 * The thể don vi tinh trong database cuc lo.
 *
 * @param id           Id don vi tinh tu server.
 * @param ten          Ten don vi, vi du "Chai", "Thung".
 * @param vietTat      Viet tat, vi du "chai", "th".
 * @param laDonViCoBan True neu day la don vi co ban cua san pham.
 */
@Entity(tableName = "don_vi_tinh")
data class TheDonViTinh(
    @PrimaryKey val id: String,
    val ten: String,
    val vietTat: String = "",
    val laDonViCoBan: Boolean = true
)

/**
 * The thể don vi ban cua mot san pham trong database cuc lo.
 *
 * Luu he so quy doi va gia ban rieng cho tung don vi de POS ban theo don vi
 * quy doi ma van dung ton kho theo don vi co ban.
 *
 * @param id             Id cua product_unit tren server.
 * @param maSanPham      Id san pham chua don vi nay.
 * @param maDonViTinh    Id don vi tinh.
 * @param soLuongQuyDoi  So don vi co ban trong 1 don vi ban, vi du 1 thung = 24 chai.
 * @param gia            Gia ban cua 1 don vi ban.
 * @param laMacDinh      True neu la don vi mac dinh cua san pham.
 * @param laDonViGoc     True neu la don vi goc dung de nhap hang, vi du thung, loc.
 */
@Entity(tableName = "don_vi_san_pham")
data class TheDonViSanPham(
    @PrimaryKey val id: String,
    val maSanPham: String,
    val maDonViTinh: String,
    val soLuongQuyDoi: Int = 1,
    val gia: Double = 0.0,
    val laMacDinh: Boolean = false,
    val laDonViGoc: Boolean = false
)

/**
 * The thể khach hang trong database cuc lo.
 *
 * @param id           Id khach hang tu server.
 * @param ten          Ten khach hang.
 * @param soDienThoai  So dien thoai, dung de hien thi va tim kiem.
 * @param email        Email khach hang.
 * @param diaChi       Dia chi khach hang.
 */
@Entity(tableName = "khach_hang")
data class TheKhachHang(
    @PrimaryKey val id: String,
    val ten: String,
    val soDienThoai: String? = null,
    val email: String? = null,
    val diaChi: String? = null
)

/**
 * The thể nha cung cap trong database cuc lo.
 *
 * @param id   Id nha cung cap tu server.
 * @param ten  Ten nha cung cap.
 */
@Entity(tableName = "nha_cung_cap")
data class TheNhaCungCap(
    @PrimaryKey val id: String,
    val ten: String
)
