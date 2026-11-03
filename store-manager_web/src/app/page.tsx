"use client";

import { useState, useEffect } from "react";
import { MainLayout } from "@/components/layout";
import { Button } from "@/components/ui/button";
import {
  Package,
  FileText,
  ShoppingCart,
  AlertTriangle,
  TrendingUp,
  TrendingDown,
  ArrowRight,
  Clock,
  Plus,
  Loader2,
} from "lucide-react";
import Link from "next/link";

interface DashboardStats {
  totalProducts: number;
  lowStockProducts: number;
  totalImportInvoices: number;
  totalSaleInvoices: number;
  totalImportAmount: number;
  totalSaleAmount: number;
  totalProfit: number;
}

interface RecentActivity {
  id: string;
  type: "import" | "sale" | "alert" | "product";
  message: string;
  time: string;
  createdAt: Date;
}

interface TopProduct {
  id: string;
  name: string;
  totalSold: number;
  totalRevenue: number;
}

const formatCurrency = (value: number) => {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    minimumFractionDigits: 0,
    maximumFractionDigits: 0,
  }).format(value);
};

const formatTimeAgo = (date: Date) => {
  const now = new Date();
  const diff = now.getTime() - new Date(date).getTime();
  const minutes = Math.floor(diff / 60000);
  const hours = Math.floor(diff / 3600000);
  const days = Math.floor(diff / 86400000);

  if (minutes < 1) return "Vừa xong";
  if (minutes < 60) return `${minutes} phút trước`;
  if (hours < 24) return `${hours} giờ trước`;
  return `${days} ngày trước`;
};

