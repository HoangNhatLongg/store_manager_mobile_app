package com.example.store_manager

import android.os.Bundle
import android.util.Patterns
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.example.store_manager.du_lieu.DichVuDangNhap
import com.example.store_manager.du_lieu.KetQuaDangNhap
import com.google.android.material.button.MaterialButton
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import kotlinx.coroutines.launch

class ManHinhChinh : AppCompatActivity() {
    // Khai bao cac thanh phan giao dien cua man hinh dang nhap.
    private lateinit var oNhapEmail: TextInputEditText
    private lateinit var oNhapMatKhau: TextInputEditText
    private lateinit var khungEmail: TextInputLayout
    private lateinit var khungMatKhau: TextInputLayout
    private lateinit var oGhiNhoDangNhap: MaterialCheckBox
    private lateinit var nutDangNhap: MaterialButton
    private lateinit var vongTai: ProgressBar
    private lateinit var thongBaoLoi: TextView

    // Dich vu nay chiu trach nhiem goi API va luu access token.
    private lateinit var dichVuDangNhap: DichVuDangNhap

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.dang_nhap)

        // Khoi tao cac view sau khi layout da duoc nap vao Activity.
        khoiTaoThanhPhanGiaoDien()

        // Tao dich vu voi Context cua Activity de luu phien dang nhap.
        dichVuDangNhap = DichVuDangNhap(applicationContext)

        // Gan su kien submit cho nut dang nhap.
        nutDangNhap.setOnClickListener {
            xuLyDangNhap()
        }

        // Xu ly padding cua thanh trang thai va thanh dieu huong he thong.
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                systemBars.bottom
            )
            insets
        }
    }

    /**
     * Anh xa id trong XML vao cac bien Kotlin de cac ham xu ly ben duoi
     * khong phai goi findViewById nhieu lan.
     */
    private fun khoiTaoThanhPhanGiaoDien() {
        oNhapEmail = findViewById(R.id.o_nhap_email)
        oNhapMatKhau = findViewById(R.id.o_nhap_mat_khau)
        khungEmail = findViewById(R.id.khung_email)
        khungMatKhau = findViewById(R.id.khung_mat_khau)
        oGhiNhoDangNhap = findViewById(R.id.o_ghi_nho_dang_nhap)
        nutDangNhap = findViewById(R.id.nut_dang_nhap)
        vongTai = findViewById(R.id.vong_tai)
        thongBaoLoi = findViewById(R.id.thong_bao_loi)
    }

    /**
     * Lay du lieu nguoi dung nhap, validate o client va bat dau coroutine
     * de goi API ma khong lam dong bang giao dien.
     */
    private fun xuLyDangNhap() {
        val diaChiEmail = oNhapEmail.text?.toString()?.trim().orEmpty()
        val matKhau = oNhapMatKhau.text?.toString().orEmpty()

        if (!kiemTraDuLieuDangNhap(diaChiEmail, matKhau)) {
            return
        }

        // Vo hieu hoa nut trong luc request de tranh nguoi dung bam trung nhieu lan.
        capNhatTrangThaiDangNhap(dangTai = true)

        lifecycleScope.launch {
            val ketQua = dichVuDangNhap.dangNhap(
                diaChiEmail = diaChiEmail,
                matKhau = matKhau
            )

            // Chi cap nhat giao dien sau khi request ket thuc.
            capNhatTrangThaiDangNhap(dangTai = false)
            xuLyKetQuaDangNhap(ketQua)
        }
    }

    /** Kiem tra du lieu co ban truoc khi gui len server. */
    private fun kiemTraDuLieuDangNhap(
        diaChiEmail: String,
        matKhau: String
    ): Boolean {
        xoaThongBaoLoi()
        khungEmail.error = null
        khungMatKhau.error = null

        if (diaChiEmail.isBlank()) {
            khungEmail.error = getString(R.string.loi_email_rong)
            oNhapEmail.requestFocus()
            return false
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(diaChiEmail).matches()) {
            khungEmail.error = getString(R.string.loi_email_sai)
            oNhapEmail.requestFocus()
            return false
        }

        if (matKhau.isBlank()) {
            khungMatKhau.error = getString(R.string.loi_mat_khau_rong)
            oNhapMatKhau.requestFocus()
            return false
        }

        return true
    }

    /** Hien loading va khoa cac control trong luc dang goi API. */
    private fun capNhatTrangThaiDangNhap(dangTai: Boolean) {
        nutDangNhap.isEnabled = !dangTai
        oNhapEmail.isEnabled = !dangTai
        oNhapMatKhau.isEnabled = !dangTai
        oGhiNhoDangNhap.isEnabled = !dangTai
        vongTai.visibility = if (dangTai) View.VISIBLE else View.GONE
        nutDangNhap.text = if (dangTai) {
            getString(R.string.dang_dang_nhap)
        } else {
            getString(R.string.nut_dang_nhap)
        }
    }

    /** Phan tach ket qua thanh cong va that bai tu DichVuDangNhap. */
    private fun xuLyKetQuaDangNhap(ketQua: KetQuaDangNhap) {
        when (ketQua) {
            is KetQuaDangNhap.ThanhCong -> {
                xoaThongBaoLoi()
                Toast.makeText(
                    this,
                    getString(R.string.loi_dang_nhap_thanh_cong),
                    Toast.LENGTH_SHORT
                ).show()

                // Tam thoi giu tai man hinh nay de kiem tra token.
                // Buoc tiep theo se mo dashboard sau khi co man hinh navigation.
                nutDangNhap.text = getString(R.string.da_dang_nhap)
            }

            is KetQuaDangNhap.ThatBai -> {
                hienThiThongBaoLoi(ketQua.thongBao)
            }
        }
    }

    /** Hien thong bao loi tu validate hoac tu backend. */
    private fun hienThiThongBaoLoi(thongBao: String) {
        thongBaoLoi.text = thongBao
        thongBaoLoi.visibility = View.VISIBLE
    }

    /** An thong bao loi cu truoc moi lan submit. */
    private fun xoaThongBaoLoi() {
        thongBaoLoi.text = ""
        thongBaoLoi.visibility = View.GONE
    }
}
