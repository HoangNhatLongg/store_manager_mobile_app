import { NextRequest } from "next/server";
import { verifyMobileAccessToken } from "@/lib/mobile-jwt";

/**
 * Lấy người dùng từ header:
 *
 * Authorization: Bearer <access-token>
 *
 * Trả về null thay vì ném lỗi để route có thể trả response 401 thống nhất.
 */
export async function getMobileUser(request: NextRequest) {
  const authorization = request.headers.get("authorization");

  if (!authorization?.startsWith("Bearer ")) {
    return null;
  }

  const token = authorization.slice("Bearer ".length).trim();

  if (!token) {
    return null;
  }

  try {
    return await verifyMobileAccessToken(token);
  } catch {
    return null;
  }
}

/**
 * Response 401 dùng chung cho các mobile endpoint được bảo vệ.
 */
export function mobileUnauthorizedResponse() {
  return Response.json(
    {
      code: "UNAUTHORIZED",
      message: "Access token không hợp lệ hoặc đã hết hạn",
    },
    { status: 401 }
  );
}
