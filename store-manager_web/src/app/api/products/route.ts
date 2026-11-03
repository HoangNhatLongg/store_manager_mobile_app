import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/auth";
import {
  LoiDonViSanPham,
  chuyenThamSoDonVi,
  ganDonViChoSanPham,
} from "@/lib/don-vi-san-pham";

export async function GET(request: NextRequest) {
  try {
    const session = await getServerSession(authOptions);
    if (!session) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const { searchParams } = new URL(request.url);
    const page = Math.max(1, parseInt(searchParams.get("page") || "1"));
    const limit = Math.min(50, Math.max(1, parseInt(searchParams.get("limit") || "15")));
    const search = searchParams.get("search") || "";
    const categoryId = searchParams.get("categoryId");
    const includeHidden = searchParams.get("includeHidden") === "true";
    const sortBy = searchParams.get("sortBy") || "createdAt";
    const sortOrder = searchParams.get("sortOrder") || "desc";

    const where: any = {};

    if (!includeHidden) {
      where.isActive = true;
    }
    
    if (search) {
      where.OR = [
        { name: { contains: search, mode: "insensitive" } },
        { sku: { contains: search, mode: "insensitive" } },
      ];
    }
    
    if (categoryId) {
      where.categoryId = categoryId;
    }

    const [products, total] = await Promise.all([
      prisma.product.findMany({
        where,
        include: {
          category: {
            select: { id: true, name: true, slug: true },
          },
          units: {
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
            orderBy: [{ isPurchase: "desc" }, { isDefault: "desc" }, { conversionQty: "asc" }],
          },
        },
        orderBy: { [sortBy]: sortOrder },
        skip: (page - 1) * limit,
        take: limit,
      }),
      prisma.product.count({ where }),
    ]);

    return NextResponse.json({
      products,
      pagination: {
        page,
        limit,
        total,
        totalPages: Math.ceil(total / limit),
      },
    });
  } catch (error) {
    console.error("Error fetching products:", error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
}

export async function POST(request: NextRequest) {
  try {
    const session = await getServerSession(authOptions);
    if (!session) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const body = await request.json();
    const { name, sku, categoryId, importPrice, sellPrice, stock, minStock, description, image, imageUrl } = body;

    if (!name || !sku || !categoryId || importPrice === undefined || sellPrice === undefined) {
      return NextResponse.json(
        { error: "Missing required fields" },
        { status: 400 }
      );
    }

    // Sản phẩm bắt buộc phải có đơn vị bán lẻ mặc định để tính tồn kho.
    const thamSoDonVi = chuyenThamSoDonVi(
      body.defaultUnit,
      body.purchaseUnit,
      parseFloat(sellPrice)
    );

    const existingProduct = await prisma.product.findFirst({
      where: { OR: [{ sku }, { name }] },
    });

    if (existingProduct) {
      return NextResponse.json(
        { error: "Sản phẩm đã tồn tại với SKU hoặc tên này" },
        { status: 400 }
      );
    }
    const slug = name.toLowerCase()
      .normalize("NFD")
      .replace(/[\u0300-\u036f]/g, "")
      .replace(/[^a-z0-9]+/g, "-")
      .replace(/^-+|-+$/g, "");

    const product = await prisma.$transaction(async (tx) => {
      const sanPhamMoi = await tx.product.create({
        data: {
          name,
          slug,
          sku,
          categoryId,
          importPrice: parseFloat(importPrice),
          sellPrice: parseFloat(sellPrice),
          stock: parseInt(stock) || 0,
          minStock: parseInt(minStock) || 10,
          description,
          imageUrl: imageUrl || image || null,
        },
      });

      await ganDonViChoSanPham(tx, sanPhamMoi.id, thamSoDonVi);

      return tx.product.findUniqueOrThrow({
        where: { id: sanPhamMoi.id },
        include: {
          category: {
            select: { id: true, name: true, slug: true },
          },
          units: {
            include: { unit: true },
            orderBy: [
              { isPurchase: "desc" },
              { isDefault: "desc" },
              { conversionQty: "asc" },
            ],
          },
        },
      });
    });

    return NextResponse.json(product, { status: 201 });
  } catch (error) {
    if (error instanceof LoiDonViSanPham) {
      return NextResponse.json(
        { error: error.message },
        { status: error.trangThai }
      );
    }

    console.error("Error creating product:", error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
}
