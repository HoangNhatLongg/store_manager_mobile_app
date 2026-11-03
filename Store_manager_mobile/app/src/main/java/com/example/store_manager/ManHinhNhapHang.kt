package com.example.store_manager

import android.view.LayoutInflater
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.store_manager.du_lieu.DanhMuc
import com.example.store_manager.du_lieu.DichVuNhapHang
import com.example.store_manager.du_lieu.DinhDangTien
import com.example.store_manager.du_lieu.DonViSanPham
import com.example.store_manager.du_lieu.DongHangNhap
import com.example.store_manager.du_lieu.DongHangNhapGui
import com.example.store_manager.du_lieu.KetQuaTaoPhieuNhap
import com.example.store_manager.du_lieu.NhaCungCap
import com.example.store_manager.du_lieu.SanPham
import com.example.store_manager.du_lieu.YeuCauTaoPhieuNhap
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch
import java.util.UUID

/**
 * Man hinh nhap hang, dung de lap phieu nhap tu nha cung cap.
 *
 * Luong lam viec:
 * 1. Tai danh sach nha cung cap, danh muc va san pham de doi chieu ten.
 * 2. Nguoi dung them dong hang: ten, so luong, don gia, danh muc.
 * 3. Khi ten khong khop san pham nao, hop thoai goi y se hien ra de nguoi
 *    dung chon san pham tuong tu hoac tao san pham moi.
 * 4. Luu phieu: goi POST /api/v1/imports. Server tang ton kho, cap nhat gia
 *    nhap va tao phieu trong mot transaction.
 */
class ManHinhNhapHang : ManHinhCoThanhTab() {
    /** Layout noi dung cua man hinh nhap hang. */
    override val taiNguyenNoiDung: Int = R.layout.nhap_hang

    private lateinit var boLocNhaCungCap: Spinner
    private lateinit var oGhiChu: TextInputEditText
    private lateinit var danhSachDongHang: RecyclerView
    private lateinit var thongBaoRong: TextView
    private lateinit var tongSoHang: TextView
    private lateinit var tongSoLuong: TextView
    private lateinit var tongTien: TextView
    private lateinit var nutLuuPhieu: MaterialButton
    private lateinit var nutThemDong: MaterialButton
    private lateinit var vongTai: ProgressBar

    private lateinit var dichVu: DichVuNhapHang
    private lateinit var trinhBayDongHang: TrinhBayDongHangNhap

    /** Danh sach dong hang dang soan trong phieu nhap. */
    private val danhSachDongHangTam = mutableListOf<DongHangNhap>()

    /** Danh sach nha cung cap dung de chon trong Spinner. */
    private var danhSachNhaCungCap = listOf<NhaCungCap>()

    /**
     * Danh muc dung de gan cho dong hang tao san pham moi.
     *
     * Dung danh sach co the sua doi noi tai vi adapter giu nguyen tham chieu
     * toi cung mot danh sach, khong can tao lai adapter khi tai du lieu.
     */
    private val danhSachDanhMuc = mutableListOf<DanhMuc>()

    /** Toan bo san pham hien co, dung de doi chieu ten khi nguoi dung nhap. */
    private var danhSachSanPham = listOf<SanPham>()

    /** Hop thoai goi y san pham, giu lai de biet dang mo hay da dong. */
    private var hopThoaiGoiY: AlertDialog? = null

    /**
     * True khi dang gui phieu len server.
     *
     * Trong khoang thoi gian nay khong mo hop thoai goi y de tranh nguoi dung
     * bi gian lao giua luc phieu dang duoc luu.
     */
    private var dangLuuPhieu = false

    /**
     * Khoi tao giao dien va tai du lieu.
     *
     * Lớp cha nap layout trong onStart nên mọi thành phần giao diện chỉ tồn
     * tại sau khi super.onStart() chạy xong.
     */
    override fun onStart() {
        super.onStart()

        khoiTaoThanhPhanGiaoDien()
        dichVu = DichVuNhapHang(applicationContext)
        ganSuKien()

        // Phieu moi luôn có một dòng hàng để người dùng nhập ngay. Nhưng khi
        // quay lại màn hình thì không thêm lại để giữ nguyên phần đang soạn.
        if (danhSachDongHangTam.isEmpty()) {
            themDongHang()
        } else {
            capNhatTongQuan()
        }

        taiDuLieuBanDau()
    }

