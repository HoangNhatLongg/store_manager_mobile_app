package com.example.store_manager

import android.content.Intent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.store_manager.du_lieu.DichVuDoanhThu
import com.example.store_manager.du_lieu.DinhDangTien
import com.example.store_manager.du_lieu.DoanhThuTheoKy
import com.example.store_manager.du_lieu.KetQuaDoanhThu
import com.example.store_manager.du_lieu.PhanHoiDoanhThuNgay
import com.example.store_manager.du_lieu.TongHopDoanhThu
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Man hinh bao cao doanh thu, bam sat giao dien web `/revenue`.
 *
 * Man hinh hoat dong theo 3 cap: nam -> thang -> ngay. Nguoi dung cham vao
 * cot tren bieu do de drill-down xuong cap tiep theo.
 */
class ManHinhDoanhThu : ManHinhCoThanhTab() {
    override val taiNguyenNoiDung: Int = R.layout.doanh_thu

    private lateinit var boLocNam: Spinner
    private lateinit var duongDanDoanhThu: TextView
    private lateinit var nutQuayLai: MaterialButton
    private lateinit var vongTai: ProgressBar
    private lateinit var thongBaoLoi: TextView

    // Khuc vuc hien thi theo nam.
    private lateinit var khuVucNam: View
    private lateinit var bieuDoNam: BieuDoCot
    private lateinit var giaTriTongDoanhThuNam: TextView
    private lateinit var giaTriTongLoiNhuan: TextView
    private lateinit var giaTriSoDonHang: TextView
    private lateinit var giaTriTrungBinhThang: TextView
    private lateinit var tieuDeBieuDoNam: TextView

    // Khuc vuc hien thi theo thang.
    private lateinit var khuVucThang: View
    private lateinit var bieuDoThang: BieuDoCot
    private lateinit var danhSachNgayTrongThang: RecyclerView
    private lateinit var giaTriDoanhThuThang: TextView
    private lateinit var giaTriLoiNhuanThang: TextView
    private lateinit var giaTriSoDonThang: TextView
    private lateinit var tieuDeBieuDoThang: TextView

    // Khuc vuc hien thi theo ngay.
    private lateinit var khuVucNgay: View
    private lateinit var danhSachSanPhamDaBan: RecyclerView
    private lateinit var thongBaoRongSanPhamBan: TextView
    private lateinit var giaTriDoanhThuNgay: TextView
    private lateinit var giaTriLoiNhuanNgay: TextView
    private lateinit var giaTriSoDonNgay: TextView
    private lateinit var giaTriSoSanPhamBan: TextView

    private lateinit var dichVuDoanhThu: DichVuDoanhThu
    private lateinit var trinhBayNgayTrongThang: TrinhBayNgayTrongThang
    private lateinit var trinhBaySanPhamDaBan: TrinhBaySanPhamDaBan

    /** Nam dang xem. */
    private var namDangXem = Calendar.getInstance().get(Calendar.YEAR)

    /** Thang dang xem, null neu dang o cap nam. */
    private var thangDangXem: Int? = null

    /** Ngay dang xem, null neu dang o cap thang. */
    private var ngayDangXem: String? = null

    /** Co dang tai du lieu hay khong. */
    private var dangTai = false

    override fun onStart() {
        super.onStart()

        // Base Activity vua nap layout, vi vay phai anh xa va tai du lieu sau do.
        khoiTaoThanhPhanGiaoDien()
        dichVuDoanhThu = DichVuDoanhThu(applicationContext)
        khoiTaoDanhSach()
        khoiTaoBoLocNam()
        ganSuKien()
        capNhatKhuVucHienThi()
        taiDuLieuDoanhThu()
    }

