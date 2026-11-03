"use client";

import { useState, useEffect } from "react";
import { MainLayout } from "@/components/layout";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Package,
  AlertTriangle,
  TrendingUp,
  TrendingDown,
  Search,
  Download,
  Loader2,
  ChevronLeft,
  ChevronRight,
  RefreshCw,
} from "lucide-react";
import Link from "next/link";

interface Category {
  id: string;
  name: string;
  slug: string;
}

interface Product {
  id: string;
  name: string;
  sku: string;
  stock: number;
  minStock: number;
  sellPrice: number;
  importPrice: number;
  category: Category;
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
    minimumFractionDigits: 0,
  }).format(value);
};

export default function InventoryPage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [pagination, setPagination] = useState<Pagination>({
    page: 1,
    limit: 20,
    total: 0,
    totalPages: 0,
  });
  const [isLoading, setIsLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const [filterStatus, setFilterStatus] = useState("all");
  const [categoryFilter, setCategoryFilter] = useState("all");
  const [categories, setCategories] = useState<Category[]>([]);

  // Load data
  useEffect(() => {
    const loadData = async () => {
      // Load categories
      try {
        const catRes = await fetch("/api/categories");
        if (catRes.ok) {
          const catData = await catRes.json();
          setCategories(Array.isArray(catData) ? catData : []);
        }
      } catch (e) {
        console.error("Error loading categories:", e);
      }
    };
    loadData();
  }, []);

  // Load products with debounce
  useEffect(() => {
    const timeoutId = setTimeout(async () => {
      setIsLoading(true);
      try {
        const params = new URLSearchParams({
          page: pagination.page.toString(),
          limit: pagination.limit.toString(),
          search: searchTerm,
        });
        const res = await fetch(`/api/products?${params}`);
        if (res.ok) {
          const data = await res.json();
          setProducts(data.products || []);
          setPagination((prev) => ({
            ...prev,
            total: data.pagination?.total || 0,
            totalPages: data.pagination?.totalPages || 0,
          }));
        }
      } catch (e) {
        console.error("Error loading products:", e);
      } finally {
        setIsLoading(false);
      }
    }, 300);
    return () => clearTimeout(timeoutId);
  }, [pagination.page, pagination.limit, searchTerm]);

  const filteredProducts = products.filter((product) => {
    if (categoryFilter !== "all" && product.category?.id !== categoryFilter) {
      return false;
    }
    if (filterStatus === "low" && product.stock > product.minStock) {
      return false;
    }
    if (filterStatus === "normal" && product.stock <= product.minStock) {
      return false;
    }
    return true;
  });

  const totalProducts = products.length;
  const lowStockCount = products.filter((p) => p.stock <= p.minStock && p.stock > 0).length;
  const outOfStockCount = products.filter((p) => p.stock === 0).length;

  const stats = [
    {
      title: "Tổng sản phẩm",
      value: pagination.total.toString(),
      icon: Package,
      bgColor: "bg-blue-50 dark:bg-blue-950/50",
      iconColor: "text-blue-600 dark:text-blue-400",
    },
    {
      title: "Sắp hết hàng",
      value: lowStockCount.toString(),
      icon: AlertTriangle,
      bgColor: "bg-amber-50 dark:bg-amber-950/50",
      iconColor: "text-amber-600 dark:text-amber-400",
    },
    {
      title: "Hết hàng",
      value: outOfStockCount.toString(),
      icon: TrendingDown,
      bgColor: "bg-red-50 dark:bg-red-950/50",
      iconColor: "text-red-600 dark:text-red-400",
    },
    {
      title: "Còn hàng",
      value: (totalProducts - lowStockCount - outOfStockCount).toString(),
      icon: TrendingUp,
      bgColor: "bg-emerald-50 dark:bg-emerald-950/50",
      iconColor: "text-emerald-600 dark:text-emerald-400",
    },
  ];

  const handleRefresh = () => {
    setPagination((prev) => ({ ...prev, page: 1 }));
    setSearchTerm("");
    setFilterStatus("all");
    setCategoryFilter("all");
  };

  return (
    <MainLayout>
      <div className="space-y-8">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h1 className="text-3xl font-semibold tracking-tight">Tồn kho</h1>
            <p className="text-muted-foreground mt-1">
              Theo dõi và quản lý hàng tồn kho
            </p>
          </div>
          <div className="flex gap-2">
            <Button variant="outline" onClick={handleRefresh} className="rounded-xl">
              <RefreshCw className="mr-2 h-4 w-4" />
              Làm mới
            </Button>
            <Button variant="outline" className="rounded-full">
              <Download className="mr-2 h-4 w-4" />
              Xuất báo cáo
            </Button>
          </div>
        </div>

        {/* Stats Cards */}
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          {stats.map((stat, index) => (
            <div
              key={index}
              className="bg-card rounded-2xl p-5 hover:shadow-md transition-shadow"
            >
              <div className="flex items-center gap-4">
                <div className={`p-3 rounded-xl ${stat.bgColor}`}>
                  <stat.icon className={`h-6 w-6 ${stat.iconColor}`} />
                </div>
                <div>
                  <p className="text-sm text-muted-foreground">{stat.title}</p>
                  <p className="text-2xl font-semibold">{stat.value}</p>
                </div>
              </div>
            </div>
          ))}
        </div>

        {/* Search & Filter */}
        <div className="flex flex-col sm:flex-row gap-3">
          <div className="relative flex-1">
            <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
            <Input
              placeholder="Tìm kiếm sản phẩm..."
              className="pl-10 bg-muted/50 rounded-xl border-0"
              value={searchTerm}
              onChange={(e) => {
                setSearchTerm(e.target.value);
                setPagination((prev) => ({ ...prev, page: 1 }));
              }}
            />
          </div>
          <Select
            value={categoryFilter}
            onValueChange={(value) => {
              setCategoryFilter(value);
              setPagination((prev) => ({ ...prev, page: 1 }));
            }}
          >
            <SelectTrigger className="w-[180px] rounded-xl">
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
          <div className="flex gap-2">
            <Button
              variant={filterStatus === "all" ? "default" : "outline"}
              onClick={() => setFilterStatus("all")}
              className="rounded-xl"
            >
              Tất cả
            </Button>
            <Button
              variant={filterStatus === "normal" ? "default" : "outline"}
              onClick={() => setFilterStatus("normal")}
              className="rounded-xl"
            >
              Còn hàng
            </Button>
            <Button
              variant={filterStatus === "low" ? "default" : "outline"}
              onClick={() => setFilterStatus("low")}
              className="rounded-xl"
            >
              Sắp hết
            </Button>
          </div>
        </div>

        {/* Inventory Table */}
        <div className="bg-card rounded-2xl overflow-hidden">
          <Table>
            <TableHeader>
              <TableRow className="bg-muted/30 hover:bg-muted/30">
                <TableHead className="w-[100px]">SKU</TableHead>
                <TableHead>Tên sản phẩm</TableHead>
                <TableHead>Danh mục</TableHead>
                <TableHead className="text-right">Giá nhập</TableHead>
                <TableHead className="text-right">Giá bán</TableHead>
                <TableHead className="text-center">Tồn kho</TableHead>
                <TableHead className="text-center">Tối thiểu</TableHead>
                <TableHead className="text-center">Trạng thái</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading ? (
                <TableRow>
                  <TableCell colSpan={8} className="text-center py-8">
                    <Loader2 className="h-6 w-6 animate-spin mx-auto text-muted-foreground" />
                  </TableCell>
                </TableRow>
              ) : filteredProducts.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={8} className="text-center py-8 text-muted-foreground">
                    Không có sản phẩm nào
                  </TableCell>
                </TableRow>
              ) : (
                filteredProducts.map((product) => {
                  const isLowStock = product.stock <= product.minStock && product.stock > 0;
                  const isOutOfStock = product.stock === 0;

                  return (
                    <TableRow key={product.id}>
                      <TableCell className="font-medium">{product.sku}</TableCell>
                      <TableCell>
                        <Link
                          href={`/products/${product.id}`}
                          className="font-medium hover:text-primary"
                        >
                          {product.name}
                        </Link>
                      </TableCell>
                      <TableCell>
                        <Badge variant="secondary">
                          {product.category?.name || "Không có"}
                        </Badge>
                      </TableCell>
                      <TableCell className="text-right text-muted-foreground">
                        {formatCurrency(product.importPrice)}
                      </TableCell>
                      <TableCell className="text-right font-medium text-primary">
                        {formatCurrency(product.sellPrice)}
                      </TableCell>
                      <TableCell className="text-center">
                        <span
                          className={`font-medium ${
                            isOutOfStock
                              ? "text-red-600"
                              : isLowStock
                              ? "text-amber-600"
                              : ""
                          }`}
                        >
                          {product.stock}
                        </span>
                      </TableCell>
                      <TableCell className="text-center text-muted-foreground">
                        {product.minStock}
                      </TableCell>
                      <TableCell className="text-center">
                        {isOutOfStock ? (
                          <Badge variant="destructive">Hết hàng</Badge>
                        ) : isLowStock ? (
                          <Badge className="bg-amber-100 text-amber-700 hover:bg-amber-100">
                            Sắp hết
                          </Badge>
                        ) : (
                          <Badge className="bg-emerald-100 text-emerald-700 hover:bg-emerald-100">
                            Còn hàng
                          </Badge>
                        )}
                      </TableCell>
                    </TableRow>
                  );
                })
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
    </MainLayout>
  );
}
