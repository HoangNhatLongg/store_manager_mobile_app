import { PrismaClient } from "@prisma/client";
import bcrypt from "bcryptjs";

const prisma = new PrismaClient();

async function main() {
  console.log("Start seeding...");

  // Create admin user
  const adminPassword = await bcrypt.hash("admin123", 10);
  const admin = await prisma.user.upsert({
    where: { email: "admin@store.com" },
    update: {},
    create: {
      email: "admin@store.com",
      password: adminPassword,
      name: "Admin",
      role: "ADMIN",
    },
  });
  console.log("Created admin user:", admin.email);

  // Create staff user
  const staffPassword = await bcrypt.hash("staff123", 10);
  const staff = await prisma.user.upsert({
    where: { email: "staff@store.com" },
    update: {},
    create: {
      email: "staff@store.com",
      password: staffPassword,
      name: "Nhân viên",
      role: "STAFF",
    },
  });
  console.log("Created staff user:", staff.email);

  // Create categories
  const categories = [
    { name: "Mì ăn liền", slug: "mi-an-lien", description: "Các loại mì ăn liền" },
    { name: "Nước giải khát", slug: "nuoc-giai-khat", description: "Nước ngọt, nước suối" },
    { name: "Bánh kẹo", slug: "banh-keo", description: "Bánh, kẹo các loại" },
    { name: "Sữa", slug: "sua", description: "Sữa tươi, sữa hộp" },
    { name: "Bia", slug: "bia", description: "Bia lon, bia chai" },
    { name: "Cà phê", slug: "ca-phe", description: "Cà phê hòa tan, cà phê lon" },
    { name: "Dầu ăn", slug: "dau-an", description: "Dầu ăn các loại" },
    { name: "Gạo", slug: "gao", description: "Gạo các loại" },
  ];

  for (const cat of categories) {
    await prisma.category.upsert({
      where: { slug: cat.slug },
      update: {},
      create: cat,
    });
  }
  console.log("Created categories:", categories.length);

  // Create suppliers - nhà cung cấp dùng cho phiếu nhập hàng
  const suppliers = [
    {
      name: "Công ty TNHH Phân phối ABC",
      phone: "0901234567",
      email: "contact@abc.vn",
      address: "123 Nguyễn Huệ, Quận 1, TP.HCM",
    },
    {
      name: "Công ty Cổ phần Thực phẩm Việt",
      phone: "0912345678",
      email: "sales@vietfood.vn",
      address: "456 Lê Lợi, Quận 1, TP.HCM",
    },
    {
      name: "Nhà phân phối Đông Nam",
      phone: "0923456789",
      email: "info@dongnam.vn",
      address: "789 Trần Hưng Đạo, Quận 5, TP.HCM",
    },
  ];

  for (const supplier of suppliers) {
    const existing = await prisma.supplier.findFirst({
      where: { name: supplier.name },
    });

    if (!existing) {
      await prisma.supplier.create({ data: supplier });
    }
  }
  console.log("Created suppliers:", suppliers.length);

  // Create customers - khách hàng dùng cho hóa đơn bán hàng
  const customers = [
    {
      name: "Nguyễn Văn A",
      phone: "0909876543",
      email: "nguyenvana@example.com",
      address: "12 Lý Thường Kiệt, Quận 1, TP.HCM",
    },
    {
      name: "Trần Thị B",
      phone: "0918765432",
      email: "tranthib@example.com",
      address: "34 Nguyễn Văn Trỗi, Phường 15, Quận 3, TP.HCM",
    },
  ];

  for (const customer of customers) {
    const existing = await prisma.customer.findFirst({
      where: { name: customer.name },
    });

    if (!existing) {
      await prisma.customer.create({ data: customer });
    }
  }
  console.log("Created customers:", customers.length);

  // Create default units
  const units = [
    { name: "Cái", abbreviation: "cái", isBaseUnit: true },
    { name: "Lon", abbreviation: "lon", isBaseUnit: true },
    { name: "Chai", abbreviation: "chai", isBaseUnit: true },
    { name: "Gói", abbreviation: "gói", isBaseUnit: true },
    { name: "Hộp", abbreviation: "hộp", isBaseUnit: true },
    { name: "Túi", abbreviation: "túi", isBaseUnit: true },
    { name: "Thùng", abbreviation: "thùng", isBaseUnit: false },
    { name: "Lốc", abbreviation: "lốc", isBaseUnit: false },
    { name: "Kg", abbreviation: "kg", isBaseUnit: true },
    { name: "Lít", abbreviation: "lít", isBaseUnit: true },
  ];

  for (const unit of units) {
    await prisma.unit.upsert({
      where: { abbreviation: unit.abbreviation },
      update: {},
      create: unit,
    });
  }
  console.log("Created units:", units.length);

  // Create products
  const categoryList = await prisma.category.findMany();
  const products = [
    // Mì ăn liền
    { name: "Mì Hảo Hảo tôm", sku: "MI001", importPrice: 4500, sellPrice: 6000, stock: 150, categorySlug: "mi-an-lien" },
    { name: "Mì Hảo Hảo beef", sku: "MI002", importPrice: 5000, sellPrice: 7000, stock: 120, categorySlug: "mi-an-lien" },
    { name: "Mì Omachi", sku: "MI003", importPrice: 7000, sellPrice: 10000, stock: 80, categorySlug: "mi-an-lien" },
    
    // Nước giải khát
    { name: "Nước ngọt Coca", sku: "NC001", importPrice: 12000, sellPrice: 15000, stock: 100, categorySlug: "nuoc-giai-khat" },
    { name: "Nước ngọt Pepsi", sku: "NC002", importPrice: 11000, sellPrice: 14000, stock: 90, categorySlug: "nuoc-giai-khat" },
    { name: "Nước suối Lavie", sku: "NS001", importPrice: 3000, sellPrice: 5000, stock: 200, categorySlug: "nuoc-giai-khat" },
    { name: "Nước suối Vinamilk", sku: "NS002", importPrice: 3500, sellPrice: 6000, stock: 150, categorySlug: "nuoc-giai-khat" },
    
    // Bánh kẹo
    { name: "Bánh Oreo", sku: "BO001", importPrice: 12000, sellPrice: 15000, stock: 75, categorySlug: "banh-keo" },
    { name: "Bánh KitKat", sku: "BK001", importPrice: 10000, sellPrice: 13000, stock: 60, categorySlug: "banh-keo" },
    { name: "Kẹo sữa", sku: "KS001", importPrice: 5000, sellPrice: 8000, stock: 100, categorySlug: "banh-keo" },
    
    // Sữa
    { name: "Sữa tươi Vinamilk 180ml", sku: "SV001", importPrice: 8000, sellPrice: 12000, stock: 80, categorySlug: "sua" },
    { name: "Sữa hộp Vinamilk", sku: "SV002", importPrice: 14000, sellPrice: 18000, stock: 50, categorySlug: "sua" },
    { name: "Sữa chua Vinamilk", sku: "SC001", importPrice: 6000, sellPrice: 9000, stock: 60, categorySlug: "sua" },
    
    // Bia
    { name: "Bia Tiger lon", sku: "BT001", importPrice: 18000, sellPrice: 25000, stock: 100, categorySlug: "bia" },
    { name: "Bia Heineken lon", sku: "BH001", importPrice: 22000, sellPrice: 30000, stock: 60, categorySlug: "bia" },
    
    // Cà phê
    { name: "Cà phê G7", sku: "CG001", importPrice: 3000, sellPrice: 5000, stock: 200, categorySlug: "ca-phe" },
    { name: "Cà phê wake-up", sku: "CW001", importPrice: 2500, sellPrice: 4000, stock: 150, categorySlug: "ca-phe" },
    
    // Dầu ăn
    { name: "Dầu ăn Meizan", sku: "DM001", importPrice: 30000, sellPrice: 38000, stock: 40, categorySlug: "dau-an" },
    { name: "Dầu ăn Simply", sku: "DS001", importPrice: 35000, sellPrice: 42000, stock: 35, categorySlug: "dau-an" },
    
    // Gạo
    { name: "Gạo ST25", sku: "GS001", importPrice: 25000, sellPrice: 35000, stock: 100, categorySlug: "gao" },
    { name: "Gạo Thơm Hương", sku: "GH001", importPrice: 18000, sellPrice: 25000, stock: 150, categorySlug: "gao" },
  ];

  for (const product of products) {
    const category = categoryList.find((c) => c.slug === product.categorySlug);
    if (category) {
      await prisma.product.upsert({
        where: { sku: product.sku },
        update: {},
        create: {
          name: product.name,
          slug: product.name.toLowerCase().replace(/\s+/g, "-"),
          sku: product.sku,
          importPrice: product.importPrice,
          sellPrice: product.sellPrice,
          stock: product.stock,
          categoryId: category.id,
          minStock: 10,
        },
      });
    }
  }
  console.log("Created products:", products.length);

  // Cấu hình đơn vị gốc và đơn vị bán lẻ cho một số sản phẩm mẫu.
  //
  // Ví dụ: Bia Tiger lon có 2 đơn vị:
  // - Lon: đơn vị bán lẻ cơ bản, 1 lon = 1 lon, đơn vị gốc khi nhập 1 thùng.
  // - Thùng: đơn vị gốc, 1 thùng = 24 lon.
  //
  // Tồn kho luôn lưu theo đơn vị bán lẻ cơ bản nên nhập 10 thùng sẽ tăng
  // 10 * 24 = 240 lon.
  const donViTheoSanPham = [
    {
      sku: "BT001",
      donVi: [
        { vietTat: "lon", conversionQty: 1, heSoGia: 1, laMacDinh: true, laDonViGoc: false },
        { vietTat: "thùng", conversionQty: 24, heSoGia: 24, laMacDinh: false, laDonViGoc: true },
      ],
    },
    {
      sku: "NC001",
      donVi: [
        { vietTat: "chai", conversionQty: 1, heSoGia: 1, laMacDinh: true, laDonViGoc: false },
        { vietTat: "thùng", conversionQty: 24, heSoGia: 24, laMacDinh: false, laDonViGoc: true },
      ],
    },
    {
      sku: "MI001",
      donVi: [
        { vietTat: "gói", conversionQty: 1, heSoGia: 1, laMacDinh: true, laDonViGoc: false },
        { vietTat: "thùng", conversionQty: 30, heSoGia: 30, laMacDinh: false, laDonViGoc: true },
      ],
    },
    {
      sku: "DM001",
      donVi: [
        { vietTat: "chai", conversionQty: 1, heSoGia: 1, laMacDinh: true, laDonViGoc: false },
        { vietTat: "lốc", conversionQty: 12, heSoGia: 12, laMacDinh: false, laDonViGoc: true },
      ],
    },
  ];

  const donViTinhTrongDb = await prisma.unit.findMany();

  for (const cauHinh of donViTheoSanPham) {
    const sanPham = await prisma.product.findUnique({
      where: { sku: cauHinh.sku },
    });
    if (!sanPham) continue;

    // Gia ban theo don vi ban le co ban la gia ban cua san pham.
    const giaBanCoBan = Number(sanPham.sellPrice);

    for (const donVi of cauHinh.donVi) {
      const donViTinh = donViTinhTrongDb.find(
        (u) => u.abbreviation === donVi.vietTat
      );
      if (!donViTinh) continue;

      await prisma.productUnit.upsert({
        where: {
          productId_unitId: {
            productId: sanPham.id,
            unitId: donViTinh.id,
          },
        },
        update: {
          conversionQty: donVi.conversionQty,
          isDefault: donVi.laMacDinh,
          isPurchase: donVi.laDonViGoc,
        },
        create: {
          productId: sanPham.id,
          unitId: donViTinh.id,
          conversionQty: donVi.conversionQty,
          price: Math.round(giaBanCoBan * donVi.heSoGia),
          isDefault: donVi.laMacDinh,
          isPurchase: donVi.laDonViGoc,
        },
      });
    }
  }

  console.log("Created product units for sample products:", donViTheoSanPham.length);

  // Moi san pham deu phai co it nhat mot don vi ban le mac dinh de tinh ton
  // kho. San pham chua duoc cau hinh o tren se duoc gan don vi co ban.
  const donViMacDinh = donViTinhTrongDb.find((u) => u.abbreviation === "cái")
    ?? donViTinhTrongDb.find((u) => u.isBaseUnit);

  if (donViMacDinh) {
    const sanPhamThieuDonVi = await prisma.product.findMany({
      where: { units: { none: {} } },
      select: { id: true, sku: true, name: true, sellPrice: true },
    });

    for (const sanPham of sanPhamThieuDonVi) {
      await prisma.productUnit.create({
        data: {
          productId: sanPham.id,
          unitId: donViMacDinh.id,
          conversionQty: 1,
          price: Number(sanPham.sellPrice),
          isDefault: true,
          // Chua co don vi goc rieng thi don vi mac dinh dong vai tro goc.
          isPurchase: true,
        },
      });
    }

    console.log(
      "Assigned default unit to products without units:",
      sanPhamThieuDonVi.length
    );
  }

  // Create settings
  const settings = [
    { key: "store_name", value: "Cửa hàng tiện lợi ABC", group: "general", isPublic: true },
    { key: "currency", value: "VND", group: "general", isPublic: true },
    { key: "ai_model", value: "gpt-4o", group: "ai", isPublic: false },
    { key: "low_stock_threshold", value: "10", group: "inventory", isPublic: false },
  ];

  for (const setting of settings) {
    await prisma.setting.upsert({
      where: { key: setting.key },
      update: {},
      create: setting,
    });
  }
  console.log("Created settings:", settings.length);

  console.log("Seeding finished!");
}

main()
  .catch((e) => {
    console.error(e);
    process.exit(1);
  })
  .finally(async () => {
    await prisma.$disconnect();
  });
