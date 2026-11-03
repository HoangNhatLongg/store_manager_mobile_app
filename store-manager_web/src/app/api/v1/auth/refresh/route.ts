import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import {
  createMobileAccessToken,
  createMobileRefreshToken,
  verifyMobileRefreshToken,
} from "@/lib/mobile-jwt";
import { mobileErrorResponse } from "@/lib/mobile-http";

/**
 * POST /api/v1/auth/refresh
 *
 * Đổi refresh token lấy access token mới.
 *
 * Access token chỉ sống 15 phút nên khi người dùng mất mạng lâu, token hết
 * hạn và app bị đăng xuất oan. Endpoint này cho phép app lấy lại access token
 * mà không cần người dùng nhập lại mật khẩu.
 *
 * Mỗi lần refresh, token cũ bị thu hồi và token mới được cấp. Nhờ vậy nếu
 * refresh token bị lộ thì token đó cũng chỉ dùng được một lần.
 */
export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const refreshToken = String(body.refreshToken || "").trim();

    if (!refreshToken) {
      return mobileErrorResponse(
        "VALIDATION_ERROR",
        "Refresh token là bắt buộc",
        400
      );
    }

    // Chữ ký hoặc thời hạn không hợp lệ thì coi như token đã hết hiệu lực.
    let payload;
    try {
      payload = await verifyMobileRefreshToken(refreshToken);
    } catch {
      return mobileErrorResponse(
        "INVALID_REFRESH_TOKEN",
        "Refresh token không hợp lệ hoặc đã hết hạn",
        401
      );
    }

    // Tra cứu token trong DB để kiểm tra đã bị thu hồi hay chưa.
    const tokenDaLuu = await prisma.mobileRefreshToken.findUnique({
      where: { tokenId: payload.tokenId },
    });

    if (!tokenDaLuu || tokenDaLuu.revokedAt || tokenDaLuu.expiresAt < new Date()) {
      return mobileErrorResponse(
        "INVALID_REFRESH_TOKEN",
        "Refresh token không hợp lệ hoặc đã hết hạn",
        401
      );
    }

    // Token thuộc về user khác với token vừa xác thực thì không hợp lệ.
    if (tokenDaLuu.userId !== payload.userId) {
      return mobileErrorResponse(
        "INVALID_REFRESH_TOKEN",
        "Refresh token không hợp lệ hoặc đã hết hạn",
        401
      );
    }

    // Thu hồi token cũ và cấp token mới.
    await prisma.mobileRefreshToken.update({
      where: { tokenId: payload.tokenId },
      data: { revokedAt: new Date() },
    });

    const tokenIdMoi = crypto.randomUUID();
    const refreshTokenMoi = await createMobileRefreshToken({
      userId: payload.userId,
      email: payload.email,
      role: payload.role,
      tokenId: tokenIdMoi,
    });

    await prisma.mobileRefreshToken.create({
      data: {
        tokenId: tokenIdMoi,
        userId: payload.userId,
        expiresAt: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000),
      },
    });

    const accessTokenMoi = await createMobileAccessToken({
      userId: payload.userId,
      email: payload.email,
      role: payload.role,
    });

    return NextResponse.json({
      accessToken: accessTokenMoi,
      refreshToken: refreshTokenMoi,
      tokenType: "Bearer",
      expiresIn: 15 * 60,
    });
  } catch (error) {
    console.error("Mobile refresh error:", error);

    return mobileErrorResponse(
      "SERVER_ERROR",
      "Không thể làm mới phiên đăng nhập lúc này",
      500
    );
  }
}
