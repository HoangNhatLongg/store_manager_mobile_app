"use client";

import { useState, useEffect } from "react";
import { MainLayout } from "@/components/layout";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Badge } from "@/components/ui/badge";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
} from "@/components/ui/dialog";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Plus,
  Search,
  Trash2,
  Minus,
  ShoppingCart,
  Loader2,
  Package,
  Receipt,
  Ruler,
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
  price: number;
  isDefault: boolean;
  unit: Unit;
}

interface Product {
  id: string;
  name: string;
  sku: string;
  sellPrice: number;
  stock: number;
  isActive: boolean;
  units?: ProductUnit[];
}

interface CartItem {
  id: string;
  productId: string;
  name: string;
  price: number;
  quantity: number;
  maxQuantity: number;
  unitName: string;
  unitAbbreviation: string;
  // Số lượng đơn vị cơ bản trong 1 đơn vị bán, dùng để quy đổi tồn kho.
  conversionQty: number;
}

interface Customer {
  id: string;
  name: string;
  phone: string | null;
}

const formatCurrency = (value: number) => {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
  }).format(value);
};

export default function SalesPage() {
  const [products, setProducts] = useState<Product[]>([]);
  const [allProductUnits, setAllProductUnits] = useState<Record<string, ProductUnit[]>>({});
  const [customers, setCustomers] = useState<Customer[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState("");
  const [cart, setCart] = useState<CartItem[]>([]);
  const [isPaymentDialogOpen, setIsPaymentDialogOpen] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  
  // Dialog chọn đơn vị
  const [isUnitDialogOpen, setIsUnitDialogOpen] = useState(false);
  const [selectedProduct, setSelectedProduct] = useState<Product | null>(null);
  const [selectedUnit, setSelectedUnit] = useState<ProductUnit | null>(null);
  
  const [paymentData, setPaymentData] = useState({
    customerId: "",
    paymentMethod: "CASH",
    cashReceived: "",
    notes: "",
    discount: 0,
  });
  const [paymentSuccess, setPaymentSuccess] = useState<{
    invoiceNumber: string;
    change: number;
  } | null>(null);

  useEffect(() => {
    fetchData();
  }, []);

  async function fetchData() {
    setIsLoading(true);
    try {
      const [productsRes, customersRes] = await Promise.all([
        fetch("/api/products?isActive=true"),
        fetch("/api/customers"),
      ]);

      if (productsRes.ok) {
        const data = await productsRes.json();
        setProducts(data.products || []);
        
        // Load product units cho từng sản phẩm
        const productIds = (data.products || []).map((p: Product) => p.id);
        const unitsPromises = productIds.map((id: string) => 
          fetch(`/api/product-units?productId=${id}`)
            .then(res => res.ok ? res.json() : [])
            .then(units => ({ id, units: Array.isArray(units) ? units : [] }))
            .catch(() => ({ id, units: [] }))
        );
        const unitsResults = await Promise.all(unitsPromises);
        const unitsMap: Record<string, ProductUnit[]> = {};
        unitsResults.forEach(({ id, units }) => {
          unitsMap[id] = units;
        });
        setAllProductUnits(unitsMap);
      }

      if (customersRes.ok) {
        const data = await customersRes.json();
        setCustomers(Array.isArray(data) ? data : []);
      }
    } catch (error) {
      console.error("Error fetching data:", error);
    } finally {
      setIsLoading(false);
    }
  }

  const filteredProducts = products.filter(
    (p) =>
      p.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      p.sku.toLowerCase().includes(searchTerm.toLowerCase())
  );

  const getProductPrice = (product: Product): number => {
    const units = allProductUnits[product.id] || [];
    const defaultUnit = units.find((u) => u.isDefault);
    if (defaultUnit) return defaultUnit.price;
    if (units.length > 0) return units[0].price;
    return product.sellPrice;
  };

  const getProductUnitName = (product: Product): string => {
    const units = allProductUnits[product.id] || [];
    const defaultUnit = units.find((u) => u.isDefault);
    if (defaultUnit) return defaultUnit.unit.name;
    if (units.length > 0) return units[0].unit.name;
    return "";
  };

  const hasMultipleUnits = (product: Product): boolean => {
    const units = allProductUnits[product.id] || [];
    return units.length > 1;
  };

  const handleProductClick = (product: Product) => {
    const units = allProductUnits[product.id] || [];

    if (units.length === 0) {
      // Không có đơn vị, thêm trực tiếp với giá mặc định
      addToCart(product, product.sellPrice, "cái", "cái", 1);
    } else if (units.length === 1) {
      // Chỉ có 1 đơn vị
      const unit = units[0];
      addToCart(product, unit.price, unit.unit.name, unit.unit.abbreviation, unit.conversionQty);
    } else {
      // Nhiều đơn vị, hiện dialog chọn
      setSelectedProduct(product);
      const defaultUnit = units.find((u) => u.isDefault) || units[0];
      setSelectedUnit(defaultUnit);
      setIsUnitDialogOpen(true);
    }
  };

  const addToCart = (
    product: Product,
    price: number,
    unitName: string,
    unitAbbreviation: string = "cái",
    conversionQty: number = 1
  ) => {
    if (product.stock <= 0) return;

    // Tính max quantity dựa trên đơn vị
    const units = allProductUnits[product.id] || [];
    const baseUnit = units.find((u) => u.unit.isBaseUnit);
    const selectedUnitInfo = units.find((u) => u.unit.name === unitName);

    let maxQty = product.stock;
    if (baseUnit && selectedUnitInfo && !selectedUnitInfo.unit.isBaseUnit) {
      // Nếu chọn đơn vị quy đổi (thùng), tính số thùng tối đa
      maxQty = Math.floor(product.stock / selectedUnitInfo.conversionQty);
    }

    if (maxQty <= 0) return;

    // Check nếu sản phẩm đã có trong cart với cùng đơn vị
    setCart((prev) => {
      const existing = prev.find(
        (item) =>
          item.productId === product.id &&
          item.unitName === unitName
      );
      if (existing) {
        if (existing.quantity >= maxQty) return prev;
        return prev.map((item) =>
          item.id === existing.id
            ? { ...item, quantity: item.quantity + 1 }
            : item
        );
      }
      return [
        ...prev,
        {
          id: `${product.id}-${unitName}-${Date.now()}`,
          productId: product.id,
          name: product.name,
          price,
          quantity: 1,
          maxQuantity: maxQty,
          unitName,
          unitAbbreviation,
          conversionQty,
        },
      ];
    });
  };

  const updateQuantity = (itemId: string, delta: number) => {
    setCart((prev) =>
      prev
        .map((item) => {
          if (item.id === itemId) {
            const newQty = item.quantity + delta;
            if (newQty <= 0) return null;
            if (newQty > item.maxQuantity) return item;
            return { ...item, quantity: newQty };
          }
          return item;
        })
        .filter((item) => item !== null) as CartItem[]
    );
  };

  const removeFromCart = (itemId: string) => {
    setCart((prev) => prev.filter((item) => item.id !== itemId));
  };

  const subtotal = cart.reduce(
    (sum, item) => sum + item.price * item.quantity,
    0
  );
  const discount = paymentData.discount || 0;
  const totalAmount = subtotal - discount;
  const cashReceived = parseFloat(paymentData.cashReceived) || 0;
  const change = cashReceived - totalAmount;

  const handlePayment = () => {
    setPaymentData({
      ...paymentData,
      cashReceived: totalAmount.toString(),
    });
    setIsPaymentDialogOpen(true);
  };

  async function handleCompletePayment() {
    if (cart.length === 0) return;

    setIsSubmitting(true);
    try {
      const items = cart.map((item) => ({
        productId: item.productId,
        quantity: item.quantity,
        unitPrice: item.price,
        conversionQty: item.conversionQty,
      }));

      const res = await fetch("/api/sales", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          customerId: paymentData.customerId || null,
          paymentMethod: paymentData.paymentMethod,
          notes: paymentData.notes,
          discount,
          items,
        }),
      });

      if (!res.ok) {
        const error = await res.json();
        alert(error.error || "Có lỗi xảy ra");
        return;
      }

      const invoice = await res.json();
      setPaymentSuccess({
        invoiceNumber: invoice.invoiceNumber,
        change: change > 0 ? change : 0,
      });
      setCart([]);
      
      fetchData();
    } catch (error) {
      console.error("Error:", error);
      alert("Có lỗi xảy ra");
    } finally {
      setIsSubmitting(false);
    }
  }

  const closePaymentDialog = () => {
    setIsPaymentDialogOpen(false);
    setPaymentSuccess(null);
    setPaymentData({
      customerId: "",
      paymentMethod: "CASH",
      cashReceived: "",
      notes: "",
      discount: 0,
    });
  };

  const confirmUnitSelection = () => {
    if (selectedProduct && selectedUnit) {
      addToCart(
        selectedProduct,
        selectedUnit.price,
        selectedUnit.unit.name,
        selectedUnit.unit.abbreviation,
        selectedUnit.conversionQty
      );
    }
    setIsUnitDialogOpen(false);
    setSelectedProduct(null);
    setSelectedUnit(null);
  };

  return (
    <MainLayout>
      <div className="space-y-8">
        {/* Header */}
        <div>
          <h1 className="text-3xl font-semibold tracking-tight">Bán hàng</h1>
          <p className="text-muted-foreground mt-1">
            Tạo hóa đơn và xử lý thanh toán
          </p>
        </div>

        <div className="grid gap-6 lg:grid-cols-3">
          {/* Products Grid */}
          <div className="lg:col-span-2 space-y-4">
            {/* Search */}
            <div className="relative">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Tìm kiếm sản phẩm..."
                className="pl-10 bg-card rounded-xl border-0"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>

            {/* Product Grid */}
            {isLoading ? (
              <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3">
                {[...Array(8)].map((_, i) => (
                  <div
                    key={i}
                    className="bg-card rounded-2xl p-4 animate-pulse"
                  >
                    <div className="aspect-square rounded-xl bg-muted/50 mb-3" />
                    <div className="h-4 bg-muted/50 rounded w-3/4 mb-2" />
                    <div className="h-5 bg-muted/50 rounded w-1/2" />
                  </div>
                ))}
              </div>
            ) : filteredProducts.length === 0 ? (
              <div className="text-center py-12 bg-card rounded-2xl">
                <Package className="h-12 w-12 mx-auto mb-4 text-muted-foreground opacity-50" />
                <p className="text-muted-foreground">Không tìm thấy sản phẩm</p>
              </div>
            ) : (
              <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3">
                {filteredProducts.map((product) => {
                  const units = allProductUnits[product.id] || [];
                  const displayPrice = getProductPrice(product);
                  const unitName = getProductUnitName(product);
                  const hasMultiple = hasMultipleUnits(product);

                  return (
                    <button
                      key={product.id}
                      onClick={() => handleProductClick(product)}
                      disabled={product.stock <= 0}
                      className={`bg-card rounded-2xl p-4 text-left hover:shadow-md transition-all group relative ${
                        product.stock <= 0
                          ? "opacity-50 cursor-not-allowed"
                          : "hover:bg-muted/50"
                      }`}
                    >
                      {hasMultiple && (
                        <div className="absolute top-2 right-2">
                          <Ruler className="h-4 w-4 text-muted-foreground" />
                        </div>
                      )}
                      <div className="aspect-square rounded-xl bg-muted/50 flex items-center justify-center mb-3 group-hover:bg-primary/10 transition-colors overflow-hidden">
                        {(product as any).imageUrl ? (
                          <img
                            src={(product as any).imageUrl}
                            alt={product.name}
                            className="w-full h-full object-cover"
                          />
                        ) : (
                          <span className="text-2xl font-semibold text-muted-foreground group-hover:text-primary transition-colors">
                            {product.name.charAt(0)}
                          </span>
                        )}
                      </div>
                      <h3 className="font-medium text-sm line-clamp-1">
                        {product.name}
                      </h3>
                      <p className="text-primary font-semibold mt-1">
                        {formatCurrency(displayPrice)}
                      </p>
                      {unitName && (
                        <p className="text-xs text-muted-foreground">/{unitName}</p>
                      )}
                      <Badge
                        variant={product.stock > 10 ? "secondary" : "destructive"}
                        className="mt-2 text-xs"
                      >
                        {product.stock > 0 ? `Còn ${product.stock}` : "Hết hàng"}
                      </Badge>
                    </button>
                  );
                })}
              </div>
            )}
          </div>

          {/* Cart */}
          <div className="bg-card rounded-2xl p-6 h-fit sticky top-24 space-y-6">
            <div className="flex items-center justify-between">
              <h2 className="text-lg font-medium flex items-center gap-2">
                <ShoppingCart className="h-5 w-5" />
                Giỏ hàng
              </h2>
              {cart.length > 0 && (
                <Badge variant="secondary">{cart.length} sản phẩm</Badge>
              )}
            </div>

            {cart.length > 0 ? (
              <>
                {/* Cart Items */}
                <div className="space-y-3 max-h-[400px] overflow-y-auto">
                  {cart.map((item) => (
                    <div
                      key={item.id}
                      className="flex items-center justify-between p-3 rounded-xl bg-muted/30 hover:bg-muted/50 transition-colors"
                    >
                      <div className="flex-1 min-w-0">
                        <p className="font-medium text-sm line-clamp-1">
                          {item.name}
                        </p>
                        <p className="text-xs text-muted-foreground">
                          {formatCurrency(item.price)}/{item.unitName}
                        </p>
                      </div>
                      <div className="flex items-center gap-1 ml-4">
                        <Button
                          variant="outline"
                          size="icon"
                          className="h-7 w-7 rounded-lg"
                          onClick={() => updateQuantity(item.id, -1)}
                        >
                          <Minus className="h-3 w-3" />
                        </Button>
                        <span className="w-8 text-center font-medium text-sm">
                          {item.quantity}
                        </span>
                        <Button
                          variant="outline"
                          size="icon"
                          className="h-7 w-7 rounded-lg"
                          onClick={() => updateQuantity(item.id, 1)}
                          disabled={item.quantity >= item.maxQuantity}
                        >
                          <Plus className="h-3 w-3" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="h-7 w-7 rounded-lg text-destructive hover:text-destructive"
                          onClick={() => removeFromCart(item.id)}
                        >
                          <Trash2 className="h-3 w-3" />
                        </Button>
                      </div>
                    </div>
                  ))}
                </div>

                {/* Total */}
                <div className="pt-4 border-t border-border/50 space-y-2">
                  <div className="flex justify-between items-center">
                    <span className="text-muted-foreground">Tạm tính</span>
                    <span className="text-muted-foreground">
                      {formatCurrency(subtotal)}
                    </span>
                  </div>
                  {discount > 0 && (
                    <div className="flex justify-between items-center text-green-600">
                      <span>Giảm giá</span>
                      <span>-{formatCurrency(discount)}</span>
                    </div>
                  )}
                  <div className="flex justify-between items-center">
                    <span className="font-medium">Tổng cộng</span>
                    <span className="text-2xl font-bold text-primary">
                      {formatCurrency(totalAmount)}
                    </span>
                  </div>
                </div>

                {/* Payment Button */}
                <Button
                  className="w-full rounded-xl"
                  size="lg"
                  onClick={handlePayment}
                >
                  Thanh toán
                </Button>
              </>
            ) : (
              <div className="text-center py-12 text-muted-foreground">
                <ShoppingCart className="h-12 w-12 mx-auto mb-4 opacity-50" />
                <p>Giỏ hàng trống</p>
                <p className="text-sm">Chọn sản phẩm để thêm vào giỏ</p>
              </div>
            )}
          </div>
        </div>

        {/* Unit Selection Dialog */}
        <Dialog open={isUnitDialogOpen} onOpenChange={setIsUnitDialogOpen}>
          <DialogContent className="rounded-2xl">
            <DialogHeader>
              <DialogTitle>Chọn đơn vị</DialogTitle>
            </DialogHeader>
            <div className="space-y-4">
              <p className="font-medium">{selectedProduct?.name}</p>
              <div className="space-y-2">
                {(allProductUnits[selectedProduct?.id || ""] || []).map((unit) => (
                  <button
                    key={unit.id}
                    onClick={() => setSelectedUnit(unit)}
                    className={`w-full p-4 rounded-xl border text-left transition-all ${
                      selectedUnit?.id === unit.id
                        ? "border-primary bg-primary/5"
                        : "border-border hover:border-primary/50"
                    }`}
                  >
                    <div className="flex justify-between items-center">
                      <div>
                        <p className="font-medium">{unit.unit.name}</p>
                        {!unit.unit.isBaseUnit && (
                          <p className="text-sm text-muted-foreground">
                            1 {unit.unit.name} = {unit.conversionQty}{" "}
                            {(allProductUnits[selectedProduct?.id || ""] || []).find(
                              (u) => u.unit.isBaseUnit
                            )?.unit.abbreviation || "đơn vị"}
                          </p>
                        )}
                      </div>
                      <div className="text-right">
                        <p className="font-semibold text-primary">
                          {formatCurrency(unit.price)}
                        </p>
                        <p className="text-xs text-muted-foreground">
                          /{unit.unit.name}
                        </p>
                      </div>
                    </div>
                  </button>
                ))}
              </div>
            </div>
            <DialogFooter>
              <Button
                variant="outline"
                onClick={() => setIsUnitDialogOpen(false)}
                className="rounded-xl"
              >
                Hủy
              </Button>
              <Button
                onClick={confirmUnitSelection}
                disabled={!selectedUnit}
                className="rounded-xl"
              >
                Thêm vào giỏ
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>

        {/* Payment Dialog */}
        <Dialog open={isPaymentDialogOpen} onOpenChange={closePaymentDialog}>
          <DialogContent className="rounded-2xl sm:max-w-[450px]">
            <DialogHeader>
              <DialogTitle className="flex items-center gap-2">
                {paymentSuccess ? (
                  <>
                    <Receipt className="h-5 w-5 text-green-600" />
                    Thanh toán thành công
                  </>
                ) : (
                  "Thanh toán"
                )}
              </DialogTitle>
            </DialogHeader>

            {paymentSuccess ? (
              <div className="space-y-6 py-4">
                <div className="text-center">
                  <p className="text-sm text-muted-foreground">Mã hóa đơn</p>
                  <p className="text-2xl font-bold text-primary">
                    {paymentSuccess.invoiceNumber}
                  </p>
                </div>
                <div className="bg-green-50 dark:bg-green-950/20 rounded-xl p-4 text-center">
                  <p className="text-sm text-green-600 dark:text-green-400">
                    Tiền thừa
                  </p>
                  <p className="text-3xl font-bold text-green-600 dark:text-green-400">
                    {formatCurrency(paymentSuccess.change)}
                  </p>
                </div>
                <Button className="w-full rounded-xl" onClick={closePaymentDialog}>
                  Đóng
                </Button>
              </div>
            ) : (
              <div className="space-y-6 py-4">
                {/* Order Summary */}
                <div className="bg-muted/30 rounded-xl p-4 space-y-2">
                  <div className="flex justify-between text-sm">
                    <span>Tạm tính</span>
                    <span>{formatCurrency(subtotal)}</span>
                  </div>
                  {discount > 0 && (
                    <div className="flex justify-between text-sm text-green-600">
                      <span>Giảm giá</span>
                      <span>-{formatCurrency(discount)}</span>
                    </div>
                  )}
                  <div className="flex justify-between font-medium pt-2 border-t">
                    <span>Tổng cộng</span>
                    <span className="text-primary">{formatCurrency(totalAmount)}</span>
                  </div>
                </div>

                {/* Customer */}
                <div className="space-y-2">
                  <Label>Khách hàng (tùy chọn)</Label>
                  <Select
                    value={paymentData.customerId || "none"}
                    onValueChange={(value) =>
                      setPaymentData({
                        ...paymentData,
                        customerId: value === "none" ? "" : value,
                      })
                    }
                  >
                    <SelectTrigger className="rounded-xl">
                      <SelectValue placeholder="Chọn khách hàng" />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="none">Khách lẻ</SelectItem>
                      {customers.map((customer) => (
                        <SelectItem key={customer.id} value={customer.id}>
                          {customer.name}
                          {customer.phone && ` - ${customer.phone}`}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>

                {/* Payment Method */}
                <div className="space-y-2">
                  <Label>Phương thức thanh toán</Label>
                  <Select
                    value={paymentData.paymentMethod}
                    onValueChange={(value) =>
                      setPaymentData({ ...paymentData, paymentMethod: value })
                    }
                  >
                    <SelectTrigger className="rounded-xl">
                      <SelectValue />
                    </SelectTrigger>
                    <SelectContent>
                      <SelectItem value="CASH">Tiền mặt</SelectItem>
                      <SelectItem value="BANK_TRANSFER">Chuyển khoản</SelectItem>
                      <SelectItem value="CARD">Thẻ</SelectItem>
                      <SelectItem value="E_WALLET">Ví điện tử</SelectItem>
                    </SelectContent>
                  </Select>
                </div>

                {/* Cash Received */}
                {paymentData.paymentMethod === "CASH" && (
                  <div className="space-y-2">
                    <Label>Tiền khách đưa</Label>
                    <Input
                      type="number"
                      placeholder="Nhập số tiền"
                      className="rounded-xl text-lg"
                      value={paymentData.cashReceived}
                      onChange={(e) =>
                        setPaymentData({
                          ...paymentData,
                          cashReceived: e.target.value,
                        })
                      }
                    />
                    {change >= 0 && cashReceived > 0 && (
                      <div className="text-sm text-green-600">
                        Tiền thừa: {formatCurrency(change)}
                      </div>
                    )}
                    {change < 0 && cashReceived > 0 && (
                      <div className="text-sm text-destructive">
                        Tiền thiếu: {formatCurrency(Math.abs(change))}
                      </div>
                    )}
                  </div>
                )}

                {/* Notes */}
                <div className="space-y-2">
                  <Label>Ghi chú</Label>
                  <Input
                    placeholder="Ghi chú (tùy chọn)"
                    className="rounded-xl"
                    value={paymentData.notes}
                    onChange={(e) =>
                      setPaymentData({ ...paymentData, notes: e.target.value })
                    }
                  />
                </div>
              </div>
            )}

            {!paymentSuccess && (
              <DialogFooter>
                <Button
                  variant="outline"
                  onClick={closePaymentDialog}
                  className="rounded-xl"
                >
                  Hủy
                </Button>
                <Button
                  onClick={handleCompletePayment}
                  disabled={
                    isSubmitting ||
                    (paymentData.paymentMethod === "CASH" && cashReceived < totalAmount)
                  }
                  className="rounded-xl"
                >
                  {isSubmitting ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      Đang xử lý...
                    </>
                  ) : (
                    "Hoàn thành"
                  )}
                </Button>
              </DialogFooter>
            )}
          </DialogContent>
        </Dialog>
      </div>
    </MainLayout>
  );
}
