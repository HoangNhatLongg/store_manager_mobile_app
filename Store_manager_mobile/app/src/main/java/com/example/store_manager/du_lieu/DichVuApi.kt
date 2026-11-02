package com.example.store_manager.du_lieu

import android.content.Context
import com.example.store_manager.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PATCH
import retrofit2.http.Query
import okhttp3.Interceptor
import java.util.concurrent.TimeUnit

/**
 * Khai bao cac endpoint cua backend mobile.
 * Retrofit se tu dong chuyen ham Kotlin nay thanh HTTP request.
 */
interface ApiCuaHang {
    @POST("api/v1/auth/login")
    suspend fun dangNhap(
        @Body yeuCau: YeuCauDangNhap
    ): Response<PhanHoiDangNhap>

    /**
     * Đổi refresh token lấy access token mới.
     *
     * Dùng khi access token hết hạn mà người dùng không cần đăng nhập lại.
     */
    @POST("api/v1/auth/refresh")
    suspend fun lamMoiToken(
        @Body yeuCau: YeuCauLamMoiToken
    ): Response<PhanHoiLamMoiToken>

    /** Thu hồi refresh token khi người dùng đăng xuất. */
    @POST("api/v1/auth/logout")
    suspend fun dangXuat(
        @Body yeuCau: YeuCauDangXuat
    ): Response<Unit>

    /** Goi API dashboard voi access token da duoc gan tu dong boi interceptor. */
    @GET("api/v1/dashboard")
    suspend fun layDuLieuTongQuan(): Response<PhanHoiTongQuan>

    /**
     * Kiem tra access token con hieu luc hay khong.
     *
     * Dung khi mo app de phat hien token da het han truoc khi vao man hinh chinh.
     */
    @GET("api/v1/me")
    suspend fun layThongTinTaiKhoan(): Response<PhanHoiTaiKhoan>

    /**
     * Lay danh sach san pham, co the truyen tu khoa tim kiem.
     *
     * @param tuKhoa             Tu khoa tim kiem.
     * @param maDanhMuc          Id danh muc de loc.
     * @param trang              So trang.
     * @param gioiHan            So ban ghi toi da mot lan.
     * @param baoGomSanPhamAn    Co lay ca san pham dang an hay khong.
     * @param capNhatSau         Chi lay san pham co thoi diem cap nhat lon hon moc nay.
     */
    @GET("api/v1/products")
    suspend fun layDanhSachSanPham(
        @Query("search") tuKhoa: String? = null,
        @Query("categoryId") maDanhMuc: String? = null,
        @Query("page") trang: Int = 1,
        @Query("limit") gioiHan: Int = 50,
        @Query("includeHidden") baoGomSanPhamAn: Boolean = false,
        @Query("updatedAfter") capNhatSau: String? = null
    ): Response<PhanHoiSanPham>

    /** Lay danh muc tu bang categories de lam bo loc tren man hinh ban hang. */
    @GET("api/v1/categories")
    suspend fun layDanhSachDanhMuc(): Response<PhanHoiDanhMuc>

    // ======== Danh muc (P4) ========

    /**
     * Lay danh muc kem so san pham thuoc tung danh muc.
     *
     * @param tuKhoa        Tu khoa tim kiem theo ten.
     * @param baoGomAn     True neu muon lay ca danh muc dang an.
     */
    @GET("api/v1/categories")
    suspend fun layDanhSachDanhMucQuanLy(
        @Query("search") tuKhoa: String? = null,
        @Query("includeHidden") baoGomAn: Boolean = false
    ): Response<PhanHoiDanhMuc>

    /** Tao danh muc moi. */
    @POST("api/v1/categories")
    suspend fun taoDanhMuc(@Body duLieu: DuLieuDanhMuc): Response<PhanHoiDanhMucDon>

    /** Cap nhat danh muc. */
    @PUT("api/v1/categories/{id}")
    suspend fun suaDanhMuc(
        @Path("id") maDanhMuc: String,
        @Body duLieu: DuLieuDanhMuc
    ): Response<PhanHoiDanhMucDon>

    /** Xoa danh muc. */
    @DELETE("api/v1/categories/{id}")
    suspend fun xoaDanhMuc(@Path("id") maDanhMuc: String): Response<Unit>

    // ======== Don vi tinh (P5) ========

