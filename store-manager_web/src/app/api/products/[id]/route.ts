import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/auth";
import {
  LoiDonViSanPham,
  capNhatDonViChoSanPham,
  chuyenThamSoDonVi,
} from "@/lib/don-vi-san-pham";

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const session = await getServerSession(authOptions);
    if (!session) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const { id } = await params;
    const product = await prisma.product.findUnique({
      where: { id },
      include: {
        category: true,
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

    if (!product) {
      return NextResponse.json({ error: "Product not found" }, { status: 404 });
    }

    return NextResponse.json(product);
  } catch (error) {
    console.error("Error fetching product:", error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
}

export async function PUT(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const session = await getServerSession(authOptions);
    if (!session) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const { id } = await params;
    const body = await request.json();
    const { name, sku, categoryId, importPrice, sellPrice, stock, minStock, description, image, imageUrl } = body;

    const existingProduct = await prisma.product.findUnique({
      where: { id },
    });

    if (!existingProduct) {
      return NextResponse.json({ error: "Product not found" }, { status: 404 });
    }

    const duplicateCheck = await prisma.product.findFirst({
      where: {
        OR: [{ sku }, { name }],
        NOT: { id },
      },
    });

    if (duplicateCheck) {
      return NextResponse.json(
        { error: "SKU hoặc tên sản phẩm đã tồn tại" },
        { status: 400 }
      );
    }

    const slug = name
      ? name.toLowerCase()
          .normalize("NFD")
          .replace(/[\u0300-\u036f]/g, "")
          .replace(/[^a-z0-9]+/g, "-")
          .replace(/^-+|-+$/g, "")
      : existingProduct.slug;

    const product = await prisma.$transaction(async (tx) => {
      const sanPham = await tx.product.update({
        where: { id },
        data: {
          name,
          slug,
          sku,
          categoryId,
          importPrice: importPrice !== undefined ? parseFloat(importPrice) : undefined,
          sellPrice: sellPrice !== undefined ? parseFloat(sellPrice) : undefined,
          stock: stock !== undefined ? parseInt(stock) : undefined,
          minStock: minStock !== undefined ? parseInt(minStock) : undefined,
          description,
          imageUrl: imageUrl || image || null,
        },
      });

      // Neu client gui don vi thi cap nhat don vi ban le mac dinh.
      if (body.defaultUnit !== undefined || body.purchaseUnit !== undefined) {
        const thamSoDonVi = chuyenThamSoDonVi(
          body.defaultUnit,
          body.purchaseUnit,
          sellPrice !== undefined ? parseFloat(sellPrice) : Number(existingProduct.sellPrice)
        );
        await capNhatDonViChoSanPham(tx, id, thamSoDonVi);
      }

      return tx.product.findUniqueOrThrow({
        where: { id },
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

    return NextResponse.json(product);
  } catch (error) {
    if (error instanceof LoiDonViSanPham) {
      return NextResponse.json(
        { error: error.message },
        { status: error.trangThai }
      );
    }

    console.error("Error updating product:", error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
}

export async function DELETE(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const session = await getServerSession(authOptions);
    if (!session) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const { id } = await params;

    const existingProduct = await prisma.product.findUnique({
      where: { id },
      include: { importItems: true, saleItems: true },
    });

    if (!existingProduct) {
      return NextResponse.json({ error: "Product not found" }, { status: 404 });
    }

    // Không xóa dữ liệu đã xuất hiện trong bất kỳ hóa đơn nào.
    if (existingProduct.importItems.length > 0 || existingProduct.saleItems.length > 0) {
      return NextResponse.json(
        { error: "Sản phẩm đã có trong hóa đơn, không thể xóa. Bạn có thể ẩn sản phẩm thay thế." },
        { status: 409 }
      );
    }

    await prisma.product.delete({
      where: { id },
    });

    return NextResponse.json({ message: "Product deleted successfully" });
  } catch (error) {
    console.error("Error deleting product:", error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
}

export async function PATCH(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const session = await getServerSession(authOptions);
    if (!session) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const { id } = await params;
    const body = await request.json();
    if (typeof body.isActive !== "boolean") {
      return NextResponse.json({ error: "isActive phải là boolean" }, { status: 400 });
    }

    const product = await prisma.product.update({
      where: { id },
      data: { isActive: body.isActive },
    });

    return NextResponse.json({ id: product.id, isActive: product.isActive });
  } catch (error) {
    console.error("Error changing product visibility:", error);
    return NextResponse.json(
      { error: "Không thể thay đổi trạng thái sản phẩm" },
      { status: 500 }
    );
  }
}
