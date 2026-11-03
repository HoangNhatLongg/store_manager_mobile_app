package com.example.store_manager

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.launch

/**
 * Activity co so cho cac man hinh chinh.
 *
 * Lop nay chiu trach nhiem:
 * - Nap layout khung_man_hinh.
 * - Nap noi dung cua man hinh con vao FrameLayout.
 * - Gan su kien cho 5 tab thuong dung.
 * - Mo menu cac chuc nang con lai bang BottomSheetDialog.
 */
abstract class ManHinhCoThanhTab : AppCompatActivity() {
    protected abstract val taiNguyenNoiDung: Int

    override fun onStart() {
        super.onStart()
        // Moi Activity con co the duoc mo truc tiep, vi vay tab duoc gan lai
        // o onStart de luon co trang thai dung sau khi quay lai man hinh.
        ganNoiDungVaThanhTab()
    }

    /** Nap noi dung va khoi tao su kien dieu huong dung chung. */
    private fun ganNoiDungVaThanhTab() {
        setContentView(R.layout.khung_man_hinh)

        val khungNoiDung = findViewById<android.widget.FrameLayout>(
            R.id.noi_dung_man_hinh
        )
        LayoutInflater.from(this).inflate(taiNguyenNoiDung, khungNoiDung, true)

        ganSuKienTab()
    }

    /**
     * Gan click cho cac tab. Tab hien tai duoc mo lai cung Activity de tranh
     * tao stack man hinh trung lap khi nguoi dung bam nhieu lan.
     */
    private fun ganSuKienTab() {
        val nutTongQuan = findViewById<MaterialButton>(R.id.tab_tong_quan)
        // Tong quan chi danh cho tai khoan ADMIN.
        nutTongQuan.visibility = if (laQuanTriVien()) View.VISIBLE else View.GONE
        nutTongQuan.setOnClickListener {
            moManHinh(ManHinhTongQuan::class.java)
        }
        findViewById<MaterialButton>(R.id.tab_ban_hang).setOnClickListener {
            moManHinh(ManHinhBanHang::class.java)
        }
        findViewById<MaterialButton>(R.id.tab_ton_kho).setOnClickListener {
            moManHinh(ManHinhTonKho::class.java)
        }
        findViewById<MaterialButton>(R.id.tab_san_pham).setOnClickListener {
            moManHinh(ManHinhSanPham::class.java)
        }
        findViewById<MaterialButton>(R.id.tab_menu).setOnClickListener {
            moMenuManHinh()
        }

        // Doi mau nen va mau chu de tab hien tai co the nhan biet ngay.
        toMauTabDangChon()
    }

    /** Kiem tra role luu trong phien dang nhap. */
    private fun laQuanTriVien(): Boolean {
        return com.example.store_manager.du_lieu.BoNhoPhienDangNhap(
            applicationContext
        ).laQuanTriVien()
    }

    /**
     * Tab dang mo duoc to nen xanh nhat; cac tab con lai trong suot.
     * Cach nay khong phu thuoc vao icon state va de doc voi nguoi moi.
     */
    private fun toMauTabDangChon() {
        val cacTab = listOf(
            R.id.tab_tong_quan,
            R.id.tab_ban_hang,
            R.id.tab_ton_kho,
            R.id.tab_san_pham,
            R.id.tab_menu
        )
        val tabHienTai = when (this) {
            is ManHinhTongQuan -> R.id.tab_tong_quan
            is ManHinhBanHang -> R.id.tab_ban_hang
            is ManHinhTonKho -> R.id.tab_ton_kho
            is ManHinhSanPham -> R.id.tab_san_pham
            else -> null
        }

        cacTab.forEach { idTab ->
            findViewById<MaterialButton>(idTab).apply {
                setBackgroundResource(
                    if (idTab == tabHienTai) {
                        R.drawable.nen_tab_duoc_chon
                    } else {
                        R.drawable.nen_tab_binh_thuong
                    }
                )
                setTextColor(
                    getColor(
                        if (idTab == tabHienTai) {
                            R.color.xanh_dam
                        } else {
                            R.color.xam_phu
                        }
                    )
                )
                iconTint = getColorStateList(
                    if (idTab == tabHienTai) {
                        R.color.xanh_dam
                    } else {
                        R.color.xam_phu
                    }
                )
            }
        }
    }

