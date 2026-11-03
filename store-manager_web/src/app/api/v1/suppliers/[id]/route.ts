import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { getMobileUser, mobileUnauthorizedResponse } from "@/lib/mobile-auth";
import { mobileErrorResponse } from "@/lib/mobile-http";

type ThamSo = { params: Promise<{ id: string }> };

/** PUT /api/v1/suppliers/:id - Cap nhat nha cung cap tu mobile. */
export async function PUT(request: NextRequest, { params }: ThamSo) {
  if (!(await getMobileUser(request))) return mobileUnauthorizedResponse();

  try {
    const { id } = await params;
    const body = await request.json();

    const nhaCungCapCu = await prisma.supplier.findUnique({ where: { id } });
    if (!nhaCungCapCu) {
      return mobileErrorResponse("NOT_FOUND", "Không tìm thấy nhà cung cấp", 404);
    }

    const ten = String(body.name ?? nhaCungCapCu.name).trim();
    if (!ten) {
      return mobileErrorResponse("VALIDATION_ERROR", "Tên nhà cung cấp là bắt buộc", 400);
    }

    const nhaCungCap = await prisma.supplier.update({
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
      include: { _count: { select: { importInvoices: true } } },
    });

    return NextResponse.json({ data: nhaCungCap });
  } catch (error) {
    console.error("Mobile update supplier error:", error);
    return mobileErrorResponse("SERVER_ERROR", "Không thể cập nhật nhà cung cấp", 500);
  }
}

/**
 * DELETE /api/v1/suppliers/:id - Xoa nha cung cap tu mobile.
 *
 * Nha cung cap da co phieu nhap thi chi an di, khong xoa han, vi phieu nhap
 * cu can thong tin nha cung cap de doi soat ton kho.
 */
export async function DELETE(request: NextRequest, { params }: ThamSo) {
  if (!(await getMobileUser(request))) return mobileUnauthorizedResponse();

  try {
    const { id } = await params;

    const nhaCungCap = await prisma.supplier.findUnique({
      where: { id },
      include: { _count: { select: { importInvoices: true } } },
    });
    if (!nhaCungCap) {
      return mobileErrorResponse("NOT_FOUND", "Không tìm thấy nhà cung cấp", 404);
    }

    if (nhaCungCap._count.importInvoices > 0) {
      // An nha cung cap thay vi xoa de giu dung lich su phieu nhap.
      const daAn = await prisma.supplier.update({
        where: { id },
        data: { isActive: false },
      });
      return NextResponse.json({ data: { id: daAn.id, isActive: false } });
    }

    await prisma.supplier.delete({ where: { id } });
    return NextResponse.json({ data: { id } });
  } catch (error) {
    console.error("Mobile delete supplier error:", error);
    return mobileErrorResponse("SERVER_ERROR", "Không thể xóa nhà cung cấp", 500);
  }
}
