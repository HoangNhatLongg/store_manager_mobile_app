import { NextRequest, NextResponse } from "next/server";
import { Prisma } from "@prisma/client";
import { prisma } from "@/lib/prisma";
import { getMobileUser, mobileUnauthorizedResponse } from "@/lib/mobile-auth";
import { mobileErrorResponse } from "@/lib/mobile-http";

/**
 * GET /api/v1/inventory/stats
 *
 * Thong ke ton kho tren toan bo san pham, dung cho bon the so lieu o dau
 * man hinh ton kho. So lieu khong phu thuoc phan trang nen luon phan anh
 * dung toan bo cua hang chu khong phai rieng trang dang xem.
 */
export async function GET(request: NextRequest) {
  const user = await getMobileUser(request);

  if (!user) {
    return mobileUnauthorizedResponse();
  }

  try {
    const { searchParams } = new URL(request.url);
    const categoryId = searchParams.get("categoryId") || null;
    const search = searchParams.get("search")?.trim() || "";

    // Bo loc giong het voi API danh sach san pham de so lieu khop voi
    // danh sach nguoi dung dang xem.
    const dieuKien: Prisma.Sql[] = [Prisma.sql`p."isActive" = true`];

    if (categoryId) {
      dieuKien.push(Prisma.sql`p."categoryId" = ${categoryId}`);
    }

    if (search) {
      dieuKien.push(
        Prisma.sql`(
          p."name" ILIKE ${`%${search}%`}
          OR p."sku" ILIKE ${`%${search}%`}
          OR p."barcode" ILIKE ${`%${search}%`}
        )`
      );
    }

    const dieuKienChung = Prisma.join(dieuKien, " AND ");

    // Prisma khong so sanh duoc hai truong cua cung mot bang, nen phai
    // dem truc tiep bang SQL de so sanh "stock" voi "minStock".
    const ketQua = await prisma.$queryRaw<
      Array<{
        tongSanPham: bigint;
        hetHang: bigint;
        sapHet: bigint;
      }>
    >`
      SELECT
        COUNT(*) AS "tongSanPham",
        COUNT(*) FILTER (WHERE p."stock" = 0) AS "hetHang",
        COUNT(*) FILTER (WHERE p."stock" > 0 AND p."stock" <= p."minStock") AS "sapHet"
      FROM "products" p
      WHERE ${dieuKienChung}
    `;

    const dong = ketQua[0];
    const tongSanPham = Number(dong?.tongSanPham ?? 0);
    const hetHang = Number(dong?.hetHang ?? 0);
    const sapHet = Number(dong?.sapHet ?? 0);

    return NextResponse.json({
      data: {
        totalProducts: tongSanPham,
        outOfStock: hetHang,
        lowStock: sapHet,
        inStock: tongSanPham - hetHang - sapHet,
      },
      serverTime: new Date().toISOString(),
    });
  } catch (error) {
    console.error("Mobile inventory stats error:", error);

    return mobileErrorResponse(
      "SERVER_ERROR",
      "Không thể tải thống kê tồn kho",
      500
    );
  }
}
