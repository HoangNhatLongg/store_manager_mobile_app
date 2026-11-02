package com.example.store_manager

import android.view.LayoutInflater
import android.view.View
import android.widget.CheckBox
import android.widget.ImageButton
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.switchmaterial.SwitchMaterial
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Gia tri cua form them hoac sua ban ghi.
 *
 * Gom cac truong chung cua ca bon man hinh quan ly du lieu master nen chi
 * can mot lop duy nhat la du cho danh muc, don vi, khach hang va nha cung cap.
 *
 * @param ten          Ten ban ghi.
 * @param vietTat      Viet tat don vi, chi dung o man hinh don vi tinh.
 * @param moTa         Mo ta, chi dung o man hinh danh muc.
 * @param laDonViCoBan Danh dau don vi co ban, chi dung o man hinh don vi tinh.
 * @param dienThoai    So dien thoai lien he.
 * @param email        Dia chi email lien he.
 * @param diaChi       Dia chi lien he.
 */
data class DuLieuFormBanGhi(
    val ten: String = "",
    val vietTat: String = "",
    val moTa: String = "",
    val laDonViCoBan: Boolean = false,
    val dienThoai: String = "",
    val email: String = "",
    val diaChi: String = ""
)

/**
 * Lop co so cho cac man hinh quan ly du lieu master: danh muc, don vi tinh,
 * khach hang va nha cung cap (P4 - P7).
 *
 * Bon man hinh nay co cung mot bo cuc: tieu de, tim kiem, danh sach the
 * card, them moi, sua va xoa. Lop nay giu phan giao dien chung, man hinh con
 * chi can mo ta nhung phan khac nhau.
 *
 * @param T Loai du lieu cua ban ghi.
 */
abstract class ManHinhQuanLyDuLieu<T : Any> : ManHinhCoThanhTab() {

    override val taiNguyenNoiDung: Int = R.layout.khung_quan_ly_du_lieu

    /** Tieu de man hinh. */
    protected abstract val tieuDeManHinh: Int

    /** Mo ta phu duoi tieu de. */
    protected abstract val moTaManHinh: Int

    /** Chuoi tren nut them moi. */
    protected abstract val chuoiNutThem: Int

    /** Icon hien ben canh ten ban ghi trong danh sach. */
    protected abstract val bieuTuongBanGhi: Int

    /** Tieu de cua bottom sheet form. */
    protected abstract val tieuDeHopThoai: Int

    /** Mo ta cua bottom sheet form. */
    protected abstract val moTaHopThoai: Int

    /** Chuoi tren nut luu trong bottom sheet form. */
    protected abstract val chuoiNutLuu: Int

    /** Nhan cho trang thai rong khi khong co du lieu. */
    protected abstract val chuoiKhongCoDuLieu: Int

    /** Hien o tim kiem hay khong. */
    protected open val coHienTimKiem: Boolean = true

    /** Hien checkbox an hien ban ghi dang an hay khong. */
    protected open val coHienTuyChonAn: Boolean = false

    /** Adapter hien thi danh sach ban ghi. */
    protected abstract fun taoAdapter(): TrinhBayBanGhi<T>

    /**
     * Tai danh sach ban ghi tu server.
     *
     * @param tuKhoa    Tu khoa tim kiem, null la tai tat ca.
     * @param baoGomAn True neu muon lay ca ban ghi dang an.
     * @return Ket qua tra ve theo ma loi chuan.
     */
    protected abstract suspend fun taiDanhSach(
        tuKhoa: String?,
        baoGomAn: Boolean
    ): Result<List<T>>

    /**
     * Tao ban ghi moi tren server.
     *
     * @param duLieu  Gia tri cua form.
     * @param banGhiCu Ban ghi dang sua, null khi tao moi.
     * @return Ket qua tra ve theo ma loi chuan.
     */
    protected abstract suspend fun taoBanGhi(
        duLieu: DuLieuFormBanGhi,
        banGhiCu: T?
    ): Result<T>

    /**
     * Cap nhat ban ghi tren server.
     *
     * @param duLieu  Gia tri cua form.
     * @param banGhiCu Ban ghi dang sua.
     * @return Ket qua tra ve theo ma loi chuan.
     */
    protected abstract suspend fun suaBanGhi(
        duLieu: DuLieuFormBanGhi,
        banGhiCu: T
    ): Result<T>

    /**
     * Xoa ban ghi tren server.
     *
     * @param banGhi Ban ghi can xoa.
     * @return Ket qua tra ve theo ma loi chuan.
     */
    protected abstract suspend fun xoaBanGhi(banGhi: T): Result<Unit>

