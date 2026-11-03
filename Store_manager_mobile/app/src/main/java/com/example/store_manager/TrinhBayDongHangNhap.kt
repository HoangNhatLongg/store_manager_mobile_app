package com.example.store_manager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.Spinner
import android.widget.TextView
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.example.store_manager.du_lieu.DanhMuc
import com.example.store_manager.du_lieu.DinhDangTien
import com.example.store_manager.du_lieu.DonViSanPham
import com.example.store_manager.du_lieu.DongHangNhap

/**
 * Adapter hien cac dong hang trong phieu nhap.
 *
 * Luu y quan trong: khong duoc goi notifyDataSetChanged() khi nguoi dung
 * dang nhap ten, so luong hoac don gia, vi lenh nay huy toan bo ViewHolder
 * dang hien thi. O nhap se bi reset ve gia tri cu, con tro nhip nhay va nguoi
 * dung mat so lieu vua nhap.
 *
 * Vi vay adapter chi danh dau lai dong dang sua doi thay vi tai tao toan bo
 * danh sach, va dong dang nguoi dung dang nhap duoc bo qua hoan toan. Cac
 * thay doi cua dong do duoc gan lai truc tiep tren giao dien.
 */
class TrinhBayDongHangNhap(
    private val layDanhSachDanhMuc: () -> List<DanhMuc>,
    private val layDonViGocCuaSanPham: (String?) -> List<DonViSanPham>,
    private val khiDoiTen: (DongHangNhap, String) -> Boolean,
    private val khiHoanTatTen: (DongHangNhap, String) -> Unit,
    private val khiDoiSoLuong: (DongHangNhap, Int) -> Unit,
    private val khiDoiDonGia: (DongHangNhap, Double) -> Unit,
    private val khiDoiDanhMuc: (DongHangNhap, String?) -> Unit,
    private val khiDoiDonViGoc: (DongHangNhap, DonViSanPham) -> Unit,
    private val khiXoaDong: (DongHangNhap) -> Unit
) : RecyclerView.Adapter<TrinhBayDongHangNhap.KhungDongHang>() {
    private val danhSach = mutableListOf<DongHangNhap>()

    /**
     * Ma dong hang cua duong dang duoc nguoi dung sua.
     *
     * Dong nay duoc cap nhat truc tiep tren giao dien thay vi danh dau lai,
     * de khong lam mat con tro nhap va khong dong ban phim ao.
     */
    private var maDongDangSua: String? = null

    /**
     * Danh sach ViewHolder theo ma dong hang.
     *
     * Dung de gan truc tiep gia tri khi nguoi dung chon san pham tu hop
     * thoai goi y, vi dong dang sua thi khong the danh dau lai de tranh
     * lam mat con tro nhap.
     */
    private val khungTheoMaDong = mutableMapOf<String, KhungDongHang>()

    /**
     * Lay dong hang hien tai theo ma dong.
     *
     * @param maDong Ma dong can tim.
     * @return Dong hang dang co trong danh sach, null neu da bi xoa.
     */
    private fun layDongHienTai(maDong: String): DongHangNhap? {
        return danhSach.firstOrNull { it.maDong == maDong }
    }

    /**
     * Làm mới danh sách danh mục hiển thị trên các dòng hàng.
     *
     * Danh mục được tải sau khi dựng danh sách nên cần gọi lại hàm này để
     * các dòng đang hiển thị nhận được danh mục mới.
     */
    fun lamMoiDanhMuc() {
        notifyDataSetChanged()
    }

    /**
     * Cap nhat danh sach dong hang can hien thi.
     *
     * Chi danh dau lai nhung dong thay doi thuc su, tranh goi
     * notifyDataSetChanged() lam mat focus cua o nhap dang duoc nguoi dung
     * thao tac.
     *
     * @param danhSachMoi Danh sach dong hang moi.
     */
    fun capNhatDanhSach(danhSachMoi: List<DongHangNhap>) {
        // Giu lai danh sach cu de so sanh tung dong. Neu clear truoc khi so
        // sanh thi hai danh sach luon giong nhau va dong bi thay doi se khong
        // bao gio duoc danh dau lai.
        val danhSachCu = danhSach.toList()
        val soDongCu = danhSachCu.size
        val soDongMoi = danhSachMoi.size
        danhSach.clear()
        danhSach.addAll(danhSachMoi)

        when {
            // Danh sach rong, bo sung het cac dong dang hien thi.
            soDongCu == 0 -> notifyItemRangeInserted(0, soDongMoi)

            // Danh sach rong, xoa het cac dong dang hien thi.
            soDongMoi == 0 -> notifyItemRangeRemoved(0, soDongCu)

            // So dong giong nhau, chi danh dau lai dong bi thay doi.
            else -> {
                val soDongChung = minOf(soDongCu, soDongMoi)
                for (chiSo in 0 until soDongChung) {
                    // Dong dang duoc sua thi cap nhat truc tiep tren giao dien
                    // de giu nguyen con tro nhap.
                    if (danhSachMoi[chiSo].maDong == maDongDangSua) {
                        continue
                    }

                    if (danhSachCu[chiSo] != danhSachMoi[chiSo]) {
                        notifyItemChanged(chiSo)
                    }
                }
                // Them dong moi o cuoi danh sach.
                if (soDongMoi > soDongCu) {
                    notifyItemRangeInserted(soDongCu, soDongMoi - soDongCu)
                }
                // Xoa dong thua o cuoi danh sach.
                if (soDongMoi < soDongCu) {
                    notifyItemRangeRemoved(soDongMoi, soDongCu - soDongMoi)
                }
            }
        }
    }

    override fun onCreateViewHolder(cha: ViewGroup, loai: Int): KhungDongHang {
        val view = LayoutInflater.from(cha.context)
            .inflate(R.layout.dong_hang_nhap, cha, false)
        val khung = KhungDongHang(view)

        // Cac bo theo doi duoc gan mot lan duy nhat khi tao ViewHolder.
        // Neu gan lai moi lan bind, se co nhieu bo theo doi chay song song
        // va gay loi cap nhat trung lap.
        khung.ganBoTheoDoi()
        return khung
    }

    override fun onBindViewHolder(khung: KhungDongHang, viTri: Int) {
        khung.ganDuLieu(danhSach[viTri])
        khungTheoMaDong[danhSach[viTri].maDong] = khung
    }

    override fun getItemCount(): Int = danhSach.size

    override fun onViewRecycled(holder: KhungDongHang) {
        super.onViewRecycled(holder)
        holder.huyTacHoanTatTen()
        khungTheoMaDong.remove(holder.maDongHienTai)
    }

    /**
     * Áp dụng sản phẩm người dùng chọn từ hộp thoại gợi ý lên dòng hàng.
     *
     * Dòng hàng lúc này thường đang được người dùng soạn nên không thể gọi
     * notifyItemChanged, vì làm vậy sẽ mất con trỏ nhập và đóng bàn phím ảo.
     * Vì vậy giá trị được gán thẳng lên ô nhập đang hiển thị.
     *
     * @param maDong      Mã dòng hàng cần cập nhật.
     * @param tenSanPham  Tên sản phẩm được chọn.
     * @param donGia      Giá nhập của sản phẩm được chọn.
     */
    fun apDungSanPhamDaChon(maDong: String, tenSanPham: String, donGia: Double) {
        khungTheoMaDong[maDong]?.apDungSanPhamDaChon(tenSanPham, donGia)
    }

    companion object {
        /**
         * Do dai cho ngung go truoc khi hien hop thoai goi y san pham.
         *
         * Thoi gian cho du de nguoi dung go xong ten truoc khi popup hien len.
         */
        private const val DO_DI_CHO_HIEN_GOI_Y = 600L
    }

    inner class KhungDongHang(view: View) : RecyclerView.ViewHolder(view) {
        private val tenSanPham: EditText = view.findViewById(R.id.o_ten_san_pham_nhap)
        private val trangThaiKhop: TextView = view.findViewById(R.id.trang_thai_khop_san_pham_nhap)
        private val boLocDanhMuc: Spinner = view.findViewById(R.id.bo_loc_danh_muc_dong_hang_nhap)
        private val boLocDonViGoc: Spinner = view.findViewById(R.id.bo_loc_don_vi_goc_dong_hang_nhap)
        private val ghiChuQuyDoi: TextView = view.findViewById(R.id.ghi_chu_quy_doi_dong_hang_nhap)
        private val soLuong: EditText = view.findViewById(R.id.o_so_luong_nhap)
        private val donGia: EditText = view.findViewById(R.id.o_don_gia_nhap)
        private val thanhTien: TextView = view.findViewById(R.id.thanh_tien_dong_hang_nhap)
        private val nutXoa: View = view.findViewById(R.id.nut_xoa_dong_hang_nhap)

        /** Ma dong hang dang duoc gan cho ViewHolder nay. */
        var maDongHienTai: String = ""

        /**
         * Tac hoan tat viec go ten, dung de hien hop thoai goi y sau khi
         * nguoi dung ngung go mot luc.
         */
        private val tacHoanTatTen = Runnable {
            val dong = layDongHienTai(maDongHienTai) ?: return@Runnable
            val tenMoi = tenSanPham.text.toString().trim()

            // Khi o nhap mat focus moi chot lai ten va hien goi y.
            khiHoanTatTen(dong, tenMoi)
        }

        /**
         * Gan cac bo theo doi su kien cho cac o nhap.
         *
         * Ham nay duoc goi mot lan duy nhat khi tao ViewHolder.
         */
        fun ganBoTheoDoi() {
            // Ten san pham: gia tri duoc gui len ngay khi go de dong hang luon
            // khop voi noi dung dang soan. Hop thoai goi y chi mo ra khi nguoi
            // dung ngung go va chuyen sang o khac.
            tenSanPham.doAfterTextChanged { noiDung ->
                val dong = layDongHienTai(maDongHienTai) ?: return@doAfterTextChanged
                val tenMoi = noiDung?.toString().orEmpty().trim()

                if (tenMoi != dong.tenSanPham) {
                    val daKhop = khiDoiTen(dong, tenMoi)
                    capNhatTrangThaiKhop(daKhop, tenMoi.isNotBlank())

                    // Khi ten khop san pham co san, don gia duoc gan theo gia
                    // nhap cua san pham do. Dong nay dang duoc sua nen khong
                    // gan lai qua notifyItemChanged, phai gan truc tiep.
                    val dongMoi = layDongHienTai(maDongHienTai)
                    if (dongMoi != null && dongMoi.donGia != dong.donGia) {
                        val donGiaMoi =
                            DinhDangTien.taoChuoiChoOCoNhap(dongMoi.donGia)
                        if (donGia.text.toString() != donGiaMoi) {
                            donGia.setText(donGiaMoi)
                        }
                        capNhatThanhTien(dongMoi.soLuong, dongMoi.donGia)
                    }

                    // Chi hien hop thoai goi y khi nguoi dung ngung go mot luc,
                    // tranh hien popup lien tuc khi dang go.
                    if (daKhop) {
                        itemView.removeCallbacks(tacHoanTatTen)
                    } else {
                        itemView.removeCallbacks(tacHoanTatTen)
                        itemView.postDelayed(tacHoanTatTen, DO_DI_CHO_HIEN_GOI_Y)
                    }
                }
            }

            tenSanPham.setOnFocusChangeListener { _, coFocus ->
                if (coFocus) {
                    maDongDangSua = maDongHienTai
                } else {
                    maDongDangSua = null
                }
            }

            // So luong: cap nhat ngay khi go de khong bo so lieu khi nguoi dung
            // bam "Luu phieu" ngay sau khi nhap xong.
            soLuong.doAfterTextChanged { noiDung ->
                val dong = layDongHienTai(maDongHienTai) ?: return@doAfterTextChanged
                val soLuongMoi = noiDung?.toString().orEmpty().trim().toIntOrNull()
                    ?: return@doAfterTextChanged

                if (soLuongMoi != dong.soLuong) {
                    khiDoiSoLuong(dong, soLuongMoi)
                    capNhatThanhTien(soLuongMoi, dong.donGia)
                }
            }

            soLuong.setOnFocusChangeListener { _, coFocus ->
                if (coFocus) {
                    maDongDangSua = maDongHienTai
                } else {
                    maDongDangSua = null
                    xuLyNhapSoLuong(layDongHienTai(maDongHienTai))
                }
            }

            // Don gia: cap nhat ngay khi go.
            donGia.doAfterTextChanged { noiDung ->
                val dong = layDongHienTai(maDongHienTai) ?: return@doAfterTextChanged
                val donGiaMoi = DinhDangTien.chuyenChuoiSangTien(noiDung?.toString())

                if (donGiaMoi != dong.donGia) {
                    khiDoiDonGia(dong, donGiaMoi)
                    capNhatThanhTien(dong.soLuong, donGiaMoi)
                }
            }

            donGia.setOnFocusChangeListener { _, coFocus ->
                if (coFocus) {
                    maDongDangSua = maDongHienTai
                } else {
                    maDongDangSua = null
                    xuLyNhapDonGia(layDongHienTai(maDongHienTai))
                }
            }
        }

        /**
         * Cap nhat thanh tien cua dong hang ngay tren giao dien.
         *
         * @param soLuong  So luong moi.
         * @param donGia   Don gia moi.
         */
        private fun capNhatThanhTien(soLuong: Int, donGia: Double) {
            thanhTien.text = DinhDangTien.dinhDangTien(soLuong * donGia)
        }

        /**
         * Huy tac hoan tat go ten khi dong hang bi tai lai trong danh sach.
         */
        fun huyTacHoanTatTen() {
            itemView.removeCallbacks(tacHoanTatTen)
        }

        /**
         * Gan san pham nguoi dung chon tu hop thoai goi y vao dong hang.
         *
         * @param tenSanPham Ten san pham duoc chon.
         * @param donGia     Gia nhap cua san pham duoc chon.
         */
        fun apDungSanPhamDaChon(tenSanPham: String, donGia: Double) {
            if (this.tenSanPham.text.toString() != tenSanPham) {
                this.tenSanPham.setText(tenSanPham)
            }

            val donGiaHienThi = DinhDangTien.taoChuoiChoOCoNhap(donGia)
            if (this.donGia.text.toString() != donGiaHienThi) {
                this.donGia.setText(donGiaHienThi)
            }

            // Dong hang da co san pham khop san nen hien thi trang thai
            // "da khop" thay vi "se tao san pham moi".
            capNhatTrangThaiKhop(daKhop = true, coTen = true)

            val soLuongHienTai = layDongHienTai(maDongHienTai)?.soLuong ?: 1
            capNhatThanhTien(soLuongHienTai, donGia)
        }

        /**
         * Hien thi trang thai khop san pham cua dong hang.
         *
         * @param daKhop   True neu ten da khop san pham co san.
         * @param coTen    True neu dong hang da co ten san pham.
         */
        private fun capNhatTrangThaiKhop(daKhop: Boolean, coTen: Boolean) {
            val boiCanh = itemView.context

            when {
                daKhop -> {
                    trangThaiKhop.visibility = View.VISIBLE
                    trangThaiKhop.text = boiCanh.getString(R.string.da_khop_san_pham)
                    trangThaiKhop.setTextColor(boiCanh.getColor(R.color.xanh_dam))
                }

                coTen -> {
                    trangThaiKhop.visibility = View.VISIBLE
                    trangThaiKhop.text = boiCanh.getString(R.string.se_tao_san_pham_moi)
                    trangThaiKhop.setTextColor(boiCanh.getColor(R.color.vang_chu))
                }

                else -> {
                    trangThaiKhop.visibility = View.GONE
                }
            }
        }

        /**
         * Gan du lieu cua mot dong hang len giao dien.
         *
         * @param dongHang Dong hang can hien thi.
         */
        fun ganDuLieu(dongHang: DongHangNhap) {
            val boiCanh = itemView.context
            maDongHienTai = dongHang.maDong

            // Chi gan lai gia tri o nhap khi nguoi dung khong dang sua o nay.
            // Neu dang sua thi giu nguyen noi dung nguoi dung vua nhap.
            if (dongHang.maDong != maDongDangSua) {
                val tenHienThi = dongHang.tenSanPham
                if (tenSanPham.text.toString() != tenHienThi) {
                    tenSanPham.setText(tenHienThi)
                }

                val soLuongHienThi = dongHang.soLuong.toString()
                if (soLuong.text.toString() != soLuongHienThi) {
                    soLuong.setText(soLuongHienThi)
                }

                val donGiaHienThi = DinhDangTien.taoChuoiChoOCoNhap(dongHang.donGia)
                if (donGia.text.toString() != donGiaHienThi) {
                    donGia.setText(donGiaHienThi)
                }
            }

            // Trang thai khop giup nguoi dung biet dong hang se cap nhat san pham
            // nao hay se tao san pham moi khi luu phieu.
            capNhatTrangThaiKhop(
                daKhop = dongHang.maSanPham != null,
                coTen = dongHang.tenSanPham.isNotBlank()
            )

            // Spinner danh muc: muc dau tien la "Không chọn".
            // Listener duoc gan sau khi chon vi tri de tranh su kien chon
            // phat sinh tu viec gan du lieu bi ghi nhan la thao tac cua nguoi
            // dung va gan danh muc sai.
            boLocDanhMuc.onItemSelectedListener = null

            val danhSachDanhMucHienTai = layDanhSachDanhMuc()
            val tenDanhMuc = mutableListOf(boiCanh.getString(R.string.khong_chon_danh_muc))
            tenDanhMuc.addAll(danhSachDanhMucHienTai.map { it.name })
            boLocDanhMuc.adapter = ArrayAdapter(
                boiCanh,
                android.R.layout.simple_spinner_dropdown_item,
                tenDanhMuc
            )

            // Chon lai danh muc dang goc cua dong hang.
            val viTriDanhMuc = if (dongHang.maDanhMuc == null) {
                0
            } else {
                danhSachDanhMucHienTai.indexOfFirst { it.id == dongHang.maDanhMuc } + 1
            }
            boLocDanhMuc.setSelection(viTriDanhMuc.coerceAtLeast(0))

            boLocDanhMuc.onItemSelectedListener =
                object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: android.widget.AdapterView<*>?,
                        view: View?,
                        viTri: Int,
                        id: Long
                    ) {
                        val maDanhMuc = danhSachDanhMucHienTai.getOrNull(viTri - 1)?.id

                        // Chi gui thay doi khi gia tri thuc su khac voi gia tri
                        // dang goc cua dong hang.
                        if (maDanhMuc != dongHang.maDanhMuc) {
                            khiDoiDanhMuc(dongHang, maDanhMuc)
                        }
                    }

                    override fun onNothingSelected(
                        parent: android.widget.AdapterView<*>?
                    ) = Unit
                }

            capNhatDonViGoc(dongHang)

            thanhTien.text = DinhDangTien.dinhDangTien(dongHang.thanhTien)

            nutXoa.setOnClickListener {
                khiXoaDong(dongHang)
            }
        }

        /**
         * Cap nhat Spinner chon don vi goc cua dong hang.
         *
         * Danh sach don vi duoc lay tu san pham da khop. Neu san pham chua co
         * don vi nao thi Spinner bi khoa va hien thi thong bao nhac nguoi dung
         * cau hinhinh don vi truoc khi nhap.
         *
         * @param dongHang Dong hang dang hien thi.
         */
        private fun capNhatDonViGoc(dongHang: DongHangNhap) {
            val boiCanh = itemView.context
            val donVis = layDonViGocCuaSanPham(dongHang.maSanPham)

            boLocDonViGoc.onItemSelectedListener = null

            if (donVis.isEmpty()) {
                boLocDonViGoc.adapter = ArrayAdapter(
                    boiCanh,
                    android.R.layout.simple_spinner_dropdown_item,
                    listOf(boiCanh.getString(R.string.chua_cau_hinhinh_don_vi_nhap))
                )
                boLocDonViGoc.isEnabled = false
                ghiChuQuyDoi.visibility = View.VISIBLE
                ghiChuQuyDoi.text = boiCanh.getString(R.string.loi_chua_cau_hinhinh_don_vi_nhap)
                return
            }

            boLocDonViGoc.isEnabled = true

            val tenDonVi = donVis.map { donVi ->
                if (donVi.conversionQty > 1) {
                    "${donVi.unit?.name ?: donVi.unitId} (1 = ${donVi.conversionQty})"
                } else {
                    donVi.unit?.name ?: donVi.unitId
                }
            }

            boLocDonViGoc.adapter = ArrayAdapter(
                boiCanh,
                android.R.layout.simple_spinner_dropdown_item,
                tenDonVi
            )

            // Chon lai don vi goc dang dung cua dong hang.
            val viTri = donVis.indexOfFirst { it.unitId == dongHang.maDonViGoc }
            val viTriChon = if (viTri >= 0) {
                viTri
            } else {
                donVis.indexOfFirst { it.isPurchase }.takeIf { it >= 0 }
                    ?: donVis.indexOfFirst { it.conversionQty == 1 }.takeIf { it >= 0 }
                    ?: 0
            }
            boLocDonViGoc.setSelection(viTriChon.coerceIn(0, donVis.size - 1))

            boLocDonViGoc.onItemSelectedListener =
                object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(
                        parent: android.widget.AdapterView<*>?,
                        view: View?,
                        viTri: Int,
                        id: Long
                    ) {
                        val donVi = donVis.getOrNull(viTri) ?: return

                        // Chi gui thay doi khi don vi thuc su khac don vi dang dung.
                        if (donVi.unitId != dongHang.maDonViGoc) {
                            khiDoiDonViGoc(dongHang, donVi)
                        }
                    }

                    override fun onNothingSelected(
                        parent: android.widget.AdapterView<*>?
                    ) = Unit
                }

            capNhatGhiChuQuyDoi(dongHang, donVis.getOrNull(viTriChon))
        }

        /**
         * Cap nhat ghi chu quy doi duoi dong hang.
         *
         * Ghi chu cho nguoi dung biet 1 don vi goc tuong duong bao nhieu don vi
         * ban le co ban va tong so luong quy doi sau khi nhap.
         *
         * @param dongHang Dong hang dang hien thi.
         * @param donVi    Don vi goc dang chon, null neu khong co.
         */
        private fun capNhatGhiChuQuyDoi(dongHang: DongHangNhap, donVi: DonViSanPham?) {
            if (donVi == null || donVi.conversionQty <= 1) {
                ghiChuQuyDoi.visibility = View.GONE
                return
            }

            val boiCanh = itemView.context
            val donViCoBan = layDonViGocCuaSanPham(dongHang.maSanPham)
                .firstOrNull { it.conversionQty == 1 }

            val tenDonViCoBan = donViCoBan?.unit?.abbreviation ?: donVi.unit?.abbreviation.orEmpty()
            val soLuong = dongHang.soLuong

            ghiChuQuyDoi.visibility = View.VISIBLE
            ghiChuQuyDoi.text = boiCanh.getString(
                R.string.thong_bao_quy_doi_don_vi_goc,
                donVi.unit?.name ?: donVi.unitId,
                donVi.conversionQty,
                tenDonViCoBan,
                soLuong * donVi.conversionQty
            )
        }

        /**
         * Doc so luong nguoi dung nhap va bao Activity cap nhat dong hang.
         *
         * Neu o nhap trong thi giu nguyen so luong hien tai thay vi coi la 0.
         */
        private fun xuLyNhapSoLuong(dongHang: DongHangNhap?) {
            val dong = dongHang ?: return
            val noiDung = soLuong.text.toString().trim()

            // O nhap trong nghia la nguoi dung chua nhap gi. Giu nguyen gia
            // tri cu de khong lam mat du lieu dang soan.
            if (noiDung.isEmpty()) {
                soLuong.setText(dong.soLuong.toString())
                return
            }

            // Neu khong phai so thi giu nguyen gia tri cu.
            val soLuongMoi = noiDung.toIntOrNull() ?: run {
                soLuong.setText(dong.soLuong.toString())
                return
            }

            if (soLuongMoi != dong.soLuong) {
                khiDoiSoLuong(dong, soLuongMoi)
            }
        }

        /**
         * Doc don gia nguoi dung nhap va bao Activity cap nhat dong hang.
         *
         * Don gia khong hop le thi giu nguyen gia tri cu, tranh lam mat du lieu.
         */
        private fun xuLyNhapDonGia(dongHang: DongHangNhap?) {
            val dong = dongHang ?: return
            val noiDung = donGia.text.toString().trim()

            if (noiDung.isEmpty()) {
                donGia.setText(DinhDangTien.taoChuoiChoOCoNhap(dong.donGia))
                return
            }

            val donGiaMoi = DinhDangTien.chuyenChuoiSangTien(noiDung)
            if (donGiaMoi != dong.donGia) {
                khiDoiDonGia(dong, donGiaMoi)
            }
        }
    }
}
