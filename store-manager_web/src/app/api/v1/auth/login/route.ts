import { NextRequest, NextResponse } from "next/server";
import bcrypt from "bcryptjs";
import { prisma } from "@/lib/prisma";
import {
  createMobileAccessToken,
  createMobileRefreshToken,
} from "@/lib/mobile-jwt";
import { mobileErrorResponse } from "@/lib/mobile-http";

/**
 * POST /api/v1/auth/login
 *
 * Endpoint đăng nhập dành cho Android/mobile.
 * Web vẫn tiếp tục dùng NextAuth tại /api/auth/[...nextauth].
 */
export async function POST(request: NextRequest) {
  try {
    const body = await request.json();
    const email = String(body.email || "").trim().toLowerCase();
    const password = String(body.password || "");

    if (!email || !password) {
      return mobileErrorResponse(
        "VALIDATION_ERROR",
        "Email và mật khẩu là bắt buộc",
        400
      );
    }

    const user = await prisma.user.findUnique({
      where: { email },
    });

    // Không nói rõ email có tồn tại hay không để tránh làm lộ thông tin tài khoản.
    if (!user || !(await bcrypt.compare(password, user.password))) {
      return mobileErrorResponse(
        "INVALID_CREDENTIALS",
        "Email hoặc mật khẩu không đúng",
        401
      );
    }

    const accessToken = await createMobileAccessToken({
      userId: user.id,
      email: user.email,
      role: user.role,
    });

    // Cấp refresh token để app có thể lấy lại access token khi hết hạn mà
    // không cần người dùng đăng nhập lại.
    const tokenId = crypto.randomUUID();
    const refreshToken = await createMobileRefreshToken({
      userId: user.id,
      email: user.email,
      role: user.role,
      tokenId,
    });

    await prisma.mobileRefreshToken.create({
      data: {
        tokenId,
        userId: user.id,
        expiresAt: new Date(Date.now() + 30 * 24 * 60 * 60 * 1000),
      },
    });

    return NextResponse.json({
      accessToken,
      refreshToken,
      tokenType: "Bearer",
      expiresIn: 15 * 60,
      user: {
        id: user.id,
        email: user.email,
        name: user.name,
        role: user.role,
      },
    });
  } catch (error) {
    console.error("Mobile login error:", error);

    return mobileErrorResponse(
      "SERVER_ERROR",
      "Không thể đăng nhập lúc này",
      500
    );
  }
}
