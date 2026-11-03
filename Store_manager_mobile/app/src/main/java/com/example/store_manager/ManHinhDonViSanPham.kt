package com.example.store_manager

import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.store_manager.du_lieu.DichVuSanPham
import com.example.store_manager.du_lieu.DinhDangTien
import com.example.store_manager.du_lieu.DonViSanPham
import com.example.store_manager.du_lieu.DonViTinh
import com.example.store_manager.du_lieu.YeuCauDonViSanPham
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.launch

/** Man hinh chon, them, sua va xoa don vi cua mot san pham. */
class ManHinhDonViSanPham : ManHinhCoThanhTab() {
    override val taiNguyenNoiDung: Int = R.layout.don_vi_san_pham
    private lateinit var dichVuSanPham: DichVuSanPham
    private lateinit var danhSachDonVi: RecyclerView
    private lateinit var tenSanPham: TextView
    private lateinit var trinhBayDonVi: TrinhBayDonViSanPham
    private var cacDonVi = listOf<DonViTinh>()
    private var cacDonViCuaSanPham = listOf<DonViSanPham>()
    private val maSanPham: String get() = intent.getStringExtra(KHOA_MA_SAN_PHAM).orEmpty()

    override fun onStart() {
        super.onStart()
        dichVuSanPham = DichVuSanPham(applicationContext)
        tenSanPham = findViewById(R.id.ten_san_pham_don_vi)
        tenSanPham.text = intent.getStringExtra(KHOA_TEN_SAN_PHAM).orEmpty()
        danhSachDonVi = findViewById(R.id.danh_sach_don_vi_san_pham)
        trinhBayDonVi = TrinhBayDonViSanPham(
            khiSua = { moHopThoaiDonVi(it) },
            khiXoa = { xacNhanXoa(it) }
        )
        danhSachDonVi.layoutManager = LinearLayoutManager(this)
        danhSachDonVi.adapter = trinhBayDonVi
        findViewById<MaterialButton>(R.id.nut_them_don_vi).setOnClickListener { moHopThoaiDonVi(null) }
        taiDuLieu()
    }

    private fun taiDuLieu() {
        lifecycleScope.launch {
            cacDonVi = dichVuSanPham.taiDonVi().getOrDefault(emptyList())
            cacDonViCuaSanPham = dichVuSanPham.taiDonViSanPham(maSanPham).getOrDefault(emptyList())
            trinhBayDonVi.capNhatDanhSach(cacDonViCuaSanPham)
        }
    }

    private fun moHopThoaiDonVi(donViDangSua: DonViSanPham?) {
        val giaoDien = LayoutInflater.from(this).inflate(R.layout.hop_thoai_don_vi, null)
        val boChonDonVi = giaoDien.findViewById<Spinner>(R.id.bo_chon_don_vi)
        val oQuyDoi = giaoDien.findViewById<TextInputEditText>(R.id.o_so_luong_quy_doi)
        val oGia = giaoDien.findViewById<TextInputEditText>(R.id.o_gia_theo_don_vi)
        val congTacMacDinh = giaoDien.findViewById<SwitchMaterial>(R.id.cong_tac_don_vi_mac_dinh)
        val congTacDonViGoc = giaoDien.findViewById<SwitchMaterial>(R.id.cong_tac_don_vi_goc)
        boChonDonVi.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, cacDonVi.map { it.name })
        val viTri = cacDonVi.indexOfFirst { it.id == donViDangSua?.unitId }
        if (viTri >= 0) boChonDonVi.setSelection(viTri)
        donViDangSua?.let {
            oQuyDoi.setText(it.conversionQty.toString())
            oGia.setText(DinhDangTien.taoChuoiChoOCoNhap(it.price))
            congTacMacDinh.isChecked = it.isDefault
            congTacDonViGoc.isChecked = it.isPurchase
        } ?: oQuyDoi.setText("1")

