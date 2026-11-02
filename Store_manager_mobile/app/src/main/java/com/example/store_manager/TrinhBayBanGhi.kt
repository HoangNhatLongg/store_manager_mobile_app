package com.example.store_manager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.card.MaterialCardView

/**
 * Du lieu hien thi cua mot ban ghi trong danh sach quan ly.
 *
 * Lop nay bien moi loai du lieu (danh muc, don vi, khach hang, nha cung cap)
 * ve cung mot khuon hien thi de co the dung chung mot layout dong.
 *
 * @param ten          Ten ban ghi.
 * @param moTa         Mo ta phu canh ban ghi.
 * @param soLuong      Chuoi so luong lien quan, vi du "12 sản phẩm".
 * @param nhanTrangThai Nhan trang thai, null neu khong can hien thi.
 * @param dangAn       True neu ban ghi dang an khoi man hinh chinh.
 */
data class ThongTinHienThi(
    val ten: String = "",
    val moTa: String = "",
    val soLuong: String = "",
    val nhanTrangThai: String? = null,
    val dangAn: Boolean = false
)

/**
 * Adapter danh sach cho cac man hinh quan ly du lieu master (P4 - P7).
 *
 * Moi man hinh chi can cung cap ham chuyen du lieu thanh [ThongTinHienThi],
 * phan con lai cua giao dien giong nhau nen dung chung layout dong.
 *
 * @param T Loai du lieu cua ban ghi.
 * @param chuyenThongTin Ham chuyen mot ban ghi thanh du lieu hien thi.
 * @param khiSuaBanGhi  Callback khi nguoi dung bam nut sua.
 * @param khiXoaBanGhi  Callback khi nguoi dung bam nut xoa.
 */
class TrinhBayBanGhi<T : Any>(
    private val chuyenThongTin: (T) -> ThongTinHienThi,
    private val khiSuaBanGhi: (T) -> Unit,
    private val khiXoaBanGhi: (T) -> Unit,
    private val bieuTuongBanGhi: Int
) : RecyclerView.Adapter<TrinhBayBanGhi<T>.KhungBanGhi>() {

    /** Danh sach ban ghi dang hien thi. */
    protected val danhSach = mutableListOf<T>()

    /**
     * Thay the danh sach dang hien thi.
     *
     * @param danhSachMoi Danh sach ban ghi moi.
     */
    fun capNhatDanhSach(danhSachMoi: List<T>) {
        danhSach.clear()
        danhSach.addAll(danhSachMoi)
        notifyDataSetChanged()
    }

    /**
     * Lay ban ghi theo vi tri trong danh sach hien thi.
     *
     * @param viTri Vi tri cua dong, bat dau tu 0.
     * @return Ban ghi tuong ung, null neu vi tri khong hop le.
     */
    fun layBanGhi(viTri: Int): T? = danhSach.getOrNull(viTri)

    override fun onCreateViewHolder(cha: ViewGroup, loai: Int): KhungBanGhi {
        val view = LayoutInflater.from(cha.context).inflate(R.layout.dong_ban_ghi_quan_ly, cha, false)
        return KhungBanGhi(view)
    }

    override fun onBindViewHolder(khung: KhungBanGhi, viTri: Int) {
        khung.ganDuLieu(danhSach[viTri])
    }

    override fun getItemCount(): Int = danhSach.size

    /**
     * Khung hien thi mot ban ghi trong danh sach.
     */
    inner class KhungBanGhi(view: View) : RecyclerView.ViewHolder(view) {
        private val theBanGhi: MaterialCardView = view.findViewById(R.id.the_ban_ghi)
        private val hinhBanGhi: ImageView = view.findViewById(R.id.bieu_tuong_ban_ghi)
        private val tenBanGhi: TextView = view.findViewById(R.id.ten_ban_ghi)
        private val moTaBanGhi: TextView = view.findViewById(R.id.mo_ta_ban_ghi)
        private val soLuongBanGhi: TextView = view.findViewById(R.id.so_lieu_ban_ghi)
        private val nhanTrangThai: TextView = view.findViewById(R.id.nhan_trang_thai)
        private val nutSuaBanGhi: ImageButton = view.findViewById(R.id.nut_sua_ban_ghi)
        private val nutXoaBanGhi: ImageButton = view.findViewById(R.id.nut_xoa_ban_ghi)

        /**
         * Gan du lieu cua ban ghi vao giao dien.
         *
         * @param banGhi Ban ghi can hien thi.
         */
        fun ganDuLieu(banGhi: T) {
            val boiCanh = itemView.context
            val thongTin = chuyenThongTin(banGhi)

            // Icon phu thuoc loai ban ghi, vi du thu muc cho danh muc.
            hinhBanGhi.setImageResource(bieuTuongBanGhi)

            tenBanGhi.text = thongTin.ten
            moTaBanGhi.text = thongTin.moTa
            moTaBanGhi.visibility = if (thongTin.moTa.isBlank()) View.GONE else View.VISIBLE

            soLuongBanGhi.text = thongTin.soLuong
            soLuongBanGhi.visibility = if (thongTin.soLuong.isBlank()) View.GONE else View.VISIBLE

            // Nhan trang thai chi hien khi ban ghi dang an.
            if (thongTin.nhanTrangThai == null) {
                nhanTrangThai.visibility = View.GONE
            } else {
                nhanTrangThai.visibility = View.VISIBLE
                nhanTrangThai.text = thongTin.nhanTrangThai
                nhanTrangThai.setBackgroundResource(R.drawable.nen_trang_thai_dang_an)
                nhanTrangThai.setTextColor(boiCanh.getColor(R.color.xam_phu))
            }

            // Ban ghi dang an thi lam mo toan bo dong cho nguoi dung nhin thay.
            theBanGhi.alpha = if (thongTin.dangAn) 0.6f else 1f

            nutSuaBanGhi.setOnClickListener { khiSuaBanGhi(banGhi) }
            nutXoaBanGhi.setOnClickListener { khiXoaBanGhi(banGhi) }
        }
    }
}
