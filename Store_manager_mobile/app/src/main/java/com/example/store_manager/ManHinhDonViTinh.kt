package com.example.store_manager

import com.example.store_manager.du_lieu.DichVuDonViTinh
import com.example.store_manager.du_lieu.DuLieuDonViTinh
import com.example.store_manager.du_lieu.DonViTinh

/**
 * Man hinh quan ly don vi tinh (P5).
 *
 * Bam sat trang web `/units`: danh sach don vi, danh dau don vi co ban, them
 * moi, sua va xoa. Viet tat la khoa duy nhat nen khong cho trung.
 */
class ManHinhDonViTinh : ManHinhQuanLyDuLieu<DonViTinh>() {

    override val tieuDeManHinh: Int = R.string.quan_ly_don_vi
    override val moTaManHinh: Int = R.string.mo_ta_quan_ly_don_vi
    override val chuoiNutThem: Int = R.string.them_don_vi_tinh
    override val bieuTuongBanGhi: Int = R.drawable.bieu_tuong_don_vi_tinh
    override val tieuDeHopThoai: Int = R.string.them_don_vi_tinh_moi
    override val moTaHopThoai: Int = R.string.mo_ta_them_don_vi
    override val chuoiNutLuu: Int = R.string.them_don_vi_tinh
    override val chuoiKhongCoDuLieu: Int = R.string.chua_co_don_vi_nao

    private val dichVuDonViTinh by lazy { DichVuDonViTinh(applicationContext) }

    override fun taoAdapter(): TrinhBayBanGhi<DonViTinh> {
        return TrinhBayBanGhi(
            chuyenThongTin = { chuyenThongTinHienThi(it) },
            khiSuaBanGhi = { moFormBanGhi(it) },
            khiXoaBanGhi = { xacNhanXoaBanGhi(it) },
            bieuTuongBanGhi = bieuTuongBanGhi
        )
    }

    override suspend fun taiDanhSach(
        tuKhoa: String?,
        baoGomAn: Boolean
    ): Result<List<DonViTinh>> = dichVuDonViTinh.taiDanhSach(tuKhoa)

    override suspend fun taoBanGhi(
        duLieu: DuLieuFormBanGhi,
        banGhiCu: DonViTinh?
    ): Result<DonViTinh> {
        return dichVuDonViTinh.taoDonViTinh(
            DuLieuDonViTinh(
                name = duLieu.ten,
                abbreviation = duLieu.vietTat.lowercase(),
                isBaseUnit = duLieu.laDonViCoBan
            )
        )
    }

    override suspend fun suaBanGhi(
        duLieu: DuLieuFormBanGhi,
        banGhiCu: DonViTinh
    ): Result<DonViTinh> {
        return dichVuDonViTinh.suaDonViTinh(
            banGhiCu.id,
            DuLieuDonViTinh(
                name = duLieu.ten,
                abbreviation = duLieu.vietTat.lowercase(),
                isBaseUnit = duLieu.laDonViCoBan
            )
        )
    }

    override suspend fun xoaBanGhi(banGhi: DonViTinh): Result<Unit> {
        return dichVuDonViTinh.xoaDonViTinh(banGhi.id)
    }

    override fun chuyenThongTinHienThi(banGhi: DonViTinh): ThongTinHienThi {
        // Don vi co ban duoc danh dau rieng vi anh huong cach quy doi ton kho.
        val nhan = if (banGhi.laDonViCoBan) {
            getString(R.string.don_vi_co_ban)
        } else {
            null
        }

        return ThongTinHienThi(
            ten = banGhi.name,
            moTa = getString(R.string.viet_tat_cua, banGhi.abbreviation),
            soLuong = if (banGhi.soSanPham > 0) {
                getString(R.string.so_san_pham, banGhi.soSanPham)
            } else {
                ""
            },
            nhanTrangThai = nhan
        )
    }

    override fun thongBaoLoi(ketQua: Result<*>): String = dichVuDonViTinh.thongBaoLoi(ketQua)

    override val chuoiXacNhanXoa: (DonViTinh) -> String = { donVi ->
        getString(R.string.xac_nhan_xoa_don_vi, donVi.name)
    }

    override val lyDoKhongTheXoa: (DonViTinh) -> String? = { donVi ->
        // Don vi dang gan cho san pham thi xoa se lam mat lien ket.
        if (donVi.soSanPham > 0) {
            getString(R.string.loi_xoa_don_vi_dang_dung)
        } else {
            null
        }
    }

    override fun layGiaTriForm(banGhi: DonViTinh?): DuLieuFormBanGhi {
        return DuLieuFormBanGhi(
            ten = banGhi?.name.orEmpty(),
            vietTat = banGhi?.abbreviation.orEmpty(),
            laDonViCoBan = banGhi?.laDonViCoBan ?: false
        )
    }

    override fun taoBanGhiTuForm(
        duLieu: DuLieuFormBanGhi,
        banGhiCu: DonViTinh?
    ): DonViTinh {
        return DonViTinh(
            id = banGhiCu?.id.orEmpty(),
            name = duLieu.ten,
            abbreviation = duLieu.vietTat.lowercase(),
            laDonViCoBan = duLieu.laDonViCoBan,
            soSanPham = banGhiCu?.soSanPham ?: 0
        )
    }

    override fun moFormBanGhi(banGhi: DonViTinh?) {
        moBottomSheetForm(
            banGhi = banGhi,
            tieuDe = if (banGhi == null) {
                R.string.them_don_vi_tinh_moi
            } else {
                R.string.chinh_sua_don_vi_tinh
            },
            moTa = if (banGhi == null) {
                R.string.mo_ta_them_don_vi
            } else {
                R.string.mo_ta_sua_don_vi
            },
            chuoiLuu = if (banGhi == null) {
                R.string.them_don_vi_tinh
            } else {
                R.string.luu_thay_doi
            },
            hienVietTat = true,
            hienDonViCoBan = true
        )
    }
}
