import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { getMobileUser, mobileUnauthorizedResponse } from "@/lib/mobile-auth";
import { decimalToString, mobileErrorResponse } from "@/lib/mobile-http";

/** GET /api/v1/imports/history - Lich su phieu nhap tren mobile. */
export async function GET(request: NextRequest) {
  if (!(await getMobileUser(request))) {
    return mobileUnauthorizedResponse();
  }

  try {
    const { searchParams } = new URL(request.url);
    const page = Math.max(1, Number(searchParams.get("page") || "1"));
    const limit = Math.min(50, Math.max(1, Number(searchParams.get("limit") || "20")));

    const [invoices, total] = await Promise.all([
      prisma.importInvoice.findMany({
        orderBy: { createdAt: "desc" },
        skip: (page - 1) * limit,
        take: limit,
        include: {
          supplier: { select: { id: true, name: true } },
          items: { include: { product: { select: { id: true, name: true, sku: true } } } },
        },
      }),
      prisma.importInvoice.count(),
    ]);

    return NextResponse.json({
      data: invoices.map((invoice) => ({
        ...invoice,
        totalAmount: decimalToString(invoice.totalAmount),
        items: invoice.items.map((item) => ({
          ...item,
          unitPrice: decimalToString(item.unitPrice),
          previousUnitPrice: decimalToString(item.previousUnitPrice),
          totalPrice: decimalToString(item.totalPrice),
        })),
      })),
      pagination: {
        page,
        limit,
        total,
        totalPages: Math.ceil(total / limit),
      },
    });
  } catch (error) {
    console.error("Mobile imports history error:", error);
    return mobileErrorResponse("SERVER_ERROR", "Không thể tải lịch sử nhập hàng", 500);
  }
}