    /** Anh xa cac id trong XML vao bien co ten de hieu. */
    private fun khoiTaoThanhPhanGiaoDien() {
        boLocNam = findViewById(R.id.bo_loc_nam_doanh_thu)
        duongDanDoanhThu = findViewById(R.id.duong_dan_doanh_thu)
        nutQuayLai = findViewById(R.id.nut_quay_lai_doanh_thu)
        vongTai = findViewById(R.id.vong_tai_doanh_thu)
        thongBaoLoi = findViewById(R.id.thong_bao_loi_doanh_thu)

        khuVucNam = findViewById(R.id.khu_vuc_nam)
        bieuDoNam = findViewById(R.id.bieu_do_doanh_thu_nam)
        giaTriTongDoanhThuNam = findViewById(R.id.gia_tri_tong_doanh_thu_nam)
        giaTriTongLoiNhuan = findViewById(R.id.gia_tri_tong_loi_nhuan)
        giaTriSoDonHang = findViewById(R.id.gia_tri_so_don_hang)
        giaTriTrungBinhThang = findViewById(R.id.gia_tri_trung_binh_thang)
        tieuDeBieuDoNam = findViewById(R.id.tieu_de_bieu_do_nam)

        khuVucThang = findViewById(R.id.khu_vuc_thang)
        bieuDoThang = findViewById(R.id.bieu_do_doanh_thu_thang)
        danhSachNgayTrongThang = findViewById(R.id.danh_sach_ngay_trong_thang)
        giaTriDoanhThuThang = findViewById(R.id.gia_tri_doanh_thu_thang)
        giaTriLoiNhuanThang = findViewById(R.id.gia_tri_loi_nhuan_thang)
        giaTriSoDonThang = findViewById(R.id.gia_tri_so_don_thang)
        tieuDeBieuDoThang = findViewById(R.id.tieu_de_bieu_do_thang)

        khuVucNgay = findViewById(R.id.khu_vuc_ngay)
        danhSachSanPhamDaBan = findViewById(R.id.danh_sach_san_pham_ban)
        thongBaoRongSanPhamBan = findViewById(R.id.thong_bao_rong_san_pham_ban)
        giaTriDoanhThuNgay = findViewById(R.id.gia_tri_doanh_thu_ngay)
        giaTriLoiNhuanNgay = findViewById(R.id.gia_tri_loi_nhuan_ngay)
        giaTriSoDonNgay = findViewById(R.id.gia_tri_so_don_ngay)
        giaTriSoSanPhamBan = findViewById(R.id.gia_tri_so_san_pham_ban)
    }

    /** Tao cac adapter cho danh sach ngay va san pham da ban. */
    private fun khoiTaoDanhSach() {
        trinhBayNgayTrongThang = TrinhBayNgayTrongThang { duLieu ->
            moChiTietNgay(duLieu)
        }
        danhSachNgayTrongThang.layoutManager = GridLayoutManager(this, 3)
        danhSachNgayTrongThang.adapter = trinhBayNgayTrongThang

        trinhBaySanPhamDaBan = TrinhBaySanPhamDaBan()
        danhSachSanPhamDaBan.layoutManager = LinearLayoutManager(this)
        danhSachSanPhamDaBan.adapter = trinhBaySanPhamDaBan
    }

    /** Dua danh sach nam vao Spinner. */
    private fun khoiTaoBoLocNam() {
        val namHienTai = Calendar.getInstance().get(Calendar.YEAR)

        // Cho phep xem 2 nam truoc va 1 nam sau, giong danh sach tren web.
        val danhSachNam = (namHienTai - 2..namHienTai + 1).map { it.toString() }

        boLocNam.adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            danhSachNam
        )

        // Chon nam dang xem trong danh sach.
        val viTri = danhSachNam.indexOf(namDangXem.toString())
        if (viTri >= 0) {
            boLocNam.setSelection(viTri)
        }

