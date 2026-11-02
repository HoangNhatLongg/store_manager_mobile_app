package com.example.store_manager

import android.content.Intent
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.store_manager.du_lieu.DanhMuc
import com.example.store_manager.du_lieu.DichVuSanPham
import com.example.store_manager.du_lieu.DinhDangTien
import com.example.store_manager.du_lieu.DuLieuSanPham
import com.example.store_manager.du_lieu.KetQuaSanPham
import com.example.store_manager.du_lieu.SanPham
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Man hinh quan ly san pham, chuyen cac thao tac chinh tu giao dien web sang mobile.
 *
 * Giao dien bo cuc theo trang web `/products`: tieu de, bo loc, danh sach
 * va phan trang. Bang duoc doi thanh card de hien thi tot tren dien thoai.
 */
class ManHinhSanPham : ManHinhCoThanhTab() {
    override val taiNguyenNoiDung: Int = R.layout.san_pham

    private lateinit var oTimKiem: TextInputEditText
    private lateinit var boLocDanhMuc: Spinner
    private lateinit var danhSachSanPham: RecyclerView
    private lateinit var vongTaiSanPham: ProgressBar
    private lateinit var thongBaoSanPham: TextView
    private lateinit var thongBaoRongSanPham: TextView
    private lateinit var tongSoSanPham: TextView
    private lateinit var nutThemSanPham: MaterialButton
    private lateinit var nutTrangTruoc: MaterialButton
    private lateinit var nutTrangSau: MaterialButton
    private lateinit var khuVucPhanTrang: View
    private lateinit var thongTinPhanTrang: TextView
    private lateinit var boLocSanPhamAn: com.google.android.material.checkbox.MaterialCheckBox
    private lateinit var trinhBaySanPham: TrinhBaySanPham
    private lateinit var dichVuSanPham: DichVuSanPham
    private var danhSachDanhMuc = listOf<DanhMuc>()
    private var congViecTimKiem: Job? = null
    private var trangHienTai = 1
    private var tongSoTrang = 1
    private var tongSoSanPhamToanBo = 0
    private var baoGomSanPhamAn = false
    private var dangTai = false

    override fun onStart() {
        super.onStart()
        khoiTaoThanhPhanGiaoDien()
        dichVuSanPham = DichVuSanPham(applicationContext)
        trinhBaySanPham = TrinhBaySanPham(
            khiSuaSanPham = { moHopThoaiSanPham(it) },
            khiXoaSanPham = { xacNhanXoaSanPham(it) },
            khiMoDonVi = { moManHinhDonVi(it) },
            khiDoiAnHien = { doiAnHienSanPham(it) }
        )
        danhSachSanPham.layoutManager = LinearLayoutManager(this)
        danhSachSanPham.adapter = trinhBaySanPham
        ganSuKienTimKiem()
        nutThemSanPham.setOnClickListener { moHopThoaiSanPham(null) }
        nutTrangTruoc.setOnClickListener {
            if (trangHienTai > 1) {
                trangHienTai--
                taiDanhSachSanPham(oTimKiem.text?.toString())
            }
        }
        nutTrangSau.setOnClickListener {
            if (trangHienTai < tongSoTrang) {
                trangHienTai++
                taiDanhSachSanPham(oTimKiem.text?.toString())
            }
        }
        boLocSanPhamAn.setOnCheckedChangeListener { _, checked ->
            baoGomSanPhamAn = checked
            trangHienTai = 1
            taiDanhSachSanPham(oTimKiem.text?.toString())
        }
        taiDanhMuc()
        taiDanhSachSanPham()
    }

