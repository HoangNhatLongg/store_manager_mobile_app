import { NextRequest, NextResponse } from "next/server";
import { prisma } from "@/lib/prisma";
import type {
  PaymentMethod,
  Prisma,
  SyncOperation,
} from "@prisma/client";
import { getMobileUser, mobileUnauthorizedResponse } from "@/lib/mobile-auth";
import { mobileErrorResponse, decimalToString } from "@/lib/mobile-http";

type SaleItemInput = {
  productId?: unknown;
  quantity?: unknown;
  unitPrice?: unknown;
  conversionQty?: unknown;
};

/**
 * POST /api/v1/sales
 *
 * Tạo hóa đơn bán cho mobile hoặc xử lý một hóa đơn đã tạo offline.
 *
 * Điểm quan trọng:
 * - Toàn bộ việc kiểm tra tồn kho và ghi dữ liệu nằm trong transaction.
 * - operationId là bắt buộc để request có tính idempotent.
 * - Nếu mobile gửi lại cùng operationId, server trả kết quả cũ.
 */
export async function POST(request: NextRequest) {
  const user = await getMobileUser(request);
  let operationIdForError: string | null = null;

  if (!user) {
    return mobileUnauthorizedResponse();
  }

  try {
    const body = await request.json();
    const operationId = readRequiredString(body.operationId);
    operationIdForError = operationId;
    const customerId = readNullableString(body.customerId);
    const notes = readNullableString(body.notes);
    const paymentMethod = readPaymentMethod(body.paymentMethod);
    const discount = readNonNegativeNumber(body.discount ?? 0);
    const items = readItems(body.items);

    if (!operationId) {
      return mobileErrorResponse(
        "VALIDATION_ERROR",
        "operationId là bắt buộc",
        400
      );
    }

    if (!items || discount === null || paymentMethod === null) {
      return mobileErrorResponse(
        "VALIDATION_ERROR",
        "Danh sách sản phẩm không hợp lệ",
        400
      );
    }

    // Kiểm tra trước transaction để retry không tạo bản ghi mới.
    const existingOperation = await prisma.syncOperation.findUnique({
      where: { operationId },
    });

    if (existingOperation) {
      // Thao tac da thanh cong thi tra lai ket qua cu, khong tao hoa don moi.
      if (existingOperation.status === "SYNCED") {
        return responseFromExistingOperation(existingOperation);
      }

      // Thao tac truoc do da that bai (vi du thieu ton kho) va chua tao duoc
      // hoa don. Khi do phai xoa ban ghi de cho phep nguoi dung thu lai
      // voi cung operationId, vi du sau khi da nhap them ton kho.
      // Chi xoa ban ghi da that bai, giu nguyen ban ghi dang xu ly de khong
      // pha vỡ idempotency khi hai request cùng chạy song song.
      await prisma.syncOperation.deleteMany({
        where: { operationId, status: { in: ["FAILED", "CONFLICT"] } },
      });
    }

    const invoice = await prisma.$transaction(async (tx) => {
      // Hai request khác nhau có thể cùng qua bước findUnique bên ngoài.
      // Tạo record idempotency trước; unique constraint sẽ chặn request thứ hai.
      try {
        await tx.syncOperation.create({
          data: {
            operationId,
            entityType: "SALE_INVOICE",
            entityId: operationId,
            operationType: "CREATE",
            status: "PROCESSING",
            createdById: user.userId,
          },
        });
      } catch (error) {
        if (isUniqueConstraintError(error)) {
          throw new DuplicateOperationError();
        }
        throw error;
      }

      if (customerId) {
        const customer = await tx.customer.findUnique({
          where: { id: customerId },
          select: { id: true, isActive: true },
        });

        if (!customer || !customer.isActive) {
          throw new MobileSaleError(
            "CUSTOMER_NOT_FOUND",
            "Không tìm thấy khách hàng đang hoạt động",
            404
          );
        }
      }

      let subtotal = 0;
      const preparedItems = [];

      for (const item of items) {
        const product = await tx.product.findUnique({
          where: { id: item.productId },
          select: {
            id: true,
            name: true,
            stock: true,
            importPrice: true,
            isActive: true,
          },
        });

        if (!product || !product.isActive) {
          throw new MobileSaleError(
            "PRODUCT_NOT_FOUND",
            `Không tìm thấy sản phẩm ${item.productId}`,
            404
          );
        }

        // Ton kho trong database luong theo don vi co ban. Khi mobile ban theo
        // don vi quy doi (thung, chai...) thi so luong phai duoc quy doi ve
        // don vi co ban truoc khi so sanh va tru khoi ton kho.
        const soLuongDonViCoBan = item.quantity * item.conversionQty;

        if (product.stock < soLuongDonViCoBan) {
          throw new MobileSaleError(
            "STOCK_CONFLICT",
            `Sản phẩm "${product.name}" không đủ tồn kho`,
            409,
            {
              productId: product.id,
              requested: soLuongDonViCoBan,
              available: product.stock,
            }
          );
        }

        // Tong tien cua dong hang khong doi khi quy doi don vi.
        const totalPrice = item.quantity * item.unitPrice;
        subtotal += totalPrice;

        // Don gia duoc luu theo don vi co ban de bao dam loi nhuan tinh dung:
        // loi nhuan = tong tien - (gia nhap don vi co ban x so luong don vi co ban).
        const donGiaDonViCoBan =
          soLuongDonViCoBan > 0 ? totalPrice / soLuongDonViCoBan : item.unitPrice;

        preparedItems.push({
          productId: product.id,
          quantity: soLuongDonViCoBan,
          unitPrice: donGiaDonViCoBan,
          importPrice: product.importPrice,
          totalPrice,
        });

        // Dieu kien stock >= soLuongDonViCoBan giup tranh giam ton kho xuong am.
        const stockUpdate = await tx.product.updateMany({
          where: {
            id: product.id,
            isActive: true,
            stock: { gte: soLuongDonViCoBan },
          },
          data: {
            stock: { decrement: soLuongDonViCoBan },
          },
        });

        if (stockUpdate.count !== 1) {
          throw new MobileSaleError(
            "STOCK_CONFLICT",
            `Tồn kho sản phẩm "${product.name}" vừa thay đổi`,
            409,
            { productId: product.id }
          );
        }
      }

      if (discount > subtotal) {
        throw new MobileSaleError(
          "VALIDATION_ERROR",
          "Giảm giá không được lớn hơn tạm tính",
          400
        );
      }

      const invoice = await tx.saleInvoice.create({
        data: {
          invoiceNumber: await generateInvoiceNumber(tx),
          customerId,
          subtotal,
          discount,
          totalAmount: subtotal - discount,
          paymentMethod,
          notes,
          status: "COMPLETED",
          createdById: user.userId,
          items: {
            create: preparedItems,
          },
        },
        include: {
          customer: {
            select: { id: true, name: true, phone: true },
          },
          items: {
            include: {
              product: {
                select: { id: true, name: true, sku: true },
              },
            },
          },
        },
      });

      const result = serializeInvoice(invoice);

      await tx.syncOperation.update({
        where: { operationId },
        data: {
          entityId: invoice.id,
          status: "SYNCED",
          result,
        },
      });

      return result;
    });

    return NextResponse.json(
      {
        status: "SYNCED",
        invoice,
      },
      { status: 201 }
    );
  } catch (error) {
    if (error instanceof DuplicateOperationError) {
      const existingOperation = operationIdForError
        ? await prisma.syncOperation.findUnique({
            where: { operationId: operationIdForError },
          })
        : null;

      if (existingOperation) {
        return responseFromExistingOperation(existingOperation);
      }
    }

    if (error instanceof MobileSaleError) {
      // Lưu lỗi nghiệp vụ để client có thể hiển thị trạng thái conflict/failed.
      await markOperationFailed(operationIdForError, user.userId, error);

      return mobileErrorResponse(
        error.code,
        error.message,
        error.status,
        error.details
      );
    }

    console.error("Mobile sale error:", error);

    return mobileErrorResponse(
      "SERVER_ERROR",
      "Không thể tạo hóa đơn bán",
      500
    );
  }
}

