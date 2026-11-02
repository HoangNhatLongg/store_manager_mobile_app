package com.example.store_manager

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.store_manager.du_lieu.BoNhoPhienDangNhap
import com.example.store_manager.du_lieu.DichVuBanHang
import com.example.store_manager.du_lieu.DinhDangTien
import com.example.store_manager.du_lieu.DongHangHoaDon
import com.example.store_manager.du_lieu.HoaDonDaTao
import com.example.store_manager.du_lieu.KetQuaTaoHoaDon
import com.example.store_manager.du_lieu.KhachHang
import com.example.store_manager.du_lieu.KiemTraKetNoi
import com.example.store_manager.du_lieu.KhoDuLieuSanPham
import com.example.store_manager.du_lieu.MatHangGioHang
import com.example.store_manager.du_lieu.PhuongThucThanhToan
import com.example.store_manager.du_lieu.WorkerDongBoHoaDon
import com.example.store_manager.du_lieu.YeuCauTaoHoaDon
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.launch

/**
 * Man hinh thanh toan hoa don ban.
 *
 * Nguoi dung chon khach hang, phuong thuc thanh toan, nhap tien khach dua va
 * ghi chu, sau do xac nhan de tao hoa don that tren server.
 *
 * Man hinh nay dung chung khung voi cac man hinh khac nen no khong ke thua
 * ManHinhCoThanhTab, nhu vay khong co thanh dieu huong duoi va nguoi dung
 * khong the chuyen man hinh khi dang thanh toan.
 */
class ManHinhChiTietHoaDon : AppCompatActivity() {
    private lateinit var tongSoMatHangHoaDon: TextView
    private lateinit var tienTamTinhHoaDon: TextView
    private lateinit var tienGiamHoaDon: TextView
    private lateinit var tongCongHoaDon: TextView
    private lateinit var thongBaoHoaDon: TextView
    private lateinit var oChietKhauPhanTram: TextInputEditText
    private lateinit var oChietKhauTien: TextInputEditText
    private lateinit var oTienKhachDua: TextInputEditText
    private lateinit var oGhiChuHoaDon: TextInputEditText
    private lateinit var khungTienKhachDua: LinearLayout
    private lateinit var tienThuaHoaDon: TextView
    private lateinit var boChonKhachHang: Spinner
    private lateinit var boChonPhuongThucThanhToan: Spinner
    private lateinit var nutXacNhanThanhToan: MaterialButton
    private lateinit var nutDongHoaDon: MaterialButton

    private lateinit var dichVuBanHang: DichVuBanHang
    private lateinit var trinhBayDongHoaDon: TrinhBayDongHoaDon

    private var danhSachMatHang = emptyList<MatHangGioHang>()
    private var danhSachKhachHang = listOf<KhachHang>()
    private var tongTienTamTinh = 0.0
    private var tongCongHienTai = 0.0

    /**
     * Ma thao tac dung de lam idempotency key cho yeu cau tao hoa don.
     *
     * Ma nay duoc giu nguyen khi nguoi dung bam lai nut xac nhan, nho do retry
     * khong tao ra hai hoa don khac nhau.
     */
    private var maThaoTac = ""

    /**
     * Bundle giu lai ma thao tac khi man hinh bi tao lai do xoay man hinh.
     */
    private var bundleNhoMaThaoTac: Bundle? = null

