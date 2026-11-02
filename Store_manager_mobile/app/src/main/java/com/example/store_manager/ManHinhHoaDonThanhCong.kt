package com.example.store_manager

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.store_manager.du_lieu.DinhDangTien
import com.google.android.material.button.MaterialButton

/**
 * Man hinh thong bao thanh cong sau khi tao hoa don ban.
 *
 * Man hinh nay khong nam trong khung chung vi day la buoc ket thuc cua mot
 * lan thanh toan, nguoi dung chi can dong no de quay lai man hinh ban hang.
 */
class ManHinhHoaDonThanhCong : AppCompatActivity() {
    private lateinit var soHoaDon: TextView
    private lateinit var tongCong: TextView
    private lateinit var tienThua: TextView
    private lateinit var thongBaoDaTonTai: TextView
    private lateinit var thongBaoOffline: TextView
    private lateinit var nutHoanTat: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.hoa_don_thanh_cong)

        soHoaDon = findViewById(R.id.so_hoa_don_thanh_cong)
        tongCong = findViewById(R.id.tong_cong_thanh_cong)
        tienThua = findViewById(R.id.tien_thua_thanh_cong)
        thongBaoDaTonTai = findViewById(R.id.thong_bao_da_ton_tai)
        thongBaoOffline = findViewById(R.id.thong_bao_offline)
        nutHoanTat = findViewById(R.id.nut_hoan_tat)

        hienThiThongTinHoaDon()

        // Dong man hinh va tra ket qua thanh cong ve man hinh ban hang
        // de xoa gio hang va lam moi ton kho.
        nutHoanTat.setOnClickListener {
            traKetQuaThanhCong()
        }

        // Khi nguoi dung nhan phim Back thi hoa don van da duoc tao nen van
        // phai tra ket qua thanh cong ve man hinh ban hang.
        onBackPressedDispatcher.addCallback(
            this,
            object : androidx.activity.OnBackPressedCallback(true) {
                override fun handleOnBackPressed() {
                    traKetQuaThanhCong()
                }
            }
        )
    }

    /**
     * Tra ket qua thanh cong ve man hinh ban hang va dong man hinh nay.
     *
     * Ket qua luon duoc tra ve vi hoa don da duoc tao that tren server.
     */
    private fun traKetQuaThanhCong() {
        setResult(Activity.RESULT_OK, Intent().putExtra(KHOA_THANH_CONG, true))
        finish()
    }

    /**
     * Hien thi so hoa don, tong cong va tien thua len giao dien.
     */
    private fun hienThiThongTinHoaDon() {
        val soHoaDonCanHien = intent.getStringExtra(KHOA_SO_HOA_DON).orEmpty()
        val tongCongCanHien = intent.getDoubleExtra(KHOA_TONG_CONG, 0.0)
        val tienThuaCanHien = intent.getDoubleExtra(KHOA_TIEN_THUA, 0.0)
        val daTonTai = intent.getBooleanExtra(KHOA_DA_TON_TAI, false)
        val taoOffline = intent.getBooleanExtra(KHOA_TAO_OFFLINE, false)

        soHoaDon.text = getString(R.string.so_hoa_don, soHoaDonCanHien)
        tongCong.text = DinhDangTien.dinhDangTien(tongCongCanHien)

        // Chi hien tien thua khi người dùng thanh toán bằng tiền mặt.
        tienThua.text = if (tienThuaCanHien > 0) {
            getString(R.string.tien_thua, DinhDangTien.dinhDangTien(tienThuaCanHien))
        } else {
            ""
        }

        // Hoa don da ton tai tu lan bam truoc do, can bao nguoi dung biet.
        thongBaoDaTonTai.visibility = if (daTonTai) android.view.View.VISIBLE else android.view.View.GONE

        // Hoa don tao khi mat mang, thong bao se duoc gui len may chu sau.
        thongBaoOffline.visibility =
            if (taoOffline) android.view.View.VISIBLE else android.view.View.GONE
    }

    companion object {
        const val KHOA_SO_HOA_DON = "so_hoa_don"
        const val KHOA_TONG_CONG = "tong_cong"
        const val KHOA_TIEN_THUA = "tien_thua"
        const val KHOA_DA_TON_TAI = "da_ton_tai"
        const val KHOA_TAO_OFFLINE = "tao_offline"
        const val KHOA_THANH_CONG = "thanh_cong"
    }
}
