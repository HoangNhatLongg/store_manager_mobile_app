import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/auth";
import {
  LoiNhapHang,
  taoPhieuNhapVaTangTon,
  type DongHangNhapInput,
} from "@/lib/nhap-hang";

export async function GET(request: NextRequest) {
  try {
    const session = await getServerSession(authOptions);
    if (!session) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const { searchParams } = new URL(request.url);
    const page = parseInt(searchParams.get("page") || "1");
    const limit = parseInt(searchParams.get("limit") || "20");
    const search = searchParams.get("search") || "";
    const supplierId = searchParams.get("supplierId");
    const status = searchParams.get("status");
    const startDate = searchParams.get("startDate");
    const endDate = searchParams.get("endDate");

    const where: any = {};

    if (search) {
      where.OR = [
        { invoiceNumber: { contains: search, mode: "insensitive" } },
        { supplier: { name: { contains: search, mode: "insensitive" } } },
      ];
    }

    if (supplierId) {
      where.supplierId = supplierId;
    }

    if (status) {
      where.status = status;
    }

    if (startDate || endDate) {
      where.createdAt = {};
      if (startDate) where.createdAt.gte = new Date(startDate);
      if (endDate) where.createdAt.lte = new Date(endDate);
    }

    const [invoices, total] = await Promise.all([
      prisma.importInvoice.findMany({
        where,
        include: {
          supplier: {
            select: { id: true, name: true },
          },
          createdBy: {
            select: { id: true, name: true, email: true },
          },
          items: {
            include: {
              product: {
                select: { id: true, name: true, sku: true },
              },
            },
          },
        },
        orderBy: { createdAt: "desc" },
        skip: (page - 1) * limit,
        take: limit,
      }),
      prisma.importInvoice.count({ where }),
    ]);

    return NextResponse.json({
      invoices,
      pagination: {
        page,
        limit,
        total,
        totalPages: Math.ceil(total / limit),
      },
    });
  } catch (error) {
    console.error("Error fetching import invoices:", error);
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
    const { supplierId, notes, items, status = "COMPLETED" } = body;

    if (!items || !Array.isArray(items) || items.length === 0) {
      return NextResponse.json(
        { error: "Danh sách sản phẩm không được trống" },
        { status: 400 }
      );
    }

    // Validate items - chỉ yêu cầu productName, quantity, unitPrice
    for (const item of items) {
      if (!item.productName || item.quantity <= 0 || item.unitPrice <= 0) {
        return NextResponse.json(
          { error: "Thông tin sản phẩm không hợp lệ" },
          { status: 400 }
        );
      }
    }

    // Tạo phiếu nhập và cập nhật tồn kho trong cùng một transaction.
    // Số lượng nhập tính theo đơn vị gốc, tồn kho tăng theo đơn vị bán lẻ cơ bản.
    const result = await prisma.$transaction(async (tx) => {
      const { phieu } = await taoPhieuNhapVaTangTon(tx, {
        supplierId: supplierId || null,
        notes: notes || null,
        status: status === "PENDING" ? "PENDING" : "COMPLETED",
        createdById: (session.user as any).id,
        items: items as DongHangNhapInput[],
      });

      return tx.importInvoice.findUniqueOrThrow({
        where: { id: phieu.id },
        include: {
          supplier: true,
          items: {
            include: {
              product: true,
              unit: true,
            },
          },
        },
      });
    });

    return NextResponse.json(result, { status: 201 });
  } catch (error) {
    if (error instanceof LoiNhapHang) {
      return NextResponse.json(
        { error: error.message },
        { status: error.trangThai }
      );
    }

    console.error("Error creating import invoice:", error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
}
