package com.example.store_manager

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.store_manager.du_lieu.DanhMuc
import com.example.store_manager.du_lieu.DichVuSanPham
import com.example.store_manager.du_lieu.DinhDangTien
import com.example.store_manager.du_lieu.DonViSanPham
import com.example.store_manager.du_lieu.KetQuaSanPham
import com.example.store_manager.du_lieu.KetQuaTaiSanPham
import com.example.store_manager.du_lieu.KhoDuLieuSanPham
import com.example.store_manager.du_lieu.MatHangGioHang
import com.example.store_manager.du_lieu.SanPham
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.IOException
import com.google.gson.Gson

/** Khung man hinh ban hang, da duoc dat tren tab thuong dung. */
class ManHinhBanHang : ManHinhCoThanhTab() {
    override val taiNguyenNoiDung: Int = R.layout.ban_hang

    private lateinit var danhSachSanPham: RecyclerView
    private lateinit var danhSachGioHang: RecyclerView
    private lateinit var boLocDanhMuc: Spinner
    private lateinit var oTimSanPham: TextInputEditText
    private lateinit var tongTienTamTinh: TextView
    private lateinit var tongSoSanPham: TextView
    private lateinit var thongBaoGioHang: TextView
    private lateinit var vongTaiBanHang: ProgressBar
    private lateinit var nutTinhTien: MaterialButton
    private lateinit var nutXoaMatHang: MaterialButton
    private lateinit var trinhBaySanPham: TrinhBaySanPhamBan
    private lateinit var trinhBayGioHang: TrinhBayGioHang
    private lateinit var dichVuSanPham: DichVuSanPham
    private lateinit var khoDuLieu: KhoDuLieuSanPham
    private val danhSachGioHangTam = mutableListOf<MatHangGioHang>()
    private var matHangDangChon: MatHangGioHang? = null
    private val tonKhoBanDau = mutableMapOf<String, Int>()
    private var danhSachDanhMuc = listOf<DanhMuc>()
    private var congViecTimKiem: Job? = null

    /** Trang san pham dang hien thi tren danh sach POS. */
    private var trangHienTai = 1

    /** Tong so trang san pham co the tai. */
    private var tongTrangSanPham = 1

    /** Co dang tai them san pham hay khong, dung de tranh tai trung lap. */
    private var dangTaiThemSanPham = false

    /** Hop thoai chon don vi ban, giu lai de biet dang mo hay da dong. */
    private lateinit var hopThoaiDonVi: androidx.appcompat.app.AlertDialog

    /**
     * Bo nhan ket qua tu man hinh chi tiet hoa don.
     *
     * Ket qua nay cho biet hoa don co duoc tao thanh cong hay khong. Neu
     * thanh cong, gio hang se duoc xoa va ton kho duoc lam moi.
     */
    private val boNhanKetQuaThanhToan = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { ketQua ->
        if (ketQua.resultCode != Activity.RESULT_OK) {
            return@registerForActivityResult
        }

        // Ket qua duoc tra ve khi man hinh bat dau hien thi lai. Giao dien luc
        // do chua duoc khoi tao nen chi ghi nhan co, viec xoa gio hang se
        // duoc thuc hien sau khi khoi tao xong trong onStart.
        canXoaGioHangSauKhiBan = true
    }

    /** Co bao cho biet can xoa gio hang sau khi ban thanh cong. */
    private var canXoaGioHangSauKhiBan = false

    override fun onStart() {
        super.onStart()

        khoiTaoThanhPhanGiaoDien()
        dichVuSanPham = DichVuSanPham(applicationContext)
        khoDuLieu = KhoDuLieuSanPham(applicationContext)
        khoiTaoDanhSach()
        ganSuKienTimKiemVaBoLoc()

        // Ket qua thanh toan duoc tra ve ngay trong super.onStart(), nen co
        // gia tri da dung o day. Neu vua ban thanh cong thi gio hang phai duoc
        // xoa truoc khi hien thi lai cho nguoi dung.
        if (canXoaGioHangSauKhiBan) {
            canXoaGioHangSauKhiBan = false
            danhSachGioHangTam.clear()
            matHangDangChon = null
        }

        taiDanhMuc()
        taiDanhSachSanPham()
        capNhatGioHang()
    }

