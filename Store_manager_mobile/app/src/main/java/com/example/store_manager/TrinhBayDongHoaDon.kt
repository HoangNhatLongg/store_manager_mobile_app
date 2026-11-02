package com.example.store_manager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.store_manager.du_lieu.DinhDangTien
import com.example.store_manager.du_lieu.MatHangGioHang

/**
 * Adapter hien cac dong hang cua hoa don tren man hinh thanh toan.
 */
class TrinhBayDongHoaDon : RecyclerView.Adapter<TrinhBayDongHoaDon.KhungDong>() {
    private val danhSach = mutableListOf<MatHangGioHang>()

    /**
     * Cap nhat danh sach dong hang can hien thi.
     *
     * @param danhSachMoi Danh sach dong hang moi.
     */
    fun capNhatDanhSach(danhSachMoi: List<MatHangGioHang>) {
        danhSach.clear()
        danhSach.addAll(danhSachMoi)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(cha: ViewGroup, loai: Int): KhungDong {
        val view = LayoutInflater.from(cha.context)
            .inflate(R.layout.dong_dong_hoa_don, cha, false)
        return KhungDong(view)
    }

    override fun onBindViewHolder(khung: KhungDong, viTri: Int) {
        khung.ganDuLieu(danhSach[viTri])
    }

    override fun getItemCount(): Int = danhSach.size

    inner class KhungDong(view: View) : RecyclerView.ViewHolder(view) {
        private val tenDong: TextView = view.findViewById(R.id.ten_dong_hoa_don)
        private val donViDong: TextView = view.findViewById(R.id.don_vi_dong_hoa_don)
        private val soLuongDong: TextView = view.findViewById(R.id.so_luong_dong_hoa_don)
        private val thanhTienDong: TextView = view.findViewById(R.id.thanh_tien_dong_hoa_don)

        /**
         * Gan du lieu cua mot dong hang len giao dien.
         *
         * @param matHang Dong hang can hien thi.
         */
        fun ganDuLieu(matHang: MatHangGioHang) {
            val boiCanh = itemView.context

            tenDong.text = matHang.sanPham.name

            // Ten don vi ban giup nguoi dung phan biet cac dong cung mot san pham.
            donViDong.text = matHang.tenDonViBan.ifBlank {
                boiCanh.getString(R.string.don_vi_co_ban)
            }

            soLuongDong.text = matHang.soLuong.toString()
            thanhTienDong.text = DinhDangTien.dinhDangTien(matHang.thanhTien)
        }
    }
}
