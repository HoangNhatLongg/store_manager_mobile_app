import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { getMobileUser, mobileUnauthorizedResponse } from "@/lib/mobile-auth";
import { mobileErrorResponse } from "@/lib/mobile-http";

/**
 * GET /api/v1/analytics/revenue
 *
 * Bao cao doanh thu cho ung dung mobile.
 *
 * Tham so:
 * - startDate, endDate: khoang ngay duong dang `yyyy-MM-dd`.
 * - period: khoang gom nhom du lieu.
 *   + `year`  : gom theo thang, dung cho bieu do doanh thu ca nam.
 *   + `month` : gom theo ngay, dung cho bieu do doanh thu trong thang.
 *   + `day`   : gom theo gio, dung cho chi tiet mot ngay.
 *
 * Khong co `startDate` thi mac dinh lay 365 ngay gan nhat.
 */
export async function GET(request: NextRequest) {
  const user = await getMobileUser(request);

  if (!user) {
    return mobileUnauthorizedResponse();
  }

  try {
    const { searchParams } = new URL(request.url);
    const chuoiNgayBatDau = searchParams.get("startDate");
    const chuoiNgayKetThuc = searchParams.get("endDate");
    const period = searchParams.get("period") || "month";

    // Chuan hoa moc thoi gian theo gio dia phuong cua may chu.
    const ngayBatDau = chuoiNgayBatDau ? tachNgay(chuoiNgayBatDau, true) : null;
    const ngayKetThuc = chuoiNgayKetThuc ? tachNgay(chuoiNgayKetThuc, false) : null;

    if (
      (chuoiNgayBatDau && !ngayBatDau) ||
      (chuoiNgayKetThuc && !ngayKetThuc)
    ) {
      return mobileErrorResponse(
        "VALIDATION_ERROR",
        "startDate hoặc endDate không đúng định dạng yyyy-MM-dd",
        400
      );
    }

    // Moc mac dinh la 365 ngay gan nhat neu nguoi dung khong chon khoang.
    const mocMacDinh = new Date();
    mocMacDinh.setHours(23, 59, 59, 999);
    const mocBatDauMacDinh = new Date(mocMacDinh.getTime() - 365 * 24 * 60 * 60 * 1000);

    const batDau = ngayBatDau ?? mocBatDauMacDinh;
    const ketThuc = ngayKetThuc ?? mocMacDinh;

    const hoaDon = await prisma.saleInvoice.findMany({
      where: {
        status: "COMPLETED",
        createdAt: { gte: batDau, lte: ketThuc },
      },
      include: { items: true },
      orderBy: { createdAt: "asc" },
    });

    // Tinh tong hop doanh thu, gia von va loi nhuan.
    let tongDoanhThu = 0;
    let tongGiaVon = 0;
    let tongSoLuongBan = 0;

    const nhomTheoKy = new Map<
      string,
      { revenue: number; importCost: number; profit: number; count: number }
    >();

    for (const hoaDonItem of hoaDon) {
      // Khoa nhom du lieu de gom cac hoa don cung ky lai mot nhom.
        const khoa = layKhoaKy(hoaDonItem.createdAt, period);
        const nhom = nhomTheoKy.get(khoa) ?? {
          revenue: 0,
          importCost: 0,
          profit: 0,
          count: 0,
        };

        for (const dong of hoaDonItem.items) {
          const doanhThu = Number(dong.totalPrice);
          const giaVon = Number(dong.importPrice) * dong.quantity;

          tongDoanhThu += doanhThu;
          tongGiaVon += giaVon;
          tongSoLuongBan += dong.quantity;

          nhom.revenue += doanhThu;
          nhom.importCost += giaVon;
          nhom.profit += doanhThu - giaVon;
        }

        // So don duoc dem theo hoa don, khong phai theo dong hang.
        nhom.count += 1;

        nhomTheoKy.set(khoa, nhom);
    }

    // So khach hang mua hang trong khoang la duy nhat.
    const soKhachHang = await prisma.saleInvoice.groupBy({
      by: ["customerId"],
      where: {
        status: "COMPLETED",
        createdAt: { gte: batDau, lte: ketThuc },
        customerId: { not: null },
      },
    }).then((ketQua) => ketQua.length);

    const tongLoiNhuan = tongDoanhThu - tongGiaVon;

    return NextResponse.json({
      data: {
        summary: {
          totalRevenue: tongDoanhThu,
          totalImportCost: tongGiaVon,
          totalProfit: tongLoiNhuan,
          grossMargin: tongDoanhThu > 0 ? (tongLoiNhuan / tongDoanhThu) * 100 : 0,
          totalSales: hoaDon.length,
          totalInvoices: hoaDon.length,
          totalProductsSold: tongSoLuongBan,
          totalCustomers: soKhachHang,
        },
        byPeriod: Array.from(nhomTheoKy.entries())
          .map(([khoa, soLieu]) => ({
            key: khoa,
            label: layNhanKy(khoa, period),
            revenue: soLieu.revenue,
            importCost: soLieu.importCost,
            profit: soLieu.profit,
            count: soLieu.count,
          }))
          .sort((a, b) => a.key.localeCompare(b.key)),
      },
      serverTime: new Date().toISOString(),
    });
  } catch (error) {
    console.error("Mobile revenue analytics error:", error);

    return mobileErrorResponse(
      "SERVER_ERROR",
      "Không thể tải báo cáo doanh thu",
      500
    );
  }
}