    /**
     * Chuyen ban ghi thanh thong tin hien thi tren danh sach.
     *
     * @param banGhi Ban ghi dang hien thi.
     * @return Thong tin hien thi.
     */
    protected abstract fun chuyenThongTinHienThi(banGhi: T): ThongTinHienThi

    /**
     * Chuyen loi tu server thanh thong bao tieng Viet.
     *
     * @param ketQua Ket qua tra ve tu server.
     * @return Chuoi thong bao hien thi cho nguoi dung.
     */
    protected abstract fun thongBaoLoi(ketQua: Result<*>): String

    /**
     * Mo form them hoac sua ban ghi.
     *
     * @param banGhi Ban ghi dang sua, null neu dang them moi.
     */
    protected abstract fun moFormBanGhi(banGhi: T?)

    /**
     * Chuoi canh bao khi xoa ban ghi.
     *
     * @param banGhi Ban ghi can xoa.
     * @return Noi dung xac nhan xoa.
     */
    protected abstract val chuoiXacNhanXoa: (T) -> String

    /**
     * Kiem tra ban ghi co the xoa hay khong.
     *
     * @param banGhi Ban ghi can xoa.
     * @return Chuoi ly do khong the xoa, null neu co the xoa.
     */
    protected open val lyDoKhongTheXoa: (T) -> String? = { null }

    /**
     * Dien gia tri ban ghi dang sua vao form.
     *
     * @param banGhi Ban ghi dang sua, null neu dang them moi.
     * @return Gia tri cua form.
     */
    protected abstract fun layGiaTriForm(banGhi: T?): DuLieuFormBanGhi

    /**
     * Tao ban ghi moi tu gia tri form.
     *
     * @param duLieu  Gia tri cua form.
     * @param banGhiCu Ban ghi dang sua, null khi tao moi.
     * @return Ban ghi moi.
     */
    protected abstract fun taoBanGhiTuForm(
        duLieu: DuLieuFormBanGhi,
        banGhiCu: T?
    ): T

    private lateinit var oTimKiem: TextInputEditText
    private lateinit var boLocDangAn: CheckBox
    private lateinit var vongTaiDuLieu: ProgressBar
    private lateinit var thongBaoLoi: TextView
    private lateinit var thongBaoRong: TextView
    private lateinit var tongSoBanGhi: TextView
    private lateinit var nutThemMoi: MaterialButton
    private lateinit var danhSachBanGhi: RecyclerView
    private lateinit var tieuDeManHinhView: TextView
    private lateinit var moTaManHinhView: TextView
    private lateinit var trinhBayBanGhi: TrinhBayBanGhi<T>

    private var congViecTimKiem: Job? = null
    private var dangTai = false

    override fun onStart() {
        super.onStart()
        khoiTaoThanhPhanGiaoDien()
        trinhBayBanGhi = taoAdapter()
        danhSachBanGhi.layoutManager = LinearLayoutManager(this)
        danhSachBanGhi.adapter = trinhBayBanGhi

        nutThemMoi.setOnClickListener { moFormBanGhi(null) }
        ganSuKienTimKiem()
        taiDanhSach()
    }

    /** Anh xa cac id trong XML vao bien co ten de hieu. */
    private fun khoiTaoThanhPhanGiaoDien() {
        oTimKiem = findViewById(R.id.o_tim_kiem)
        boLocDangAn = findViewById(R.id.bo_loc_dang_an)
        vongTaiDuLieu = findViewById(R.id.vong_tai_du_lieu)
        thongBaoLoi = findViewById(R.id.thong_bao_loi)
        thongBaoRong = findViewById(R.id.thong_bao_rong)
        tongSoBanGhi = findViewById(R.id.tong_so_ban_ghi)
        nutThemMoi = findViewById(R.id.nut_them_moi)
        danhSachBanGhi = findViewById(R.id.danh_sach_ban_ghi)
        tieuDeManHinhView = findViewById(R.id.tieu_de_man_hinh)
        moTaManHinhView = findViewById(R.id.mo_ta_man_hinh)

        // Tieu de va mo ta do lop con quy dinh.
        tieuDeManHinhView.setText(tieuDeManHinh)
        moTaManHinhView.setText(moTaManHinh)
        nutThemMoi.setText(chuoiNutThem)

        // O tim kiem chi hien khi man hinh ho tro tim kiem.
        findViewById<View>(R.id.khung_tim_kiem).visibility =
            if (coHienTimKiem) View.VISIBLE else View.GONE

        // Checkbox an hien chi hien voi du lieu co truong an.
        boLocDangAn.visibility = if (coHienTuyChonAn) View.VISIBLE else View.GONE
        boLocDangAn.setOnCheckedChangeListener { _, _ ->
            taiDanhSach()
        }

        thongBaoRong.setText(chuoiKhongCoDuLieu)
    }

