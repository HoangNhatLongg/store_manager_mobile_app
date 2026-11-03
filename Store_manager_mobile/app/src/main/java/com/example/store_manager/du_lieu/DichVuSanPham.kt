package com.example.store_manager.du_lieu

import android.content.Context
import com.example.store_manager.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

/** Lop xu ly tai san pham tu mobile API. */
class DichVuSanPham(boiCanh: Context) {
    private val boiCanh = boiCanh.applicationContext
    private val api = BoTaoApi.taoApi(this.boiCanh)

    /**
     * Tai danh sach san pham theo tu khoa.
     *
     * @param tuKhoa        Tu khoa tim kiem, null la tai tat ca.
     * @param maDanhMuc     Id danh muc de loc, null la khong loc.
     * @param trang         So trang can tai, bat dau tu 1.
     * @param baoGomSanPhamAn True neu muon lay ca san pham dang an.
     * @param gioiHan       So san pham toi da tren mot trang.
     */
    suspend fun taiDanhSach(
        tuKhoa: String? = null,
        maDanhMuc: String? = null,
        trang: Int = 1,
        baoGomSanPhamAn: Boolean = false,
        gioiHan: Int = 50
    ): KetQuaSanPham =
        withContext(Dispatchers.IO) {
            try {
                val phanHoi = api.layDanhSachSanPham(
                    tuKhoa = tuKhoa?.takeIf { it.isNotBlank() },
                    maDanhMuc = maDanhMuc,
                    trang = trang,
                    gioiHan = gioiHan,
                    baoGomSanPhamAn = baoGomSanPhamAn
                )

                if (phanHoi.isSuccessful) {
                    return@withContext KetQuaSanPham.ThanhCong(
                        danhSach = phanHoi.body()?.data.orEmpty(),
                        phanTrang = phanHoi.body()?.pagination ?: PhanTrang()
                    )
                }

                KetQuaSanPham.ThatBai(
                    maLoi = PhanTichLoi.layMaLoi(phanHoi),
                    thongBao = boiCanh.getString(R.string.khong_the_tai_du_lieu)
                )
            } catch (_: IOException) {
                KetQuaSanPham.ThatBai(
                    maLoi = "LOI_KET_NOI",
                    thongBao = boiCanh.getString(R.string.loi_ket_noi)
                )
            } catch (_: Exception) {
                KetQuaSanPham.ThatBai(
                    maLoi = "LOI_KHONG_XAC_DINH",
                    thongBao = boiCanh.getString(R.string.khong_the_tai_du_lieu)
                )
            }
        }

    /**
     * Tai toan bo ton kho cua cac san pham dang co trong gio hang.
     *
     * API san pham co phan trang, nen phai duyet het cac trang de lay ton kho
     * cua tat ca san pham dang nam trong gio hang. Neu chi tai trang dau tien
     * thi san pham nam o cac trang sau se bi coi nhu het hang.
     *
     * Khi mat mang, ham nay tra ve map rong thay vi nem loi. Nho vay nguoi
     * dung van duoc sang man hinh thanh toan va tao hoa don offline.
     *
     * @param maSanPham Danh sach id san pham can lay ton kho.
     * @return Map id san pham va so ton kho tuong ung, rong neu khong tai duoc.
     */
    suspend fun taiTonKhoTheoIdSanPham(maSanPham: List<String>): Map<String, Int> =
        withContext(Dispatchers.IO) {
            if (maSanPham.isEmpty()) {
                return@withContext emptyMap()
            }

            val tonKhoTheoId = mutableMapOf<String, Int>()
            var trang = 1
            var tongTrang = 1

            // Duyet lap lai cac trang cho den khi lay duoc het san pham can tim.
            while (trang <= tongTrang) {
                // Loi ket noi khong duoc nem ra ngoai, vi man hinh ban hang se
                // bi dong va nguoi dung khong the thanh toan offline.
                val phanHoi = try {
                    api.layDanhSachSanPham(trang = trang, gioiHan = 50)
                } catch (loiHuy: kotlinx.coroutines.CancellationException) {
                    // Man hinh bi dong giua chung, khong phai loi ket noi.
                    throw loiHuy
                } catch (_: IOException) {
                    // Mat mang: dung vong lap, tra ve ton kho tai duoc den gio.
                    break
                }

                if (!phanHoi.isSuccessful) {
                    break
                }

                val thanhHoi = phanHoi.body() ?: break
                thanhHoi.data.forEach { sanPham ->
                    if (maSanPham.contains(sanPham.id)) {
                        tonKhoTheoId[sanPham.id] = sanPham.stock
                    }
                }

                tongTrang = thanhHoi.pagination.totalPages
                trang++
            }

            tonKhoTheoId
        }

    /** Tai danh muc tu backend de hien trong Spinner bo loc. */
    suspend fun taiDanhMuc(): Result<List<DanhMuc>> = withContext(Dispatchers.IO) {
        runCatching {
            val phanHoi = api.layDanhSachDanhMuc()
            check(phanHoi.isSuccessful) { "HTTP_${phanHoi.code()}" }
            phanHoi.body()?.data.orEmpty()
        }
    }

