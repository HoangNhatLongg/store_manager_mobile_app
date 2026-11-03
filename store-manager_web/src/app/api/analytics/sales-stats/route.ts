import { NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/auth";

export async function GET() {
  try {
    const session = await getServerSession(authOptions);
    if (!session) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    // Get all sale invoices count by status
    const [completed, pending, cancelled] = await Promise.all([
      prisma.saleInvoice.count({ where: { status: "COMPLETED" } }),
      prisma.saleInvoice.count({ where: { status: "PENDING" } }),
      prisma.saleInvoice.count({ where: { status: "CANCELLED" } }),
    ]);

    // Get total count
    const total = await prisma.saleInvoice.count();

    // Get recent invoices
    const recentInvoices = await prisma.saleInvoice.findMany({
      take: 5,
      orderBy: { createdAt: "desc" },
      select: {
        id: true,
        invoiceNumber: true,
        status: true,
        totalAmount: true,
        createdAt: true,
      },
    });

    return NextResponse.json({
      summary: { total, completed, pending, cancelled },
      recentInvoices,
    });
  } catch (error) {
    console.error("Error:", error);
    return NextResponse.json({ error: "Internal server error" }, { status: 500 });
  }
}
