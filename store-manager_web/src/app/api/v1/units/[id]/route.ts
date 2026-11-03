import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { getMobileUser, mobileUnauthorizedResponse } from "@/lib/mobile-auth";
import { mobileErrorResponse } from "@/lib/mobile-http";

type ThamSo = { params: Promise<{ id: string }> };

/** PUT /api/v1/units/:id - Cap nhat don vi tinh tu mobile. */
export async function PUT(request: NextRequest, { params }: ThamSo) {
  if (!(await getMobileUser(request))) return mobileUnauthorizedResponse();

  try {
    const { id } = await params;
    const body = await request.json();

    const donViCu = await prisma.unit.findUnique({ where: { id } });
    if (!donViCu) {
      return mobileErrorResponse("NOT_FOUND", "Không tìm thấy đơn vị tính", 404);
    }

    const ten = String(body.name ?? donViCu.name).trim();
    const vietTat = String(body.abbreviation ?? donViCu.abbreviation).trim().toLowerCase();
    const laDonViCoBan = body.isBaseUnit === undefined
      ? donViCu.isBaseUnit
      : Boolean(body.isBaseUnit);

    if (!ten) {
      return mobileErrorResponse("VALIDATION_ERROR", "Tên đơn vị là bắt buộc", 400);
    }
    if (!vietTat) {
      return mobileErrorResponse("VALIDATION_ERROR", "Viết tắt là bắt buộc", 400);
    }

    // Viet tat la khoa duy nhat, nen can kiem tra truoc khi cap nhat.
    const trung = await prisma.unit.findFirst({
      where: { abbreviation: vietTat, NOT: { id } },
    });
    if (trung) {
      return mobileErrorResponse("DUPLICATE", "Viết tắt đơn vị đã tồn tại", 400);
    }

    const donVi = await prisma.unit.update({
      where: { id },
      data: { name: ten, abbreviation: vietTat, isBaseUnit: laDonViCoBan },
      include: { _count: { select: { products: true } } },
    });

    return NextResponse.json({ data: donVi });
  } catch (error) {
    console.error("Mobile update unit error:", error);
    return mobileErrorResponse("SERVER_ERROR", "Không thể cập nhật đơn vị tính", 500);
  }
}

/** DELETE /api/v1/units/:id - Xoa don vi tinh tu mobile. */
export async function DELETE(request: NextRequest, { params }: ThamSo) {
  if (!(await getMobileUser(request))) return mobileUnauthorizedResponse();

  try {
    const { id } = await params;

    const donVi = await prisma.unit.findUnique({
      where: { id },
      include: { _count: { select: { products: true } } },
    });
    if (!donVi) {
      return mobileErrorResponse("NOT_FOUND", "Không tìm thấy đơn vị tính", 404);
    }

    // Don vi dang gan voi san pham thi khong xoa duoc.
    if (donVi._count.products > 0) {
      return mobileErrorResponse(
        "UNIT_IN_USE",
        "Đơn vị đang được dùng cho sản phẩm, không thể xóa.",
        409
      );
    }

    await prisma.unit.delete({ where: { id } });
    return NextResponse.json({ data: { id } });
  } catch (error) {
    console.error("Mobile delete unit error:", error);
    return mobileErrorResponse("SERVER_ERROR", "Không thể xóa đơn vị tính", 500);
  }
}
