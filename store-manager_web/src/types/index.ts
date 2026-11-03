export interface Product {
  id: string;
  name: string;
  sku: string;
  importPrice: number;
  sellPrice: number;
  stockQuantity: number;
  categoryId: string;
  category?: Category;
  createdAt: Date;
  updatedAt: Date;
}

export interface Category {
  id: string;
  name: string;
  products?: Product[];
}

export interface ImportInvoice {
  id: string;
  importDate: Date;
  totalAmount: number;
  items: ImportInvoiceItem[];
  createdAt: Date;
  updatedAt: Date;
}

export interface ImportInvoiceItem {
  id: string;
  invoiceId: string;
  productName: string;
  quantity: number;
  unitPrice: number;
}

export interface SaleInvoice {
  id: string;
  totalAmount: number;
  items: SaleInvoiceItem[];
  createdAt: Date;
  updatedAt: Date;
}

export interface SaleInvoiceItem {
  id: string;
  saleInvoiceId: string;
  productId: string;
  product?: Product;
  quantity: number;
  sellPrice: number;
}

export interface OCRResult {
  items: OCRItem[];
  importDate?: string;
  confidence?: number;
}

export interface OCRItem {
  name: string;
  quantity: number;
  unitPrice: number;
  confidence?: number;
}

export interface AIModel {
  id: string;
  name: string;
  provider: string;
}
