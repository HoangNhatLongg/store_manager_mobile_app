package com.example.store_manager.du_lieu

import android.content.Context
import com.example.store_manager.R
import com.example.store_manager.du_lieu.the_the_phong.CSDLPhong
import com.example.store_manager.du_lieu.the_the_phong.ChuyenDoiDuLieu
import com.example.store_manager.du_lieu.the_the_phong.DaoHangDongBo
import com.example.store_manager.du_lieu.the_the_phong.TheDonViSanPham
import com.example.store_manager.du_lieu.the_the_phong.TheHangDongBo
import com.example.store_manager.du_lieu.the_the_phong.TheHoaDonBan
import com.example.store_manager.du_lieu.the_the_phong.TheDongHoaDonBan
import com.example.store_manager.du_lieu.the_the_phong.TrangThaiHoaDon
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.UUID

/**
 * Kho du lieu dung chung giua server va database cuc lo.
 *
 * Lop nay la noi duy nhat doc va ghi du lieu san pham. Khi co mang thi du
 * lieu duoc tai tu server va luu xuong database cuc lo. Khi mat mang thi
 * du lieu duoc doc tu database cuc lo, giup nhan vien van ban hang duoc.
 */
class KhoDuLieuSanPham(boiCanh: Context) {
    private val boiCanh = boiCanh.applicationContext
    private val csdl = CSDLPhong.layDatabase(this.boiCanh)
    private val dichVuSanPham = DichVuSanPham(this.boiCanh)
    private val dichVuBanHang = DichVuBanHang(this.boiCanh)
    private val boPhanTichJson = Gson()

    /**
     * Tai danh sach san pham.
     *
     * Luon uu tien du lieu tu server khi ket noi duoc. Neu mat mang thi doc
     * tu database cuc lo de nguoi dung van thay duoc san pham.
     *
     * @param tuKhoa    Tu khoa tim kiem, null la tai tat ca.
     * @param maDanhMuc Id danh muc de loc, null la khong loc.
     * @param trang     So trang can tai.
     * @param gioiHan   So san pham toi da tren mot trang, null la khong gioi han.
     * @return Ket qua chua danh sach san pham va thong bao loi neu co.
     */
    suspend fun taiSanPham(
        tuKhoa: String? = null,
        maDanhMuc: String? = null,
        trang: Int = 1,
        gioiHan: Int? = null
    ): KetQuaTaiSanPham = withContext(Dispatchers.IO) {
        try {
            val ketQua = dichVuSanPham.taiDanhSach(
                tuKhoa = tuKhoa,
                maDanhMuc = maDanhMuc,
                trang = trang,
                gioiHan = gioiHan ?: GIOI_HAN_MAC_DINH
            )

            when (ketQua) {
                is KetQuaSanPham.ThanhCong -> {
                    // Luu du lieu moi xuong database cuc lo de san sang cho lan sau.
                    luuSanPhamVaoCucBo(ketQua.danhSach)
                    return@withContext KetQuaTaiSanPham.ThanhCong(
                        danhSach = ketQua.danhSach,
                        phanTrang = ketQua.phanTrang,
                        tuCache = false
                    )
                }

                is KetQuaSanPham.ThatBai -> {
                    // Khong tai duoc tu server thi thu doc tu database cuc lo.
                    val duLieuCucBo = docSanPhamTuCucBo(tuKhoa, maDanhMuc)
                    if (duLieuCucBo.isNotEmpty()) {
                        return@withContext KetQuaTaiSanPham.ThanhCong(
                            danhSach = catTrangCucBo(duLieuCucBo, trang, gioiHan),
                            phanTrang = phanTrangCucBo(duLieuCucBo.size, trang, gioiHan),
                            tuCache = true
                        )
                    }

                    return@withContext KetQuaTaiSanPham.ThatBai(
                        maLoi = ketQua.maLoi,
                        thongBao = ketQua.thongBao
                    )
                }
            }
        } catch (_: IOException) {
            // Mat mang: doc tu database cuc lo.
            val duLieuCucBo = docSanPhamTuCucBo(tuKhoa, maDanhMuc)
            if (duLieuCucBo.isNotEmpty()) {
                return@withContext KetQuaTaiSanPham.ThanhCong(
                    danhSach = catTrangCucBo(duLieuCucBo, trang, gioiHan),
                    phanTrang = phanTrangCucBo(duLieuCucBo.size, trang, gioiHan),
                    tuCache = true
                )
            }

            KetQuaTaiSanPham.ThatBai(
                maLoi = "LOI_KET_NOI",
                thongBao = boiCanh.getString(R.string.loi_ket_noi)
            )
        }
    }