    /**
     * Lay danh sach don vi tinh kem so san pham dung don vi do.
     *
     * @param tuKhoa Tu khoa tim kiem theo ten hoac viet tat.
     */
    @GET("api/v1/units")
    suspend fun layDanhSachDonViTinh(
        @Query("search") tuKhoa: String? = null
    ): Response<PhanHoiDonVi>

    /** Tao don vi tinh moi. */
    @POST("api/v1/units")
    suspend fun taoDonViTinh(@Body duLieu: DuLieuDonViTinh): Response<PhanHoiDonViTinhDon>

    /** Cap nhat don vi tinh. */
    @PUT("api/v1/units/{id}")
    suspend fun suaDonViTinh(
        @Path("id") maDonVi: String,
        @Body duLieu: DuLieuDonViTinh
    ): Response<PhanHoiDonViTinhDon>

    /** Xoa don vi tinh. */
    @DELETE("api/v1/units/{id}")
    suspend fun xoaDonViTinh(@Path("id") maDonVi: String): Response<Unit>

    // ======== Khach hang (P6) ========

    /**
     * Lay danh sach khach hang.
     *
     * @param tuKhoa    Tu khoa tim kiem theo ten, dien thoai hoac email.
     * @param baoGomAn True neu muon lay ca khach hang dang an.
     */
    @GET("api/v1/customers")
    suspend fun layDanhSachKhachHangQuanLy(
        @Query("search") tuKhoa: String? = null,
        @Query("includeHidden") baoGomAn: Boolean = false
    ): Response<PhanHoiKhachHang>

    /** Tao khach hang moi. */
    @POST("api/v1/customers")
    suspend fun taoKhachHang(@Body duLieu: DuLieuKhachHang): Response<PhanHoiKhachHangDon>

    /** Cap nhat khach hang. */
    @PUT("api/v1/customers/{id}")
    suspend fun suaKhachHang(
        @Path("id") maKhachHang: String,
        @Body duLieu: DuLieuKhachHang
    ): Response<PhanHoiKhachHangDon>

    /** Xoa hoac an khach hang. */
    @DELETE("api/v1/customers/{id}")
    suspend fun xoaKhachHang(@Path("id") maKhachHang: String): Response<Unit>

    // ======== Nha cung cap (P7) ========

    /**
     * Lay danh sach nha cung cap.
     *
     * @param tuKhoa    Tu khoa tim kiem theo ten, dien thoai hoac email.
     * @param baoGomAn True neu muon lay ca nha cung cap dang an.
     */
    @GET("api/v1/suppliers")
    suspend fun layDanhSachNhaCungCap(
        @Query("search") tuKhoa: String? = null,
        @Query("includeHidden") baoGomAn: Boolean = false
    ): Response<PhanHoiNhaCungCap>

    /** Tao nha cung cap moi. */
    @POST("api/v1/suppliers")
    suspend fun taoNhaCungCap(@Body duLieu: DuLieuNhaCungCap): Response<PhanHoiNhaCungCapDon>

    /** Cap nhat nha cung cap. */
    @PUT("api/v1/suppliers/{id}")
    suspend fun suaNhaCungCap(
        @Path("id") maNhaCungCap: String,
        @Body duLieu: DuLieuNhaCungCap
    ): Response<PhanHoiNhaCungCapDon>

    /** Xoa hoac an nha cung cap. */
    @DELETE("api/v1/suppliers/{id}")
    suspend fun xoaNhaCungCap(@Path("id") maNhaCungCap: String): Response<Unit>

    /**
     * Lay thong ke ton kho tren toan bo san pham.
     *
     * So lieu nay khong phu thuoc phan trang, dung cho cac the so lieu
     * o dau man hinh ton kho.
     *
     * @param tuKhoa    Tu khoa tim kiem.
     * @param maDanhMuc Id danh muc de loc.
     */
    @GET("api/v1/inventory/stats")
    suspend fun layThongKeTonKho(
        @Query("search") tuKhoa: String? = null,
        @Query("categoryId") maDanhMuc: String? = null
    ): Response<PhanHoiThongKeTonKho>

    @POST("api/v1/products")
    suspend fun taoSanPham(@Body duLieu: DuLieuSanPham): Response<PhanHoiSanPhamDon>

    @PUT("api/v1/products/{id}")
    suspend fun suaSanPham(@Path("id") maSanPham: String, @Body duLieu: DuLieuSanPham): Response<PhanHoiSanPhamDon>

    @DELETE("api/v1/products/{id}")
    suspend fun xoaSanPham(@Path("id") maSanPham: String): Response<Unit>

