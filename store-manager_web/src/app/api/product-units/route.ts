import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/auth";

// GET /api/product-units - Lấy danh sách product-units (có thể filter theo productId)
export async function GET(request: NextRequest) {
  try {
    const session = await getServerSession(authOptions);
    if (!session) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const { searchParams } = new URL(request.url);
    const productId = searchParams.get("productId");

    if (!productId) {
      return NextResponse.json(
        { error: "productId là bắt buộc" },
        { status: 400 }
      );
    }

    const productUnits = await prisma.productUnit.findMany({
      where: { productId },
      include: {
        unit: true,
      },
      orderBy: [
        { isDefault: "desc" },
        { conversionQty: "asc" },
      ],
    });

    return NextResponse.json(productUnits);
  } catch (error) {
    console.error("Error fetching product units:", error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
}

// POST /api/product-units - Thêm đơn vị cho sản phẩm
export async function POST(request: NextRequest) {
  try {
    const session = await getServerSession(authOptions);
    if (!session) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const body = await request.json();
    const { productId, unitId, conversionQty, price, isDefault, isPurchase } = body;
    const soLuongQuyDoi = Number(conversionQty ?? 1);
    const gia = Number(price);

    if (!productId || !unitId || !Number.isInteger(soLuongQuyDoi) || soLuongQuyDoi < 1 || !Number.isFinite(gia) || gia <= 0) {
      return NextResponse.json(
        { error: "productId, unitId và price là bắt buộc" },
        { status: 400 }
      );
    }

    // Kiểm tra sản phẩm tồn tại
    const product = await prisma.product.findUnique({
      where: { id: productId },
    });
    if (!product) {
      return NextResponse.json(
        { error: "Không tìm thấy sản phẩm" },
        { status: 404 }
      );
    }

    // Kiểm tra đơn vị tồn tại
    const unit = await prisma.unit.findUnique({
      where: { id: unitId },
    });
    if (!unit) {
      return NextResponse.json(
        { error: "Không tìm thấy đơn vị" },
        { status: 404 }
      );
    }

    // Kiểm tra đã có chưa
    const existing = await prisma.productUnit.findUnique({
      where: {
        productId_unitId: {
          productId,
          unitId,
        },
      },
    });
    if (existing) {
      return NextResponse.json(
        { error: "Sản phẩm đã có đơn vị này" },
        { status: 400 }
      );
    }

    // Đơn vị bán lẻ mặc định luôn có hệ số quy đổi bằng 1 vì tồn kho
    // được đếm theo đơn vị này.
    const laMacDinh = Boolean(isDefault);
    const heSoQuyDoiMacDinh = laMacDinh ? 1 : soLuongQuyDoi;

    // Nếu là default, bỏ default của các đơn vị khác
    if (laMacDinh) {
      await prisma.productUnit.updateMany({
        where: { productId, isDefault: true },
        data: { isDefault: false },
      });
    }

    // Chỉ một đơn vị gốc được dùng để nhập hàng cho mỗi sản phẩm.
    if (isPurchase) {
      await prisma.productUnit.updateMany({
        where: { productId, isPurchase: true },
        data: { isPurchase: false },
      });
    }

    const productUnit = await prisma.productUnit.create({
      data: {
        productId,
        unitId,
        conversionQty: heSoQuyDoiMacDinh,
        price: gia,
        isDefault: laMacDinh,
        isPurchase: isPurchase || false,
      },
      include: {
        unit: true,
      },
    });

    return NextResponse.json(productUnit, { status: 201 });
  } catch (error) {
    console.error("Error creating product unit:", error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
}

// PUT /api/product-units - Cập nhật product-unit
export async function PUT(request: NextRequest) {
  try {
    const session = await getServerSession(authOptions);
    if (!session) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const body = await request.json();
    const { id, conversionQty, price, isDefault, isPurchase } = body;

    if (!id) {
      return NextResponse.json(
        { error: "id là bắt buộc" },
        { status: 400 }
      );
    }

    const existing = await prisma.productUnit.findUnique({
      where: { id },
    });
    if (!existing) {
      return NextResponse.json(
        { error: "Không tìm thấy product-unit" },
        { status: 404 }
      );
    }

    if (conversionQty !== undefined && (!Number.isInteger(Number(conversionQty)) || Number(conversionQty) < 1)) {
      return NextResponse.json({ error: "Số lượng quy đổi phải là số nguyên lớn hơn 0" }, { status: 400 });
    }
    if (price !== undefined && (!Number.isFinite(Number(price)) || Number(price) <= 0)) {
      return NextResponse.json({ error: "Giá theo đơn vị phải lớn hơn 0" }, { status: 400 });
    }

    // Đơn vị bán lẻ mặc định luôn có hệ số quy đổi bằng 1.
    const laMacDinh = isDefault !== undefined ? Boolean(isDefault) : existing.isDefault;
    const heSoQuyDoi = laMacDinh
      ? 1
      : conversionQty !== undefined
        ? Number(conversionQty)
        : existing.conversionQty;

    // Khong cho bo co "ban le mac dinh" khi day la don vi mac dinh duy nhat
    // cua san pham, vi ton kho duoc dem theo don vi nay.
    if (existing.isDefault && !laMacDinh) {
      const soMacDinhKhac = await prisma.productUnit.count({
        where: {
          productId: existing.productId,
          isDefault: true,
          id: { not: existing.id },
        },
      });

      if (soMacDinhKhac === 0) {
        return NextResponse.json(
          {
            error:
              "Không thể bỏ đơn vị bán lẻ mặc định duy nhất của sản phẩm. Hãy đặt một đơn vị khác làm mặc định trước.",
          },
          { status: 409 }
        );
      }
    }

    // Nếu là default, bỏ default của các đơn vị khác
    if (laMacDinh && !existing.isDefault) {
      await prisma.productUnit.updateMany({
        where: { productId: existing.productId, isDefault: true },
        data: { isDefault: false },
      });
    }

    // Chỉ một đơn vị gốc được dùng để nhập hàng cho mỗi sản phẩm.
    if (isPurchase && !existing.isPurchase) {
      await prisma.productUnit.updateMany({
        where: { productId: existing.productId, isPurchase: true },
        data: { isPurchase: false },
      });
    }

    const productUnit = await prisma.productUnit.update({
      where: { id },
      data: {
        conversionQty: heSoQuyDoi,
        price: price !== undefined ? parseFloat(price) : existing.price,
        isDefault: laMacDinh,
        isPurchase: isPurchase !== undefined ? isPurchase : existing.isPurchase,
      },
      include: {
        unit: true,
      },
    });

    return NextResponse.json(productUnit);
  } catch (error) {
    console.error("Error updating product unit:", error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
}

// DELETE /api/product-units - Xóa product-unit
export async function DELETE(request: NextRequest) {
  try {
    const session = await getServerSession(authOptions);
    if (!session) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const { searchParams } = new URL(request.url);
    const id = searchParams.get("id");

    if (!id) {
      return NextResponse.json(
        { error: "id là bắt buộc" },
        { status: 400 }
      );
    }

    const existing = await prisma.productUnit.findUnique({
      where: { id },
    });
    if (!existing) {
      return NextResponse.json(
        { error: "Không tìm thấy product-unit" },
        { status: 404 }
      );
    }

    // Khong cho xoa don vi ban le mac dinh cuoi cung cua san pham, vi ton kho
    // duoc dem theo don vi nay.
    if (existing.isDefault) {
      const soDonViKhac = await prisma.productUnit.count({
        where: { productId: existing.productId, id: { not: existing.id } },
      });

      if (soDonViKhac === 0) {
        return NextResponse.json(
          {
            error:
              "Không thể xóa đơn vị bán lẻ mặc định duy nhất của sản phẩm. Hãy thêm đơn vị khác trước.",
          },
          { status: 409 }
        );
      }
    }

    await prisma.productUnit.delete({
      where: { id },
    });

    return NextResponse.json({ message: "Xóa thành công" });
  } catch (error) {
    console.error("Error deleting product unit:", error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
}
