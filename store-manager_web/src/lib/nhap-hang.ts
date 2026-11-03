import type { Prisma } from "@prisma/client";

/**
 * Loi nghi nghiep trong luong nhap hang.
 *
 * Ma loi dung chung giua web va mobile de client doc ra thong bao dung.
 */
export class LoiNhapHang extends Error {
  constructor(
    public readonly maLoi: string,
    message: string,
    public readonly trangThai: number = 400
  ) {
    super(message);
    this.name = "LoiNhapHang";
  }
}

/**
 * Don vị của sản phẩm ở dạng đã đọc từ database.
 *
 * Gom cả thông tin đơn vị tính để hiển thị tên khi ghi phiếu nhập.
 */
export type DonViSanPhamDb = {
  id: string;
  unitId: string;
  conversionQty: number;
  price: Prisma.Decimal | number;
  isDefault: boolean;
  isPurchase: boolean;
  unit: {
    id: string;
    name: string;
    abbreviation: string;
    isBaseUnit: boolean;
  };
};

/**
 * Đơn vị gốc dùng để nhập hàng của một sản phẩm.
 *
 * @param unitId        Id đơn vị tính.
 * @param unitName      Tên đơn vị tính.
 * @param conversionQty Số lượng đơn vị bán lẻ cơ bản có trong 1 đơn vị gốc.
 */
export type DonViGocDaChon = {
  unitId: string;
  unitName: string;
  conversionQty: number;
};

/**
 * Đơn vị bán lẻ cơ bản của sản phẩm.
 *
 * Đây là đơn vị dùng để đếm tồn kho trong database. Mỗi sản phẩm nên có
 * đúng một đơn vị có `conversionQty = 1`.
 */
export function timDonViBanLeCoBan(
  donVis: DonViSanPhamDb[]
): DonViSanPhamDb | null {
  return (
    donVis.find((donVi) => donVi.conversionQty === 1) ??
    donVis.find((donVi) => donVi.isDefault) ??
    null
  );
}

/**
 * Tìm đơn vị gốc dùng để nhập hàng của sản phẩm.
 *
 * Thứ tự ưu tiên:
 * 1. Đơn vị người dùng chỉ định trong phiếu nhập.
 * 2. Đơn vị được cấu hình là đơn vị gốc (`isPurchase`).
 * 3. Đơn vị bán lẻ cơ bản, coi như nhập theo đơn vị lẻ.
 *
 * @param donVis  Danh sách đơn vị của sản phẩm.
 * @param unitId  Đơn vị người dùng chỉ định, null hoặc rỗng nghĩa là tự chọn.
 * @return Đơn vị gốc, null nếu sản phẩm chưa có đơn vị nào.
 */
export function timDonViGoc(
  donVis: DonViSanPhamDb[],
  unitId?: string | null
): DonViSanPhamDb | null {
  if (unitId) {
    const donViChon = donVis.find((donVi) => donVi.unitId === unitId);

    if (!donViChon) {
      throw new LoiNhapHang(
        "UNIT_NOT_FOUND",
        "Đơn vị gốc không thuộc sản phẩm này",
        400
      );
    }

    return donViChon;
  }

  const donViGoc = donVis.find((donVi) => donVi.isPurchase);

  if (donViGoc) {
    return donViGoc;
  }

  return timDonViBanLeCoBan(donVis);
}

/**
 * Quy đổi giá của đơn vị gốc sang giá của đơn vị bán lẻ cơ bản.
 *
 * Giá nhập lưu trên sản phẩm luôn tính theo đơn vị bán lẻ cơ bản, nhờ vậy
 * biên lợi nhuận ở POS luôn đúng dù bán theo thùng hay bán lẻ.
 *
 * @param giaDonViGoc Giá của 1 đơn vị gốc.
 * @param heSoQuyDoi Số lượng đơn vị bán lẻ cơ bản trong 1 đơn vị gốc.
 * @return Giá của 1 đơn vị bán lẻ cơ bản.
 */
