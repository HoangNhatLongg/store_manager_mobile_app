import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { getMobileUser, mobileUnauthorizedResponse } from "@/lib/mobile-auth";
import { mobileErrorResponse } from "@/lib/mobile-http";

type ThamSo = { params: Promise<{ id: string }> };

/** PUT /api/v1/customers/:id - Cap nhat khach hang tu mobile. */
export async function PUT(request: NextRequest, { params }: ThamSo) {
  if (!(await getMobileUser(request))) return mobileUnauthorizedResponse();

  try {
    const { id } = await params;
    const body = await request.json();

    const khachHangCu = await prisma.customer.findUnique({ where: { id } });
    if (!khachHangCu) {
      return mobileErrorResponse("NOT_FOUND", "Không tìm thấy khách hàng", 404);
    }

    const ten = String(body.name ?? khachHangCu.name).trim();
    if (!ten) {
      return mobileErrorResponse("VALIDATION_ERROR", "Tên khách hàng là bắt buộc", 400);
    }

    const khachHang = await prisma.customer.update({
      where: { id },
      data: {
        name: ten,
        phone: body.phone === undefined
          ? undefined
          : String(body.phone).trim() || null,
        email: body.email === undefined
          ? undefined
          : String(body.email).trim() || null,
        address: body.address === undefined
          ? undefined
          : String(body.address).trim() || null,
      },
      include: { _count: { select: { saleInvoices: true } } },
    });

    return NextResponse.json({ data: khachHang });
  } catch (error) {
    console.error("Mobile update customer error:", error);
    return mobileErrorResponse("SERVER_ERROR", "Không thể cập nhật khách hàng", 500);
  }
}

/**
 * DELETE /api/v1/customers/:id - Xoa khach hang tu mobile.
 *
 * Khach hang da phat sinh hoa don thi chi an di, khong xoa han, vi hoa don
 * cu can thong tin khach de doi soat.
 */
export async function DELETE(request: NextRequest, { params }: ThamSo) {
  if (!(await getMobileUser(request))) return mobileUnauthorizedResponse();

  try {
    const { id } = await params;

    const khachHang = await prisma.customer.findUnique({
      where: { id },
      include: { _count: { select: { saleInvoices: true } } },
    });
    if (!khachHang) {
      return mobileErrorResponse("NOT_FOUND", "Không tìm thấy khách hàng", 404);
    }

    if (khachHang._count.saleInvoices > 0) {
      // An khach thay vi xoa de giu dung lich su hoa don.
      const daAn = await prisma.customer.update({
        where: { id },
        data: { isActive: false },
      });
      return NextResponse.json({ data: { id: daAn.id, isActive: false } });
    }

    await prisma.customer.delete({ where: { id } });
    return NextResponse.json({ data: { id } });
  } catch (error) {
    console.error("Mobile delete customer error:", error);
    return mobileErrorResponse("SERVER_ERROR", "Không thể xóa khách hàng", 500);
  }
}