    /**
     * Tai thong ke ton kho cua toan bo san pham.
     *
     * So lieu duoc dem tren server nen khong phu thuoc phan trang, dam bao
     * cac the so lieu o dau man hinh ton kho luon dung so voi thuc te.
     *
     * @param tuKhoa    Tu khoa tim kiem, null la thong ke toan bo.
     * @param maDanhMuc Id danh muc de loc, null la thong ke toan bo.
     * @return Ket qua chua so lieu thong ke hoac loi theo ma loi chuan.
     */
    suspend fun taiThongKeTonKho(
        tuKhoa: String? = null,
        maDanhMuc: String? = null
    ): Result<SoLieuThongKeTonKho> = withContext(Dispatchers.IO) {
        runCatching {
            val phanHoi = api.layThongKeTonKho(
                tuKhoa = tuKhoa?.takeIf { it.isNotBlank() },
                maDanhMuc = maDanhMuc
            )
            check(phanHoi.isSuccessful) { PhanTichLoi.layMaLoi(phanHoi) }
            phanHoi.body()?.data ?: SoLieuThongKeTonKho()
        }
    }

    /**
     * Tao san pham moi tren server.
     *
     * @param duLieu Thong tin san pham can tao.
     * @return Ket qua chua san pham vua tao hoac loi theo ma loi chuan.
     */
    suspend fun taoSanPham(duLieu: DuLieuSanPham): Result<SanPham> = withContext(Dispatchers.IO) {
        runCatching {
            val phanHoi = api.taoSanPham(duLieu)
            if (!phanHoi.isSuccessful) throw loiApi(phanHoi)
            requireNotNull(phanHoi.body()?.data)
        }
    }

    /**
     * Cap nhat san pham tren server.
     *
     * @param maSanPham  Id cua san pham can sua.
     * @param duLieu     Thong tin moi cua san pham.
     * @return Ket qua chua san pham vua cap nhat hoac loi theo ma loi chuan.
     */
    suspend fun suaSanPham(maSanPham: String, duLieu: DuLieuSanPham): Result<SanPham> =
        withContext(Dispatchers.IO) {
            runCatching {
                val phanHoi = api.suaSanPham(maSanPham, duLieu)
                if (!phanHoi.isSuccessful) throw loiApi(phanHoi)
                requireNotNull(phanHoi.body()?.data)
            }
        }

    /**
     * Xoa san pham tren server.
     *
     * Loi duoc nem ra voi ma loi chuan de Activity quyet dinh hien thi thong bao,
     * vi du "PRODUCT_HAS_INVOICE_HISTORY" khi san pham da co trong hoa don.
     *
     * @param maSanPham Id cua san pham can xoa.
     * @return Ket qua rong neu xoa thanh cong.
     */
    suspend fun xoaSanPham(maSanPham: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val phanHoi = api.xoaSanPham(maSanPham)
            check(phanHoi.isSuccessful) { PhanTichLoi.layMaLoi(phanHoi) }
        }
    }

    /**
     * An hoac hien san pham tren man hinh ban hang.
     *
     * @param maSanPham Id cua san pham.
     * @param dangHien  True neu hien, false neu an.
     * @return Ket qua rong neu doi trang thai thanh cong.
     */
    suspend fun doiTrangThaiSanPham(maSanPham: String, dangHien: Boolean): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val phanHoi = api.doiTrangThaiSanPham(maSanPham, mapOf("isActive" to dangHien))
                check(phanHoi.isSuccessful) { PhanTichLoi.layMaLoi(phanHoi) }
            }
        }

    suspend fun taiDonVi(): Result<List<DonViTinh>> = withContext(Dispatchers.IO) {
        runCatching {
            val phanHoi = api.layDanhSachDonVi()
            check(phanHoi.isSuccessful) { "HTTP_${phanHoi.code()}" }
            phanHoi.body()?.data.orEmpty()
        }
    }

    suspend fun taiDonViSanPham(maSanPham: String): Result<List<DonViSanPham>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val phanHoi = api.layDonViSanPham(maSanPham)
                check(phanHoi.isSuccessful) { "HTTP_${phanHoi.code()}" }
                phanHoi.body()?.data.orEmpty()
            }
        }

    suspend fun themDonViSanPham(yeuCau: YeuCauDonViSanPham): Result<DonViSanPham> =
        withContext(Dispatchers.IO) {
            runCatching {
                val phanHoi = api.themDonViSanPham(yeuCau)
                if (!phanHoi.isSuccessful) throw loiApi(phanHoi)
                requireNotNull(phanHoi.body())
            }
        }

    suspend fun suaDonViSanPham(
        maDonVi: String,
        yeuCau: YeuCauDonViSanPham
    ): Result<DonViSanPham> = withContext(Dispatchers.IO) {
        runCatching {
            val phanHoi = api.suaDonViSanPham(maDonVi, yeuCau)
            if (!phanHoi.isSuccessful) throw loiApi(phanHoi)
            requireNotNull(phanHoi.body())
        }
    }

    suspend fun xoaDonViSanPham(maDonVi: String): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            val phanHoi = api.xoaDonViSanPham(maDonVi)
            if (!phanHoi.isSuccessful) throw loiApi(phanHoi)
        }
    }

    /**
     * Chuyen response loi cua server thanh exception giu ma loi va thong bao.
     *
     * @param phanHoi Response loi tu Retrofit.
     * @return Exception de nem ra cho Activity hien thi.
     */
    private fun loiApi(phanHoi: retrofit2.Response<*>): LoiApiException {
        val loi = PhanTichLoi.layLoi(phanHoi)
        return LoiApiException(loi.maLoi, loi.thongBao)
    }
}
