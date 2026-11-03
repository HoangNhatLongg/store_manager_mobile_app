"use client";

import { useState, useEffect } from "react";
import { MainLayout } from "@/components/layout";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  ArrowLeft,
  TrendingUp,
  TrendingDown,
  DollarSign,
  ShoppingCart,
  Package,
  Calendar,
  BarChart3,
  Loader2,
} from "lucide-react";

interface MonthlyRevenue {
  month: string;
  revenue: number;
  profit: number;
  orders: number;
}

interface DailyRevenue {
  date: string;
  dayName: string;
  revenue: number;
  profit: number;
  orders: number;
}

interface ProductSale {
  productId: string;
  productName: string;
  productSku: string;
  categoryName: string | null;
  quantity: number;
  unitPrice: number;
  totalPrice: number;
  importPrice: number;
  profit: number;
}

interface DailyDetail {
  date: string;
  summary: {
    totalRevenue: number;
    totalImportCost: number;
    totalProfit: number;
    grossMargin: number;
    totalInvoices: number;
    totalProductsSold: number;
  };
  productsSold: ProductSale[];
}

const formatCurrency = (value: number) => {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    minimumFractionDigits: 0,
  }).format(value);
};

const MONTHS_VN = [
  "Tháng 1", "Tháng 2", "Tháng 3", "Tháng 4",
  "Tháng 5", "Tháng 6", "Tháng 7", "Tháng 8",
  "Tháng 9", "Tháng 10", "Tháng 11", "Tháng 12"
];

const DAYS_VN = ["CN", "T2", "T3", "T4", "T5", "T6", "T7"];

