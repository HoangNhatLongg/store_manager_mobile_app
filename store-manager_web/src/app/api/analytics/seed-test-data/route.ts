import { NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/auth";
import type { PaymentMethod } from "@prisma/client";

export async function POST() {
  try {
    const session = await getServerSession(authOptions);
    if (!session) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    // Get all products with stock > 0
    const products = await prisma.product.findMany({
      where: { stock: { gt: 0 } },
      take: 10,
    });

    if (products.length === 0) {
      return NextResponse.json({
        error: "Không có sản phẩm trong kho. Hãy nhập hàng trước.",
      }, { status: 400 });
    }

    // Get user
    const user = await prisma.user.findFirst();
    if (!user) {
      return NextResponse.json({
        error: "Không tìm thấy user",
      }, { status: 400 });
    }

    const results = [];

    // Create sales for last 7 days
    for (let dayOffset = 0; dayOffset < 7; dayOffset++) {
      const date = new Date();
      date.setDate(date.getDate() - dayOffset);

      // Random number of orders per day (3-8)
      const ordersPerDay = Math.floor(Math.random() * 6) + 3;

      for (let i = 0; i < ordersPerDay; i++) {
        // Random time during the day
        const orderDate = new Date(date);
        orderDate.setHours(Math.floor(Math.random() * 12) + 8); // 8am - 8pm
        orderDate.setMinutes(Math.floor(Math.random() * 60));
        orderDate.setSeconds(0);

        // Random products for this order (1-3 items)
        const numItems = Math.floor(Math.random() * 3) + 1;
        const selectedProducts = [];
        const usedIndices = new Set();

        for (let j = 0; j < numItems; j++) {
          let idx;
          do {
            idx = Math.floor(Math.random() * products.length);
          } while (usedIndices.has(idx));
          usedIndices.add(idx);
          selectedProducts.push(products[idx]);
        }

        // Calculate totals
        const items = selectedProducts.map(p => {
          const quantity = Math.floor(Math.random() * 3) + 1;
          const unitPrice = parseFloat(p.sellPrice.toString());
          const importPrice = parseFloat(p.importPrice.toString());
          return {
            productId: p.id,
            quantity,
            unitPrice,
            importPrice,
            totalPrice: unitPrice * quantity,
          };
        });

        const subtotal = items.reduce((sum, item) => sum + item.totalPrice, 0);
        const discount = Math.random() > 0.7 ? Math.floor(subtotal * 0.1) : 0;
        const totalAmount = subtotal - discount;
        const paymentMethods: PaymentMethod[] = ["CASH", "BANK_TRANSFER", "CARD", "E_WALLET"];
        const paymentMethod = paymentMethods[Math.floor(Math.random() * paymentMethods.length)];

        // Create sale invoice
        const invoice = await prisma.saleInvoice.create({
          data: {
            invoiceNumber: `HD${Date.now()}${dayOffset}${i}`,
            customerId: null,
            subtotal,
            discount,
            totalAmount,
            paymentMethod,
            status: "COMPLETED",
            createdAt: orderDate,
            createdById: user.id,
            items: {
              create: items.map(item => ({
                productId: item.productId,
                quantity: item.quantity,
                unitPrice: item.unitPrice,
                importPrice: item.importPrice,
                totalPrice: item.totalPrice,
              })),
            },
          },
          include: { items: true },
        });

        // Update product stock
        for (const item of items) {
          await prisma.product.update({
            where: { id: item.productId },
            data: { stock: { decrement: item.quantity } },
          });
        }

        results.push({
          invoiceNumber: invoice.invoiceNumber,
          totalAmount: invoice.totalAmount,
          date: orderDate,
        });
      }
    }

    return NextResponse.json({
      success: true,
      message: `Đã tạo ${results.length} đơn bán hàng mẫu`,
      invoices: results,
    });
  } catch (error) {
    console.error("Seed error:", error);
    return NextResponse.json(
      { error: "Lỗi khi tạo dữ liệu: " + (error instanceof Error ? error.message : "Unknown") },
      { status: 500 }
    );
  }
}
