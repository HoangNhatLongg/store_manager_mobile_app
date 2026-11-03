package com.example.store_manager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.store_manager.du_lieu.DinhDangTien
import com.example.store_manager.du_lieu.SanPham

/**
 * Adapter hien danh sach san pham goi y trong hop thoai khi ten san pham
 * khong khop san pham nao dang co.
 *
 * @param khiChonSanPham Callback khi nguoi dung chon mot san pham de ghep
 *                       vao dong hang dang soan.
 */
class TrinhBaySanPhamGoiY(
    private val khiChonSanPham: (SanPham) -> Unit
) : RecyclerView.Adapter<TrinhBaySanPhamGoiY.KhungSanPham>() {
    private val danhSach = mutableListOf<SanPham>()

    /**
     * Cap nhat danh sach san pham goi y can hien thi.
     *
     * @param danhSachMoi Danh sach san pham moi.
     */
    fun capNhatDanhSach(danhSachMoi: List<SanPham>) {
        danhSach.clear()
        danhSach.addAll(danhSachMoi)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(cha: ViewGroup, loai: Int): KhungSanPham {
        val view = LayoutInflater.from(cha.context)
            .inflate(R.layout.dong_goi_y_san_pham, cha, false)
        return KhungSanPham(view)
    }

    override fun onBindViewHolder(khung: KhungSanPham, viTri: Int) {
        khung.ganDuLieu(danhSach[viTri])
    }

    override fun getItemCount(): Int = danhSach.size

    inner class KhungSanPham(view: View) : RecyclerView.ViewHolder(view) {
        private val tenSanPham: TextView = view.findViewById(R.id.ten_san_pham_goi_y)
        private val skuSanPham: TextView = view.findViewById(R.id.sku_san_pham_goi_y)
        private val giaNhap: TextView = view.findViewById(R.id.gia_nhap_san_pham_goi_y)

        /**
         * Gan du lieu cua mot san pham len dong giao dien.
         *
         * @param sanPham San pham goi y can hien thi.
         */
        fun ganDuLieu(sanPham: SanPham) {
            val boiCanh = itemView.context

            tenSanPham.text = sanPham.name
            skuSanPham.text = boiCanh.getString(R.string.sku_va_mo_ta, sanPham.sku)
            giaNhap.text = DinhDangTien.dinhDangTien(sanPham.importPrice)

            itemView.setOnClickListener {
                khiChonSanPham(sanPham)
            }
        }
    }
}
