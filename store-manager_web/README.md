# Store Manager - AI Grocery Store Management System

Hệ thống quản lý cửa hàng tạp hóa với tích hợp AI OCR, tự động hóa và thống kê doanh thu.

> **Dự án học tập** - Xây dựng hệ thống quản lý cửa hàng với các tính năng hiện đại.

## Thông tin dự án

| Thông tin | Chi tiết |
|---|---|
| **Tên dự án** | Store Manager |
| **Phiên bản** | 1.0.0 |
| **Framework** | Next.js 15 + TypeScript |
| **Database** | PostgreSQL + Prisma ORM |
| **UI Library** | TailwindCSS v4 + Shadcn UI |
| **Trạng thái** | Đang phát triển |

## Tech Stack

- **Frontend**: Next.js 15, TypeScript, TailwindCSS v4, Shadcn UI
- **Backend**: Next.js API Routes
- **Database**: PostgreSQL with Prisma ORM 5.x
- **AI OCR**: 9Router API (GPT-4o, Claude, Gemini, Mistral)
- **Authentication**: NextAuth.js
- **Container**: Docker & Docker Compose

## Tính năng

### Quản lý cơ bản
- [x] Dashboard với thống kê tổng quan
- [x] Quản lý sản phẩm (CRUD, hình ảnh, mã vạch)
- [x] Quản lý danh mục sản phẩm
- [x] Quản lý đơn vị tính
- [x] Quản lý nhà cung cấp
- [x] Quản lý khách hàng

### Nghiệp vụ
- [x] Nhập hàng với AI OCR nhận diện hóa đơn
- [x] Bán hàng (POS) với nhiều phương thức thanh toán
- [x] Quản lý tồn kho theo thời gian thực
- [x] Quản lý doanh thu theo ngày/tháng/năm
- [x] Biểu đồ thống kê doanh thu trực quan

### AI & Tự động hóa
- [x] AI OCR nhận diện hóa đơn nhập hàng
- [x] Hỗ trợ nhiều AI Model (GPT-4o, Claude, Gemini, Mistral)
- [x] Tạo sản phẩm tự động từ OCR

### Hệ thống
- [x] Authentication với NextAuth.js
- [x] Audit Log theo dõi thay đổi
- [x] Responsive UI với Dark Mode
- [x] Cài đặt hệ thống

## Getting Started

### Prerequisites

- Node.js 18+
- Docker & Docker Compose (khuyến nghị)
- PostgreSQL (hoặc dùng Docker)

### Cài đặt nhanh với Docker

```bash
# 1. Khởi động PostgreSQL và pgAdmin
docker-compose up -d

# 2. Cài đặt dependencies
npm install

# 3. Copy và cập nhật .env
cp .env.example .env
# Chỉnh sửa DATABASE_URL nếu cần

# 4. Generate Prisma Client
npm run db:generate

# 5. Push schema xuống database
npm run db:push

# 6. Seed database (tùy chọn)
npm run db:seed

# 7. Khởi động dev server
npm run dev
```

### Cài đặt thủ công (không Docker)

```bash
# 1. Cài PostgreSQL trên máy
# 2. Tạo database: store_manager

# 3. Cài đặt dependencies
npm install

# 4. Copy và cập nhật .env
cp .env.example .env
# Cập nhật DATABASE_URL phù hợp với PostgreSQL của bạn

# 5. Generate Prisma Client
npm run db:generate

# 6. Run migrations
npm run db:migrate

# 7. Khởi động dev server
npm run dev
```

### Truy cập

- **App**: http://localhost:3000
- **App qua mạng LAN**: cập nhật `NEXTAUTH_URL` trong `.env` thành URL mà thiết bị khác sử dụng, ví dụ `http://172.20.10.3:3000`, sau đó khởi động lại dev server. Nếu IP máy thay đổi, cập nhật lại cả `NEXTAUTH_URL` và `allowedDevOrigins` trong `next.config.ts`.
- **pgAdmin**: http://localhost:5050
  - Email: admin@store.com
  - Password: admin123

## Database Commands

```bash
npm run db:generate   # Generate Prisma Client
npm run db:push       # Push schema to database (dev)
npm run db:migrate    # Run migrations (production)
npm run db:studio     # Open Prisma Studio
npm run db:seed       # Seed database with sample data
```

## Docker Configuration

File `docker-compose.yml` chứa:

- **PostgreSQL 16**: Database chính
  - Port: 5432
  - User: store_admin
  - Password: store_password
  - Database: store_manager

- **pgAdmin 4**: Quản lý database GUI
  - Port: 5050
  - Email: admin@store.com
  - Password: admin123

### Quản lý Docker

```bash
# Khởi động
docker-compose up -d

# Dừng
docker-compose down

# Xóa volumes (reset database)
docker-compose down -v
```

## Project Structure

```
store-manager/
├── src/
│   ├── app/                    # Next.js App Router
│   │   ├── page.tsx           # Dashboard
│   │   ├── products/          # Products management
│   │   ├── categories/         # Categories management
│   │   ├── units/             # Units management
│   │   ├── suppliers/         # Suppliers management
│   │   ├── customers/         # Customers management
│   │   ├── import/            # Import invoices
│   │   ├── sales/             # Sales POS
│   │   ├── revenue/           # Revenue management
│   │   ├── inventory/         # Inventory management
│   │   ├── ai-ocr/            # AI OCR settings
│   │   ├── settings/          # App settings
│   │   └── api/               # API routes
│   ├── components/
│   │   ├── ui/                # Shadcn UI components
│   │   └── layout/            # Layout components
│   ├── lib/
│   │   ├── prisma.ts          # Prisma client
│   │   └── utils.ts           # Utilities
│   └── types/                 # TypeScript types
├── prisma/
│   └── schema.prisma          # Database schema
├── docs/
│   └── database.md            # Database documentation
├── docker-compose.yml         # Docker configuration
└── package.json
```

## Database Schema

Xem chi tiết tại [docs/database.md](./docs/database.md)

### Entities chính

| Entity | Mô tả |
|---|---|
| User | Người dùng hệ thống |
| Category | Danh mục sản phẩm |
| Product | Sản phẩm |
| Unit | Đơn vị tính |
| Supplier | Nhà cung cấp |
| Customer | Khách hàng |
| ImportInvoice | Hóa đơn nhập hàng |
| ImportInvoiceItem | Chi tiết nhập hàng |
| SaleInvoice | Hóa đơn bán hàng |
| SaleInvoiceItem | Chi tiết bán hàng |
| Setting | Cài đặt hệ thống |
| AuditLog | Nhật ký hệ thống |

## Scripts

```bash
npm run dev          # Start development server
npm run build        # Build for production
npm run start        # Start production server
npm run lint         # Run ESLint
```

## License

MIT
