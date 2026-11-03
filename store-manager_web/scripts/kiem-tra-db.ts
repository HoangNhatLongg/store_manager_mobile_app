import { PrismaClient } from "@prisma/client";

const prisma = new PrismaClient();

async function main() {
  const bangs = await prisma.$queryRawUnsafe<{ table_name: string }[]>(
    "SELECT table_name FROM information_schema.tables WHERE table_schema='public' ORDER BY table_name;"
  );
  console.log("=== BANG ===");
  console.log(bangs.map((b) => b.table_name).join("\n"));

  const sanPham = await prisma.product.findMany({
    select: { id: true, name: true, sku: true, stock: true, units: { select: { id: true, unitId: true, conversionQty: true, isDefault: true } } },
    take: 10,
  });
  console.log("=== SAN PHAM (10) ===");
  console.log(JSON.stringify(sanPham, null, 2));

  const donVi = await prisma.unit.findMany();
  console.log("=== DON VI ===");
  console.log(JSON.stringify(donVi.map((d) => ({ id: d.id, name: d.name, abbr: d.abbreviation, base: d.isBaseUnit })), null, 2));

  const phieu = await prisma.importInvoice.findMany({
    select: { invoiceNumber: true, totalAmount: true, items: { select: { productId: true, quantity: true, unitPrice: true } } },
    take: 5,
  });
  console.log("=== PHIEU NHAP (5) ===");
  console.log(JSON.stringify(phieu, null, 2));
}

main()
  .catch((e) => {
    console.error("LOI:", e.message);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
