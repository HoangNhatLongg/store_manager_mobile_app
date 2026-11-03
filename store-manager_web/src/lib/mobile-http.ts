import { NextResponse } from "next/server";

/**
 * Chuẩn hóa response lỗi để Android có thể xử lý bằng code thay vì phải
 * phụ thuộc vào nội dung message tiếng Việt.
 */
export function mobileErrorResponse(
  code: string,
  message: string,
  status: number,
  details?: unknown
) {
  return NextResponse.json(
    {
      code,
      message,
      ...(details === undefined ? {} : { details }),
    },
    { status }
  );
}

/**
 * Chuyển giá trị Decimal của Prisma sang chuỗi JSON ổn định.
 * Mobile nên parse chuỗi tiền tệ thành kiểu số nguyên phù hợp với app.
 */
export function decimalToString(value: unknown) {
  return value === null || value === undefined ? null : String(value);
}
