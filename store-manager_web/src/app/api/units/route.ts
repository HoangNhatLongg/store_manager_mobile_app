import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/auth";

export async function GET() {
  try {
    const session = await getServerSession(authOptions);
    if (!session) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const units = await prisma.unit.findMany({
      orderBy: [
        { isBaseUnit: "desc" },
        { name: "asc" }
      ],
    });

    return NextResponse.json(units);
  } catch (error) {
    console.error("Error fetching units:", error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
}

export async function POST(request: NextRequest) {
  try {
    const session = await getServerSession(authOptions);
    if (!session) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const body = await request.json();
    const { name, abbreviation, isBaseUnit } = body;

    if (!name || !name.trim()) {
      return NextResponse.json(
        { error: "Tên đơn vị là bắt buộc" },
        { status: 400 }
      );
    }

    if (!abbreviation || !abbreviation.trim()) {
      return NextResponse.json(
        { error: "Viết tắt là bắt buộc" },
        { status: 400 }
      );
    }

    const unit = await prisma.unit.create({
      data: {
        name: name.trim(),
        abbreviation: abbreviation.trim().toLowerCase(),
        isBaseUnit: isBaseUnit ?? false,
      },
    });

    return NextResponse.json(unit, { status: 201 });
  } catch (error) {
    console.error("Error creating unit:", error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
}
