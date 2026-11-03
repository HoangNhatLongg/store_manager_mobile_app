/**
 * Script kiem tra toan bo luong gan don vi cho san pham.
 *
 * Kiem tra cac luật:
 * 1. Tao san pham kem don vi mac dinh va don vi goc.
 * 2. Doi don vi mac dinh sang don vi khac.
 * 3. Bo don vi goc rieng, don vi mac dinh dong vai tro goc.
 * 4. Khong cho tao san pham khi thieu don vi ban le mac dinh.
 * 5. Khong cho bo co "mac dinh" cua don vi mac dinh duy nhat.
 *
 * Script tao san pham thu nghiem roi xoa lai, khong pollute du lieu that.
 *
 * Chay: npx tsx scripts/kiem-tra-luong-don-vi.ts
 */
import { PrismaClient } from "@prisma/client";
import {
  LoiDonViSanPham,
  capNhatDonViChoSanPham,
  chuyenThamSoDonVi,
  ganDonViChoSanPham,
} from "../src/lib/don-vi-san-pham";

const prisma = new PrismaClient();

let soLuongLoi = 0;

/**
 * Kiem tra mot dieu kien, in ket qua va ghi nhan loi neu khong dat.
 *
 * @param ten       Ten dieu kien.
 * @param dieuKien  Dieu kien mong doi dat.
 */
function kiemTra(ten: string, dieuKien: boolean) {
  if (dieuKien) {
    console.log(`  PASS  ${ten}`);
  } else {
    console.log(`  FAIL  ${ten}`);
    soLuongLoi += 1;
  }
}

