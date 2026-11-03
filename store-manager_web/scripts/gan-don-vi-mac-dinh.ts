/**
 * Script gan don vi mac dinh cho cac san pham chua co don vi nao.
 *
 * Truoc day san pham duoc tao truc tiep qua API nen co the ton tai san pham
 * khong co `ProductUnit`. Nhung san pham do khong tinh duoc ton kho theo don
 * vi ban le co ban va khong nhap hang duoc. Script nay se gan don vi mac dinh
 * cho chung.
 *
 * Chay: npx tsx scripts/gan-don-vi-mac-dinh.ts
 */
import { PrismaClient } from "@prisma/client";

const prisma = new PrismaClient();

async function main() {
  // San pham chua co don vi nao.
  const sanPhamChuaCoDonVi = await prisma.product.findMany({
    where: { units: { none: {} } },
    select: {
      id: true,
      name: true,
      sku: true,
      sellPrice: true,
      importPrice: true,
    },
  });

  if (sanPhamChuaCoDonVi.length === 0) {
    console.log("Moi san pham deu da co don vi mac dinh.");
    return;
  }

  console.log(`Tim thay ${sanPhamChuaCoDonVi.length} san pham chua co don vi.`);

  // Uu tien don vi co ban, neu khong co thi lay don vi dau tien.
  const donViTinh = await prisma.unit.findMany({
    orderBy: [{ isBaseUnit: "desc" }, { createdAt: "asc" }],
  });

  if (donViTinh.length === 0) {
    console.log("Khong co don vi tinh nao trong he thong, hay tao don vi truoc.");
    return;
  }

  const donViMacDinh = donViTinh[0];

  for (const sanPham of sanPhamChuaCoDonVi) {
    await prisma.productUnit.create({
      data: {
        productId: sanPham.id,
        unitId: donViMacDinh.id,
        conversionQty: 1,
        price: Number(sanPham.sellPrice),
        isDefault: true,
        // Chua co don vi goc rieng thi don vi mac dinh dong vai tro nhap hang.
        isPurchase: true,
      },
    });

    console.log(
      `Da gan don vi "${donViMacDinh.name}" cho san pham ${sanPham.sku} - ${sanPham.name}`
    );
  }

  console.log(`Hoan tat ${sanPhamChuaCoDonVi.length} san pham.`);
}

main()
  .catch((error) => {
    console.error("LOI:", error);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
