package com.example.store_manager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.store_manager.du_lieu.DinhDangTien
import com.example.store_manager.du_lieu.SanPham
import com.google.android.material.card.MaterialCardView

/** Adapter san pham POS co mau canh bao theo ton kho. */
class TrinhBaySanPhamBan(
    private val khiChonSanPham: (SanPham) -> Unit
) : RecyclerView.Adapter<TrinhBaySanPhamBan.KhungSanPhamBan>() {
    private val danhSach = mutableListOf<SanPham>()

    fun capNhatDanhSach(danhSachMoi: List<SanPham>) {
        danhSach.clear()
        danhSach.addAll(danhSachMoi)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(cha: ViewGroup, loai: Int): KhungSanPhamBan {
        val view = LayoutInflater.from(cha.context)
            .inflate(R.layout.dong_san_pham_ban, cha, false)
        return KhungSanPhamBan(view)
    }

    override fun onBindViewHolder(khung: KhungSanPhamBan, viTri: Int) {
        khung.ganDuLieu(danhSach[viTri])
    }

    override fun getItemCount(): Int = danhSach.size

    inner class KhungSanPhamBan(view: View) : RecyclerView.ViewHolder(view) {
        private val theSanPham: MaterialCardView = view.findViewById(R.id.the_san_pham_ban)
        private val tenSanPham: TextView = view.findViewById(R.id.ten_san_pham_ban)
        private val thongTinSanPham: TextView = view.findViewById(R.id.thong_tin_san_pham_ban)
        private val giaSanPham: TextView = view.findViewById(R.id.gia_san_pham_ban)

        fun ganDuLieu(sanPham: SanPham) {
            val mauNen = when {
                sanPham.stock == 0 -> R.drawable.nen_san_pham_nguy_hiem
                sanPham.stock <= 3 -> R.drawable.nen_san_pham_nguy_hiem
                sanPham.stock <= 10 -> R.drawable.nen_san_pham_canh_bao
                else -> R.drawable.nen_san_pham_binh_thuong
            }
            theSanPham.setBackgroundResource(mauNen)

            // Don vi mac dinh la don vi duoc hien thi tren the san pham. Khi
            // san pham khong co cau hinh don vi thi hien thi don vi co ban.
            val donViMacDinh = sanPham.units.firstOrNull { it.isDefault }
                ?: sanPham.units.firstOrNull()
            val donViTinh = donViMacDinh?.unit?.layTenHienThi()?.takeIf { it.isNotBlank() }
                ?: "-"

            tenSanPham.text = "${sanPham.name} \\ $donViTinh"
            thongTinSanPham.text = itemView.context.getString(
                if (sanPham.stock == 0) R.string.het_hang else R.string.thong_tin_ton_don_vi,
                sanPham.stock,
                sanPham.category?.name ?: "-",
                donViTinh
            )

            // Gia hien thi phai khop voi don vi mac dinh, vi do chinh don vi
            // nay se duoc chon khi nguoi dung them san pham vao gio hang.
            giaSanPham.text = DinhDangTien.dinhDangTien(donViMacDinh?.price ?: sanPham.sellPrice)

            if (sanPham.stock == 0) {
                itemView.alpha = 0.45f
                itemView.isClickable = false
                itemView.setOnClickListener(null)
            } else {
                itemView.alpha = 1f
                itemView.isClickable = true
                itemView.setOnClickListener { khiChonSanPham(sanPham) }
            }
        }
    }
}
