/**
 * Script chuan hoa don vi mac dinh cho san pham theo ten san pham.
 *
 * Script `gan-don-vi-mac-dinh.ts` gan don vi co ban chung ("Cái") cho cac san
 * pham cu chua co don vi. Doi voi san pham co ten goi ro (bia lon, sua hop,
 * mi goi...) thi don vi do khong phu hop, nen script nay se doi sang don vi
 * phu hop hon.
 *
 * Chi sua san pham chua co phieu nhap hoac hoa don ban, vi don vi da dung
 * trong lich su giao dich khong nen doi de tranh sai lech ton kho.
 *
 * Chay: npx tsx scripts/chuan-hoa-don-vi-mac-dinh.ts
 */
import { PrismaClient } from "@prisma/client";

const prisma = new PrismaClient();

/**
 * Cac tu khoa trong ten san pham va don vi tuong ung.
 *
 * Thu tu quan trong: don vi nao khop truoc se duoc chon.
 */
const TU_KHOA_DON_VI: { tuKhoa: string[]; vietTat: string }[] = [
  { vietTat: "lon", tuKhoa: ["lon", "bia lon", "lager"] },
  { vietTat: "hộp", tuKhoa: ["hộp", "hop"] },
  { vietTat: "chai", tuKhoa: ["chai"] },
  { vietTat: "gói", tuKhoa: ["gói", "goi"] },
  { vietTat: "tuýp", tuKhoa: ["tuýp", "tuy"] },
  { vietTat: "kg", tuKhoa: ["kg", "kilogram"] },
  { vietTat: "hộp nhỏ", tuKhoa: ["180ml", "250ml", "1l"] },
];

async function main() {
  const donViTinh = await prisma.unit.findMany();
  const theoVietTat = new Map(donViTinh.map((donVi) => [donVi.abbreviation, donVi]));

  // San pham chua co phieu nhap hoac hoa don ban thi moi sua duoc.
  const sanPham = await prisma.product.findMany({
    where: {
      importItems: { none: {} },
      saleItems: { none: {} },
    },
    select: {
      id: true,
      name: true,
      sku: true,
      sellPrice: true,
      units: {
        select: {
          id: true,
          unitId: true,
          conversionQty: true,
          isDefault: true,
          isPurchase: true,
        },
      },
    },
  });

  let soDaSua = 0;

  for (const sanPhamItem of sanPham) {
    // Chi xet san pham co duy nhat mot don vi mac dinh.
    const donViMacDinh = sanPhamItem.units.find((donVi) => donVi.isDefault);
    if (!donViMacDinh || sanPhamItem.units.length !== 1) continue;

    const ten = sanPhamItem.name.toLowerCase();

    const donViPhuHop = TU_KHOA_DON_VI.find((quyTac) =>
      quyTac.tuKhoa.some((tuKhoa) => ten.includes(tuKhoa))
    );

    if (!donViPhuHop) continue;

    const donViMoi = theoVietTat.get(donViPhuHop.vietTat);
    if (!donViMoi || donViMoi.id === donViMacDinh.unitId) continue;

    // Doi thang dong sang don vi phu hop. San pham chua phat sinh giao dich
    // nen khong anh huong ton kho hay lich su.
    await prisma.productUnit.update({
      where: { id: donViMacDinh.id },
      data: { unitId: donViMoi.id },
    });

    soDaSua += 1;
    console.log(
      `Da doi don vi cua ${sanPhamItem.sku} - ${sanPhamItem.name} sang "${donViMoi.name}".`
    );
  }

  console.log(`Hoan tat, da chuan hoa ${soDaSua} san pham.`);
}

main()
  .catch((error) => {
    console.error("LOI:", error);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
