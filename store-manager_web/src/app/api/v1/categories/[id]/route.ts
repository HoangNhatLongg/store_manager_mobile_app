import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { getMobileUser, mobileUnauthorizedResponse } from "@/lib/mobile-auth";
import { mobileErrorResponse } from "@/lib/mobile-http";

type ThamSo = { params: Promise<{ id: string }> };

/** Tao slug tu ten danh muc, bo dau va ky tu dac biet. */
function taoSlug(ten: string): string {
  return ten
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
}

/** PUT /api/v1/categories/:id - Cap nhat danh muc tu mobile. */
export async function PUT(request: NextRequest, { params }: ThamSo) {
  if (!(await getMobileUser(request))) return mobileUnauthorizedResponse();

  try {
    const { id } = await params;
    const body = await request.json();

    const danhMucCu = await prisma.category.findUnique({ where: { id } });
    if (!danhMucCu) {
      return mobileErrorResponse("NOT_FOUND", "Không tìm thấy danh mục", 404);
    }

    const ten = String(body.name ?? danhMucCu.name).trim();
    const moTa = String(body.description ?? danhMucCu.description ?? "").trim();

    if (!ten) {
      return mobileErrorResponse("VALIDATION_ERROR", "Tên danh mục là bắt buộc", 400);
    }

    // Ten trung voi danh muc khac se lam bo loc san pham bi nhieu nghia.
    const trung = await prisma.category.findFirst({
      where: { name: ten, NOT: { id } },
    });
    if (trung) {
      return mobileErrorResponse("DUPLICATE", "Tên danh mục đã tồn tại", 400);
    }

    const danhMuc = await prisma.category.update({
      where: { id },
      data: { name: ten, slug: taoSlug(ten), description: moTa || null },
      include: { _count: { select: { products: true } } },
    });

    return NextResponse.json({ data: danhMuc });
  } catch (error) {
    console.error("Mobile update category error:", error);
    return mobileErrorResponse("SERVER_ERROR", "Không thể cập nhật danh mục", 500);
  }
}

/** DELETE /api/v1/categories/:id - Xoa danh muc tu mobile. */
export async function DELETE(request: NextRequest, { params }: ThamSo) {
  if (!(await getMobileUser(request))) return mobileUnauthorizedResponse();

  try {
    const { id } = await params;

    const danhMuc = await prisma.category.findUnique({
      where: { id },
      include: { _count: { select: { products: true } } },
    });
    if (!danhMuc) {
      return mobileErrorResponse("NOT_FOUND", "Không tìm thấy danh mục", 404);
    }

    // Danh muc con san pham thi khong xoa duoc, neu xoa se lam san pham mat danh muc.
    if (danhMuc._count.products > 0) {
      return mobileErrorResponse(
        "CATEGORY_HAS_PRODUCTS",
        "Không thể xóa danh mục đang có sản phẩm. Hãy chuyển sản phẩm sang danh mục khác trước.",
        409
      );
    }

    await prisma.category.delete({ where: { id } });
    return NextResponse.json({ data: { id } });
  } catch (error) {
    console.error("Mobile delete category error:", error);
    return mobileErrorResponse("SERVER_ERROR", "Không thể xóa danh mục", 500);
  }
}
