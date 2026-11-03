import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { getMobileUser, mobileUnauthorizedResponse } from "@/lib/mobile-auth";
import { mobileErrorResponse } from "@/lib/mobile-http";

/**
 * GET /api/v1/suppliers - Danh sach nha cung cap cho mobile.
 *
 * @param search Tu khoa tim kiem theo ten, so dien thoai hoac email.
 * @param includeHidden Khi la "true" thi tra ca nha cung cap dang an.
 */
export async function GET(request: NextRequest) {
  if (!(await getMobileUser(request))) {
    return mobileUnauthorizedResponse();
  }

  try {
    const { searchParams } = new URL(request.url);
    const tuKhoa = searchParams.get("search")?.trim() || "";
    const includeHidden = searchParams.get("includeHidden") === "true";

    const nhaCungCap = await prisma.supplier.findMany({
      where: {
        ...(includeHidden ? {} : { isActive: true }),
        ...(tuKhoa
          ? {
              OR: [
                { name: { contains: tuKhoa, mode: "insensitive" as const } },
                { phone: { contains: tuKhoa } },
                { email: { contains: tuKhoa, mode: "insensitive" as const } },
              ],
            }
          : {}),
      },
      orderBy: { name: "asc" },
      include: { _count: { select: { importInvoices: true } } },
    });

    return NextResponse.json({ data: nhaCungCap });
  } catch (error) {
    console.error("Mobile suppliers error:", error);
    return mobileErrorResponse("SERVER_ERROR", "Không thể tải nhà cung cấp", 500);
  }
}

/** POST /api/v1/suppliers - Tao nha cung cap moi tu mobile. */
export async function POST(request: NextRequest) {
  if (!(await getMobileUser(request))) {
    return mobileUnauthorizedResponse();
  }

  try {
    const body = await request.json();
    const ten = String(body.name ?? "").trim();

    if (!ten) {
      return mobileErrorResponse("VALIDATION_ERROR", "Tên nhà cung cấp là bắt buộc", 400);
    }

    const nhaCungCap = await prisma.supplier.create({
      data: {
        name: ten,
        phone: String(body.phone ?? "").trim() || null,
        email: String(body.email ?? "").trim() || null,
        address: String(body.address ?? "").trim() || null,
      },
      include: { _count: { select: { importInvoices: true } } },
    });

    return NextResponse.json({ data: nhaCungCap }, { status: 201 });
  } catch (error) {
    console.error("Mobile create supplier error:", error);
    return mobileErrorResponse("SERVER_ERROR", "Không thể tạo nhà cung cấp", 500);
  }
}
