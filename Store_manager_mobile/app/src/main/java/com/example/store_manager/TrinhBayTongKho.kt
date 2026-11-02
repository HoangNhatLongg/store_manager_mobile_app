package com.example.store_manager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.store_manager.du_lieu.DinhDangTien
import com.example.store_manager.du_lieu.SanPham
import com.example.store_manager.du_lieu.TrangThaiTonKho

/**
 * Adapter hien thi mot dong trong bang ton kho.
 *
 * Moi dong tuong ung voi mot san pham, kiem tra duoc boi
 * `TrinhBayTongKho` de hien thi dung mau sac theo trang thai ton kho.
 */
class TrinhBayTongKho(
    private val khiChonSanPham: (SanPham) -> Unit
) : RecyclerView.Adapter<TrinhBayTongKho.KhungTongKho>() {
    private val danhSach = mutableListOf<SanPham>()

    /**
     * Thay the danh sach dang hien thi.
     *
     * @param danhSachMoi Danh sach san pham moi.
     */
    fun capNhatDanhSach(danhSachMoi: List<SanPham>) {
        danhSach.clear()
        danhSach.addAll(danhSachMoi)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(cha: ViewGroup, loai: Int): KhungTongKho {
        val view = LayoutInflater.from(cha.context).inflate(R.layout.dong_tong_kho, cha, false)
        return KhungTongKho(view)
    }

    override fun onBindViewHolder(khung: KhungTongKho, viTri: Int) {
        khung.ganDuLieu(danhSach[viTri])
    }

    override fun getItemCount(): Int = danhSach.size

    inner class KhungTongKho(view: View) : RecyclerView.ViewHolder(view) {
        private val skuSanPham: TextView = view.findViewById(R.id.sku_tong_kho)
        private val tenSanPham: TextView = view.findViewById(R.id.ten_tong_kho)
        private val danhMucSanPham: TextView = view.findViewById(R.id.danh_muc_tong_kho)
        private val giaNhapSanPham: TextView = view.findViewById(R.id.gia_nhap_tong_kho)
        private val giaBanSanPham: TextView = view.findViewById(R.id.gia_ban_tong_kho)
        private val tonKhoSanPham: TextView = view.findViewById(R.id.ton_kho_tong_kho)
        private val tonKhoToiDa: TextView = view.findViewById(R.id.ton_kho_toi_da_tong_kho)
        private val donViSanPham: TextView = view.findViewById(R.id.don_vi_tong_kho)
        private val trangThaiSanPham: TextView = view.findViewById(R.id.trang_thai_tong_kho)

        /**
         * Gan du lieu cua san pham vao cac truong giao dien.
         *
         * @param sanPham San pham can hien thi.
         */
        fun ganDuLieu(sanPham: SanPham) {
            val boiCanh = itemView.context

            skuSanPham.text = sanPham.sku
            tenSanPham.text = sanPham.name
            danhMucSanPham.text = sanPham.category?.name
                ?: boiCanh.getString(R.string.khong_co_danh_muc)
            giaNhapSanPham.text = DinhDangTien.dinhDangTien(sanPham.importPrice)
            giaBanSanPham.text = DinhDangTien.dinhDangTien(sanPham.sellPrice)
            tonKhoSanPham.text = sanPham.stock.toString()
            tonKhoToiDa.text = sanPham.minStock.toString()

            // Don vi ban mac dinh duoc uu tien hien thi, neu khong co thi hien
            // don vi co ban cua san pham.
            donViSanPham.text = sanPham.units
                .firstOrNull { it.isDefault }
                ?.unit
                ?.layTenHienThi()
                ?.ifBlank { null }
                ?: sanPham.units.firstOrNull()?.unit?.layTenHienThi()
                ?: boiCanh.getString(R.string.don_vi_co_ban)

            // Mau sac va nhan trang thai theo muc ton kho.
            val trangThai = TrangThaiTonKho.cuaSanPham(sanPham)
            trangThaiSanPham.text = boiCanh.getString(
                when (trangThai) {
                    TrangThaiTonKho.HET_HANG -> R.string.het_hang
                    TrangThaiTonKho.SAP_HET -> R.string.sap_het_hang
                    TrangThaiTonKho.CON_HANG -> R.string.con_hang
                }
            )
            trangThaiSanPham.setTextColor(ContextCompat.getColor(boiCanh, mauChuTrangThai(trangThai)))
            trangThaiSanPham.setBackgroundResource(nenTrangThai(trangThai))

            // So ton kho doi mau theo muc do, mau do thi gan nhu canh bao.
            tonKhoSanPham.setTextColor(ContextCompat.getColor(boiCanh, mauChuTrangThai(trangThai)))

            itemView.setOnClickListener { khiChonSanPham(sanPham) }
        }

        /**
         * Lay mau chu theo trang thai ton kho.
         *
         * @param trangThai Trang thai ton kho.
         * @return Ma mau cua resource.
         */
        private fun mauChuTrangThai(trangThai: TrangThaiTonKho): Int {
            return when (trangThai) {
                TrangThaiTonKho.HET_HANG -> R.color.do_chu
                TrangThaiTonKho.SAP_HET -> R.color.vang_chu
                TrangThaiTonKho.CON_HANG -> R.color.xanh_dam
            }
        }

        /**
         * Lay nen hinh cho nhan trang thai.
         *
         * @param trangThai Trang thai ton kho.
         * @return Id cua drawable nen.
         */
        private fun nenTrangThai(trangThai: TrangThaiTonKho): Int {
            return when (trangThai) {
                TrangThaiTonKho.HET_HANG -> R.drawable.nen_tong_kho_het
                TrangThaiTonKho.SAP_HET -> R.drawable.nen_tong_kho_sap_het
                TrangThaiTonKho.CON_HANG -> R.drawable.nen_trang_thai_hien
            }
        }
    }
}
