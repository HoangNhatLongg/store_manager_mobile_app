package com.example.store_manager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.store_manager.du_lieu.DinhDangTien
import com.example.store_manager.du_lieu.SanPhamDaBan

/**
 * Adapter hien thi danh sach san pham da ban trong mot ngay.
 *
 * Moi dong tuong ung voi mot san pham, kem so luong, don gia, thanh tien
 * va loi nhuan tinh duoc.
 */
class TrinhBaySanPhamDaBan : RecyclerView.Adapter<TrinhBaySanPhamDaBan.KhungSanPhamDaBan>() {
    private val danhSach = mutableListOf<SanPhamDaBan>()

    /**
     * Thay the danh sach dang hien thi.
     *
     * @param danhSachMoi Danh sach san pham moi.
     */
    fun capNhatDanhSach(danhSachMoi: List<SanPhamDaBan>) {
        danhSach.clear()
        danhSach.addAll(danhSachMoi)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(cha: ViewGroup, loai: Int): KhungSanPhamDaBan {
        val view = LayoutInflater.from(cha.context)
            .inflate(R.layout.dong_san_pham_da_ban, cha, false)
        return KhungSanPhamDaBan(view)
    }

    override fun onBindViewHolder(khung: KhungSanPhamDaBan, viTri: Int) {
        khung.ganDuLieu(danhSach[viTri])
    }

    override fun getItemCount(): Int = danhSach.size

    inner class KhungSanPhamDaBan(view: View) : RecyclerView.ViewHolder(view) {
        private val tenSanPham: TextView = view.findViewById(R.id.ten_san_pham_da_ban)
        private val thongTinSanPham: TextView = view.findViewById(R.id.thong_tin_san_pham_da_ban)
        private val soLuongSanPham: TextView = view.findViewById(R.id.so_luong_san_pham_da_ban)
        private val donGiaSanPham: TextView = view.findViewById(R.id.don_gia_san_pham_da_ban)
        private val thanhTienSanPham: TextView = view.findViewById(R.id.thanh_tien_san_pham_da_ban)
        private val loiNhuanSanPham: TextView = view.findViewById(R.id.loi_nhuan_san_pham_da_ban)

        /**
         * Gan du lieu cua san pham vao cac truong giao dien.
         *
         * @param sanPham San pham da ban can hien thi.
         */
        fun ganDuLieu(sanPham: SanPhamDaBan) {
            val boiCanh = itemView.context

            tenSanPham.text = sanPham.productName
            thongTinSanPham.text = buildString {
                append("SKU: ${sanPham.productSku}")
                append(" • ${sanPham.categoryName ?: boiCanh.getString(R.string.khong_co_danh_muc)}")
            }
            soLuongSanPham.text = sanPham.quantity.toString()
            donGiaSanPham.text = DinhDangTien.dinhDangTien(sanPham.unitPrice)
            thanhTienSanPham.text = DinhDangTien.dinhDangTien(sanPham.totalPrice)
            loiNhuanSanPham.text = DinhDangTien.dinhDangTien(sanPham.profit)

            // Loi nhuan am duoc to mau do de canh bao.
            loiNhuanSanPham.setTextColor(
                ContextCompat.getColor(
                    boiCanh,
                    if (sanPham.profit >= 0) R.color.xanh_dam else R.color.do_chu
                )
            )
        }
    }
}
