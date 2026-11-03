import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { getMobileUser, mobileUnauthorizedResponse } from "@/lib/mobile-auth";
import { decimalToString, mobileErrorResponse } from "@/lib/mobile-http";

/**
 * GET /api/v1/dashboard
 *
 * Mobile dùng một endpoint duy nhất cho màn hình tổng quan. Cách này giảm số
 * lần Android phải gọi mạng và giúp dashboard có thể lưu một snapshot local
 * trong SQLite ở bước phát triển tiếp theo.
 */
export async function GET(request: NextRequest) {
  const user = await getMobileUser(request);

  if (!user) {
    return mobileUnauthorizedResponse();
  }

  try {
    // Tải các nhóm dữ liệu độc lập song song để giảm thời gian phản hồi.
    const [
      totalProducts,
      allActiveProducts,
      importSummary,
      saleSummary,
      saleItems,
      recentImports,
      recentSales,
      topProductGroups,
    ] = await Promise.all([
      prisma.product.count({
        where: { isActive: true },
      }),
      prisma.product.findMany({
        where: { isActive: true },
        select: {
          id: true,
          name: true,
          sku: true,
          stock: true,
          minStock: true,
          sellPrice: true,
        },
        orderBy: [{ stock: "asc" }, { name: "asc" }],
      }),
      prisma.importInvoice.aggregate({
        _count: true,
        _sum: { totalAmount: true },
      }),
      prisma.saleInvoice.aggregate({
        _count: true,
        _sum: { totalAmount: true },
      }),
      prisma.saleInvoiceItem.findMany({
        select: {
          quantity: true,
          unitPrice: true,
          importPrice: true,
        },
      }),
      prisma.importInvoice.findMany({
        orderBy: { createdAt: "desc" },
        take: 3,
        select: {
          id: true,
          invoiceNumber: true,
          createdAt: true,
        },
      }),
      prisma.saleInvoice.findMany({
        orderBy: { createdAt: "desc" },
        take: 3,
        select: {
          id: true,
          invoiceNumber: true,
          createdAt: true,
        },
      }),
      prisma.saleInvoiceItem.groupBy({
        by: ["productId"],
        _sum: {
          quantity: true,
          totalPrice: true,
        },
        orderBy: {
          _sum: { quantity: "desc" },
        },
        take: 5,
      }),
    ]);

    // Lấy tên sản phẩm sau khi groupBy vì Prisma không include relation trong groupBy.
    const topProductIds = topProductGroups.map((item) => item.productId);
    const topProducts = await prisma.product.findMany({
      where: { id: { in: topProductIds } },
      select: { id: true, name: true },
    });
    const tenSanPham = new Map(
      topProducts.map((product) => [product.id, product.name])
    );

    // Tính lợi nhuận theo từng dòng bán: doanh thu dòng - giá vốn * số lượng.
    const totalImportCost = saleItems.reduce(
      (tong, item) =>
        tong + Number(item.importPrice) * item.quantity,
      0
    );
    const totalSaleAmount = Number(saleSummary._sum.totalAmount || 0);

    // Gom lịch sử nhập/bán rồi sắp xếp giống dashboard web.
    const recentActivities = [
      ...recentImports.map((invoice) => ({
        id: `import-${invoice.id}`,
        type: "import" as const,
        message: `Nhập hàng #${invoice.invoiceNumber}`,
        createdAt: invoice.createdAt.toISOString(),
      })),
      ...recentSales.map((invoice) => ({
        id: `sale-${invoice.id}`,
        type: "sale" as const,
        message: `Bán hàng #${invoice.invoiceNumber}`,
        createdAt: invoice.createdAt.toISOString(),
      })),
    ]
      .sort(
        (a, b) =>
          new Date(b.createdAt).getTime() - new Date(a.createdAt).getTime()
      )
      .slice(0, 5);

    return NextResponse.json({
      serverTime: new Date().toISOString(),
      stats: {
        totalProducts,
        // Đếm toàn bộ sản phẩm sắp hết, không chỉ 5 sản phẩm trả về bên dưới.
        lowStockProducts: allActiveProducts.filter(
          (product) => product.stock <= product.minStock
        ).length,
        totalImportInvoices: importSummary._count,
        totalSaleInvoices: saleSummary._count,
        totalImportAmount: Number(importSummary._sum.totalAmount || 0),
        totalSaleAmount,
        totalProfit: totalSaleAmount - totalImportCost,
      },
      recentActivities,
      lowStockProducts: allActiveProducts
        .filter((product) => product.stock <= product.minStock)
        .slice(0, 5)
        .map((product) => ({
        ...product,
        sellPrice: decimalToString(product.sellPrice),
        })),
      topProducts: topProductGroups.map((item) => ({
        id: item.productId,
        name: tenSanPham.get(item.productId) || "Không xác định",
        totalSold: item._sum.quantity || 0,
        totalRevenue: Number(item._sum.totalPrice || 0),
      })),
    });
  } catch (error) {
    console.error("Mobile dashboard error:", error);

    return mobileErrorResponse(
      "SERVER_ERROR",
      "Không thể tải dữ liệu tổng quan",
      500
    );
  }
}