export function quyDoiGiaVeDonViCoBan(
  giaDonViGoc: number,
  heSoQuyDoi: number
): number {
  if (!heSoQuyDoi || heSoQuyDoi <= 0) {
    return giaDonViGoc;
  }

  return giaDonViGoc / heSoQuyDoi;
}

/**
 * Dữ liệu một dòng hàng người dùng gửi lên khi tạo phiếu nhập.
 */
export type DongHangNhapInput = {
  productId?: string | null;
  productName: string;
  categoryId?: string | null;
  quantity: number;
  unitPrice: number;
  unitId?: string | null;
  baseUnitId?: string | null;
  conversionQty?: number | null;
};

/**
 * Tham số tạo phiếu nhập.
 */
export type ThamSoTaoPhieuNhap = {
  supplierId?: string | null;
  notes?: string | null;
  status?: "PENDING" | "COMPLETED" | "CANCELLED";
  createdById: string;
  items: DongHangNhapInput[];
};

/**
 * Kết quả tạo phiếu nhập.
 */
export type KetQuaTaoPhieuNhap = {
  phieu: any;
  tongTien: number;
};

/**
 * Tạo phiếu nhập hàng và cập nhật tồn kho trong một transaction.
 *
 * Quy tắc quan trọng:
 * - Số lượng nhập tính theo đơn vị gốc (thùng, lốc...).
 * - Tồn kho lưu theo đơn vị bán lẻ cơ bản nên tăng `quantity * conversionQty`.
 * - Giá nhập của sản phẩm được quy đổi về đơn vị bán lẻ cơ bản.
 * - Đơn vị gốc, hệ số quy đổi và số lượng quy đổi được chụp lại trên dòng hàng
 *   để lịch sử nhập hàng không bị ảnh hưởng khi cấu hình đơn vị thay đổi.
 *
 * @param tx     Transaction client của Prisma.
 * @param thamSo Dữ liệu phiếu nhập cần tạo.
 * @return Phiếu nhập vừa tạo kèm tổng tiền.
 */
