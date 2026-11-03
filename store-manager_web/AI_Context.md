# AI_Context.md

## 1. Tổng quan dự án

Tên dự án:
AI Grocery Store Management System

Mục tiêu:
Xây dựng hệ thống quản lý cửa hàng tạp hóa có tích hợp AI OCR để tự động nhận diện hóa đơn nhập hàng từ ảnh và điền dữ liệu vào form nhập hàng.

Người dùng vẫn có thể chỉnh sửa hoặc nhập tay dữ liệu.

---

## 2. Công nghệ sử dụng

### Frontend

* Next.js 15
* TypeScript
* TailwindCSS
* Shadcn UI

### Backend

* Next.js API Route / Server Actions

### Database

* PostgreSQL

### ORM

* Prisma ORM

### Container

* Docker
* Docker Compose

### AI OCR

* 9Router API

### Authentication

* NextAuth hoặc JWT

---

## 3. Chức năng hệ thống

### 3.1 Quản lý sản phẩm

* Thêm sản phẩm
* Cập nhật sản phẩm
* Xóa sản phẩm
* Tìm kiếm sản phẩm
* Quản lý tồn kho
* Danh mục sản phẩm

Thông tin sản phẩm:

* Tên sản phẩm
* Mã sản phẩm
* Giá nhập
* Giá bán
* Số lượng tồn kho
* Danh mục
* Ngày tạo

---

### 3.2 Nhập hàng

Người dùng có thể:

* Nhập tay
* Upload ảnh hóa đơn

Thông tin hóa đơn:

* Tên sản phẩm
* Số lượng
* Đơn giá
* Ngày nhập

Hệ thống:

* OCR nhận diện hóa đơn
* AI phân tích nội dung
* Tự động điền form nhập hàng

Người dùng:

* Có thể chỉnh sửa dữ liệu AI trước khi lưu.

---

### 3.3 Bán hàng

* Tạo hóa đơn bán hàng
* Chọn sản phẩm
* Trừ số lượng tồn kho
* Tính tổng tiền
* Lưu lịch sử bán hàng

---

### 3.4 Quản lý tồn kho

* Theo dõi số lượng hàng hóa
* Cảnh báo sắp hết hàng
* Thống kê nhập xuất tồn

---

### 3.5 AI OCR

Hệ thống hỗ trợ:

* Upload ảnh hóa đơn
* Gửi ảnh tới 9Router AI
* Cho phép người dùng chọn model AI

Ví dụ model:

* GPT-4o
* Claude
* Gemini
* Mistral OCR

AI trả về:

* Danh sách sản phẩm
* Số lượng
* Đơn giá
* Ngày nhập

---

## 4. Kiến trúc hệ thống

Người dùng
↓
Frontend Next.js
↓
API Route
↓
AI OCR Service (9Router)
↓
Parse JSON
↓
Autofill Form
↓
Save PostgreSQL

---

## 5. Database chính

### Product

* id
* name
* sku
* importPrice
* sellPrice
* stockQuantity
* categoryId
* createdAt

### Category

* id
* name

### ImportInvoice

* id
* importDate
* totalAmount
* createdAt

### ImportInvoiceItem

* id
* invoiceId
* productName
* quantity
* unitPrice

### SaleInvoice

* id
* totalAmount
* createdAt

### SaleInvoiceItem

* id
* saleInvoiceId
* productId
* quantity
* sellPrice

---

## 6. AI OCR Flow

### Step 1

User upload invoice image

### Step 2

Backend gửi ảnh tới 9Router

### Step 3

AI OCR đọc nội dung hóa đơn

### Step 4

AI trả JSON:

```json
{
  "items": [
    {
      "name": "Coca Cola",
      "quantity": 5,
      "unitPrice": 12000
    }
  ],
  "importDate": "2026-05-28"
}
```

### Step 5

Frontend autofill form

### Step 6

User xác nhận và lưu

---

## 7. Mục tiêu đồ án

* Xây dựng website quản lý cửa hàng tạp hóa
* Tích hợp AI OCR thực tế
* Tự động hóa nhập liệu
* Giảm thao tác nhập tay
* Tăng tốc độ xử lý hóa đơn

---

## 8. Phạm vi dự án

Trong phạm vi đồ án:

* Chỉ xử lý hóa đơn đơn giản
* OCR tiếng Việt cơ bản
* Không train AI model riêng
* Sử dụng AI API từ 9Router

---

## 9. Hướng mở rộng tương lai

* Barcode Scanner
* AI nhận diện sản phẩm
* Dashboard thống kê nâng cao
* Mobile App
* Multi-store management
* AI inventory prediction
