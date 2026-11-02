package com.example.store_manager

import android.content.Intent
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.store_manager.du_lieu.DichVuTongQuan
import com.example.store_manager.du_lieu.DinhDangTien
import com.example.store_manager.du_lieu.HoatDongGanDay
import com.example.store_manager.du_lieu.KetQuaTongQuan
import com.example.store_manager.du_lieu.PhanHoiTongQuan
import com.example.store_manager.du_lieu.SanPhamBanChay
import com.example.store_manager.du_lieu.SanPhamSapHet
import com.example.store_manager.du_lieu.SoLieuTongQuan
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ManHinhTongQuan : ManHinhCoThanhTab() {
    override val taiNguyenNoiDung: Int = R.layout.tong_quan

    private lateinit var giaTriTongSanPham: TextView
    private lateinit var giaTriHangSapHet: TextView
    private lateinit var giaTriHoaDonNhap: TextView
    private lateinit var giaTriHoaDonBan: TextView
    private lateinit var giaTriTongTienBan: TextView
    private lateinit var giaTriTongLoiNhuan: TextView
    private lateinit var danhSachHoatDong: LinearLayout
    private lateinit var danhSachSapHet: LinearLayout
    private lateinit var danhSachBanChay: LinearLayout
    private lateinit var vongTaiTongQuan: ProgressBar
    private lateinit var thongBaoLoiTongQuan: TextView
    private lateinit var thoiGianCapNhat: TextView
    private lateinit var nutLamMoi: MaterialButton
    private lateinit var dichVuTongQuan: DichVuTongQuan

    override fun onStart() {
        super.onStart()
        // Base Activity vua nap layout, vi vay phai anh xa va tai du lieu sau do.
        khoiTaoThanhPhanGiaoDien()
        dichVuTongQuan = DichVuTongQuan(applicationContext)
        nutLamMoi.setOnClickListener { taiDuLieuTongQuan() }
        taiDuLieuTongQuan()
    }

    private fun khoiTaoThanhPhanGiaoDien() {
        giaTriTongSanPham = findViewById(R.id.gia_tri_tong_san_pham)
        giaTriHangSapHet = findViewById(R.id.gia_tri_hang_sap_het)
        giaTriHoaDonNhap = findViewById(R.id.gia_tri_hoa_don_nhap)
        giaTriHoaDonBan = findViewById(R.id.gia_tri_hoa_don_ban)
        giaTriTongTienBan = findViewById(R.id.gia_tri_tong_tien_ban)
        giaTriTongLoiNhuan = findViewById(R.id.gia_tri_tong_loi_nhuan)
        danhSachHoatDong = findViewById(R.id.danh_sach_hoat_dong)
        danhSachSapHet = findViewById(R.id.danh_sach_sap_het)
        danhSachBanChay = findViewById(R.id.danh_sach_ban_chay)
        vongTaiTongQuan = findViewById(R.id.vong_tai_tong_quan)
        thongBaoLoiTongQuan = findViewById(R.id.thong_bao_loi_tong_quan)
        thoiGianCapNhat = findViewById(R.id.thoi_gian_cap_nhat)
        nutLamMoi = findViewById(R.id.nut_lam_moi)
    }

    private fun taiDuLieuTongQuan() {
        capNhatTrangThaiTaiDuLieu(true)
        lifecycleScope.launch {
            when (val ketQua = dichVuTongQuan.taiDuLieu()) {
                is KetQuaTongQuan.ThanhCong -> {
                    hienThiTongQuan(ketQua.duLieu)
                    capNhatTrangThaiTaiDuLieu(false)
                }
                is KetQuaTongQuan.ThatBai -> {
                    capNhatTrangThaiTaiDuLieu(false)
                    hienThiLoiTongQuan(ketQua.thongBao)
                    if (ketQua.maLoi == "UNAUTHORIZED") {
                        startActivity(Intent(this@ManHinhTongQuan, DangNhap::class.java))
                        finish()
                    }
                }
            }
        }
    }

    private fun hienThiTongQuan(duLieu: PhanHoiTongQuan) {
        xoaLoiTongQuan()
        hienThiSoLieu(duLieu.stats)
        hienThiHoatDong(duLieu.recentActivities)
        hienThiSanPhamSapHet(duLieu.lowStockProducts)
        hienThiSanPhamBanChay(duLieu.topProducts)
        thoiGianCapNhat.text = getString(
            R.string.cap_nhat_luc,
            dinhDangThoiGian(duLieu.serverTime)
        )
    }

    private fun hienThiSoLieu(soLieu: SoLieuTongQuan) {
        giaTriTongSanPham.text = soLieu.totalProducts.toString()
        giaTriHangSapHet.text = soLieu.lowStockProducts.toString()
        giaTriHoaDonNhap.text = soLieu.totalImportInvoices.toString()
        giaTriHoaDonBan.text = soLieu.totalSaleInvoices.toString()
        giaTriTongTienBan.text = dinhDangTien(soLieu.totalSaleAmount)
        giaTriTongLoiNhuan.text = dinhDangTien(soLieu.totalProfit)
    }

    private fun hienThiHoatDong(danhSach: List<HoatDongGanDay>) {
        danhSachHoatDong.removeAllViews()
        if (danhSach.isEmpty()) {
            themDongTrong(danhSachHoatDong)
            return
        }
        danhSach.forEach { hoatDong ->
            danhSachHoatDong.addView(taoDongDanhSach().apply { text = hoatDong.message })
        }
    }

    private fun hienThiSanPhamSapHet(danhSach: List<SanPhamSapHet>) {
        danhSachSapHet.removeAllViews()
        if (danhSach.isEmpty()) {
            themDongTrong(danhSachSapHet)
            return
        }
        danhSach.forEach { sanPham ->
            danhSachSapHet.addView(taoDongDanhSach().apply {
                text = getString(R.string.dong_sap_het, sanPham.name, sanPham.stock, sanPham.minStock)
            })
        }
    }

    private fun hienThiSanPhamBanChay(danhSach: List<SanPhamBanChay>) {
        danhSachBanChay.removeAllViews()
        if (danhSach.isEmpty()) {
            themDongTrong(danhSachBanChay)
            return
        }
        danhSach.forEach { sanPham ->
            danhSachBanChay.addView(taoDongDanhSach().apply {
                text = getString(
                    R.string.dong_ban_chay,
                    sanPham.name,
                    sanPham.totalSold,
                    dinhDangTien(sanPham.totalRevenue)
                )
            })
        }
    }

    private fun taoDongDanhSach(): TextView {
        return TextView(this).apply {
            setPadding(0, 14, 0, 14)
            setTextColor(ContextCompat.getColor(context, R.color.xam_phu))
            textSize = 14f
        }
    }

    private fun themDongTrong(danhSach: LinearLayout) {
        danhSach.addView(taoDongDanhSach().apply {
            text = getString(R.string.chua_co_du_lieu)
        })
    }

    private fun capNhatTrangThaiTaiDuLieu(dangTai: Boolean) {
        vongTaiTongQuan.visibility = if (dangTai) View.VISIBLE else View.GONE
        nutLamMoi.isEnabled = !dangTai
    }

    private fun hienThiLoiTongQuan(thongBao: String) {
        thongBaoLoiTongQuan.text = thongBao
        thongBaoLoiTongQuan.visibility = View.VISIBLE
    }

    private fun xoaLoiTongQuan() {
        thongBaoLoiTongQuan.text = ""
        thongBaoLoiTongQuan.visibility = View.GONE
    }

    private fun dinhDangTien(giaTri: Double): String {
        return DinhDangTien.dinhDangTien(giaTri)
    }

    private fun dinhDangThoiGian(thoiGian: String): String {
        return try {
            val dauVao = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            val ngay = dauVao.parse(thoiGian) ?: Date()
            SimpleDateFormat("HH:mm dd/MM/yyyy", Locale("vi", "VN")).format(ngay)
        } catch (_: Exception) {
            thoiGian
        }
    }
}
