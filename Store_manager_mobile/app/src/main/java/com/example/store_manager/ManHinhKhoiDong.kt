package com.example.store_manager

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.store_manager.du_lieu.BoNhoPhienDangNhap
import com.example.store_manager.du_lieu.DichVuTaiKhoan
import com.example.store_manager.du_lieu.KetQuaKiemTraToken
import kotlinx.coroutines.launch

/**
 * Man hinh khoi dong app.
 *
 * Vien duong ra co hai buoc: hien splash roi kiem tra access token co con
 * hieu luc khong de quyet dinh vao man hinh nao.
 */
class ManHinhKhoiDong : AppCompatActivity() {
    // Handler dung de chuyen man hinh sau mot khoang ngan de splash co thoi gian hien thi.
    private val boXuLy = Handler(Looper.getMainLooper())

    // Thoi gian cho splash hien thi truoc khi bat dau kiem tra token.
    private val thoiGianHienSplash = 1200L

    /**
     * Lenh chay sau khi splash da hien xong.
     *
     * Lenh nay se kiem tra token truoc, khong dieu huong truc tiep de tranh
     * dua nguoi dung vao man hinh chinh khi token da het han.
     */
    private val batDauKiemTraToken = Runnable {
        kiemTraTokenVaDieuHuong()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.khoi_dong)

        // Hien splash trong mot khoang ngan de nguoi dung nhin thay logo.
        boXuLy.postDelayed(batDauKiemTraToken, thoiGianHienSplash)
    }

    /**
     * Kiem tra access token truoc khi vao man hinh chinh.
     *
     * Token het han se bi xoa va nguoi dung phai dang nhap lai. Loi ket noi
     * khong lam mat phien dang nhap, nguoi dung van vao app duoc va thu lai
     * o man hinh sau.
     */
    private fun kiemTraTokenVaDieuHuong() {
        val boNhoPhien = BoNhoPhienDangNhap(applicationContext)
        val token = boNhoPhien.layMaTruyCap()

        // Khong co token thi chuyen thang sang man hinh dang nhap.
        if (token.isNullOrBlank()) {
            chuyenSangManHinh(DangNhap::class.java)
            return
        }

        lifecycleScope.launch {
            when (val ketQua = DichVuTaiKhoan(applicationContext).kiemTraToken()) {
                is KetQuaKiemTraToken.HieuLuc -> {
                    // Token con hieu luc, mo man hinh theo vai tro cua nguoi dung.
                    val manHinhTiepTheo =
                        if (ketQua.taiKhoan.role.equals("ADMIN", true)) {
                            ManHinhTongQuan::class.java
                        } else {
                            ManHinhBanHang::class.java
                        }

                    chuyenSangManHinh(manHinhTiepTheo)
                }

                is KetQuaKiemTraToken.HetHan -> {
                    // Token khong con hop le, xoa phien va bat buoc dang nhap lai.
                    boNhoPhien.xoaPhien()
                    chuyenSangManHinh(DangNhap::class.java)
                }

                is KetQuaKiemTraToken.LoiKetNoi -> {
                    // Khong ket noi duoc may chu, giu nguyen phien de nguoi dung
                    // thu lai thay vi bi kick ra khoi app ngay lap tuc.
                    val manHinhTiepTheo = if (boNhoPhien.laQuanTriVien()) {
                        ManHinhTongQuan::class.java
                    } else {
                        ManHinhBanHang::class.java
                    }

                    chuyenSangManHinh(manHinhTiepTheo)
                }
            }
        }
    }

    /**
     * Chuyen sang man hinh moi va dong man hinh khoi dong.
     *
     * @param loaiManHinh Lop Activity can mo.
     */
    private fun chuyenSangManHinh(loaiManHinh: Class<out AppCompatActivity>) {
        startActivity(Intent(this, loaiManHinh))
        finish()
    }

    override fun onDestroy() {
        // Huy lenh cho de tranh mo Activity moi sau khi splash da bi huy.
        boXuLy.removeCallbacks(batDauKiemTraToken)
        super.onDestroy()
    }
}