    /**
     * Mo Activity moi va dong Activity hien tai de back stack gon gang.
     *
     * Co the dung CLEAR_TOP de xoa cac Activity cung loai dang nam trong stack.
     * Nhu vay nguoi dung chuyen qua lai giua cac tab nhieu lan thi khong bi
     * ton tai san man hinh trung lap.
     *
     * @param loaiManHinh Lop Activity can mo.
     */
    protected fun moManHinh(loaiManHinh: Class<out AppCompatActivity>) {
        if (this::class.java == loaiManHinh) {
            return
        }

        startActivity(
            Intent(this, loaiManHinh).apply {
                addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
        )
    }

    /**
     * Menu chua cac man hinh it dung hon. Bottom sheet phu hop mobile hon
     * sidebar web va van giu duoc toan bo danh sach chuc nang.
     */
    private fun moMenuManHinh() {
        val hopThoai = BottomSheetDialog(this)
        val giaoDienMenu = layoutInflater.inflate(R.layout.menu_man_hinh, null)
        hopThoai.setContentView(giaoDienMenu)

        giaoDienMenu.findViewById<MaterialButton>(R.id.menu_san_pham)
            .setOnClickListener {
                hopThoai.dismiss()
                moManHinh(ManHinhSanPham::class.java)
            }

        // Bao cao doanh thu da co man hinh rieng nen mo truc tiep.
        giaoDienMenu.findViewById<MaterialButton>(R.id.menu_doanh_thu)
            .setOnClickListener {
                hopThoai.dismiss()
                moManHinh(ManHinhDoanhThu::class.java)
            }

        // Nhap hang da co man hinh rieng, mo truc tiep tu menu.
        giaoDienMenu.findViewById<MaterialButton>(R.id.menu_nhap_hang)
            .setOnClickListener {
                hopThoai.dismiss()
                moManHinh(ManHinhNhapHang::class.java)
            }

        ganMenuDangPhatTrien(giaoDienMenu, R.id.menu_lich_su_nhap, R.string.lich_su_nhap, hopThoai)
        ganMenuDangPhatTrien(giaoDienMenu, R.id.menu_lich_su_ban, R.string.lich_su_ban, hopThoai)

        // Cac man hinh quan ly du lieu master da co san CRUD day du.
        giaoDienMenu.findViewById<MaterialButton>(R.id.menu_danh_muc)
            .setOnClickListener {
                hopThoai.dismiss()
                moManHinh(ManHinhDanhMuc::class.java)
            }
        giaoDienMenu.findViewById<MaterialButton>(R.id.menu_don_vi)
            .setOnClickListener {
                hopThoai.dismiss()
                moManHinh(ManHinhDonViTinh::class.java)
            }
        giaoDienMenu.findViewById<MaterialButton>(R.id.menu_khach_hang)
            .setOnClickListener {
                hopThoai.dismiss()
                moManHinh(ManHinhKhachHang::class.java)
            }
        giaoDienMenu.findViewById<MaterialButton>(R.id.menu_nha_cung_cap)
            .setOnClickListener {
                hopThoai.dismiss()
                moManHinh(ManHinhNhaCungCap::class.java)
            }

        ganMenuDangPhatTrien(giaoDienMenu, R.id.menu_ocr, R.string.ocr_hoa_don, hopThoai)
        ganMenuDangPhatTrien(giaoDienMenu, R.id.menu_cai_dat, R.string.cai_dat, hopThoai)

        giaoDienMenu.findViewById<MaterialButton>(R.id.menu_dang_xuat)
            .setOnClickListener {
                hopThoai.dismiss()
                dangXuat()
            }

        hopThoai.show()
    }

    /** Gan xu ly chung cho muc menu chua co Activity nghiep vu. */
    private fun ganMenuDangPhatTrien(
        giaoDienMenu: View,
        idNut: Int,
        idTen: Int,
        hopThoai: BottomSheetDialog
    ) {
        giaoDienMenu.findViewById<MaterialButton>(idNut).setOnClickListener {
            hopThoai.dismiss()
            Toast.makeText(
                this,
                getString(R.string.dong_menu_dang_phat_trien, getString(idTen)),
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    /**
     * Xoa token va quay lai man hinh dang nhap.
     *
     * Khong dung finishAffinity vi cach do se dong toan bo app, nguoi dung
     * phai mo lai app tu dau. Thay vao do chi mo man hinh dang nhap va dung
     * finish de giai phong man hinh hien tai.
     */
    private fun dangXuat() {
        // Thu goi API de thu hoi refresh token. Viec goi API co the that bai
        // khi mat mang, nhung phien dang nhap van duoc xoa tren thiet bi.
        lifecycleScope.launch {
            com.example.store_manager.du_lieu.DichVuDangNhap(applicationContext)
                .dangXuat()

            // CLEAR_TASK dam bao khong con man hinh cu cua phien dang nhap truoc do
            // nam lai trong back stack sau khi dang xuat.
            startActivity(
                Intent(this@ManHinhCoThanhTab, DangNhap::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
            )
            finish()
        }
    }
}