export async function taoPhieuNhapVaTangTon(
  tx: Prisma.TransactionClient,
  thamSo: ThamSoTaoPhieuNhap
): Promise<KetQuaTaoPhieuNhap> {
  let tongTien = 0;
  const dongHangChuanBia: Prisma.ImportInvoiceItemCreateWithoutImportInvoiceInput[] =
    [];

  for (const item of thamSo.items) {
    const thanhTien = item.quantity * item.unitPrice;
    tongTien += thanhTien;

    let maSanPham = item.productId || null;
    let donVis: DonViSanPhamDb[] = [];
    let giaNhapCu: Prisma.Decimal | null = null;

    if (maSanPham) {
      const sanPham = await tx.product.findUnique({
        where: { id: maSanPham },
        include: { units: { include: { unit: true } } },
      });

      if (!sanPham) {
        throw new LoiNhapHang(
          "PRODUCT_NOT_FOUND",
          `Không tìm thấy sản phẩm ${maSanPham}`,
          404
        );
      }

      donVis = sanPham.units as unknown as DonViSanPhamDb[];
      giaNhapCu = sanPham.importPrice;
    } else {
      // Tim san pham trung ten truoc khi tao moi, tranh tao trung san pham.
      const sanPhamTrungTen = await tx.product.findFirst({
        where: { name: { equals: item.productName, mode: "insensitive" } },
        include: { units: { include: { unit: true } } },
      });

      if (sanPhamTrungTen) {
        maSanPham = sanPhamTrungTen.id;
        donVis = sanPhamTrungTen.units as unknown as DonViSanPhamDb[];
        giaNhapCu = sanPhamTrungTen.importPrice;
      } else {
        const sanPhamMoi = await taoSanPhamMoi(
          tx,
          item,
          thamSo.items.filter((dong) => dong.productId).length
        );

        maSanPham = sanPhamMoi.id;
        donVis = sanPhamMoi.donVis as unknown as DonViSanPhamDb[];
        giaNhapCu = null;
      }
    }

    // Chon don vi goc de nhap. Neu san pham chua co don vi nao thi mac
    // dinh dung 1 don vi co ban.
    const donViGoc = timDonViGoc(donVis, item.unitId);
    const heSoQuyDoi = donViGoc?.conversionQty ?? 1;
    const soLuongCoBan = item.quantity * heSoQuyDoi;

    // Gia nhap luong theo don vi ban le co ban.
    const giaNhapCoBan = quyDoiGiaVeDonViCoBan(item.unitPrice, heSoQuyDoi);

    // Tang ton kho va cap nhat gia nhap. Dieu kien stock >= 0 chan giam am.
    await tx.product.update({
      where: { id: maSanPham },
      data: {
        stock: { increment: soLuongCoBan },
        previousImportPrice: giaNhapCu,
        importPrice: giaNhapCoBan,
      },
    });

    dongHangChuanBia.push({
      product: { connect: { id: maSanPham } },
      quantity: item.quantity,
      unit: donViGoc?.unitId ? { connect: { id: donViGoc.unitId } } : undefined,
      unitName: donViGoc?.unit.name ?? null,
      conversionQty: heSoQuyDoi,
      quantityBase: soLuongCoBan,
      unitPrice: item.unitPrice,
      previousUnitPrice: giaNhapCu,
      totalPrice: thanhTien,
    });
  }

  const phieu = await tx.importInvoice.create({
    data: {
      invoiceNumber: await taoSoPhieuNhap(tx),
      supplierId: thamSo.supplierId || null,
      totalAmount: tongTien,
      notes: thamSo.notes || null,
      status: thamSo.status ?? "COMPLETED",
      createdById: thamSo.createdById,
      items: { create: dongHangChuanBia },
    },
    include: {
      supplier: { select: { id: true, name: true, phone: true } },
      items: {
        include: {
          product: { select: { id: true, name: true, sku: true } },
          unit: { select: { id: true, name: true, abbreviation: true } },
        },
      },
    },
  });

  return { phieu, tongTien };
}

/**
 * Tạo sản phẩm mới kèm cấu hình đơn vị khi nhập hàng.
 *
 * Sản phẩm mới cần ít nhất một đơn vị bán lẻ cơ bản. Nếu người dùng chọn
 * đơn vị gốc khác đơn vị cơ bản thì tạo thêm đơn vị gốc với hệ số quy đổi.
 *
 * @param tx       Transaction client của Prisma.
 * @param item     Dòng hàng đang nhập.
 * @param soDong   Số dòng hàng đã xử lý, dùng để sinh mã khác nhau.
 * @return Sản phẩm vừa tạo kèm danh sách đơn vị đã cấu hình.
 */
