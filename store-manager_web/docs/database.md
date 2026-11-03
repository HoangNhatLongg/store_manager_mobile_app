# Database Schema Documentation

Tài liệu này mô tả cấu trúc cơ sở dữ liệu của Store Manager.

## Entity Relationship Diagram

```
┌─────────────┐       ┌─────────────┐       ┌─────────────┐
│    User     │──────<│ImportInvoice│──────<│  Supplier   │
└─────────────┘       └─────────────┘       └─────────────┘
       │                     │
       │                     │
       ▼                     ▼
┌─────────────┐       ┌─────────────────────┐
│SaleInvoice  │──────<│SaleInvoiceItem       │
└─────────────┘       └─────────────────────┘
       │                     │
       │                     ▼
       │              ┌─────────────┐
       ▼              │   Product   │
┌─────────────┐       └─────────────┘
│  Customer   │              ▲
└─────────────┘              │
                             │
              ┌──────────────┼──────────────┐
              ▼              ▼              ▼
       ┌─────────────┐ ┌───────────┐ ┌─────────────┐
       │  Category   │ │   Unit    │ │ProductUnit  │
       └─────────────┘ └───────────┘ └─────────────┘
```

## Tables

### 1. Users - Người dùng

| Column | Type | Constraints | Mô tả |
|---|---|---|---|
| id | VARCHAR(25) | PK, DEFAULT cuid() | ID người dùng |
| email | VARCHAR(255) | UNIQUE, NOT NULL | Email đăng nhập |
| password | VARCHAR(255) | NOT NULL | Mật khẩu (đã hash) |
| name | VARCHAR(255) | | Tên hiển thị |
| role | ENUM | DEFAULT STAFF | Vai trò: ADMIN, MANAGER, STAFF |
| createdAt | TIMESTAMP | DEFAULT NOW() | Ngày tạo |
| updatedAt | TIMESTAMP | | Ngày cập nhật |

### 2. Categories - Danh mục sản phẩm

| Column | Type | Constraints | Mô tả |
|---|---|---|---|
| id | VARCHAR(25) | PK | ID danh mục |
| name | VARCHAR(255) | NOT NULL | Tên danh mục |
| slug | VARCHAR(255) | UNIQUE | Slug URL |
| description | TEXT | | Mô tả |
| imageUrl | VARCHAR(500) | | URL hình ảnh |
| isActive | BOOLEAN | DEFAULT TRUE | Trạng thái hoạt động |
| createdAt | TIMESTAMP | | Ngày tạo |
| updatedAt | TIMESTAMP | | Ngày cập nhật |

### 3. Products - Sản phẩm

| Column | Type | Constraints | Mô tả |
|---|---|---|---|
| id | VARCHAR(25) | PK | ID sản phẩm |
| name | VARCHAR(255) | NOT NULL | Tên sản phẩm |
| slug | VARCHAR(255) | UNIQUE | Slug URL |
| sku | VARCHAR(100) | UNIQUE | Mã sản phẩm |
| barcode | VARCHAR(100) | UNIQUE | Mã vạch |
| description | TEXT | | Mô tả |
| imageUrl | VARCHAR(500) | | URL hình ảnh |
| importPrice | DECIMAL(12,2) | NOT NULL | Giá nhập |
| previousImportPrice | DECIMAL(12,2) | | Giá nhập trước đó |
| sellPrice | DECIMAL(12,2) | NOT NULL | Giá bán |
| previousSellPrice | DECIMAL(12,2) | | Giá bán trước đó |
| stock | INT | DEFAULT 0 | Số lượng tồn kho |
| minStock | INT | DEFAULT 10 | Tồn kho tối thiểu |
| isActive | BOOLEAN | DEFAULT TRUE | Trạng thái |
| categoryId | VARCHAR(25) | FK | ID danh mục |
| createdAt | TIMESTAMP | | Ngày tạo |
| updatedAt | TIMESTAMP | | Ngày cập nhật |

### 4. Units - Đơn vị tính

| Column | Type | Constraints | Mô tả |
|---|---|---|---|
| id | VARCHAR(25) | PK | ID đơn vị |
| name | VARCHAR(100) | NOT NULL | Tên: Lon, Thùng, Chai, Kg |
| abbreviation | VARCHAR(20) | UNIQUE | Viết tắt: lon, thung, chai, kg |
| isBaseUnit | BOOLEAN | DEFAULT TRUE | Là đơn vị cơ bản |
| createdAt | TIMESTAMP | | Ngày tạo |

### 5. ProductUnits - Liên kết sản phẩm - đơn vị

