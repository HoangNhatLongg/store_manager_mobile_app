import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { getMobileUser, mobileUnauthorizedResponse } from "@/lib/mobile-auth";
import { mobileErrorResponse } from "@/lib/mobile-http";

/**
 * GET /api/v1/analytics/revenue-daily
 *
 * Chi tiet doanh thu cua mot ngay, dung cho buoc drill-down cuoi cung tren
 * man hinh doanh thu (thang -> ngay -> san pham da ban).
 *
 * Tham so:
 * - date: ngay can xem, dinh dang `yyyy-MM-dd`.
 */
export async function GET(request: NextRequest) {
  const user = await getMobileUser(request);

  if (!user) {
    return mobileUnauthorizedResponse();
  }

  try {
    const { searchParams } = new URL(request.url);
    const chuoiNgay = searchParams.get("date");

    if (!chuoiNgay) {
      return mobileErrorResponse(
        "VALIDATION_ERROR",
        "Thiếu tham số date",
        400
      );
    }

    const phan = chuoiNgay.split("-").map(Number);

    if (phan.length !== 3 || phan.some((giaTri) => Number.isNaN(giaTri))) {
      return mobileErrorResponse(
        "VALIDATION_ERROR",
        "date không đúng định dạng yyyy-MM-dd",
        400
      );
    }

    const [nam, thang, ngay] = phan;
    const dauNgay = new Date(nam, thang - 1, ngay, 0, 0, 0, 0);
    const cuoiNgay = new Date(nam, thang - 1, ngay, 23, 59, 59, 999);

    const hoaDon = await prisma.saleInvoice.findMany({
      where: {
        status: "COMPLETED",
        createdAt: { gte: dauNgay, lte: cuoiNgay },
      },
      include: {
        items: {
          include: {
            product: {
              select: {
                id: true,
                name: true,
                sku: true,
                category: { select: { id: true, name: true } },
              },
            },
          },
        },
      },
      orderBy: { createdAt: "desc" },
    });

    // Gom cac dong hang theo san pham de hien thi trong mot bang.
    const banSanPham = new Map<
      string,
      {
        productId: string;
        productName: string;
        productSku: string;
        categoryName: string | null;
        quantity: number;
        unitPrice: number;
        totalPrice: number;
        importPrice: number;
        profit: number;
      }
    >();

    let tongDoanhThu = 0;
    let tongGiaVon = 0;
    let tongSoLuongBan = 0;

    for (const hoaDonItem of hoaDon) {
      for (const dong of hoaDonItem.items) {
        const doanhThu = Number(dong.totalPrice);
        const giaVon = Number(dong.importPrice) * dong.quantity;
        const loiNhuan = doanhThu - giaVon;

        tongDoanhThu += doanhThu;
        tongGiaVon += giaVon;
        tongSoLuongBan += dong.quantity;

        const sanPhamDaBan = banSanPham.get(dong.productId);

        if (sanPhamDaBan) {
          sanPhamDaBan.quantity += dong.quantity;
          sanPhamDaBan.totalPrice += doanhThu;
          sanPhamDaBan.profit += loiNhuan;
        } else {
          banSanPham.set(dong.productId, {
            productId: dong.productId,
            productName: dong.product?.name || "Không xác định",
            productSku: dong.product?.sku || "",
            categoryName: dong.product?.category?.name || null,
            quantity: dong.quantity,
            unitPrice: Number(dong.unitPrice),
            totalPrice: doanhThu,
            importPrice: Number(dong.importPrice),
            profit: loiNhuan,
          });
        }
      }
    }

    const tongLoiNhuan = tongDoanhThu - tongGiaVon;

    return NextResponse.json({
      data: {
        date: chuoiNgay,
        summary: {
          totalRevenue: tongDoanhThu,
          totalImportCost: tongGiaVon,
          totalProfit: tongLoiNhuan,
          grossMargin: tongDoanhThu > 0 ? (tongLoiNhuan / tongDoanhThu) * 100 : 0,
          totalInvoices: hoaDon.length,
          totalProductsSold: tongSoLuongBan,
        },
        // San pham ban duoc sap xep theo so luong giam dan.
        productsSold: Array.from(banSanPham.values()).sort(
          (a, b) => b.quantity - a.quantity
        ),
      },
      serverTime: new Date().toISOString(),
    });
  } catch (error) {
    console.error("Mobile revenue daily error:", error);

    return mobileErrorResponse(
      "SERVER_ERROR",
      "Không thể tải chi tiết doanh thu trong ngày",
      500
    );
  }
}