    /**
     * Cat danh sach cuc lo theo trang dang xem.
     *
     * @param danhSach Toan bo danh sach doc tu database cuc lo.
     * @param trang    Trang dang xem, bat dau tu 1.
     * @param gioiHan  So ban ghi moi trang, null la tra ve toan bo.
     * @return Phan du lieu cua trang dang xem.
     */
    private fun catTrangCucBo(
        danhSach: List<SanPham>,
        trang: Int,
        gioiHan: Int?
    ): List<SanPham> {
        // Khong gioi han so san pham thi tra ve toan bo danh sach.
        if (gioiHan == null || gioiHan <= 0) {
            return danhSach
        }

        val batDau = (trang - 1) * gioiHan
        if (batDau >= danhSach.size) {
            return emptyList()
        }

        return danhSach.subList(batDau, minOf(batDau + gioiHan, danhSach.size))
    }

    /**
     * Tinh phan trang cho du lieu doc tu database cuc lo.
     *
     * @param tongSo   Tong so san pham doc duoc.
     * @param trang    Trang dang xem.
     * @param gioiHan  So ban ghi moi trang, null la khong phan trang.
     * @return Thong tin phan trang.
     */
    private fun phanTrangCucBo(tongSo: Int, trang: Int, gioiHan: Int?): PhanTrang {
        // Khong gioi han so san pham thi coi nhu chi co mot trang.
        if (gioiHan == null || gioiHan <= 0) {
            return PhanTrang(page = 1, limit = tongSo, total = tongSo, totalPages = 1)
        }

        return PhanTrang(
            page = trang,
            limit = gioiHan,
            total = tongSo,
            totalPages = (tongSo + gioiHan - 1) / gioiHan
        )
    }

    /**
     * Tai thong ke ton kho, uu tien du lieu tu server.
     *
     * Khi mat mang thi thong ke duoc tinh tren database cuc bo de nguoi dung
     * van thay duoc so lieu ton kho cua cua hang.
     *
     * @param tuKhoa    Tu khoa tim kiem, null la thong ke toan bo.
     * @param maDanhMuc Id danh muc de loc, null la thong ke toan bo.
     * @return Ket qua chua so lieu thong ke.
     */
    suspend fun taiThongKeTonKho(
        tuKhoa: String? = null,
        maDanhMuc: String? = null
    ): Result<SoLieuThongKeTonKho> = withContext(Dispatchers.IO) {
        runCatching {
            dichVuSanPham.taiThongKeTonKho(tuKhoa, maDanhMuc).getOrElse {
                // Mat mang thi tinh thong ke tren du lieu cuc bo.
                val tongSanPham = csdl.daoSanPham()
                    .demSanPhamTheoBocLoc(maDanhMuc, tuKhoa?.takeIf { it.isNotBlank() })
                val hetHang = csdl.daoSanPham()
                    .demSanPhamHetHang(maDanhMuc, tuKhoa?.takeIf { it.isNotBlank() })
                val sapHet = csdl.daoSanPham()
                    .demSanPhamSapHet(maDanhMuc, tuKhoa?.takeIf { it.isNotBlank() })

                SoLieuThongKeTonKho(
                    totalProducts = tongSanPham,
                    outOfStock = hetHang,
                    lowStock = sapHet,
                    inStock = tongSanPham - hetHang - sapHet
                )
            }
        }
    }

