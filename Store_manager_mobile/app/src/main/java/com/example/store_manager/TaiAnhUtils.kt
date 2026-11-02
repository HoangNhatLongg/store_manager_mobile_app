package com.example.store_manager

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Handler
import android.os.Looper
import android.util.LruCache
import android.widget.ImageView
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors

/**
 * Tai anh tu duong dan internet va gan vao ImageView.
 *
 * App chua dung thu vien anh nen phai tai thu cong qua HttpURLConnection.
 * Anh duoc giu lai trong bo dem nho de mo lai danh sach khong phai tai lai.
 */
fun ImageView.setImageURL(duongDan: String) {
    // Giu duong dan hien tai trong tag de biet anh nao moi dung cho View nay.
    tag = duongDan

    val anhDaTai = BoDemAnh.layAnh(duongDan)
    if (anhDaTai != null) {
        setImageBitmap(anhDaTai)
        return
    }

    // Chua co anh san, cho BoDemAnh tai va goi lai khi xong.
    BoDemAnh.taiAnh(duongDan) { anh ->
        if (anh != null && tag == duongDan) {
            setImageBitmap(anh)
        }
    }
}

/**
 * Bo dem anh dung chung cho toan bo ung dung.
 *
 * Anh duoc luu theo duong dan va gioi han so luong trong bo dem de tranh
 * chiem qua nhieu bo nho cua may.
 */
object BoDemAnh {
    /** So anh toi da giu trong bo dem. */
    private const val SO_ANH_TOI_DA = 60

    private val boDem = LruCache<String, Bitmap>(SO_ANH_TOI_DA)

    /** Luong phu dung de tai anh, tranh chan luong giao dien. */
    private val luongTai = Executors.newSingleThreadExecutor()

    private val luongChinh = Handler(Looper.getMainLooper())

    /** Duong dan dang duoc tai, tranh tai trung mot anh o nhieu lan. */
    private val dangTai = mutableSetOf<String>()

    /**
     * Lay anh da tai trong bo dem.
     *
     * @param duongDan Duong dan anh.
     * @return Bitmap anh hoac null neu chua tai.
     */
    fun layAnh(duongDan: String): Bitmap? = boDem.get(duongDan)

    /**
     * Tai anh tu mang va goi callback khi xong.
     *
     * @param duongDan Duong dan anh.
     * @param callback Callback nhan anh, chay tren luong giao dien.
     */
    fun taiAnh(duongDan: String, callback: (Bitmap?) -> Unit) {
        synchronized(dangTai) {
            if (!dangTai.add(duongDan)) {
                // Anh dang duoc tai, dung bo dem de lay ket qua sau.
                return
            }
        }

        luongTai.execute {
            val anh = taiAnhTuMang(duongDan)
            if (anh != null) {
                boDem.put(duongDan, anh)
            }

            synchronized(dangTai) {
                dangTai.remove(duongDan)
            }

            luongChinh.post { callback(anh) }
        }
    }

    /**
     * Tai anh tu mang va giai nen san cho dung.
     *
     * @param duongDan Duong dan anh.
     * @return Bitmap anh hoac null neu tai khong thanh cong.
     */
    private fun taiAnhTuMang(duongDan: String): Bitmap? {
        var ketNoi: HttpURLConnection? = null
        return try {
            ketNoi = URL(duongDan).openConnection() as HttpURLConnection
            ketNoi.connectTimeout = 8000
            ketNoi.readTimeout = 8000
            ketNoi.instanceFollowRedirects = true
            ketNoi.connect()

            if (ketNoi.responseCode !in 200..299) {
                null
            } else {
                val stream: InputStream = ketNoi.inputStream
                val anh = BitmapFactory.decodeStream(stream)
                stream.close()
                anh
            }
        } catch (_: Exception) {
            null
        } finally {
            ketNoi?.disconnect()
        }
    }
}