async function generateInvoiceNumber(
  tx: Prisma.TransactionClient
) {
  const now = new Date();
  const date = [
    now.getFullYear(),
    String(now.getMonth() + 1).padStart(2, "0"),
    String(now.getDate()).padStart(2, "0"),
  ].join("");

  // Vòng lặp xử lý xác suất trùng số ngẫu nhiên bằng cách kiểm tra unique index.
  for (let attempt = 0; attempt < 5; attempt += 1) {
    const random = Math.floor(Math.random() * 1_000_000)
      .toString()
      .padStart(6, "0");
    const invoiceNumber = `BH-${date}-${random}`;

    const existing = await tx.saleInvoice.findUnique({
      where: { invoiceNumber },
      select: { id: true },
    });

    if (!existing) {
      return invoiceNumber;
    }
  }

  throw new Error("Không thể tạo số hóa đơn duy nhất");
}

function readItems(value: unknown) {
  if (!Array.isArray(value) || value.length === 0) {
    return null;
  }

  const items = [];

  for (const rawItem of value as SaleItemInput[]) {
    const productId = readRequiredString(rawItem.productId);
    const quantity = readPositiveInteger(rawItem.quantity);
    const unitPrice = readPositiveNumber(rawItem.unitPrice);
    // He quy doi mac dinh la 1, nghia la san pham duoc ban theo don vi co ban.
    // Khi ban theo don vi quy doi (thung, chai...) mobile gui them so lon hon 1.
    const conversionQty = readPositiveInteger(rawItem.conversionQty ?? 1) ?? 1;

    if (!productId || quantity === null || unitPrice === null) {
      return null;
    }

    items.push({ productId, quantity, unitPrice, conversionQty });
  }

  return items;
}