    /** Ánh xạ các khu vực của màn hình vào biến có tên để hiểu. */
    private fun khoiTaoThanhPhanGiaoDien() {
        boLocNhaCungCap = findViewById(R.id.bo_loc_nha_cung_cap_nhap)
        oGhiChu = findViewById(R.id.o_ghi_chu_phieu_nhap)
        danhSachDongHang = findViewById(R.id.danh_sach_dong_hang_nhap)
        thongBaoRong = findViewById(R.id.thong_bao_rong_phieu_nhap)
        tongSoHang = findViewById(R.id.tong_so_hang_nhap)
        tongSoLuong = findViewById(R.id.tong_so_luong_nhap)
        tongTien = findViewById(R.id.tong_tien_phieu_nhap)
        nutLuuPhieu = findViewById(R.id.nut_luu_phieu_nhap)
        nutThemDong = findViewById(R.id.nut_them_dong_hang)
        vongTai = findViewById(R.id.vong_tai_nhap_hang)
    }

    /** Gắn sự kiện cho các nút và danh sách dòng hàng. */
    private fun ganSuKien() {
        nutThemDong.setOnClickListener {
            themDongHang()
        }

        nutLuuPhieu.setOnClickListener {
            luuPhieuNhap()
        }

        trinhBayDongHang = TrinhBayDongHangNhap(
            layDanhSachDanhMuc = { danhSachDanhMuc },
            layDonViGocCuaSanPham = { maSanPham ->
                layDonViGocCuaSanPham(maSanPham)
            },
            khiDoiTen = { dongHang, tenMoi ->
                xuLyDoiTen(dongHang, tenMoi)
            },
            khiHoanTatTen = { dongHang, tenMoi ->
                xuLyHoanTatTen(dongHang, tenMoi)
            },
            khiDoiSoLuong = { dongHang, soLuongMoi ->
                capNhatDongHang(dongHang) { it.copy(soLuong = soLuongMoi) }
            },
            khiDoiDonGia = { dongHang, donGiaMoi ->
                capNhatDongHang(dongHang) { it.copy(donGia = donGiaMoi) }
            },
            khiDoiDanhMuc = { dongHang, maDanhMuc ->
                capNhatDongHang(dongHang) { it.copy(maDanhMuc = maDanhMuc) }
            },
            khiDoiDonViGoc = { dongHang, donVi ->
                capNhatDongHang(dongHang) {
                    it.copy(
                        maDonViGoc = donVi.unitId,
                        heSoQuyDoi = donVi.conversionQty,
                        tenDonViGoc = donVi.unit?.name.orEmpty()
                    )
                }
            },
            khiXoaDong = { dongHang ->
                xoaDongHang(dongHang)
            }
        )

        danhSachDongHang.layoutManager = LinearLayoutManager(this)
        danhSachDongHang.adapter = trinhBayDongHang
    }

