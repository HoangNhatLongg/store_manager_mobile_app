package com.example.store_manager.du_lieu.the_the_phong

import com.example.store_manager.du_lieu.DanhMuc
import com.example.store_manager.du_lieu.DonViSanPham
import com.example.store_manager.du_lieu.DonViTinh
import com.example.store_manager.du_lieu.KhachHang
import com.example.store_manager.du_lieu.SanPham

/**
 * Lop chuyen doi du lieu giua model dung chung cho UI va the thể Room.
 *
 * Model dung chung phuc vu giao dien va goi API, con the thể Room phuc vu
 * database cuc lo. Hai ben duoc chuyen doi qua lop nay de khong lan man.
 */
object ChuyenDoiDuLieu {

    /**
     * Chuyen danh muc cua server thanh the thể Room.
     *
     * @param danhMuc Danh muc tu server.
     * @return The thể Room tuong ung.
     */
    fun sangTheDanhMuc(danhMuc: DanhMuc): TheDanhMuc {
        return TheDanhMuc(
            id = danhMuc.id,
            ten = danhMuc.name,
            slug = danhMuc.slug
        )
    }

    /**
     * Chuyen danh muc Room thanh model dung chung.
     *
     * @param the The thể Room.
     * @return Model dung chung tuong ung.
     */
    fun sangModelDanhMuc(the: TheDanhMuc): DanhMuc {
        return DanhMuc(
            id = the.id,
            name = the.ten,
            slug = the.slug
        )
    }

    /**
     * Chuyen don vi tinh cua server thanh the thể Room.
     *
     * @param donViTinh Don vi tinh tu server.
     * @return The thể Room tuong ung.
     */
    fun sangTheDonViTinh(donViTinh: DonViTinh): TheDonViTinh {
        return TheDonViTinh(
            id = donViTinh.id,
            ten = donViTinh.name,
            vietTat = donViTinh.abbreviation
        )
    }

    /**
     * Chuyen don vi tinh Room thanh model dung chung.
     *
     * @param the The thể Room.
     * @return Model dung chung tuong ung.
     */
    fun sangModelDonViTinh(the: TheDonViTinh): DonViTinh {
        return DonViTinh(
            id = the.id,
            name = the.ten,
            abbreviation = the.vietTat
        )
    }

    /**
     * Chuyen don vi ban cua server thanh the thể Room.
     *
     * @param donViSanPham Don vi ban tu server.
     * @param maSanPham    Id san pham chua don vi ban nay.
     * @return The thể Room tuong ung.
     */
    fun sangTheDonViSanPham(
        donViSanPham: DonViSanPham,
        maSanPham: String
    ): TheDonViSanPham {
        return TheDonViSanPham(
            id = donViSanPham.id,
            maSanPham = maSanPham,
            maDonViTinh = donViSanPham.unitId,
            soLuongQuyDoi = donViSanPham.conversionQty,
            gia = donViSanPham.price,
            laMacDinh = donViSanPham.isDefault,
            laDonViGoc = donViSanPham.isPurchase
        )
    }

    /**
     * Chuyen khach hang cua server thanh the thể Room.
     *
     * @param khachHang Khach hang tu server.
     * @return The thể Room tuong ung.
     */
    fun sangTheKhachHang(khachHang: KhachHang): TheKhachHang {
        return TheKhachHang(
            id = khachHang.id,
            ten = khachHang.name,
            soDienThoai = khachHang.phone,
            email = khachHang.email,
            diaChi = khachHang.address
        )
    }

    /**
     * Chuyen khach hang Room thanh model dung chung.
     *
     * @param the The thể Room.
     * @return Model dung chung tuong ung.
     */
    fun sangModelKhachHang(the: TheKhachHang): KhachHang {
        return KhachHang(
            id = the.id,
            name = the.ten,
            phone = the.soDienThoai,
            email = the.email,
            address = the.diaChi
        )
    }

    /**
     * Chuyen san pham cua server thanh the thể Room.
     *
     * @param sanPham     San pham tu server.
     * @param capNhatLuc  Thoi diem server cap nhat lan cuoi.
     * @return The thể Room tuong ung.
     */
    fun sangTheSanPham(sanPham: SanPham, capNhatLuc: Long = 0L): TheSanPham {
        return TheSanPham(
            id = sanPham.id,
            ten = sanPham.name,
            maSku = sanPham.sku,
            maVach = sanPham.barcode,
            giaNhap = sanPham.importPrice,
            giaBan = sanPham.sellPrice,
            tonKho = sanPham.stock,
            tonToiThieu = sanPham.minStock,
            moTa = sanPham.description,
            anh = sanPham.imageUrl,
            maDanhMuc = sanPham.categoryId,
            dangHoatDong = sanPham.isActive,
            capNhatLuc = capNhatLuc
        )
    }

    /**
     * Chuyen san pham Room thanh model dung chung.
     *
     * @param the           The thể Room.
     * @param danhMuc       Danh muc tuong ung, null neu khong co.
     * @param donViSanPham  Danh sach don vi ban cua san pham.
     * @return Model dung chung tuong ung.
     */
    fun sangModelSanPham(
        the: TheSanPham,
        danhMuc: DanhMuc? = null,
        donViSanPham: List<DonViSanPham> = emptyList()
    ): SanPham {
        return SanPham(
            id = the.id,
            name = the.ten,
            sku = the.maSku,
            barcode = the.maVach,
            importPrice = the.giaNhap,
            sellPrice = the.giaBan,
            stock = the.tonKho,
            minStock = the.tonToiThieu,
            description = the.moTa,
            imageUrl = the.anh,
            categoryId = the.maDanhMuc,
            isActive = the.dangHoatDong,
            category = danhMuc,
            units = donViSanPham
        )
    }
}
