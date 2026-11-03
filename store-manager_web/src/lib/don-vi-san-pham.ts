import type { Prisma } from "@prisma/client";

/**
 * Loi lien quan den don vi cua san pham.
 *
 * Ma loi dung chung giua web va mobile de client doc ra thong bao dung.
 */
export class LoiDonViSanPham extends Error {
  constructor(
    public readonly maLoi: string,
    message: string,
    public readonly trangThai: number = 400
  ) {
    super(message);
    this.name = "LoiDonViSanPham";
  }
}

/**
 * Don vi ban le co ban (don vi mac dinh) cua san pham.
 *
 * Day la don vi dung de dem ton kho trong database. Moi san pham nen co
 * dung mot don voi `conversionQty = 1`.
 */
export type DonViMacDinh = {
  /** Id don vi tinh trong bang `units`. */
  unitId: string;
  /** He so quy doi, don vi ban le co ban luon bang 1. */
  conversionQty: number;
  /** Gia ban theo don vi ban le co ban. */
  price: number;
  /** True neu don vi nay cung la don vi goc de nhap hang. */
  isPurchase: boolean;
};

/**
 * Don vi goc dung de nhap hang, khac don vi ban le co ban.
 *
 * Vi du 1 thung = 24 lon. Don vi nay chi dung khi nhap hang, ton kho van
 * duoc dem theo don vi ban le co ban.
 */
export type DonViGoc = {
  /** Id don vi tinh trong bang `units`. */
  unitId: string;
  /** So luong don vi ban le co ban co trong 1 don vi goc. */
  conversionQty: number;
  /** Gia cua 1 don vi goc, dung de tinh thanh tien phieu nhap. */
  price: number;
};

/**
 * Tham so don vi can gan cho san pham khi tao moi.
 */
export type ThamSoDonViSanPham = {
  /** Don vi ban le co ban, bat buoc co. */
  macDinh: DonViMacDinh;
  /** Don vi goc nhap hang, co the null neu san pham chi ban le. */
  goc?: DonViGoc | null;
};

/**
 * Gac don vi mac dinh va don vi goc cho san pham vua tao.
 *
 * Luon tao don vi ban le co ban truoc voi `isDefault = true`, `conversionQty = 1`.
 * Sau do neu co don vi goc khac don vi ban le co ban thi tao them dong ho rieng
 * voi `isPurchase = true`.
 *
 * @param tx       Transaction client cua Prisma.
 * @param maSanPham Id san pham vua tao.
 * @param thamSo   Don vi can gan.
 */
export async function ganDonViChoSanPham(
  tx: Prisma.TransactionClient,
  maSanPham: string,
  thamSo: ThamSoDonViSanPham
): Promise<void> {
  const { macDinh, goc } = thamSo;

  if (!macDinh?.unitId) {
    throw new LoiDonViSanPham(
      "UNIT_REQUIRED",
      "Sản phẩm phải có đơn vị bán lẻ mặc định",
      400
    );
  }

  const donViMacDinh = await tx.unit.findUnique({
    where: { id: macDinh.unitId },
    select: { id: true },
  });

  if (!donViMacDinh) {
    throw new LoiDonViSanPham(
      "UNIT_NOT_FOUND",
      "Đơn vị bán lẻ mặc định không tồn tại",
      400
    );
  }

  await tx.productUnit.create({
    data: {
      productId: maSanPham,
      unitId: macDinh.unitId,
      conversionQty: 1,
      price: macDinh.price,
      isDefault: true,
      // Don vi goc co the trung voi don vi ban le co ban, truong hop san pham
      // chi ban le va nhap theo cung 1 don vi.
      isPurchase: goc ? goc.unitId === macDinh.unitId : macDinh.isPurchase,
    },
  });

  if (!goc || goc.unitId === macDinh.unitId) {
    return;
  }

  const donViGoc = await tx.unit.findUnique({
    where: { id: goc.unitId },
    select: { id: true },
  });

  if (!donViGoc) {
    throw new LoiDonViSanPham(
      "UNIT_NOT_FOUND",
      "Đơn vị gốc không tồn tại",
      400
    );
  }

  await tx.productUnit.create({
    data: {
      productId: maSanPham,
      unitId: goc.unitId,
      conversionQty: Math.max(1, goc.conversionQty),
      price: goc.price,
      isDefault: false,
      isPurchase: true,
    },
  });
}

/**
 * Cap nhat don vi ban le mac dinh va don vi goc cho san pham da ton tai.
 *
 * Don vi mac dinh luon co `conversionQty = 1` va `isDefault = true`. Chi mot
 * don vi goc duoc dung de nhap hang cho moi san pham.
 *
 * @param tx       Transaction client cua Prisma.
 * @param maSanPham Id san pham can cap nhat.
 * @param thamSo   Don vi moi can gan.
 */
