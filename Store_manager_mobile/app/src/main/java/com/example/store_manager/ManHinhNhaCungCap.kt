package com.example.store_manager

import com.example.store_manager.du_lieu.DichVuNhaCungCap
import com.example.store_manager.du_lieu.DuLieuNhaCungCap
import com.example.store_manager.du_lieu.NhaCungCap

/**
 * Man hinh quan ly nha cung cap (P7).
 *
 * Bam sat trang web: danh sach nha cung cap kem so dien thoai va email, them
 * moi, sua va xoa. Nha cung cap da co phieu nhap thi chi an di khong xoa han.
 */
class ManHinhNhaCungCap : ManHinhQuanLyDuLieu<NhaCungCap>() {

    override val tieuDeManHinh: Int = R.string.quan_ly_nha_cung_cap
    override val moTaManHinh: Int = R.string.mo_ta_quan_ly_nha_cung_cap
    override val chuoiNutThem: Int = R.string.them_nha_cung_cap
    override val bieuTuongBanGhi: Int = R.drawable.bieu_tuong_nha_cung_cap
    override val tieuDeHopThoai: Int = R.string.them_nha_cung_cap_moi
    override val moTaHopThoai: Int = R.string.mo_ta_them_nha_cung_cap
    override val chuoiNutLuu: Int = R.string.them_nha_cung_cap
    override val chuoiKhongCoDuLieu: Int = R.string.chua_co_nha_cung_cap_nao
    override val coHienTuyChonAn: Boolean = true

    private val dichVuNhaCungCap by lazy { DichVuNhaCungCap(applicationContext) }

    override fun taoAdapter(): TrinhBayBanGhi<NhaCungCap> {
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
    ): Result<List<NhaCungCap>> = dichVuNhaCungCap.taiDanhSach(tuKhoa, baoGomAn)

    override suspend fun taoBanGhi(
        duLieu: DuLieuFormBanGhi,
        banGhiCu: NhaCungCap?
    ): Result<NhaCungCap> {
        return dichVuNhaCungCap.taoNhaCungCap(chuyenDuLieu(duLieu))
    }

    override suspend fun suaBanGhi(
        duLieu: DuLieuFormBanGhi,
        banGhiCu: NhaCungCap
    ): Result<NhaCungCap> {
        return dichVuNhaCungCap.suaNhaCungCap(banGhiCu.id, chuyenDuLieu(duLieu))
    }

    override suspend fun xoaBanGhi(banGhi: NhaCungCap): Result<Unit> {
        return dichVuNhaCungCap.xoaNhaCungCap(banGhi.id)
    }

    override fun chuyenThongTinHienThi(banGhi: NhaCungCap): ThongTinHienThi {
        // Dien thoai la thong tin nguoi dung thuong nhin dau tien, hien truoc email.
        val moTa = listOfNotNull(
            banGhi.phone?.takeIf { it.isNotBlank() },
            banGhi.email?.takeIf { it.isNotBlank() }
        ).joinToString(" • ")

        return ThongTinHienThi(
            ten = banGhi.name,
            moTa = moTa,
            soLuong = if (banGhi.soPhieuNhap > 0) {
                getString(R.string.so_phieu_nhap, banGhi.soPhieuNhap)
            } else {
                ""
            },
            nhanTrangThai = if (banGhi.isActive) null else getString(R.string.dang_an),
            dangAn = !banGhi.isActive
        )
    }

    override fun thongBaoLoi(ketQua: Result<*>): String = dichVuNhaCungCap.thongBaoLoi(ketQua)

    override val chuoiXacNhanXoa: (NhaCungCap) -> String = { nhaCungCap ->
        getString(R.string.xac_nhan_xoa_nha_cung_cap, nhaCungCap.name)
    }

    override fun layGiaTriForm(banGhi: NhaCungCap?): DuLieuFormBanGhi {
        return DuLieuFormBanGhi(
            ten = banGhi?.name.orEmpty(),
            dienThoai = banGhi?.phone.orEmpty(),
            email = banGhi?.email.orEmpty(),
            diaChi = banGhi?.address.orEmpty()
        )
    }

    override fun taoBanGhiTuForm(
        duLieu: DuLieuFormBanGhi,
        banGhiCu: NhaCungCap?
    ): NhaCungCap {
        return NhaCungCap(
            id = banGhiCu?.id.orEmpty(),
            name = duLieu.ten,
            phone = duLieu.dienThoai.ifBlank { null },
            email = duLieu.email.ifBlank { null },
            address = duLieu.diaChi.ifBlank { null },
            isActive = banGhiCu?.isActive ?: true,
            soPhieuNhap = banGhiCu?.soPhieuNhap ?: 0
        )
    }

    override fun moFormBanGhi(banGhi: NhaCungCap?) {
        moBottomSheetForm(
            banGhi = banGhi,
            tieuDe = if (banGhi == null) {
                R.string.them_nha_cung_cap_moi
            } else {
                R.string.chinh_sua_nha_cung_cap
            },
            moTa = if (banGhi == null) {
                R.string.mo_ta_them_nha_cung_cap
            } else {
                R.string.mo_ta_sua_nha_cung_cap
            },
            chuoiLuu = if (banGhi == null) {
                R.string.them_nha_cung_cap
            } else {
                R.string.luu_thay_doi
            },
            hienLienHe = true
        )
    }

    /**
     * Chuyen gia tri cua form thanh du lieu gui len server.
     *
     * @param duLieu Gia tri cua form.
     * @return Du lieu gui len server.
     */
    private fun chuyenDuLieu(duLieu: DuLieuFormBanGhi): DuLieuNhaCungCap {
        return DuLieuNhaCungCap(
            name = duLieu.ten,
            phone = duLieu.dienThoai,
            email = duLieu.email,
            address = duLieu.diaChi
        )
    }
}
