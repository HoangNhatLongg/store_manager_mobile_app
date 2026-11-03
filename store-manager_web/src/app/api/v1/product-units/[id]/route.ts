import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { getMobileUser, mobileUnauthorizedResponse } from "@/lib/mobile-auth";
import { decimalToString, mobileErrorResponse } from "@/lib/mobile-http";
import { LoiDonViSanPham } from "@/lib/don-vi-san-pham";

type ThamSo = { params: Promise<{ id: string }> };

/** PUT /api/v1/product-units/:id */
export async function PUT(request: NextRequest, { params }: ThamSo) {
  if (!(await getMobileUser(request))) return mobileUnauthorizedResponse();
  try {
    const { id } = await params;
    const body = await request.json();
    const current = await prisma.productUnit.findUnique({ where: { id } });
    if (!current) return mobileErrorResponse("NOT_FOUND", "Không tìm thấy đơn vị sản phẩm", 404);

    if (body.conversionQty !== undefined && (!Number.isInteger(Number(body.conversionQty)) || Number(body.conversionQty) < 1)) {
      return mobileErrorResponse("VALIDATION_ERROR", "Số lượng quy đổi phải là số nguyên lớn hơn 0", 400);
    }
    if (body.price !== undefined && (!Number.isFinite(Number(body.price)) || Number(body.price) <= 0)) {
      return mobileErrorResponse("VALIDATION_ERROR", "Giá theo đơn vị phải lớn hơn 0", 400);
    }

    const item = await prisma.$transaction(async (tx) => {
      // Don vi ban le mac dinh luon co he so quy doi bang 1.
      const laMacDinh =
        body.isDefault === undefined ? current.isDefault : Boolean(body.isDefault);
      const heSoQuyDoi = laMacDinh
        ? 1
        : body.conversionQty === undefined
          ? current.conversionQty
          : Number(body.conversionQty);

      if (laMacDinh && !current.isDefault) {
        await tx.productUnit.updateMany({ where: { productId: current.productId }, data: { isDefault: false } });
      }

      // Khong cho bo co "ban le mac dinh" khi day la don vi mac dinh duy nhat
      // cua san pham, vi ton kho duoc dem theo don vi nay.
      if (current.isDefault && !laMacDinh) {
        const soMacDinhKhac = await tx.productUnit.count({
          where: { productId: current.productId, isDefault: true, id: { not: current.id } },
        });
        if (soMacDinhKhac === 0) {
          throw new LoiDonViSanPham(
            "CANNOT_REMOVE_LAST_DEFAULT",
            "Không thể bỏ đơn vị bán lẻ mặc định duy nhất của sản phẩm. Hãy đặt một đơn vị khác làm mặc định trước.",
            409
          );
        }
      }
      // Chi mot don vi goc duoc dung de nhap hang cho moi san pham.
      if (body.isPurchase && !current.isPurchase) {
        await tx.productUnit.updateMany({ where: { productId: current.productId }, data: { isPurchase: false } });
      }
      return tx.productUnit.update({
        where: { id },
        data: {
          unitId: body.unitId ?? undefined,
          conversionQty: heSoQuyDoi,
          price: body.price === undefined ? undefined : Number(body.price),
          isDefault: laMacDinh,
          isPurchase: body.isPurchase === undefined ? undefined : Boolean(body.isPurchase),
        },
        include: { unit: true },
      });
    });
    return NextResponse.json({ data: { ...item, price: decimalToString(item.price) } });
  } catch (error) {
    if (error instanceof LoiDonViSanPham) {
      return mobileErrorResponse(error.maLoi, error.message, error.trangThai);
    }

    console.error("Mobile update product unit error:", error);
    return mobileErrorResponse("SERVER_ERROR", "Không thể cập nhật đơn vị sản phẩm", 500);
  }
}

/** DELETE /api/v1/product-units/:id */
export async function DELETE(request: NextRequest, { params }: ThamSo) {
  if (!(await getMobileUser(request))) return mobileUnauthorizedResponse();
  try {
    const { id } = await params;
    const current = await prisma.productUnit.findUnique({ where: { id } });
    if (!current) return mobileErrorResponse("NOT_FOUND", "Không tìm thấy đơn vị sản phẩm", 404);

    // Khong cho xoa don vi ban le mac dinh cuoi cung cua san pham.
    if (current.isDefault) {
      const soDonViKhac = await prisma.productUnit.count({
        where: { productId: current.productId, id: { not: current.id } },
      });
      if (soDonViKhac === 0) {
        return mobileErrorResponse(
          "CANNOT_DELETE_LAST_DEFAULT",
          "Không thể xóa đơn vị bán lẻ mặc định duy nhất của sản phẩm",
          409
        );
      }
    }

    await prisma.productUnit.delete({ where: { id } });
    return NextResponse.json({ data: { id } });
  } catch (error) {
    console.error("Mobile delete product unit error:", error);
    return mobileErrorResponse("SERVER_ERROR", "Không thể xóa đơn vị sản phẩm", 500);
  }
}
