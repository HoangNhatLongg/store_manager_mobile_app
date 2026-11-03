"use client";

import { useState, useRef, useEffect } from "react";
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
} from "@/components/ui/dialog";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import {
  Plus,
  Trash2,
  Loader2,
  ArrowLeft,
  History,
  Bot,
  Save,
  Search,
  Package,
  Upload,
  X,
  Image as ImageIcon,
  Sparkles,
  Check,
  AlertCircle,
} from "lucide-react";
import Link from "next/link";

interface Supplier {
  id: string;
  name: string;
}

interface Category {
  id: string;
  name: string;
  slug: string;
}

interface Product {
  id: string;
  name: string;
  sku: string;
  importPrice: string;
  categoryId?: string;
  category?: Category;
  units?: ProductUnit[];
}

interface ProductUnit {
  id: string;
  unitId: string;
  conversionQty: number;
  price: string;
  isDefault: boolean;
  isPurchase: boolean;
  unit: {
    id: string;
    name: string;
    abbreviation: string;
    isBaseUnit: boolean;
  };
}

interface ImportItem {
  id: string;
  productId: string | null;
  productName: string;
  quantity: number;
  unitPrice: number;
  categoryId?: string;
  categoryName?: string;
  /** Id đơn vị gốc dùng khi nhập hàng. */
  unitId?: string;
  /** Hệ số quy đổi của đơn vị gốc. */
  conversionQty?: number;
  /** Tên đơn vị gốc, dùng để hiển thị. */
  unitName?: string;
  /** Id đơn vị bán lẻ cơ bản. */
  baseUnitId?: string;
  /** Tên đơn vị bán lẻ cơ bản. */
  baseUnitName?: string;
}

interface SimilarProduct {
  id: string;
  name: string;
  sku: string;
  importPrice: string;
}

interface OCRResult {
  items: Array<{
    productName: string;
    quantity: number;
    unitPrice: number;
    category?: string;
  }>;
  confidence: number;
}

const MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
const ACCEPTED_TYPES = ["image/jpeg", "image/png", "image/webp", "image/jpg"];