| Column | Type | Constraints | Mô tả |
|---|---|---|---|
| id | VARCHAR(25) | PK | ID |
| productId | VARCHAR(25) | FK, NOT NULL | ID sản phẩm |
| unitId | VARCHAR(25) | FK, NOT NULL | ID đơn vị |
| conversionQty | INT | DEFAULT 1 | Số lượng quy đổi (1 thùng = 24 lon) |
| price | DECIMAL(12,2) | NOT NULL | Giá theo đơn vị |
| isDefault | BOOLEAN | DEFAULT FALSE | Là đơn vị mặc định |
| createdAt | TIMESTAMP | | Ngày tạo |

**Unique constraint**: (productId, unitId)

### 6. Suppliers - Nhà cung cấp

| Column | Type | Constraints | Mô tả |
|---|---|---|---|
| id | VARCHAR(25) | PK | ID nhà cung cấp |
| name | VARCHAR(255) | NOT NULL | Tên |
| phone | VARCHAR(20) | | Số điện thoại |
| email | VARCHAR(255) | | Email |
| address | TEXT | | Địa chỉ |
| isActive | BOOLEAN | DEFAULT TRUE | Trạng thái |
| createdAt | TIMESTAMP | | Ngày tạo |
| updatedAt | TIMESTAMP | | Ngày cập nhật |

### 7. Customers - Khách hàng

| Column | Type | Constraints | Mô tả |
|---|---|---|---|
| id | VARCHAR(25) | PK | ID khách hàng |
| name | VARCHAR(255) | NOT NULL | Tên |
| phone | VARCHAR(20) | | Số điện thoại |
| email | VARCHAR(255) | | Email |
| address | TEXT | | Địa chỉ |
| points | INT | DEFAULT 0 | Điểm tích lũy |
| isActive | BOOLEAN | DEFAULT TRUE | Trạng thái |
| createdAt | TIMESTAMP | | Ngày tạo |
| updatedAt | TIMESTAMP | | Ngày cập nhật |

### 8. ImportInvoices - Hóa đơn nhập hàng

| Column | Type | Constraints | Mô tả |
|---|---|---|---|
| id | VARCHAR(25) | PK | ID hóa đơn |
| invoiceNumber | VARCHAR(100) | UNIQUE | Số hóa đơn |
| supplierId | VARCHAR(25) | FK | ID nhà cung cấp |
| totalAmount | DECIMAL(15,2) | NOT NULL | Tổng tiền |
| notes | TEXT | | Ghi chú |
| status | ENUM | DEFAULT PENDING | Trạng thái: PENDING, COMPLETED, CANCELLED |
| createdById | VARCHAR(25) | FK, NOT NULL | Người tạo |
| createdAt | TIMESTAMP | | Ngày tạo |
| updatedAt | TIMESTAMP | | Ngày cập nhật |

### 9. ImportInvoiceItems - Chi tiết nhập hàng

| Column | Type | Constraints | Mô tả |
|---|---|---|---|
| id | VARCHAR(25) | PK | ID |
| importInvoiceId | VARCHAR(25) | FK, NOT NULL | ID hóa đơn nhập |
| productId | VARCHAR(25) | FK, NOT NULL | ID sản phẩm |
| quantity | INT | NOT NULL | Số lượng |
| unitPrice | DECIMAL(12,2) | NOT NULL | Đơn giá |
| previousUnitPrice | DECIMAL(12,2) | | Giá trước đó |
| totalPrice | DECIMAL(15,2) | NOT NULL | Thành tiền |
| createdAt | TIMESTAMP | | Ngày tạo |

### 10. SaleInvoices - Hóa đơn bán hàng

| Column | Type | Constraints | Mô tả |
|---|---|---|---|
| id | VARCHAR(25) | PK | ID hóa đơn |
| invoiceNumber | VARCHAR(100) | UNIQUE | Số hóa đơn |
| customerId | VARCHAR(25) | FK | ID khách hàng |
| subtotal | DECIMAL(15,2) | NOT NULL | Tổng phụ |
| discount | DECIMAL(15,2) | DEFAULT 0 | Giảm giá |
| totalAmount | DECIMAL(15,2) | NOT NULL | Tổng tiền |
| paymentMethod | ENUM | DEFAULT CASH | Thanh toán: CASH, BANK_TRANSFER, CARD, E_WALLET |
| notes | TEXT | | Ghi chú |
| status | ENUM | DEFAULT COMPLETED | Trạng thái: PENDING, COMPLETED, CANCELLED |
| createdById | VARCHAR(25) | FK, NOT NULL | Người tạo |
| createdAt | TIMESTAMP | | Ngày tạo |
| updatedAt | TIMESTAMP | | Ngày cập nhật |

