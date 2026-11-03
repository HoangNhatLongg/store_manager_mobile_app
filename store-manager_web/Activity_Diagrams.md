# Sơ đồ hoạt động (Activity Diagrams) với Swimlanes

Tài liệu này tổng hợp các sơ đồ hoạt động dạng **Swimlane** bằng ngôn ngữ **PlantUML** cho các chức năng chính của hệ thống Quản lý cửa hàng tạp hóa tích hợp AI OCR.

---

## 1. Chức năng Đăng nhập (Authentication)

Sơ đồ thể hiện luồng đăng nhập vào hệ thống của nhân viên hoặc quản lý.

```plantuml
@startuml
|Nhân viên/Quản lý|
start
:Truy cập trang đăng nhập;
:Nhập Email và Mật khẩu;
:Nhấn nút "Đăng nhập";

|Frontend|
:Validate dữ liệu đầu vào (Client-side);
if (Hợp lệ?) then (Không)
  :Hiển thị lỗi validate form;
  detach
else (Có)
  :Gửi request đăng nhập (POST /api/auth/login);
endif

|Backend API (NextAuth)|
:Mã hóa/kiểm tra mật khẩu;
:Truy vấn cơ sở dữ liệu;

|Database|
:Kiểm tra thông tin tài khoản;
:Trả về thông tin User (nếu khớp);

|Backend API (NextAuth)|
if (Tài khoản & mật khẩu đúng?) then (Không)
  :Trả về mã lỗi (401 Unauthorized);
  |Frontend|
  :Hiển thị thông báo "Sai tài khoản hoặc mật khẩu";
  detach
else (Có)
  :Tạo Session/JWT token;
  :Trả về thành công và Session token;
endif

|Frontend|
:Lưu session vào context/cookies;
:Chuyển hướng sang trang Dashboard;

|Nhân viên/Quản lý|
:Xem màn hình Dashboard;
stop
@endum
```

---

## 2. Chức năng Quản lý Sản phẩm (CRUD Product)

Sơ đồ thể hiện luồng thêm mới/cập nhật sản phẩm.

```plantuml
@startuml
|Quản lý|
start
:Chọn chức năng "Sản phẩm";
:Nhấn nút "Thêm sản phẩm mới";
:Nhập thông tin (Tên, danh mục, giá nhập, giá bán, tồn kho);
:Nhấn nút "Lưu";

|Frontend|
:Kiểm tra dữ liệu nhập (Client-side);
if (Đầy đủ thông tin?) then (Không)
  :Cảnh báo lỗi nhập liệu;
  detach
else (Có)
  :Gửi request lưu sản phẩm (POST/PUT /api/products);
endif

|Backend API|
:Xác thực Session người dùng;
if (Hợp lệ?) then (Không)
  :Trả về lỗi 401 Unauthorized;
  |Frontend|
  :Yêu cầu đăng nhập lại;
  detach
else (Có)
  |Backend API|
  :Validate nghiệp vụ (trùng tên, định dạng số);
  :Gửi câu lệnh Prisma Create/Update;
endif

|Database|
:Ghi dữ liệu sản phẩm mới/cập nhật;
:Trả về trạng thái thành công;

|Backend API|
:Trả về HTTP 201 Created hoặc 200 OK;

|Frontend|
:Hiển thị Toast thông báo thành công;
:Tải lại danh sách sản phẩm mới nhất;

|Quản lý|
:Xem sản phẩm mới xuất hiện trên bảng dữ liệu;
stop
@endum
```

---

## 3. Chức năng Nhập hàng bằng AI OCR (Tính năng cốt lõi)

Sơ đồ thể hiện luồng tải hóa đơn giấy lên, AI tự động nhận diện và hoàn tất hóa đơn nhập kho.