export default function RevenuePage() {
  const [selectedYear, setSelectedYear] = useState(new Date().getFullYear());
  const [selectedMonth, setSelectedMonth] = useState<number | null>(null);
  const [selectedDay, setSelectedDay] = useState<string | null>(null);

  const [monthlyData, setMonthlyData] = useState<MonthlyRevenue[]>([]);
  const [dailyData, setDailyData] = useState<DailyRevenue[]>([]);
  const [dailyDetail, setDailyDetail] = useState<DailyDetail | null>(null);

  const [isLoading, setIsLoading] = useState(true);
  const [isLoadingDetail, setIsLoadingDetail] = useState(false);
  const [isSeeding, setIsSeeding] = useState(false);

  // Fetch monthly revenue
  useEffect(() => {
    fetchMonthlyRevenue();
  }, [selectedYear]);

  // Fetch daily data when month is selected
  useEffect(() => {
    if (selectedMonth !== null) {
      fetchDailyRevenue();
    }
  }, [selectedMonth, selectedYear]);

  // Fetch daily detail when day is selected
  useEffect(() => {
    if (selectedDay) {
      fetchDailyDetail();
    }
  }, [selectedDay]);

  async function fetchMonthlyRevenue() {
    setIsLoading(true);
    try {
      const startDate = `${selectedYear}-01-01`;
      const endDate = `${selectedYear}-12-31`;

      const res = await fetch(
        `/api/analytics/revenue?startDate=${startDate}&endDate=${endDate}&period=month`
      );

      if (res.ok) {
        const data = await res.json();
        processMonthlyData(data.byPeriod || []);
      }
    } catch (error) {
      console.error("Error fetching monthly revenue:", error);
    } finally {
      setIsLoading(false);
    }
  }

  function processMonthlyData(byPeriod: any[]) {
    const monthly: Record<number, MonthlyRevenue> = {};

    // Initialize all months
    for (let i = 0; i < 12; i++) {
      monthly[i] = {
        month: MONTHS_VN[i],
        revenue: 0,
        profit: 0,
        orders: 0,
      };
    }

    // Fill data - API returns "DD-MM" format (e.g., "23-05")
    for (const item of byPeriod) {
      const label = item.label || "";
      // Split by "-" - first part is day, second is month
      const parts = label.split("-");
      if (parts.length === 2) {
        const monthIndex = parseInt(parts[1], 10) - 1;
        if (monthIndex >= 0 && monthIndex < 12) {
          monthly[monthIndex].revenue += item.revenue || 0;
          monthly[monthIndex].profit += item.profit || 0;
          monthly[monthIndex].orders += item.count || 0;
        }
      }
    }

    setMonthlyData(Object.values(monthly));
  }

  async function fetchDailyRevenue() {
    if (selectedMonth === null) return;

    setIsLoading(true);
    setDailyDetail(null);
    setSelectedDay(null);

    try {
      const startDate = `${selectedYear}-${String(selectedMonth + 1).padStart(2, "0")}-01`;
      const lastDay = new Date(selectedYear, selectedMonth + 1, 0).getDate();
      const endDate = `${selectedYear}-${String(selectedMonth + 1).padStart(2, "0")}-${lastDay}`;

      const res = await fetch(
        `/api/analytics/revenue?startDate=${startDate}&endDate=${endDate}&period=month`
      );

      if (res.ok) {
        const data = await res.json();
        processDailyData(data.byPeriod || []);
      }
    } catch (error) {
      console.error("Error fetching daily revenue:", error);
    } finally {
      setIsLoading(false);
    }
  }

  function processDailyData(byPeriod: any[]) {
    const daily: Record<string, DailyRevenue> = {};

    // Initialize all days of the month
    if (selectedMonth !== null) {
      const lastDay = new Date(selectedYear, selectedMonth + 1, 0).getDate();
      for (let d = 1; d <= lastDay; d++) {
        const dateStr = `${selectedYear}-${String(selectedMonth + 1).padStart(2, "0")}-${String(d).padStart(2, "0")}`;
        // Create date in local timezone
        const date = new Date(selectedYear, selectedMonth, d);
        daily[dateStr] = {
          date: dateStr,
          dayName: DAYS_VN[date.getDay()],
          revenue: 0,
          profit: 0,
          orders: 0,
        };
      }
    }

    // Fill data - API returns "DD-MM" format
    for (const item of byPeriod) {
      const parts = item.label?.split("-");
      if (parts && parts.length === 2) {
        const day = parseInt(parts[0], 10);
        const month = parseInt(parts[1], 10);
        const dateStr = `${selectedYear}-${String(month).padStart(2, "0")}-${String(day).padStart(2, "0")}`;
        if (daily[dateStr]) {
          daily[dateStr].revenue += item.revenue || 0;
          daily[dateStr].profit += item.profit || 0;
          daily[dateStr].orders += item.count || 0;
        }
      }
    }

    setDailyData(Object.values(daily).sort((a, b) => a.date.localeCompare(b.date)));
  }

  async function fetchDailyDetail() {
    if (!selectedDay) return;

    setIsLoadingDetail(true);
    try {
      const res = await fetch(`/api/analytics/revenue-daily?date=${selectedDay}`);

      if (res.ok) {
        const data = await res.json();
        setDailyDetail(data);
      }
    } catch (error) {
      console.error("Error fetching daily detail:", error);
    } finally {
      setIsLoadingDetail(false);
    }
  }

  async function handleSeedData() {
    setIsSeeding(true);
    try {
      const res = await fetch("/api/analytics/seed-test-data", { method: "POST" });
      const data = await res.json();

      if (res.ok) {
        alert(data.message);
        fetchMonthlyRevenue();
      } else {
        alert(data.error || "Có lỗi xảy ra");
      }
    } catch (error) {
      alert("Có lỗi xảy ra");
    } finally {
      setIsSeeding(false);
    }
  }

  // Calculate chart height
  const maxRevenue = Math.max(...monthlyData.map(m => m.revenue), 1);

  return (
    <MainLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <h1 className="text-3xl font-semibold tracking-tight">Quản lý doanh thu</h1>
          </div>
          <div className="flex items-center gap-4">
            <Select value={selectedYear.toString()} onValueChange={(v) => setSelectedYear(parseInt(v))}>
              <SelectTrigger className="w-[140px] rounded-xl">
                <SelectValue />
              </SelectTrigger>
              <SelectContent>
                {[2024, 2025, 2026, 2027].map(year => (
                  <SelectItem key={year} value={year.toString()}>{year}</SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>
        </div>

        {/* Navigation */}
        {selectedMonth !== null && (
          <div className="flex items-center gap-2">
            <Button variant="outline" onClick={() => {
              setSelectedMonth(null);
              setSelectedDay(null);
              setDailyDetail(null);
            }} className="rounded-xl">
              <ArrowLeft className="h-4 w-4 mr-2" />
              Quay lại
            </Button>
            <span className="text-muted-foreground">
              {MONTHS_VN[selectedMonth]} {selectedYear}
              {selectedDay && ` > Ngày ${selectedDay.split("-")[2]}`}
            </span>
          </div>
        )}

        {/* Monthly View */}
        {selectedMonth === null && (
          <>
            {/* Summary Cards */}
            <div className="grid gap-4 md:grid-cols-4">
              <Card className="rounded-2xl">
                <CardHeader className="flex flex-row items-center justify-between pb-2">
                  <CardTitle className="text-sm font-medium">Tổng doanh thu năm</CardTitle>
                  <DollarSign className="h-4 w-4 text-muted-foreground" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold">
                    {formatCurrency(monthlyData.reduce((sum, m) => sum + m.revenue, 0))}
                  </div>
                </CardContent>
              </Card>
              <Card className="rounded-2xl">
                <CardHeader className="flex flex-row items-center justify-between pb-2">
                  <CardTitle className="text-sm font-medium">Tổng lợi nhuận</CardTitle>
                  <TrendingUp className="h-4 w-4 text-green-500" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold text-green-600">
                    {formatCurrency(monthlyData.reduce((sum, m) => sum + m.profit, 0))}
                  </div>
                </CardContent>
              </Card>
              <Card className="rounded-2xl">
                <CardHeader className="flex flex-row items-center justify-between pb-2">
                  <CardTitle className="text-sm font-medium">Số đơn hàng</CardTitle>
                  <ShoppingCart className="h-4 w-4 text-muted-foreground" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold">
                    {monthlyData.reduce((sum, m) => sum + m.orders, 0)}
                  </div>
                </CardContent>
              </Card>
              <Card className="rounded-2xl">
                <CardHeader className="flex flex-row items-center justify-between pb-2">
                  <CardTitle className="text-sm font-medium">TB tháng</CardTitle>
                  <BarChart3 className="h-4 w-4 text-muted-foreground" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold">
                    {formatCurrency(
                      monthlyData.reduce((sum, m) => sum + m.revenue, 0) / 12 || 0
                    )}
                  </div>
                </CardContent>
              </Card>
            </div>

            {/* Monthly Chart */}
            <Card className="rounded-2xl">
              <CardHeader>
                <CardTitle>Doanh thu theo tháng - {selectedYear}</CardTitle>
              </CardHeader>
              <CardContent>
                {isLoading ? (
                  <div className="h-[300px] flex items-center justify-center">
                    <p className="text-muted-foreground">Đang tải...</p>
                  </div>
                ) : monthlyData.reduce((sum, m) => sum + m.revenue, 0) === 0 ? (
                  <div className="h-[300px] flex flex-col items-center justify-center text-center space-y-4">
                    <BarChart3 className="h-12 w-12 text-muted-foreground" />
                    <div>
                      <p className="text-lg font-medium">Chưa có dữ liệu doanh thu</p>
                      <p className="text-sm text-muted-foreground mt-2">
                        Hãy tạo đơn bán hàng tại trang Bán hàng để xem thống kê
                      </p>
                    </div>
                    <Button onClick={handleSeedData} disabled={isSeeding} className="rounded-xl">
                      {isSeeding ? (
                        <>
                          <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                          Đang tạo...
                        </>
                      ) : "Tạo dữ liệu mẫu"}
                    </Button>
                  </div>
                ) : (
                  <div className="space-y-4">
                    {/* Chart */}
                    <div className="flex items-end justify-between gap-2 h-[300px]">
                      {monthlyData.map((month, index) => (
                        <div
                          key={index}
                          className="flex-1 flex flex-col items-center cursor-pointer group"
                          onClick={() => setSelectedMonth(index)}
                        >
                          <div className="w-full flex flex-col items-center justify-end h-[260px]">
                            <div
                              className="w-full max-w-[50px] bg-primary rounded-t-lg transition-all hover:bg-primary/80 relative group-hover:opacity-80"
                              style={{
                                height: `${Math.max((month.revenue / maxRevenue) * 200, month.revenue > 0 ? 20 : 0)}px`,
                              }}
                            >
                              <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 opacity-0 group-hover:opacity-100 transition-opacity bg-black text-white text-xs px-2 py-1 rounded whitespace-nowrap z-10">
                                {formatCurrency(month.revenue)}
                              </div>
                            </div>
                          </div>
                          <p className="text-xs text-muted-foreground mt-2">
                            {MONTHS_VN[index].replace("Tháng ", "")}
                          </p>
                        </div>
                      ))}
                    </div>
                    <p className="text-center text-sm text-muted-foreground">
                      Click vào cột để xem chi tiết tháng
                    </p>
                  </div>
                )}
              </CardContent>
            </Card>
          </>
        )}

        {/* Daily View */}
        {selectedMonth !== null && selectedDay === null && (
          <>
            {/* Month Summary */}
            <div className="grid gap-4 md:grid-cols-3">
              <Card className="rounded-2xl">
                <CardHeader className="flex flex-row items-center justify-between pb-2">
                  <CardTitle className="text-sm font-medium">Doanh thu tháng</CardTitle>
                  <DollarSign className="h-4 w-4 text-muted-foreground" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold">
                    {formatCurrency(dailyData.reduce((sum, d) => sum + d.revenue, 0))}
                  </div>
                </CardContent>
              </Card>
              <Card className="rounded-2xl">
                <CardHeader className="flex flex-row items-center justify-between pb-2">
                  <CardTitle className="text-sm font-medium">Lợi nhuận tháng</CardTitle>
                  <TrendingUp className="h-4 w-4 text-green-500" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold text-green-600">
                    {formatCurrency(dailyData.reduce((sum, d) => sum + d.profit, 0))}
                  </div>
                </CardContent>
              </Card>
              <Card className="rounded-2xl">
                <CardHeader className="flex flex-row items-center justify-between pb-2">
                  <CardTitle className="text-sm font-medium">Số đơn tháng</CardTitle>
                  <ShoppingCart className="h-4 w-4 text-muted-foreground" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold">
                    {dailyData.reduce((sum, d) => sum + d.orders, 0)}
                  </div>
                </CardContent>
              </Card>
            </div>

            {/* Daily Chart */}
            <Card className="rounded-2xl">
              <CardHeader>
                <CardTitle>Doanh thu theo ngày - {MONTHS_VN[selectedMonth]} {selectedYear}</CardTitle>
              </CardHeader>
              <CardContent>
                {isLoading ? (
                  <div className="h-[200px] flex items-center justify-center">
                    <p className="text-muted-foreground">Đang tải...</p>
                  </div>
                ) : (
                  <div className="space-y-4">
                    <div className="flex items-end justify-between gap-1 h-[200px]">
                      {dailyData.map((day, index) => (
                        <div
                          key={index}
                          className="flex-1 flex flex-col items-center cursor-pointer group"
                          onClick={() => day.revenue > 0 && setSelectedDay(day.date)}
                        >
                          <div className="w-full flex flex-col items-center justify-end h-[160px]">
                            <div
                              className={`w-full rounded-t ${day.revenue > 0 ? "bg-primary hover:bg-primary/80" : "bg-muted"} transition-all relative group-hover:opacity-80`}
                              style={{
                                height: `${Math.max((day.revenue / Math.max(...dailyData.map(d => d.revenue), 1)) * 120, day.revenue > 0 ? 10 : 0)}px`,
                              }}
                            >
                              {day.revenue > 0 && (
                                <div className="absolute bottom-full left-1/2 -translate-x-1/2 mb-2 opacity-0 group-hover:opacity-100 transition-opacity bg-black text-white text-[10px] px-1 py-0.5 rounded whitespace-nowrap z-10">
                                  {formatCurrency(day.revenue)}
                                </div>
                              )}
                            </div>
                          </div>
                          <p className="text-[10px] text-muted-foreground mt-1">
                            {day.date.split("-")[2]}
                          </p>
                        </div>
                      ))}
                    </div>
                    <p className="text-center text-sm text-muted-foreground">
                      Click vào cột có doanh thu để xem chi tiết ngày
                    </p>
                  </div>
                )}
              </CardContent>
            </Card>

            {/* Daily List */}
            <Card className="rounded-2xl">
              <CardHeader>
                <CardTitle>Danh sách ngày trong tháng</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="grid gap-2 sm:grid-cols-2 md:grid-cols-4 lg:grid-cols-7">
                  {dailyData.map((day, index) => (
                    <button
                      key={index}
                      onClick={() => day.revenue > 0 && setSelectedDay(day.date)}
                      disabled={day.revenue === 0}
                      className={`p-3 rounded-xl text-center transition-all ${
                        day.revenue > 0
                          ? "bg-muted hover:bg-muted/80 cursor-pointer"
                          : "bg-muted/50 opacity-50 cursor-not-allowed"
                      }`}
                    >
                      <p className="text-xs text-muted-foreground">{day.dayName}</p>
                      <p className="text-lg font-bold">{day.date.split("-")[2]}</p>
                      {day.revenue > 0 ? (
                        <p className="text-xs text-primary font-medium">
                          {formatCurrency(day.revenue)}
                        </p>
                      ) : (
                        <p className="text-xs text-muted-foreground">Không có</p>
                      )}
                    </button>
                  ))}
                </div>
              </CardContent>
            </Card>
          </>
        )}

        {/* Day Detail */}
        {selectedDay && dailyDetail && (
          <>
            {/* Day Summary */}
            <div className="grid gap-4 md:grid-cols-4">
              <Card className="rounded-2xl">
                <CardHeader className="flex flex-row items-center justify-between pb-2">
                  <CardTitle className="text-sm font-medium">Doanh thu ngày</CardTitle>
                  <DollarSign className="h-4 w-4 text-muted-foreground" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold">
                    {formatCurrency(dailyDetail.summary.totalRevenue)}
                  </div>
                </CardContent>
              </Card>
              <Card className="rounded-2xl">
                <CardHeader className="flex flex-row items-center justify-between pb-2">
                  <CardTitle className="text-sm font-medium">Lợi nhuận</CardTitle>
                  <TrendingUp className="h-4 w-4 text-green-500" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold text-green-600">
                    {formatCurrency(dailyDetail.summary.totalProfit)}
                  </div>
                </CardContent>
              </Card>
              <Card className="rounded-2xl">
                <CardHeader className="flex flex-row items-center justify-between pb-2">
                  <CardTitle className="text-sm font-medium">Số đơn</CardTitle>
                  <ShoppingCart className="h-4 w-4 text-muted-foreground" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold">
                    {dailyDetail.summary.totalInvoices}
                  </div>
                </CardContent>
              </Card>
              <Card className="rounded-2xl">
                <CardHeader className="flex flex-row items-center justify-between pb-2">
                  <CardTitle className="text-sm font-medium">Sản phẩm bán</CardTitle>
                  <Package className="h-4 w-4 text-muted-foreground" />
                </CardHeader>
                <CardContent>
                  <div className="text-2xl font-bold">
                    {dailyDetail.summary.totalProductsSold}
                  </div>
                </CardContent>
              </Card>
            </div>

            {/* Products Sold */}
            <Card className="rounded-2xl">
              <CardHeader>
                <CardTitle>Sản phẩm bán trong ngày</CardTitle>
              </CardHeader>
              <CardContent>
                {isLoadingDetail ? (
                  <div className="py-8 text-center">
                    <p className="text-muted-foreground">Đang tải...</p>
                  </div>
                ) : dailyDetail.productsSold.length === 0 ? (
                  <div className="py-8 text-center">
                    <p className="text-muted-foreground">Không có sản phẩm nào được bán</p>
                  </div>
                ) : (
                  <div className="overflow-x-auto">
                    <table className="w-full text-sm">
                      <thead>
                        <tr className="border-b">
                          <th className="text-left p-3 font-medium">STT</th>
                          <th className="text-left p-3 font-medium">Sản phẩm</th>
                          <th className="text-left p-3 font-medium">Danh mục</th>
                          <th className="text-center p-3 font-medium">SL</th>
                          <th className="text-right p-3 font-medium">Đơn giá</th>
                          <th className="text-right p-3 font-medium">Thành tiền</th>
                          <th className="text-right p-3 font-medium">Lợi nhuận</th>
                        </tr>
                      </thead>
                      <tbody>
                        {dailyDetail.productsSold.map((product, index) => (
                          <tr key={product.productId} className="border-b hover:bg-muted/50">
                            <td className="p-3">{index + 1}</td>
                            <td className="p-3">
                              <div>
                                <p className="font-medium">{product.productName}</p>
                                <p className="text-xs text-muted-foreground">SKU: {product.productSku}</p>
                              </div>
                            </td>
                            <td className="p-3">
                              {product.categoryName || <span className="text-muted-foreground">-</span>}
                            </td>
                            <td className="p-3 text-center">{product.quantity}</td>
                            <td className="p-3 text-right">{formatCurrency(product.unitPrice)}</td>
                            <td className="p-3 text-right font-medium">{formatCurrency(product.totalPrice)}</td>
                            <td className={`p-3 text-right ${product.profit >= 0 ? "text-green-600" : "text-red-600"}`}>
                              {formatCurrency(product.profit)}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                      <tfoot>
                        <tr className="bg-muted font-bold">
                          <td className="p-3" colSpan={3}>Tổng cộng</td>
                          <td className="p-3 text-center">
                            {dailyDetail.productsSold.reduce((sum, p) => sum + p.quantity, 0)}
                          </td>
                          <td className="p-3 text-right"></td>
                          <td className="p-3 text-right">{formatCurrency(dailyDetail.summary.totalRevenue)}</td>
                          <td className="p-3 text-right text-green-600">{formatCurrency(dailyDetail.summary.totalProfit)}</td>
                        </tr>
                      </tfoot>
                    </table>
                  </div>
                )}
              </CardContent>
            </Card>
          </>
        )}
      </div>
    </MainLayout>
  );
}