### 11. SaleInvoiceItems - Chi tiết bán hàng

| Column | Type | Constraints | Mô tả |
|---|---|---|---|
| id | VARCHAR(25) | PK | ID |
| saleInvoiceId | VARCHAR(25) | FK, NOT NULL | ID hóa đơn bán |
| productId | VARCHAR(25) | FK, NOT NULL | ID sản phẩm |
| quantity | INT | NOT NULL | Số lượng |
| unitPrice | DECIMAL(12,2) | NOT NULL | Đơn giá bán |
| importPrice | DECIMAL(12,2) | NOT NULL | Giá nhập tại thời điểm bán |
| totalPrice | DECIMAL(15,2) | NOT NULL | Thành tiền |
| createdAt | TIMESTAMP | | Ngày tạo |

### 12. Settings - Cài đặt hệ thống

| Column | Type | Constraints | Mô tả |
|---|---|---|---|
| id | VARCHAR(25) | PK | ID |
| key | VARCHAR(100) | UNIQUE | Key cài đặt |
| value | TEXT | NOT NULL | Giá trị |
| type | VARCHAR(50) | DEFAULT string | Kiểu dữ liệu |
| group | VARCHAR(50) | DEFAULT general | Nhóm cài đặt |
| isPublic | BOOLEAN | DEFAULT FALSE | Công khai |
| createdAt | TIMESTAMP | | Ngày tạo |
| updatedAt | TIMESTAMP | | Ngày cập nhật |

### 13. AuditLogs - Nhật ký hệ thống

| Column | Type | Constraints | Mô tả |
|---|---|---|---|
| id | VARCHAR(25) | PK | ID |
| userId | VARCHAR(25) | | Người thực hiện |
| action | VARCHAR(50) | NOT NULL | Hành động: CREATE, UPDATE, DELETE |
| entity | VARCHAR(50) | NOT NULL | Entity bị tác động |
| entityId | VARCHAR(25) | | ID entity |
| changes | JSONB | | Chi tiết thay đổi |
| ipAddress | VARCHAR(50) | | IP người dùng |
| userAgent | TEXT | | User Agent |
| createdAt | TIMESTAMP | | Thời gian |

## Enums

### UserRole
- `ADMIN` - Quản trị viên
- `MANAGER` - Quản lý
- `STAFF` - Nhân viên

### InvoiceStatus
- `PENDING` - Đang xử lý
- `COMPLETED` - Hoàn thành
- `CANCELLED` - Đã hủy

### PaymentMethod
- `CASH` - Tiền mặt
- `BANK_TRANSFER` - Chuyển khoản
- `CARD` - Thẻ
- `E_WALLET` - Ví điện tử

## Indexes

Các indexes được tạo tự động:

- Primary keys trên tất cả các bảng
- Unique indexes trên: email (users), slug (categories, products), sku (products), barcode (products)
- Unique constraint trên: invoiceNumber (import_invoices, sale_invoices), key (settings)
- Unique constraint trên: (productId, unitId) trong product_units

## Relationships

### User relationships
- User -> ImportInvoice (1:N)
- User -> SaleInvoice (1:N)

### Category relationships
- Category -> Product (1:N)

### Product relationships
- Product -> Category (N:1)
- Product -> ImportInvoiceItem (1:N)
- Product -> SaleInvoiceItem (1:N)
- Product -> ProductUnit (1:N)

### Unit relationships
- Unit -> ProductUnit (1:N)

### Supplier relationships
- Supplier -> ImportInvoice (1:N)

### Customer relationships
- Customer -> SaleInvoice (1:N)

### Invoice relationships
- ImportInvoice -> User (N:1)
- ImportInvoice -> Supplier (N:1)
- ImportInvoice -> ImportInvoiceItem (1:N)
- SaleInvoice -> User (N:1)
- SaleInvoice -> Customer (N:1)
- SaleInvoice -> SaleInvoiceItem (1:N)

## Notes

1. **Decimal precision**: Sử dụng DECIMAL(12,2) cho giá, DECIMAL(15,2) cho tổng tiền
2. **Soft delete**: Không có soft delete, xóa trực tiếp hoặc qua cascade
3. **Audit logging**: Tất cả thay đổi được ghi log vào bảng audit_logs
4. **Timestamps**: Mặc định sử dụng timezone UTC
5. **Stock management**: Stock được cập nhật khi nhập hàng và bán hàng