    /**
     * Bo nhan ket qua tu man hinh thanh cong.
     *
     * Ket qua nay duoc chuyen tiep ve man hinh ban hang de xoa gio hang.
     */
    private val boNhanKetQuaThanhCong = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        // Hoa don da duoc tao that tren server nen luon bao thanh cong cho
        // man hinh ban hang, ke ca khi nguoi dung dong man hinh bang phim Back.
        setResult(Activity.RESULT_OK)
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.chi_tiet_hoa_don)

        // Giu lai bundle de lay dung ma thao tac sau khi man hinh bi tao lai.
        bundleNhoMaThaoTac = savedInstanceState

        khoiTaoThanhPhanGiaoDien()
        dichVuBanHang = DichVuBanHang(applicationContext)

        // Sinh ma thao tac ngay khi mo man hinh de moi phien thanh toan co
        // mot dinh danh rieng. Neu man hinh bi tao lai thi dung lai ma cu.
        maThaoTac = bundleNhoMaThaoTac?.getString(KHOA_MA_THAO_TAC)
            ?.takeIf { it.isNotBlank() }
            ?: dichVuBanHang.sinhMaThaoTac()

        khoiTaoDanhSachDong()
        docGioHangTuIntent()
        ganSuKienChonThanhToan()
        ganSuKienTinhChietKhau()
        ganSuKienTienKhachDua()
        capNhatHoaDon()

        // Bao ngay cho nhan vien biet dang offline de ho khong phan lac
        // khi bam xac nhan thanh toan.
        if (!KiemTraKetNoi.coKetNoi(applicationContext)) {
            hienThiThongBao(getString(R.string.thanh_toan_offline))
        }

        nutDongHoaDon.setOnClickListener { finish() }
        nutXacNhanThanhToan.setOnClickListener { xacNhanThanhToan() }
    }

    /**
     * Luu ma thao tac vao bundle khi Activity bi huy de tao lai.
     *
     * @param outState Bundle luu trang thai cua man hinh.
     */
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KHOA_MA_THAO_TAC, maThaoTac)
        bundleNhoMaThaoTac = outState
    }

    private fun khoiTaoThanhPhanGiaoDien() {
        tongSoMatHangHoaDon = findViewById(R.id.tong_so_mat_hang_hoa_don)
        tienTamTinhHoaDon = findViewById(R.id.tien_tam_tinh_hoa_don)
        tienGiamHoaDon = findViewById(R.id.tien_giam_hoa_don)
        tongCongHoaDon = findViewById(R.id.tong_cong_hoa_don)
        thongBaoHoaDon = findViewById(R.id.thong_bao_hoa_don)
        oChietKhauPhanTram = findViewById(R.id.o_chiet_khau_phan_tram)
        oChietKhauTien = findViewById(R.id.o_chiet_khau_tien)
        oTienKhachDua = findViewById(R.id.o_tien_khach_dua)
        oGhiChuHoaDon = findViewById(R.id.o_ghi_chu_hoa_don)
        khungTienKhachDua = findViewById(R.id.khung_tien_khach_dua)
        tienThuaHoaDon = findViewById(R.id.tien_thua_hoa_don)
        boChonKhachHang = findViewById(R.id.bo_chon_khach_hang)
        boChonPhuongThucThanhToan = findViewById(R.id.bo_chon_phuong_thuc_thanh_toan)
        nutXacNhanThanhToan = findViewById(R.id.nut_xac_nhan_thanh_toan)
        nutDongHoaDon = findViewById(R.id.nut_dong_hoa_don)
    }

    /** Tao adapter cho danh sach dong hang va gan vao danh sach. */
    private fun khoiTaoDanhSachDong() {
        trinhBayDongHoaDon = TrinhBayDongHoaDon()
        findViewById<RecyclerView>(R.id.danh_sach_dong_hoa_don).apply {
            layoutManager = LinearLayoutManager(this@ManHinhChiTietHoaDon)
            adapter = trinhBayDongHoaDon
        }
    }

    /**
     * Giai ma gio hang tu JSON de tranh phu thuoc Serializable.
     */
    private fun docGioHangTuIntent() {
        val chuoiGioHang = intent.getStringExtra(KHOA_GIO_HANG).orEmpty()
        val kieuDanhSach = object : TypeToken<List<MatHangGioHang>>() {}.type
        danhSachMatHang = runCatching {
            Gson().fromJson<List<MatHangGioHang>>(chuoiGioHang, kieuDanhSach)
        }.getOrNull().orEmpty()

        trinhBayDongHoaDon.capNhatDanhSach(danhSachMatHang)
    }

    /**
     * Tai danh sach khach hang va phuong thuc thanh toan len hai Spinner.
     */
    private fun ganSuKienChonThanhToan() {
        // Danh sach phuong thuc thanh toan dung ngay tu dau, khong can tai tu server.
        val tenPhuongThuc = PhuongThucThanhToan.entries.map { phuongThuc ->
            getString(
                when (phuongThuc) {
                    PhuongThucThanhToan.TIEN_MAT -> R.string.thanh_toan_tien_mat
                    PhuongThucThanhToan.CHUYEN_KHOAN -> R.string.thanh_toan_chuyen_khoan
                    PhuongThucThanhToan.THE -> R.string.thanh_toan_the
                    PhuongThucThanhToan.VI_DIEN_TU -> R.string.thanh_toan_vi_dien_tu
                }
            )
        }
        boChonPhuongThucThanhToan.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            tenPhuongThuc
        )

        // Doi phuong thuc thanh toan se an hien o nhap tien khach dua.
        boChonPhuongThucThanhToan.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    capNhatTienKhachDua()
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
            }

        taiDanhSachKhachHang()
    }

    /**
     * Tai danh sach khach hang tu server va dua vao Spinner.
     */
    private fun taiDanhSachKhachHang() {
        lifecycleScope.launch {
            danhSachKhachHang = dichVuBanHang.taiKhachHang().getOrDefault(emptyList())

            // Luon co mot muc "Khach le" de nguoi dung co the ban khong can chon.
            val tenKhachHang = mutableListOf(getString(R.string.khach_le))
            tenKhachHang.addAll(danhSachKhachHang.map { khachHang ->
                khachHang.phone?.takeIf { it.isNotBlank() }
                    ?.let { "${khachHang.name} - $it" }
                    ?: khachHang.name
            })

            boChonKhachHang.adapter = ArrayAdapter(
                this@ManHinhChiTietHoaDon,
                android.R.layout.simple_spinner_dropdown_item,
                tenKhachHang
            )
        }
    }

    /**
     * Moi thay doi giam gia deu cap nhat lai tong cong ngay lap tuc.
     */
    private fun ganSuKienTinhChietKhau() {
        oChietKhauPhanTram.doAfterTextChanged { capNhatHoaDon() }
        oChietKhauTien.doAfterTextChanged { capNhatHoaDon() }
    }

    /**
     * Moi thay doi tien khach dua deu tinh lai tien thua hoac tien thieu.
     */
    private fun ganSuKienTienKhachDua() {
        oTienKhachDua.doAfterTextChanged { capNhatTienKhachDua() }
    }

    /**
     * Tinh lai toan bo thong tin hoa don khi chiet khau thay doi.
     */
    private fun capNhatHoaDon() {
        tongTienTamTinh = danhSachMatHang.sumOf { it.thanhTien }
        val phanTram = oChietKhauPhanTram.text?.toString()?.toDoubleOrNull() ?: 0.0
        val giamTheoTien = oChietKhauTien.text?.toString()?.toDoubleOrNull() ?: 0.0

        // Chiết khấu phần trăm và giảm giá tiền không được vượt quá tạm tính.
        // Giá trị vượt quá được chặn bên dưới thay vì dừng cập nhật, để tổng
        // tiền hiển thị luôn khớp với tổng tiền sẽ gửi lên server.
        val phanTramHopLe = phanTram.coerceIn(0.0, 100.0)
        val giamHopLe = giamTheoTien.coerceAtLeast(0.0)
        val tienGiam = minOf(
            tongTienTamTinh,
            tongTienTamTinh * phanTramHopLe / 100.0 + giamHopLe
        )
        tongCongHienTai = tongTienTamTinh - tienGiam

        tongSoMatHangHoaDon.text = getString(
            R.string.so_san_pham,
            danhSachMatHang.sumOf { it.soLuong }
        )
        tienTamTinhHoaDon.text = getString(
            R.string.dong_tien_tam_tinh,
            DinhDangTien.dinhDangTien(tongTienTamTinh)
        )
        tienGiamHoaDon.text = getString(
            R.string.dong_tien_giam,
            DinhDangTien.dinhDangTien(tienGiam)
        )
        tongCongHoaDon.text = getString(
            R.string.dong_tong_cong,
            DinhDangTien.dinhDangTien(tongCongHienTai)
        )

        capNhatTienKhachDua()

        // Cảnh báo khi người dùng nhập giảm giá vượt mức cho phép.
        if (phanTram !in 0.0..100.0 || giamTheoTien < 0.0) {
            hienThiThongBao(getString(R.string.loi_giam_gia))
        } else {
            xoaThongBao()
        }
    }

    /**
     * Hien hoac an o nhap tien khach dua theo phuong thuc thanh toan.
     *
     * Chi khi thanh toan bang tien mat moi can nhap tien khach dua de tinh
     * tien thua.
     */
    private fun capNhatTienKhachDua() {
        val laTienMat = layPhuongThucThanhToan() == PhuongThucThanhToan.TIEN_MAT
        khungTienKhachDua.visibility = if (laTienMat) View.VISIBLE else View.GONE

        if (!laTienMat) {
            tienThuaHoaDon.text = ""
            return
        }

        // Neu nguoi dung chua nhap gi thi mac dinh khach da dua du tien, nen
        // tien thua bang 0. Van hien tong cong de nguoi dung biet can bao nhieu.
        val tienKhachDua = oTienKhachDua.text?.toString()?.toDoubleOrNull()
        if (tienKhachDua == null) {
            tienThuaHoaDon.text = getString(
                R.string.tien_thua_khong_co,
                DinhDangTien.dinhDangTien(0.0)
            )
            return
        }

        val tienThua = tienKhachDua - tongCongHienTai
        tienThuaHoaDon.text = if (tienThua >= 0) {
            getString(R.string.tien_thua, DinhDangTien.dinhDangTien(tienThua))
        } else {
            getString(R.string.tien_thieu, DinhDangTien.dinhDangTien(-tienThua))
        }
    }
    /**
     * Lay phuong thuc thanh toan dang chon.
     *
     * @return Enum phuong thuc thanh toan.
     */
    private fun layPhuongThucThanhToan(): PhuongThucThanhToan {
        val viTri = boChonPhuongThucThanhToan.selectedItemPosition
        return PhuongThucThanhToan.entries.getOrNull(viTri) ?: PhuongThucThanhToan.TIEN_MAT
    }

    /**
     * Lay id khach hang dang chon.
     *
     * @return Id khach hang, null neu chon khach le.
     */
    private fun layKhachHangDaChon(): String? {
        val viTri = boChonKhachHang.selectedItemPosition
        return danhSachKhachHang.getOrNull(viTri - 1)?.id
    }

    /**
     * Kiem tra du lieu thanh toan truoc khi gui len server.
     *
     * @return Chuoi loi neu khong hop le, null neu hop le.
     */
    private fun kiemTraDuLieuThanhToan(): String? {
        if (danhSachMatHang.isEmpty()) {
            return getString(R.string.loi_gio_hang_trong)
        }

        // Khi thanh toan bang tien mat, người dung có thể nhập tiền khách đưa
        // để tính tiền thừa. Nếu không nhập thì được hiểu là khách đã đưa đủ
        // tiền, nên không chặn thanh toán.
        if (layPhuongThucThanhToan() == PhuongThucThanhToan.TIEN_MAT) {
            val tienKhachDua = oTienKhachDua.text?.toString()?.toDoubleOrNull()
            // Chỉ chặn khi người dung đã nhập một số tiền mà số tiền đó ít
            // hơn tổng cộng, tức là khách chưa đủ tiền.
            if (tienKhachDua != null && tienKhachDua < tongCongHienTai) {
                return getString(R.string.loi_tien_khach_dua_khong_du)
            }
        }

        return null
    }

    /**
     * Gui yeu cau tao hoa don len server.
     */
    private fun xacNhanThanhToan() {
        val loi = kiemTraDuLieuThanhToan()
        if (loi != null) {
            hienThiThongBao(loi)
            return
        }

        // Chuyen dong hang trong gio thanh dong hang gui len server.
        val dongHang = danhSachMatHang.map { matHang ->
            DongHangHoaDon(
                productId = matHang.sanPham.id,
                quantity = matHang.soLuong,
                unitPrice = matHang.donGia,
                conversionQty = matHang.heSoQuyDoi
            )
        }

        // Ma thao tac giu nguyen qua cac lan bam lai de tranh tao trung hoa don.
        val yeuCau = YeuCauTaoHoaDon(
            operationId = maThaoTac,
            customerId = layKhachHangDaChon(),
            items = dongHang,
            paymentMethod = layPhuongThucThanhToan().maGuiLenServer,
            // Tien giam duoc lam tron ve 2 chu so thap phan vi server luu tien
            // voi kieu Decimal(15, 2). Khong lam tron se gay sai so nho khi so
            // sanh discount voi subtotal va server tra loi VALIDATION_ERROR.
            discount = lamTronHaiChuSo(tongTienTamTinh - tongCongHienTai),
            notes = oGhiChuHoaDon.text?.toString()?.trim()?.ifBlank { null }
        )

        capNhatTrangThaiDangGui(true)
        lifecycleScope.launch {
            when (val ketQua = dichVuBanHang.taoHoaDon(yeuCau)) {
                is KetQuaTaoHoaDon.ThanhCong -> {
                    capNhatTrangThaiDangGui(false)
                    moManHinhThanhCong(ketQua.hoaDon, ketQua.daTonTai)
                }

                is KetQuaTaoHoaDon.ThatBai -> {
                    capNhatTrangThaiDangGui(false)

                    // Mat mang thi luu hoa don vao database cuc lo va xep vao
                    // hang doi dong bo, thay vi bao loi cho nguoi dung.
                    if (ketQua.maLoi == MA_LOI_MAT_MANG) {
                        luuHoaDonOffline(yeuCau)
                        return@launch
                    }

                    xuLyLoiTaoHoaDon(ketQua.maLoi, ketQua.thongBao)
                }
            }
        }
    }

    /**
     * Luu hoa don tao offline vao database cuc lo va len lich dong bo.
     *
     * Khi mat mang, hoa don van duoc ghi nhan va hien thi thanh cong cho
     * nhan vien. Worker se gui hoa don len server khi co ket noi lai.
     *
     * @param yeuCau Yeu cau tao hoa don da chuan hoa.
     */
    private fun luuHoaDonOffline(yeuCau: YeuCauTaoHoaDon) {
        lifecycleScope.launch {
            val khoDuLieu = KhoDuLieuSanPham(applicationContext)

            // Ghi hoa don vao database cuc lo kem hang doi dong bo.
            khoDuLieu.luuHoaDonOffline(yeuCau, soHoaDon = null)

            // Len lich worker dong bo, worker chi chay khi co ket noi mang.
            WorkerDongBoHoaDon.lenLich(applicationContext)

            // Hien thi man hinh thanh cong voi so hoa don tam thoi.
            moManHinhThanhCong(
                hoaDon = HoaDonDaTao(
                    id = maThaoTac,
                    invoiceNumber = getString(R.string.so_hoa_don_offline),
                    subtotal = tongTienTamTinh,
                    discount = lamTronHaiChuSo(tongTienTamTinh - tongCongHienTai),
                    totalAmount = tongCongHienTai,
                    paymentMethod = layPhuongThucThanhToan().maGuiLenServer
                ),
                daTonTai = false,
                taoOffline = true
            )
        }
    }

    /**
     * Lam tien ve 2 chu so thap phan.
     *
     * Server luu tien voi kieu Decimal(15, 2), nen moi gia tri gui len deu
     * phai khop ve 2 chu so thap phan de tranh sai so khi so sanh.
     *
     * @param gia Gia tri can lam tron.
     * @return Gia tri da lam tron.
     */
    private fun lamTronHaiChuSo(gia: Double): Double {
        return Math.round(gia * 100.0) / 100.0
    }

    /**
     * Xu ly loi khi tao hoa don that bai.
     *
     * @param maLoi    Ma loi chuan tu server.
     * @param thongBao Thong bao cho nguoi dung.
     */
    private fun xuLyLoiTaoHoaDon(maLoi: String, thongBao: String) {
        // Token het han thi phai dang nhap lai ngay.
        if (maLoi == "UNAUTHORIZED" || maLoi == "HTTP_401") {
            BoNhoPhienDangNhap(applicationContext).xoaPhien()
            startActivity(
                Intent(this, DangNhap::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            )
            finish()
            return
        }

        // Khong du ton kho thi bao ro thong bao tu server de nguoi dung
        // biet can xem lai gio hang.
        if (maLoi == "STOCK_CONFLICT") {
            hienThiThongBao(
                getString(R.string.loi_thieu_ton_kho_thanh_toan, thongBao)
            )
            return
        }

        hienThiThongBao(thongBao)
    }

    /**
     * Mo man hinh thong bao thanh cong.
     *
     * @param hoaDon   Hoa don server vua tao.
     * @param daTonTai True neu hoa don da duoc tao truoc do do retry.
     */
    private fun moManHinhThanhCong(hoaDon: HoaDonDaTao, daTonTai: Boolean, taoOffline: Boolean = false) {
        // Chi phuong thuc tien mat moi co tien thua. Voi cac phuong thuc khac
        // o nhap tien khach dua dang an nen khong co so tien thua.
        //
        // Neu nguoi dung khong nhap tien khach dua thi mac dinh khach da dua
        // du tien, nen tien thua bang 0.
        val tienKhachDua = if (layPhuongThucThanhToan() == PhuongThucThanhToan.TIEN_MAT) {
            oTienKhachDua.text?.toString()?.toDoubleOrNull() ?: 0.0
        } else {
            0.0
        }

        // Tong cong phai lay tu server de khop chinh xac voi hoa don da tao,
        // vi server moi la noi chuyen don vi ban ve don vi co ban truoc khi
        // tinh tien. Dung tong cong tinh o client se lech khi lam tron.
        val tongCongThucTe = hoaDon.totalAmount

        // Tien thua khong duoc am. Khi khach dua du tien thi tien thua la 0.
        val tienThuaThucTe = (tienKhachDua - tongCongThucTe).coerceAtLeast(0.0)

        boNhanKetQuaThanhCong.launch(
            Intent(this, ManHinhHoaDonThanhCong::class.java).apply {
                putExtra(ManHinhHoaDonThanhCong.KHOA_SO_HOA_DON, hoaDon.invoiceNumber)
                putExtra(ManHinhHoaDonThanhCong.KHOA_TONG_CONG, tongCongThucTe)
                putExtra(ManHinhHoaDonThanhCong.KHOA_TIEN_THUA, tienThuaThucTe)
                putExtra(ManHinhHoaDonThanhCong.KHOA_DA_TON_TAI, daTonTai)
                putExtra(ManHinhHoaDonThanhCong.KHOA_TAO_OFFLINE, taoOffline)
            }
        )
    }

    /**
     * Bat hoac tat trang thai dang gui yeu cau.
     *
     * @param dangGui True khi dang gui yeu cau tao hoa don.
     */
    private fun capNhatTrangThaiDangGui(dangGui: Boolean) {
        nutXacNhanThanhToan.isEnabled = !dangGui
        nutDongHoaDon.isEnabled = !dangGui
        nutXacNhanThanhToan.text = getString(
            if (dangGui) R.string.dang_xac_nhan_thanh_toan else R.string.xac_nhan_thanh_toan
        )
    }

    private fun hienThiThongBao(thongBao: String) {
        thongBaoHoaDon.text = thongBao
        thongBaoHoaDon.visibility = View.VISIBLE
    }

    private fun xoaThongBao() {
        thongBaoHoaDon.text = ""
        thongBaoHoaDon.visibility = View.GONE
    }

    companion object {
        const val KHOA_GIO_HANG = "gio_hang_json"
        const val KHOA_MA_THAO_TAC = "ma_thao_tac"

        /**
         * Ma loi dung khi khong ket noi duoc voi may chu.
         *
         * Trong truong hop nay hoa don duoc luu cuc bo thay vi bao loi.
         */
        const val MA_LOI_MAT_MANG = "LOI_KET_NOI"
    }
}
