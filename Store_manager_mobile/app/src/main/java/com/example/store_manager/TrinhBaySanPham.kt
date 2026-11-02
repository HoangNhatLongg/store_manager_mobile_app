package com.example.store_manager

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.store_manager.du_lieu.DinhDangTien
import com.example.store_manager.du_lieu.SanPham
import com.google.android.material.card.MaterialCardView

/**
 * Adapter danh sach quan ly san pham.
 *
 * Bang cua web duoc doi thanh card de doc tot tren man hinh dien thoai,
 * nhung van giu nguyen cac thao tac: quan ly don vi, an hien, sua va xoa.
 */
class TrinhBaySanPham(
    private val khiSuaSanPham: (SanPham) -> Unit,
    private val khiXoaSanPham: (SanPham) -> Unit,
    private val khiMoDonVi: (SanPham) -> Unit,
    private val khiDoiAnHien: (SanPham) -> Unit
) : RecyclerView.Adapter<TrinhBaySanPham.KhungSanPham>() {
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

    override fun onCreateViewHolder(cha: ViewGroup, loai: Int): KhungSanPham {
        val view = LayoutInflater.from(cha.context).inflate(R.layout.dong_san_pham, cha, false)
        return KhungSanPham(view)
    }

    override fun onBindViewHolder(khung: KhungSanPham, viTri: Int) {
        khung.ganDuLieu(danhSach[viTri])
    }

    override fun getItemCount(): Int = danhSach.size

    inner class KhungSanPham(view: View) : RecyclerView.ViewHolder(view) {
        private val theSanPham: MaterialCardView = view.findViewById(R.id.the_san_pham)
        private val tenSanPham: TextView = view.findViewById(R.id.ten_san_pham)
        private val thongTinSanPham: TextView = view.findViewById(R.id.thong_tin_san_pham)
        private val danhMucSanPham: TextView = view.findViewById(R.id.danh_muc_san_pham)
        private val giaBanSanPham: TextView = view.findViewById(R.id.gia_ban_san_pham)
        private val giaNhapSanPham: TextView = view.findViewById(R.id.gia_nhap_san_pham)
        private val soLuongTonSanPham: TextView = view.findViewById(R.id.so_luong_ton_san_pham)
        private val trangThaiSanPham: TextView = view.findViewById(R.id.trang_thai_san_pham)
        private val hangCanhBao: View = view.findViewById(R.id.hang_canh_bao)
        private val noiDungCanhBao: TextView = view.findViewById(R.id.noi_dung_canh_bao)
        private val hinhSanPham: ImageView = view.findViewById(R.id.hinh_san_pham)
        private val bieuTuongSanPham: ImageView = view.findViewById(R.id.bieu_tuong_san_pham)
        private val nutSuaSanPham: ImageButton = view.findViewById(R.id.nut_sua_san_pham)
        private val nutXoaSanPham: ImageButton = view.findViewById(R.id.nut_xoa_san_pham)
        private val nutDonViSanPham: ImageButton = view.findViewById(R.id.nut_don_vi_san_pham)
        private val nutAnSanPham: ImageButton = view.findViewById(R.id.nut_an_san_pham)

        /**
         * Gan du lieu cua san pham vao giao dien.
         *
         * @param sanPham San pham can hien thi.
         */
        fun ganDuLieu(sanPham: SanPham) {
            val boiCanh = itemView.context

            tenSanPham.text = sanPham.name

            // SKU va ma vach duoc gom chung tren mot dong cho gon.
            val sku = sanPham.sku.ifBlank { "-" }
            val maVach = sanPham.barcode?.takeIf { it.isNotBlank() }
            thongTinSanPham.text = if (maVach == null) {
                boiCanh.getString(R.string.sku_va_mo_ta, sku)
            } else {
                boiCanh.getString(R.string.sku_va_ma_vach, sku, maVach)
            }

            danhMucSanPham.text = boiCanh.getString(
                R.string.danh_muc_cua_san_pham,
                sanPham.category?.name ?: boiCanh.getString(R.string.khong_co_danh_muc)
            )

            giaBanSanPham.text = DinhDangTien.dinhDangTien(sanPham.sellPrice)
            giaNhapSanPham.text = boiCanh.getString(
                R.string.gia_von,
                DinhDangTien.dinhDangTien(sanPham.importPrice)
            )

            // So luong ton doi mau theo muc canh bao cua web.
            val sapHet = sanPham.stock <= sanPham.minStock
            soLuongTonSanPham.text = sanPham.stock.toString()
            soLuongTonSanPham.setBackgroundResource(
                if (sapHet) R.drawable.nen_so_luong_ton_canh_bao
                else R.drawable.nen_so_luong_ton
            )
            soLuongTonSanPham.setTextColor(
                boiCanh.getColor(if (sapHet) R.color.vang_chu else R.color.xanh_dam)
            )

            // Canh bao sap het hang chi hien khi ton kho cham nguong toi thieu.
            hangCanhBao.visibility = if (sapHet) View.VISIBLE else View.GONE
            noiDungCanhBao.text = boiCanh.getString(
                R.string.canh_bao_sap_het,
                sanPham.stock,
                sanPham.minStock
            )

            // Trang thai hien an tren man hinh ban hang.
            trangThaiSanPham.text = boiCanh.getString(
                if (sanPham.isActive) R.string.dang_hien else R.string.dang_an
            )
            trangThaiSanPham.setBackgroundResource(
                if (sanPham.isActive) R.drawable.nen_trang_thai_dang_hien
                else R.drawable.nen_trang_thai_dang_an
            )
            trangThaiSanPham.setTextColor(
                boiCanh.getColor(if (sanPham.isActive) R.color.xanh_dam else R.color.xam_phu)
            )

            // Hinh anh san pham, neu khong co thi hien bieu tuong goi y.
            val duongDanHinh = sanPham.imageUrl?.takeIf { it.isNotBlank() }
            if (duongDanHinh == null) {
                hinhSanPham.visibility = View.GONE
                bieuTuongSanPham.visibility = View.VISIBLE
            } else {
                hinhSanPham.visibility = View.VISIBLE
                bieuTuongSanPham.visibility = View.GONE
                hinhSanPham.setImageURL(duongDanHinh)
            }

            // San pham dang bi an thi lam mo toan bo dong.
            theSanPham.alpha = if (sanPham.isActive) 1f else 0.6f

            // Icon an hien doi chieu theo trang thai hien tai.
            nutAnSanPham.setImageResource(
                if (sanPham.isActive) R.drawable.bieu_tuong_an else R.drawable.bieu_tuong_hien
            )
            nutAnSanPham.contentDescription = boiCanh.getString(
                if (sanPham.isActive) R.string.an_san_pham else R.string.hien_san_pham
            )

            nutSuaSanPham.setOnClickListener { khiSuaSanPham(sanPham) }
            nutXoaSanPham.setOnClickListener { khiXoaSanPham(sanPham) }
            nutDonViSanPham.setOnClickListener { khiMoDonVi(sanPham) }
            nutAnSanPham.setOnClickListener { khiDoiAnHien(sanPham) }
        }
    }
}
