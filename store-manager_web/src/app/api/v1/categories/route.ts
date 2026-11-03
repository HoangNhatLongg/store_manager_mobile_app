import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { getMobileUser, mobileUnauthorizedResponse } from "@/lib/mobile-auth";
import { mobileErrorResponse } from "@/lib/mobile-http";

/**
 * Tao slug tu ten danh muc, bo dau va ky tu dac biet.
 *
 * Slug dung lam dinh danh duy nhat nen phai sinh tu ten de tranh trung.
 */
function taoSlug(ten: string): string {
  return ten
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
}

/**
 * GET /api/v1/categories - Danh sach danh muc cho mobile.
 *
 * @param includeHidden Khi la "true" thi tra ca danh muc dang an.
 */
export async function GET(request: NextRequest) {
  if (!(await getMobileUser(request))) {
    return mobileUnauthorizedResponse();
  }

  try {
    const { searchParams } = new URL(request.url);
    const includeHidden = searchParams.get("includeHidden") === "true";
    const tuKhoa = searchParams.get("search")?.trim() || "";

    const danhMuc = await prisma.category.findMany({
      where: {
        ...(includeHidden ? {} : { isActive: true }),
        ...(tuKhoa
          ? { name: { contains: tuKhoa, mode: "insensitive" as const } }
          : {}),
      },
      orderBy: { name: "asc" },
      include: { _count: { select: { products: true } } },
    });

    return NextResponse.json({ data: danhMuc });
  } catch (error) {
    console.error("Mobile categories error:", error);
    return mobileErrorResponse("SERVER_ERROR", "Không thể tải danh mục", 500);
  }
}

/** POST /api/v1/categories - Tao danh muc moi tu mobile. */
export async function POST(request: NextRequest) {
  if (!(await getMobileUser(request))) {
    return mobileUnauthorizedResponse();
  }

  try {
    const body = await request.json();
    const ten = String(body.name ?? "").trim();
    const moTa = String(body.description ?? "").trim();

    if (!ten) {
      return mobileErrorResponse("VALIDATION_ERROR", "Tên danh mục là bắt buộc", 400);
    }

    // Danh muc trung ten se lam bo loc san pham bi nhieu nghia.
    const trung = await prisma.category.findFirst({ where: { name: ten } });
    if (trung) {
      return mobileErrorResponse("DUPLICATE", "Tên danh mục đã tồn tại", 400);
    }

    const danhMuc = await prisma.category.create({
      data: { name: ten, slug: taoSlug(ten), description: moTa || null },
      include: { _count: { select: { products: true } } },
    });

    return NextResponse.json({ data: danhMuc }, { status: 201 });
  } catch (error) {
    console.error("Mobile create category error:", error);
    return mobileErrorResponse("SERVER_ERROR", "Không thể tạo danh mục", 500);
  }
}