        // Khong cho bo co "ban le mac dinh" khi day la don vi mac dinh duy nhat
        // cua san pham, vi ton kho duoc dem theo don vi nay.
        val laMacDinhDuyNhat = donViDangSua?.isDefault == true &&
            cacDonViCuaSanPham.count { it.isDefault } <= 1
        congTacMacDinh.isEnabled = !laMacDinhDuyNhat

        // Don vi ban le mac dinh luon co he so quy doi bang 1, nen khi bat cong
        // tac nay thi khoa o he so quy doi.
        val capNhatHeSoQuyDoi = {
            val laMacDinh = congTacMacDinh.isChecked
            oQuyDoi.isEnabled = !laMacDinh
            if (laMacDinh) oQuyDoi.setText("1")
        }
        congTacMacDinh.setOnCheckedChangeListener { _, _ -> capNhatHeSoQuyDoi() }
        capNhatHeSoQuyDoi()

        val hopThoai = MaterialAlertDialogBuilder(this)
            .setTitle(if (donViDangSua == null) R.string.them_don_vi else R.string.sua_don_vi)
            .setView(giaoDien)
            .setNegativeButton(R.string.huy, null)
            .setPositiveButton(R.string.luu, null)
            .create()
        hopThoai.setOnShowListener {
            hopThoai.getButton(android.app.AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val donVi = cacDonVi.getOrNull(boChonDonVi.selectedItemPosition)
                val yeuCau = donVi?.let {
                    val soLuongQuyDoi = oQuyDoi.text.toString().toIntOrNull()
                    // Gia duoc chuan hoa de khong nhap nham dau phay theo locale Viet Nam.
                    val gia = DinhDangTien.chuyenChuoiSangTien(oGia.text.toString())
                    if (soLuongQuyDoi == null || soLuongQuyDoi < 1 || gia <= 0) {
                        Toast.makeText(this, R.string.du_lieu_khong_hop_le, Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }
                    YeuCauDonViSanPham(
                        productId = maSanPham,
                        unitId = it.id,
                        // Don vi ban le mac dinh luon co he so quy doi bang 1.
                        conversionQty = if (congTacMacDinh.isChecked) 1 else soLuongQuyDoi,
                        price = gia,
                        isDefault = congTacMacDinh.isChecked,
                        isPurchase = congTacDonViGoc.isChecked
                    )
                }
                if (yeuCau == null) {
                    Toast.makeText(this, R.string.du_lieu_khong_hop_le, Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                lifecycleScope.launch {
                    val ketQua = if (donViDangSua == null) {
                        dichVuSanPham.themDonViSanPham(yeuCau)
                    } else {
                        dichVuSanPham.suaDonViSanPham(donViDangSua.id, yeuCau)
                    }
                    if (ketQua.isSuccess) {
                        hopThoai.dismiss()
                        taiDuLieu()
                    } else {
                        // Hien thi thong bao do server tra ve, vi du khong the bo
                        // co don vi ban le mac dinh duy nhat cua san pham.
                        val loi = ketQua.exceptionOrNull()?.message
                        Toast.makeText(
                            this@ManHinhDonViSanPham,
                            loi?.takeIf { it.isNotBlank() } ?: getString(R.string.khong_the_tai_du_lieu),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
        }
        hopThoai.show()
    }

    private fun xacNhanXoa(donVi: DonViSanPham) {
        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.xoa)
            .setMessage(R.string.xoa_xac_nhan)
            .setNegativeButton(R.string.huy, null)
            .setPositiveButton(R.string.xoa) { _, _ ->
                lifecycleScope.launch {
                    if (dichVuSanPham.xoaDonViSanPham(donVi.id).isSuccess) taiDuLieu()
                }
            }.show()
    }

    companion object {
        const val KHOA_MA_SAN_PHAM = "ma_san_pham"
        const val KHOA_TEN_SAN_PHAM = "ten_san_pham"
    }
}