export async function capNhatDonViChoSanPham(
  tx: Prisma.TransactionClient,
  maSanPham: string,
  thamSo: ThamSoDonViSanPham
): Promise<void> {
  const { macDinh, goc } = thamSo;

  if (!macDinh?.unitId) {
    throw new LoiDonViSanPham(
      "UNIT_REQUIRED",
      "Sản phẩm phải có đơn vị bán lẻ mặc định",
      400
    );
  }

  const donVis = await tx.productUnit.findMany({
    where: { productId: maSanPham },
    include: { unit: true },
  });

  let donViMacDinh = donVis.find((donVi) => donVi.unitId === macDinh.unitId);

  if (!donViMacDinh) {
    // Don vi ban le mac dinh chua duoc gan cho san pham nay. Tao moi de
    // nguoi dung khong phai sang man hinh don vi de cau hinh lai.
    // Chi mot don vi duoc la don vi ban le mac dinh nen bo co cua don vi cu
    // truoc khi gan don vi moi.
    await tx.productUnit.updateMany({
      where: { productId: maSanPham },
      data: { isDefault: false },
    });

    donViMacDinh = await tx.productUnit.create({
      data: {
        productId: maSanPham,
        unitId: macDinh.unitId,
        conversionQty: 1,
        price: macDinh.price,
        isDefault: true,
        isPurchase: false,
      },
      include: { unit: true },
    });
  } else {
    // Chi mot don vi duoc la don vi ban le mac dinh.
    await tx.productUnit.updateMany({
      where: { productId: maSanPham, id: { not: donViMacDinh.id } },
      data: { isDefault: false },
    });

    await tx.productUnit.update({
      where: { id: donViMacDinh.id },
      data: {
        conversionQty: 1,
        price: macDinh.price,
        isDefault: true,
      },
    });
  }

  // Chi mot don vi duoc la don vi goc nhap hang.
  await tx.productUnit.updateMany({
    where: { productId: maSanPham },
    data: { isPurchase: false },
  });

  if (!goc) {
    // Neu khong chon don vi goc rieng thi don vi mac dinh dong vai tro goc.
    await tx.productUnit.update({
      where: { id: donViMacDinh.id },
      data: { isPurchase: true },
    });
    return;
  }

  let donViGoc = donVis.find((donVi) => donVi.unitId === goc.unitId);

  // Don vi goc trung voi don vi ban le mac dinh thi don vi mac dinh dong
  // vai tro goc. He so quy doi luon giu bang 1 vi day la don vi ban le.
  if (donViGoc && donViGoc.id === donViMacDinh.id) {
    await tx.productUnit.update({
      where: { id: donViGoc.id },
      data: { isPurchase: true },
    });
    return;
  }

  if (!donViGoc) {
    donViGoc = await tx.productUnit.create({
      data: {
        productId: maSanPham,
        unitId: goc.unitId,
        conversionQty: Math.max(1, goc.conversionQty),
        price: goc.price,
        isDefault: false,
        isPurchase: true,
      },
      include: { unit: true },
    });
    return;
  }

  await tx.productUnit.update({
    where: { id: donViGoc.id },
    data: {
      conversionQty: Math.max(1, goc.conversionQty),
      price: goc.price,
      isPurchase: true,
    },
  });
}

/**
 * Chuyen doi danh sach don vi do client gui len thanh tham so gan don vi.
 *
 * @param donViMacDinh Don vi ban le co ban do client chon.
 * @param donViGoc     Don vi goc nhap hang do client chon, co the null.
 * @param giaBanMacDinh Gia ban theo don vi ban le co ban, dung khi client
 *                      khong gui gia rieng cho don vi goc.
 */
export function chuyenThamSoDonVi(
  donViMacDinh: { unitId?: string | null; price?: number | null } | null | undefined,
  donViGoc:
    | { unitId?: string | null; conversionQty?: number | null; price?: number | null }
    | null
    | undefined,
  giaBanMacDinh: number
): ThamSoDonViSanPham {
  const maDonViMacDinh = donViMacDinh?.unitId ?? null;

  if (!maDonViMacDinh) {
    throw new LoiDonViSanPham(
      "UNIT_REQUIRED",
      "Sản phẩm phải có đơn vị bán lẻ mặc định",
      400
    );
  }

  const giaMacDinh =
    donViMacDinh?.price !== undefined &&
    donViMacDinh?.price !== null &&
    Number.isFinite(Number(donViMacDinh.price)) &&
    Number(donViMacDinh.price) > 0
      ? Number(donViMacDinh.price)
      : giaBanMacDinh;

  const maDonViGoc = donViGoc?.unitId ?? null;

  if (!maDonViGoc || maDonViGoc === maDonViMacDinh) {
    return {
      macDinh: {
        unitId: maDonViMacDinh,
        conversionQty: 1,
        price: giaMacDinh,
        isPurchase: true,
      },
      goc: null,
    };
  }

  const heSoQuyDoi =
    donViGoc?.conversionQty && donViGoc.conversionQty > 0
      ? Math.floor(donViGoc.conversionQty)
      : 1;

  const giaGoc =
    donViGoc?.price !== undefined &&
    donViGoc?.price !== null &&
    Number.isFinite(Number(donViGoc.price)) &&
    Number(donViGoc.price) > 0
      ? Number(donViGoc.price)
      : giaMacDinh * heSoQuyDoi;

  return {
    macDinh: {
      unitId: maDonViMacDinh,
      conversionQty: 1,
      price: giaMacDinh,
      isPurchase: false,
    },
    goc: {
      unitId: maDonViGoc,
      conversionQty: heSoQuyDoi,
      price: giaGoc,
    },
  };
}