    /**
     * Gan su kien tim kiem, chi tai lai sau khi nguoi dung ngung go.
     */
    private fun ganSuKienTimKiem() {
        if (!coHienTimKiem) {
            return
        }

        oTimKiem.doAfterTextChanged {
            congViecTimKiem?.cancel()
            congViecTimKiem = lifecycleScope.launch {
                delay(350)
                taiDanhSach()
            }
        }
    }

    /**
     * Tai danh sach ban ghi theo bo loc hien tai.
     */
    protected fun taiDanhSach() {
        if (dangTai) {
            return
        }

        dangTai = true
        vongTaiDuLieu.visibility = View.VISIBLE
        anThongBaoLoi()

        lifecycleScope.launch {
            val ketQua = taiDanhSach(
                oTimKiem.text?.toString(),
                boLocDangAn.isChecked
            )

            ketQua.onSuccess { danhSach ->
                trinhBayBanGhi.capNhatDanhSach(danhSach)
                capNhatTongSo(danhSach.size)
            }.onFailure {
                // Loi nam trong exception nen giu nguyen ma loi de hien thi dung.
                thongBaoLoi.text = thongBaoLoi(Result.failure<Any>(it))
                thongBaoLoi.visibility = View.VISIBLE
            }

            dangTai = false
            vongTaiDuLieu.visibility = View.GONE
        }
    }

    /**
     * Cap nhat dong thong ke so ban ghi dang co.
     *
     * @param soBanGhi So ban ghi vua tai duoc.
     */
    private fun capNhatTongSo(soBanGhi: Int) {
        tongSoBanGhi.text = getString(R.string.so_ban_ghi, soBanGhi)
        thongBaoRong.visibility = if (soBanGhi == 0) View.VISIBLE else View.GONE
    }

    /** An thong bao loi cu truoc moi lan tai. */
    private fun anThongBaoLoi() {
        thongBaoLoi.text = ""
        thongBaoLoi.visibility = View.GONE
    }

