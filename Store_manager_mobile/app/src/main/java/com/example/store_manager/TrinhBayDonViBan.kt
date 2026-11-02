package com.example.store_manager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.store_manager.du_lieu.DinhDangTien
import com.example.store_manager.du_lieu.DonViSanPham

/**
 * Adapter hien cac don vi ban cua mot san pham trong hop thoai chon don vi.
 *
 * @param khiChonDonVi Callback khi nguoi dung chon mot don vi ban.
 * @param layTonKhoConLai Hàm tra tồn kho còn lại theo đơn vị cơ bản. Được gọi
 *                        lúc hiển thị để số lượng tối đa luôn khớp với giỏ hàng.
 */
class TrinhBayDonViBan(
    private val khiChonDonVi: (DonViSanPham) -> Unit,
    private val layTonKhoConLai: () -> Int
) : RecyclerView.Adapter<TrinhBayDonViBan.KhungDonViBan>() {
    private val danhSach = mutableListOf<DonViSanPham>()

    /**
     * Cap nhat danh sach don vi can hien thi.
     *
     * @param danhSachMoi Danh sach don vi moi.
     */
    fun capNhatDanhSach(danhSachMoi: List<DonViSanPham>) {
        danhSach.clear()
        danhSach.addAll(danhSachMoi)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(cha: ViewGroup, loai: Int): KhungDonViBan {
        val view = LayoutInflater.from(cha.context)
            .inflate(R.layout.dong_don_vi_ban, cha, false)
        return KhungDonViBan(view)
    }

    override fun onBindViewHolder(khung: KhungDonViBan, viTri: Int) {
        khung.ganDuLieu(danhSach[viTri])
    }

    override fun getItemCount(): Int = danhSach.size

    inner class KhungDonViBan(view: View) : RecyclerView.ViewHolder(view) {
        private val tenDonVi: TextView = view.findViewById(R.id.ten_don_vi_ban)
        private val quyDoiDonVi: TextView = view.findViewById(R.id.quy_doi_don_vi_ban)
        private val giaDonVi: TextView = view.findViewById(R.id.gia_don_vi_ban)
        private val soLuongToiDa: TextView = view.findViewById(R.id.so_luong_toi_da_don_vi_ban)

        /**
         * Gan du lieu cua mot don vi ban len dong giao dien.
         *
         * @param donVi Don vi ban can hien thi.
         */
        fun ganDuLieu(donVi: DonViSanPham) {
            val boiCanh = itemView.context

            tenDonVi.text = donVi.unit?.layTenHienThi() ?: "-"

            // He so quy doi giup nguoi dung hieu 1 don vi nay bang bao nhieu
            // don vi co ban cua san pham.
            quyDoiDonVi.text = boiCanh.getString(
                R.string.quy_doi_don_vi_ban,
                donVi.conversionQty
            )

            giaDonVi.text = DinhDangTien.dinhDangTien(donVi.price)

            // So luong toi da la so don vi ban lon nhat ma ton kho cho phep.
            // Ton kho duoc doc tai thoi diem hien thi de luon khop voi gio hang.
            val tonKhoConLai = layTonKhoConLai()
            val soLuongBan = tonKhoConLai / donVi.conversionQty.coerceAtLeast(1)
            soLuongToiDa.text = boiCanh.getString(R.string.so_luong_toi_da, soLuongBan)

            // Dong khong the ban se bi lam mo, nguoi dung biet ngay ly do.
            itemView.alpha = if (soLuongBan > 0) 1f else 0.45f
            itemView.isClickable = soLuongBan > 0

            itemView.setOnClickListener {
                if (soLuongBan > 0) {
                    khiChonDonVi(donVi)
                }
            }
        }
    }
}
