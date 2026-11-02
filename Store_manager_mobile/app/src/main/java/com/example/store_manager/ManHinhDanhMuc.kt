package com.example.store_manager

import com.example.store_manager.du_lieu.DanhMuc
import com.example.store_manager.du_lieu.DichVuDanhMuc
import com.example.store_manager.du_lieu.DuLieuDanhMuc

/**
 * Man hinh quan ly danh muc san pham (P4).
 *
 * Bam sat trang web `/categories`: danh sach the ten, so san pham thuoc danh
 * muc, them moi, sua va xoa. Danh muc con san pham thi khong cho xoa.
 */
class ManHinhDanhMuc : ManHinhQuanLyDuLieu<DanhMuc>() {

    override val tieuDeManHinh: Int = R.string.quan_ly_danh_muc
    override val moTaManHinh: Int = R.string.mo_ta_quan_ly_danh_muc
    override val chuoiNutThem: Int = R.string.them_danh_muc
    override val bieuTuongBanGhi: Int = R.drawable.bieu_tuong_thu_muc
    override val tieuDeHopThoai: Int = R.string.them_danh_muc_moi
    override val moTaHopThoai: Int = R.string.mo_ta_them_danh_muc
    override val chuoiNutLuu: Int = R.string.them_danh_muc
    override val chuoiKhongCoDuLieu: Int = R.string.chua_co_danh_muc_nao
    override val coHienTuyChonAn: Boolean = true

    private val dichVuDanhMuc by lazy { DichVuDanhMuc(applicationContext) }

    override fun taoAdapter(): TrinhBayBanGhi<DanhMuc> {
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
    ): Result<List<DanhMuc>> = dichVuDanhMuc.taiDanhSach(tuKhoa, baoGomAn)

    override suspend fun taoBanGhi(
        duLieu: DuLieuFormBanGhi,
        banGhiCu: DanhMuc?
    ): Result<DanhMuc> {
        return dichVuDanhMuc.taoDanhMuc(
            DuLieuDanhMuc(name = duLieu.ten, description = duLieu.moTa)
        )
    }

    override suspend fun suaBanGhi(
        duLieu: DuLieuFormBanGhi,
        banGhiCu: DanhMuc
    ): Result<DanhMuc> {
        return dichVuDanhMuc.suaDanhMuc(
            banGhiCu.id,
            DuLieuDanhMuc(name = duLieu.ten, description = duLieu.moTa)
        )
    }

    override suspend fun xoaBanGhi(banGhi: DanhMuc): Result<Unit> {
        return dichVuDanhMuc.xoaDanhMuc(banGhi.id)
    }

    override fun chuyenThongTinHienThi(banGhi: DanhMuc): ThongTinHienThi {
        return ThongTinHienThi(
            ten = banGhi.name,
            moTa = banGhi.description.orEmpty(),
            soLuong = getString(R.string.so_san_pham, banGhi.soSanPham),
            nhanTrangThai = if (banGhi.isActive) null else getString(R.string.dang_an),
            dangAn = !banGhi.isActive
        )
    }

    override fun thongBaoLoi(ketQua: Result<*>): String = dichVuDanhMuc.thongBaoLoi(ketQua)

    override val chuoiXacNhanXoa: (DanhMuc) -> String = { danhMuc ->
        getString(R.string.xac_nhan_xoa_danh_muc, danhMuc.name)
    }

    override val lyDoKhongTheXoa: (DanhMuc) -> String? = { danhMuc ->
        // Danh muc con san pham thi xoa se lam san pham mat danh muc.
        if (danhMuc.soSanPham > 0) {
            getString(R.string.loi_xoa_danh_muc_con_san_pham)
        } else {
            null
        }
    }

    override fun layGiaTriForm(banGhi: DanhMuc?): DuLieuFormBanGhi {
        return DuLieuFormBanGhi(
            ten = banGhi?.name.orEmpty(),
            moTa = banGhi?.description.orEmpty()
        )
    }

    override fun taoBanGhiTuForm(
        duLieu: DuLieuFormBanGhi,
        banGhiCu: DanhMuc?
    ): DanhMuc {
        return DanhMuc(
            id = banGhiCu?.id.orEmpty(),
            name = duLieu.ten,
            description = duLieu.moTa.ifBlank { null },
            isActive = banGhiCu?.isActive ?: true,
            soSanPham = banGhiCu?.soSanPham ?: 0
        )
    }

    override fun moFormBanGhi(banGhi: DanhMuc?) {
        moBottomSheetForm(
            banGhi = banGhi,
            tieuDe = if (banGhi == null) {
                R.string.them_danh_muc_moi
            } else {
                R.string.chinh_sua_danh_muc
            },
            moTa = if (banGhi == null) {
                R.string.mo_ta_them_danh_muc
            } else {
                R.string.mo_ta_sua_danh_muc
            },
            chuoiLuu = if (banGhi == null) {
                R.string.them_danh_muc
            } else {
                R.string.luu_thay_doi
            },
            hienMoTa = true
        )
    }
}
