import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/auth";

export async function GET(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const session = await getServerSession(authOptions);
    if (!session) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const { id } = await params;

    const unit = await prisma.unit.findUnique({
      where: { id },
      include: {
        _count: {
          select: { products: true },
        },
      },
    });

    if (!unit) {
      return NextResponse.json(
        { error: "Không tìm thấy đơn vị" },
        { status: 404 }
      );
    }

    return NextResponse.json(unit);
  } catch (error) {
    console.error("Error fetching unit:", error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
}

export async function PUT(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const session = await getServerSession(authOptions);
    if (!session) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const { id } = await params;
    const body = await request.json();
    const { name, abbreviation, isBaseUnit } = body;

    if (!name || !name.trim()) {
      return NextResponse.json(
        { error: "Tên đơn vị là bắt buộc" },
        { status: 400 }
      );
    }

    const existingUnit = await prisma.unit.findUnique({
      where: { id },
    });

    if (!existingUnit) {
      return NextResponse.json(
        { error: "Không tìm thấy đơn vị" },
        { status: 404 }
      );
    }

    const unit = await prisma.unit.update({
      where: { id },
      data: {
        name: name.trim(),
        abbreviation: abbreviation?.trim().toLowerCase() || existingUnit.abbreviation,
        isBaseUnit: isBaseUnit ?? existingUnit.isBaseUnit,
      },
    });

    return NextResponse.json(unit);
  } catch (error) {
    console.error("Error updating unit:", error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
}

export async function DELETE(
  request: NextRequest,
  { params }: { params: Promise<{ id: string }> }
) {
  try {
    const session = await getServerSession(authOptions);
    if (!session) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const { id } = await params;

    const existingUnit = await prisma.unit.findUnique({
      where: { id },
      include: {
        _count: {
          select: { products: true },
        },
      },
    });

    if (!existingUnit) {
      return NextResponse.json(
        { error: "Không tìm thấy đơn vị" },
        { status: 404 }
      );
    }

    if (existingUnit._count.products > 0) {
      return NextResponse.json(
        { error: "Đơn vị đang được sử dụng, không thể xóa" },
        { status: 400 }
      );
    }

    await prisma.unit.delete({
      where: { id },
    });

    return NextResponse.json({ message: "Xóa đơn vị thành công" });
  } catch (error) {
    console.error("Error deleting unit:", error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
}
