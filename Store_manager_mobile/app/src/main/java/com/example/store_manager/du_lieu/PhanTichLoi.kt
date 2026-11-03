package com.example.store_manager.du_lieu

import com.google.gson.Gson
import retrofit2.Response

/**
 * Thong tin loi do server tra ve, tach rieng ma loi va thong bao.
 *
 * @param maLoi     Ma loi chuan, vi du "VALIDATION_ERROR".
 * @param thongBao  Message tieng Viet cho nguoi dung.
 */
data class LoiApi(
    val maLoi: String,
    val thongBao: String
)

/**
 * Loi do server tra ve, giu ca ma loi va thong bao de hien thi cho nguoi dung.
 *
 * @param maLoi     Ma loi chuan, vi du "VALIDATION_ERROR".
 * @param thongBao  Message tieng Viet cho nguoi dung.
 */
class LoiApiException(
    val maLoi: String,
    val thongBao: String
) : Exception(thongBao.ifBlank { maLoi })

/**
 * Lop phan tich loi tu phia server theo ma loi chuan.
 *
 * Backend mobile luon tra loi voi duong dan { code, message }. App phai dua
 * quyet dinh xu ly vao ma loi chu khong phai noi dung message vi message
 * co the bi thay doi theo ngon ngu.
 */
object PhanTichLoi {
    private val boPhanTichJson = Gson()

    /**
     * Doc ca ma loi va thong bao tu body loi cua response.
     *
     * Body loi cua Retrofit chi doc duoc mot lan, nen phai doc va phan tich
     * trong cung mot lan goi de khong bi mat du lieu.
     *
     * @param response Response loi tu Retrofit.
     * @return Doi tuong chua ma loi va thong bao.
     */
    fun layLoi(response: Response<*>): LoiApi {
        // Doc body loi va chuyen sang chuoi, vi body chi doc duoc mot lan.
        val noiDungLoi = runCatching { response.errorBody()?.string() }.getOrNull()

        // Parse JSON loi de lay code va message do backend quy dinh.
        val phanHoiLoi = noiDungLoi?.let { raw ->
            runCatching {
                boPhanTichJson.fromJson(raw, PhanHoiLoi::class.java)
            }.getOrNull()
        }

        return LoiApi(
            maLoi = phanHoiLoi?.code?.takeIf { it.isNotBlank() } ?: "HTTP_${response.code()}",
            thongBao = phanHoiLoi?.message.orEmpty()
        )
    }

    /**
     * Chi lay ma loi, bo qua thong bao.
     *
     * @param response Response loi tu Retrofit.
     * @return Ma loi chuan theo backend.
     */
    fun layMaLoi(response: Response<*>): String = layLoi(response).maLoi
}
