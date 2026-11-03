import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { getMobileUser, mobileUnauthorizedResponse } from "@/lib/mobile-auth";
import { decimalToString, mobileErrorResponse } from "@/lib/mobile-http";

/** GET /api/v1/product-units?productId=... */
export async function GET(request: NextRequest) {
  if (!(await getMobileUser(request))) return mobileUnauthorizedResponse();
  const productId = new URL(request.url).searchParams.get("productId");
  if (!productId) return mobileErrorResponse("VALIDATION_ERROR", "productId là bắt buộc", 400);

  try {
    const data = await prisma.productUnit.findMany({
      where: { productId },
      include: { unit: true },
      orderBy: [{ isDefault: "desc" }, { conversionQty: "asc" }],
    });
    return NextResponse.json({ data: data.map((item) => ({ ...item, price: decimalToString(item.price) })) });
  } catch (error) {
    console.error("Mobile product units error:", error);
    return mobileErrorResponse("SERVER_ERROR", "Không thể tải đơn vị sản phẩm", 500);
  }
}

/** POST /api/v1/product-units */
export async function POST(request: NextRequest) {
  if (!(await getMobileUser(request))) return mobileUnauthorizedResponse();
  try {
    const body = await request.json();
    const { productId, unitId, conversionQty, price, isDefault, isPurchase } = body;
    const soLuongQuyDoi = Number(conversionQty ?? 1);
    const gia = Number(price);
    if (!productId || !unitId || !Number.isInteger(soLuongQuyDoi) || soLuongQuyDoi < 1 || !Number.isFinite(gia) || gia <= 0) {
      return mobileErrorResponse("VALIDATION_ERROR", "Thiếu sản phẩm, đơn vị hoặc giá", 400);
    }

    const [sanPham, donViTinh] = await Promise.all([
      prisma.product.findUnique({ where: { id: productId }, select: { id: true } }),
      prisma.unit.findUnique({ where: { id: unitId }, select: { id: true } }),
    ]);

    if (!sanPham) {
      return mobileErrorResponse("NOT_FOUND", "Không tìm thấy sản phẩm", 404);
    }

    if (!donViTinh) {
      return mobileErrorResponse("NOT_FOUND", "Không tìm thấy đơn vị tính", 404);
    }

    const daTonTai = await prisma.productUnit.findUnique({
      where: { productId_unitId: { productId, unitId } },
      select: { id: true },
    });

    if (daTonTai) {
      return mobileErrorResponse("DUPLICATE", "Sản phẩm đã có đơn vị này", 400);
    }
    const item = await prisma.$transaction(async (tx) => {
      // Don vi ban le mac dinh luon co he so quy doi bang 1.
      const laMacDinh = Boolean(isDefault);
      if (laMacDinh) {
        await tx.productUnit.updateMany({ where: { productId }, data: { isDefault: false } });
      }
      // Chi mot don vi goc duoc dung de nhap hang cho moi san pham.
      if (isPurchase) {
        await tx.productUnit.updateMany({ where: { productId }, data: { isPurchase: false } });
      }
      return tx.productUnit.create({
        data: {
          productId,
          unitId,
          conversionQty: laMacDinh ? 1 : soLuongQuyDoi,
          price: gia,
          isDefault: laMacDinh,
          isPurchase: Boolean(isPurchase),
        },
        include: { unit: true },
      });
    });
    return NextResponse.json({ data: { ...item, price: decimalToString(item.price) } }, { status: 201 });
  } catch (error) {
    console.error("Mobile create product unit error:", error);
    return mobileErrorResponse("SERVER_ERROR", "Không thể thêm đơn vị sản phẩm", 500);
  }
}