    /**
     * Ghi san pham va don vi ban cua no xuong database cuc lo.
     *
     * @param danhSach Danh sach san pham tu server.
     */
    private suspend fun luuSanPhamVaoCucBo(danhSach: List<SanPham>) {
        val theSanPham = danhSach.map { ChuyenDoiDuLieu.sangTheSanPham(it) }
        csdl.daoSanPham().luuSanPham(theSanPham)

        // Don vi ban luu rieng va thay the cho tung san pham de tranh du lieu cu.
        danhSach.forEach { sanPham ->
            val theDonVi = sanPham.units.map { donVi ->
                ChuyenDoiDuLieu.sangTheDonViSanPham(donVi, sanPham.id)
            }
            csdl.daoDonViSanPham().xoaDonViCuaSanPham(sanPham.id)
            if (theDonVi.isNotEmpty()) {
                csdl.daoDonViSanPham().luuDonViSanPham(theDonVi)
            }
        }
    }

    /**
     * Doc san pham tu database cuc lo.
     *
     * @param tuKhoa    Tu khoa tim kiem, null la lay tat ca.
     * @param maDanhMuc Id danh muc de loc, null la khong loc.
     * @return Danh sach san pham kem don vi ban.
     */
    private suspend fun docSanPhamTuCucBo(
        tuKhoa: String?,
        maDanhMuc: String?
    ): List<SanPham> {
        val daoSanPham = csdl.daoSanPham()
        val theDanhMuc = csdl.daoDanhMuc().layDanhMuc()
        val theDonViTinh = csdl.daoDonViTinh().layDonViTinh()
        val theDonViSanPham = csdl.daoDonViSanPham().layTatCaDonViSanPham()

        // Lay danh sach theo tu khoa truoc, neu khong co tu khoa thi lay toan bo.
        val theSanPham = if (tuKhoa.isNullOrBlank()) {
            daoSanPham.laySanPhamDangHoatDong()
        } else {
            daoSanPham.timSanPham(tuKhoa)
        }

        return theSanPham
            .filter { maDanhMuc == null || it.maDanhMuc == maDanhMuc }
            .map { the ->
                val donViCuaSanPham = theDonViSanPham
                    .filter { it.maSanPham == the.id }
                    .map { theDonVi ->
                        DonViSanPham(
                            id = theDonVi.id,
                            unitId = theDonVi.maDonViTinh,
                            conversionQty = theDonVi.soLuongQuyDoi,
                            price = theDonVi.gia,
                            isDefault = theDonVi.laMacDinh,
                            unit = theDonViTinh
                                .firstOrNull { it.id == theDonVi.maDonViTinh }
                                ?.let { ChuyenDoiDuLieu.sangModelDonViTinh(it) }
                        )
                    }

                ChuyenDoiDuLieu.sangModelSanPham(
                    the = the,
                    danhMuc = theDanhMuc
                        .firstOrNull { it.id == the.maDanhMuc }
                        ?.let { ChuyenDoiDuLieu.sangModelDanhMuc(it) },
                    donViSanPham = donViCuaSanPham
                )
            }
    }

