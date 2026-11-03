import { SignJWT, jwtVerify, type JWTPayload } from "jose";

/**
 * Payload tối thiểu được nhúng trong access token của mobile.
 *
 * Access token chỉ chứa thông tin nhận diện người dùng, không chứa dữ liệu
 * nhạy cảm như password. Mobile dùng token này trong header Authorization.
 */
export type MobileTokenPayload = {
  userId: string;
  email: string;
  role: string;
};

/**
 * Payload của refresh token.
 *
 * Ngoài thông tin người dùng còn có tokenId là định danh duy nhất của token
 * trong DB, dùng để tra cứu và thu hồi token khi cần.
 */
export type MobileRefreshTokenPayload = MobileTokenPayload & {
  tokenId: string;
};

function getJwtSecret() {
  const value = process.env.MOBILE_JWT_SECRET || process.env.NEXTAUTH_SECRET;

  if (!value) {
    throw new Error(
      "MOBILE_JWT_SECRET hoặc NEXTAUTH_SECRET phải được cấu hình"
    );
  }

  return new TextEncoder().encode(value);
}

/**
 * Tạo access token có thời hạn ngắn cho mobile.
 *
 * Access token dùng để gọi các API nghiệp vụ. Token này hết hạn sau 15 phút
 * nên luôn cần refresh token đi kèm.
 */
export async function createMobileAccessToken(
  payload: MobileTokenPayload
) {
  return new SignJWT(payload)
    .setProtectedHeader({ alg: "HS256", typ: "JWT" })
    .setIssuedAt()
    .setExpirationTime("15m")
    .sign(getJwtSecret());
}

/**
 * Tạo refresh token có thời hạn dài hơn cho mobile.
 *
 * Refresh token sống 30 ngày. Mỗi lần dùng nó để lấy access token mới, token
 * cũ sẽ bị thu hồi và token mới được cấp, tránh việc dùng lại nhiều lần.
 *
 * @param payload Thông tin người dùng kèm tokenId đã lưu trong DB.
 * @param soNgay So ngay hạn của token.
 * @return Chuoi JWT refresh token.
 */
export async function createMobileRefreshToken(
  payload: MobileRefreshTokenPayload,
  soNgay = 30
) {
  return new SignJWT(payload)
    .setProtectedHeader({ alg: "HS256", typ: "JWT" })
    .setIssuedAt()
    .setExpirationTime(`${soNgay}d`)
    .sign(getJwtSecret());
}

/**
 * Kiểm tra chữ ký và thời hạn access token.
 * Hàm này ném lỗi nếu token không hợp lệ hoặc đã hết hạn.
 */
export async function verifyMobileAccessToken(token: string) {
  const { payload } = await jwtVerify(token, getJwtSecret());

  const userId = readStringClaim(payload, "userId");
  const email = readStringClaim(payload, "email");
  const role = readStringClaim(payload, "role");

  if (!userId || !email || !role) {
    throw new Error("Mobile token thiếu thông tin người dùng");
  }

  return { userId, email, role } satisfies MobileTokenPayload;
}

/**
 * Kiểm tra chữ ký và thời hạn refresh token.
 *
 * @param token Chuoi refresh token can kiem tra.
 * @return Payload gom thong tin nguoi dung va tokenId.
 */
export async function verifyMobileRefreshToken(token: string) {
  const { payload } = await jwtVerify(token, getJwtSecret());

  const userId = readStringClaim(payload, "userId");
  const email = readStringClaim(payload, "email");
  const role = readStringClaim(payload, "role");
  const tokenId = readStringClaim(payload, "tokenId");

  if (!userId || !email || !role || !tokenId) {
    throw new Error("Refresh token thiếu thông tin người dùng");
  }

  return { userId, email, role, tokenId } satisfies MobileRefreshTokenPayload;
}

function readStringClaim(payload: JWTPayload, key: string) {
  const value = payload[key];
  return typeof value === "string" ? value : null;
}