    @PATCH("api/v1/products/{id}")
    suspend fun doiTrangThaiSanPham(
        @Path("id") maSanPham: String,
        @Body duLieu: Map<String, Boolean>
    ): Response<Unit>

    @GET("api/v1/units")
    suspend fun layDanhSachDonVi(): Response<PhanHoiDonVi>

    @GET("api/v1/product-units")
    suspend fun layDonViSanPham(@Query("productId") maSanPham: String): Response<PhanHoiDonViSanPham>

    @POST("api/v1/product-units")
    suspend fun themDonViSanPham(@Body yeuCau: YeuCauDonViSanPham): Response<DonViSanPham>

    @PUT("api/v1/product-units/{id}")
    suspend fun suaDonViSanPham(@Path("id") maDonViSanPham: String, @Body yeuCau: YeuCauDonViSanPham): Response<DonViSanPham>

    @DELETE("api/v1/product-units/{id}")
    suspend fun xoaDonViSanPham(@Path("id") maDonViSanPham: String): Response<Unit>

    /** Lay danh sach khach hang de gan vao hoa don ban. */
    @GET("api/v1/customers")
    suspend fun layDanhSachKhachHang(): Response<PhanHoiKhachHang>

    /**
     * Tao hoa don ban tren server.
     *
     * operationId trong body la idempotency key, phai giu nguyen khi retry
     * de tranh tao trung hoa don.
     */
    @POST("api/v1/sales")
    suspend fun taoHoaDon(@Body yeuCau: YeuCauTaoHoaDon): Response<PhanHoiTaoHoaDon>

    /**
     * Lay bao cao doanh thu theo khoang thoi gian.
     *
     * @param ngayBatDau Ngay bat dau, dinh dang `yyyy-MM-dd`.
     * @param ngayKetThuc Ngay ket thuc, dinh dang `yyyy-MM-dd`.
     * @param ky         Ky gom nhom: `year`, `month` hoac `day`.
     */
    @GET("api/v1/analytics/revenue")
    suspend fun layBaoCaoDoanhThu(
        @Query("startDate") ngayBatDau: String? = null,
        @Query("endDate") ngayKetThuc: String? = null,
        @Query("period") ky: String = "month"
    ): Response<PhanHoiBaoCaoDoanhThu>

    /**
     * Lay chi tiet doanh thu cua mot ngay.
     *
     * @param ngay Ngay can xem, dinh dang `yyyy-MM-dd`.
     */
    @GET("api/v1/analytics/revenue-daily")
    suspend fun layChiTietDoanhThuNgay(
        @Query("date") ngay: String
    ): Response<PhanHoiChiTietDoanhThuNgay>
}

/**
 * Noi tao Retrofit dung chung cho toan bo ung dung.
 */
object BoTaoApi {
    fun taoApi(boiCanh: Context): ApiCuaHang {
        // Interceptor nay chi de xem request/response khi debug, khong bat body
        // log trong production vi response co the chua access token.
        val boGhiLog = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BASIC
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        // Interceptor tu dong gan access token cho cac request sau dang nhap.
        // Request login van hoat dong binh thuong vi khi do token co the chua ton tai.
        val ganMaTruyCap = Interceptor { chuoi ->
            val boNhoPhien = BoNhoPhienDangNhap(boiCanh.applicationContext)
            val maTruyCap = boNhoPhien.layMaTruyCap()
            val yeuCau = chuoi.request().newBuilder()

            if (!maTruyCap.isNullOrBlank()) {
                yeuCau.addHeader("Authorization", "Bearer $maTruyCap")
            }

            chuoi.proceed(yeuCau.build())
        }

        // Cau hinh timeout de app khong bi treo vo han neu may chu khong phan hoi.
        val khachHttp = OkHttpClient.Builder()
            .addInterceptor(ganMaTruyCap)
            .addInterceptor(boGhiLog)
            // Khi server tra 401, tu dong doi refresh token lay access token moi
            // roi thu lai request. Nho vay app khong bi dang xuat oan khi
            // access token het han trong luc nguoi dung mat mang.
            .authenticator(LamMoiTokenTuDong(boiCanh))
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        // DIA_CHI_API duoc dat trong build.gradle.kts de de thay doi theo moi truong.
        return Retrofit.Builder()
            .baseUrl(BuildConfig.DIA_CHI_API)
            .client(khachHttp)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiCuaHang::class.java)
    }
}