export default function HomePage() {
  const [stats, setStats] = useState<DashboardStats | null>(null);
  const [recentActivities, setRecentActivities] = useState<RecentActivity[]>([]);
  const [topProducts, setTopProducts] = useState<TopProduct[]>([]);
  const [lowStockProducts, setLowStockProducts] = useState<any[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    fetchDashboardData();
  }, []);

  async function fetchDashboardData() {
    setIsLoading(true);
    try {
      // Fetch stats
      const statsRes = await fetch("/api/analytics/dashboard");
      if (statsRes.ok) {
        const data = await statsRes.json();
        setStats(data);
      }

      // Fetch recent activities (imports and sales)
      const activities: RecentActivity[] = [];

      const importsRes = await fetch("/api/imports?limit=3");
      if (importsRes.ok) {
        const imports = await importsRes.json();
        (imports.invoices || []).slice(0, 3).forEach((inv: any) => {
          activities.push({
            id: `import-${inv.id}`,
            type: "import",
            message: `Nhập hàng #${inv.invoiceNumber}`,
            time: formatTimeAgo(new Date(inv.createdAt)),
            createdAt: new Date(inv.createdAt),
          });
        });
      }

      const salesRes = await fetch("/api/sales?limit=3");
      if (salesRes.ok) {
        const sales = await salesRes.json();
        (sales.invoices || []).slice(0, 3).forEach((inv: any) => {
          activities.push({
            id: `sale-${inv.id}`,
            type: "sale",
            message: `Bán hàng #${inv.invoiceNumber}`,
            time: formatTimeAgo(new Date(inv.createdAt)),
            createdAt: new Date(inv.createdAt),
          });
        });
      }

      // Sort by date and take top 5
      activities.sort((a, b) => b.createdAt.getTime() - a.createdAt.getTime());
      setRecentActivities(activities.slice(0, 5));

      // Fetch low stock products
      const productsRes = await fetch("/api/products?limit=100");
      if (productsRes.ok) {
        const data = await productsRes.json();
        const lowStock = (data.products || []).filter(
          (p: any) => p.stock <= p.minStock
        );
        setLowStockProducts(lowStock);

        // Add alerts for low stock
        lowStock.slice(0, 2).forEach((p: any) => {
          if (!activities.find(a => a.message.includes(p.name))) {
            activities.push({
              id: `alert-${p.id}`,
              type: "alert",
              message: `${p.name} sắp hết hàng (${p.stock} trong kho)`,
              time: "",
              createdAt: new Date(),
            });
          }
        });
      }

      // Fetch top selling products
      const topRes = await fetch("/api/analytics/top-products?limit=5");
      if (topRes.ok) {
        const data = await topRes.json();
        setTopProducts(Array.isArray(data) ? data : []);
      }
    } catch (error) {
      console.error("Error fetching dashboard data:", error);
    } finally {
      setIsLoading(false);
    }
  }

  if (isLoading) {
    return (
      <MainLayout>
        <div className="flex items-center justify-center min-h-[400px]">
          <Loader2 className="h-8 w-8 animate-spin text-muted-foreground" />
        </div>
      </MainLayout>
    );
  }

  const statsData = [
    {
      title: "Tổng sản phẩm",
      value: stats?.totalProducts?.toLocaleString() || "0",
      icon: Package,
      bgColor: "bg-emerald-50 dark:bg-emerald-950/50",
      iconColor: "text-emerald-600 dark:text-emerald-400",
    },
    {
      title: "Hàng sắp hết",
      value: (stats?.lowStockProducts || 0).toString(),
      icon: AlertTriangle,
      bgColor: "bg-amber-50 dark:bg-amber-950/50",
      iconColor: "text-amber-600 dark:text-amber-400",
    },
    {
      title: "Hóa đơn nhập",
      value: stats?.totalImportInvoices?.toString() || "0",
      icon: FileText,
      bgColor: "bg-blue-50 dark:bg-blue-950/50",
      iconColor: "text-blue-600 dark:text-blue-400",
    },
    {
      title: "Hóa đơn bán",
      value: stats?.totalSaleInvoices?.toString() || "0",
      icon: ShoppingCart,
      bgColor: "bg-violet-50 dark:bg-violet-950/50",
      iconColor: "text-violet-600 dark:text-violet-400",
    },
  ];

  const getActivityIcon = (type: string) => {
    switch (type) {
      case "import":
        return { icon: FileText, color: "text-blue-500", bgColor: "bg-blue-50 dark:bg-blue-950/50" };
      case "sale":
        return { icon: ShoppingCart, color: "text-emerald-500", bgColor: "bg-emerald-50 dark:bg-emerald-950/50" };
      case "alert":
        return { icon: AlertTriangle, color: "text-amber-500", bgColor: "bg-amber-50 dark:bg-amber-950/50" };
      default:
        return { icon: Package, color: "text-violet-500", bgColor: "bg-violet-50 dark:bg-violet-950/50" };
    }
  };

  return (
    <MainLayout>
      <div className="space-y-8">
        {/* Page Header */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h1 className="text-3xl font-semibold tracking-tight">Dashboard</h1>
            <p className="text-muted-foreground mt-1">
              Chào mừng bạn đến với hệ thống quản lý cửa hàng
            </p>
          </div>
          <div className="flex gap-3">
            <Button variant="outline" asChild className="rounded-full">
              <Link href="/import">
                <FileText className="mr-2 h-4 w-4" />
                Nhập hàng
              </Link>
            </Button>
            <Button asChild className="rounded-full">
              <Link href="/sales">
                <Plus className="mr-2 h-4 w-4" />
                Bán hàng
              </Link>
            </Button>
          </div>
        </div>

        {/* Stats Cards */}
        <div className="grid gap-4 md:grid-cols-2 lg:grid-cols-4">
          {statsData.map((stat, index) => (
            <div
              key={index}
              className="bg-card rounded-2xl p-5 hover:shadow-md transition-shadow"
            >
              <div className="flex items-center gap-4">
                <div className={`p-3 rounded-xl ${stat.bgColor}`}>
                  <stat.icon className={`h-6 w-6 ${stat.iconColor}`} />
                </div>
                <div className="flex-1">
                  <p className="text-sm text-muted-foreground">{stat.title}</p>
                  <p className="text-2xl font-semibold mt-1">{stat.value}</p>
                </div>
              </div>
            </div>
          ))}
        </div>

        {/* Financial Stats */}
        <div className="grid gap-4 md:grid-cols-3">
          <div className="bg-card rounded-2xl p-5">
            <p className="text-sm text-muted-foreground">Tổng nhập hàng</p>
            <p className="text-xl font-semibold text-blue-600 mt-1">
              {formatCurrency(stats?.totalImportAmount || 0)}
            </p>
          </div>
          <div className="bg-card rounded-2xl p-5">
            <p className="text-sm text-muted-foreground">Tổng bán hàng</p>
            <p className="text-xl font-semibold text-emerald-600 mt-1">
              {formatCurrency(stats?.totalSaleAmount || 0)}
            </p>
          </div>
          <div className="bg-card rounded-2xl p-5">
            <p className="text-sm text-muted-foreground">Lợi nhuận ước tính</p>
            <p className="text-xl font-semibold text-violet-600 mt-1">
              {formatCurrency(stats?.totalProfit || 0)}
            </p>
          </div>
        </div>

        {/* Main Content - Two columns */}
        <div className="grid gap-6 lg:grid-cols-3">
          {/* Recent Activities */}
          <div className="lg:col-span-2 bg-card rounded-2xl p-6">
            <div className="flex items-center justify-between mb-5">
              <h2 className="text-lg font-medium flex items-center gap-2">
                Hoạt động gần đây
              </h2>
            </div>
            {recentActivities.length > 0 ? (
              <div className="space-y-1">
                {recentActivities.map((activity) => {
                  const { icon: Icon, color, bgColor } = getActivityIcon(activity.type);
                  return (
                    <div
                      key={activity.id}
                      className="flex items-center gap-4 p-3 rounded-xl hover:bg-muted/40 transition-colors"
                    >
                      <div className={`p-2.5 rounded-xl ${bgColor}`}>
                        <Icon className={`h-4 w-4 ${color}`} />
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="text-sm font-medium">{activity.message}</p>
                        {activity.time && (
                          <p className="text-xs text-muted-foreground flex items-center gap-1 mt-0.5">
                            <Clock className="h-3 w-3" />
                            {activity.time}
                          </p>
                        )}
                      </div>
                    </div>
                  );
                })}
              </div>
            ) : (
              <p className="text-center text-muted-foreground py-8">
                Chưa có hoạt động nào
              </p>
            )}
          </div>

          {/* Top Products */}
          <div className="bg-card rounded-2xl p-6">
            <div className="flex items-center justify-between mb-5">
              <h2 className="text-lg font-medium">Sản phẩm bán chạy</h2>
              <Button variant="ghost" size="sm" asChild className="text-muted-foreground">
                <Link href="/products">
                  <ArrowRight className="h-4 w-4" />
                </Link>
              </Button>
            </div>
            {topProducts.length > 0 ? (
              <div className="space-y-3">
                {topProducts.map((product, index) => (
                  <div key={product.id} className="flex items-center justify-between">
                    <div className="flex items-center gap-3">
                      <div className="flex h-8 w-8 items-center justify-center rounded-full bg-primary/10 text-xs font-medium text-primary">
                        {index + 1}
                      </div>
                      <div>
                        <p className="text-sm font-medium">{product.name}</p>
                        <p className="text-xs text-muted-foreground">
                          {product.totalSold} đã bán
                        </p>
                      </div>
                    </div>
                    <p className="text-sm font-medium text-primary">
                      {formatCurrency(product.totalRevenue)}
                    </p>
                  </div>
                ))}
              </div>
            ) : (
              <p className="text-center text-muted-foreground py-8">
                Chưa có dữ liệu
              </p>
            )}
          </div>
        </div>

        {/* Low Stock Alert */}
        {lowStockProducts.length > 0 && (
          <div className="bg-amber-50 dark:bg-amber-950/30 border border-amber-200 dark:border-amber-800 rounded-2xl p-6">
            <div className="flex items-center gap-2 mb-4">
              <AlertTriangle className="h-5 w-5 text-amber-600" />
              <h2 className="text-lg font-medium text-amber-800 dark:text-amber-200">
                Cảnh báo sắp hết hàng
              </h2>
            </div>
            <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
              {lowStockProducts.slice(0, 4).map((product) => (
                <div
                  key={product.id}
                  className="bg-white dark:bg-amber-900/20 rounded-xl p-3"
                >
                  <p className="font-medium text-sm">{product.name}</p>
                  <p className="text-xs text-amber-600 mt-1">
                    Còn {product.stock} / Tối thiểu {product.minStock}
                  </p>
                </div>
              ))}
            </div>
            {lowStockProducts.length > 4 && (
              <p className="text-sm text-amber-700 dark:text-amber-300 mt-3">
                Và {lowStockProducts.length - 4} sản phẩm khác...
              </p>
            )}
          </div>
        )}

        {/* Quick Actions */}
        <div className="grid gap-4 md:grid-cols-3">
          <Link
            href="/import"
            className="group bg-card rounded-2xl p-5 hover:shadow-md transition-all"
          >
            <div className="flex items-center gap-4">
              <div className="p-3 rounded-xl bg-blue-50 dark:bg-blue-950/50">
                <FileText className="h-5 w-5 text-blue-600 dark:text-blue-400" />
              </div>
              <div>
                <h3 className="font-medium group-hover:text-primary transition-colors">Nhập hàng mới</h3>
                <p className="text-sm text-muted-foreground">
                  Tải ảnh hóa đơn lên để AI nhận diện
                </p>
              </div>
            </div>
          </Link>

          <Link
            href="/sales"
            className="group bg-card rounded-2xl p-5 hover:shadow-md transition-all"
          >
            <div className="flex items-center gap-4">
              <div className="p-3 rounded-xl bg-emerald-50 dark:bg-emerald-950/50">
                <ShoppingCart className="h-5 w-5 text-emerald-600 dark:text-emerald-400" />
              </div>
              <div>
                <h3 className="font-medium group-hover:text-primary transition-colors">Bán hàng</h3>
                <p className="text-sm text-muted-foreground">
                  Tạo hóa đơn bán hàng mới
                </p>
              </div>
            </div>
          </Link>

          <Link
            href="/products"
            className="group bg-card rounded-2xl p-5 hover:shadow-md transition-all"
          >
            <div className="flex items-center gap-4">
              <div className="p-3 rounded-xl bg-violet-50 dark:bg-violet-950/50">
                <Package className="h-5 w-5 text-violet-600 dark:text-violet-400" />
              </div>
              <div>
                <h3 className="font-medium group-hover:text-primary transition-colors">Quản lý sản phẩm</h3>
                <p className="text-sm text-muted-foreground">
                  Thêm, sửa, xóa sản phẩm
                </p>
              </div>
            </div>
          </Link>
        </div>
      </div>
    </MainLayout>
  );
}
