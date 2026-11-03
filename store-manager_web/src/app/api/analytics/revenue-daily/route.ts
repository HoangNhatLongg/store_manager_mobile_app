import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/auth";

export async function GET(request: NextRequest) {
  try {
    const session = await getServerSession(authOptions);
    if (!session) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const { searchParams } = new URL(request.url);
    const date = searchParams.get("date"); // format: YYYY-MM-DD

    if (!date) {
      return NextResponse.json({ error: "Date is required" }, { status: 400 });
    }

    const startOfDay = new Date(date);
    startOfDay.setHours(0, 0, 0, 0);

    const endOfDay = new Date(date);
    endOfDay.setHours(23, 59, 59, 999);

    // Get all sales for the day
    const salesInvoices = await prisma.saleInvoice.findMany({
      where: {
        status: "COMPLETED",
        createdAt: {
          gte: startOfDay,
          lte: endOfDay,
        },
      },
      include: {
        items: {
          include: {
            product: {
              select: {
                id: true,
                name: true,
                sku: true,
                category: {
                  select: {
                    id: true,
                    name: true,
                  },
                },
              },
            },
          },
        },
        customer: {
          select: {
            id: true,
            name: true,
            phone: true,
          },
        },
        createdBy: {
          select: {
            id: true,
            name: true,
          },
        },
      },
      orderBy: { createdAt: "desc" },
    });

    // Aggregate products sold
    const productSales: Record<string, {
      productId: string;
      productName: string;
      productSku: string;
      categoryName: string | null;
      quantity: number;
      unitPrice: number;
      totalPrice: number;
      importPrice: number;
      profit: number;
    }> = {};

    let totalRevenue = 0;
    let totalImportCost = 0;
    let totalProfit = 0;
    let totalQuantity = 0;

    for (const invoice of salesInvoices) {
      for (const item of invoice.items) {
        const productId = item.productId;
        const revenue = parseFloat(item.totalPrice.toString());
        const importCost = parseFloat(item.importPrice.toString()) * item.quantity;
        const profit = revenue - importCost;

        totalRevenue += revenue;
        totalImportCost += importCost;
        totalProfit += profit;
        totalQuantity += item.quantity;

        if (productSales[productId]) {
          productSales[productId].quantity += item.quantity;
          productSales[productId].totalPrice += revenue;
          productSales[productId].profit += profit;
        } else {
          productSales[productId] = {
            productId,
            productName: item.product?.name || "Unknown",
            productSku: item.product?.sku || "",
            categoryName: item.product?.category?.name || null,
            quantity: item.quantity,
            unitPrice: parseFloat(item.unitPrice.toString()),
            totalPrice: revenue,
            importPrice: parseFloat(item.importPrice.toString()),
            profit,
          };
        }
      }
    }

    // Convert to array and sort by quantity
    const productsSold = Object.values(productSales).sort((a, b) => b.quantity - a.quantity);

    return NextResponse.json({
      date,
      summary: {
        totalRevenue,
        totalImportCost,
        totalProfit,
        grossMargin: totalRevenue > 0 ? (totalProfit / totalRevenue) * 100 : 0,
        totalInvoices: salesInvoices.length,
        totalProductsSold: totalQuantity,
      },
      invoices: salesInvoices.map(inv => ({
        id: inv.id,
        invoiceNumber: inv.invoiceNumber,
        customer: inv.customer,
        subtotal: parseFloat(inv.subtotal.toString()),
        discount: parseFloat(inv.discount.toString()),
        totalAmount: parseFloat(inv.totalAmount.toString()),
        paymentMethod: inv.paymentMethod,
        createdAt: inv.createdAt,
        createdBy: inv.createdBy,
        items: inv.items.map(item => ({
          id: item.id,
          productName: item.product?.name || "Unknown",
          quantity: item.quantity,
          unitPrice: parseFloat(item.unitPrice.toString()),
          totalPrice: parseFloat(item.totalPrice.toString()),
        })),
      })),
      productsSold,
    });
  } catch (error) {
    console.error("Error getting daily revenue:", error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
}
