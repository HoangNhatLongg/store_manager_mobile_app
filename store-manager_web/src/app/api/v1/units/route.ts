import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { getMobileUser, mobileUnauthorizedResponse } from "@/lib/mobile-auth";
import { mobileErrorResponse } from "@/lib/mobile-http";

/**
 * GET /api/v1/units - Danh sach don vi tinh cho mobile.
 *
 * @param search Tu khoa tim kiem theo ten hoac viet tat.
 */
export async function GET(request: NextRequest) {
  if (!(await getMobileUser(request))) {
    return mobileUnauthorizedResponse();
  }

  try {
    const { searchParams } = new URL(request.url);
    const tuKhoa = searchParams.get("search")?.trim() || "";

    const donVi = await prisma.unit.findMany({
      where: tuKhoa
        ? {
            OR: [
              { name: { contains: tuKhoa, mode: "insensitive" as const } },
              { abbreviation: { contains: tuKhoa, mode: "insensitive" as const } },
            ],
          }
        : {},
      orderBy: [{ isBaseUnit: "desc" }, { name: "asc" }],
      include: { _count: { select: { products: true } } },
    });

    return NextResponse.json({ data: donVi });
  } catch (error) {
    console.error("Mobile units error:", error);
    return mobileErrorResponse("SERVER_ERROR", "Không thể tải đơn vị tính", 500);
  }
}

/** POST /api/v1/units - Tao don vi tinh moi tu mobile. */
export async function POST(request: NextRequest) {
  if (!(await getMobileUser(request))) {
    return mobileUnauthorizedResponse();
  }

  try {
    const body = await request.json();
    const ten = String(body.name ?? "").trim();
    const vietTat = String(body.abbreviation ?? "").trim().toLowerCase();
    const laDonViCoBan = Boolean(body.isBaseUnit);

    if (!ten) {
      return mobileErrorResponse("VALIDATION_ERROR", "Tên đơn vị là bắt buộc", 400);
    }
    if (!vietTat) {
      return mobileErrorResponse("VALIDATION_ERROR", "Viết tắt là bắt buộc", 400);
    }

    // Viet tat la khoa duy nhat trong database, nen phai kiem tra truoc.
    const trung = await prisma.unit.findUnique({ where: { abbreviation: vietTat } });
    if (trung) {
      return mobileErrorResponse("DUPLICATE", "Viết tắt đơn vị đã tồn tại", 400);
    }

    const donVi = await prisma.unit.create({
      data: { name: ten, abbreviation: vietTat, isBaseUnit: laDonViCoBan },
      include: { _count: { select: { products: true } } },
    });

    return NextResponse.json({ data: donVi }, { status: 201 });
  } catch (error) {
    console.error("Mobile create unit error:", error);
    return mobileErrorResponse("SERVER_ERROR", "Không thể tạo đơn vị tính", 500);
  }
}