function readRequiredString(value: unknown) {
  if (typeof value !== "string") {
    return null;
  }

  const result = value.trim();
  return result || null;
}

function readNullableString(value: unknown) {
  if (value === null || value === undefined) {
    return null;
  }

  return readRequiredString(value);
}

function readPositiveInteger(value: unknown) {
  const number = typeof value === "number" ? value : Number(value);

  return Number.isInteger(number) && number > 0 ? number : null;
}

function readPositiveNumber(value: unknown) {
  const number = typeof value === "number" ? value : Number(value);

  return Number.isFinite(number) && number > 0 ? number : null;
}

function readNonNegativeNumber(value: unknown) {
  const number = typeof value === "number" ? value : Number(value);

  return Number.isFinite(number) && number >= 0 ? number : null;
}

function readPaymentMethod(value: unknown): PaymentMethod | null {
  // Không tự động đổi giá trị sai thành CASH vì mobile cần biết request của
  // mình không hợp lệ để sửa trước khi retry.
  const allowed: PaymentMethod[] = [
    "CASH",
    "BANK_TRANSFER",
    "CARD",
    "E_WALLET",
  ];

  return typeof value === "string" && allowed.includes(value as PaymentMethod)
    ? (value as PaymentMethod)
    : value === undefined || value === null
      ? "CASH"
      : null;
}

type SaleInvoiceWithRelations = Prisma.SaleInvoiceGetPayload<{
  include: {
    customer: {
      select: { id: true; name: true; phone: true };
    };
    items: {
      include: {
        product: {
          select: { id: true; name: true; sku: true };
        };
      };
    };
  };
}>;

function serializeInvoice(invoice: SaleInvoiceWithRelations) {
  return {
    ...invoice,
    createdAt: invoice.createdAt.toISOString(),
    updatedAt: invoice.updatedAt.toISOString(),
    subtotal: decimalToString(invoice.subtotal),
    discount: decimalToString(invoice.discount),
    totalAmount: decimalToString(invoice.totalAmount),
    items: invoice.items.map((item) => ({
      ...item,
      createdAt: item.createdAt.toISOString(),
      unitPrice: decimalToString(item.unitPrice),
      importPrice: decimalToString(item.importPrice),
      totalPrice: decimalToString(item.totalPrice),
    })),
  };
}

function responseFromExistingOperation(operation: SyncOperation) {
  if (operation.status === "SYNCED" && operation.result) {
    return NextResponse.json({
      status: "ALREADY_PROCESSED",
      invoice: operation.result,
    });
  }

  if (operation.status === "CONFLICT" || operation.status === "FAILED") {
    return mobileErrorResponse(
      operation.errorCode || "SALE_FAILED",
      operation.errorMessage || "Thao tác bán hàng đã thất bại",
      operation.status === "CONFLICT" ? 409 : 400
    );
  }

  return mobileErrorResponse(
    "OPERATION_IN_PROGRESS",
    "Thao tác này đang được xử lý, hãy thử lại sau",
    409
  );
}

async function markOperationFailed(
  operationId: string | null,
  userId: string,
  error: MobileSaleError
) {
  try {
    if (!operationId) {
      return;
    }

    await prisma.syncOperation.upsert({
      where: { operationId },
      create: {
        operationId,
        entityType: "SALE_INVOICE",
        entityId: operationId,
        operationType: "CREATE",
        status: error.code === "STOCK_CONFLICT" ? "CONFLICT" : "FAILED",
        errorCode: error.code,
        errorMessage: error.message,
        createdById: userId,
      },
      update: {
        status: error.code === "STOCK_CONFLICT" ? "CONFLICT" : "FAILED",
        errorCode: error.code,
        errorMessage: error.message,
      },
    });
  } catch (markError) {
    console.error("Unable to record mobile operation failure:", markError);
  }
}

function isUniqueConstraintError(error: unknown) {
  return (
    typeof error === "object" &&
    error !== null &&
    "code" in error &&
    error.code === "P2002"
  );
}

class DuplicateOperationError extends Error {}

class MobileSaleError extends Error {
  constructor(
    public readonly code: string,
    message: string,
    public readonly status: number,
    public readonly details?: unknown
  ) {
    super(message);
  }
}
