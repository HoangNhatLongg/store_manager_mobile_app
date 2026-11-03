import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { getMobileUser, mobileUnauthorizedResponse } from "@/lib/mobile-auth";
import { mobileErrorResponse, decimalToString } from "@/lib/mobile-http";
import {
  LoiDonViSanPham,
  chuyenThamSoDonVi,
  ganDonViChoSanPham,
} from "@/lib/don-vi-san-pham";

/**
 * GET /api/v1/products
 *
 * Lấy danh sách sản phẩm hoạt động cho mobile.
 * Hỗ trợ phân trang, tìm kiếm và đồng bộ gia tăng qua updatedAfter.
 */
export async function GET(request: NextRequest) {
  const user = await getMobileUser(request);

  if (!user) {
    return mobileUnauthorizedResponse();
  }

  try {
    const { searchParams } = new URL(request.url);
    const page = Math.max(1, Number(searchParams.get("page") || "1"));
    const limit = Math.min(
      50,
      Math.max(1, Number(searchParams.get("limit") || "15"))
    );
    const search = searchParams.get("search")?.trim() || "";
    const categoryId = searchParams.get("categoryId") || undefined;
    const updatedAfter = searchParams.get("updatedAfter");

    const parsedUpdatedAfter = updatedAfter
      ? new Date(updatedAfter)
      : undefined;

    if (parsedUpdatedAfter && Number.isNaN(parsedUpdatedAfter.getTime())) {
      return mobileErrorResponse(
        "VALIDATION_ERROR",
        "updatedAfter không phải thời gian ISO hợp lệ",
        400
      );
    }

    const where = {
      ...(searchParams.get("includeHidden") === "true" ? {} : { isActive: true }),
      ...(categoryId ? { categoryId } : {}),
      ...(parsedUpdatedAfter
        ? { updatedAt: { gt: parsedUpdatedAfter } }
        : {}),
      ...(search
        ? {
            OR: [
              { name: { contains: search, mode: "insensitive" as const } },
              { sku: { contains: search, mode: "insensitive" as const } },
              {
                barcode: {
                  contains: search,
                  mode: "insensitive" as const,
                },
              },
            ],
          }
        : {}),
    };

    const [products, total] = await Promise.all([
      prisma.product.findMany({
        where,
        orderBy: [{ updatedAt: "asc" }, { id: "asc" }],
        skip: (page - 1) * limit,
        take: limit,
        include: {
          category: {
            select: {
              id: true,
              name: true,
              slug: true,
            },
          },
          units: {
            orderBy: [{ isPurchase: "desc" }, { isDefault: "desc" }, { conversionQty: "asc" }],
            include: {
              unit: {
                select: {
                  id: true,
                  name: true,
                  abbreviation: true,
                  isBaseUnit: true,
                },
              },
            },
          },
        },
      }),
      prisma.product.count({ where }),
    ]);

    return NextResponse.json({
      data: products.map((product) => ({
        ...product,
        importPrice: decimalToString(product.importPrice),
        previousImportPrice: decimalToString(product.previousImportPrice),
        sellPrice: decimalToString(product.sellPrice),
        previousSellPrice: decimalToString(product.previousSellPrice),
        units: product.units.map((productUnit) => ({
          ...productUnit,
          price: decimalToString(productUnit.price),
        })),
      })),
      pagination: {
        page,
        limit,
        total,
        totalPages: Math.ceil(total / limit),
      },
      serverTime: new Date().toISOString(),
    });
  } catch (error) {
    console.error("Mobile products error:", error);

    return mobileErrorResponse(
      "SERVER_ERROR",
      "Không thể tải danh sách sản phẩm",
      500
    );
  }
}

/** POST /api/v1/products - Tao san pham tu mobile. */
export async function POST(request: NextRequest) {
  const user = await getMobileUser(request);
  if (!user) return mobileUnauthorizedResponse();

  try {
    const body = await request.json();
    const name = String(body.name || "").trim();
    const sku = String(body.sku || "").trim();
    if (!name || !sku || body.importPrice === undefined || body.sellPrice === undefined) {
      return mobileErrorResponse("VALIDATION_ERROR", "Thiếu tên, SKU hoặc giá sản phẩm", 400);
    }
    const existing = await prisma.product.findFirst({ where: { OR: [{ name }, { sku }] } });
    if (existing) return mobileErrorResponse("DUPLICATE", "Tên hoặc SKU đã tồn tại", 400);
    const slug = name.toLowerCase().normalize("NFD").replace(/[\u0300-\u036f]/g, "").replace(/[^a-z0-9]+/g, "-").replace(/^-+|-+$/g, "");

    // San pham phai co don vi ban le mac dinh de tinh ton kho.
    let thamSoDonVi;
    try {
      thamSoDonVi = chuyenThamSoDonVi(
        body.defaultUnit,
        body.purchaseUnit,
        Number(body.sellPrice)
      );
    } catch (error) {
      if (error instanceof LoiDonViSanPham) {
        return mobileErrorResponse(error.maLoi, error.message, error.trangThai);
      }
      throw error;
    }

    const product = await prisma.$transaction(async (tx) => {
      const sanPhamMoi = await tx.product.create({
        data: {
          name, sku, slug,
          categoryId: body.categoryId || null,
          // Barcode phai duoc luu ngay khi tao. API tim kiem cung chap nhan barcode
          // nen neu bo qua o day thi san pham vua tao se khong tim duoc bang ma vach.
          barcode: body.barcode ? String(body.barcode).trim() : null,
          importPrice: Number(body.importPrice), sellPrice: Number(body.sellPrice),
          stock: Number(body.stock || 0), minStock: Number(body.minStock || 10),
          description: body.description || null,
          imageUrl: body.imageUrl ? String(body.imageUrl).trim() : null,
        },
      });

      await ganDonViChoSanPham(tx, sanPhamMoi.id, thamSoDonVi);

      return tx.product.findUniqueOrThrow({
        where: { id: sanPhamMoi.id },
        include: { category: true, units: { include: { unit: true } } },
      });
    });

    return NextResponse.json({ data: chuyenSanPham(product) }, { status: 201 });
  } catch (error) {
    if (error instanceof LoiDonViSanPham) {
      return mobileErrorResponse(error.maLoi, error.message, error.trangThai);
    }

    console.error("Mobile create product error:", error);
    return mobileErrorResponse("SERVER_ERROR", "Không thể tạo sản phẩm", 500);
  }
}

/**
 * Chuyen san pham Prisma ve dang JSON phu hop cho mobile.
 *
 * Gia tien duoc doi thanh chuoi de tranh loi lam tron so thuc khi Android
 * xu ly tien te. Don vi ban cung duoc doi gia theo cung quy tac.
 */
function chuyenSanPham(product: any) {
  return {
    ...product,
    importPrice: decimalToString(product.importPrice),
    previousImportPrice: decimalToString(product.previousImportPrice),
    sellPrice: decimalToString(product.sellPrice),
    previousSellPrice: decimalToString(product.previousSellPrice),
    units: product.units?.map((item: any) => ({
      ...item,
      price: decimalToString(item.price),
    })) || [],
  };
}