    /**
     * Tải nhà cung cấp, danh mục và sản phẩm để màn hình hoạt động.
     *
     * Tải theo thứ tự vì danh mục và sản phẩm chỉ dùng để gán cho dòng
     * hàng, nên lỗi tải không chặn việc hiển thị form.
     */
    private fun taiDuLieuBanDau() {
        capNhatTrangThaiTai(true)

        lifecycleScope.launch {
            val ketQuaNhaCungCap = dichVu.taiNhaCungCap()
            danhSachNhaCungCap = ketQuaNhaCungCap.getOrDefault(emptyList())
            capNhatBoLocNhaCungCap()

            // Danh mục nạp vào cùng danh sách mà adapter đang giữ tham chiếu,
            // nên các dòng hàng hiện có sẽ tự đọc danh mục mới.
            val ketQuaDanhMuc = dichVu.taiDanhMuc()
            danhSachDanhMuc.clear()
            danhSachDanhMuc.addAll(ketQuaDanhMuc.getOrDefault(emptyList()))
            trinhBayDongHang.lamMoiDanhMuc()

            val ketQuaSanPham = dichVu.taiToanBoSanPham()
            danhSachSanPham = ketQuaSanPham.getOrDefault(emptyList())

            capNhatTrangThaiTai(false)
            capNhatTongQuan()

            // Cảnh báo người dùng khi không tải được danh sách sản phẩm, vì
            // khi đó không thể gắn sản phẩm có sẵn cho dòng hàng.
            if (ketQuaSanPham.isFailure) {
                Toast.makeText(
                    this@ManHinhNhapHang,
                    getString(R.string.loi_tai_danh_sach_san_pham),
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    /** Nạp danh sách nhà cung cấp vào Spinner, thêm mục "Chọn nhà cung cấp" ở đầu. */
    private fun capNhatBoLocNhaCungCap() {
        val tenNhaCungCap = mutableListOf(getString(R.string.khong_chon_nha_cung_cap))
        tenNhaCungCap.addAll(danhSachNhaCungCap.map { it.name })

        boLocNhaCungCap.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            tenNhaCungCap
        )
    }

    /**
     * Thêm một dòng hàng mới vào phiếu nhập.
     *
     * Dòng mới có số lượng 1 và giá nhập 0 để người dùng nhập tiếp.
     */
    private fun themDongHang() {
        danhSachDongHangTam.add(
            DongHangNhap(
                maDong = UUID.randomUUID().toString(),
                soLuong = 1,
                donGia = 0.0
            )
        )
        capNhatTongQuan()
    }

    /**
     * Xoá một dòng hàng khỏi phiếu nhập.
     *
     * @param dongHang Dòng hàng cần xoá.
     */
    private fun xoaDongHang(dongHang: DongHangNhap) {
        danhSachDongHangTam.removeAll { it.maDong == dongHang.maDong }
        capNhatTongQuan()
    }

    /**
     * Cập nhật một dòng hàng trong danh sách và làm mới giao diện.
     *
     * @param dongHang Dòng hàng cần cập nhật.
     * @param bienDoi  Hàm tạo bản sao mới của dòng hàng.
     */
    private fun capNhatDongHang(
        dongHang: DongHangNhap,
        bienDoi: (DongHangNhap) -> DongHangNhap
    ) {
        val viTri = danhSachDongHangTam.indexOfFirst { it.maDong == dongHang.maDong }
        if (viTri < 0) return

        danhSachDongHangTam[viTri] = bienDoi(danhSachDongHangTam[viTri])
        capNhatTongQuan()
    }

    /**
     * Xử ly khi người dùng đang gõ tên sản phẩm của một dòng hàng.
     *
     * Tên được cập nhật ngay khi gõ để dòng hàng luôn khớp với nội dung
     * đang soạn. Việc tìm sản phẩm tương tự và hiện hộp thoại gợi ý được
     * hoãn lại tới khi người dùng ngừng gõ, xem [xuLyHoanTatTen].
     *
     * @param dongHang Dòng hàng đang soạn.
     * @param tenMoi   Tên sản phẩm mới người dùng nhập.
     * @return True nếu tên đã khớp một sản phẩm có sẵn.
     */
    private fun xuLyDoiTen(dongHang: DongHangNhap, tenMoi: String): Boolean {
        if (tenMoi.isBlank()) {
            capNhatDongHang(dongHang) {
                it.copy(tenSanPham = "", maSanPham = null)
            }
            return false
        }

        // Khớp chính xác (không phân biệt hoa thường) với sản phẩm có sẵn.
        val sanPhamKhop = danhSachSanPham.firstOrNull {
            it.name.equals(tenMoi, ignoreCase = true)
        }

        if (sanPhamKhop != null) {
            val donViGoc = sanPhamKhop.donViGoc
            capNhatDongHang(dongHang) {
                it.copy(
                    tenSanPham = sanPhamKhop.name,
                    maSanPham = sanPhamKhop.id,
                    donGia = sanPhamKhop.importPrice,
                    maDonViGoc = donViGoc?.unitId,
                    heSoQuyDoi = donViGoc?.conversionQty ?: 1,
                    tenDonViGoc = donViGoc?.unit?.name.orEmpty()
                )
            }
            return true
        }

        // Chưa khớp sản phẩm nào. Gán maSanPham = null để khi lưu phiếu, nếu
        // người dùng không chọn sản phẩm tương tự thì server sẽ tạo mới.
        capNhatDongHang(dongHang) {
            it.copy(tenSanPham = tenMoi, maSanPham = null)
        }
        return false
    }

    /**
     * Lấy danh sách đơn vị của sản phẩm để chọn đơn vị gốc khi nhập hàng.
     *
     * @param maSanPham Id sản phẩm, null nếu dòng hàng chưa khớp sản phẩm nào.
     * @return Danh sách đơn vị của sản phẩm, rỗng nếu không tìm thấy.
     */
    private fun layDonViGocCuaSanPham(maSanPham: String?): List<DonViSanPham> {
        if (maSanPham == null) {
            return emptyList()
        }

        return danhSachSanPham
            .firstOrNull { it.id == maSanPham }
            ?.units
            .orEmpty()
    }

    /**
     * Xử lý khi người dùng ngừng gõ tên sản phẩm và chuyển sang ô khác.
     *
     * Tại thời điểm này tên đã được chốt, nên mới tìm sản phẩm tương tự để
     * hỏi người dùng có muốn dùng lại sản phẩm có sẵn hay không.
     *
     * @param dongHang Dòng hàng đang soạn.
     * @param tenMoi   Tên sản phẩm đã chốt.
     */
    private fun xuLyHoanTatTen(dongHang: DongHangNhap, tenMoi: String) {
        // Đang lưu phiếu thì không mở hộp thoại để không làm người dùng bị
        // gián đoạn giữa lúc chờ kết quả.
        if (dangLuuPhieu) {
            return
        }

        if (tenMoi.isBlank()) {
            return
        }

        // Tên đã khớp một sản phẩm có sẵn thì không cần gợi ý.
        val sanPhamKhop = danhSachSanPham.firstOrNull {
            it.name.equals(tenMoi, ignoreCase = true)
        }

        if (sanPhamKhop != null) {
            return
        }

        val goiY = timSanPhamGoiY(tenMoi)
        if (goiY.isNotEmpty()) {
            moHopThoaiGoiY(dongHang, tenMoi, goiY)
        }
    }

    /**
     * Tìm các sản phẩm có tên hoặc SKU chứa chuỗi người dùng nhập.
     *
     * @param tenMoi Tên sản phẩm người dùng nhập.
     * @return Danh sách tối đa 5 sản phẩm tương tự.
     */
    private fun timSanPhamGoiY(tenMoi: String): List<SanPham> {
        val tuKhoa = tenMoi.lowercase().trim()

        return danhSachSanPham
            .filter {
                it.name.lowercase().contains(tuKhoa) ||
                    it.sku.lowercase().contains(tuKhoa)
            }
            .take(5)
    }

    /**
     * Mở hộp thoại gợi y sản phẩm tương tự.
     *
     * @param dongHang Dòng hàng đang soạn.
     * @param tenMoi   Tên sản phẩm người dùng nhập.
     * @param goiY     Danh sách sản phẩm tương tự để chọn.
     */
    private fun moHopThoaiGoiY(
        dongHang: DongHangNhap,
        tenMoi: String,
        goiY: List<SanPham>
    ) {
        val giaoDien = LayoutInflater.from(this)
            .inflate(R.layout.hop_thoai_goi_y_san_pham, null)

        giaoDien.findViewById<TextView>(R.id.mo_ta_goi_y_san_pham).text =
            getString(R.string.mo_ta_goi_y_san_pham, tenMoi)

        val danhSachGoiY = giaoDien.findViewById<RecyclerView>(R.id.danh_sach_goi_y_san_pham)
        val trinhBayGoiY = TrinhBaySanPhamGoiY { sanPham ->
            hopThoaiGoiY?.dismiss()
            capNhatDongHang(dongHang) {
                it.copy(
                    tenSanPham = sanPham.name,
                    maSanPham = sanPham.id,
                    donGia = sanPham.importPrice
                )
            }

            // Dong hang dang soan nen khong danh dau lai duoc. Gia tri duoc
            // gan thang len o nhap de nguoi dung thay ten, gia va trang thai
            // khop ngay tren man hinh.
            trinhBayDongHang.apDungSanPhamDaChon(
                maDong = dongHang.maDong,
                tenSanPham = sanPham.name,
                donGia = sanPham.importPrice
            )
        }
        danhSachGoiY.layoutManager = LinearLayoutManager(this)
        danhSachGoiY.adapter = trinhBayGoiY
        trinhBayGoiY.capNhatDanhSach(goiY)

        val hopThoai = MaterialAlertDialogBuilder(this)
            .setView(giaoDien)
            .setPositiveButton(R.string.tao_san_pham_moi) { _, _ ->
                // Người dùng chọn tạo sản phẩm mới, dòng hàng giữ nguyên
                // maSanPham = null để server tạo sản phẩm khi lưu phiếu.
            }
            .setNegativeButton(R.string.dong_hoa_don, null)
            .create()
        hopThoaiGoiY = hopThoai
        hopThoai.show()
    }

    /**
     * Kiểm tra dữ liệu phiếu trước khi gửi lên server.
     *
     * @return Danh sách dòng hàng hợp lệ, null nếu có lỗi.
     */
    private fun kiemTraDuLieu(): List<DongHangNhap>? {
        // Nhà cung cấp là thông tin bắt buộc của phiếu nhập.
        if (layNhaCungCapDaChon() == null) {
            Toast.makeText(
                this,
                getString(R.string.loi_chua_chon_nha_cung_cap),
                Toast.LENGTH_SHORT
            ).show()
            return null
        }

        // Chỉ lấy các dòng hàng đã nhập tên sản phẩm.
        val dongCoTen = danhSachDongHangTam.filter { it.coTen }

        if (dongCoTen.isEmpty()) {
            Toast.makeText(
                this,
                getString(R.string.loi_chua_co_dong_hang),
                Toast.LENGTH_SHORT
            ).show()
            return null
        }

        // Số lượng và đơn giá phải lớn hơn 0.
        val dongKhongHopLe = dongCoTen.filter { it.soLuong <= 0 || it.donGia <= 0 }
        if (dongKhongHopLe.isNotEmpty()) {
            Toast.makeText(
                this,
                getString(R.string.loi_so_luong_hoac_don_gia_khong_hop_le),
                Toast.LENGTH_LONG
            ).show()
            return null
        }

        // Sản phẩm đã có trong hệ thống phải có đơn vị gốc để nhập hàng.
        // Sản phẩm mới sẽ được server tạo kèm đơn vị khi lưu phiếu.
        val dongChuaCoDonVi = dongCoTen.filter { dongHang ->
            dongHang.maSanPham != null && dongHang.maDonViGoc == null
        }
        if (dongChuaCoDonVi.isNotEmpty()) {
            Toast.makeText(
                this,
                getString(R.string.loi_chua_cau_hinhinh_don_vi_nhap),
                Toast.LENGTH_LONG
            ).show()
            return null
        }

        return dongCoTen
    }

    /**
     * Lấy nhà cung cấp đang chọn trong Spinner.
     *
     * @return Nhà cung cấp hoặc null nếu chưa chọn.
     */
    private fun layNhaCungCapDaChon(): NhaCungCap? {
        val viTri = boLocNhaCungCap.selectedItemPosition - 1
        return danhSachNhaCungCap.getOrNull(viTri)
    }

    /**
     * Lưu phiếu nhập lên server.
     *
     * Sau khi lưu thành công, form được xoá để người dùng nhập phiếu mới.
     */
    private fun luuPhieuNhap() {
        val dongCoTen = kiemTraDuLieu() ?: return
        val nhaCungCap = layNhaCungCapDaChon()

        val yeuCau = YeuCauTaoPhieuNhap(
            supplierId = nhaCungCap?.id,
            notes = oGhiChu.text?.toString()?.trim()?.takeIf { it.isNotEmpty() },
            items = dongCoTen.map { dongHang ->
                val donViGoc = layDonViGocCuaSanPham(dongHang.maSanPham)
                    .firstOrNull { it.unitId == dongHang.maDonViGoc }

                DongHangNhapGui(
                    productId = dongHang.maSanPham,
                    productName = dongHang.tenSanPham,
                    categoryId = dongHang.maDanhMuc,
                    quantity = dongHang.soLuong,
                    unitPrice = dongHang.donGia,
                    unitId = dongHang.maDonViGoc,
                    baseUnitId = donViGoc
                        ?.let { donVi ->
                            layDonViGocCuaSanPham(dongHang.maSanPham)
                                .firstOrNull { it.conversionQty == 1 }
                                ?.unitId
                        },
                    conversionQty = dongHang.heSoQuyDoi
                )
            }
        )

        capNhatTrangThaiTai(true)
        nutLuuPhieu.isEnabled = false
        dangLuuPhieu = true

        lifecycleScope.launch {
            when (val ketQua = dichVu.taoPhieuNhap(yeuCau)) {
                is KetQuaTaoPhieuNhap.ThanhCong -> {
                    capNhatTrangThaiTai(false)
                    nutLuuPhieu.isEnabled = true
                    dangLuuPhieu = false
                    hienThiKetQuaLuu(ketQua.phieu.invoiceNumber, ketQua.phieu.totalAmount)
                    xoaFormSauKhiLuu()
                }

                is KetQuaTaoPhieuNhap.ThatBai -> {
                    capNhatTrangThaiTai(false)
                    nutLuuPhieu.isEnabled = true
                    dangLuuPhieu = false
                    Toast.makeText(
                        this@ManHinhNhapHang,
                        ketQua.thongBao,
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    /**
     * Hiện hộp thoại thông báo kết quả lưu phiếu.
     *
     * @param soPhieu  Số phiếu nhập do server sinh.
     * @param tongTien Tổng tiền của phiếu nhập.
     */
    private fun hienThiKetQuaLuu(soPhieu: String, tongTien: Double) {
        val noiDung = getString(R.string.noi_dung_luu_phieu_nhap, soPhieu) +
            "\n" +
            getString(
                R.string.tong_tien_phieu_nhap_lai,
                DinhDangTien.dinhDangTien(tongTien)
            )

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.nhap_hang_thanh_cong)
            .setMessage(noiDung)
            .setPositiveButton(R.string.dong_hoa_don, null)
            .show()
    }

    /** Xoá form sau khi lưu thành công để người dùng nhập phiếu mới. */
    private fun xoaFormSauKhiLuu() {
        danhSachDongHangTam.clear()
        oGhiChu.setText("")
        themDongHang()
    }

    /** Cập nhật phần tổng quan: số hàng, số lượng và tổng tiền. */
    private fun capNhatTongQuan() {
        val dongCoTen = danhSachDongHangTam.filter { it.coTen }
        val tongSoLuongHang = dongCoTen.sumOf { it.soLuong }
        val tongTienHang = dongCoTen.sumOf { it.thanhTien }

        tongSoHang.text = dongCoTen.size.toString()
        tongSoLuong.text = tongSoLuongHang.toString()
        tongTien.text = DinhDangTien.dinhDangTien(tongTienHang)

        thongBaoRong.visibility = if (dongCoTen.isEmpty()) View.VISIBLE else View.GONE

        // Khi đang lưu phiếu thì giữ nguyên trạng thái vô hiệu hoá nút.
        if (!dangLuuPhieu) {
            nutLuuPhieu.isEnabled = dongCoTen.isNotEmpty()
        }

        trinhBayDongHang.capNhatDanhSach(danhSachDongHangTam)
    }

    /**
     * Bật hoặc tắt vòng tải và nút thêm dòng.
     *
     * @param dangTai True nếu đang tải dữ liệu.
     */
    private fun capNhatTrangThaiTai(dangTai: Boolean) {
        vongTai.visibility = if (dangTai) View.VISIBLE else View.GONE
        nutThemDong.isEnabled = !dangTai
    }
}