    /** Anh xa cac id trong XML vao bien co ten de hieu. */
    private fun khoiTaoThanhPhanGiaoDien() {
        oTimKiem = findViewById(R.id.o_tim_kiem)
        boLocDanhMuc = findViewById(R.id.bo_loc_danh_muc_san_pham)
        danhSachSanPham = findViewById(R.id.danh_sach_san_pham)
        vongTaiSanPham = findViewById(R.id.vong_tai_san_pham)
        thongBaoSanPham = findViewById(R.id.thong_bao_san_pham)
        thongBaoRongSanPham = findViewById(R.id.thong_bao_rong_san_pham)
        tongSoSanPham = findViewById(R.id.tong_so_san_pham)
        nutThemSanPham = findViewById(R.id.nut_them_san_pham)
        nutTrangTruoc = findViewById(R.id.nut_trang_truoc)
        nutTrangSau = findViewById(R.id.nut_trang_sau)
        khuVucPhanTrang = findViewById(R.id.khu_vuc_phan_trang)
        thongTinPhanTrang = findViewById(R.id.thong_tin_phan_trang)
        boLocSanPhamAn = findViewById(R.id.bo_loc_san_pham_an)
    }

    /**
     * Gan su kien tim kiem, chi tai lai sau khi nguoi dung ngung go.
     *
     * @param vanBan Noi dung dang nhap.
     */
    private fun ganSuKienTimKiem() {
        oTimKiem.doAfterTextChanged { vanBan ->
            congViecTimKiem?.cancel()
            congViecTimKiem = lifecycleScope.launch {
                delay(350)
                trangHienTai = 1
                taiDanhSachSanPham(vanBan?.toString())
            }
        }
    }