    /**
     * Tai va luu toan bo du lieu danh muc, don vi, khach hang, nha cung cap.
     *
     * Duoc goi khi dang nhap lai hoac khi thay doi tren may khac, de dam bao
     * du lieu cuc bo khong bi cu.
     *
     * @return True neu tai thanh cong.
     */
    suspend fun dongBoDuLieuNen(): Boolean = withContext(Dispatchers.IO) {
        try {
            val ketQua = dichVuSanPham.taiDanhMuc()
            if (ketQua.isSuccess) {
                val danhMuc = ketQua.getOrDefault(emptyList())
                csdl.daoDanhMuc().thayTheDanhMuc(
                    danhMuc.map { ChuyenDoiDuLieu.sangTheDanhMuc(it) }
                )
            }

            val ketQuaDonVi = dichVuSanPham.taiDonVi()
            if (ketQuaDonVi.isSuccess) {
                csdl.daoDonViTinh().thayTheDonViTinh(
                    ketQuaDonVi.getOrDefault(emptyList())
                        .map { ChuyenDoiDuLieu.sangTheDonViTinh(it) }
                )
            }

            val ketQuaKhachHang = dichVuBanHang.taiKhachHang()
            if (ketQuaKhachHang.isSuccess) {
                csdl.daoKhachHang().thayTheKhachHang(
                    ketQuaKhachHang.getOrDefault(emptyList())
                        .map { ChuyenDoiDuLieu.sangTheKhachHang(it) }
                )
            }

            true
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Luu hoa don ban tao offline vao database cuc lo va vao hang doi dong bo.
     *
     * @param yeuCau Yeu cau tao hoa don da chuan hoa.
     * @param soHoaDon Server tra ve so hoa don, null neu chua dong bo.
     * @return Id cua hoa don cuc lo.
     */
    suspend fun luuHoaDonOffline(
        yeuCau: YeuCauTaoHoaDon,
        soHoaDon: String?
    ): String = withContext(Dispatchers.IO) {
        val maHoaDon = UUID.randomUUID().toString()

        val theHoaDon = TheHoaDonBan(
            id = maHoaDon,
            operationId = yeuCau.operationId,
            soHoaDon = soHoaDon,
            maKhachHang = yeuCau.customerId,
            tamTinh = yeuCau.items.sumOf { it.quantity * it.unitPrice },
            giamGia = yeuCau.discount,
            tongCong = yeuCau.items.sumOf { it.quantity * it.unitPrice } - yeuCau.discount,
            phuongThucThanhToan = yeuCau.paymentMethod,
            ghiChu = yeuCau.notes,
            trangThai = if (soHoaDon != null) {
                TrangThaiHoaDon.DA_DONG_BO
            } else {
                TrangThaiHoaDon.CHUA_DONG_BO
            },
            taoLuc = System.currentTimeMillis()
        )

        val dongHang = yeuCau.items.map { dong ->
            TheDongHoaDonBan(
                id = UUID.randomUUID().toString(),
                maHoaDon = maHoaDon,
                maSanPham = dong.productId,
                soLuong = dong.quantity,
                heSoQuyDoi = dong.conversionQty,
                donGia = dong.unitPrice,
                thanhTien = dong.quantity * dong.unitPrice
            )
        }

        csdl.daoHoaDonBan().luuHoaDonKemDongHang(theHoaDon, dongHang)

        // Chi dua vao hang doi khi chua dong bo duoc thi moi can gui lai.
        if (soHoaDon == null) {
            csdl.daoHangDongBo().luuHangDongBo(
                TheHangDongBo(
                    id = UUID.randomUUID().toString(),
                    operationId = yeuCau.operationId,
                    loaiThaoTac = LOAI_THAO_TAC_TAO_HOA_DON,
                    duLieuJson = boPhanTichJson.toJson(yeuCau),
                    trangThai = TRANG_THAI_CHO_GUI,
                    taoLuc = System.currentTimeMillis()
                )
            )

            // Tru ton kho cuc bo ngay de nhan vien khong ban vuot qua so
            // luong thuc te. Ton kho tren server se duoc dong bo lai sau.
            truTonKhoCucLo(yeuCau)
        }

        maHoaDon
    }

    /**
     * Tru ton kho cuc lo theo hoa don vua tao offline.
     *
     * Ton kho luong theo don vi co ban, nen so luong phai nhan voi he so
     * quy doi cua tung don vi ban.
     *
     * @param yeuCau Yeu cau tao hoa don da chuan hoa.
     */
    private suspend fun truTonKhoCucLo(yeuCau: YeuCauTaoHoaDon) {
        val daoSanPham = csdl.daoSanPham()

        yeuCau.items.forEach { dong ->
            val sanPham = daoSanPham.laySanPham(dong.productId) ?: return@forEach
            val soLuongTru = dong.quantity * dong.conversionQty
            val tonKhoMoi = (sanPham.tonKho - soLuongTru).coerceAtLeast(0)

            daoSanPham.capNhatTonKho(sanPham.id, tonKhoMoi)
        }
    }

    /**
     * Doc yeu cau tao hoa don tu phan tu hang doi.
     *
     * @param phanTu Phan tu hang doi.
     * @return Yeu cau tao hoa don, null neu du lieu khong doc duoc.
     */
    fun docYeuCauTuHangDongBo(phanTu: TheHangDongBo): YeuCauTaoHoaDon? {
        return runCatching {
            boPhanTichJson.fromJson(phanTu.duLieuJson, YeuCauTaoHoaDon::class.java)
        }.getOrNull()
    }

    /**
     * Cap nhat trang thai cua hoa don cuc lo.
     *
     * @param operationId  Idempotency key cua hoa don.
     * @param trangThai     Trang thai moi.
     * @param soHoaDonServer So hoa don tu server, null neu khong co.
     * @param thongBaoLoi   Thong bao loi, null neu khong co.
     */
    suspend fun capNhatTrangThaiHoaDon(
        operationId: String,
        trangThai: TrangThaiHoaDon,
        soHoaDonServer: String? = null,
        thongBaoLoi: String? = null
    ) = withContext(Dispatchers.IO) {
        val hoaDon = csdl.daoHoaDonBan().layHoaDonTheoOperationId(operationId) ?: return@withContext
        csdl.daoHoaDonBan().capNhatHoaDon(
            hoaDon.copy(
                trangThai = trangThai,
                soHoaDon = soHoaDonServer ?: hoaDon.soHoaDon,
                soHoaDonServer = soHoaDonServer,
                thongBaoLoi = thongBaoLoi
            )
        )
    }

    /**
     * Dem so hoa don chua dong bo.
     *
     * @return So hoa don dang cho gui len server.
     */
    suspend fun demSoHoaDonChuaDongBo(): Int = withContext(Dispatchers.IO) {
        csdl.daoHoaDonBan().demSoHoaDonChuaDongBo()
    }

    /**
     * Lay dao hang doi dong bo de worker su dung.
     *
     * @return Dao hang doi dong bo.
     */
    fun layDaoHangDongBo(): DaoHangDongBo = csdl.daoHangDongBo()

    companion object {
        /** So san pham toi da tren mot trang khi khong gioi han. */
        const val GIOI_HAN_MAC_DINH = 50

        /** Loai thao tac tao hoa don ban trong hang doi. */
        const val LOAI_THAO_TAC_TAO_HOA_DON = "TAO_HOA_DON_BAN"

        /** Trang thai phan tu hang doi dang cho gui. */
        const val TRANG_THAI_CHO_GUI = "CHO_GUI"

        /** Trang thai phan tu hang doi dang duoc gui. */
        const val TRANG_THAI_DANG_GUI = "DANG_GUI"

        /** Trang thai phan tu hang doi gap xung dot ton kho. */
        const val TRANG_THAI_XUNG_DOT = "XUNG_DOT"

        /** Trang thai phan tu hang doi that bai. */
        const val TRANG_THAI_THAT_BAI = "THAT_BAI"
    }
}

/**
 * Ket qua tai san pham, kem thong tin du lieu co tu database cuc lo hay khong.
 */
sealed interface KetQuaTaiSanPham {
    /**
     * Tai thanh cong.
     *
     * @param danhSach Danh sach san pham.
     * @param tuCache   True neu du lieu duoc doc tu database cuc lo do mat mang.
     */
    data class ThanhCong(
        val danhSach: List<SanPham>,
        val phanTrang: PhanTrang = PhanTrang(),
        val tuCache: Boolean = false
    ) : KetQuaTaiSanPham

    /**
     * Tai that bai.
     *
     * @param maLoi    Ma loi chuan tu server.
     * @param thongBao Thong bao cho nguoi dung.
     */
    data class ThatBai(
        val maLoi: String,
        val thongBao: String
    ) : KetQuaTaiSanPham
}
