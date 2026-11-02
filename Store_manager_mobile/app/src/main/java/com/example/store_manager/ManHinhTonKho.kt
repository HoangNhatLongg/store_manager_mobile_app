package com.example.store_manager

import android.content.Intent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.store_manager.du_lieu.BaoCaoCsv
import com.example.store_manager.du_lieu.DanhMuc
import com.example.store_manager.du_lieu.DichVuSanPham
import com.example.store_manager.du_lieu.KetQuaTaiSanPham
import com.example.store_manager.du_lieu.KhoDuLieuSanPham
import com.example.store_manager.du_lieu.PhanTrang
import com.example.store_manager.du_lieu.SanPham
import com.example.store_manager.du_lieu.TrangThaiTonKho
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Man hinh ton kho, bam sat giao dien web `/inventory`.
 *
 * Man hinh gom bon the thong ke, o tim kiem, bo loc danh muc, bo loc trang thai
 * ton kho va danh sach san pham co phan trang.
 */
class ManHinhTonKho : ManHinhCoThanhTab() {
    override val taiNguyenNoiDung: Int = R.layout.ton_kho

    private lateinit var danhSachTonKho: RecyclerView
    private lateinit var oTimKiem: com.google.android.material.textfield.TextInputEditText
    private lateinit var boLocDanhMuc: Spinner
    private lateinit var giaTriTongSanPham: TextView
    private lateinit var giaTriSapHet: TextView
    private lateinit var giaTriHetHang: TextView
    private lateinit var giaTriConHang: TextView
    private lateinit var soLuongHienThi: TextView
    private lateinit var thongBaoLoi: TextView
    private lateinit var thongBaoRong: TextView
    private lateinit var vongTai: ProgressBar
    private lateinit var nutLamMoi: MaterialButton
    private lateinit var nutXuatBaoCao: MaterialButton
    private lateinit var nutLocTatCa: MaterialButton
    private lateinit var nutLocConHang: MaterialButton
    private lateinit var nutLocSapHet: MaterialButton
    private lateinit var nutLocHetHang: MaterialButton
    private lateinit var khuVucPhanTrang: View
    private lateinit var moTaPhanTrang: TextView
    private lateinit var viTriTrang: TextView
    private lateinit var nutTrangTruoc: MaterialButton
    private lateinit var nutTrangSau: MaterialButton

    private lateinit var trinhBayTongKho: TrinhBayTongKho
    private lateinit var dichVuSanPham: DichVuSanPham
    private lateinit var khoDuLieu: KhoDuLieuSanPham

    /** Danh sach danh muc dung cho bo loc. */
    private var danhSachDanhMuc = listOf<DanhMuc>()

    /** Danh sach san pham dang hien thi sau khi loc. */
    private var danhSachHienThi = emptyList<SanPham>()

    /** Trang dang xem. */
    private var trangHienTai = 1

    /** Tong so trang co the xem. */
    private var tongTrang = 1

    /** So san pham tren mot trang. */
    private val soSanPhamMoiTrang = 20

    /** Trang thai loc hien tai, null la khong loc theo trang thai. */
    private var trangThaiDangLoc: TrangThaiTonKho? = null

    /** Cong viec debounce kiem trao tu khoa, dung de huy khi nguoi dung go nhanh. */
    private var congViecTimKiem: Job? = null

    /** Co dang tai du lieu hay khong, dung de tranh tai trung lap. */
    private var dangTai = false

    override fun onStart() {
        super.onStart()

        // Base Activity vua nap layout, vi vay phai anh xa va tai du lieu sau do.
        khoiTaoThanhPhanGiaoDien()
        dichVuSanPham = DichVuSanPham(applicationContext)
        khoDuLieu = KhoDuLieuSanPham(applicationContext)
        khoiTaoDanhSach()
        ganSuKienTimKiemVaBoLoc()

        taiDanhMuc()
        taiDanhSachTonKho()
    }