export default function ImportPage() {
  const router = useRouter();
  const fileInputRef = useRef<HTMLInputElement>(null);
  
  const [suppliers, setSuppliers] = useState<Supplier[]>([]);
  const [products, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<Category[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Form state
  const [supplierId, setSupplierId] = useState<string>("");
  const [items, setItems] = useState<ImportItem[]>([
    { id: "1", productId: null, productName: "", quantity: 1, unitPrice: 0 }
  ]);

  // Dialog state
  const [showSimilarDialog, setShowSimilarDialog] = useState(false);
  const [similarProducts, setSimilarProducts] = useState<SimilarProduct[]>([]);
  const [tempItemId, setTempItemId] = useState<string>("");
  const [tempProductName, setTempProductName] = useState<string>("");

  // OCR Dialog state
  const [isOcrDialogOpen, setIsOcrDialogOpen] = useState(false);
  const [isOcrProcessing, setIsOcrProcessing] = useState(false);
  const [ocrError, setOcrError] = useState<string | null>(null);
  const [selectedImage, setSelectedImage] = useState<File | null>(null);
  const [imagePreview, setImagePreview] = useState<string | null>(null);

  // Fetch data
  async function fetchData() {
    setIsLoading(true);
    try {
      const [suppliersRes, productsRes, categoriesRes] = await Promise.all([
        fetch("/api/suppliers"),
        fetch("/api/products?limit=1000"),
        fetch("/api/categories")
      ]);
      
      const suppliersData = await suppliersRes.json();
      const productsData = await productsRes.json();
      const categoriesData = await categoriesRes.json();
      
      if (Array.isArray(suppliersData)) {
        setSuppliers(suppliersData);
      }
      if (productsData.products && Array.isArray(productsData.products)) {
        setProducts(productsData.products);
      }
      if (Array.isArray(categoriesData)) {
        setCategories(categoriesData);
      }
    } catch (error) {
      console.error("Error fetching data:", error);
    } finally {
      setIsLoading(false);
    }
  }

  // Initial load
  useEffect(() => {
    fetchData();
  }, []);

  function addRow() {
    setItems(prev => [
      ...prev,
      { id: Date.now().toString(), productId: null, productName: "", quantity: 1, unitPrice: 0 }
    ]);
  }

  function removeRow(id: string) {
    if (items.length > 1) {
      setItems(prev => prev.filter(item => item.id !== id));
    }
  }

  function updateItemName(id: string, name: string) {
    setItems(prev => prev.map(item => {
      if (item.id !== id) return item;

      const match = products.find(p => p.name.toLowerCase() === name.toLowerCase());
      if (match) {
        return {
          ...item,
          productName: name,
          productId: match.id,
          unitPrice: parseFloat(match.importPrice) || 0,
          ...donViGocMacDinh(match),
        };
      }
      return { ...item, productName: name, productId: null };
    }));
  }

  /**
   * Chọn đơn vị gốc mặc định của sản phẩm.
   *
   * Ưu tiên đơn vị được cấu hình là đơn vị gốc, sau đó tới đơn vị bán lẻ
   * cơ bản. Kết quả gồm đơn vị gốc, hệ số quy đổi và đơn vị bán lẻ cơ bản.
   */
  function donViGocMacDinh(product: Product) {
    const units = product.units || [];
    const donViGoc = units.find((u) => u.isPurchase)
      || units.find((u) => u.conversionQty === 1)
      || units.find((u) => u.isDefault)
      || units[0];
    const donViCoBan = units.find((u) => u.conversionQty === 1)
      || units.find((u) => u.isDefault);

    return {
      unitId: donViGoc?.unitId,
      conversionQty: donViGoc?.conversionQty ?? 1,
      unitName: donViGoc?.unit.name,
      baseUnitId: donViCoBan?.unitId,
      baseUnitName: donViCoBan?.unit.abbreviation,
    };
  }

  /**
   * Đổi đơn vị gốc của một dòng hàng.
   *
   * Khi đổi đơn vị, số lượng nhập và đơn giá được hiểu theo đơn vị mới nên
   * giá được tính lại từ giá của đơn vị bán lẻ cơ bản.
   */
  function updateItemUnit(id: string, unitId: string) {
    setItems(prev => prev.map(item => {
      if (item.id !== id) return item;

      const product = products.find(p => p.id === item.productId);
      if (!product) return item;

      const units = product.units || [];
      const donViChon = units.find(u => u.unitId === unitId);
      if (!donViChon) return item;

      const donViCoBan = units.find(u => u.conversionQty === 1) || units.find(u => u.isDefault);
      const giaCoBan = donViCoBan ? parseFloat(donViCoBan.price) : parseFloat(product.importPrice);

      return {
        ...item,
        unitId: donViChon.unitId,
        conversionQty: donViChon.conversionQty,
        unitName: donViChon.unit.name,
        baseUnitId: donViCoBan?.unitId,
        baseUnitName: donViCoBan?.unit.abbreviation,
        unitPrice: donViChon.conversionQty > 0
          ? Math.round(giaCoBan * donViChon.conversionQty)
          : item.unitPrice,
      };
    }));
  }

  /**
   * Danh sách đơn vị có thể chọn làm đơn vị gốc của một sản phẩm.
   */
  function donViCuaSanPham(productId: string | null): ProductUnit[] {
    if (!productId) return [];
    const product = products.find(p => p.id === productId);
    return product?.units || [];
  }

  function updateItemQuantity(id: string, qty: number) {
    setItems(prev => prev.map(item =>
      item.id === id ? { ...item, quantity: qty } : item
    ));
  }

  function updateItemPrice(id: string, price: number) {
    setItems(prev => prev.map(item =>
      item.id === id ? { ...item, unitPrice: price } : item
    ));
  }

  function updateItemCategory(id: string, categoryId: string) {
    const category = categories.find(c => c.id === categoryId);
    setItems(prev => prev.map(item =>
      item.id === id ? { ...item, categoryId, categoryName: category?.name } : item
    ));
  }

  function getTotal() {
    return items.reduce((sum, item) => sum + (item.quantity * item.unitPrice), 0);
  }

  function handleSubmit() {
    const validItems = items.filter(item => item.productName.trim() !== "");
    
    if (validItems.length === 0) {
      alert("Vui lòng nhập ít nhất một sản phẩm");
      return;
    }

    for (const item of validItems) {
      if (item.quantity <= 0) {
        alert("Số lượng phải lớn hơn 0");
        return;
      }
      if (item.unitPrice <= 0) {
        alert("Đơn giá phải lớn hơn 0");
        return;
      }
      // Sản phẩm đã có trong hệ thống bắt buộc phải chọn đơn vị gốc.
      if (item.productId && !item.unitId) {
        alert(`Sản phẩm "${item.productName}" chưa có đơn vị gốc để nhập hàng. Vui lòng chọn đơn vị.`);
        return;
      }
    }

    const uncheckedItems = validItems.filter(item => !item.productId);
    if (uncheckedItems.length > 0) {
      const first = uncheckedItems[0];
      const query = first.productName.toLowerCase();
      const similar = products
        .filter(p => p.name.toLowerCase().includes(query) || p.sku.toLowerCase().includes(query))
        .slice(0, 5);

      if (similar.length > 0) {
        setTempItemId(first.id);
        setTempProductName(first.productName);
        setSimilarProducts(similar);
        setShowSimilarDialog(true);
        return;
      }
    }

    submitImport(validItems);
  }

  async function submitImport(validItems: ImportItem[]) {
    setIsSubmitting(true);
    try {
      const res = await fetch("/api/imports", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          supplierId: supplierId || null,
          items: validItems.map(item => ({
            productId: item.productId || undefined,
            productName: item.productName,
            quantity: item.quantity,
            unitPrice: item.unitPrice,
            categoryId: item.categoryId || undefined,
            unitId: item.unitId || undefined,
            baseUnitId: item.baseUnitId || undefined,
            conversionQty: item.conversionQty || 1,
          })),
          status: "COMPLETED",
        }),
      });

      if (!res.ok) {
        const error = await res.json();
        alert(error.error || "Có lỗi xảy ra");
        return;
      }

      const data = await res.json();
      alert("Nhập hàng thành công!");
      router.push(`/import/${data.id}`);
    } catch (error) {
      console.error("Error:", error);
      alert("Có lỗi xảy ra");
    } finally {
      setIsSubmitting(false);
    }
  }

  function selectSimilarProduct(product: SimilarProduct) {
    setItems(prev => prev.map(item =>
      item.id === tempItemId
        ? {
            ...item,
            productId: product.id,
            productName: product.name,
            unitPrice: parseFloat(product.importPrice) || 0,
            ...donViGocMacDinh(products.find(p => p.id === product.id) || (product as unknown as Product)),
          }
        : item
    ));
    setShowSimilarDialog(false);
  }

  // Image handling
  function handleImageSelect(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) return;

    // Validate file type
    if (!ACCEPTED_TYPES.includes(file.type)) {
      setOcrError("Chỉ chấp nhận file ảnh (JPG, PNG, WebP)");
      return;
    }

    // Validate file size
    if (file.size > MAX_FILE_SIZE) {
      setOcrError("File quá lớn. Vui lòng chọn file nhỏ hơn 10MB");
      return;
    }

    setOcrError(null);
    setSelectedImage(file);
    
    // Create preview
    const reader = new FileReader();
    reader.onload = (e) => {
      setImagePreview(e.target?.result as string);
    };
    reader.readAsDataURL(file);
  }

  function clearImage() {
    setSelectedImage(null);
    setImagePreview(null);
    setOcrError(null);
    if (fileInputRef.current) {
      fileInputRef.current.value = "";
    }
  }

  async function processOCR() {
    if (!selectedImage) return;

    setIsOcrProcessing(true);
    setOcrError(null);

    try {
      const formData = new FormData();
      formData.append("image", selectedImage);

      const res = await fetch("/api/ocr/process", {
        method: "POST",
        body: formData,
      });

      if (!res.ok) {
        const error = await res.json();
        throw new Error(error.error || "Có lỗi xảy ra");
      }

      const data: OCRResult = await res.json();

      // Apply OCR results to form
      if (data.items && data.items.length > 0) {
        const newItems: ImportItem[] = data.items.map((item, index) => {
          // Try to match product
          const match = products.find(
            p => p.name.toLowerCase().includes(item.productName.toLowerCase()) ||
                 item.productName.toLowerCase().includes(p.name.toLowerCase())
          );

          // Try to match category from AI response
          let matchedCategoryId: string | undefined;
          if (item.category && categories.length > 0) {
            // Try exact match first
            const exactMatch = categories.find(
              c => c.name.toLowerCase() === item.category?.toLowerCase()
            );
            if (exactMatch) {
              matchedCategoryId = exactMatch.id;
            } else {
              // Try partial match
              const partialMatch = categories.find(
                c => c.name.toLowerCase().includes(item.category!.toLowerCase()) ||
                     item.category!.toLowerCase().includes(c.name.toLowerCase())
              );
              if (partialMatch) {
                matchedCategoryId = partialMatch.id;
              }
            }
          }

          return {
            id: `ocr-${Date.now()}-${index}`,
            productId: match?.id || null,
            productName: item.productName,
            quantity: item.quantity,
            unitPrice: item.unitPrice,
            categoryId: matchedCategoryId,
            categoryName: matchedCategoryId ? categories.find(c => c.id === matchedCategoryId)?.name : undefined,
            ...(match ? donViGocMacDinh(match) : {}),
          };
        });

        setItems(newItems);
      } else {
        setOcrError("Không tìm thấy dữ liệu hóa đơn trong ảnh");
      }

      setIsOcrDialogOpen(false);
    } catch (error: any) {
      setOcrError(error.message || "Có lỗi xảy ra");
    } finally {
      setIsOcrProcessing(false);
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

  return (
    <MainLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-4">
            <Link href="/import/history">
              <Button variant="ghost" size="icon" className="rounded-xl">
                <ArrowLeft className="h-5 w-5" />
              </Button>
            </Link>
            <h1 className="text-3xl font-semibold">Nhập hàng</h1>
          </div>
          <div className="flex gap-2">
            <Button 
              variant="outline" 
              className="rounded-xl"
              onClick={() => setIsOcrDialogOpen(true)}
            >
              <Bot className="mr-2 h-4 w-4" />
              Nhận diện ảnh
            </Button>
            <Link href="/import/history">
              <Button variant="outline" className="rounded-xl">
                <History className="mr-2 h-4 w-4" />
                Lịch sử
              </Button>
            </Link>
          </div>
        </div>

        <div className="grid gap-6 lg:grid-cols-3">
          {/* Main Form */}
          <div className="lg:col-span-2 space-y-6">
            {/* Supplier */}
            <div className="bg-card rounded-2xl p-6 space-y-4">
              <h2 className="text-lg font-medium">Thông tin phiếu nhập</h2>
              <div className="max-w-sm">
                <Label>Nhà cung cấp</Label>
                <Select value={supplierId} onValueChange={setSupplierId}>
                  <SelectTrigger className="rounded-xl mt-2">
                    <SelectValue placeholder="Chọn nhà cung cấp" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="none">Không có</SelectItem>
                    {suppliers.map(s => (
                      <SelectItem key={s.id} value={s.id}>{s.name}</SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
            </div>

            {/* Products */}
            <div className="bg-card rounded-2xl p-6 space-y-4">
              <div className="flex items-center justify-between">
                <h2 className="text-lg font-medium">Danh sách sản phẩm</h2>
                <Button onClick={addRow} className="rounded-xl">
                  <Plus className="h-4 w-4 mr-2" />
                  Thêm hàng
                </Button>
              </div>

              {/* Table Header */}
              <div className="flex gap-4 px-4 py-2 bg-muted rounded-xl text-sm font-medium text-muted-foreground">
                <div className="flex-1">Tên sản phẩm</div>
                <div className="w-40">Đơn vị gốc</div>
                <div className="w-28">Danh mục</div>
                <div className="w-20 text-center">SL</div>
                <div className="w-32 text-right">Đơn giá</div>
                <div className="w-28 text-right">Thành tiền</div>
                <div className="w-10"></div>
              </div>

              {/* Items */}
              <div className="space-y-2">
                {items.map((item, index) => (
                  <div key={item.id} className="flex gap-4 items-center p-3 bg-muted/50 rounded-xl">
                    <div className="flex-1">
                      <Input
                        value={item.productName}
                        onChange={(e) => updateItemName(item.id, e.target.value)}
                        placeholder={`Tên sản phẩm ${index + 1}`}
                        className="rounded-xl"
                      />
                      {item.productId ? (
                        <p className="text-xs text-green-600 mt-1 flex items-center gap-1">
                          <Check className="h-3 w-3" />
                          Đã khớp với sản phẩm
                        </p>
                      ) : item.productName ? (
                        <p className="text-xs text-amber-600 mt-1 flex items-center gap-1">
                          <AlertCircle className="h-3 w-3" />
                          Sản phẩm mới
                        </p>
                      ) : null}
                    </div>
                    <div className="w-40">
                      <Select
                        value={item.unitId || "none"}
                        onValueChange={(value) => {
                          if (value !== "none") {
                            updateItemUnit(item.id, value);
                          }
                        }}
                        disabled={!item.productId}
                      >
                        <SelectTrigger className="rounded-xl">
                          <SelectValue placeholder="Chọn đơn vị" />
                        </SelectTrigger>
                        <SelectContent>
                          {item.productId ? (
                            donViCuaSanPham(item.productId).map((donVi) => (
                              <SelectItem key={donVi.unitId} value={donVi.unitId}>
                                {donVi.unit.name}
                                {donVi.conversionQty > 1 ? ` (1 = ${donVi.conversionQty})` : ""}
                              </SelectItem>
                            ))
                          ) : (
                            <SelectItem value="none">Chưa chọn sản phẩm</SelectItem>
                          )}
                        </SelectContent>
                      </Select>
                      {item.conversionQty && item.conversionQty > 1 && (
                        <p className="text-xs text-muted-foreground mt-1">
                          = {item.quantity * item.conversionQty} {item.baseUnitName || "đơn vị lẻ"}
                        </p>
                      )}
                    </div>
                    <div className="w-28">
                      <Select 
                        value={item.categoryId || ""} 
                        onValueChange={(value) => updateItemCategory(item.id, value)}
                      >
                        <SelectTrigger className="rounded-xl">
                          <SelectValue placeholder="Chọn" />
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
                    <div className="w-20">
                      <Input
                        type="number"
                        min="1"
                        value={item.quantity}
                        onChange={(e) => updateItemQuantity(item.id, parseInt(e.target.value) || 1)}
                        className="rounded-xl text-center"
                      />
                    </div>
                    <div className="w-32">
                      <Input
                        type="number"
                        min="0"
                        value={item.unitPrice}
                        onChange={(e) => updateItemPrice(item.id, parseFloat(e.target.value) || 0)}
                        className="rounded-xl text-right"
                        placeholder="0"
                      />
                    </div>
                    <div className="w-28 text-right font-medium">
                      {(item.quantity * item.unitPrice).toLocaleString()} đ
                    </div>
                    <div className="w-10">
                      <Button
                        variant="ghost"
                        size="icon"
                        className="rounded-xl text-destructive"
                        onClick={() => removeRow(item.id)}
                        disabled={items.length === 1}
                      >
                        <Trash2 className="h-4 w-4" />
                      </Button>
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>

          {/* Summary */}
          <div>
            <div className="bg-card rounded-2xl p-6 space-y-4 sticky top-24">
              <h2 className="text-lg font-medium">Tổng quan</h2>
              <div className="space-y-2">
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Số hàng</span>
                  <span>{items.filter(i => i.productName).length}</span>
                </div>
                <div className="flex justify-between">
                  <span className="text-muted-foreground">Tổng số lượng</span>
                  <span>{items.filter(i => i.productName).reduce((s, i) => s + i.quantity, 0)}</span>
                </div>
                <div className="border-t pt-4 flex justify-between">
                  <span className="font-medium">Tổng tiền</span>
                  <span className="text-xl font-bold text-primary">{getTotal().toLocaleString()} đ</span>
                </div>
              </div>
              <Button
                className="w-full rounded-xl"
                size="lg"
                disabled={items.filter(i => i.productName).length === 0 || isSubmitting}
                onClick={handleSubmit}
              >
                {isSubmitting ? (
                  <>
                    <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                    Đang xử lý...
                  </>
                ) : (
                  <>
                    <Save className="mr-2 h-4 w-4" />
                    Lưu phiếu nhập
                  </>
                )}
              </Button>
            </div>
          </div>
        </div>
      </div>

      {/* Similar Products Dialog */}
      <Dialog open={showSimilarDialog} onOpenChange={setShowSimilarDialog}>
        <DialogContent className="rounded-2xl">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Search className="h-5 w-5" />
              Sản phẩm tương tự
            </DialogTitle>
          </DialogHeader>
          <p className="text-muted-foreground">
            Sản phẩm "{tempProductName}" chưa có. Chọn sản phẩm tương tự hoặc tạo mới.
          </p>
          <div className="space-y-2 max-h-60 overflow-y-auto">
            {similarProducts.map(product => (
              <button
                key={product.id}
                onClick={() => selectSimilarProduct(product)}
                className="w-full flex items-center gap-3 p-3 bg-muted/50 hover:bg-muted rounded-xl text-left"
              >
                <Package className="h-5 w-5 text-muted-foreground" />
                <div className="flex-1">
                  <p className="font-medium">{product.name}</p>
                  <p className="text-sm text-muted-foreground">SKU: {product.sku}</p>
                </div>
                <span className="font-medium">{parseFloat(product.importPrice).toLocaleString()} đ</span>
              </button>
            ))}
          </div>
          <div className="flex gap-2 pt-2">
            <Button
              variant="outline"
              className="flex-1 rounded-xl"
              onClick={() => setShowSimilarDialog(false)}
            >
              Tạo sản phẩm mới
            </Button>
            <Button
              variant="ghost"
              className="flex-1 rounded-xl"
              onClick={() => setShowSimilarDialog(false)}
            >
              Đóng
            </Button>
          </div>
        </DialogContent>
      </Dialog>

      {/* OCR Upload Dialog */}
      <Dialog open={isOcrDialogOpen} onOpenChange={setIsOcrDialogOpen}>
        <DialogContent className="rounded-2xl sm:max-w-lg">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Sparkles className="h-5 w-5 text-primary" />
              Nhận diện ảnh hóa đơn
            </DialogTitle>
          </DialogHeader>
          
          <div className="space-y-4 py-4">
            {/* Upload Area */}
            {!imagePreview ? (
              <div
                className="border-2 border-dashed border-muted-foreground/25 rounded-2xl p-8 text-center hover:border-primary/50 transition-colors cursor-pointer"
                onClick={() => fileInputRef.current?.click()}
              >
                <input
                  ref={fileInputRef}
                  type="file"
                  accept={ACCEPTED_TYPES.join(",")}
                  onChange={handleImageSelect}
                  className="hidden"
                />
                <Upload className="h-12 w-12 mx-auto mb-4 text-muted-foreground" />
                <p className="text-lg font-medium">Tải lên ảnh hóa đơn</p>
                <p className="text-sm text-muted-foreground mt-1">
                  Kéo thả hoặc click để chọn file
                </p>
                <p className="text-xs text-muted-foreground mt-2">
                  Chấp nhận: JPG, PNG, WebP (tối đa 10MB)
                </p>
              </div>
            ) : (
              <div className="relative rounded-2xl overflow-hidden bg-muted">
                <img
                  src={imagePreview}
                  alt="Preview"
                  className="w-full h-64 object-contain"
                />
                <Button
                  variant="destructive"
                  size="icon"
                  className="absolute top-2 right-2 rounded-full"
                  onClick={clearImage}
                >
                  <X className="h-4 w-4" />
                </Button>
              </div>
            )}

            {/* Error Message */}
            {ocrError && (
              <div className="flex items-center gap-2 p-3 bg-destructive/10 text-destructive rounded-xl">
                <AlertCircle className="h-4 w-4" />
                <p className="text-sm">{ocrError}</p>
              </div>
            )}

            {/* Tips */}
            <div className="bg-muted/50 rounded-xl p-4">
              <p className="text-sm font-medium mb-2">Mẹo để nhận diện tốt hơn:</p>
              <ul className="text-xs text-muted-foreground space-y-1">
                <li>• Sử dụng ảnh rõ nét, đủ ánh sáng</li>
                <li>• Chụp toàn bộ hóa đơn, không cắt góc</li>
                <li>• Ưu tiên hóa đơn có chữ in, tránh chữ viết tay</li>
              </ul>
            </div>
          </div>

          <div className="flex gap-2">
            <Button
              variant="outline"
              className="flex-1 rounded-xl"
              onClick={() => {
                setIsOcrDialogOpen(false);
                clearImage();
              }}
            >
              Hủy
            </Button>
            <Button
              className="flex-1 rounded-xl"
              disabled={!selectedImage || isOcrProcessing}
              onClick={processOCR}
            >
              {isOcrProcessing ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Đang xử lý...
                </>
              ) : (
                <>
                  <Sparkles className="mr-2 h-4 w-4" />
                  Nhận diện
                </>
              )}
            </Button>
          </div>
        </DialogContent>
      </Dialog>
    </MainLayout>
  );
}
