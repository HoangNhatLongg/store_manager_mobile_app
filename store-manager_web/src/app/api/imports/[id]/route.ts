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
    const invoice = await prisma.importInvoice.findUnique({
      where: { id },
      include: {
        supplier: true,
        createdBy: {
          select: { id: true, name: true, email: true },
        },
        items: {
          include: {
            product: {
              select: {
                id: true,
                name: true,
                sku: true,
                importPrice: true,
                previousImportPrice: true,
              },
            },
          },
        },
      },
    });

    if (!invoice) {
      return NextResponse.json(
        { error: "Không tìm thấy hóa đơn" },
        { status: 404 }
      );
    }

    return NextResponse.json(invoice);
  } catch (error) {
    console.error("Error fetching import invoice:", error);
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
    const { supplierId, notes, status } = body;

    const existingInvoice = await prisma.importInvoice.findUnique({
      where: { id },
      include: { items: true },
    });

    if (!existingInvoice) {
      return NextResponse.json(
        { error: "Không tìm thấy hóa đơn" },
        { status: 404 }
      );
    }

    // If changing status to CANCELLED, reverse the stock
    if (status === "CANCELLED" && existingInvoice.status !== "CANCELLED") {
      await prisma.$transaction(async (tx) => {
        // Reverse stock for each item
        for (const item of existingInvoice.items) {
          await tx.product.update({
            where: { id: item.productId },
            data: {
              stock: { decrement: item.quantity },
            },
          });
        }

        // Update invoice status
        await tx.importInvoice.update({
          where: { id },
          data: { status: "CANCELLED", notes },
        });
      });
    }

    const invoice = await prisma.importInvoice.update({
      where: { id },
      data: {
        supplierId: supplierId !== undefined ? supplierId : undefined,
        notes: notes !== undefined ? notes : undefined,
        status: status !== undefined ? status : undefined,
      },
      include: {
        supplier: true,
        items: {
          include: {
            product: true,
          },
        },
      },
    });

    return NextResponse.json(invoice);
  } catch (error) {
    console.error("Error updating import invoice:", error);
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

    const existingInvoice = await prisma.importInvoice.findUnique({
      where: { id },
      include: { items: true },
    });

    if (!existingInvoice) {
      return NextResponse.json(
        { error: "Không tìm thấy hóa đơn" },
        { status: 404 }
      );
    }

    // Only allow deletion of PENDING invoices
    if (existingInvoice.status !== "PENDING") {
      return NextResponse.json(
        { error: "Chỉ có thể xóa hóa đơn ở trạng thái chờ xử lý" },
        { status: 400 }
      );
    }

    await prisma.$transaction(async (tx) => {
      // Reverse stock for each item
      for (const item of existingInvoice.items) {
        await tx.product.update({
          where: { id: item.productId },
          data: {
            stock: { decrement: item.quantity },
          },
        });
      }

      // Delete invoice (items will be cascade deleted)
      await tx.importInvoice.delete({
        where: { id },
      });
    });

    return NextResponse.json({ message: "Đã xóa hóa đơn" });
  } catch (error) {
    console.error("Error deleting import invoice:", error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
}
