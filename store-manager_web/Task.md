# TASKS.md

# Giai đoạn 1 — Khởi tạo dự án

## Task 1 — Setup Next.js Project

### Mục tiêu

Khởi tạo project Next.js fullstack với TypeScript.

### Prompt

Tạo cấu trúc project Next.js 15 sử dụng App Router, TypeScript, TailwindCSS và ESLint cho hệ thống quản lý cửa hàng tạp hóa.

Yêu cầu:

* Cấu trúc thư mục sạch
* Hỗ trợ frontend + backend
* Có folder components, lib, services, prisma, app
* Cấu hình alias @/*
* Sử dụng App Router

---

## Task 2 — Setup Docker + PostgreSQL

### Prompt

Tạo docker-compose cho:

* PostgreSQL
* pgAdmin

Yêu cầu:

* Có volume persist dữ liệu
* Có biến môi trường
* Port dễ cấu hình

---

## Task 3 — Setup Prisma ORM

### Prompt

Cấu hình Prisma cho Next.js với PostgreSQL.

Yêu cầu:

* Tạo Prisma schema
* Kết nối database
* Setup Prisma Client
* Script migrate

---

# Giai đoạn 2 — Thiết kế Database

## Task 4 — Thiết kế schema database

### Prompt

Thiết kế Prisma schema cho hệ thống quản lý cửa hàng tạp hóa.

Bao gồm:

* Product
* Category
* ImportInvoice
* ImportInvoiceItem
* SaleInvoice
* SaleInvoiceItem

Yêu cầu:

* Có quan hệ rõ ràng
* Có createdAt, updatedAt
* Hỗ trợ thống kê tồn kho

---

# Giai đoạn 3 — Authentication

## Task 5 — Đăng nhập

### Prompt

Xây dựng chức năng đăng nhập bằng NextAuth hoặc JWT cho hệ thống quản lý cửa hàng.

Yêu cầu:

* Login
* Logout
* Session
* Protected route

---

# Giai đoạn 4 — Quản lý sản phẩm

## Task 6 — CRUD sản phẩm

### Prompt

Xây dựng chức năng CRUD sản phẩm.

Yêu cầu:

* Thêm sản phẩm
* Sửa sản phẩm
* Xóa sản phẩm
* Tìm kiếm
* Pagination

UI:

* Responsive
* Sử dụng Shadcn UI

---

## Task 7 — CRUD danh mục

### Prompt

Xây dựng chức năng quản lý danh mục sản phẩm.

Bao gồm:

* Thêm
* Sửa
* Xóa
* Danh sách danh mục

---

# Giai đoạn 5 — Nhập hàng

## Task 8 — Form nhập hàng thủ công

### Prompt

Xây dựng form nhập hàng cho cửa hàng tạp hóa.

Thông tin:

* Tên sản phẩm
* Số lượng
* Đơn giá
* Ngày nhập

Yêu cầu:

* Dynamic item rows
* Tính tổng tiền
* Validate dữ liệu

---

## Task 9 — Lưu hóa đơn nhập hàng

### Prompt

Xây dựng API lưu hóa đơn nhập hàng.

Yêu cầu:

* Lưu invoice
* Lưu invoice items
* Tăng tồn kho sản phẩm

---

# Giai đoạn 6 — Bán hàng

## Task 10 — Form bán hàng

### Prompt

Xây dựng giao diện bán hàng POS đơn giản.

Yêu cầu:

* Chọn sản phẩm
* Nhập số lượng
* Tính tổng tiền
* Responsive

---

## Task 11 — Xử lý bán hàng

### Prompt

Xây dựng API xử lý bán hàng.

Yêu cầu:

* Trừ tồn kho
* Kiểm tra đủ hàng
* Lưu hóa đơn bán

---

# Giai đoạn 7 — Quản lý tồn kho

## Task 12 — Dashboard tồn kho

### Prompt

Xây dựng dashboard quản lý tồn kho.

Bao gồm:

* Tổng sản phẩm
* Sản phẩm sắp hết
* Tổng nhập
* Tổng bán

---

# Giai đoạn 8 — AI OCR

## Task 13 — Upload ảnh hóa đơn

### Prompt

Xây dựng chức năng upload ảnh hóa đơn.

Yêu cầu:

* Preview ảnh
* Validate file
* Loading state

---

## Task 14 — Tích hợp 9Router AI OCR

### Prompt

Tích hợp 9Router AI OCR vào Next.js API Route.

Yêu cầu:

* Upload image
* Gửi ảnh tới AI model
* Nhận response JSON
* Handle error

---

## Task 15 — Prompt OCR chuẩn hóa dữ liệu

### Prompt

Viết prompt AI OCR để trích xuất dữ liệu hóa đơn nhập hàng.

Output JSON:

* productName
* quantity
* unitPrice
* importDate

Yêu cầu:

* Chỉ trả JSON
* Không markdown
* Hỗ trợ tiếng Việt

---

## Task 16 — Autofill form từ AI

### Prompt

Xây dựng chức năng tự động điền form nhập hàng từ kết quả AI OCR.

Yêu cầu:

* Mapping dữ liệu
* User có thể chỉnh sửa
* Hiển thị confidence nếu có

---

# Giai đoạn 9 — Chọn AI Model

## Task 17 — AI Model Selector

### Prompt

Xây dựng chức năng cho phép người dùng chọn AI model OCR.

Bao gồm:

* Dropdown model
* Save setting
* Dynamic API request

Ví dụ model:

* GPT-4o
* Claude
* Gemini
* Mistral OCR

---

# Giai đoạn 10 — Hoàn thiện

## Task 18 — Logging và Error Handling

### Prompt

Xây dựng logging và error handling cho hệ thống.

Bao gồm:

* API errors
* OCR errors
* Database errors

---

## Task 19 — UI/UX hoàn thiện

### Prompt

Cải thiện UI/UX cho hệ thống quản lý cửa hàng tạp hóa.

Yêu cầu:

* Modern UI
* Dark mode
* Responsive
* Loading skeleton

---

## Task 20 — Deploy Docker

### Prompt

Dockerize toàn bộ hệ thống Next.js + PostgreSQL.

Yêu cầu:

* Dockerfile
* docker-compose
* Production ready
* Environment variables