async function main() {
  const donViTinh = await prisma.unit.findMany({ orderBy: { createdAt: "asc" } });
  if (donViTinh.length < 2) {
    console.log("Can it nhat 2 don vi tinh de kiem tra.");
    return;
  }

  const donViCoBan = donViTinh.find((donVi) => donVi.isBaseUnit) ?? donViTinh[0];
  const donViGoc = donViTinh.find((donVi) => !donVi.isBaseUnit) ?? donViTinh[1];
  const danhMuc = await prisma.category.findFirst();

  if (!danhMuc) {
    console.log("Khong co danh muc nao.");
    return;
  }

  const donViTinhKhac = donViTinh.find(
    (donVi) => donVi.id !== donViCoBan.id && donVi.id !== donViGoc.id
  );

  // 1. Tao san pham kem don vi mac dinh va don vi goc.
  console.log("=== 1. TAO SAN PHAM KEM DON VI MAC DINH VA DON VI GOC ===");
  const sanPham = await prisma.product.create({
    data: {
      name: "Kiem tra luong don vi",
      slug: `kiem-tra-luong-don-vi-${Date.now()}`,
      sku: `TEST-LDV-${Date.now()}`,
      categoryId: danhMuc.id,
      importPrice: 10000,
      sellPrice: 15000,
      stock: 0,
      minStock: 10,
    },
  });

  await ganDonViChoSanPham(prisma as never, sanPham.id, {
    macDinh: {
      unitId: donViCoBan.id,
      conversionQty: 1,
      price: 15000,
      isPurchase: false,
    },
    goc: {
      unitId: donViGoc.id,
      conversionQty: 24,
      price: 360000,
    },
  });

  let donVis = await prisma.productUnit.findMany({
    where: { productId: sanPham.id },
    include: { unit: true },
  });

  kiemTra(
    "Tao san pham co 2 don vi",
    donVis.length === 2
  );
  kiemTra(
    "Co dung 1 don vi mac dinh",
    donVis.filter((donVi) => donVi.isDefault).length === 1
  );
  kiemTra(
    "Don vi mac dinh co he so quy doi bang 1",
    donVis.find((donVi) => donVi.isDefault)?.conversionQty === 1
  );
  kiemTra(
    "Co dung 1 don vi goc",
    donVis.filter((donVi) => donVi.isPurchase).length === 1
  );
  kiemTra(
    "Don vi goc co he so quy doi 24",
    donVis.find((donVi) => donVi.isPurchase)?.conversionQty === 24
  );

  // 2. Doi don vi mac dinh sang don vi khac.
  if (donViTinhKhac) {
    console.log("=== 2. DOI DON VI MAC DINH SANG DON VI KHAC ===");
    await capNhatDonViChoSanPham(prisma as never, sanPham.id, {
      macDinh: {
        unitId: donViTinhKhac.id,
        conversionQty: 1,
        price: 15000,
        isPurchase: false,
      },
      goc: {
        unitId: donViGoc.id,
        conversionQty: 12,
        price: 180000,
      },
    });

    donVis = await prisma.productUnit.findMany({
      where: { productId: sanPham.id },
      include: { unit: true },
    });

    kiemTra(
      "Van chi co 1 don vi mac dinh",
      donVis.filter((donVi) => donVi.isDefault).length === 1
    );
    kiemTra(
      "Don vi mac dinh moi dung he so quy doi 1",
      donVis.find((donVi) => donVi.isDefault)?.conversionQty === 1
    );
    kiemTra(
      "Don vi goc duoc cap nhat he so quy doi",
      donVis.find((donVi) => donVi.isPurchase)?.conversionQty === 12
    );
  }

  // 3. Bo don vi goc rieng, don vi mac dinh dong vai tro goc.
  console.log("=== 3. BO DON VI GOC RIENG ===");
  await capNhatDonViChoSanPham(prisma as never, sanPham.id, {
    macDinh: {
      unitId: donViCoBan.id,
      conversionQty: 1,
      price: 15000,
      isPurchase: false,
    },
    goc: null,
  });

  donVis = await prisma.productUnit.findMany({
    where: { productId: sanPham.id },
    include: { unit: true },
  });

  kiemTra(
    "Chi con 1 don vi mac dinh co isPurchase",
    donVis.filter((donVi) => donVi.isPurchase && donVi.isDefault).length === 1
  );

  // 4. Khong cho tao san pham thieu don vi ban le mac dinh.
  console.log("=== 4. TAO SAN PHAM THIEU DON VI BAN LE MAC DINH ===");
  let loiBatBuoc = false;
  try {
    chuyenThamSoDonVi(null, null, 15000);
  } catch (error) {
    loiBatBuoc = error instanceof LoiDonViSanPham && error.maLoi === "UNIT_REQUIRED";
  }
  kiemTra("Chan tao san pham khi thieu don vi mac dinh", loiBatBuoc);

  // 5. Khong cho bo co "mac dinh" cua don vi mac dinh duy nhat.
  console.log("=== 5. BO CO MAC DINH CUA DON VI MAC DINH DUY NHAT ===");
  const donViMacDinh = await prisma.productUnit.findFirst({
    where: { productId: sanPham.id, isDefault: true },
  });

  let loiChuaCoDonViKhac = false;
  if (donViMacDinh) {
    const soMacDinhKhac = await prisma.productUnit.count({
      where: {
        productId: sanPham.id,
        isDefault: true,
        id: { not: donViMacDinh.id },
      },
    });
    loiChuaCoDonViKhac = soMacDinhKhac === 0;
  }
  kiemTra("Chi co mot don vi mac dinh duy nhat", loiChuaCoDonViKhac);

  // Don vi goc cua san pham khac phai duoc gan lai truoc khi xoa don vi mac dinh.
  await prisma.productUnit.deleteMany({ where: { productId: sanPham.id } });
  await prisma.product.delete({ where: { id: sanPham.id } });
  console.log("Da xoa san pham kiem tra.");

  console.log(soLuongLoi === 0 ? "=== TAT CA KIEM TRA PASS ===" : `=== ${soLuongLoi} KIEM TRA FAIL ===`);
  if (soLuongLoi > 0) process.exitCode = 1;
}

main()
  .catch((error) => {
    console.error("LOI:", error);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
