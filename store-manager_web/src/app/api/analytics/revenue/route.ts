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
    const startDate = searchParams.get("startDate");
    const endDate = searchParams.get("endDate");
    const period = searchParams.get("period") || "day"; // day, week, month

    // Build date filter - use end of day for endDate to include all sales on that day
    const dateFilter: any = {};
    if (startDate) {
      // Start of day in local timezone
      const [year, month, day] = startDate.split("-").map(Number);
      dateFilter.gte = new Date(year, month - 1, day, 0, 0, 0);
    }
    if (endDate) {
      // End of day in local timezone
      const [year, month, day] = endDate.split("-").map(Number);
      dateFilter.lte = new Date(year, month - 1, day, 23, 59, 59, 999);
    }

    const whereClause: any = { status: "COMPLETED" };
    if (Object.keys(dateFilter).length > 0) {
      whereClause.createdAt = dateFilter;
    }

    // Get sales data with import prices
    const salesInvoices = await prisma.saleInvoice.findMany({
      where: whereClause,
      include: {
        items: true,
      },
    });

    // Calculate totals
    let totalRevenue = 0; // Tổng tiền bán
    let totalImportCost = 0; // Tổng giá vốn
    let totalProfit = 0; // Lợi nhuận

    for (const invoice of salesInvoices) {
      for (const item of invoice.items) {
        const revenue = parseFloat(item.totalPrice.toString());
        const importCost = parseFloat(item.importPrice.toString()) * item.quantity;
        
        totalRevenue += revenue;
        totalImportCost += importCost;
        totalProfit += revenue - importCost;
      }
    }

    // Get additional stats
    const [
      totalSales,
      totalProductsSold,
      totalCustomers,
    ] = await Promise.all([
      salesInvoices.length,
      salesInvoices.reduce((sum, inv) => 
        sum + inv.items.reduce((s, item) => s + item.quantity, 0), 0
      ),
      prisma.saleInvoice.groupBy({
        by: ["customerId"],
        where: {
          ...whereClause,
          customerId: { not: null },
        },
      }).then(results => results.length),
    ]);

    // Get sales by day/week/month
    const filter = Object.keys(dateFilter).length > 0 ? dateFilter : undefined;
    const salesByPeriod = await getSalesByPeriod(period, filter);

    return NextResponse.json({
      summary: {
        totalRevenue,
        totalImportCost,
        totalProfit,
        grossMargin: totalRevenue > 0 ? (totalProfit / totalRevenue) * 100 : 0,
        totalSales,
        totalProductsSold,
        totalCustomers,
      },
      byPeriod: salesByPeriod,
    });
  } catch (error) {
    console.error("Error calculating revenue:", error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
}

async function getSalesByPeriod(period: string, filter?: { gte?: Date; lte?: Date }) {
  const now = new Date();
  
  // Use filter dates or default based on period
  let startDate: Date;
  let endDate: Date;
  
  switch (period) {
    case "month":
      // For month view, default to last 365 days if no filter
      startDate = filter?.gte || new Date(now.getTime() - 365 * 24 * 60 * 60 * 1000);
      endDate = filter?.lte || now;
      break;
    case "week":
      // For week view, default to last 30 days
      startDate = filter?.gte || new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000);
      endDate = filter?.lte || now;
      break;
    default: // day
      startDate = filter?.gte || new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000);
      endDate = filter?.lte || now;
  }

  const sales = await prisma.saleInvoice.findMany({
    where: {
      status: "COMPLETED",
      createdAt: {
        gte: startDate,
        lte: endDate,
      },
    },
    include: {
      items: true,
    },
    orderBy: { createdAt: "asc" },
  });

  // Group by period
  const grouped: Record<string, { revenue: number; importCost: number; profit: number; count: number }> = {};

  for (const sale of sales) {
    let key: string;
    const date = sale.createdAt;

    if (period === "week") {
      key = date.toLocaleDateString("vi-VN", { weekday: "long" });
    } else if (period === "month") {
      key = date.toLocaleDateString("vi-VN", { day: "2-digit", month: "2-digit" });
    } else {
      key = date.toLocaleTimeString("vi-VN", { hour: "2-digit", minute: "2-digit" });
    }

    if (!grouped[key]) {
      grouped[key] = { revenue: 0, importCost: 0, profit: 0, count: 0 };
    }

    for (const item of sale.items) {
      const revenue = parseFloat(item.totalPrice.toString());
      const importCost = parseFloat(item.importPrice.toString()) * item.quantity;
      
      grouped[key].revenue += revenue;
      grouped[key].importCost += importCost;
      grouped[key].profit += revenue - importCost;
    }
    grouped[key].count += 1;
  }

  return Object.entries(grouped).map(([label, data]) => ({
    label,
    ...data,
  }));
}
