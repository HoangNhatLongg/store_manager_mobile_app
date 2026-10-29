package com.example.store_manager

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity

class ManHinhKhoiDong : AppCompatActivity() {
    // Handler dung de chuyen man hinh sau mot khoang ngan de splash co thoi gian hien thi.
    private val boXuLy = Handler(Looper.getMainLooper())

    // Lenh chuyen sang man hinh dang nhap duoc luu lai de huy neu Activity bi dong som.
    private val lenhMoManHinhDangNhap = Runnable {
        startActivity(Intent(this, ManHinhChinh::class.java))
        finish()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.khoi_dong)

        // Hien splash trong 1200ms, sau do mo man hinh dang nhap.
        boXuLy.postDelayed(lenhMoManHinhDangNhap, 1200L)
    }

    override fun onDestroy() {
        // Huy lenh dang cho de tranh mo Activity moi sau khi splash da bi huy.
        boXuLy.removeCallbacks(lenhMoManHinhDangNhap)
        super.onDestroy()
    }
}
