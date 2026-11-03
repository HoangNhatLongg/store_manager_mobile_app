# UI_DESIGN_PROMPT.md

Thiết kế giao diện hiện đại cho hệ thống quản lý cửa hàng tạp hóa tích hợp AI OCR.

Công nghệ:

- Next.js 15
- TypeScript
- TailwindCSS
- Shadcn UI

Phong cách giao diện:

- Modern SaaS Dashboard
- Tối giản
- Chuyên nghiệp
- Dễ thao tác
- Responsive
- Hỗ trợ dark mode
- UX tối ưu cho nhân viên cửa hàng

Màu sắc:

- Primary: Emerald / Green
- Secondary: Slate
- Accent: Orange nhẹ
- Background sáng sạch sẽ
- Card bo góc mềm
- Shadow nhẹ
- Không dùng màu quá chói

Typography:

- Font hiện đại dễ đọc
- Khoảng cách thoáng
- Hierarchy rõ ràng

Yêu cầu UX:

- Người dùng thao tác nhanh
- Giảm số click
- Form nhập liệu rõ ràng
- Nút hành động dễ thấy
- Dashboard trực quan
- Hỗ trợ dùng trên laptop và tablet

---

# Thiết kế Layout tổng thể

## Sidebar trái

Bao gồm:

- Dashboard
- Sản phẩm
- Danh mục
- Nhập hàng
- Bán hàng
- Tồn kho
- AI OCR
- Cài đặt

Sidebar:

- Có icon
- Có trạng thái active
- Collapse được

---

## Header topbar

Bao gồm:

- Search
- Notification
- User avatar
- Dark mode toggle

---

# Dashboard

Thiết kế dashboard hiện đại gồm:

## Statistic Cards

- Tổng sản phẩm
- Tổng tồn kho
- Hàng sắp hết
- Doanh thu hôm nay

Card:

- Có icon
- Gradient nhẹ
- Hover animation

---

## Charts

- Biểu đồ nhập hàng
- Biểu đồ bán hàng
- Thống kê tồn kho

---

## Recent Activities

- Hóa đơn mới
- Sản phẩm mới nhập
- Cảnh báo tồn kho

---

# Trang quản lý sản phẩm

Thiết kế:

- Data table hiện đại
- Search realtime
- Filter danh mục
- Pagination

Cột:

- Ảnh
- Tên sản phẩm
- Giá nhập
- Giá bán
- Tồn kho
- Danh mục
- Hành động

Nút:

- Thêm sản phẩm
- Sửa
- Xóa

Modal thêm sản phẩm:

- UI đẹp
- Validation rõ ràng

---

# Trang nhập hàng

Đây là trang quan trọng nhất.

Thiết kế trải nghiệm AI OCR nổi bật.

## Layout chia 2 cột

### Cột trái

Upload hóa đơn:

- Drag & Drop
- Preview ảnh
- Loading AI OCR
- Chọn AI model

### Cột phải

Form nhập hàng:

- Dynamic rows
- Tên sản phẩm
- Số lượng
- Đơn giá
- Thành tiền

Yêu cầu:

- AI tự động điền dữ liệu
- User có thể chỉnh sửa
- Highlight field được AI detect

---

# AI OCR Experience

Khi upload ảnh:

- Hiển thị loading animation hiện đại
- Hiển thị trạng thái:
  - Uploading
  - AI analyzing
  - Extracting data

Sau khi AI xử lý:

- Autofill mềm mại
- Có badge AI Generated
- Có confidence score nếu có

---

# Trang bán hàng POS

Thiết kế POS hiện đại:

## Bên trái

- Danh sách sản phẩm
- Search nhanh
- Card sản phẩm

## Bên phải

- Giỏ hàng
- Tổng tiền
- Thanh toán

Yêu cầu:

- Thao tác nhanh
- Hỗ trợ cảm ứng
- Responsive

---

# Trang tồn kho

Hiển thị:

- Sản phẩm sắp hết
- Sản phẩm tồn nhiều
- Lịch sử nhập xuất

Có:

- Filter
- Export Excel
- Status badge

---

# Animation

Sử dụng:

- Framer Motion

Animation:

- Fade in
- Slide up
- Hover card
- Smooth transition

Không dùng animation quá mạnh gây rối mắt.

---

# UI Components

Sử dụng:

- Shadcn UI

Bao gồm:

- Card
- Table
- Dialog
- Drawer
- Dropdown
- Tabs
- Tooltip
- Toast
- Skeleton loading

---

# Yêu cầu Responsive

Desktop:

- Dashboard đầy đủ

Tablet:

- Sidebar collapse

Mobile:

- Drawer navigation
- Stack layout

---

# Cảm giác tổng thể

Hệ thống cần mang cảm giác:

- Chuyên nghiệp
- Thông minh
- Hiện đại
- Có AI
- Dễ sử dụng cho nhân viên cửa hàng

Tránh:

- Giao diện quá màu mè
- Quá nhiều màu
- Quá nhiều hiệu ứng
- Layout chật chội

Ưu tiên:

- Không gian thoáng
- Trải nghiệm nhập liệu nhanh
- Focus vào productivity
