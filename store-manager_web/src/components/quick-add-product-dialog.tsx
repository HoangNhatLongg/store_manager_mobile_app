"use client";

import { useState, useEffect } from "react";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { QuickAddCategoryDialog } from "./quick-add-category-dialog";
import {
  ChonDonViSanPham,
  type DonViForm,
  type DonViTinh,
} from "@/components/chon-don-vi-san-pham";
import { Loader2, Plus } from "lucide-react";

interface Category {
  id: string;
  name: string;
  slug: string;
}

interface Product {
  id: string;
  name: string;
  sku: string;
  categoryId: string | null;
  importPrice: number;
  sellPrice: number;
  stock: number;
  minStock: number;
}

interface QuickAddProductDialogProps {
  open: boolean;
  onOpenChange: (open: boolean) => void;
  onSuccess: (product: Product) => void;
  defaultCategoryId?: string;
}

export function QuickAddProductDialog({
  open,
  onOpenChange,
  onSuccess,
  defaultCategoryId,
}: QuickAddProductDialogProps) {
  const [categories, setCategories] = useState<Category[]>([]);
  const [donViTinh, setDonViTinh] = useState<DonViTinh[]>([]);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [isCategoryDialogOpen, setIsCategoryDialogOpen] = useState(false);
  const [donViForm, setDonViForm] = useState<DonViForm>({
    defaultUnit: null,
    purchaseUnit: null,
  });
  const [formData, setFormData] = useState({
    name: "",
    sku: "",
    categoryId: "",
    importPrice: "",
    sellPrice: "",
    stock: "0",
    minStock: "10",
  });

  useEffect(() => {
    if (open) {
      fetchCategories();
      fetchDonViTinh();
      // Set default category if provided
      if (defaultCategoryId) {
        setFormData(prev => ({ ...prev, categoryId: defaultCategoryId }));
      }
    }
  }, [open, defaultCategoryId]);

  const fetchCategories = async () => {
    try {
      const res = await fetch("/api/categories");
      const data = await res.json();
      setCategories(data);
    } catch (error) {
      console.error("Error fetching categories:", error);
    }
  };

  /** Tai danh sach don vi tinh de chon don vi mac dinh va don vi goc. */
  const fetchDonViTinh = async () => {
    try {
      const res = await fetch("/api/units");
      const data = await res.json();
      setDonViTinh(Array.isArray(data) ? data : []);
    } catch (error) {
      console.error("Error fetching units:", error);
    }
  };

  const handleCategoryCreated = (category: Category) => {    setCategories([...categories, category]);
    setFormData({ ...formData, categoryId: category.id });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.name.trim() || !formData.sku.trim()) return;

    // San pham bat buoc phai co don vi ban le mac dinh de tinh ton kho.
    if (!donViForm.defaultUnit) {
      alert("Vui lòng chọn đơn vị bán lẻ mặc định cho sản phẩm");
      return;
    }

    setIsSubmitting(true);
    try {
      const res = await fetch("/api/products", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          name: formData.name,
          sku: formData.sku,
          categoryId: formData.categoryId || null,
          importPrice: parseFloat(formData.importPrice) || 0,
          sellPrice: parseFloat(formData.sellPrice) || 0,
          stock: parseInt(formData.stock) || 0,
          minStock: parseInt(formData.minStock) || 10,
          defaultUnit: donViForm.defaultUnit,
          purchaseUnit: donViForm.purchaseUnit,
        }),
      });

      if (!res.ok) {
        const error = await res.json();
        alert(error.error || "Có lỗi xảy ra");
        return;
      }

      const product = await res.json();
      onSuccess(product);
      setFormData({
        name: "",
        sku: "",
        categoryId: "",
        importPrice: "",
        sellPrice: "",
        stock: "0",
        minStock: "10",
      });
      setDonViForm({ defaultUnit: null, purchaseUnit: null });
      onOpenChange(false);
    } catch (error) {
      console.error("Error creating product:", error);
      alert("Có lỗi xảy ra");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <>
      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto rounded-2xl">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Plus className="h-5 w-5" />
              Thêm sản phẩm mới
            </DialogTitle>
          </DialogHeader>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="product-name">Tên sản phẩm *</Label>
                <Input
                  id="product-name"
                  value={formData.name}
                  onChange={(e) =>
                    setFormData({ ...formData, name: e.target.value })
                  }
                  required
                  className="rounded-xl"
                  placeholder="VD: Mì Hảo Hảo"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="product-sku">SKU *</Label>
                <Input
                  id="product-sku"
                  value={formData.sku}
                  onChange={(e) =>
                    setFormData({ ...formData, sku: e.target.value })
                  }
                  required
                  className="rounded-xl"
                  placeholder="VD: MIHH001"
                />
              </div>
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <Label htmlFor="product-category">Danh mục</Label>
                  <Button
                    type="button"
                    variant="ghost"
                    size="sm"
                    className="h-7 text-xs text-primary"
                    onClick={() => setIsCategoryDialogOpen(true)}
                  >
                    <Plus className="h-3 w-3 mr-1" />
                    Thêm mới
                  </Button>
                </div>
                <Select
                  value={formData.categoryId}
                  onValueChange={(value) =>
                    setFormData({ ...formData, categoryId: value })
                  }
                >
                  <SelectTrigger className="rounded-xl">
                    <SelectValue placeholder="Chọn danh mục" />
                  </SelectTrigger>
                  <SelectContent>
                    {categories.map((cat) => (
                      <SelectItem key={cat.id} value={cat.id}>
                        {cat.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label htmlFor="product-stock">Số lượng</Label>
                <Input
                  id="product-stock"
                  type="number"
                  min="0"
                  value={formData.stock}
                  onChange={(e) =>
                    setFormData({ ...formData, stock: e.target.value })
                  }
                  className="rounded-xl"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="product-import-price">Giá nhập</Label>
                <Input
                  id="product-import-price"
                  type="number"
                  min="0"
                  value={formData.importPrice}
                  onChange={(e) =>
                    setFormData({ ...formData, importPrice: e.target.value })
                  }
                  className="rounded-xl"
                  placeholder="0"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="product-sell-price">Giá bán</Label>
                <Input
                  id="product-sell-price"
                  type="number"
                  min="0"
                  value={formData.sellPrice}
                  onChange={(e) =>
                    setFormData({ ...formData, sellPrice: e.target.value })
                  }
                  className="rounded-xl"
                  placeholder="0"
                />
              </div>
              <div className="space-y-2 sm:col-span-2">
                <Label htmlFor="product-min-stock">Tồn kho tối thiểu</Label>
                <Input
                  id="product-min-stock"
                  type="number"
                  min="0"
                  value={formData.minStock}
                  onChange={(e) =>
                    setFormData({ ...formData, minStock: e.target.value })
                  }
                  className="rounded-xl w-full sm:w-1/2"
                />
              </div>
              <div className="sm:col-span-2">
                <ChonDonViSanPham
                  donViTinh={donViTinh}
                  giaBan={parseFloat(formData.sellPrice) || 0}
                  value={donViForm}
                  onChange={setDonViForm}
                />
              </div>
            </div>
            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => onOpenChange(false)}
                className="rounded-xl"
              >
                Hủy
              </Button>
              <Button type="submit" disabled={isSubmitting} className="rounded-xl">
                {isSubmitting ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Đang xử lý...
                  </>
                ) : (
                  "Thêm sản phẩm"
                )}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <QuickAddCategoryDialog
        open={isCategoryDialogOpen}
        onOpenChange={setIsCategoryDialogOpen}
        onSuccess={handleCategoryCreated}
      />
    </>
  );
}
