"use client";

import { useState, useEffect } from "react";
import { useParams } from "next/navigation";
import { MainLayout } from "@/components/layout";
import { Button } from "@/components/ui/button";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogDescription,
} from "@/components/ui/dialog";
import {
  ArrowLeft,
  Package,
  User,
  Building2,
  History,
  Printer,
  AlertTriangle,
  Loader2,
} from "lucide-react";
import Link from "next/link";

interface ImportItem {
  id: string;
  quantity: number;
  unitPrice: string;
  previousUnitPrice?: string | null;
  totalPrice: string;
  product: {
    id: string;
    name: string;
    sku: string;
    importPrice: string;
    previousImportPrice?: string | null;
  };
}

interface ImportInvoice {
  id: string;
  invoiceNumber: string;
  totalAmount: string;
  status: string;
  notes?: string;
  createdAt: string;
  supplier?: {
    id: string;
    name: string;
    phone?: string;
    email?: string;
  };
  createdBy: {
    id: string;
    name: string;
    email: string;
  };
  items: ImportItem[];
}

const formatCurrency = (value: string | number) => {
  const numValue = typeof value === "string" ? parseFloat(value) : value;
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(numValue);
};

const formatDate = (dateString: string) => {
  return new Date(dateString).toLocaleDateString("vi-VN", {
    weekday: "long",
    day: "2-digit",
    month: "2-digit",
    year: "numeric",
    hour: "2-digit",
    minute: "2-digit",
  });
};

