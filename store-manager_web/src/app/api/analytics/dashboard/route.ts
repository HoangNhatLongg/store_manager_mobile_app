import { NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/auth";

export async function GET() {
  try {
    const session = await getServerSession(authOptions);
    if (!session) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    // Get total products
    const totalProducts = await prisma.product.count({
      where: { isActive: true },
    });

    // Get low stock products
    const allProducts = await prisma.product.findMany({
      where: { isActive: true },
      select: {
        id: true,
        stock: true,
        minStock: true,
      },
    });

    const lowStockProducts = allProducts.filter(
      (p) => p.stock <= p.minStock
    ).length;

    // Get import invoices count and total amount
    const importInvoices = await prisma.importInvoice.aggregate({
      _count: true,
      _sum: {
        totalAmount: true,
      },
    });

    // Get sale invoices count and total amount
    const saleInvoices = await prisma.saleInvoice.aggregate({
      _count: true,
      _sum: {
        totalAmount: true,
      },
    });

    // Calculate profit (total sales - total import cost)
    // For accurate profit, we need to sum importPrice from sale items
    const saleItems = await prisma.saleInvoiceItem.aggregate({
      _sum: {
        importPrice: true,
      },
    });

    const totalImportAmount = importInvoices._sum.totalAmount || 0;
    const totalSaleAmount = saleInvoices._sum.totalAmount || 0;
    const totalImportCost = saleItems._sum.importPrice || 0;
    const totalProfit = Number(totalSaleAmount) - Number(totalImportCost);

    return NextResponse.json({
      totalProducts,
      lowStockProducts,
      totalImportInvoices: importInvoices._count,
      totalSaleInvoices: saleInvoices._count,
      totalImportAmount: Number(totalImportAmount),
      totalSaleAmount: Number(totalSaleAmount),
      totalProfit,
    });
  } catch (error) {
    console.error("Error fetching dashboard stats:", error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
}
