import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { getMobileUser, mobileUnauthorizedResponse } from "@/lib/mobile-auth";
import { decimalToString, mobileErrorResponse } from "@/lib/mobile-http";
import {
  LoiDonViSanPham,
  capNhatDonViChoSanPham,
  chuyenThamSoDonVi,
} from "@/lib/don-vi-san-pham";

type ThamSo = { params: Promise<{ id: string }> };

function taoSlug(ten: string): string {
  return ten.toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");
}

/** PUT /api/v1/products/:id - Cap nhat san pham tu mobile. */
export async function PUT(request: NextRequest, { params }: ThamSo) {
  if (!(await getMobileUser(request))) return mobileUnauthorizedResponse();

  try {
    const { id } = await params;
    const body = await request.json();
    const product = await prisma.product.findUnique({ where: { id } });
    if (!product) return mobileErrorResponse("NOT_FOUND", "Không tìm thấy sản phẩm", 404);

    const name = String(body.name ?? product.name).trim();
    const sku = String(body.sku ?? product.sku).trim();
    const duplicate = await prisma.product.findFirst({
      where: { OR: [{ name }, { sku }], NOT: { id } },
    });
    if (duplicate) return mobileErrorResponse("DUPLICATE", "Tên hoặc SKU đã tồn tại", 400);

    // Neu client gui don vi thi cap nhat luon don vi ban le mac dinh.
    const coDonVi =
      body.defaultUnit !== undefined || body.purchaseUnit !== undefined;

    let thamSoDonVi = null;
    if (coDonVi) {
      try {
        thamSoDonVi = chuyenThamSoDonVi(
          body.defaultUnit,
          body.purchaseUnit,
          Number(body.sellPrice ?? product.sellPrice)
        );
      } catch (error) {
        if (error instanceof LoiDonViSanPham) {
          return mobileErrorResponse(error.maLoi, error.message, error.trangThai);
        }
        throw error;
      }
    }

    const updated = await prisma.$transaction(async (tx) => {
      const sanPham = await tx.product.update({
        where: { id },
        data: {
          name,
          slug: taoSlug(name),
          sku,
          barcode: body.barcode === undefined ? undefined : body.barcode || null,
          categoryId: body.categoryId === undefined ? undefined : body.categoryId || null,
          importPrice: body.importPrice === undefined ? undefined : Number(body.importPrice),
          sellPrice: body.sellPrice === undefined ? undefined : Number(body.sellPrice),
          stock: body.stock === undefined ? undefined : Number(body.stock),
          minStock: body.minStock === undefined ? undefined : Number(body.minStock),
          description: body.description === undefined ? undefined : body.description || null,
          imageUrl: body.imageUrl === undefined ? undefined : body.imageUrl || null,
        },
      });

      if (thamSoDonVi) {
        await capNhatDonViChoSanPham(tx, id, thamSoDonVi);
      }

      return tx.product.findUniqueOrThrow({
        where: { id },
        include: {
          category: true,
          units: { include: { unit: true }, orderBy: [{ isPurchase: "desc" }, { isDefault: "desc" }, { conversionQty: "asc" }] },
        },
      });
    });

    return NextResponse.json({ data: chuyenSanPham(updated) });
  } catch (error) {
    if (error instanceof LoiDonViSanPham) {
      return mobileErrorResponse(error.maLoi, error.message, error.trangThai);
    }

    console.error("Mobile update product error:", error);
    return mobileErrorResponse("SERVER_ERROR", "Không thể cập nhật sản phẩm", 500);
  }
}

/** DELETE /api/v1/products/:id - Xoa mem san pham de khong mat lich su hoa don. */
export async function DELETE(request: NextRequest, { params }: ThamSo) {
  if (!(await getMobileUser(request))) return mobileUnauthorizedResponse();

  try {
    const { id } = await params;
    const product = await prisma.product.findUnique({ where: { id } });
    if (!product) return mobileErrorResponse("NOT_FOUND", "Không tìm thấy sản phẩm", 404);

    const [soPhieuNhap, soPhieuBan] = await Promise.all([
      prisma.importInvoiceItem.count({ where: { productId: id } }),
      prisma.saleInvoiceItem.count({ where: { productId: id } }),
    ]);
    if (soPhieuNhap > 0 || soPhieuBan > 0) {
      return mobileErrorResponse(
        "PRODUCT_HAS_INVOICE_HISTORY",
        "Không thể xóa sản phẩm đã tồn tại trong hóa đơn. Hãy ẩn sản phẩm thay thế.",
        409
      );
    }

    await prisma.product.delete({ where: { id } });
    return NextResponse.json({ data: { id } });
  } catch (error) {
    console.error("Mobile delete product error:", error);
    return mobileErrorResponse("SERVER_ERROR", "Không thể xóa sản phẩm", 500);
  }
}

/** PATCH /api/v1/products/:id/visibility - An/hien san pham tren POS. */
export async function PATCH(request: NextRequest, { params }: ThamSo) {
  if (!(await getMobileUser(request))) return mobileUnauthorizedResponse();

  try {
    const { id } = await params;
    const body = await request.json();
    const product = await prisma.product.update({
      where: { id },
      data: { isActive: Boolean(body.isActive) },
    });
    return NextResponse.json({ data: { id: product.id, isActive: product.isActive } });
  } catch (error) {
    console.error("Mobile change product visibility error:", error);
    return mobileErrorResponse("SERVER_ERROR", "Không thể thay đổi trạng thái sản phẩm", 500);
  }
}

function chuyenSanPham(product: any) {
  return {
    ...product,
    importPrice: decimalToString(product.importPrice),
    sellPrice: decimalToString(product.sellPrice),
    units: product.units?.map((item: any) => ({
      ...item,
      price: decimalToString(item.price),
    })) || [],
  };
}