export default function ImportDetailPage() {
  const params = useParams();
  const [invoice, setInvoice] = useState<ImportInvoice | null>(null);
  const [isLoading, setIsLoading] = useState(true);
  const [isCancelOpen, setIsCancelOpen] = useState(false);
  const [isCancelling, setIsCancelling] = useState(false);

  useEffect(() => {
    fetchInvoice();
  }, [params.id]);

  const fetchInvoice = async () => {
    setIsLoading(true);
    try {
      const res = await fetch(`/api/imports/${params.id}`);
      if (!res.ok) {
        throw new Error("Failed to fetch");
      }
      const data = await res.json();
      setInvoice(data);
    } catch (error) {
      console.error("Error fetching invoice:", error);
    } finally {
      setIsLoading(false);
    }
  };

  const handleCancel = async () => {
    setIsCancelling(true);
    try {
      const res = await fetch(`/api/imports/${params.id}`, {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ status: "CANCELLED" }),
      });

      if (!res.ok) {
        const error = await res.json();
        alert(error.error || "Có lỗi xảy ra");
        return;
      }

      setIsCancelOpen(false);
      fetchInvoice();
    } catch (error) {
      console.error("Error cancelling invoice:", error);
      alert("Có lỗi xảy ra");
    } finally {
      setIsCancelling(false);
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case "COMPLETED":
        return "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/50 dark:text-emerald-400";
      case "PENDING":
        return "bg-amber-100 text-amber-700 dark:bg-amber-900/50 dark:text-amber-400";
      case "CANCELLED":
        return "bg-red-100 text-red-700 dark:bg-red-900/50 dark:text-red-400";
      default:
        return "bg-gray-100 text-gray-700";
    }
  };

  const getStatusText = (status: string) => {
    switch (status) {
      case "COMPLETED":
        return "Hoàn thành";
      case "PENDING":
        return "Chờ xử lý";
      case "CANCELLED":
        return "Đã hủy";
      default:
        return status;
    }
  };

  const totalQuantity = invoice?.items.reduce((sum, item) => sum + item.quantity, 0) || 0;

  if (isLoading) {
    return (
      <MainLayout>
        <div className="flex items-center justify-center min-h-[400px]">
          <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
        </div>
      </MainLayout>
    );
  }

  if (!invoice) {
    return (
      <MainLayout>
        <div className="text-center py-12">
          <h2 className="text-2xl font-semibold mb-2">Không tìm thấy phiếu nhập</h2>
          <Link href="/import/history">
            <Button className="rounded-xl">
              <ArrowLeft className="mr-2 h-4 w-4" />
              Quay lại lịch sử
            </Button>
          </Link>
        </div>
      </MainLayout>
    );
  }

  return (
    <MainLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div className="flex items-center gap-4">
            <Link href="/import/history">
              <Button variant="ghost" size="icon" className="rounded-xl">
                <ArrowLeft className="h-5 w-5" />
              </Button>
            </Link>
            <div>
              <div className="flex items-center gap-3">
                <h1 className="text-3xl font-semibold tracking-tight">
                  {invoice.invoiceNumber}
                </h1>
                <span
                  className={`px-3 py-1 rounded-full text-sm font-medium ${getStatusBadge(
                    invoice.status
                  )}`}
                >
                  {getStatusText(invoice.status)}
                </span>
              </div>
              <p className="text-muted-foreground mt-1">
                Ngày tạo: {formatDate(invoice.createdAt)}
              </p>
            </div>
          </div>
          <div className="flex gap-2">
            <Button variant="outline" className="rounded-xl">
              <Printer className="mr-2 h-4 w-4" />
              In phiếu
            </Button>
            {invoice.status !== "CANCELLED" && (
              <Button
                variant="outline"
                className="rounded-xl text-destructive border-destructive hover:bg-destructive/10"
                onClick={() => setIsCancelOpen(true)}
              >
                <AlertTriangle className="mr-2 h-4 w-4" />
                Hủy phiếu
              </Button>
            )}
          </div>
        </div>

        <div className="grid gap-6 lg:grid-cols-3">
          {/* Main Content */}
          <div className="lg:col-span-2 space-y-6">
            {/* Info Cards */}
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="bg-card rounded-2xl p-5">
                <div className="flex items-center gap-3 mb-3">
                  <div className="p-2 rounded-xl bg-blue-100 dark:bg-blue-900/50">
                    <Building2 className="h-5 w-5 text-blue-600 dark:text-blue-400" />
                  </div>
                  <span className="text-sm text-muted-foreground">Nhà cung cấp</span>
                </div>
                <p className="text-lg font-medium">
                  {invoice.supplier?.name || "Không có"}
                </p>
                {invoice.supplier?.phone && (
                  <p className="text-sm text-muted-foreground">
                    {invoice.supplier.phone}
                  </p>
                )}
              </div>
              <div className="bg-card rounded-2xl p-5">
                <div className="flex items-center gap-3 mb-3">
                  <div className="p-2 rounded-xl bg-violet-100 dark:bg-violet-900/50">
                    <User className="h-5 w-5 text-violet-600 dark:text-violet-400" />
                  </div>
                  <span className="text-sm text-muted-foreground">Người tạo</span>
                </div>
                <p className="text-lg font-medium">
                  {invoice.createdBy?.name || invoice.createdBy?.email}
                </p>
              </div>
            </div>

            {/* Notes */}
            {invoice.notes && (
              <div className="bg-card rounded-2xl p-5">
                <h3 className="font-medium mb-2">Ghi chú</h3>
                <p className="text-muted-foreground">{invoice.notes}</p>
              </div>
            )}

            {/* Items */}
            <div className="bg-card rounded-2xl overflow-hidden">
              <div className="p-5 border-b">
                <h3 className="text-lg font-medium">Danh sách sản phẩm</h3>
                <p className="text-sm text-muted-foreground">
                  {invoice.items.length} sản phẩm, {totalQuantity} đơn vị
                </p>
              </div>
              <div className="overflow-x-auto">
                <table className="w-full">
                  <thead className="bg-muted/50">
                    <tr>
                      <th className="text-left p-4 text-sm font-medium">Sản phẩm</th>
                      <th className="text-right p-4 text-sm font-medium">Số lượng</th>
                      <th className="text-right p-4 text-sm font-medium">Đơn giá</th>
                      <th className="text-right p-4 text-sm font-medium">Thành tiền</th>
                    </tr>
                  </thead>
                  <tbody>
                    {invoice.items.map((item) => {
                      const hasPriceChange =
                        item.previousUnitPrice &&
                        parseFloat(String(item.unitPrice)) !== parseFloat(String(item.previousUnitPrice));
                      return (
                        <tr key={item.id} className="border-t">
                          <td className="p-4">
                            <div className="flex items-center gap-3">
                              <div className="h-10 w-10 rounded-lg bg-muted flex items-center justify-center">
                                <Package className="h-5 w-5 text-muted-foreground" />
                              </div>
                              <div>
                                <p className="font-medium">{item.product.name}</p>
                                <p className="text-xs text-muted-foreground">
                                  SKU: {item.product.sku}
                                </p>
                                {hasPriceChange && (
                                  <p className="text-xs text-amber-600 flex items-center gap-1 mt-1">
                                    <AlertTriangle className="h-3 w-3" />
                                    Giá thay đổi
                                  </p>
                                )}
                              </div>
                            </div>
                          </td>
                          <td className="p-4 text-right font-medium">
                            {item.quantity}
                          </td>
                          <td className="p-4 text-right">
                            <p className="font-medium">
                              {formatCurrency(item.unitPrice)}
                            </p>
                            {hasPriceChange && (
                              <p className="text-xs text-muted-foreground line-through">
                                {formatCurrency(item.previousUnitPrice ?? 0)}
                              </p>
                            )}
                          </td>
                          <td className="p-4 text-right font-medium">
                            {formatCurrency(item.totalPrice)}
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          {/* Summary */}
          <div className="space-y-6">
            <div className="bg-card rounded-2xl p-6 space-y-6 sticky top-24">
              <h3 className="text-lg font-medium">Tổng quan</h3>
              <div className="space-y-4">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Số sản phẩm</span>
                  <span className="font-medium">{invoice.items.length}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Tổng số lượng</span>
                  <span className="font-medium">{totalQuantity}</span>
                </div>
                <div className="border-t pt-4">
                  <div className="flex justify-between items-center">
                    <span className="text-lg font-medium">Tổng tiền</span>
                    <span className="text-2xl font-bold text-primary">
                      {formatCurrency(invoice.totalAmount)}
                    </span>
                  </div>
                </div>
              </div>

              {invoice.status === "CANCELLED" && (
                <div className="p-4 bg-red-50 dark:bg-red-900/20 rounded-xl">
                  <p className="text-sm text-red-600 dark:text-red-400">
                    Phiếu nhập này đã bị hủy. Tồn kho đã được hoàn trả.
                  </p>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Cancel Confirmation Dialog */}
      <Dialog open={isCancelOpen} onOpenChange={setIsCancelOpen}>
        <DialogContent className="rounded-2xl">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <AlertTriangle className="h-5 w-5 text-destructive" />
              Hủy phiếu nhập
            </DialogTitle>
            <DialogDescription>
              Bạn có chắc chắn muốn hủy phiếu nhập này không? Tồn kho sẽ được hoàn trả.
            </DialogDescription>
          </DialogHeader>
          <div className="p-4 bg-amber-50 dark:bg-amber-900/20 rounded-xl">
            <p className="text-sm text-amber-700 dark:text-amber-400">
              Lưu ý: Hành động này sẽ hoàn trả {totalQuantity} đơn vị vào kho.
            </p>
          </div>
          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setIsCancelOpen(false)}
              className="rounded-xl"
            >
              Đóng
            </Button>
            <Button
              variant="destructive"
              onClick={handleCancel}
              disabled={isCancelling}
              className="rounded-xl"
            >
              {isCancelling ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Đang hủy...
                </>
              ) : (
                "Xác nhận hủy"
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </MainLayout>
  );
}
