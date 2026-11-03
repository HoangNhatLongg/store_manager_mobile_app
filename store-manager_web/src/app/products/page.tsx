"use client";

import { useState, useEffect } from "react";
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
} from "@/components/ui/dialog";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Package,
  Plus,
  Search,
  Pencil,
  Trash2,
  ChevronLeft,
  ChevronRight,
  AlertTriangle,
  Loader2,
  Ruler,
  Eye,
  EyeOff,
} from "lucide-react";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  ChonDonViSanPham,
  type DonViForm,
  type DonViSanPham,
  type DonViTinh,
} from "@/components/chon-don-vi-san-pham";

interface Category {
  id: string;
  name: string;
  slug: string;
}

interface Product {
  id: string;
  name: string;
  sku: string;
  slug: string;
  importPrice: number;
  sellPrice: number;
  stock: number;
  minStock: number;
  description: string | null;
  image: string | null;
  category: Category;
  createdAt: string;
  isActive: boolean;
  units?: DonViSanPham[];
}

interface Pagination {
  page: number;
  limit: number;
  total: number;
  totalPages: number;
}

const formatCurrency = (value: number) => {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(value);
};

export default function ProductsPage() {
  const router = useRouter();
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [pagination, setPagination] = useState<Pagination>({
    page: 1,
    limit: 10,
    total: 0,
    totalPages: 0,
  });
  const [isLoading, setIsLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [categoryFilter, setCategoryFilter] = useState<string>("");
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [includeHidden, setIncludeHidden] = useState(true);
  const [donViTinh, setDonViTinh] = useState<DonViTinh[]>([]);
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
    stock: "",
    minStock: "10",
    description: "",
    image: "",
  });

  useEffect(() => {
    fetchProducts();
    fetchCategories();
  }, [pagination.page, search, categoryFilter]);

  const fetchProducts = async () => {
    setIsLoading(true);
    try {
      const params = new URLSearchParams({
        page: pagination.page.toString(),
        limit: pagination.limit.toString(),
        search,
      });
      if (categoryFilter) params.append("categoryId", categoryFilter);
      if (includeHidden) params.append("includeHidden", "true");

      const res = await fetch(`/api/products?${params}`);
      const data = await res.json();
      setProducts(data.products);
      setPagination(data.pagination);
    } catch (error) {
      console.error("Error fetching products:", error);
    } finally {
      setIsLoading(false);
    }
  };

  const toggleVisibility = async (product: Product) => {
    try {
      const res = await fetch(`/api/products/${product.id}`, {
        method: "PATCH",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ isActive: !product.isActive }),
      });
      if (!res.ok) {
        const error = await res.json();
        alert(error.error || "Không thể thay đổi trạng thái sản phẩm");
        return;
      }
      fetchProducts();
    } catch (error) {
      console.error("Error changing product visibility:", error);
      alert("Không thể thay đổi trạng thái sản phẩm");
    }
  };

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

  const handleSearch = (e: React.ChangeEvent<HTMLInputElement>) => {
    setSearch(e.target.value);
    setPagination((prev) => ({ ...prev, page: 1 }));
  };

  const handleCategoryFilter = (value: string) => {
    setCategoryFilter(value === "all" ? "" : value);
    setPagination((prev) => ({ ...prev, page: 1 }));
  };

  const openAddDialog = () => {
    setSelectedProduct(null);
    setFormData({
      name: "",
      sku: "",
      categoryId: "",
      importPrice: "",
      sellPrice: "",
      stock: "",
      minStock: "10",
      description: "",
      image: "",
    });
    setDonViForm({ defaultUnit: null, purchaseUnit: null });
    setIsDialogOpen(true);
  };

  const openEditDialog = (product: Product) => {
    setSelectedProduct(product);
    setFormData({
      name: product.name,
      sku: product.sku,
      categoryId: product.category.id,
      importPrice: product.importPrice.toString(),
      sellPrice: product.sellPrice.toString(),
      stock: product.stock.toString(),
      minStock: product.minStock.toString(),
      description: product.description || "",
      image: (product as any).imageUrl || "",
    });

    // Nap lai don vi dang gan cho san pham de sua duoc.
    const donViMacDinh = (product.units || []).find(
      (donVi) => donVi.isDefault
    );
    const donViGoc = (product.units || []).find((donVi) => donVi.isPurchase);

    setDonViForm({
      defaultUnit: donViMacDinh
        ? {
            unitId: donViMacDinh.unitId,
            price: parseFloat(donViMacDinh.price),
          }
        : null,
      purchaseUnit:
        donViGoc && donViMacDinh && donViGoc.unitId !== donViMacDinh.unitId
          ? {
              unitId: donViGoc.unitId,
              conversionQty: donViGoc.conversionQty,
              price: parseFloat(donViGoc.price),
            }
          : null,
    });
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();

    // San pham bat buoc phai co don vi ban le mac dinh de tinh ton kho.
    if (!donViForm.defaultUnit) {
      alert("Vui lòng chọn đơn vị bán lẻ mặc định cho sản phẩm");
      return;
    }

    setIsSubmitting(true);

    try {
      const url = selectedProduct
        ? `/api/products/${selectedProduct.id}`
        : "/api/products";
      const method = selectedProduct ? "PUT" : "POST";

      const res = await fetch(url, {
        method,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          ...formData,
          defaultUnit: donViForm.defaultUnit,
          purchaseUnit: donViForm.purchaseUnit,
        }),
      });

      if (!res.ok) {
        const error = await res.json();
        alert(error.error || "Có lỗi xảy ra");
        return;
      }

      setIsDialogOpen(false);
      fetchProducts();
    } catch (error) {
      console.error("Error submitting form:", error);
      alert("Có lỗi xảy ra");
    } finally {
      setIsSubmitting(false);
    }
  };

  const handleDelete = async () => {
    if (!selectedProduct) return;
    setIsSubmitting(true);

    try {
      const res = await fetch(`/api/products/${selectedProduct.id}`, {
        method: "DELETE",
      });

      if (!res.ok) {
        const error = await res.json();
        alert(error.error || "Có lỗi xảy ra");
        return;
      }

      setIsDeleteDialogOpen(false);
      setSelectedProduct(null);
      fetchProducts();
    } catch (error) {
      console.error("Error deleting product:", error);
      alert("Có lỗi xảy ra");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <MainLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h1 className="text-3xl font-semibold tracking-tight">Sản phẩm</h1>
            <p className="text-muted-foreground mt-1">
              Quản lý danh sách sản phẩm
            </p>
          </div>
          <Button onClick={openAddDialog} className="rounded-xl">
            <Plus className="mr-2 h-4 w-4" />
            Thêm sản phẩm
          </Button>
        </div>

        {/* Filters */}
        <div className="flex flex-col sm:flex-row gap-4">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder="Tìm kiếm sản phẩm..."
              value={search}
              onChange={handleSearch}
              className="pl-10 rounded-xl"
            />
          </div>
          <Select value={categoryFilter || "all"} onValueChange={handleCategoryFilter}>
            <SelectTrigger className="w-full sm:w-[200px] rounded-xl">
              <SelectValue placeholder="Danh mục" />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="all">Tất cả danh mục</SelectItem>
              {categories.map((cat) => (
                <SelectItem key={cat.id} value={cat.id}>
                  {cat.name}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        </div>

        {/* Table */}
        <div className="bg-card rounded-2xl overflow-hidden">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Sản phẩm</TableHead>
                <TableHead>SKU</TableHead>
                <TableHead>Danh mục</TableHead>
                <TableHead>Đơn vị</TableHead>
                <TableHead className="text-right">Giá nhập</TableHead>
                <TableHead className="text-right">Giá bán</TableHead>
                <TableHead className="text-right">Tồn kho</TableHead>
                <TableHead className="text-right">Thao tác</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading ? (
                <TableRow>
                  <TableCell colSpan={8} className="text-center py-8">
                    <Loader2 className="h-8 w-8 animate-spin mx-auto text-muted-foreground" />
                  </TableCell>
                </TableRow>
              ) : products.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={8} className="text-center py-8 text-muted-foreground">
                    Không có sản phẩm nào
                  </TableCell>
                </TableRow>
              ) : (
                products.map((product) => (
                   <TableRow key={product.id} className={!product.isActive ? "opacity-60" : undefined}>
                    <TableCell>
                      <div className="flex items-center gap-3">
                        {(product as any).imageUrl ? (
                          <img
                            src={(product as any).imageUrl}
                            alt={product.name}
                            className="h-10 w-10 rounded-xl object-cover"
                          />
                        ) : (
                          <div className="h-10 w-10 rounded-xl bg-muted flex items-center justify-center">
                            <Package className="h-5 w-5 text-muted-foreground" />
                          </div>
                        )}
                        <div>
                          <p className="font-medium">{product.name}</p>
                          {!product.isActive && (
                            <p className="text-xs text-muted-foreground">Đang ẩn khỏi màn hình bán hàng</p>
                          )}
                          {product.stock <= product.minStock && (
                            <p className="text-xs text-amber-600 flex items-center gap-1">
                              <AlertTriangle className="h-3 w-3" />
                              Sắp hết hàng
                            </p>
                          )}
                        </div>
                      </div>
                    </TableCell>
                    <TableCell className="font-mono text-sm">
                      {product.sku}
                    </TableCell>
                    <TableCell>{product.category?.name || "Không có"}</TableCell>
                    <TableCell>
                      {(() => {
                        const donViMacDinh = (product.units || []).find(
                          (donVi) => donVi.isDefault
                        );
                        const donViGoc = (product.units || []).find(
                          (donVi) => donVi.isPurchase
                        );

                        if (!donViMacDinh) {
                          return (
                            <span className="text-xs text-amber-600">
                              Chưa có đơn vị
                            </span>
                          );
                        }

                        return (
                          <div className="text-sm">
                            <span className="font-medium">
                              {donViMacDinh.unit.name}
                            </span>
                            {donViGoc &&
                              donViGoc.unitId !== donViMacDinh.unitId && (
                                <span className="block text-xs text-muted-foreground">
                                  Nhập: {donViGoc.unit.name} (1 ={" "}
                                  {donViGoc.conversionQty})
                                </span>
                              )}
                          </div>
                        );
                      })()}
                    </TableCell>
                    <TableCell className="text-right">
                      {formatCurrency(product.importPrice)}
                    </TableCell>
                    <TableCell className="text-right font-medium">
                      {formatCurrency(product.sellPrice)}
                    </TableCell>
                    <TableCell className="text-right">
                      <span
                        className={`px-2 py-1 rounded-lg text-sm ${
                          product.stock <= product.minStock
                            ? "bg-amber-100 text-amber-700 dark:bg-amber-900/50 dark:text-amber-400"
                            : "bg-emerald-100 text-emerald-700 dark:bg-emerald-900/50 dark:text-emerald-400"
                        }`}
                      >
                        {product.stock}
                      </span>
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex justify-end gap-2">
                        <Button
                          variant="ghost"
                          size="icon"
                          className="rounded-xl"
                          title="Quản lý đơn vị"
                          onClick={() => router.push(`/products/${product.id}/units`)}
                        >
                          <Ruler className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="rounded-xl"
                          title={product.isActive ? "Ẩn khỏi màn hình bán hàng" : "Hiện trên màn hình bán hàng"}
                          onClick={() => toggleVisibility(product)}
                        >
                          {product.isActive ? <EyeOff className="h-4 w-4" /> : <Eye className="h-4 w-4" />}
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="rounded-xl"
                          onClick={() => openEditDialog(product)}
                        >
                          <Pencil className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="rounded-xl text-destructive hover:text-destructive"
                          onClick={() => {
                            setSelectedProduct(product);
                            setIsDeleteDialogOpen(true);
                          }}
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

        {/* Pagination */}
        {pagination.totalPages > 1 && (
          <div className="flex items-center justify-between">
            <p className="text-sm text-muted-foreground">
              Hiển thị {(pagination.page - 1) * pagination.limit + 1} -{" "}
              {Math.min(pagination.page * pagination.limit, pagination.total)} của{" "}
              {pagination.total} sản phẩm
            </p>
            <div className="flex gap-2">
              <Button
                variant="outline"
                size="sm"
                className="rounded-xl"
                disabled={pagination.page === 1}
                onClick={() =>
                  setPagination((prev) => ({ ...prev, page: prev.page - 1 }))
                }
              >
                <ChevronLeft className="h-4 w-4" />
              </Button>
              <span className="px-4 py-2 text-sm">
                Trang {pagination.page} / {pagination.totalPages}
              </span>
              <Button
                variant="outline"
                size="sm"
                className="rounded-xl"
                disabled={pagination.page === pagination.totalPages}
                onClick={() =>
                  setPagination((prev) => ({ ...prev, page: prev.page + 1 }))
                }
              >
                <ChevronRight className="h-4 w-4" />
              </Button>
            </div>
          </div>
        )}
      </div>

      {/* Add/Edit Dialog */}
      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent className="max-w-2xl max-h-[90vh] overflow-y-auto rounded-2xl">
          <DialogHeader>
            <DialogTitle>
              {selectedProduct ? "Sửa sản phẩm" : "Thêm sản phẩm mới"}
            </DialogTitle>
          </DialogHeader>
          <form onSubmit={handleSubmit} className="space-y-4">
            <div className="grid gap-4 sm:grid-cols-2">
              <div className="space-y-2">
                <Label htmlFor="name">Tên sản phẩm *</Label>
                <Input
                  id="name"
                  value={formData.name}
                  onChange={(e) =>
                    setFormData({ ...formData, name: e.target.value })
                  }
                  required
                  className="rounded-xl"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="sku">SKU *</Label>
                <Input
                  id="sku"
                  value={formData.sku}
                  onChange={(e) =>
                    setFormData({ ...formData, sku: e.target.value })
                  }
                  required
                  className="rounded-xl"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="categoryId">Danh mục *</Label>
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
                <Label htmlFor="stock">Số lượng</Label>
                <Input
                  id="stock"
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
                <Label htmlFor="importPrice">Giá nhập (VNĐ) *</Label>
                <Input
                  id="importPrice"
                  type="number"
                  min="0"
                  value={formData.importPrice}
                  onChange={(e) =>
                    setFormData({ ...formData, importPrice: e.target.value })
                  }
                  required
                  className="rounded-xl"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="sellPrice">Giá bán (VNĐ) *</Label>
                <Input
                  id="sellPrice"
                  type="number"
                  min="0"
                  value={formData.sellPrice}
                  onChange={(e) =>
                    setFormData({ ...formData, sellPrice: e.target.value })
                  }
                  required
                  className="rounded-xl"
                />
              </div>
            <div className="space-y-2">
              <Label htmlFor="minStock">Tồn kho tối thiểu</Label>
              <Input
                id="minStock"
                type="number"
                min="0"
                value={formData.minStock}
                onChange={(e) =>
                  setFormData({ ...formData, minStock: e.target.value })
                }
                className="rounded-xl"
              />
            </div>
            <div className="sm:col-span-2">
              <ChonDonViSanPham
                donViTinh={donViTinh}
                donViHienTai={selectedProduct?.units || []}
                giaBan={parseFloat(formData.sellPrice) || 0}
                value={donViForm}
                onChange={setDonViForm}
              />
            </div>
            </div>
            <div className="space-y-2">
              <Label htmlFor="description">Mô tả</Label>
              <Input
                id="description"
                value={formData.description}
                onChange={(e) =>
                  setFormData({ ...formData, description: e.target.value })
                }
                className="rounded-xl"
              />
            </div>
            <div className="space-y-2">
              <Label htmlFor="image">URL Hình ảnh</Label>
              <Input
                id="image"
                type="url"
                value={formData.image}
                onChange={(e) =>
                  setFormData({ ...formData, image: e.target.value })
                }
                placeholder="https://example.com/image.jpg"
                className="rounded-xl"
              />
              {formData.image && (
                <div className="mt-2">
                  <img
                    src={formData.image}
                    alt="Preview"
                    className="h-24 w-24 object-cover rounded-xl border"
                    onError={(e) => {
                      (e.target as HTMLImageElement).style.display = 'none';
                    }}
                  />
                </div>
              )}
            </div>
            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={() => setIsDialogOpen(false)}
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
                ) : selectedProduct ? (
                  "Lưu thay đổi"
                ) : (
                  "Thêm sản phẩm"
                )}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <Dialog open={isDeleteDialogOpen} onOpenChange={setIsDeleteDialogOpen}>
        <DialogContent className="rounded-2xl">
          <DialogHeader>
            <DialogTitle>Xóa sản phẩm</DialogTitle>
          </DialogHeader>
          <p>
            Bạn có chắc chắn muốn xóa sản phẩm{" "}
            <strong>{selectedProduct?.name}</strong> không?
          </p>
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