```plantuml
@startuml
|Nhân viên|
start
:Vào trang "Nhập hàng";
:Chọn AI Model mong muốn (Gemini, GPT-4o, ...);
:Kéo thả hoặc tải ảnh hóa đơn mua hàng lên;

|Frontend|
:Kiểm tra định dạng file (PNG, JPG, PDF) & dung lượng;
if (Hợp lệ?) then (Không)
  :Hiển thị thông báo định dạng sai;
  detach
else (Có)
  :Hiển thị trạng thái Loading (Uploading & Analyzing);
  :Gửi file ảnh và model đã chọn tới API;
endif

|Backend API|
:Nhận ảnh và thông tin cấu hình model;
:Gửi ảnh tới OpenRouter API Gateway với prompt phù hợp;

|OpenRouter API|
:Phân tích hình ảnh hóa đơn;
:Trích xuất thông tin (Tên sản phẩm, số lượng, đơn giá);
:Trả về dữ liệu định dạng JSON chuẩn;

|Backend API|
:Nhận kết quả JSON từ AI;
:Chuẩn hóa dữ liệu tương thích với DB;
:Trả về kết quả (HTTP 200) cùng dữ liệu trích xuất;

|Frontend|
:Tự động điền (Autofill) thông tin vào bảng nhập hàng;
:Đánh dấu (Highlight) các trường dữ liệu do AI điền;

|Nhân viên|
:Kiểm tra lại dữ liệu AI tự điền;
if (Dữ liệu chính xác?) then (Không)
  :Chỉnh sửa thủ công các ô sai lệch;
else (Có)
endif
:Nhấn nút "Xác nhận nhập kho";

|Frontend|
:Gửi payload hóa đơn nhập hàng hoàn chỉnh (POST /api/imports);

|Backend API|
:Mở một Transaction database;
:Lưu thông tin ImportInvoice và các ImportInvoiceItem;
:Cộng dồn số lượng tồn kho của từng sản phẩm tương ứng;

|Database|
:Thực thi Transaction cập nhật DB;
:Trả về kết quả Transaction thành công;

|Backend API|
:Đóng Transaction thành công;
:Trả về kết quả lưu hóa đơn;

|Frontend|
:Hiển thị Toast thành công "Nhập hàng thành công!";
:Chuyển hướng về trang danh sách tồn kho;

|Nhân viên|
:Xem danh sách hàng hóa đã được cập nhật số lượng tồn;
stop
@endum
```

---

## 4. Chức năng Bán hàng tại quầy (POS Sale)

Sơ đồ thể hiện luồng chọn sản phẩm, tính tiền, và trừ kho tự động khi tạo hóa đơn bán.

```plantuml
@startuml
|Nhân viên|
start
:Truy cập giao diện "Bán hàng POS";
repeat
  :Tìm kiếm sản phẩm hoặc quét mã;
  :Chọn sản phẩm đưa vào giỏ hàng;
  :Điều chỉnh số lượng mua;
backward:Cập nhật giỏ hàng;
repeat while (Tiếp tục mua hàng?) is (Có) not (Không)
:Xem tổng tiền cần thanh toán;
:Nhấp nút "Thanh toán";

|Frontend|
:Gửi yêu cầu thanh toán (POST /api/sales);

|Backend API|
:Khởi động DB Transaction;
:Kiểm tra tồn kho cho các sản phẩm;
if (Đủ số lượng tồn kho?) then (Không)
  :Hủy Transaction;
  :Trả về lỗi 400 (Không đủ tồn kho);
else (Có)
  :Trừ số lượng tồn kho của sản phẩm;
  :Tạo mới SaleInvoice và các SaleInvoiceItem;
  |Database|
  :Thực hiện lưu dữ liệu hóa đơn;
  |Backend API|
  :Trả về HTTP 200 OK thành công;
endif

|Frontend|
if (Phản hồi thành công (200 OK)?) then (Có)
  :Hiển thị thông báo thành công;
  :In hóa đơn bán hàng (nếu có);
  :Làm trống giỏ hàng;
  |Nhân viên|
  :Bàn giao hàng cho khách hàng;
else (Không)
  |Frontend|
  :Hiển thị thông báo "Sản phẩm không đủ số lượng tồn";
endif
|Nhân viên|
stop
@endum
```

---

## 5. Chức năng Dashboard Quản lý tồn kho & Báo cáo

Sơ đồ thể hiện luồng lấy dữ liệu thống kê tổng quan của cửa hàng.

```plantuml
@startuml
|Quản lý|
start
:Mở trang "Dashboard";

|Frontend|
:Gửi request lấy số liệu thống kê (GET /api/dashboard/stats);

|Backend API|
:Truy vấn dữ liệu tổng hợp:
- Tổng số lượng sản phẩm
- Danh sách sản phẩm sắp hết hàng (tồn kho < ngưỡng)
- Tổng tiền nhập hàng & doanh thu bán hàng;

|Database|
:Thực thi các câu lệnh Aggregate (Sum, Count) & Filter;
:Trả về kết quả tổng hợp cho API;

|Backend API|
:Tính toán lợi nhuận dự kiến;
:Trả về cục JSON thống kê đầy đủ;

|Frontend|
:Render dữ liệu lên các Statistic Cards;
:Vẽ biểu đồ nhập/bán (Sử dụng thư viện Chart/Recharts);
:Hiển thị danh sách cảnh báo hàng sắp hết;

|Quản lý|
:Xem báo cáo trực quan và đưa ra quyết định nhập hàng;
stop
@endum
```
