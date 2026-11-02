package com.example.store_manager.du_lieu.the_the_phong

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * The thể san pham trong database cuc lo.
 *
 * Ton kho luong theo don vi co ban, giong voi server. Khi ban offline, so
 * luong ton kho con lai se duoc tinh tren gia tri cua bang nay.
 *
 * @param id           Id san pham tu server.
 * @param ten          Ten san pham.
 * @param maSku        Ma SKU.
 * @param maVach       Ma vach.
 * @param giaNhap      Gia nhap theo don vi co ban.
 * @param giaBan       Gia ban theo don vi co ban.
 * @param tonKho       Ton kho theo don vi co ban.
 * @param tonToiThieu  Ton kho toi thieu de canh bao het hang.
 * @param moTa         Mo ta san pham.
 * @param anh          Duong dan anh san pham.
 * @param maDanhMuc    Id danh muc.
 * @param dangHoatDong True neu san pham dang duoc hien thi.
 * @param capNhatLuc   Thoi diem server cap nhat lan cuoi, dung cho dong bo gia tang.
 */
@Entity(tableName = "san_pham")
data class TheSanPham(
    @PrimaryKey val id: String,
    val ten: String,
    val maSku: String = "",
    val maVach: String? = null,
    val giaNhap: Double = 0.0,
    val giaBan: Double = 0.0,
    val tonKho: Int = 0,
    val tonToiThieu: Int = 10,
    val moTa: String? = null,
    val anh: String? = null,
    val maDanhMuc: String? = null,
    val dangHoatDong: Boolean = true,
    val capNhatLuc: Long = 0L
)
