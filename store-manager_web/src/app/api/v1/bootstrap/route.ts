import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { getMobileUser, mobileUnauthorizedResponse } from "@/lib/mobile-auth";
import { mobileErrorResponse, decimalToString } from "@/lib/mobile-http";

/**
 * GET /api/v1/bootstrap
 *
 * Tải dữ liệu nền khi mobile đăng nhập lần đầu hoặc cần khôi phục local DB.
 * Dữ liệu hóa đơn lịch sử không đưa vào response này để tránh payload quá lớn.
 */
export async function GET(request: NextRequest) {
  const user = await getMobileUser(request);

  if (!user) {
    return mobileUnauthorizedResponse();
  }

  try {
    const [categories, products, units, customers, suppliers] =
      await Promise.all([
        prisma.category.findMany({
          where: { isActive: true },
          orderBy: { name: "asc" },
        }),
        prisma.product.findMany({
          where: { isActive: true },
          orderBy: [{ updatedAt: "asc" }, { id: "asc" }],
          include: {
            category: {
              select: { id: true, name: true, slug: true },
            },
            units: {
              orderBy: [{ isPurchase: "desc" }, { isDefault: "desc" }, { conversionQty: "asc" }],
              include: {
                unit: {
                  select: {
                    id: true,
                    name: true,
                    abbreviation: true,
                    isBaseUnit: true,
                  },
                },
              },
            },
          },
        }),
        prisma.unit.findMany({
          orderBy: [{ isBaseUnit: "desc" }, { name: "asc" }],
        }),
        prisma.customer.findMany({
          where: { isActive: true },
          orderBy: { name: "asc" },
        }),
        prisma.supplier.findMany({
          where: { isActive: true },
          orderBy: { name: "asc" },
        }),
      ]);

    return NextResponse.json({
      serverTime: new Date().toISOString(),
      data: {
        categories,
        products: products.map((product) => ({
          ...product,
          importPrice: decimalToString(product.importPrice),
          previousImportPrice: decimalToString(product.previousImportPrice),
          sellPrice: decimalToString(product.sellPrice),
          previousSellPrice: decimalToString(product.previousSellPrice),
          units: product.units.map((productUnit) => ({
            ...productUnit,
            price: decimalToString(productUnit.price),
          })),
        })),
        units,
        customers,
        suppliers,
      },
    });
  } catch (error) {
    console.error("Mobile bootstrap error:", error);

    return mobileErrorResponse(
      "SERVER_ERROR",
      "Không thể tải dữ liệu khởi tạo",
      500
    );
  }
}