async function taoSanPhamMoi(
  tx: Prisma.TransactionClient,
  item: DongHangNhapInput,
  soDong: number
) {
  const { sku, slug } = await taoMaSanPham(tx, item.productName, soDong);

  const sanPham = await tx.product.create({
    data: {
      name: item.productName,
      sku,
      slug,
      categoryId: item.categoryId || null,
      importPrice: item.unitPrice,
      // Gia ban mac dinh gap 30% gia nhap, giong web.
      sellPrice: item.unitPrice * 1.3,
      stock: 0,
      minStock: 10,
    },
  });

  // Chon don vi ban le co ban. Uu tien don vi nguoi dung chon, sau do don vi
  // co ban bat dau trong bang danh muc don vi tinh.
  const donViTinhCoBan = await tx.unit.findFirst({
    where: { isBaseUnit: true },
    orderBy: { createdAt: "asc" },
  });

  const donViGoc = item.unitId
    ? await tx.unit.findUnique({ where: { id: item.unitId } })
    : null;

  const donViCoBan =
    (item.baseUnitId
      ? await tx.unit.findUnique({ where: { id: item.baseUnitId } })
      : null) ??
    (donViGoc && donViGoc.isBaseUnit ? donViGoc : null) ??
    donViTinhCoBan;

  if (!donViCoBan) {
    throw new LoiNhapHang(
      "UNIT_NOT_FOUND",
      "Chưa có đơn vị tính nào trong hệ thống, không thể tạo sản phẩm mới",
      400
    );
  }

  const heSoQuyDoi =
    item.conversionQty && item.conversionQty > 0 ? item.conversionQty : 1;

  // Don vi ban le co ban luon co he so quy doi bang 1 va la don mac dinh.
  await tx.productUnit.create({
    data: {
      productId: sanPham.id,
      unitId: donViCoBan.id,
      conversionQty: 1,
      price: quyDoiGiaVeDonViCoBan(item.unitPrice, heSoQuyDoi),
      isDefault: true,
      isPurchase: heSoQuyDoi === 1 && donViGoc?.id === donViCoBan.id,
    },
  });

  // Neu don vi goc khac don vi co ban thi them don vi goc rieng.
  if (donViGoc && donViGoc.id !== donViCoBan.id) {
    await tx.productUnit.create({
      data: {
        productId: sanPham.id,
        unitId: donViGoc.id,
        conversionQty: heSoQuyDoi,
        price: item.unitPrice,
        isDefault: false,
        isPurchase: true,
      },
    });
  }

  const donVis = await tx.productUnit.findMany({
    where: { productId: sanPham.id },
    include: { unit: true },
  });

  return { id: sanPham.id, donVis };
}

/**
 * Sinh so phieu nhap duy nhi, dang `PN-yyyyMMdd-xxxxxx`.
 *
 * Vong lap kiem tra lai so moi trong truong hop trung ngau nhien.
 */
export async function taoSoPhieuNhap(tx: Prisma.TransactionClient) {
  const now = new Date();
  const ngay = [
    now.getFullYear(),
    String(now.getMonth() + 1).padStart(2, "0"),
    String(now.getDate()).padStart(2, "0"),
  ].join("");

  for (let attempt = 0; attempt < 5; attempt += 1) {
    const ngauNhien = Math.floor(Math.random() * 1_000_000)
      .toString()
      .padStart(6, "0");
    const soPhieu = `PN-${ngay}-${ngauNhien}`;

    const existing = await tx.importInvoice.findUnique({
      where: { invoiceNumber: soPhieu },
      select: { id: true },
    });

    if (!existing) {
      return soPhieu;
    }
  }

  throw new Error("Không thể tạo số phiếu nhập duy nhất");
}

/**
 * Sinh SKU va slug duy nhi cho san pham moi.
 *
 * Ten san pham co the trung nhau giua cac lan nhap khac nhau, nen duoi mot
 * ten van sinh nhieu ma khac nhau de khong vi pham rang buoc unique.
 */
export async function taoMaSanPham(
  tx: Prisma.TransactionClient,
  tenSanPham: string,
  soDong: number = 0
) {
  const tienTo =
    tenSanPham
      .toUpperCase()
      .replace(/[^A-Z0-9]/g, "")
      .substring(0, 3) || "PRO";

  const tenChuanHoa = tenSanPham
    .toLowerCase()
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/[^a-z0-9]+/g, "-")
    .replace(/^-+|-+$/g, "");

  for (let attempt = 0; attempt < 5; attempt += 1) {
    const ngauNhien = Math.floor(Math.random() * 1_000_000)
      .toString()
      .padStart(6, "0");

    const sku = `${tienTo}-${ngauNhien}`;
    const slug = `${tenChuanHoa || "san-pham"}-${ngauNhien}-${soDong}`;

    const existingSku = await tx.product.findUnique({
      where: { sku },
      select: { id: true },
    });

    const existingSlug = await tx.product.findUnique({
      where: { slug },
      select: { id: true },
    });

    if (!existingSku && !existingSlug) {
      return { sku, slug };
    }
  }

  throw new Error("Không thể tạo mã sản phẩm duy nhất");
}
