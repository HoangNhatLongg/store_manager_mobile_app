package com.example.store_manager.du_lieu.the_the_phong

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Trang thai cua hoa don ban trong database cuc lo.
 *
 * - CHUA_DONG_BO: da tao tren mayy nhung chua gui len server.
 * - DA_DONG_BO: server da nhan va tra ve so hoa don that.
 * - XUNG_DOT: server tu choi vi khong du ton kho.
 * - THAT_BAI: server tu choi vi du lieu khong hop le.
 */
enum class TrangThaiHoaDon {
    CHUA_DONG_BO,
    DA_DONG_BO,
    XUNG_DOT,
    THAT_BAI
}

/**
 * The thể hoa don ban trong database cuc lo.
 *
 * Khi mat mang, hoa don duoc ghi vao bang nay truoc, kem operationId de khi
 * co ket noi gui len server ma khong tao trung hoa don.
 *
 * @param id                 Id cua hoa don cuc bo, sinh boi mayy.
 * @param operationId        Idempotency key gui len server.
 * @param soHoaDon           So hoa don do server tra ve, null neu chua dong bo.
 * @param maKhachHang       Id khach hang, null neu khach le.
 * @param tamTinh            Tong tien tam tinh truoc khi giam gia.
 * @param giamGia            Tong tien giam.
 * @param tongCong           Tong cong sau khi giam gia.
 * @param phuongThucThanhToan Phuong thuc thanh toan theo ma server.
 * @param ghiChu             Ghi chu hoa don.
 * @param trangThai          Trang thai dong bo, xem TrangThaiHoaDon.
 * @param soHoaDonServer     Id hoa don tren server sau khi dong bo thanh cong.
 * @param thongBaoLoi        Thong bao loi khi dong bo that bai.
 * @param taoLuc             Thoi diem tao hoa don cuc bo.
 */
@Entity(tableName = "hoa_don_ban", indices = [Index("operationId"), Index("trangThai")])
data class TheHoaDonBan(
    @PrimaryKey val id: String,
    val operationId: String,
    val soHoaDon: String? = null,
    val maKhachHang: String? = null,
    val tamTinh: Double = 0.0,
    val giamGia: Double = 0.0,
    val tongCong: Double = 0.0,
    val phuongThucThanhToan: String = "CASH",
    val ghiChu: String? = null,
    val trangThai: TrangThaiHoaDon = TrangThaiHoaDon.CHUA_DONG_BO,
    val soHoaDonServer: String? = null,
    val thongBaoLoi: String? = null,
    val taoLuc: Long = 0L
)

/**
 * The thể dong hang cua hoa don ban trong database cuc lo.
 *
 * So luong luong theo don vi ban, he so quy doi giup quy doi ve don vi co ban.
 *
 * @param id            Id cua dong hang cuc bo.
 * @param maHoaDon      Id hoa don chua dong hang nay.
 * @param maSanPham     Id san pham.
 * @param tenSanPham    Ten san pham, luu lai de hien thi khi offline.
 * @param soLuong       So luong theo don vi ban.
 * @param heSoQuyDoi    So don vi co ban trong 1 don vi ban.
 * @param donGia        Gia cua 1 don vi ban.
 * @param thanhTien     Thanh tien cua dong hang.
 * @param tenDonViBan   Ten don vi ban de hien thi.
 */
@Entity(tableName = "dong_hoa_don_ban", indices = [Index("maHoaDon")])
data class TheDongHoaDonBan(
    @PrimaryKey val id: String,
    val maHoaDon: String,
    val maSanPham: String,
    val tenSanPham: String = "",
    val soLuong: Int = 1,
    val heSoQuyDoi: Int = 1,
    val donGia: Double = 0.0,
    val thanhTien: Double = 0.0,
    val tenDonViBan: String = ""
)

/**
 * The thể phan tu trong hang doi dong bo.
 *
 * Hang doi luu day payload de gui lai len server khi co ket noi. Moi phan tu
 * giu operationId de thao tac luon idempotent.
 *
 * @param id             Id cua phan tu hang doi.
 * @param operationId    Idempotency key cua thao tac.
 * @param loaiThaoTac    Loai thao tac, vi du "TAO_HOA_DON_BAN".
 * @param duLieuJson     Payload dang JSON de gui len server.
 * @param soLanThu       So lan da thu gui.
 * @param thongBaoLoi    Thong bao loi cua lan thu gan nhat.
 * @param trangThai      Trang thai: CHO_GUI / DANG_GUI / XUNG_DOT / THAT_BAI.
 * @param taoLuc         Thoi diem dua phan tu vao hang doi.
 */
@Entity(tableName = "hang_dong_bo", indices = [Index("operationId"), Index("trangThai")])
data class TheHangDongBo(
    @PrimaryKey val id: String,
    val operationId: String,
    val loaiThaoTac: String,
    val duLieuJson: String,
    val soLanThu: Int = 0,
    val thongBaoLoi: String? = null,
    val trangThai: String = "CHO_GUI",
    val taoLuc: Long = 0L
)
