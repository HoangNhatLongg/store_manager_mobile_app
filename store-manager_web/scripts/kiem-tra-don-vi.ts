import { PrismaClient } from "@prisma/client";

const prisma = new PrismaClient();

async function main() {
  const sanPham = await prisma.product.findMany({
    select: {
      sku: true,
      name: true,
      units: {
        select: {
          isDefault: true,
          isPurchase: true,
          conversionQty: true,
          unit: { select: { name: true, abbreviation: true } },
        },
      },
    },
    orderBy: { sku: "asc" },
  });

  console.log("=== SAN PHAM THIEU DON VI MAC DINH ===");
  const thieu = sanPham.filter(
    (p) => !p.units.some((u) => u.isDefault && u.conversionQty === 1)
  );
  console.log(thieu.length === 0 ? "(khong co)" : JSON.stringify(thieu, null, 2));

  console.log("=== SAN PHAM CHUA CO DON VI NAO ===");
  const khongCoDonVi = sanPham.filter((p) => p.units.length === 0);
  console.log(
    khongCoDonVi.length === 0 ? "(khong co)" : JSON.stringify(khongCoDonVi, null, 2)
  );

  console.log("=== SAN PHAM THIEU DON VI GOC NHAP HANG ===");
  const thieuGoc = sanPham.filter((p) => !p.units.some((u) => u.isPurchase));
  console.log(
    thieuGoc.length === 0 ? "(khong co)" : JSON.stringify(thieuGoc, null, 2)
  );

  console.log("=== CO NHIEU DON VI MAC DINH ===");
  const nhieu = sanPham.filter((p) => p.units.filter((u) => u.isDefault).length > 1);
  console.log(nhieu.length === 0 ? "(khong co)" : JSON.stringify(nhieu, null, 2));

  console.log("=== CO NHIU DON VI GOC ===");
  const nhieuGoc = sanPham.filter((p) => p.units.filter((u) => u.isPurchase).length > 1);
  console.log(nhieuGoc.length === 0 ? "(khong co)" : JSON.stringify(nhieuGoc, null, 2));

  console.log("=== TONG SO SAN PHAM ===");
  console.log(sanPham.length);
}

main()
  .catch((e) => console.error("LOI:", e.message))
  .finally(async () => {
    await prisma.$disconnect();
  });
