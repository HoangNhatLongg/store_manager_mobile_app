package com.example.store_manager

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import com.example.store_manager.du_lieu.DinhDangTien
import com.example.store_manager.du_lieu.DoanhThuTheoKy
import kotlin.math.max

/**
 * Bieu do cot ve doanh thu theo ky.
 *
 * Bieu do duoc ve thu cong bang Canvas de khong phai them thu vien bieu do
 * vao ung dung. Moi cot duoc cham vao de xem gia tri doanh thu.
 */
class BieuDoCot(
    context: Context,
    thuocTinh: AttributeSet? = null
) : View(context, thuocTinh) {

    /**
     * Callback khi nguoi dung cham vao mot cot co du lieu.
     *
     * @param viTri   Vi tri cot trong danh sach du lieu.
     * @param duLieu Du lieu cua cot duoc chon.
     */
    var khiChonKy: ((viTri: Int, duLieu: DoanhThuTheoKy) -> Unit)? = null

    /** Danh sach du lieu ve len bieu do. */
    private var danhSach = emptyList<DoanhThuTheoKy>()

    /** Do lon doanh thu lon nhat, dung de quy do gia tri ve chieu cao cot. */
    private var doanhThuToiDa = 0.0

    /** Vị tri cot dang duoc chon, -1 neu khong co cot nao. */
    private var viTriDangChon = -1

    /** Khoang trong giua cac cot. */
    private val khoangTrong = 0.28f

    /** He so chuyen doi pixel sang dp, dung de ve chu va bo goc theo do phan giai man hinh. */
    private val doPhanGiai = context.resources.displayMetrics.density

    /** Vao mau cua cot dang co doanh thu. */
    private val mauCot = Paint().apply {
        color = ContextCompat.getColor(context, R.color.xanh_chu_dao)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    /** Vao mau cua cot dang duoc chon. */
    private val mauCotChon = Paint().apply {
        color = ContextCompat.getColor(context, R.color.xanh_dam)
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    /** Vao mau cua cot khong co doanh thu. */
    private val mauCotRong = Paint().apply {
        color = Color.parseColor("#E2E8F0")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    /** Vao ve duong goc cua bieu do. */
    private val veDuongGoc = Paint().apply {
        color = Color.parseColor("#CBD5E1")
        style = Paint.Style.FILL
    }

    /** Vao ve nhan duoi cot. */
    private val veNhan = Paint().apply {
        color = ContextCompat.getColor(context, R.color.xam_phu)
        textSize = 10f * doPhanGiai
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    /** Vao ve khung hien thi gia tri khi cham vao cot. */
    private val veKhungGiaTri = Paint().apply {
        color = Color.parseColor("#0F172A")
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    /** Vao ve chu ben trong khung gia tri. */
    private val veChuGiaTri = Paint().apply {
        color = Color.WHITE
        textSize = 9f * doPhanGiai
        textAlign = Paint.Align.CENTER
        isAntiAlias = true
    }

    /**
     * Thay du lieu ve len bieu do.
     *
     * @param duLieuMoi Danh sach doanh thu theo ky moi.
     */
    fun capNhatDuLieu(duLieuMoi: List<DoanhThuTheoKy>) {
        danhSach = duLieuMoi
        doanhThuToiDa = duLieuMoi.maxOfOrNull { it.revenue } ?: 0.0
        viTriDangChon = -1
        invalidate()
    }

    /**
     * Lay doanh thu lon nhat trong du lieu hien tai.
     *
     * @return Gia tri doanh thu lon nhat.
     */
    fun layDoanhThuToiDa(): Double = doanhThuToiDa

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val chieuRong = MeasureSpec.getSize(widthMeasureSpec)
        val chieuCao = MeasureSpec.getSize(heightMeasureSpec)

        setMeasuredDimension(chieuRong, chieuCao)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        // Chieu cao phan ve bieu do, tru phan nhhan duoi.
        val chieuCaoVe = height - 50f * doPhanGiai

        // Chua co du lieu thi chi ve duong goc va con trong de hien thi.
        if (danhSach.isEmpty()) {
            veDuongGoc(canvas, chieuCaoVe)
            veChuRong(canvas, chieuCaoVe, context.getString(R.string.chua_co_du_lieu))
            return
        }

        // Do rong cot duoc chia deu cho so cot, tru khoang trong giua.
        // Do phai tinh lai o day vi du lieu co the ve sau khi View da duoc
        // do kich thuoc, luc do kich thuoc khong doi nen onSizeChanged
        // khong chay lai va do rong cot se bi sai.
        val buocChieuRong = width.toFloat() / danhSach.size
        val doRongCotHienTai = buocChieuRong * (1f - khoangTrong)

        val mauChon = kiemTraDuongCham()

        // Ve duong goc truocc de cot deu nam tren cuc.
        veDuongGoc(canvas, chieuCaoVe)

        danhSach.forEachIndexed { viTri, duLieu ->
            // Vi tri giua cot de canh chinh theo giua.
            val x = viTri * buocChieuRong + (buocChieuRong - doRongCotHienTai) / 2f

            // Chieu cao cot duoc quy doi theo doanh thu lon nhat.
            val tyLe = if (doanhThuToiDa > 0) {
                (duLieu.revenue / doanhThuToiDa).toFloat()
            } else {
                0f
            }

            // Cot khong co doanh thu van ve mot khoi nho de nhin ra duoc
            // khoang trong cua bieu do, cot co doanh thu thi luon co chieu cao
            // toi thiieu de khong bi mat o muc nho.
            val chieuCaoCot = when {
                duLieu.revenue <= 0 -> 3f * doPhanGiai
                else -> max(tyLe * chieuCaoVe, 8f * doPhanGiai)
            }

            val vungCot = RectF(
                x,
                chieuCaoVe - chieuCaoCot,
                x + doRongCotHienTai,
                chieuCaoVe
            )

            // Cot khong co doanh thu ve duong xam de vanh tinh.
            val ve = when {
                duLieu.revenue <= 0 -> mauCotRong
                viTri == mauChon -> mauCotChon
                else -> mauCot
            }

            canvas.drawRoundRect(vungCot, 8f * doPhanGiai, 8f * doPhanGiai, ve)

            // Nhan duoi cot.
            canvas.drawText(
                duLieu.label,
                x + doRongCotHienTai / 2f,
                chieuCaoVe + 34f * doPhanGiai,
                veNhan
            )

            // Khung hien thi gia tri khi cot dang duoc chon.
            if (viTri == mauChon) {
                veKhungGiaTri(canvas, vungCot, duLieu)
            }
        }
    }

    /**
     * Ve duong goc cua bieu do.
     *
     * @param canvas     Canvas ve.
     * @param chieuCaoVe Chieu cao phan ve bieu do.
     */
    private fun veDuongGoc(canvas: Canvas, chieuCaoVe: Float) {
        canvas.drawRect(
            0f,
            chieuCaoVe,
            width.toFloat(),
            chieuCaoVe + 1f * doPhanGiai,
            veDuongGoc
        )
    }

    /**
     * Ve thong bao canh giua khi khong co du lieu.
     *
     * @param canvas     Canvas ve.
     * @param chieuCaoVe Chieu cao phan ve bieu do.
     * @param noiDung    Noi dung can hien thi.
     */
    private fun veChuRong(canvas: Canvas, chieuCaoVe: Float, noiDung: String) {
        canvas.drawText(
            noiDung,
            width / 2f,
            chieuCaoVe / 2f,
            veNhan
        )
    }

    /**
     * Ve khung hien thi gia tri doanh thu tren cot dang chon.
     *
     * @param canvas  Canvas ve.
     * @param vungCot Vung cot dang chon.
     * @param duLieu  Du lieu cua cot dang chon.
     */
    private fun veKhungGiaTri(canvas: Canvas, vungCot: RectF, duLieu: DoanhThuTheoKy) {
        val chuoi = DinhDangTien.dinhDangTien(duLieu.revenue)
        val doRongChu = veChuGiaTri.measureText(chuoi) + 24f * doPhanGiai
        val doCaoChu = 30f * doPhanGiai

        // Khung duoc dat ngay tren cot, neu tran thi day xuong.
        var y = vungCot.top - doCaoChu - 6f * doPhanGiai
        if (y < 0f) {
            y = 0f
        }

        val x = vungCot.centerX() - doRongChu / 2f
        val khung = RectF(x, y, x + doRongChu, y + doCaoChu)

        canvas.drawRoundRect(khung, 8f * doPhanGiai, 8f * doPhanGiai, veKhungGiaTri)
        canvas.drawText(
            chuoi,
            khung.centerX(),
            khung.centerY() + 4f * doPhanGiai,
            veChuGiaTri
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (danhSach.isEmpty()) {
            return super.onTouchEvent(event)
        }

        when (event.action) {
            // Chi xu ly khi tha tay, tranh goi callback 2 lan o DOWN va UP.
            MotionEvent.ACTION_UP -> {
                val buocChieuRong = width.toFloat() / danhSach.size
                val viTri = (event.x / buocChieuRong).toInt().coerceIn(0, danhSach.size - 1)

                // Chon cot dang bam va bao cho nguoi dung biet.
                viTriDangChon = viTri
                invalidate()
                performClick()

                // Cot co doanh thu thi moi bao nguoi dung de drill-down.
                if (danhSach[viTri].revenue > 0) {
                    khiChonKy?.invoke(viTri, danhSach[viTri])
                }
            }
        }

        return true
    }

    override fun performClick(): Boolean {
        return super.performClick()
    }

    /**
     * Kiem tra cot dang chon co con du lieu khong.
     *
     * Cot khong co doanh thu thi khong duoc chon, vi nguoi dung khong the
     * xem chi tiet mot khoang khong co phat sinh.
     *
     * @return Vi tri cot dang chon, -1 neu khong co.
     */
    private fun kiemTraDuongCham(): Int {
        if (viTriDangChon !in danhSach.indices) {
            return -1
        }

        return if (danhSach[viTriDangChon].revenue > 0) viTriDangChon else -1
    }
}
