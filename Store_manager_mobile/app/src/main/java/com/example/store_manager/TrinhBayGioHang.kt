package com.example.store_manager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.store_manager.du_lieu.DinhDangTien
import com.example.store_manager.du_lieu.MatHangGioHang

/**
 * Adapter hien cac mat hang dang nam trong gio hang tam thoi.
 *
 * Luu y quan trong: khong duoc goi notifyDataSetChanged() khi nguoi dung
 * dang nhap so luong, vi lenh nay huy toan bo ViewHolder dang hien thi. O
 * nhap so luong se bi reset ve gia tri cu, con tro nhip nhay va nguoi dung
 * mat so luong vua nhap.
 *
 * Vi vay adapter chi danh dau lai dong dang sua doi thay vi tai tao toan bo
 * danh sach, va chi gan gia tri o o nhap khi gia tri that su khac.
 */
class TrinhBayGioHang(
    private val khiThayDoiSoLuong: (MatHangGioHang, Int) -> Unit,
    private val khiNhapSoLuong: (MatHangGioHang, Int) -> Unit,
    private val khiChonMatHang: (MatHangGioHang) -> Unit
) : RecyclerView.Adapter<TrinhBayGioHang.KhungGioHang>() {
    private val danhSach = mutableListOf<MatHangGioHang>()

    /**
     * Cap nhat danh sach gio hang.
     *
     * Chi danh dau lai nhung dong thay doi thuc su, tranh goi
     * notifyDataSetChanged() lam mat focus cua o nhap dang duoc nguoi dung
     * thao tac.
     *
     * @param danhSachMoi Danh sach gio hang moi.
     */
    fun capNhatDanhSach(danhSachMoi: List<MatHangGioHang>) {
        // So dong cu va moi. Neu khac nhau thi danh dau lai toan bo de
        // RecyclerView dung lai vi tri. Viec nay khong lam mat o nhap.
        val soDongCu = danhSach.size
        val soDongMoi = danhSachMoi.size
        danhSach.clear()
        danhSach.addAll(danhSachMoi)

        when {
            // Danh sach rong, bo sung het cac dong dang hien thi.
            soDongCu == 0 -> notifyItemRangeInserted(0, soDongMoi)

            // Danh sach rong, xoa het cac dong dang hien thi.
            soDongMoi == 0 -> notifyItemRangeRemoved(0, soDongCu)

            // So dong giong nhau, chi danh dau lai dong bi thay doi.
            else -> {
                val soDongChung = minOf(soDongCu, soDongMoi)
                for (chiSo in 0 until soDongChung) {
                    if (danhSach[chiSo] != danhSachMoi[chiSo]) {
                        notifyItemChanged(chiSo)
                    }
                }
                // Them dong moi o cuoi danh sach.
                if (soDongMoi > soDongCu) {
                    notifyItemRangeInserted(soDongCu, soDongMoi - soDongCu)
                }
                // Xoa dong thua o cuoi danh sach.
                if (soDongMoi < soDongCu) {
                    notifyItemRangeRemoved(soDongMoi, soDongCu - soDongMoi)
                }
            }
        }
    }

    override fun onCreateViewHolder(cha: ViewGroup, loai: Int): KhungGioHang {
        val view = LayoutInflater.from(cha.context)
            .inflate(R.layout.dong_gio_hang, cha, false)
        return KhungGioHang(view)
    }

    override fun onBindViewHolder(khung: KhungGioHang, viTri: Int) {
        khung.ganDuLieu(danhSach[viTri])
    }

    override fun getItemCount(): Int = danhSach.size

    inner class KhungGioHang(view: View) : RecyclerView.ViewHolder(view) {
        private val tenMatHang: TextView = view.findViewById(R.id.ten_mat_hang)
        private val donViMatHang: TextView = view.findViewById(R.id.don_vi_mat_hang)
        private val soLuongMatHang: EditText = view.findViewById(R.id.so_luong_mat_hang)
        private val giaTriMatHang: TextView = view.findViewById(R.id.gia_tri_mat_hang)
        private val nutGiamSoLuong: TextView = view.findViewById(R.id.nut_giam_so_luong)
        private val nutTangSoLuong: TextView = view.findViewById(R.id.nut_tang_so_luong)

        /**
         * Coi dong nay dang duoc nguoi dung sua so luong hay khong.
         *
         * Khi dang sua thi khong gan lai gia tri o nhap, tranh ghi de
         * nội dung nguoi dung vua go.
         */
        private var dangSuaSoLuong = false

        fun ganDuLieu(matHang: MatHangGioHang) {
            tenMatHang.text = matHang.sanPham.name
            donViMatHang.text = matHang.tenDonViBan
            giaTriMatHang.text = dinhDangTien(matHang.thanhTien)

            // Chi gan lai so luong khi nguoi dung khong dang sua o nay. Neu
            // dang sua thi giu nguyen noi dung nguoi dung vua nhap.
            if (!dangSuaSoLuong) {
                val soLuongHienThi = matHang.soLuong.toString()
                if (soLuongMatHang.text.toString() != soLuongHienThi) {
                    soLuongMatHang.setText(soLuongHienThi)
                }
            }

            itemView.setOnClickListener {
                khiChonMatHang(matHang)
            }

            nutGiamSoLuong.setOnClickListener {
                khiThayDoiSoLuong(matHang, -1)
            }
            nutTangSoLuong.setOnClickListener {
                khiThayDoiSoLuong(matHang, 1)
            }

            // Nghe su kien focus. Khi o nhap duoc focus thi danh dau de
            // khong gan lai gia tri, khi mat focus thi moi xu ly so luong.
            soLuongMatHang.setOnFocusChangeListener { _, coFocus ->
                if (coFocus) {
                    dangSuaSoLuong = true
                } else {
                    dangSuaSoLuong = false
                    xuLyNhapSoLuong(matHang)
                }
            }

            // Xu ly khi nguoi dung bam phim Enter tren bo phim ao. Phim
            // Enter se dong vai tro chuyen focus, nen truoc khi chuyen focus
            // phai xu ly so luong da nhap.
            soLuongMatHang.setOnEditorActionListener { _, _, _ ->
                xuLyNhapSoLuong(matHang)
                false
            }
        }

        /**
         * Doc so luong nguoi dung nhap truc tiep va bao Activity cap nhat ton.
         *
         * Neu o nhap trong thi coi nhu khong nhap gi, nen giu nguyen so
         * luong hien tai thay vi xoa dong hang khoi gio.
         */
        private fun xuLyNhapSoLuong(matHang: MatHangGioHang) {
            val noiDung = soLuongMatHang.text.toString().trim()

            // O nhap trong nghia la nguoi dung chua nhap gi. Giu nguyen so
            // luong cu, tranh vang dong hang khoi gio hang.
            if (noiDung.isEmpty()) {
                soLuongMatHang.setText(matHang.soLuong.toString())
                return
            }

            // Neu khong phai so thi giu nguyen gia tri cu thay vi coi la 0.
            val soLuong = noiDung.toIntOrNull() ?: run {
                soLuongMatHang.setText(matHang.soLuong.toString())
                return
            }

            khiNhapSoLuong(matHang, soLuong)
        }

        private fun dinhDangTien(giaTri: Double): String {
            return DinhDangTien.dinhDangTien(giaTri)
        }
    }
}
