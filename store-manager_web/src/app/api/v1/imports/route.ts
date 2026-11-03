import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { getMobileUser, mobileUnauthorizedResponse } from "@/lib/mobile-auth";
import { mobileErrorResponse, decimalToString } from "@/lib/mobile-http";
import {
  LoiNhapHang,
  taoPhieuNhapVaTangTon,
  type DongHangNhapInput,
} from "@/lib/nhap-hang";

/**
 * POST /api/v1/imports - Tao phieu nhap hang cho mobile.
 *
 * Toan bo viec tang ton kho, cap nhat gia nhap va tao phieu + dong hang
 * nam trong mot transaction, nen khong co truong hop ton kho da tang nhung
 * phieu nhap lai khong tao duoc.
 *
 * Voi dong hang chua co productId, server se:
 * - tim san pham trung ten (khong phan biet hoa thuong), neu co thi dung
 *   san pham do;
 * - neu khong co thi tu tao san pham moi voi SKU va slug sinh tu dong.
 *
 * Moi dong hang phai co don vi goc (thung, loc...). So luong nhap tinh theo
 * don vi goc, ton kho duoc tang theo don vi ban le co ban.
 */
export async function POST(request: NextRequest) {
  const user = await getMobileUser(request);

  if (!user) {
    return mobileUnauthorizedResponse();
  }

  try {
    const body = await request.json();
    const supplierId = readNullableString(body.supplierId);
    const notes = readNullableString(body.notes);
    const items = readItems(body.items);

    if (!items) {
      return mobileErrorResponse(
        "VALIDATION_ERROR",
        "Danh sách sản phẩm không hợp lệ",
        400
      );
    }

    // Nha cung cap phai ton tai va dang hoat dong neu co truyen vao.
    if (supplierId) {
      const supplier = await prisma.supplier.findUnique({
        where: { id: supplierId },
        select: { id: true, isActive: true },
      });

      if (!supplier || !supplier.isActive) {
        return mobileErrorResponse(
          "SUPPLIER_NOT_FOUND",
          "Không tìm thấy nhà cung cấp đang hoạt động",
          404
        );
      }
    }

    const { phieu } = await prisma.$transaction(async (tx) => {
      return taoPhieuNhapVaTangTon(tx, {
        supplierId,
        notes,
        createdById: user.userId,
        items,
      });
    });

    return NextResponse.json(
      { data: chuyenPhieuNhap(phieu) },
      { status: 201 }
    );
  } catch (error) {
    if (error instanceof LoiNhapHang) {
      return mobileErrorResponse(error.maLoi, error.message, error.trangThai);
    }

    console.error("Mobile import error:", error);

    return mobileErrorResponse(
      "SERVER_ERROR",
      "Không thể tạo phiếu nhập hàng",
      500
    );
  }
}

/**
 * Doc va chuan hoa danh sach dong hang tu body.
 *
 * @return Mang dong hang da chuan hoa, null neu du lieu khong hop le.
 */
function readItems(value: unknown): DongHangNhapInput[] | null {
  if (!Array.isArray(value) || value.length === 0) {
    return null;
  }

  const items: DongHangNhapInput[] = [];

  for (const rawItem of value as Record<string, unknown>[]) {
    const maSanPham = readNullableString(rawItem.productId);
    const tenSanPham = readRequiredString(rawItem.productName);
    const maDanhMuc = readNullableString(rawItem.categoryId);
    const soLuong = readPositiveInteger(rawItem.quantity);
    const donGia = readPositiveNumber(rawItem.unitPrice);
    const maDonViGoc = readNullableString(rawItem.unitId);
    const maDonViCoBan = readNullableString(rawItem.baseUnitId);
    const heSoQuyDoi = readPositiveInteger(rawItem.conversionQty);

    // Dong hang phai co ten san pham va so luong, don gia hop le.
    if (!tenSanPham || soLuong === null || donGia === null) {
      return null;
    }

    items.push({
      productId: maSanPham,
      productName: tenSanPham,
      categoryId: maDanhMuc,
      quantity: soLuong,
      unitPrice: donGia,
      unitId: maDonViGoc,
      baseUnitId: maDonViCoBan,
      conversionQty: heSoQuyDoi,
    });
  }

  return items;
}

function readRequiredString(value: unknown) {
  if (typeof value !== "string") {
    return null;
  }

  const result = value.trim();
  return result || null;
}

function readNullableString(value: unknown) {
  if (value === null || value === undefined) {
    return null;
  }

  return readRequiredString(value);
}

function readPositiveInteger(value: unknown) {
  if (value === null || value === undefined || value === "") {
    return null;
  }

  const number = typeof value === "number" ? value : Number(value);

  return Number.isInteger(number) && number > 0 ? number : null;
}

function readPositiveNumber(value: unknown) {
  const number = typeof value === "number" ? value : Number(value);

  return Number.isFinite(number) && number > 0 ? number : null;
}

type PhieuNhapDayDu = any;

/**
 * Chuyen phieu nhap sang dang JSON phu hop cho mobile.
 *
 * Gia tien doi thanh chuoi de Android tinh toan khong bi sai so lam tron.
 */
function chuyenPhieuNhap(phieu: PhieuNhapDayDu) {
  return {
    ...phieu,
    totalAmount: decimalToString(phieu.totalAmount),
    createdAt: phieu.createdAt.toISOString(),
    updatedAt: phieu.updatedAt.toISOString(),
    items: phieu.items.map((item: any) => ({
      ...item,
      unitPrice: decimalToString(item.unitPrice),
      previousUnitPrice: decimalToString(item.previousUnitPrice),
      totalPrice: decimalToString(item.totalPrice),
    })),
  };
}
