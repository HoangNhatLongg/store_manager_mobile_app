import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import type { Prisma } from "@prisma/client";
import { getMobileUser, mobileUnauthorizedResponse } from "@/lib/mobile-auth";
import { mobileErrorResponse, decimalToString } from "@/lib/mobile-http";

/**
 * GET /api/v1/imports/{id} - Chi tiet phieu nhap cho mobile.
 */
export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  if (!(await getMobileUser(request))) {
    return mobileUnauthorizedResponse();
  }

  try {
    const { id } = await params;

    const phieu = await prisma.importInvoice.findUnique({
      where: { id },
      include: {
        supplier: { select: { id: true, name: true, phone: true } },
        createdBy: { select: { id: true, name: true, email: true } },
        items: {
          include: {
            product: {
              select: {
                id: true,
                name: true,
                sku: true,
                importPrice: true,
                previousImportPrice: true,
                stock: true,
              },
            },
          },
        },
      },
    });

    if (!phieu) {
      return mobileErrorResponse("NOT_FOUND", "Không tìm thấy phiếu nhập", 404);
    }

    return NextResponse.json({ data: chuyenPhieuNhap(phieu) });
  } catch (error) {
    console.error("Mobile import detail error:", error);
    return mobileErrorResponse(
      "SERVER_ERROR",
      "Không thể tải chi tiết phiếu nhập",
      500
    );
  }
}

type PhieuNhapDayDu = Prisma.ImportInvoiceGetPayload<{
  include: {
    supplier: { select: { id: true; name: true; phone: true } };
    createdBy: { select: { id: true; name: true; email: true } };
    items: {
      include: {
        product: {
          select: {
            id: true;
            name: true;
            sku: true;
            importPrice: true;
            previousImportPrice: true;
            stock: true;
          };
        };
      };
    };
  };
}>;

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
    items: phieu.items.map((item) => ({
      ...item,
      unitPrice: decimalToString(item.unitPrice),
      previousUnitPrice: decimalToString(item.previousUnitPrice),
      totalPrice: decimalToString(item.totalPrice),
      product: item.product
        ? {
            ...item.product,
            importPrice: decimalToString(item.product.importPrice),
            previousImportPrice: decimalToString(item.product.previousImportPrice),
          }
        : null,
    })),
  };
}
