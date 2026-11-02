package com.example.store_manager

import com.example.store_manager.du_lieu.DichVuKhachHang
import com.example.store_manager.du_lieu.DuLieuKhachHang
import com.example.store_manager.du_lieu.KhachHang

/**
 * Man hinh quan ly khach hang (P6).
 *
 * Bam sat trang web: danh sach khach hang kem so dien thoai va email, them
 * moi, sua va xoa. Khach hang da co hoa don thi chi an di khong xoa han.
 */
class ManHinhKhachHang : ManHinhQuanLyDuLieu<KhachHang>() {

    override val tieuDeManHinh: Int = R.string.quan_ly_khach_hang
    override val moTaManHinh: Int = R.string.mo_ta_quan_ly_khach_hang
    override val chuoiNutThem: Int = R.string.them_khach_hang
    override val bieuTuongBanGhi: Int = R.drawable.bieu_tuong_khach_hang
    override val tieuDeHopThoai: Int = R.string.them_khach_hang_moi
    override val moTaHopThoai: Int = R.string.mo_ta_them_khach_hang
    override val chuoiNutLuu: Int = R.string.them_khach_hang
    override val chuoiKhongCoDuLieu: Int = R.string.chua_co_khach_hang_nao
    override val coHienTuyChonAn: Boolean = true

    private val dichVuKhachHang by lazy { DichVuKhachHang(applicationContext) }

    override fun taoAdapter(): TrinhBayBanGhi<KhachHang> {
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
    ): Result<List<KhachHang>> = dichVuKhachHang.taiDanhSach(tuKhoa, baoGomAn)

    override suspend fun taoBanGhi(
        duLieu: DuLieuFormBanGhi,
        banGhiCu: KhachHang?
    ): Result<KhachHang> {
        return dichVuKhachHang.taoKhachHang(chuyenDuLieu(duLieu))
    }

    override suspend fun suaBanGhi(
        duLieu: DuLieuFormBanGhi,
        banGhiCu: KhachHang
    ): Result<KhachHang> {
        return dichVuKhachHang.suaKhachHang(banGhiCu.id, chuyenDuLieu(duLieu))
    }

    override suspend fun xoaBanGhi(banGhi: KhachHang): Result<Unit> {
        return dichVuKhachHang.xoaKhachHang(banGhi.id)
    }

    override fun chuyenThongTinHienThi(banGhi: KhachHang): ThongTinHienThi {
        // Dien thoai la thong tin nguoi dung thuong nhin dau tien, hien truoc email.
        val moTa = listOfNotNull(
            banGhi.phone?.takeIf { it.isNotBlank() },
            banGhi.email?.takeIf { it.isNotBlank() }
        ).joinToString(" • ")

        return ThongTinHienThi(
            ten = banGhi.name,
            moTa = moTa,
            soLuong = if (banGhi.soHoaDon > 0) {
                getString(R.string.so_hoa_don_cua_khach, banGhi.soHoaDon)
            } else {
                ""
            },
            nhanTrangThai = if (banGhi.isActive) null else getString(R.string.dang_an),
            dangAn = !banGhi.isActive
        )
    }

    override fun thongBaoLoi(ketQua: Result<*>): String = dichVuKhachHang.thongBaoLoi(ketQua)

    override val chuoiXacNhanXoa: (KhachHang) -> String = { khachHang ->
        getString(R.string.xac_nhan_xoa_khach_hang, khachHang.name)
    }

    override fun layGiaTriForm(banGhi: KhachHang?): DuLieuFormBanGhi {
        return DuLieuFormBanGhi(
            ten = banGhi?.name.orEmpty(),
            dienThoai = banGhi?.phone.orEmpty(),
            email = banGhi?.email.orEmpty(),
            diaChi = banGhi?.address.orEmpty()
        )
    }

    override fun taoBanGhiTuForm(
        duLieu: DuLieuFormBanGhi,
        banGhiCu: KhachHang?
    ): KhachHang {
        return KhachHang(
            id = banGhiCu?.id.orEmpty(),
            name = duLieu.ten,
            phone = duLieu.dienThoai.ifBlank { null },
            email = duLieu.email.ifBlank { null },
            address = duLieu.diaChi.ifBlank { null },
            diem = banGhiCu?.diem ?: 0,
            isActive = banGhiCu?.isActive ?: true,
            soHoaDon = banGhiCu?.soHoaDon ?: 0
        )
    }

    override fun moFormBanGhi(banGhi: KhachHang?) {
        moBottomSheetForm(
            banGhi = banGhi,
            tieuDe = if (banGhi == null) {
                R.string.them_khach_hang_moi
            } else {
                R.string.chinh_sua_khach_hang
            },
            moTa = if (banGhi == null) {
                R.string.mo_ta_them_khach_hang
            } else {
                R.string.mo_ta_sua_khach_hang
            },
            chuoiLuu = if (banGhi == null) {
                R.string.them_khach_hang
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
    private fun chuyenDuLieu(duLieu: DuLieuFormBanGhi): DuLieuKhachHang {
        return DuLieuKhachHang(
            name = duLieu.ten,
            phone = duLieu.dienThoai,
            email = duLieu.email,
            address = duLieu.diaChi
        )
    }
}
