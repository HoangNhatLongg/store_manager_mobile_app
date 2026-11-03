# Sơ đồ trình tự (Sequence Diagrams)

Tài liệu này tổng hợp các sơ đồ trình tự (Sequence Diagrams) mô tả chi tiết tương tác giữa các thành phần (Actor, Frontend, Backend, AI Service, Database) cho các tính năng chính của hệ thống.

---

## 1. Chức năng Đăng nhập (Authentication)

```plantuml
@startuml
autonumber
actor "Nhân viên / Quản lý" as User
participant "Frontend (Next.js)" as FE
participant "Backend API (NextAuth)" as BE
database "Database (PostgreSQL)" as DB

User -> FE: Nhập Email/Password và nhấn "Đăng nhập"
activate FE

FE -> FE: Validate dữ liệu form (Client-side)
FE -> BE: POST /api/auth/login (Payload: email, password)
activate BE

BE -> DB: Truy vấn User theo email
activate DB
DB --> BE: Trả về thông tin User (hash password, role...)
deactivate DB

BE -> BE: So sánh password hash (bcrypt)
alt Mật khẩu đúng
    BE -> BE: Tạo JWT / Session Token
    BE --> FE: HTTP 200 OK (Session Token & User Info)
    FE -> FE: Lưu session vào Cookies / State
    FE --> User: Chuyển hướng sang Dashboard & Hiển thị thành công
else Mật khẩu sai / Không tìm thấy user
    BE --> FE: HTTP 401 Unauthorized (Error Message)
    deactivate BE
    FE --> User: Hiển thị lỗi "Tài khoản hoặc mật khẩu không đúng"
    deactivate FE
end
@endum
```

---

## 2. Chức năng Quản lý Sản phẩm (Create Product)

```plantuml
@startuml
autonumber
actor "Quản lý" as Admin
participant "Frontend (Next.js)" as FE
participant "Backend API" as BE
database "Database (PostgreSQL)" as DB

Admin -> FE: Chọn "Thêm sản phẩm mới" & nhập thông tin sản phẩm
activate FE

Admin -> FE: Nhấp "Lưu"
FE -> FE: Validate Client-side (tên, giá > 0, tồn kho >= 0)
FE -> BE: POST /api/products (Headers: Auth Token, Body: Product Data)
activate BE

BE -> BE: Xác thực token & phân quyền (Role: Admin)
alt Không hợp lệ / Không đủ quyền
    BE --> FE: HTTP 403 Forbidden
    FE --> Admin: Hiển thị "Bạn không có quyền thực hiện hành động này"
else Hợp lệ
    BE -> DB: Prisma: product.create(data)
    activate DB
    DB --> BE: Trả về thông tin sản phẩm đã tạo thành công
    deactivate DB
    BE --> FE: HTTP 201 Created (Product JSON)
    deactivate BE
    FE -> FE: Cập nhật lại danh sách sản phẩm (Revalidate state)
    FE --> Admin: Hiển thị thông báo Toast thành công
    deactivate FE
end
@endum
```

---

## 3. Chức năng Nhập hàng bằng AI OCR (Tính năng cốt lõi)

```plantuml
@startuml
autonumber
actor "Nhân viên" as Staff
participant "Frontend (Next.js)" as FE
participant "Backend API" as BE
participant "OpenRouter API Gateway" as AI
database "Database (PostgreSQL)" as DB

Staff -> FE: Vào trang "Nhập hàng", chọn Model AI & upload ảnh hóa đơn
activate FE

FE -> FE: Preview ảnh & Validate định dạng/dung lượng file
FE -> BE: POST /api/ocr/upload (Multipart Form Data: Image File, Model ID)
activate BE

BE -> BE: Lưu file tạm thời / chuyển đổi định dạng ảnh
BE -> AI: POST /v1/chat/completions hoặc /ocr (API Key, Image Payload, Prompt trích xuất JSON)
activate AI
note over AI: AI xử lý quét hóa đơn\ntrích xuất thông tin sang JSON tiếng Việt
AI --> BE: Trả về JSON (danh sách sản phẩm, số lượng, đơn giá, ngày nhập)
deactivate AI

BE -> BE: Chuẩn hóa, làm sạch dữ liệu AI trả về
BE --> FE: HTTP 200 OK (Dữ liệu trích xuất JSON)
deactivate BE

FE -> FE: Điền tự động (Autofill) vào các trường dữ liệu trên form
FE -> FE: Đánh dấu (Highlight) những ô được AI nhận diện
FE --> Staff: Hiển thị form có sẵn dữ liệu để kiểm tra

Staff -> Staff: Rà soát, chỉnh sửa dữ liệu nếu có sai lệch
Staff -> FE: Xác nhận và nhấp nút "Nhập kho"

FE -> BE: POST /api/imports (Hóa đơn nhập kho chi tiết)
activate BE

BE -> BE: Bắt đầu Database Transaction
BE -> DB: Prisma: importInvoice.create() & importInvoiceItem.createMany()
activate DB
DB --> BE: Xác nhận tạo hóa đơn
deactivate DB

BE -> DB: Cập nhật tăng số lượng tồn kho (inventory) của từng sản phẩm
activate DB
DB --> BE: Xác nhận cập nhật tồn kho
deactivate DB

BE -> BE: Commit DB Transaction
BE --> FE: HTTP 200 OK (Import Success)
deactivate BE

FE --> Staff: Hiển thị Toast "Nhập hàng thành công!" & Reset form
deactivate FE
@endum
```