    /**
     * Hoi xac nhan truoc khi xoa ban ghi.
     *
     * @param banGhi Ban ghi can xoa.
     */
    protected fun xacNhanXoaBanGhi(banGhi: T) {
        // Ban ghi dang duoc dung thi khong cho xoa, thong bao luon ngay.
        val lyDo = lyDoKhongTheXoa(banGhi)
        if (lyDo != null) {
            Toast.makeText(this, lyDo, Toast.LENGTH_LONG).show()
            return
        }

        MaterialAlertDialogBuilder(this)
            .setTitle(R.string.xoa)
            .setMessage(chuoiXacNhanXoa(banGhi))
            .setNegativeButton(R.string.huy, null)
            .setPositiveButton(R.string.xoa) { _, _ ->
                lifecycleScope.launch {
                    val ketQua = xoaBanGhi(banGhi)

                    if (ketQua.isSuccess) {
                        taiDanhSach()
                    } else {
                        Toast.makeText(
                            this@ManHinhQuanLyDuLieu,
                            thongBaoLoi(ketQua),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }
            .show()
    }

    /**
     * Mo bottom sheet chua form them hoac sua ban ghi.
     *
     * @param banGhi        Ban ghi dang sua, null neu dang them moi.
     * @param tieuDe        Tieu de hien tren sheet.
     * @param moTa          Mo ta phu duoi tieu de.
     * @param chuoiLuu      Chuoi tren nut luu.
     * @param hienVietTat   Hien o nhap viet tat khong.
     * @param hienMoTa      Hien o nhap mo ta khong.
     * @param hienDonViCoBan Hien cong tac don vi co ban khong.
     * @param hienLienHe    Hien nhom thong tin lien he khong.
     */
    protected fun moBottomSheetForm(
        banGhi: T?,
        tieuDe: Int,
        moTa: Int,
        chuoiLuu: Int,
        hienVietTat: Boolean = false,
        hienMoTa: Boolean = false,
        hienDonViCoBan: Boolean = false,
        hienLienHe: Boolean = false
    ) {
        val giaoDien = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_ban_ghi, null)

        val oTen = giaoDien.findViewById<TextInputEditText>(R.id.o_ten)
        val oVietTat = giaoDien.findViewById<TextInputEditText>(R.id.o_viet_tat)
        val oMoTa = giaoDien.findViewById<TextInputEditText>(R.id.o_mo_ta)
        val oDienThoai = giaoDien.findViewById<TextInputEditText>(R.id.o_dien_thoai)
        val oEmail = giaoDien.findViewById<TextInputEditText>(R.id.o_email)
        val oDiaChi = giaoDien.findViewById<TextInputEditText>(R.id.o_dia_chi)
        val khungTen = giaoDien.findViewById<TextInputLayout>(R.id.khung_ten)
        val congTacDonViCoBan =
            giaoDien.findViewById<SwitchMaterial>(R.id.cong_tac_don_vi_co_ban)
        val nutLuu = giaoDien.findViewById<MaterialButton>(R.id.nut_luu_hop_thoai)
        val nutHuy = giaoDien.findViewById<MaterialButton>(R.id.nut_huy_hop_thoai)
        val nutDong = giaoDien.findViewById<ImageButton>(R.id.nut_dong_hop_thoai)

        giaoDien.findViewById<TextView>(R.id.tieu_de_hop_thoai).setText(tieuDe)
        giaoDien.findViewById<TextView>(R.id.mo_ta_hop_thoai).setText(moTa)
        nutLuu.setText(chuoiLuu)

        // Chi hien cac truong ma man hinh dang xu ly.
        giaoDien.findViewById<View>(R.id.khung_viet_tat).visibility =
            if (hienVietTat) View.VISIBLE else View.GONE
        giaoDien.findViewById<View>(R.id.khung_mo_ta).visibility =
            if (hienMoTa) View.VISIBLE else View.GONE
        congTacDonViCoBan.visibility = if (hienDonViCoBan) View.VISIBLE else View.GONE
        giaoDien.findViewById<View>(R.id.tieu_de_lien_he).visibility =
            if (hienLienHe) View.VISIBLE else View.GONE
        giaoDien.findViewById<View>(R.id.khung_dien_thoai).visibility =
            if (hienLienHe) View.VISIBLE else View.GONE
        giaoDien.findViewById<View>(R.id.khung_email).visibility =
            if (hienLienHe) View.VISIBLE else View.GONE
        giaoDien.findViewById<View>(R.id.khung_dia_chi).visibility =
            if (hienLienHe) View.VISIBLE else View.GONE

        // Dien gia tri cua ban ghi dang sua.
        val giaTriForm = layGiaTriForm(banGhi)
        oTen.setText(giaTriForm.ten)
        oVietTat.setText(giaTriForm.vietTat)
        oMoTa.setText(giaTriForm.moTa)
        oDienThoai.setText(giaTriForm.dienThoai)
        oEmail.setText(giaTriForm.email)
        oDiaChi.setText(giaTriForm.diaChi)
        congTacDonViCoBan.isChecked = giaTriForm.laDonViCoBan

        val hopThoai = BottomSheetDialog(this)
        hopThoai.setContentView(giaoDien)

        // Sheet chiem mot phan man hinh, giu lai phan tren de nguoi dung
        // van thay danh sach ban ghi phia duoi.
        val chieuCaoSheet = (resources.displayMetrics.heightPixels * 0.92f).toInt()
        hopThoai.behavior.state = BottomSheetBehavior.STATE_EXPANDED
        hopThoai.behavior.skipCollapsed = true
        hopThoai.behavior.peekHeight = chieuCaoSheet
        hopThoai.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            ?.layoutParams?.height = chieuCaoSheet

        // Ban phim phai day noi dung len de nguoi dung khong bi che o nhap.
        hopThoai.window?.setSoftInputMode(
            android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE
        )

        nutHuy.setOnClickListener { hopThoai.dismiss() }
        nutDong.setOnClickListener { hopThoai.dismiss() }

        nutLuu.setOnClickListener {
            val ten = oTen.text.toString().trim()
            if (ten.isBlank()) {
                khungTen.error = getString(R.string.ten_bat_buoc)
                khungTen.requestFocus()
                return@setOnClickListener
            }
            khungTen.error = null

            val duLieu = DuLieuFormBanGhi(
                ten = ten,
                vietTat = oVietTat.text.toString().trim(),
                moTa = oMoTa.text.toString().trim(),
                laDonViCoBan = congTacDonViCoBan.isChecked,
                dienThoai = oDienThoai.text.toString().trim(),
                email = oEmail.text.toString().trim(),
                diaChi = oDiaChi.text.toString().trim()
            )

            nutLuu.isEnabled = false
            nutLuu.text = getString(R.string.dang_xu_ly)

            lifecycleScope.launch {
                val ketQua = if (banGhi == null) {
                    taoBanGhi(duLieu, null)
                } else {
                    suaBanGhi(duLieu, banGhi)
                }

                if (ketQua.isSuccess) {
                    hopThoai.dismiss()
                    taiDanhSach()
                } else {
                    nutLuu.isEnabled = true
                    nutLuu.setText(chuoiLuu)
                    Toast.makeText(
                        this@ManHinhQuanLyDuLieu,
                        thongBaoLoi(ketQua),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }

        hopThoai.show()
    }
}