    /** Tai danh muc tu server va dua vao Spinner bo loc. */
    private fun taiDanhMuc() {
        lifecycleScope.launch {
            danhSachDanhMuc = dichVuSanPham.taiDanhMuc().getOrDefault(emptyList())
            val tenDanhMuc = mutableListOf(getString(R.string.tat_ca_danh_muc))
            tenDanhMuc.addAll(danhSachDanhMuc.map { it.name })
            boLocDanhMuc.adapter = ArrayAdapter(
                this@ManHinhSanPham,
                R.layout.dong_chon_danh_muc,
                tenDanhMuc
            ).apply {
                setDropDownViewResource(R.layout.dong_chon_danh_muc_dang_mo)
            }
            boLocDanhMuc.onItemSelectedListener =
                object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: android.widget.AdapterView<*>?,
                        view: View?,
                        position: Int,
                        id: Long
                    ) {
                        trangHienTai = 1
                        taiDanhSachSanPham(oTimKiem.text?.toString())
                    }

                    override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
                }
        }
    }

    /**
     * Tai danh sach san pham theo bo loc hien tai.
     *
     * @param tuKhoa Tu khoa tim kiem, null la tai tat ca.
     */
    private fun taiDanhSachSanPham(tuKhoa: String? = null) {
        if (dangTai) {
            return
        }

        dangTai = true
        capNhatTrangThaiTai(true)
        xoaThongBaoLoi()

        lifecycleScope.launch {
            val danhMuc =
                danhSachDanhMuc.getOrNull(boLocDanhMuc.selectedItemPosition - 1)
            when (
                val ketQua = dichVuSanPham.taiDanhSach(
                    tuKhoa,
                    danhMuc?.id,
                    trangHienTai,
                    baoGomSanPhamAn,
                    SO_SAN_PHAM_MOI_TRANG
                )
            ) {
                is KetQuaSanPham.ThanhCong -> {
                    trinhBaySanPham.capNhatDanhSach(ketQua.danhSach)
                    tongSoTrang = ketQua.phanTrang.totalPages.coerceAtLeast(1)
                    tongSoSanPhamToanBo = ketQua.phanTrang.total

                    thongTinPhanTrang.text =
                        getString(R.string.trang_san_pham, trangHienTai, tongSoTrang)
                    nutTrangTruoc.isEnabled = trangHienTai > 1
                    nutTrangSau.isEnabled = trangHienTai < tongSoTrang

                    // Khu vuc phan trang chi hien khi co nhieu trang.
                    khuVucPhanTrang.visibility =
                        if (tongSoTrang > 1) View.VISIBLE else View.GONE

                    capNhatTongSoSanPham(ketQua.danhSach.size)
                    xoaThongBaoLoi()
                }

                is KetQuaSanPham.ThatBai -> {
                    thongBaoSanPham.text = ketQua.thongBao
                    thongBaoSanPham.visibility = View.VISIBLE
                }
            }

            dangTai = false
            capNhatTrangThaiTai(false)
        }
    }

    /**
     * Cap nhat dong thong ke so san pham theo dong duoi dau danh sach.
     *
     * @param soHienThi So san pham dang co tren trang hien tai.
     */
    private fun capNhatTongSoSanPham(soHienThi: Int) {
        if (tongSoSanPhamToanBo <= 0) {
            tongSoSanPham.text = ""
            thongBaoRongSanPham.visibility = View.VISIBLE
            return
        }

        thongBaoRongSanPham.visibility = View.GONE

        // Hien thi khoang "tu - den" giong dong pagination cua web.
        val tu = (trangHienTai - 1) * SO_SAN_PHAM_MOI_TRANG + 1
        val den = minOf(trangHienTai * SO_SAN_PHAM_MOI_TRANG, tongSoSanPhamToanBo)
        tongSoSanPham.text = getString(R.string.hien_thi_tu_den_trong, tu, den, tongSoSanPhamToanBo)
    }

    /**
     * Mo bottom sheet them hoac sua san pham.
     *
     * Bottom sheet phu hop hon hop thoai tren mobile vi nguoi dung nhinh
     * thay duoc toan bo form va cac nut bam luon nam o cuoi man hinh.
     *
     * @param sanPham San pham can sua, null neu dang them moi.
     */
    private fun moHopThoaiSanPham(sanPham: SanPham?) {
        val giaoDien = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_san_pham, null)
        val tieuDe = giaoDien.findViewById<TextView>(R.id.tieu_de_hop_thoai)
        val moTa = giaoDien.findViewById<TextView>(R.id.mo_ta_hop_thoai)
        val oTen = giaoDien.findViewById<TextInputEditText>(R.id.o_ten_san_pham)
        val oSku = giaoDien.findViewById<TextInputEditText>(R.id.o_sku_san_pham)
        val oMaVach = giaoDien.findViewById<TextInputEditText>(R.id.o_ma_vach_san_pham)
        val oGiaNhap = giaoDien.findViewById<TextInputEditText>(R.id.o_gia_nhap_san_pham)
        val oGiaBan = giaoDien.findViewById<TextInputEditText>(R.id.o_gia_ban_san_pham)
        val oTon = giaoDien.findViewById<TextInputEditText>(R.id.o_ton_san_pham)
        val oTonToiThieu = giaoDien.findViewById<TextInputEditText>(R.id.o_ton_toi_thieu_san_pham)
        val oMoTa = giaoDien.findViewById<TextInputEditText>(R.id.o_mo_ta_san_pham)
        val oHinhAnh = giaoDien.findViewById<TextInputEditText>(R.id.o_hinh_anh_san_pham)
        val xemTruocHinhAnh = giaoDien.findViewById<ImageView>(R.id.xem_truoc_hinh_anh)
        val boChonDanhMuc = giaoDien.findViewById<Spinner>(R.id.bo_chon_danh_muc_san_pham)
        val khungTen = giaoDien.findViewById<TextInputLayout>(R.id.khung_ten_san_pham)
        val khungSku = giaoDien.findViewById<TextInputLayout>(R.id.khung_sku_san_pham)
        val loiGia = giaoDien.findViewById<TextView>(R.id.loi_gia)
        val nutLuu = giaoDien.findViewById<MaterialButton>(R.id.nut_luu_hop_thoai)
        val nutHuy = giaoDien.findViewById<MaterialButton>(R.id.nut_huy_hop_thoai)
        val nutDong = giaoDien.findViewById<ImageButton>(R.id.nut_dong_hop_thoai)

        val dangThemMoi = sanPham == null

        // Tieu de doi theo thao tac dang thuc hien.
        tieuDe.text = getString(
            if (dangThemMoi) R.string.them_san_pham_moi else R.string.chinh_sua_san_pham
        )
        moTa.text = getString(
            if (dangThemMoi) R.string.nhap_thong_tin_san_pham else R.string.cap_nhat_thong_tin_san_pham
        )
        nutLuu.text = getString(
            if (dangThemMoi) R.string.them_san_pham else R.string.luu_thay_doi
        )

        val tenDanhMuc = danhSachDanhMuc.map { it.name }
        boChonDanhMuc.adapter = ArrayAdapter(
            this,
            R.layout.dong_chon_danh_muc,
            tenDanhMuc
        ).apply {
            setDropDownViewResource(R.layout.dong_chon_danh_muc_dang_mo)
        }
        val viTriDanhMuc = danhSachDanhMuc.indexOfFirst { it.id == sanPham?.categoryId }
        if (viTriDanhMuc >= 0) boChonDanhMuc.setSelection(viTriDanhMuc)

        // Dien gia tri cua san pham dang sua. Gia tien duoc chuyen ve chuoi
        // de dua vao o nhap ma khong co dau phay.
        sanPham?.let {
            oTen.setText(it.name)
            oSku.setText(it.sku)
            oMaVach.setText(it.barcode.orEmpty())
            oGiaNhap.setText(DinhDangTien.taoChuoiChoOCoNhap(it.importPrice))
            oGiaBan.setText(DinhDangTien.taoChuoiChoOCoNhap(it.sellPrice))
            oTon.setText(it.stock.toString())
            oTonToiThieu.setText(it.minStock.toString())
            oMoTa.setText(it.description.orEmpty())
            oHinhAnh.setText(it.imageUrl.orEmpty())
        } ?: oTonToiThieu.setText("10")

        // Xem truoc anh ngay khi nguoi dung nhap duong dan.
        oHinhAnh.doAfterTextChanged { vanBan ->
            val duongDan = vanBan?.toString()?.trim().orEmpty()
            if (duongDan.isBlank()) {
                xemTruocHinhAnh.visibility = View.GONE
            } else {
                xemTruocHinhAnh.visibility = View.VISIBLE
                xemTruocHinhAnh.setImageURL(duongDan)
            }
        }

        // Kiem tra ngay khi ngung nhap de bao loi ngay tren o nhap.
        val theoDoiGia = object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = Unit
            override fun afterTextChanged(s: Editable?) {
                val giaNhapHopLe =
                    !DinhDangTien.laGiaKhongHopLe(oGiaNhap.text?.toString())
                val giaBanHopLe =
                    !DinhDangTien.laGiaKhongHopLe(oGiaBan.text?.toString())

                loiGia.visibility =
                    if (giaNhapHopLe && giaBanHopLe) View.GONE else View.VISIBLE
                loiGia.text = getString(R.string.loi_gia_khong_hop_le)
            }
        }
        oGiaNhap.addTextChangedListener(theoDoiGia)
        oGiaBan.addTextChangedListener(theoDoiGia)

        val hopThoai = BottomSheetDialog(this)
        hopThoai.setContentView(giaoDien)

        // Sheet chiem mot phan man hinh, giu lai phan tren de nguoi dung
        // van thay danh sach san pham phia duoi.
        val chieuCaoSheet = (resources.displayMetrics.heightPixels * 0.92f).toInt()
        hopThoai.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        hopThoai.behavior.skipCollapsed = true
        hopThoai.behavior.peekHeight = chieuCaoSheet

        // Chieu cao cua sheet phai gan bang so pixel vi thuoc tinh trong XML
        // khong ap dung cho bottom sheet.
        hopThoai.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.layoutParams?.height = chieuCaoSheet

        // Ban phim phai day noi dung len de nguoi dung khong bi che o nhap.
        hopThoai.window?.setSoftInputMode(
            android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        val dongHopThoai = {
            hopThoai.dismiss()
            Unit
        }
        nutHuy.setOnClickListener { dongHopThoai() }
        nutDong.setOnClickListener { dongHopThoai() }

        // Bam ra ngoai sheet cung dong hop thoai, giong thuong le cua bottom sheet.
        hopThoai.setOnDismissListener {
            oGiaNhap.removeTextChangedListener(theoDoiGia)
            oGiaBan.removeTextChangedListener(theoDoiGia)
        }

        nutLuu.setOnClickListener {
            // Chuyen du lieu trong form thanh doi tuong de gui len server.
            val giaNhap = DinhDangTien.chuyenChuoiSangTien(oGiaNhap.text?.toString())
            val giaBan = DinhDangTien.chuyenChuoiSangTien(oGiaBan.text?.toString())
            val duLieu = DuLieuSanPham(
                name = oTen.text.toString().trim(),
                sku = oSku.text.toString().trim(),
                barcode = oMaVach.text.toString().trim().ifBlank { null },
                categoryId = danhSachDanhMuc.getOrNull(
                    boChonDanhMuc.selectedItemPosition
                )?.id,
                importPrice = giaNhap,
                sellPrice = giaBan,
                stock = oTon.text.toString().trim().toIntOrNull() ?: 0,
                minStock = oTonToiThieu.text.toString().trim().toIntOrNull() ?: 10,
                description = oMoTa.text.toString(),
                imageUrl = oHinhAnh.text.toString().trim().ifBlank { null }
            )

            // Kiem tra cac truong bat buoc truoc khi gui request.
            if (duLieu.name.isBlank()) {
                khungTen.error = getString(R.string.ten_san_pham)
                khungTen.requestFocus()
                return@setOnClickListener
            }
            khungTen.error = null

            if (duLieu.sku.isBlank()) {
                khungSku.error = getString(R.string.ma_sku)
                khungSku.requestFocus()
                return@setOnClickListener
            }
            khungSku.error = null

            // Gia nhap phai khong am, gia ban phai lon hon 0.
            if (duLieu.importPrice < 0 || duLieu.sellPrice <= 0) {
                loiGia.visibility = View.VISIBLE
                loiGia.text = getString(R.string.loi_gia_khong_hop_le)
                return@setOnClickListener
            }

            // Neu nguoi dung nhap chuoi khong phai so thi bao loi ngay.
            if (DinhDangTien.laGiaKhongHopLe(oGiaNhap.text?.toString()) ||
                DinhDangTien.laGiaKhongHopLe(oGiaBan.text?.toString())
            ) {
                loiGia.visibility = View.VISIBLE
                loiGia.text = getString(R.string.loi_gia_khong_hop_le)
                return@setOnClickListener
            }

            nutLuu.isEnabled = false
            nutLuu.text = getString(R.string.dang_xu_ly)

            lifecycleScope.launch {
                val ketQua = if (sanPham == null) {
                    dichVuSanPham.taoSanPham(duLieu)
                } else {
                    dichVuSanPham.suaSanPham(sanPham.id, duLieu)
                }

                if (ketQua.isSuccess) {
                    hopThoai.dismiss()
                    taiDanhSachSanPham(oTimKiem.text?.toString())
                } else {
                    nutLuu.isEnabled = true
                    nutLuu.text = getString(
                        if (sanPham == null) R.string.them_san_pham else R.string.luu_thay_doi
                    )
                    Toast.makeText(
                        this@ManHinhSanPham,
                        thongBaoLoiTaoHoacSua(ketQua),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        hopThoai.show()
    }

    /**
     * Chuyen loi tu server thanh thong bao tieng Viet.
     *
     * @param ketQua Ket qua tra ve tu DichVuSanPham.
     * @return Chuoi thong bao hien thi cho nguoi dung.
     */
    private fun thongBaoLoiTaoHoacSua(ketQua: Result<*>): String {
        // Loi do server nem ra chua ma loi chuan trong message.
        val maLoi = ketQua.exceptionOrNull()?.message.orEmpty()

        return when {
            // Ten hoac SKU bi trung, huong dan nguoi dung doi lai.
            maLoi == "DUPLICATE" -> getString(R.string.loi_trung_ten_hoac_sku)

            // San pham da co trong phieu nhap nen khong the xoa.
            maLoi == "PRODUCT_HAS_INVOICE_HISTORY" ->
                getString(R.string.loi_xoa_san_pham_da_nhap)

            // Cac truong bat buoc bi thieu.
            maLoi == "VALIDATION_ERROR" -> getString(R.string.loi_du_lieu_bat_buoc)

            else -> ketQua.exceptionOrNull()?.message
                ?: getString(R.string.khong_the_tai_du_lieu)
        }
    }

    /**
     * Hoi xac nhan truoc khi xoa san pham.
     *
     * @param sanPham San pham can xoa.
     */
    private fun xacNhanXoaSanPham(sanPham: SanPham) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.xoa_san_pham)
            .setMessage(getString(R.string.xac_nhan_xoa_san_pham, sanPham.name))
            .setNegativeButton(R.string.huy, null)
            .setPositiveButton(R.string.xoa) { _, _ ->
                lifecycleScope.launch {
                    val ketQua = dichVuSanPham.xoaSanPham(sanPham.id)

                    if (ketQua.isSuccess) {
                        taiDanhSachSanPham(oTimKiem.text?.toString())
                    } else {
                        // So sanh ma loi chuan thay vi kiem tra noi dung message.
                        val maLoi = ketQua.exceptionOrNull()?.message.orEmpty()
                        val thongBao = if (maLoi == "PRODUCT_HAS_INVOICE_HISTORY") {
                            getString(R.string.loi_xoa_san_pham_da_nhap)
                        } else {
                            ketQua.exceptionOrNull()?.message
                                ?: getString(R.string.khong_the_tai_du_lieu)
                        }
                        Toast.makeText(this@ManHinhSanPham, thongBao, Toast.LENGTH_LONG).show()
                    }
                }
            }
            .show()
    }

    /**
     * An hoac hien san pham tren man hinh ban hang.
     *
     * @param sanPham San pham can doi trang thai.
     */
    private fun doiAnHienSanPham(sanPham: SanPham) {
        lifecycleScope.launch {
            if (dichVuSanPham.doiTrangThaiSanPham(sanPham.id, !sanPham.isActive).isSuccess) {
                taiDanhSachSanPham(oTimKiem.text?.toString())
            }
        }
    }

    /**
     * Mo man hinh quan ly don vi tinh cua san pham.
     *
     * @param sanPham San pham can quan ly don vi.
     */
    private fun moManHinhDonVi(sanPham: SanPham) {
        startActivity(Intent(this, ManHinhDonViSanPham::class.java).apply {
            putExtra(ManHinhDonViSanPham.KHOA_MA_SAN_PHAM, sanPham.id)
            putExtra(ManHinhDonViSanPham.KHOA_TEN_SAN_PHAM, sanPham.name)
        })
    }

    /**
     * Bat hoac tat vong tai va nut them san pham.
     *
     * @param dangTai Dang tai hay khong.
     */
    private fun capNhatTrangThaiTai(dangTai: Boolean) {
        vongTaiSanPham.visibility = if (dangTai) View.VISIBLE else View.GONE
        nutThemSanPham.isEnabled = !dangTai
    }

    /** An thong bao loi cu truoc moi lan tai. */
    private fun xoaThongBaoLoi() {
        thongBaoSanPham.text = ""
        thongBaoSanPham.visibility = View.GONE
    }

    companion object {
        /** So san pham hien tren mot trang. */
        const val SO_SAN_PHAM_MOI_TRANG = 10
    }
}
