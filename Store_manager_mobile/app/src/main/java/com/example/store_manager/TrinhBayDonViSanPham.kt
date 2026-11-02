package com.example.store_manager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.store_manager.du_lieu.DinhDangTien
import com.example.store_manager.du_lieu.DonViSanPham
import com.google.android.material.button.MaterialButton

class TrinhBayDonViSanPham(
    private val khiSua: (DonViSanPham) -> Unit,
    private val khiXoa: (DonViSanPham) -> Unit
) : RecyclerView.Adapter<TrinhBayDonViSanPham.KhungDonVi>() {
    private val danhSach = mutableListOf<DonViSanPham>()

    fun capNhatDanhSach(danhSachMoi: List<DonViSanPham>) {
        danhSach.clear()
        danhSach.addAll(danhSachMoi)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(cha: ViewGroup, loai: Int): KhungDonVi {
        return KhungDonVi(LayoutInflater.from(cha.context).inflate(R.layout.dong_don_vi_san_pham, cha, false))
    }

    override fun onBindViewHolder(khung: KhungDonVi, viTri: Int) = khung.ganDuLieu(danhSach[viTri])
    override fun getItemCount(): Int = danhSach.size

    inner class KhungDonVi(view: View) : RecyclerView.ViewHolder(view) {
        private val ten: TextView = view.findViewById(R.id.ten_don_vi_san_pham)
        private val thongTin: TextView = view.findViewById(R.id.thong_tin_don_vi_san_pham)
        private val macDinh: TextView = view.findViewById(R.id.danh_dau_mac_dinh)
        private val nutSua: MaterialButton = view.findViewById(R.id.nut_sua_don_vi)
        private val nutXoa: MaterialButton = view.findViewById(R.id.nut_xoa_don_vi)

        fun ganDuLieu(donVi: DonViSanPham) {
            ten.text = donVi.unit?.name ?: "-"
            thongTin.text = "${donVi.unit?.abbreviation ?: "-"} • Quy đổi: ${donVi.conversionQty} • ${DinhDangTien.dinhDangTien(donVi.price)}"
            macDinh.text = if (donVi.isDefault) "Đơn vị mặc định" else ""
            nutSua.setOnClickListener { khiSua(donVi) }
            nutXoa.setOnClickListener { khiXoa(donVi) }
        }
    }
}
