const API = "http://localhost:3000";

async function main() {
  const resDangNhap = await fetch(`${API}/api/v1/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: "staff@store.com", password: "staff123" }),
  });
  const { accessToken } = await resDangNhap.json();

  // Tao san pham test voi ton kho bang 1 de mo phong truong hop hai
  // nhan vien cung ban san pham cuoi cung.
  const resSanPham = await fetch(`${API}/api/v1/products?limit=50`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  const dataSanPham = await resSanPham.json();
  const sanPham = dataSanPham.data[0];

  // Chuyen ton kho ve 1 bang API cap nhat san pham.
  const resCapNhat = await fetch(`${API}/api/v1/products/${sanPham.id}`, {
    method: "PUT",
    headers: {
      "Content-Type": "application/json",
      Authorization: `Bearer ${accessToken}`,
    },
    body: JSON.stringify({
      name: sanPham.name,
      sku: sanPham.sku,
      importPrice: sanPham.importPrice,
      sellPrice: sanPham.sellPrice,
      stock: 1,
      minStock: sanPham.minStock,
    }),
  });
  console.log("CHUAN HOA TON KHO = 1:", resCapNhat.status);

  // Hai thiet bi cung gui don hang theo, dong thoi.
  const taoDon = (nhanDien) =>
    fetch(`${API}/api/v1/sales`, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${accessToken}`,
      },
      body: JSON.stringify({
        operationId: `race-${nhanDien}-${Date.now()}-${Math.random()}`,
        items: [
          {
            productId: sanPham.id,
            quantity: 1,
            unitPrice: 10000,
            conversionQty: 1,
          },
        ],
        paymentMethod: "CASH",
        discount: 0,
      }),
    }).then(async (r) => ({ status: r.status, body: await r.json() }));

  const [a, b] = await Promise.all([taoDon("A"), taoDon("B")]);

  console.log("THIET BI A:", a.status, a.body.status || a.body.code);
  console.log("THIET BI B:", b.status, b.body.status || b.body.code);

  // Kiem tra ton kho con lai sau khi hai don hang chay.
  const resSau = await fetch(`${API}/api/v1/products?limit=50`, {
    headers: { Authorization: `Bearer ${accessToken}` },
  });
  const dataSau = await resSau.json();
  const sanPhamSau = dataSau.data.find((p) => p.id === sanPham.id);
  console.log("TON KHO CON LAI:", sanPhamSau?.stock);
  console.log(
    "=> Ket qua dung:",
    (a.status === 201) !== (b.status === 201) ? "CO" : "SAI (ca hai deu thanh cong hoac ca hai deu that bai)"
  );
}

main();
