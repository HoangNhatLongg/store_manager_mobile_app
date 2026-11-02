package com.example.store_manager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.store_manager.du_lieu.DinhDangTien
import com.example.store_manager.du_lieu.DoanhThuTheoKy

/**
 * Adapter hien thi danh sach cac ngay trong thang.
 *
 * Moi ngay la mot o vuong gon, bam vao ngay co doanh thu se mo man hinh
 * chi tiet ngay do.
 */
class TrinhBayNgayTrongThang(
    private val khiChonNgay: (DoanhThuTheoKy) -> Unit
) : RecyclerView.Adapter<TrinhBayNgayTrongThang.KhungNgay>() {
    private val danhSach = mutableListOf<DoanhThuTheoKy>()

    /**
     * Thay the danh sach dang hien thi.
     *
     * @param danhSachMoi Danh sach ngay moi.
     */
    fun capNhatDanhSach(danhSachMoi: List<DoanhThuTheoKy>) {
        danhSach.clear()
        danhSach.addAll(danhSachMoi)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(cha: ViewGroup, loai: Int): KhungNgay {
        val view = LayoutInflater.from(cha.context).inflate(R.layout.dong_ngay_trong_thang, cha, false)
        return KhungNgay(view)
    }

    override fun onBindViewHolder(khung: KhungNgay, viTri: Int) {
        khung.ganDuLieu(danhSach[viTri])
    }

    override fun getItemCount(): Int = danhSach.size

    inner class KhungNgay(view: View) : RecyclerView.ViewHolder(view) {
        private val tenNgay: TextView = view.findViewById(R.id.ten_ngay_trong_thang)
        private val doanhThuNgay: TextView = view.findViewById(R.id.doanh_thu_ngay_trong_thang)

        /**
         * Gan du lieu cua ngay vao giao dien.
         *
         * @param duLieu Du lieu doanh thu cua ngay.
         */
        fun ganDuLieu(duLieu: DoanhThuTheoKy) {
            val boiCanh = itemView.context

            tenNgay.text = duLieu.label
            doanhThuNgay.text = if (duLieu.revenue > 0) {
                DinhDangTien.dinhDangTien(duLieu.revenue)
            } else {
                boiCanh.getString(R.string.khong_co)
            }

            // Ngay co doanh thu moi duoc mo chi tiet.
            val coDoanhThu = duLieu.revenue > 0
            itemView.alpha = if (coDoanhThu) 1f else 0.5f
            itemView.setBackgroundResource(
                if (coDoanhThu) R.drawable.nen_ngay_co_doanh_thu else R.drawable.nen_ngay_khong_doanh_thu
            )
            doanhThuNgay.setTextColor(
                ContextCompat.getColor(
                    boiCanh,
                    if (coDoanhThu) R.color.xanh_dam else R.color.xam_phu
                )
            )

            itemView.setOnClickListener {
                if (coDoanhThu) {
                    khiChonNgay(duLieu)
                }
            }
        }
    }
}