    /** Anh xa cac id trong XML vao bien co ten de hieu. */
    private fun khoiTaoThanhPhanGiaoDien() {
        danhSachTonKho = findViewById(R.id.danh_sach_tong_kho)
        oTimKiem = findViewById(R.id.o_tim_kiem_ton_kho)
        boLocDanhMuc = findViewById(R.id.bo_loc_danh_muc_ton_kho)
        giaTriTongSanPham = findViewById(R.id.gia_tri_tong_san_pham_ton_kho)
        giaTriSapHet = findViewById(R.id.gia_tri_sap_het_ton_kho)
        giaTriHetHang = findViewById(R.id.gia_tri_het_hang_ton_kho)
        giaTriConHang = findViewById(R.id.gia_tri_con_hang_ton_kho)
        soLuongHienThi = findViewById(R.id.so_luong_ton_kho_hien_thi)
        thongBaoLoi = findViewById(R.id.thong_bao_loi_ton_kho)
        thongBaoRong = findViewById(R.id.thong_bao_rong_ton_kho)
        vongTai = findViewById(R.id.vong_tai_ton_kho)
        nutLamMoi = findViewById(R.id.nut_lam_moi_ton_kho)
        nutXuatBaoCao = findViewById(R.id.nut_xuat_bao_cao_ton_kho)
        nutLocTatCa = findViewById(R.id.nut_loc_tat_ca_ton_kho)
        nutLocConHang = findViewById(R.id.nut_loc_con_hang_ton_kho)
        nutLocSapHet = findViewById(R.id.nut_loc_sap_het_ton_kho)
        nutLocHetHang = findViewById(R.id.nut_loc_het_hang_ton_kho)
        khuVucPhanTrang = findViewById(R.id.khu_vuc_phan_trang_ton_kho)
        moTaPhanTrang = findViewById(R.id.mo_ta_phan_trang_ton_kho)
        viTriTrang = findViewById(R.id.vi_tri_trang_ton_kho)
        nutTrangTruoc = findViewById(R.id.nut_trang_truoc_ton_kho)
        nutTrangSau = findViewById(R.id.nut_trang_sau_ton_kho)
    }

    /** Tao adapter cho danh sach san pham. */
    private fun khoiTaoDanhSach() {
        trinhBayTongKho = TrinhBayTongKho { sanPham ->
            moChiTietSanPham(sanPham)
        }
        danhSachTonKho.layoutManager = LinearLayoutManager(this)
        danhSachTonKho.adapter = trinhBayTongKho

        nutLamMoi.setOnClickListener {
            datLaiBoLoc()
            taiDanhSachTonKho()
        }

        nutXuatBaoCao.setOnClickListener {
            xuatBaoCaoTonKho()
        }

        // Cac nut loc trang thai chi doi trang thai loc hien tai.
        nutLocTatCa.setOnClickListener { doiTrangThaiLoc(null) }
        nutLocConHang.setOnClickListener { doiTrangThaiLoc(TrangThaiTonKho.CON_HANG) }
        nutLocSapHet.setOnClickListener { doiTrangThaiLoc(TrangThaiTonKho.SAP_HET) }
        nutLocHetHang.setOnClickListener { doiTrangThaiLoc(TrangThaiTonKho.HET_HANG) }

        // Nut phan trang chuyen trang.
        nutTrangTruoc.setOnClickListener {
            if (trangHienTai > 1) {
                trangHienTai--
                taiDanhSachTonKho()
            }
        }
        nutTrangSau.setOnClickListener {
            if (trangHienTai < tongTrang) {
                trangHienTai++
                taiDanhSachTonKho()
            }
        }

        capNhatMauNutLoc()
    }

    /** Gan su kien tim kiem co debounce va bo loc danh muc. */
    private fun ganSuKienTimKiemVaBoLoc() {
        // Debounce 350ms de khong goi API lien tuc khi nguoi dung go.
        oTimKiem.doAfterTextChanged { vanBan ->
            congViecTimKiem?.cancel()
            congViecTimKiem = lifecycleScope.launch {
                delay(350)
                trangHienTai = 1
                taiDanhSachTonKho(vanBan?.toString())
            }
        }
    }

