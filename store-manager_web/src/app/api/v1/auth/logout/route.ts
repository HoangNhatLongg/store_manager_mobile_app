import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import { verifyMobileRefreshToken } from "@/lib/mobile-jwt";
import { mobileErrorResponse } from "@/lib/mobile-http";

/**
 * POST /api/v1/auth/logout
 *
 * Thu hồi refresh token khi người dùng đăng xuất.
 *
 * Access token vẫn còn hiệu lực cho tới khi hết hạn vì JWT không thể thu hồi
 * được phía server. Nhưng refresh token bị thu hồi nên nếu app bị cài lại
 * mà chưa xóa dữ liệu thì cũng không thể lấy được access token mới.
 */
export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const refreshToken = String(body.refreshToken || "").trim();

    if (!refreshToken) {
      // Không có token vẫn coi như đăng xuất thành công.
      return NextResponse.json({ success: true });
    }

    try {
      const payload = await verifyMobileRefreshToken(refreshToken);

      await prisma.mobileRefreshToken.updateMany({
        where: { tokenId: payload.tokenId, revokedAt: null },
        data: { revokedAt: new Date() },
      });
    } catch {
      // Token sai chu ky hoac het han thi khong can thu hoi them.
    }

    return NextResponse.json({ success: true });
  } catch (error) {
    console.error("Mobile logout error:", error);

    return mobileErrorResponse(
      "SERVER_ERROR",
      "Không thể đăng xuất lúc này",
      500
    );
  }
}
