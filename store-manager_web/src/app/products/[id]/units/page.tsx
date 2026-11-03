"use client";

import { useState, useEffect, use } from "react";
import { useRouter } from "next/navigation";
import { MainLayout } from "@/components/layout";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogDescription,
} from "@/components/ui/dialog";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Switch } from "@/components/ui/switch";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  ArrowLeft,
  Plus,
  Pencil,
  Trash2,
  Loader2,
  Ruler,
  Package,
  Star,
  Check,
  AlertTriangle,
} from "lucide-react";

interface Unit {
  id: string;
  name: string;
  abbreviation: string;
  isBaseUnit: boolean;
}

interface ProductUnit {
  id: string;
  unitId: string;
  conversionQty: number;
  price: string;
  isDefault: boolean;
  isPurchase: boolean;
  unit: Unit;
}

interface Product {
  id: string;
  name: string;
  sku: string;
  importPrice: string;
  sellPrice: string;
}

interface ProductUnitForm {
  unitId: string;
  conversionQty: number;
  price: number;
  isDefault: boolean;
  isPurchase: boolean;
}

export default function ProductUnitsPage({ params }: { params: Promise<{ id: string }> }) {
  const { id } = use(params);
  const router = useRouter();
  
  const [product, setProduct] = useState<Product | null>(null);
  const [units, setUnits] = useState<Unit[]>([]);
  const [productUnits, setProductUnits] = useState<ProductUnit[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
  const [editingUnit, setEditingUnit] = useState<ProductUnit | null>(null);
  const [deletingUnit, setDeletingUnit] = useState<ProductUnit | null>(null);
  const [form, setForm] = useState<ProductUnitForm>({
    unitId: "",
    conversionQty: 1,
    price: 0,
    isDefault: false,
    isPurchase: false,
  });
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    fetchData();
  }, [id]);

  async function fetchData() {
    setIsLoading(true);
    try {
      const [productRes, unitsRes, productUnitsRes] = await Promise.all([
        fetch(`/api/products/${id}`),
        fetch("/api/units"),
        fetch(`/api/product-units?productId=${id}`),
      ]);

      if (productRes.ok) {
        const productData = await productRes.json();
        setProduct(productData);
      }

      const unitsData = await unitsRes.json();
      setUnits(Array.isArray(unitsData) ? unitsData : []);

      const puData = await productUnitsRes.json();
      setProductUnits(Array.isArray(puData) ? puData : []);
    } catch (error) {
      console.error("Error fetching data:", error);
    } finally {
      setIsLoading(false);
    }
  }

  function openCreateDialog() {
    setEditingUnit(null);
    setForm({
      unitId: "",
      conversionQty: 1,
      price: product ? parseFloat(product.sellPrice) : 0,
      isDefault: productUnits.length === 0,
      isPurchase: productUnits.length === 0,
    });
    setIsDialogOpen(true);
  }

  function openEditDialog(pu: ProductUnit) {
    setEditingUnit(pu);
    setForm({
      unitId: pu.unitId,
      conversionQty: pu.conversionQty,
      price: parseFloat(pu.price),
      isDefault: pu.isDefault,
      isPurchase: pu.isPurchase,
    });
    setIsDialogOpen(true);
  }

  function openDeleteDialog(pu: ProductUnit) {
    setDeletingUnit(pu);
    setIsDeleteDialogOpen(true);
  }

  /**
   * Đặt nhanh một đơn vị làm đơn vị bán lẻ mặc định.
   *
   * Đơn vị mặc định luôn có hệ số quy đổi bằng 1 vì tồn kho được đếm theo
   * đơn vị này.
   */
  async function datMacDinh(pu: ProductUnit) {
    setIsSubmitting(true);
    try {
      const res = await fetch("/api/product-units", {
        method: "PUT",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          id: pu.id,
          conversionQty: 1,
          isDefault: true,
        }),
      });

      if (!res.ok) {
        const error = await res.json();
        alert(error.error || "Có lỗi xảy ra");
        return;
      }

      fetchData();
    } catch (error) {
      console.error("Error setting default unit:", error);
      alert("Có lỗi xảy ra");
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleSubmit() {
    if (!form.unitId) {
      alert("Vui lòng chọn đơn vị");
      return;
    }
    if (form.price <= 0) {
      alert("Giá phải lớn hơn 0");
      return;
    }

    setIsSubmitting(true);
    try {
      const isEdit = !!editingUnit;
      const url = isEdit ? "/api/product-units" : "/api/product-units";
      const method = isEdit ? "PUT" : "POST";

      const body: any = isEdit
        ? {
            id: editingUnit.id,
            conversionQty: form.conversionQty,
            price: form.price,
            isDefault: form.isDefault,
            isPurchase: form.isPurchase,
          }
        : {
            productId: id,
            unitId: form.unitId,
            conversionQty: form.conversionQty,
            price: form.price,
            isDefault: form.isDefault,
            isPurchase: form.isPurchase,
          };

      const res = await fetch(url, {
        method,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(body),
      });

      if (!res.ok) {
        const error = await res.json();
        alert(error.error || "Có lỗi xảy ra");
        return;
      }

      setIsDialogOpen(false);
      fetchData();
    } catch (error) {
      console.error("Error:", error);
      alert("Có lỗi xảy ra");
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleDelete() {
    if (!deletingUnit) return;

    setIsSubmitting(true);
    try {
      const res = await fetch(`/api/product-units?id=${deletingUnit.id}`, {
        method: "DELETE",
      });

      if (!res.ok) {
        const error = await res.json();
        alert(error.error || "Có lỗi xảy ra");
        return;
      }

      setIsDeleteDialogOpen(false);
      setDeletingUnit(null);
      fetchData();
    } catch (error) {
      console.error("Error:", error);
      alert("Có lỗi xảy ra");
    } finally {
      setIsSubmitting(false);
    }
  }

  // Lấy các đơn vị đã được thêm vào sản phẩm
  const usedUnitIds = productUnits.map((pu) => pu.unitId);
  const availableUnits = units.filter((u) => !usedUnitIds.includes(u.id));

  const formatCurrency = (value: number) => {
    return new Intl.NumberFormat("vi-VN", {
      style: "currency",
      currency: "VND",
    }).format(value);
  };

  if (isLoading) {
    return (
      <MainLayout>
        <div className="flex items-center justify-center min-h-[400px]">
          <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
        </div>
      </MainLayout>
    );
  }

  if (!product) {
    return (
      <MainLayout>
        <div className="text-center py-12">
          <p className="text-muted-foreground">Không tìm thấy sản phẩm</p>
          <Button variant="outline" onClick={() => router.push("/products")} className="mt-4 rounded-xl">
            <ArrowLeft className="mr-2 h-4 w-4" />
            Quay lại
          </Button>
        </div>
      </MainLayout>
    );
  }

  return (
    <MainLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex items-center gap-4">
          <Button variant="ghost" size="icon" onClick={() => router.push("/products")} className="rounded-xl">
            <ArrowLeft className="h-5 w-5" />
          </Button>
          <div>
            <h1 className="text-3xl font-semibold tracking-tight">
              Đơn vị sản phẩm
            </h1>
            <p className="text-muted-foreground mt-1">
              {product.name} (SKU: {product.sku})
            </p>
          </div>
        </div>

        {/* Product Info */}
        <div className="bg-card rounded-2xl p-6">
          <div className="flex items-center gap-4">
            <div className="h-16 w-16 rounded-xl bg-primary/10 flex items-center justify-center">
              <Package className="h-8 w-8 text-primary" />
            </div>
            <div>
              <h2 className="text-xl font-semibold">{product.name}</h2>
              <div className="flex gap-6 mt-1 text-sm text-muted-foreground">
                <span>Giá nhập: <span className="font-medium text-foreground">{formatCurrency(parseFloat(product.importPrice))}</span></span>
                <span>Giá bán: <span className="font-medium text-foreground">{formatCurrency(parseFloat(product.sellPrice))}</span></span>
              </div>
            </div>
          </div>
        </div>

        {/* Units List */}
        <div className="bg-card rounded-2xl overflow-hidden">
          <div className="p-6 border-b flex items-center justify-between">
            <h2 className="text-lg font-medium">Danh sách đơn vị</h2>
            <Button onClick={openCreateDialog} className="rounded-xl" disabled={availableUnits.length === 0}>
              <Plus className="mr-2 h-4 w-4" />
              Thêm đơn vị
            </Button>
          </div>

          {/* Cảnh báo khi sản phẩm chưa có đơn vị bán lẻ mặc định. */}
          {!productUnits.some((pu) => pu.isDefault) && (
            <div className="m-6 p-4 bg-amber-50 dark:bg-amber-950/30 border border-amber-200 dark:border-amber-800 rounded-xl">
              <div className="flex items-start gap-3">
                <AlertTriangle className="h-5 w-5 text-amber-600 dark:text-amber-400 mt-0.5 shrink-0" />
                <div>
                  <p className="font-medium text-amber-900 dark:text-amber-100">
                    Sản phẩm chưa có đơn vị bán lẻ mặc định
                  </p>
                  <p className="text-sm text-amber-700 dark:text-amber-300 mt-1">
                    Tồn kho được đếm theo đơn vị bán lẻ mặc định. Hãy thêm một
                    đơn vị và bật "Đơn vị bán lẻ mặc định" để có thể bán hàng và
                    nhập hàng với sản phẩm này.
                  </p>
                </div>
              </div>
            </div>
          )}

          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Đơn vị</TableHead>
                <TableHead>Quy đổi</TableHead>
                <TableHead className="text-right">Giá</TableHead>
                <TableHead className="text-center">Vai trò</TableHead>
                <TableHead className="text-right">Thao tác</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {productUnits.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={5} className="text-center py-8 text-muted-foreground">
                    Chưa có đơn vị nào. Nhấn "Thêm đơn vị" để bắt đầu.
                  </TableCell>
                </TableRow>
              ) : (
                productUnits.map((pu) => (
                  <TableRow key={pu.id}>
                    <TableCell>
                      <div className="flex items-center gap-2">
                        <Ruler className="h-4 w-4 text-muted-foreground" />
                        <span className="font-medium">{pu.unit.name}</span>
                        <code className="text-xs bg-muted px-1.5 py-0.5 rounded">
                          {pu.unit.abbreviation}
                        </code>
                      </div>
                    </TableCell>
                    <TableCell>
                      {pu.unit.isBaseUnit ? (
                        <span className="text-muted-foreground">1 {pu.unit.abbreviation}</span>
                      ) : (
                        <span>1 {pu.unit.abbreviation} = {pu.conversionQty} {units.find(u => u.id === pu.unitId)?.abbreviation || ""}</span>
                      )}
                    </TableCell>
                    <TableCell className="text-right font-medium">
                      {formatCurrency(parseFloat(pu.price))}
                    </TableCell>
                    <TableCell className="text-center">
                      <div className="flex flex-col items-center gap-1">
                        {pu.isPurchase && (
                          <span className="inline-flex items-center gap-1 rounded-full bg-amber-100 px-2 py-0.5 text-xs font-medium text-amber-800">
                            <Package className="h-3 w-3" />
                            Đơn vị gốc
                          </span>
                        )}
                        {pu.isDefault && (
                          <span className="inline-flex items-center gap-1 rounded-full bg-primary/10 px-2 py-0.5 text-xs font-medium text-primary">
                            <Check className="h-3 w-3" />
                            Bán lẻ mặc định
                          </span>
                        )}
                        {!pu.isPurchase && !pu.isDefault && (
                          <span className="text-muted-foreground text-sm">-</span>
                        )}
                      </div>
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex justify-end gap-2">
                        {!pu.isDefault && (
                          <Button
                            variant="ghost"
                            size="icon"
                            className="rounded-xl"
                            title="Đặt làm đơn vị bán lẻ mặc định"
                            onClick={() => datMacDinh(pu)}
                          >
                            <Star className="h-4 w-4" />
                          </Button>
                        )}
                        <Button
                          variant="ghost"
                          size="icon"
                          className="rounded-xl"
                          onClick={() => openEditDialog(pu)}
                        >
                          <Pencil className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="rounded-xl text-destructive hover:text-destructive"
                          onClick={() => openDeleteDialog(pu)}
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </div>
      </div>

      {/* Create/Edit Dialog */}
      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent className="rounded-2xl">
          <DialogHeader>
            <DialogTitle>
              {editingUnit ? "Sửa đơn vị" : "Thêm đơn vị mới"}
            </DialogTitle>
            <DialogDescription>
              {editingUnit
                ? "Cập nhật thông tin đơn vị cho sản phẩm"
                : "Thêm đơn vị mới cho sản phẩm này"}
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4">
            {!editingUnit && (
              <div className="space-y-2">
                <Label htmlFor="unitId">
                  Đơn vị <span className="text-destructive">*</span>
                </Label>
                <Select value={form.unitId} onValueChange={(value) => setForm({ ...form, unitId: value })}>
                  <SelectTrigger className="rounded-xl">
                    <SelectValue placeholder="Chọn đơn vị" />
                  </SelectTrigger>
                  <SelectContent>
                    {availableUnits.map((unit) => (
                      <SelectItem key={unit.id} value={unit.id}>
                        {unit.name} ({unit.abbreviation})
                        {unit.isBaseUnit && " - Đơn vị cơ bản"}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            )}

            {!editingUnit && form.unitId && !units.find(u => u.id === form.unitId)?.isBaseUnit && (
              <div className="space-y-2">
                <Label htmlFor="conversionQty">
                  Số lượng quy đổi <span className="text-destructive">*</span>
                </Label>
                <Input
                  id="conversionQty"
                  type="number"
                  min="1"
                  value={form.conversionQty}
                  onChange={(e) => setForm({ ...form, conversionQty: parseInt(e.target.value) || 1 })}
                  className="rounded-xl"
                />
                <p className="text-sm text-muted-foreground">
                  VD: 1 Thùng = {form.conversionQty} Lon
                </p>
              </div>
            )}

            <div className="space-y-2">
              <Label htmlFor="price">
                Giá bán <span className="text-destructive">*</span>
              </Label>
              <Input
                id="price"
                type="number"
                min="0"
                value={form.price}
                onChange={(e) => setForm({ ...form, price: parseFloat(e.target.value) || 0 })}
                className="rounded-xl"
              />
            </div>

            <div className="flex items-center justify-between rounded-xl border p-4">
              <div className="space-y-0.5">
                <Label htmlFor="isDefault" className="cursor-pointer">
                  Đơn vị bán lẻ mặc định
                </Label>
                <p className="text-sm text-muted-foreground">
                  Đơn vị được chọn khi bán hàng
                </p>
              </div>
              <Switch
                id="isDefault"
                checked={form.isDefault}
                onCheckedChange={(checked) => setForm({ ...form, isDefault: checked })}
              />
            </div>

            <div className="flex items-center justify-between rounded-xl border p-4">
              <div className="space-y-0.5">
                <Label htmlFor="isPurchase" className="cursor-pointer">
                  Đơn vị gốc (nhập hàng)
                </Label>
                <p className="text-sm text-muted-foreground">
                  Đơn vị dùng khi nhập hàng, ví dụ thùng, lốc. Hệ thống tự quy
                  đổi sang đơn vị bán lẻ.
                </p>
              </div>
              <Switch
                id="isPurchase"
                checked={form.isPurchase}
                onCheckedChange={(checked) => setForm({ ...form, isPurchase: checked })}
              />
            </div>
          </div>

          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setIsDialogOpen(false)}
              className="rounded-xl"
            >
              Hủy
            </Button>
            <Button
              onClick={handleSubmit}
              disabled={isSubmitting}
              className="rounded-xl"
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Đang xử lý...
                </>
              ) : editingUnit ? (
                "Lưu thay đổi"
              ) : (
                "Thêm mới"
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <Dialog open={isDeleteDialogOpen} onOpenChange={setIsDeleteDialogOpen}>
        <DialogContent className="rounded-2xl">
          <DialogHeader>
            <DialogTitle>Xóa đơn vị</DialogTitle>
            <DialogDescription>
              Bạn có chắc chắn muốn xóa đơn vị "{deletingUnit?.unit.name}" khỏi sản phẩm này không?
            </DialogDescription>
          </DialogHeader>

          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setIsDeleteDialogOpen(false)}
              className="rounded-xl"
            >
              Hủy
            </Button>
            <Button
              variant="destructive"
              onClick={handleDelete}
              disabled={isSubmitting}
              className="rounded-xl"
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Đang xóa...
                </>
              ) : (
                "Xóa"
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </MainLayout>
  );
}
