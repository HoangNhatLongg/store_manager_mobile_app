import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { getMobileUser, mobileUnauthorizedResponse } from "@/lib/mobile-auth";
import { mobileErrorResponse } from "@/lib/mobile-http";

/**
 * GET /api/v1/me
 *
 * Endpoint nhỏ dùnng để Adroid kiểm tra access token sau khi mở app.
 * Không dùng thông tin user trong token làm dữ liệu hiển thị cuối cùng vì
 * tên hoặc role có thể đã được cập nhật trên server.
 */
export async function GET(request: NextRequest) {
  const tokenUser = await getMobileUser(request);

  if (!tokenUser) {
    return mobileUnauthorizedResponse();
  }

  try {
    const user = await prisma.user.findUnique({
      where: { id: tokenUser.userId },
      select: {
        id: true,
        email: true,
        name: true,
        role: true,
      },
    });

    if (!user) {
      return mobileErrorResponse(
        "USER_NOT_FOUND",
        "Tài khoản không còn tồn tại",
        401
      );
    }

    return NextResponse.json({ user });
  } catch (error) {
    console.error("Mobile me error:", error);

    return mobileErrorResponse(
      "SERVER_ERROR",
      "Không thể tải thông tin tài khoản",
      500
    );
  }
}