---

## 4. Chức năng Bán hàng tại quầy (POS Sale & Trừ kho)

```plantuml
@startuml
autonumber
actor "Nhân viên" as Staff
participant "Frontend (Next.js)" as FE
participant "Backend API" as BE
database "Database (PostgreSQL)" as DB

Staff -> FE: Quét mã vạch hoặc tìm sản phẩm, thêm vào giỏ hàng
activate FE
FE -> FE: Tính toán tổng tiền realtime trên giao diện
Staff -> FE: Nhấn nút "Thanh toán"

FE -> BE: POST /api/sales (Payload: Giỏ hàng - items, quantities)
activate BE

BE -> BE: Khởi tạo Database Transaction
loop Với mỗi sản phẩm trong đơn hàng
    BE -> DB: Truy vấn số lượng tồn kho hiện tại
    activate DB
    DB --> BE: Trả về tồn kho (quantity)
    deactivate DB
    
    alt Tồn kho < Số lượng mua
        BE -> BE: Rollback Transaction
        BE --> FE: HTTP 400 Bad Request (Lỗi: Sản phẩm X không đủ hàng)
        FE --> Staff: Cảnh báo "Sản phẩm X không đủ số lượng để bán"
    else Tồn kho >= Số lượng mua
        BE -> DB: Cập nhật trừ số lượng tồn kho (new_qty = current_qty - sold_qty)
        activate DB
        DB --> BE: Xác nhận trừ kho thành công
        deactivate DB
    end
end

BE -> DB: Lưu thông tin SaleInvoice & các SaleInvoiceItem
activate DB
DB --> BE: Xác nhận lưu hóa đơn bán hàng
deactivate DB

BE -> BE: Commit Database Transaction
BE --> FE: HTTP 200 OK (Sale Success JSON)
deactivate BE

FE -> FE: Kích hoạt in hóa đơn & Làm trống giỏ hàng
FE --> Staff: Hiển thị thông báo giao dịch thành công
deactivate FE
@endum
```

---

## 5. Chức năng Dashboard Quản lý tồn kho & Báo cáo

```plantuml
@startuml
autonumber
actor "Quản lý" as Admin
participant "Frontend (Next.js)" as FE
participant "Backend API" as BE
database "Database (PostgreSQL)" as DB

Admin -> FE: Truy cập màn hình Dashboard
activate FE

FE -> BE: GET /api/dashboard/stats
activate BE

BE -> DB: Truy vấn thống kê (Count products, Sum imports, Sum sales)
activate DB
DB --> BE: Trả về kết quả thô
deactivate DB

BE -> DB: Truy vấn danh sách sản phẩm có tồn kho dưới mức tối thiểu (threshold)
activate DB
DB --> BE: Trả về danh sách sản phẩm sắp hết hàng
deactivate DB

BE -> BE: Tổng hợp doanh thu, chi phí nhập hàng, tính lợi nhuận
BE --> FE: HTTP 200 OK (Dashboard Data JSON)
deactivate BE

FE -> FE: Vẽ biểu đồ cột/đường (nhập - bán)
FE -> FE: Render các Statistic Cards & cảnh báo sắp hết hàng
FE --> Admin: Hiển thị màn hình báo cáo hoàn chỉnh
deactivate FE
@endum
```