    /** Anh xa cac khu vuc POS vao bien co ten de hieu. */
    private fun khoiTaoThanhPhanGiaoDien() {
        danhSachSanPham = findViewById(R.id.danh_sach_san_pham_ban)
        danhSachGioHang = findViewById(R.id.danh_sach_gio_hang)
        boLocDanhMuc = findViewById(R.id.bo_loc_danh_muc)
        oTimSanPham = findViewById(R.id.o_tim_san_pham_ban)
        tongTienTamTinh = findViewById(R.id.tong_tien_tam_tinh)
        tongSoSanPham = findViewById(R.id.tong_so_san_pham)
        thongBaoGioHang = findViewById(R.id.thong_bao_gio_hang)
        vongTaiBanHang = findViewById(R.id.vong_tai_ban_hang)
        nutTinhTien = findViewById(R.id.nut_tinh_tien)
        nutXoaMatHang = findViewById(R.id.nut_xoa_mat_hang)
    }

    /** Tao hai adapter: mot cho san pham, mot cho cac dong trong gio hang. */
    private fun khoiTaoDanhSach() {
        trinhBaySanPham = TrinhBaySanPhamBan { sanPham ->
            themSanPhamVaoGio(sanPham)
        }
        trinhBayGioHang = TrinhBayGioHang(
            khiThayDoiSoLuong = { matHang, thayDoi ->
                thayDoiSoLuong(matHang, thayDoi)
            },
            khiNhapSoLuong = { matHang, soLuongMoi ->
                datLaiSoLuong(matHang, soLuongMoi)
            },
            khiChonMatHang = { matHang ->
                chonMatHang(matHang)
            }
        )
        danhSachSanPham.layoutManager = LinearLayoutManager(this)
        danhSachSanPham.adapter = trinhBaySanPham
        danhSachGioHang.layoutManager = LinearLayoutManager(this)
        danhSachGioHang.adapter = trinhBayGioHang

        // Khi nguoi dung cuon den cuoi danh sach san pham thi tai trang tiep
        // theo de hien thi duoc tat ca san pham trong cua hang.
        danhSachSanPham.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(
                boDanhSach: RecyclerView,
                viTriXemDau: Int,
                viTriCuoi: Int
            ) {
                super.onScrolled(boDanhSach, viTriXemDau, viTriCuoi)
                val quanLy = boDanhSach.layoutManager as? LinearLayoutManager ?: return
                if (quanLy.findLastVisibleItemPosition() >= quanLy.itemCount - 3) {
                    taiTrangSanPhamTiepTheo()
                }
            }
        })

        nutTinhTien.setOnClickListener {
            moChiTietHoaDon()
        }
        nutXoaMatHang.setOnClickListener {
            xoaMatHangDangChon()
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
                this@ManHinhBanHang,
                android.R.layout.simple_spinner_dropdown_item,
                tenDanhMuc
            )
            boLocDanhMuc.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    viTri: Int,
                    id: Long
                ) {
                    taiDanhSachSanPham()
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
            }
        }
    }

    /** Tim kiem co debounce va loc theo categoryId khi nguoi dung chon danh muc. */
    private fun ganSuKienTimKiemVaBoLoc() {
        oTimSanPham.doAfterTextChanged { vanBan ->
            congViecTimKiem?.cancel()
            congViecTimKiem = lifecycleScope.launch {
                delay(350)
                taiDanhSachSanPham(vanBan?.toString())
            }
        }
    }

    /** Goi API san pham va loc category bo sung tren client de API cu van dung duoc. */
    private fun taiDanhSachSanPham(tuKhoa: String? = null) {
        capNhatTrangThaiTai(true)
        trangHienTai = 1
        lifecycleScope.launch {
            val viTriDanhMuc = boLocDanhMuc.selectedItemPosition
            val danhMucDangChon = danhSachDanhMuc.getOrNull(viTriDanhMuc - 1)

            // Dung kho du lieu de uu tien server, khi mat mang thi doc tu
            // database cuc lo nhung nhan vien van ban hang duoc.
            when (
                val ketQua = khoDuLieu.taiSanPham(
                    tuKhoa = tuKhoa,
                    maDanhMuc = danhMucDangChon?.id,
                    trang = 1
                )
            ) {
                is KetQuaTaiSanPham.ThanhCong -> {
                    ketHopTonKhoGoc(ketQua.danhSach)
                    sanPhamDangHienThi = ketQua.danhSach
                    tongTrangSanPham = 1
                    capNhatTonKhoHienThi()
                    capNhatTrangThaiTai(false)

                    // Hien thi canh bao khi du lieu duoc doc tu database cuc lo.
                    if (ketQua.tuCache) {
                        Toast.makeText(
                            this@ManHinhBanHang,
                            getString(R.string.thong_bao_dang_dung_du_lieu_cuc_bo),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                is KetQuaTaiSanPham.ThatBai -> {
                    capNhatTrangThaiTai(false)
                    Toast.makeText(
                        this@ManHinhBanHang,
                        ketQua.thongBao,
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    /**
     * Tai trang san pham tiep theo va noi vao cuoi danh sach hien tai.
     *
     * POS phai hien thi duoc tat ca san pham de ban hang, nen danh sach duoc
     * tai theo trang khi nguoi dung cuon xuong cuoi.
     */
    private fun taiTrangSanPhamTiepTheo() {
        // Khong tai them khi da het trang hoac dang tai.
        if (dangTaiThemSanPham || trangHienTai >= tongTrangSanPham) {
            return
        }

        dangTaiThemSanPham = true
        capNhatTrangThaiTai(true)
        lifecycleScope.launch {
            val viTriDanhMuc = boLocDanhMuc.selectedItemPosition
            val danhMucDangChon = danhSachDanhMuc.getOrNull(viTriDanhMuc - 1)
            val tuKhoa = oTimSanPham.text?.toString()

            when (
                val ketQua = dichVuSanPham.taiDanhSach(
                    tuKhoa = tuKhoa?.takeIf { it.isNotBlank() },
                    maDanhMuc = danhMucDangChon?.id,
                    trang = trangHienTai + 1
                )
            ) {
                is KetQuaSanPham.ThanhCong -> {
                    trangHienTai++
                    ketHopTonKhoGoc(ketQua.danhSach)
                    // Chi them san pham chua co trong danh sach hien thi de tranh
                    // trung khi backend tra lap lai mot san pham o hai trang.
                    val idDaCo = sanPhamDangHienThi.map { it.id }.toSet()
                    sanPhamDangHienThi = sanPhamDangHienThi + ketQua.danhSach.filter {
                        it.id !in idDaCo
                    }
                    tongTrangSanPham = ketQua.phanTrang.totalPages
                    capNhatTonKhoHienThi()
                }
                is KetQuaSanPham.ThatBai -> {
                    // Khong tai duoc trang moi thi dung tai them de tranh loop.
                    tongTrangSanPham = trangHienTai
                }
            }

            dangTaiThemSanPham = false
            capNhatTrangThaiTai(false)
        }
    }

    /**
     * Them san pham vao gio hoac tang so luong, khong vuot ton kho.
     *
     * San pham chi co mot don vi thi them thang. San pham co nhieu don vi thi
     * mo hop thoai de nguoi dung chon don vi ban truoc khi them vao gio.
     *
     * @param sanPham San pham duoc chon tren danh sach.
     */
    private fun themSanPhamVaoGio(sanPham: SanPham) {
        // San pham khong co cau hinh don vi thi ban theo don vi co ban cua san pham.
        if (sanPham.units.isEmpty()) {
            themVaoGioTheoDonVi(sanPham, donViBan = null)
            return
        }

        // Chi mot don vi thi khong can hoi lai, them thang cho nhanh.
        if (sanPham.units.size == 1) {
            themVaoGioTheoDonVi(sanPham, donViBan = sanPham.units.first())
            return
        }

        // Nhieu don vi thi mo hop thoai cho nguoi dung chon.
        moHopThoaiChonDonViBan(sanPham)
    }

    /**
     * Mo hop thoai cho nguoi dung chon don vi ban cua san pham.
     *
     * @param sanPham San pham dang chon.
     */
    private fun moHopThoaiChonDonViBan(sanPham: SanPham) {
        val giaoDien = LayoutInflater.from(this).inflate(R.layout.hop_thoai_don_vi_ban, null)
        val danhSachDonVi = giaoDien.findViewById<RecyclerView>(R.id.danh_sach_don_vi_ban)

        val trinhBayDonVi = TrinhBayDonViBan(
            khiChonDonVi = { donVi ->
                hopThoaiDonVi.dismiss()
                themVaoGioTheoDonVi(sanPham, donVi)
            },
            // Ton kho con lai duoc doc tai thoi diem hien thi de so luong toi da
            // luon khop voi so luong da them vao gio hang.
            layTonKhoConLai = { tinhTonKhoConLai(sanPham) }
        )
        danhSachDonVi.layoutManager = LinearLayoutManager(this)
        danhSachDonVi.adapter = trinhBayDonVi
        trinhBayDonVi.capNhatDanhSach(sanPham.units)

        hopThoaiDonVi = MaterialAlertDialogBuilder(this)
            .setView(giaoDien)
            .setNegativeButton(R.string.huy, null)
            .create()
        hopThoaiDonVi.show()
    }

    /**
     * Them mot dong gio hang theo don vi ban da chon.
     *
     * @param sanPham  San pham can them.
     * @param donViBan Don vi ban, null nghia la don vi co ban.
     */
    private fun themVaoGioTheoDonVi(sanPham: SanPham, donViBan: DonViSanPham?) {
        // So luong toi da cho phep theo don vi ban nay.
        val heSoQuyDoi = donViBan?.conversionQty?.takeIf { it > 0 } ?: 1
        val tonKhoConLai = tinhTonKhoConLai(sanPham)
        val soLuongToiDa = tonKhoConLai / heSoQuyDoi

        if (soLuongToiDa <= 0) {
            Toast.makeText(
                this,
                getString(R.string.loi_het_ton_kho_khi_ban, sanPham.name),
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        // Khoa dong giong san pham + don vi ban de phan biet cac dong khac nhau.
        val khoaDong = "${sanPham.id}|${donViBan?.unitId.orEmpty()}"
        val viTri = danhSachGioHangTam.indexOfFirst { it.khoaDong == khoaDong }

        if (viTri >= 0) {
            // Dong da co san pham nay, chi tang so luong neu chua vuot ton kho.
            val matHang = danhSachGioHangTam[viTri]
            if (matHang.soLuong < soLuongToiDa) {
                danhSachGioHangTam[viTri] = matHang.copy(soLuong = matHang.soLuong + 1)
            }
        } else {
            danhSachGioHangTam.add(
                MatHangGioHang(sanPham = sanPham, soLuong = 1, donViBan = donViBan)
            )
        }
        capNhatGioHang()
    }

    /**
     * Tang/giam so luong va xoa dong khi so luong ve 0.
     *
     * @param matHang Dong hang dang chon.
     * @param thayDoi Luong thay doi, -1 giam va 1 tang.
     */
    private fun thayDoiSoLuong(matHang: MatHangGioHang, thayDoi: Int) {
        val viTri = danhSachGioHangTam.indexOfFirst { it.khoaDong == matHang.khoaDong }
        if (viTri < 0) return
        val soLuongMoi = matHang.soLuong + thayDoi

        if (soLuongMoi <= 0) {
            // Giam ve 0 thi xoa dong hang khoi gio hang.
            danhSachGioHangTam.removeAt(viTri)
            capNhatGioHang()
            return
        }

        val soLuongToiDa = tinhSoLuongToiDa(matHang)
        if (soLuongMoi <= soLuongToiDa) {
            danhSachGioHangTam[viTri] = matHang.copy(soLuong = soLuongMoi)
            capNhatGioHang()
            return
        }

        // Da dat toi ton kho, bao nguoi dung biet khong the tang them.
        Toast.makeText(
            this,
            getString(R.string.loi_het_ton_kho_khi_ban, matHang.sanPham.name),
            Toast.LENGTH_SHORT
        ).show()
    }

    /**
     * Dat so luong moi tu o nhap nhanh, dong thoi cap nhat ton con lai.
     *
     * @param matHang    Dong hang dang chon.
     * @param soLuongMoi So luong nguoi dung nhap.
     */
    private fun datLaiSoLuong(matHang: MatHangGioHang, soLuongMoi: Int) {
        val viTri = danhSachGioHangTam.indexOfFirst { it.khoaDong == matHang.khoaDong }
        if (viTri < 0) return

        val soLuongToiDa = tinhSoLuongToiDa(matHang)
        when {
            // Nguoi dung nhap 0 thi dong hang duoc xoa khoi gio hang.
            soLuongMoi <= 0 -> {
                danhSachGioHangTam.removeAt(viTri)
                capNhatGioHang()
            }

            // So luong nam trong gio duoc cap nhat theo gia tri nguoi dung nhap.
            soLuongMoi <= soLuongToiDa -> {
                danhSachGioHangTam[viTri] = matHang.copy(soLuong = soLuongMoi)
                capNhatGioHang()
            }

            // Vuot qua ton kho thi gioi han lai va bao nguoi dung biet.
            else -> {
                danhSachGioHangTam[viTri] = matHang.copy(soLuong = soLuongToiDa)
                capNhatGioHang()
                Toast.makeText(
                    this,
                    getString(R.string.loi_het_ton_kho_khi_ban, matHang.sanPham.name),
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    /**
     * Tinh so luong toi da theo don vi ban cua mot dong hang.
     *
     * Ton kho luon tinh theo don vi co ban, nen phai chia cho he so quy doi
     * de ra so luong theo don vi ban.
     *
     * Luu y: so luong dang nam trong gio cua chinh dong nay duoc tru ra rieng,
     * vi so luong moi cua dong dang nhap se thay theo cho luong cu. Neu tru
     * ca so luong cu, nguoi dung nhap so lon hon ton kho thi bi chon bien.
     *
     * @param matHang Dong hang can tinh.
     * @return So luong lon nhat theo don vi ban ma ton kho cho phep.
     */
    private fun tinhSoLuongToiDa(matHang: MatHangGioHang): Int {
        val tonKhoGoc = tonKhoBanDau[matHang.sanPham.id] ?: matHang.sanPham.stock

        // So luong da dung cua cac dong khac cung san pham nay. Dong dang
        // sua duoc loai ra rieng.
        val soLuongDaDungCuaDongKhac = danhSachGioHangTam
            .filter { it.sanPham.id == matHang.sanPham.id && it.khoaDong != matHang.khoaDong }
            .sumOf { it.soLuongDonViCoBan }

        val tonKhoConLai = (tonKhoGoc - soLuongDaDungCuaDongKhac).coerceAtLeast(0)
        return tonKhoConLai / matHang.heSoQuyDoi.coerceAtLeast(1)
    }

    /** Cap nhat tong so luong, tong tien va adapter gio hang. */
    private fun capNhatGioHang() {
        capNhatTonKhoHienThi()
        trinhBayGioHang.capNhatDanhSach(danhSachGioHangTam)
        val tongSoLuong = danhSachGioHangTam.sumOf { it.soLuong }
        val tongTien = danhSachGioHangTam.sumOf { it.thanhTien }
        tongSoSanPham.text = getString(R.string.so_san_pham, tongSoLuong)
        tongTienTamTinh.text = dinhDangTien(tongTien)
        thongBaoGioHang.visibility = if (danhSachGioHangTam.isEmpty()) View.VISIBLE else View.GONE
        nutTinhTien.isEnabled = danhSachGioHangTam.isNotEmpty()
        matHangDangChon = matHangDangChon?.let { dangChon ->
            danhSachGioHangTam.firstOrNull { it.khoaDong == dangChon.khoaDong }
        }
        nutXoaMatHang.visibility = if (matHangDangChon == null) View.GONE else View.VISIBLE
    }

    /** Ghi nho dong dang chon va hien nut xoa o cot tong tien. */
    private fun chonMatHang(matHang: MatHangGioHang) {
        matHangDangChon = matHang
        nutXoaMatHang.visibility = View.VISIBLE
    }

    /** Xoa toan bo dong dang chon, tra lai toan bo ton kho tam thoi. */
    private fun xoaMatHangDangChon() {
        val matHang = matHangDangChon ?: run {
            android.widget.Toast.makeText(
                this,
                getString(R.string.chua_chon_mat_hang),
                android.widget.Toast.LENGTH_SHORT
            ).show()
            return
        }
        // Xoa dong duoc chon. Khoa dong gom ca san pham va don vi ban nen
        // chi xoa dung dong dang chon.
        danhSachGioHangTam.removeAll { it.khoaDong == matHang.khoaDong }
        matHangDangChon = null
        capNhatGioHang()
    }

    /**
     * Luu ton kho server truoc khi tru tam de co the phuc hoi khi giam gio.
     *
     * Ton kho lay tu server la nguon su that, nen ghi de gia tri moi nhat
     * moi lan tai du lieu.
     *
     * @param danhSach Danh sach san pham vua tai tu server.
     */
    private fun ketHopTonKhoGoc(danhSach: List<SanPham>) {
        danhSach.forEach { sanPham ->
            tonKhoBanDau[sanPham.id] = sanPham.stock
        }
    }

    /**
     * Lay ton kho con lai cua san pham sau khi tru so luong dang nam trong gio.
     *
     * Ton kho duoc tinh theo don vi co ban, nen moi dong hang trong gio se tru
     * so luong da quy doi ve don vi co ban.
     *
     * @param sanPham San pham can tinh ton kho con lai.
     * @return So ton kho con lai theo don vi co ban.
     */
    private fun tinhTonKhoConLai(sanPham: SanPham): Int {
        val tonKhoGoc = tonKhoBanDau[sanPham.id] ?: sanPham.stock
        val soLuongDaDung = danhSachGioHangTam
            .filter { it.sanPham.id == sanPham.id }
            .sumOf { it.soLuongDonViCoBan }
        return (tonKhoGoc - soLuongDaDung).coerceAtLeast(0)
    }

    /**
     * Tao ban sao san pham voi ton kho con lai de adapter phia duoi hien thi ngay.
     */
    private fun capNhatTonKhoHienThi() {
        trinhBaySanPham.capNhatDanhSach(
            sanPhamDangHienThi.map { sanPham ->
                sanPham.copy(stock = tinhTonKhoConLai(sanPham))
            }
        )
    }

    private var sanPhamDangHienThi = emptyList<SanPham>()

    /** Mo man hinh chi tiet hoa don va truyen gio hang dang cho thanh toan. */
    private fun moChiTietHoaDon() {
        if (danhSachGioHangTam.isEmpty()) return

        // Ton kho co the da bi thay doi o may khac trong khi nguoi dung chon
        // hang, nen phai tai lai danh sach san pham truoc khi sang man hinh
        // thanh toan de tranh ban nhung mat hang da het ton.
        capNhatTrangThaiTai(true)
        lifecycleScope.launch {
            // Chi tai ton kho cua cac san pham dang co trong gio hang. API san
            // pham co phan trang, nen phai duyet het cac trang de khong bo
            // sot san pham nam o cac trang sau.
            val maSanPham = danhSachGioHangTam.map { it.sanPham.id }.distinct()

            // Loi ket noi khong duoc lam man hinh bi dong. Khi mat mang thi
            // bo qua buoc kiem tra ton kho va cho nguoi dung thanh toan
            // offline, server se chan lai neu thieu ton kho.
            val tonKhoMoi = try {
                dichVuSanPham.taiTonKhoTheoIdSanPham(maSanPham)
            } catch (loiHuy: kotlinx.coroutines.CancellationException) {
                // Man hinh bi dong giua chung, khong phai loi ket noi.
                throw loiHuy
            } catch (_: IOException) {
                // Mat mang: bo qua kiem tra ton kho.
                emptyMap()
            }

            if (tonKhoMoi.isNotEmpty()) {
                // Cap nhat ton kho goc cua cac san pham trong gio hang.
                tonKhoMoi.forEach { (maSanPham, soTon) ->
                    tonKhoBanDau[maSanPham] = soTon
                }

                // Loai bo nhung dong hang da vuot ton kho moi. So luong duoc
                // so sanh tren don vi co ban vi ton kho server cung luong theo
                // don vi co ban.
                val dongKhongHopLe = danhSachGioHangTam.filter { matHang ->
                    val tonKhoHienTai = tonKhoMoi[matHang.sanPham.id] ?: return@filter false
                    matHang.soLuongDonViCoBan > tonKhoHienTai
                }

                if (dongKhongHopLe.isNotEmpty()) {
                    // Thong bao cac dong bi loai bo vi khong con du ton kho.
                    dongKhongHopLe.forEach { matHang ->
                        android.widget.Toast.makeText(
                            this@ManHinhBanHang,
                            getString(
                                R.string.loi_het_ton_kho_khi_ban,
                                matHang.sanPham.name
                            ),
                            android.widget.Toast.LENGTH_LONG
                        ).show()
                    }

                    // Cap nhat lai so luong cua tung dong theo ton kho moi.
                    // So luong duoc tinh nguoc lai theo he so quy doi de luon
                    // nho hon so luong toi da theo don vi ban.
                    dongKhongHopLe.forEach { matHang ->
                        val tonKhoHienTai = tonKhoMoi[matHang.sanPham.id] ?: return@forEach
                        val viTri = danhSachGioHangTam.indexOfFirst {
                            it.khoaDong == matHang.khoaDong
                        }
                        if (viTri >= 0) {
                            val soLuongMoi = tonKhoHienTai / matHang.heSoQuyDoi.coerceAtLeast(1)
                            danhSachGioHangTam[viTri] = matHang.copy(soLuong = soLuongMoi)
                        }
                    }
                }

                // Loai bo dong voi so luong bang 0 sau khi chuan hoa.
                danhSachGioHangTam.removeAll { it.soLuong <= 0 }
                capNhatGioHang()
                capNhatTrangThaiTai(false)

                if (danhSachGioHangTam.isEmpty()) {
                    return@launch
                }

                // Chuyen sang man hinh chi tiet hoa don voi gio hang hien tai.
                moManHinhChiTietHoaDon()
            } else {
                // Khong tai duoc ton kho moi thi van cho nguoi dung tiep tuc
                // thanh toan, server se chan lai neu thieu ton kho.
                capNhatTrangThaiTai(false)
                moManHinhChiTietHoaDon()
            }
        }
    }

    /** Mo man hinh chi tiet hoa don khong kiem tra lai ton kho. */
    private fun moManHinhChiTietHoaDon() {
        if (danhSachGioHangTam.isEmpty()) return
        val intent = android.content.Intent(this, ManHinhChiTietHoaDon::class.java)
        intent.putExtra(
            ManHinhChiTietHoaDon.KHOA_GIO_HANG,
            Gson().toJson(danhSachGioHangTam)
        )
        boNhanKetQuaThanhToan.launch(intent)
    }

    private fun capNhatTrangThaiTai(dangTai: Boolean) {
        vongTaiBanHang.visibility = if (dangTai) View.VISIBLE else View.GONE
    }

    private fun dinhDangTien(giaTri: Double): String {
        return DinhDangTien.dinhDangTien(giaTri)
    }
}
