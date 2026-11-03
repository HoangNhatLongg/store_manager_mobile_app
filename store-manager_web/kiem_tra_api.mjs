const API = "http://localhost:3000";

async function main() {
  // 1. Dang nhap de lay access token.
  const resDangNhap = await fetch(`${API}/api/v1/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: "staff@store.com", password: "staff123" }),
  });
  const dataDangNhap = await resDangNhap.json();
  console.log("DANG NHAP:", resDangNhap.status, JSON.stringify(dataDangNhap).slice(0, 400));

  const token =
    dataDangNhap.accessToken || dataDangNhap.token || dataDangNhap.data?.accessToken;
  if (!token) {
    console.log("Khong lay duoc token");
    return;
  }

  // 2. Tai danh sach san pham de lay id va ton kho.
  const resSanPham = await fetch(`${API}/api/v1/products?limit=3`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  const dataSanPham = await resSanPham.json();
  const sanPham = dataSanPham.data?.[0];
  console.log("SAN PHAM:", resSanPham.status, JSON.stringify(sanPham).slice(0, 300));

  // 3. Tao hoa don - day la buoc bi loi truoc do.
  const operationId = "test-op-" + Date.now();
  const resTao = await fetch(`${API}/api/v1/sales`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({
      operationId,
      customerId: null,
      items: [
        {
          productId: sanPham.id,
          quantity: 1,
          unitPrice: Number(sanPham.sellPrice),
          conversionQty: 1,
        },
      ],
      paymentMethod: "CASH",
      discount: 0,
      notes: "Kiem tra loi sync_operations",
    }),
  });
  const dataTao = await resTao.json();
  console.log("TAO HOA DON:", resTao.status, JSON.stringify(dataTao).slice(0, 500));

  // 4. Giu nguyen operationId de kiem tra idempotency.
  const resLai = await fetch(`${API}/api/v1/sales`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({
      operationId,
      items: [
        {
          productId: sanPham.id,
          quantity: 1,
          unitPrice: Number(sanPham.sellPrice),
          conversionQty: 1,
        },
      ],
      paymentMethod: "CASH",
      discount: 0,
    }),
  });
  const dataLai = await resLai.json();
  console.log("GUI LAI CUNG operationId:", resLai.status, dataLai.status);

  // 5. Kiem tra xung dot ton kho: mua so luong vuot qua ton kho.
  const resXungDot = await fetch(`${API}/api/v1/sales`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${token}`,
    },
    body: JSON.stringify({
      operationId: "test-conflict-" + Date.now(),
      items: [
        {
          productId: sanPham.id,
          quantity: 999999,
          unitPrice: 1,
          conversionQty: 1,
        },
      ],
      paymentMethod: "CASH",
      discount: 0,
    }),
  });
  const dataXungDot = await resXungDot.json();
  console.log("XUNG DOT TON KHO:", resXungDot.status, JSON.stringify(dataXungDot));
}

main();