        boLocNam.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: android.widget.AdapterView<*>?,
                view: View?,
                viTri: Int,
                id: Long
            ) {
                val namMoi = danhSachNam[viTri].toInt()

                // Nam khong doi thi khong tai lai du lieu.
                if (namMoi == namDangXem) {
                    return
                }

                namDangXem = namMoi

                // Doi nam thi phai ve lai cap nam vi thang va ngay cu khong con hop le.
                thangDangXem = null
                ngayDangXem = null
                capNhatKhuVucHienThi()
                taiDuLieuDoanhThu()
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) = Unit
        }
    }

    /** Gan su kien cho bieu do va nut quay lai. */
    private fun ganSuKien() {
        // Cham vao cot thang de drill-down xuong danh sach ngay trong thang.
        bieuDoNam.khiChonKy = { _, duLieu ->
            moThang(duLieu)
        }

        // Cham vao cot ngay de drill-down xuong chi tiet ngay.
        bieuDoThang.khiChonKy = { _, duLieu ->
            moChiTietNgay(duLieu)
        }

        nutQuayLai.setOnClickListener {
            quayLaiCapTruoc()
        }
    }

    /**
     * Mo cap thang khi nguoi dung cham vao mot cot co doanh thu.
     *
     * @param duLieu Du lieu cua cot duoc chon.
     */
    private fun moThang(duLieu: DoanhThuTheoKy) {
        // Khoa du lieu chua "yyyy-MM" nen chi lay phan thang.
        val phan = duLieu.key.split("-")
        if (phan.size < 2) {
            return
        }

        thangDangXem = phan[1].toIntOrNull() ?: return
        ngayDangXem = null

        capNhatKhuVucHienThi()
        taiDuLieuDoanhThu()
    }

    /**
     * Mo cap ngay khi nguoi dung cham vao mot cot co doanh thu.
     *
     * @param duLieu Du lieu cua cot duoc chon.
     */
    private fun moChiTietNgay(duLieu: DoanhThuTheoKy) {
        // Khoa du lieu dang duong dan day du "yyyy-MM-dd".
        if (duLieu.key.length < 10) {
            return
        }

        ngayDangXem = duLieu.key.substring(0, 10)

        capNhatKhuVucHienThi()
        taiChiTietNgay()
    }

    /** Quay lai cap truoc khi nguoi dung bam nut quay lai. */
    private fun quayLaiCapTruoc() {
        when {
            // Dang xem ngay thi quay lai cap thang.
            ngayDangXem != null -> {
                ngayDangXem = null
            }

            // Dang xem thang thi quay lai cap nam.
            thangDangXem != null -> {
                thangDangXem = null
            }
        }

        capNhatKhuVucHienThi()
        taiDuLieuDoanhThu()
    }

    /** Hien thi dung khu vuc theo cap dang xem. */
    private fun capNhatKhuVucHienThi() {
        val dangXemNgay = ngayDangXem != null
        val dangXemThang = thangDangXem != null && !dangXemNgay

        khuVucNam.visibility = if (!dangXemThang && !dangXemNgay) View.VISIBLE else View.GONE
        khuVucThang.visibility = if (dangXemThang) View.VISIBLE else View.GONE
        khuVucNgay.visibility = if (dangXemNgay) View.VISIBLE else View.GONE

        // Nut quay lai chi hien khi da drill-down khoi cap nam.
        nutQuayLai.visibility = if (dangXemThang || dangXemNgay) View.VISIBLE else View.GONE

        // Duong dan hien thi giup nguoi dung biet dang xem cap nao.
        duongDanDoanhThu.text = when {
            dangXemNgay -> getString(
                R.string.duong_dan_doanh_thu_ngay,
                ngayDangXem.orEmpty()
            )

            dangXemThang -> getString(
                R.string.duong_dan_doanh_thu_thang,
                thangDangXem ?: 1,
                namDangXem
            )

            else -> getString(R.string.mo_ta_doanh_thu_man_hinh)
        }

        // Tieu de bieu do thay doi theo nam dang xem.
        tieuDeBieuDoNam.text = getString(R.string.tieu_de_bieu_do_nam, namDangXem)
        tieuDeBieuDoThang.text = getString(
            R.string.tieu_de_bieu_do_thang,
            thangDangXem ?: 1,
            namDangXem
        )
    }

    /**
     * Tai du lieu doanh thu theo cap dang xem.
     *
     * Cap nam lay theo thang, cap thang lay theo ngay, cap ngay lay chi tiet.
     */
    private fun taiDuLieuDoanhThu() {
        if (dangTai) {
            return
        }

        // Dang xem ngay thi dung endpoint rieng cho chi tiet.
        if (ngayDangXem != null) {
            taiChiTietNgay()
            return
        }

        dangTai = true
        capNhatTrangThaiTai(true)
        xoaThongBaoLoi()

        lifecycleScope.launch {
            val thang = thangDangXem

            val ketQua = dichVuDoanhThu.taiBaoCaoDoanhThu(
                ngayBatDau = if (thang == null) {
                    "$namDangXem-01-01"
                } else {
                    taoChuoiNgayDauThang(thang)
                },
                ngayKetThuc = if (thang == null) {
                    "$namDangXem-12-31"
                } else {
                    taoChuoiNgayCuoiThang(thang)
                },
                ky = if (thang == null) {
                    DichVuDoanhThu.KY_NAM
                } else {
                    DichVuDoanhThu.KY_THANG
                }
            )

            when (ketQua) {
                is KetQuaDoanhThu.ThanhCong -> {
                    if (thang == null) {
                        hienThiCapNam(ketQua.duLieu.summary, ketQua.duLieu.byPeriod)
                    } else {
                        hienThiCapThang(ketQua.duLieu.summary, ketQua.duLieu.byPeriod)
                    }
                }

                is KetQuaDoanhThu.ThatBai -> {
                    if (ketQua.maLoi == "UNAUTHORIZED" || ketQua.maLoi == "HTTP_401") {
                        hienThiLoi(ketQua.thongBao)
                        chuyenSangDangNhap()
                        dangTai = false
                        capNhatTrangThaiTai(false)
                        return@launch
                    }

                    hienThiLoi(ketQua.thongBao)
                }

                is KetQuaDoanhThu.ThanhCongNgay -> Unit
            }

            dangTai = false
            capNhatTrangThaiTai(false)
        }
    }

    /** Tai chi tiet doanh thu cua ngay dang xem. */
    private fun taiChiTietNgay() {
        val ngay = ngayDangXem ?: return

        if (dangTai) {
            return
        }

        dangTai = true
        capNhatTrangThaiTai(true)
        xoaThongBaoLoi()

        lifecycleScope.launch {
            when (val ketQua = dichVuDoanhThu.taiChiTietNgay(ngay)) {
                is KetQuaDoanhThu.ThanhCongNgay -> {
                    hienThiCapNgay(ketQua.duLieu)
                }

                is KetQuaDoanhThu.ThatBai -> {
                    if (ketQua.maLoi == "UNAUTHORIZED" || ketQua.maLoi == "HTTP_401") {
                        hienThiLoi(ketQua.thongBao)
                        chuyenSangDangNhap()
                        dangTai = false
                        capNhatTrangThaiTai(false)
                        return@launch
                    }

                    hienThiLoi(ketQua.thongBao)
                }

                is KetQuaDoanhThu.ThanhCong -> Unit
            }

            dangTai = false
            capNhatTrangThaiTai(false)
        }
    }

    /**
     * Hien thi du lieu cap nam.
     *
     * @param tongHop   Tong hop so lieu ca nam.
     * @param doanhThu  Doanh thu theo tung thang.
     */
    private fun hienThiCapNam(tongHop: TongHopDoanhThu, doanhThu: List<DoanhThuTheoKy>) {
        giaTriTongDoanhThuNam.text = dinhDangTien(tongHop.totalRevenue)
        giaTriTongLoiNhuan.text = dinhDangTien(tongHop.totalProfit)
        giaTriSoDonHang.text = tongHop.totalSales.toString()

        // Doanh thu trung binh thang chia cho 12 thang trong nam.
        giaTriTrungBinhThang.text = dinhDangTien(tongHop.totalRevenue / SO_THANG_TRONG_NAM)

        // Bieu do luon hien 12 cot, thang khong co doanh thu van hien cot rong.
        val duLieuBieuDo = (1..SO_THANG_TRONG_NAM).map { thang ->
            val duLieuThang = doanhThu.firstOrNull { it.key.endsWith("-${thang.toString().padStart(2, '0')}") }
            DoanhThuTheoKy(
                key = "$namDangXem-${thang.toString().padStart(2, '0')}",
                label = "T${thang}",
                revenue = duLieuThang?.revenue ?: 0.0,
                importCost = duLieuThang?.importCost ?: 0.0,
                profit = duLieuThang?.profit ?: 0.0,
                count = duLieuThang?.count ?: 0
            )
        }

        bieuDoNam.capNhatDuLieu(duLieuBieuDo)
    }

    /**
     * Hien thi du lieu cap thang.
     *
     * @param tongHop  Tong hop so lieu trong thang.
     * @param doanhThu Doanh thu theo tung ngay.
     */
    private fun hienThiCapThang(tongHop: TongHopDoanhThu, doanhThu: List<DoanhThuTheoKy>) {
        giaTriDoanhThuThang.text = dinhDangTien(tongHop.totalRevenue)
        giaTriLoiNhuanThang.text = dinhDangTien(tongHop.totalProfit)
        giaTriSoDonThang.text = tongHop.totalSales.toString()

        // Danh sach ngay chi lay nhung ngay co doanh thu de gon giao dien.
        val ngayCoDoanhThu = doanhThu.filter { it.revenue > 0 }
        bieuDoThang.capNhatDuLieu(ngayCoDoanhThu)
        trinhBayNgayTrongThang.capNhatDanhSach(ngayCoDoanhThu)
    }

    /**
     * Hien thi du lieu cap ngay.
     *
     * @param duLieu Chi tiet doanh thu cua ngay.
     */
    private fun hienThiCapNgay(duLieu: PhanHoiDoanhThuNgay) {
        val tongHop = duLieu.summary

        giaTriDoanhThuNgay.text = dinhDangTien(tongHop.totalRevenue)
        giaTriLoiNhuanNgay.text = dinhDangTien(tongHop.totalProfit)
        giaTriSoDonNgay.text = tongHop.totalInvoices.toString()
        giaTriSoSanPhamBan.text = tongHop.totalProductsSold.toString()

        trinhBaySanPhamDaBan.capNhatDanhSach(duLieu.productsSold)

        // Danh sach rong thi hien thong bao thay vi con danh sach trong.
        val coSanPham = duLieu.productsSold.isNotEmpty()
        danhSachSanPhamDaBan.visibility = if (coSanPham) View.VISIBLE else View.GONE
        thongBaoRongSanPhamBan.visibility = if (coSanPham) View.GONE else View.VISIBLE
    }

    /**
     * Tao chuoi ngay dau thang theo nam dang xem.
     *
     * @param thang Thang can xem.
     * @return Chuoi ngay dang `yyyy-MM-dd`.
     */
    private fun taoChuoiNgayDauThang(thang: Int): String {
        return "$namDangXem-${thang.toString().padStart(2, '0')}-01"
    }

    /**
     * Tao chuoi ngay cuoi thang theo nam dang xem.
     *
     * @param thang Thang can xem.
     * @return Chuoi ngay dang `yyyy-MM-dd`.
     */
    private fun taoChuoiNgayCuoiThang(thang: Int): String {
        // Ngay cuoi thang duoc tinh bang Cach lay ngay 0 cua thang sau.
        val lich = Calendar.getInstance()
        // Calendar quy dinh thang bat dau tu 0, nen thang 1 la thang 1.
        lich.set(namDangXem, thang - 1, 1)
        val ngayCuoi = lich.getActualMaximum(Calendar.DAY_OF_MONTH)

        return "$namDangXem-${thang.toString().padStart(2, '0')}-$ngayCuoi"
    }

    /** Bat hoac tat vong tai. */
    private fun capNhatTrangThaiTai(dangTai: Boolean) {
        vongTai.visibility = if (dangTai) View.VISIBLE else View.GONE
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

    /** Chuyen sang man hinh dang nhap khi token khong con hieu luc. */
    private fun chuyenSangDangNhap() {
        startActivity(
            Intent(this, DangNhap::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
        )
        finish()
    }

    /**
     * Dinh dang tien theo kieu VND.
     *
     * @param giaTri Gia tri can dinh dang.
     * @return Chuoi tien da dinh dang.
     */
    private fun dinhDangTien(giaTri: Double): String {
        return DinhDangTien.dinhDangTien(giaTri)
    }

    companion object {
        /** So thang trong mot nam. */
        const val SO_THANG_TRONG_NAM = 12
    }
}
