import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/auth";

function generateInvoiceNumber() {
  const date = new Date();
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  const random = Math.floor(Math.random() * 10000).toString().padStart(4, "0");
  return `BH-${year}${month}${day}-${random}`;
}

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
    const customerId = searchParams.get("customerId");
    const status = searchParams.get("status");
    const startDate = searchParams.get("startDate");
    const endDate = searchParams.get("endDate");

    const where: any = {};

    if (search) {
      where.OR = [
        { invoiceNumber: { contains: search, mode: "insensitive" } },
        { customer: { name: { contains: search, mode: "insensitive" } } },
      ];
    }

    if (customerId) {
      where.customerId = customerId;
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
      prisma.saleInvoice.findMany({
        where,
        include: {
          customer: {
            select: { id: true, name: true, phone: true },
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
      prisma.saleInvoice.count({ where }),
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
    console.error("Error fetching sale invoices:", error);
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
    const {
      customerId,
      items,
      paymentMethod = "CASH",
      notes,
      discount = 0,
    } = body;

    if (!items || !Array.isArray(items) || items.length === 0) {
      return NextResponse.json(
        { error: "Danh sách sản phẩm không được trống" },
        { status: 400 }
      );
    }

    // Validate items
    for (const item of items) {
      if (!item.productId || item.quantity <= 0 || item.unitPrice <= 0) {
        return NextResponse.json(
          { error: "Thông tin sản phẩm không hợp lệ" },
          { status: 400 }
        );
      }
    }

    const invoiceNumber = generateInvoiceNumber();

    // Create invoice with items and update product stock in a transaction
    const result = await prisma.$transaction(async (tx) => {
      let subtotal = 0;
      const preparedItems = [];

      for (const item of items) {
        const itemTotal = item.quantity * item.unitPrice;
        subtotal += itemTotal;

        // Lấy giá nhập hiện tại của sản phẩm để lưu vào SaleInvoiceItem
        const product = await tx.product.findUnique({
          where: { id: item.productId },
        });

        if (!product) {
          throw new Error(`Không tìm thấy sản phẩm: ${item.productId}`);
        }

        // Tồn kho trong database luôn tính theo đơn vị cơ bản. Khi bán theo
        // đơn vị quy đổi (thùng, chai...) số lượng phải được quy đổi về
        // đơn vị cơ bản trước khi so sánh và trừ kho.
        const conversionQty = item.conversionQty > 0 ? item.conversionQty : 1;
        const quantityBase = item.quantity * conversionQty;

        // Kiểm tra tồn kho
        if (product.stock < quantityBase) {
          throw new Error(
            `Sản phẩm "${product.name}" không đủ tồn kho. Còn ${product.stock} sản phẩm.`
          );
        }

        // Đơn giá lưu theo đơn vị cơ bản để lợi nhuận tính đúng.
        const unitPriceBase =
          quantityBase > 0 ? itemTotal / quantityBase : item.unitPrice;

        preparedItems.push({
          productId: item.productId,
          quantity: quantityBase,
          unitPrice: unitPriceBase,
          importPrice: product.importPrice, // Lưu giá nhập tại thời điểm bán
          totalPrice: itemTotal,
        });

        // Cập nhật tồn kho. Dùng updateMany kèm điều kiện tồn kho còn đủ
        // để tránh trừ kho xuống âm khi hai phiên bán cùng lúc.
        const stockUpdate = await tx.product.updateMany({
          where: {
            id: item.productId,
            isActive: true,
            stock: { gte: quantityBase },
          },
          data: {
            stock: { decrement: quantityBase },
          },
        });

        if (stockUpdate.count !== 1) {
          throw new Error(
            `Sản phẩm "${product.name}" vừa thay đổi tồn kho, vui lòng thử lại.`
          );
        }
      }

      if (discount > subtotal) {
        throw new Error("Giảm giá không được lớn hơn tạm tính");
      }

      const totalAmount = subtotal - discount;

      const invoice = await tx.saleInvoice.create({
        data: {
          invoiceNumber,
          customerId: customerId || null,
          subtotal,
          discount,
          totalAmount,
          paymentMethod,
          notes,
          status: "COMPLETED",
          createdById: session.user.id,
          items: {
            create: preparedItems,
          },
        },
        include: {
          items: {
            include: {
              product: {
                select: { id: true, name: true, sku: true },
              },
            },
          },
          customer: {
            select: { id: true, name: true, phone: true },
          },
        },
      });

      return invoice;
    });

    return NextResponse.json(result, { status: 201 });
  } catch (error: any) {
    console.error("Error creating sale invoice:", error);
    return NextResponse.json(
      { error: error.message || "Internal server error" },
      { status: 500 }
    );
  }
}