/**
 * Tach chuoi ngay `yyyy-MM-dd` thanh moc thoi gian.
 *
 * @param chuoi   Chuoi ngay can tach.
 * @param dauNgay True neu lay dau ngay, false neu lay cuoi ngay.
 * @return Moc thoi gian hoac null neu chuoi khong hop le.
 */
function tachNgay(chuoi: string, dauNgay: boolean): Date | null {
  const phan = chuoi.split("-").map(Number);

  if (phan.length !== 3 || phan.some((giaTri) => Number.isNaN(giaTri))) {
    return null;
  }

  const [nam, thang, ngay] = phan;

  // Neu ngay khong ton tai trong thang thi tra ve null de bao loi.
  const ngayDauThang = new Date(nam, thang - 1, 1);
  const soNgayTrongThang = new Date(nam, thang, 0).getDate();

  if (ngay < 1 || ngay > soNgayTrongThang) {
    return null;
  }

  if (dauNgay) {
    return new Date(ngayDauThang.getFullYear(), thang - 1, ngay, 0, 0, 0, 0);
  }

  return new Date(nam, thang - 1, ngay, 23, 59, 59, 999);
}

/**
 * Lay khoa nhom du lieu theo ky.
 *
 * @param thoiGian Thoi diem tao hoa don.
 * @param period   Ky gom nhom.
 * @return Khoa nhom, dinh dang tuy thuoc vao ky.
 */
function layKhoaKy(thoiGian: Date, period: string): string {
  const nam = thoiGian.getFullYear();
  const thang = String(thoiGian.getMonth() + 1).padStart(2, "0");
  const ngay = String(thoiGian.getDate()).padStart(2, "0");
  const gio = String(thoiGian.getHours()).padStart(2, "0");

  switch (period) {
    // Gom theo thang de ve bieu do ca nam.
    case "year":
      return `${nam}-${thang}`;
    // Gom theo ngay de ve bieu do trong thang.
    case "month":
      return `${nam}-${thang}-${ngay}`;
    // Gom theo gio de xem chi tiet mot ngay.
    default:
      return `${nam}-${thang}-${ngay} ${gio}:00`;
  }
}

/**
 * Sinh nhan de hien thi tren bieu do.
 *
 * @param khoa   Khoa nhom du lieu.
 * @param period Ky gom nhom.
 * @return Nhan hien thi.
 */
function layNhanKy(khoa: string, period: string): string {
  const phan = khoa.split(/[- :]/);

  switch (period) {
    case "year":
      return `Tháng ${Number(phan[1])}`;
    case "month":
      return `${Number(phan[2])}/${Number(phan[1])}`;
    default:
      return `${Number(phan[2])}/${Number(phan[1])} ${phan[3]}`;
  }
}
