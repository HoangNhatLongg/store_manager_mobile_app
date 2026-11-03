/**
 * Script kiem tra luong gan don vi mac dinh khi tao san pham.
 *
 * Kiem tra truc tiep logic trong `src/lib/don-vi-san-pham.ts` bang cach tao
 * san pham thu nghiem roi xoa lai, tranh pollute du lieu that.
 *
 * Chay: npx tsx scripts/kiem-tra-tao-san-pham-co-don-vi.ts
 */
import { PrismaClient } from "@prisma/client";

const prisma = new PrismaClient();

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

  const maSanPham = `TEST-DV-${Date.now()}`;

  // Tao san pham kem don vi mac dinh va don vi goc.
  const sanPham = await prisma.product.create({
    data: {
      name: "San pham kiem tra don vi",
      slug: `test-don-vi-${Date.now()}`,
      sku: maSanPham,
      categoryId: danhMuc.id,
      importPrice: 10000,
      sellPrice: 15000,
      stock: 0,
      minStock: 10,
      units: {
        create: [
          {
            unitId: donViCoBan.id,
            conversionQty: 1,
            price: 15000,
            isDefault: true,
            isPurchase: false,
          },
          {
            unitId: donViGoc.id,
            conversionQty: 24,
            price: 360000,
            isDefault: false,
            isPurchase: true,
          },
        ],
      },
    },
    include: { units: { include: { unit: true } } },
  });

  console.log("=== DON VI DA TAO ===");
  for (const donVi of sanPham.units) {
    console.log(
      `${donVi.unit.name}: conversionQty=${donVi.conversionQty} isDefault=${donVi.isDefault} isPurchase=${donVi.isPurchase} price=${donVi.price}`
    );
  }

  const donViMacDinh = sanPham.units.filter((donVi) => donVi.isDefault);
  const donViGocChon = sanPham.units.filter((donVi) => donVi.isPurchase);

  console.log("=== KET QUA ===");
  console.log("So don vi mac dinh:", donViMacDinh.length, "(mong doi 1)");
  console.log("He so quy doi don vi mac dinh:", donViMacDinh[0]?.conversionQty, "(mong doi 1)");
  console.log("So don vi goc:", donViGocChon.length, "(mong doi 1)");

  const ok =
    donViMacDinh.length === 1 &&
    donViMacDinh[0].conversionQty === 1 &&
    donViGocChon.length === 1;
  console.log(ok ? "PASS" : "FAIL");

  // Don vi goc phai quy doi dung so luong don vi ban le co ban.
  if (donViGocChon.length === 1 && donViMacDinh.length === 1) {
    const heSo = donViGocChon[0].conversionQty;
    console.log(`Nhap 10 ${donViGocChon[0].unit.name} se tang ton kho ${10 * heSo} ${donViMacDinh[0].unit.name}`);
  }

  await prisma.product.delete({ where: { id: sanPham.id } });
  console.log("Da xoa san pham kiem tra.");
}

main()
  .catch((error) => {
    console.error("LOI:", error);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