    /** Tai danh muc va dua ten danh muc vao Spinner. */
    private fun taiDanhMuc() {
        lifecycleScope.launch {
            val ketQua = dichVuSanPham.taiDanhMuc()
            danhSachDanhMuc = ketQua.getOrDefault(emptyList())

            val tenDanhMuc = mutableListOf(getString(R.string.tat_ca_danh_muc))
            tenDanhMuc.addAll(danhSachDanhMuc.map { it.name })

            boLocDanhMuc.adapter = ArrayAdapter(
                this@ManHinhTonKho,
                android.R.layout.simple_spinner_dropdown_item,
                tenDanhMuc
            )

            boLocDanhMuc.onItemSelectedListener =
                object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: android.widget.AdapterView<*>?,
                        view: View?,
                        viTri: Int,
                        id: Long
                    ) {
                        trangHienTai = 1
                        taiDanhSachTonKho()
                    }

                    override fun onNothingSelected(
                        parent: android.widget.AdapterView<*>?
                    ) = Unit
                }
        }
    }

    /**
     * Tai danh sach ton kho tu server.
     *
     * @param tuKhoa Tu khoa tim kiem, null la tai tat ca.
     */
    private fun taiDanhSachTonKho(tuKhoa: String? = null) {
        if (dangTai) {
            return
        }

        dangTai = true
        capNhatTrangThaiTai(true)
        xoaThongBaoLoi()

        lifecycleScope.launch {
            val viTriDanhMuc = boLocDanhMuc.selectedItemPosition
            val danhMucDangChon = danhSachDanhMuc.getOrNull(viTriDanhMuc - 1)

            // Dung kho du lieu de uu tien server, khi mat mang thi doc tu
            // database cuc lo nhung nhan vien van theo doi duoc ton kho.
            when (
                val ketQua = khoDuLieu.taiSanPham(
                    tuKhoa = tuKhoa?.takeIf { it.isNotBlank() },
                    maDanhMuc = danhMucDangChon?.id,
                    trang = trangHienTai,
                    gioiHan = soSanPhamMoiTrang
                )
            ) {
                is KetQuaTaiSanPham.ThanhCong -> {
                    tongTrang = ketQua.phanTrang.totalPages.coerceAtLeast(1)
                    danhSachHienThi = locTheoTrangThai(ketQua.danhSach)
                    hienThiDanhSach(ketQua.phanTrang)
                    taiThongKeTonKho(tuKhoa, danhMucDangChon?.id)

                    // Hien thi canh bao khi du lieu duoc doc tu database cuc lo.
                    if (ketQua.tuCache) {
                        Toast.makeText(
                            this@ManHinhTonKho,
                            getString(R.string.thong_bao_dang_dung_du_lieu_cuc_bo),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }

                is KetQuaTaiSanPham.ThatBai -> {
                    // Loi 401 thi phai dang nhap lai, cac loi khac chi hien
                    // thong bao va giu nguyen danh sach cu.
                    if (ketQua.maLoi == "UNAUTHORIZED" || ketQua.maLoi == "HTTP_401") {
                        hienThiLoi(ketQua.thongBao)
                        chuyenSangDangNhap()
                        return@launch
                    }

                    hienThiLoi(ketQua.thongBao)
                }
            }

            dangTai = false
            capNhatTrangThaiTai(false)
        }
    }

    /**
     * Tai va hien thi so lieu thong ke ton kho.
     *
     * Thong ke duoc dem o server nen luon phan anh toan bo cua hang, khong
     * phu thuoc trang dang xem. Neu tai that bai thi giu nguyen so lieu cu
     * de nguoi dung van thay duoc thong tin.
     *
     * @param tuKhoa    Tu khoa tim kiem dang ap dung.
     * @param maDanhMuc Id danh muc dang ap dung.
     */
    private fun taiThongKeTonKho(tuKhoa: String?, maDanhMuc: String?) {
        lifecycleScope.launch {
            val ketQua = khoDuLieu.taiThongKeTonKho(tuKhoa, maDanhMuc)

            ketQua.onSuccess { soLieu ->
                giaTriTongSanPham.text = soLieu.totalProducts.toString()
                giaTriHetHang.text = soLieu.outOfStock.toString()
                giaTriSapHet.text = soLieu.lowStock.toString()
                giaTriConHang.text = soLieu.inStock.toString()
            }
        }
    }

    /**
     * Loc danh sach san pham theo trang thai ton kho dang chon.
     *
     * @param danhSach Danh sach san pham tu server.
     * @return Danh sach da loc.
     */
    private fun locTheoTrangThai(danhSach: List<SanPham>): List<SanPham> {
        val trangThai = trangThaiDangLoc ?: return danhSach
        return danhSach.filter { TrangThaiTonKho.cuaSanPham(it) == trangThai }
    }

    /** Hien thi danh sach san pham cung cac the thong ke. */
    private fun hienThiDanhSach(phanTrang: PhanTrang) {
        trinhBayTongKho.capNhatDanhSach(danhSachHienThi)

        // Danh sach rong thi hien thong bao thay vi con danh sach trong.
        val coDuLieu = danhSachHienThi.isNotEmpty()
        danhSachTonKho.visibility = if (coDuLieu) View.VISIBLE else View.GONE
        thongBaoRong.visibility = if (coDuLieu) View.GONE else View.VISIBLE

        soLuongHienThi.text = getString(
            R.string.tong_so_san_pham_phan_trang,
            danhSachHienThi.size
        )

        capNhatPhanTrang(phanTrang)
    }

    /** Cap nhat khu vuc phan trang va trang thai hai nut chuyen trang. */
    private fun capNhatPhanTrang(phanTrang: PhanTrang) {
        // Khu vuc phan trang chi hien khi co nhieu hon mot trang.
        if (tongTrang <= 1) {
            khuVucPhanTrang.visibility = View.GONE
            return
        }

        khuVucPhanTrang.visibility = View.VISIBLE

        val batDau = (trangHienTai - 1) * soSanPhamMoiTrang + 1
        val ketThuc = minOf(trangHienTai * soSanPhamMoiTrang, phanTrang.total)
        moTaPhanTrang.text = getString(
            R.string.hien_thi_tu_den_trong,
            batDau,
            ketThuc,
            phanTrang.total
        )

        viTriTrang.text = getString(R.string.trang_san_pham, trangHienTai, tongTrang)
        nutTrangTruoc.isEnabled = trangHienTai > 1
        nutTrangSau.isEnabled = trangHienTai < tongTrang
    }

    /** Doi trang thai loc va tai lai danh sach. */
    private fun doiTrangThaiLoc(trangThai: TrangThaiTonKho?) {
        if (trangThaiDangLoc == trangThai) {
            return
        }

        trangThaiDangLoc = trangThai
        capNhatMauNutLoc()
        trangHienTai = 1
        taiDanhSachTonKho()
    }

    /**
     * Cap nhat mau cua cac nut loc trang thai.
     *
     * Nut dang duoc chon nen nen xanh va chu dam, nut khong chon nen trong.
     */
    private fun capNhatMauNutLoc() {
        val danhSachNut = listOf(
            nutLocTatCa to null,
            nutLocConHang to TrangThaiTonKho.CON_HANG,
            nutLocSapHet to TrangThaiTonKho.SAP_HET,
            nutLocHetHang to TrangThaiTonKho.HET_HANG
        )

        danhSachNut.forEach { (nut, trangThai) ->
            val dangChon = trangThai == trangThaiDangLoc
            nut.setBackgroundResource(
                if (dangChon) R.drawable.nen_tab_duoc_chon else R.drawable.nen_tab_binh_thuong
            )
            nut.setTextColor(
                ContextCompat.getColor(
                    this,
                    if (dangChon) R.color.xanh_dam else R.color.xam_phu
                )
            )
        }
    }

    /** Dat lai toan bo bo loc ve trang thai ban dau. */
    private fun datLaiBoLoc() {
        oTimKiem.setText("")
        trangThaiDangLoc = null
        trangHienTai = 1
        capNhatMauNutLoc()
    }

    /**
     * Xuat bao cao ton kho ra file CSV va chia se cho nguoi dung.
     *
     * Chi duoc xuat khi co du lieu, neu khong thi bao nguoi dung.
     */
    private fun xuatBaoCaoTonKho() {
        if (danhSachHienThi.isEmpty()) {
            Toast.makeText(
                this,
                getString(R.string.loi_khong_co_du_lieu_de_xuat),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        try {
            val noiDung = taoNoiDungCsv(danhSachHienThi)
            val baoCao = BaoCaoCsv(applicationContext)
            val file = baoCao.taoFileCsv(tenFile(), noiDung)

            val yDinhChiaSe = baoCao.taoIntentChiaSe(
                file = file,
                tieuDe = getString(R.string.tieu_de_bao_cao_ton_kho),
                loaiMime = LOAI_MIME_CSV
            )

            startActivity(Intent.createChooser(yDinhChiaSe, getString(R.string.xuat_bao_cao)))
        } catch (loi: Exception) {
            Toast.makeText(
                this,
                getString(R.string.loi_xuat_bao_cao, loi.message ?: ""),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Sinh noi dung CSV tu danh sach san pham.
     *
     * @param danhSach Danh sach san pham can xuat.
     * @return Chuoi noi dung CSV.
     */
    private fun taoNoiDungCsv(danhSach: List<SanPham>): String {
        val dongTieuDe = listOf(
            "SKU",
            "Ten san pham",
            "Danh muc",
            "Gia nhap",
            "Gia ban",
            "Don vi ban",
            "Ton kho",
            "Ton kho toi da",
            "Trang thai"
        ).joinToString(",")

        val dongDuLieu = danhSach.map { sanPham ->
            listOf(
                sanPham.sku,
                sanPham.name,
                sanPham.category?.name.orEmpty(),
                sanPham.importPrice.toString(),
                sanPham.sellPrice.toString(),
                sanPham.units.firstOrNull { it.isDefault }?.unit?.layTenHienThi().orEmpty(),
                sanPham.stock.toString(),
                sanPham.minStock.toString(),
                when (TrangThaiTonKho.cuaSanPham(sanPham)) {
                    TrangThaiTonKho.HET_HANG -> getString(R.string.het_hang)
                    TrangThaiTonKho.SAP_HET -> getString(R.string.sap_het_hang)
                    TrangThaiTonKho.CON_HANG -> getString(R.string.con_hang)
                }
            ).joinToString(",") { giaTri -> "\"${giaTri.replace("\"", "\"\"")}\"" }
        }

        return (listOf(dongTieuDe) + dongDuLieu).joinToString("\n")
    }

    /**
     * Sinh ten file bao cao theo thoi gian hien tai.
     *
     * @return Ten file khong chua duoi file.
     */
    private fun tenFile(): String {
        val thoiGian = java.text.SimpleDateFormat(
            "yyyyMMdd_HHmmss",
            java.util.Locale("vi", "VN")
        ).format(java.util.Date())
        return "bao_cao_ton_kho_$thoiGian"
    }

    /**
     * Mo man hinh chi tiet san pham.
     *
     * @param sanPham San pham duoc chon.
     */
    private fun moChiTietSanPham(sanPham: SanPham) {
        // Man hinh chi tiet san pham hien chua duoc tach rieng, nen tam thoi
        // dua nguoi dung sang man hinh quan ly san pham de tiếp tục thao tac.
        moManHinh(ManHinhSanPham::class.java)
    }

    /** Chuyen sang man hinh dang nhap khi token khong con hieu luc. */
    private fun chuyenSangDangNhap() {
        startActivity(
            Intent(this, DangNhap::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        )
        finish()
    }

    /** Bat hoac tat vong tai va khoa nut khi dang tai du lieu. */
    private fun capNhatTrangThaiTai(dangTai: Boolean) {
        vongTai.visibility = if (dangTai) View.VISIBLE else View.GONE
        nutLamMoi.isEnabled = !dangTai
        nutXuatBaoCao.isEnabled = !dangTai
    }

    /** Hien thong bao loi len man hinh. */
    private fun hienThiLoi(thongBao: String) {
        thongBaoLoi.text = thongBao
        thongBaoLoi.visibility = View.VISIBLE
    }

    /** An thong bao loi cu truoc moi lan tai. */
    private fun xoaThongBaoLoi() {
        thongBaoLoi.text = ""
        thongBaoLoi.visibility = View.GONE
    }

    companion object {
        /** Kieu MIME cua file CSV khi chia se. */
        const val LOAI_MIME_CSV = "text/csv"
    }
}
