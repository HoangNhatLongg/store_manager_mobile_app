# OCR hoá đơn

> Hạng mục trong plan: P9 — OCR hoá đơn
> Ngày hoàn thành: (đang cập nhật)
> Trạng thái: Chưa bắt đầu

## 1. Mục đích

Cho phép chụp hoặc chọn ảnh hoá đơn nhập hàng, hệ thống dùng AI OCR để tự động
nhận diện danh sách sản phẩm, số lượng, đơn giá và đổ vào form nhập hàng để
giảm thao tác gõ tay. Bám sát luồng OCR trong web `/import` và trang thử nghiệm
`/ai-ocr`.

## 2. Luồng hoạt động

### 2.1. Luồng OCR trong màn hình nhập hàng

```text
Từ màn hình Nhập hàng → bấm nút "OCR hoá đơn"
  → Mở trình chọn ảnh (JPG / PNG / WebP, tối đa 10MB)
  → Kiểm tra định dạng và dung lượng
      ├─ Không hợp lệ → báo lỗi, không gọi API
      └─ Hợp lệ → xem trước ảnh, cho người dùng xác nhận
  → Gửi ảnh dạng base64 lên POST /api/v1/ocr/process
      ├─ Thành công → nhận về danh sách mặt hàng
      │     → đối chiếu từng mặt hàng với danh sách sản phẩm có sẵn
      │         ├─ Khớp chính xác → gắn productId
      │         ├─ Khớp gần đúng → gợi ý cho người dùng chọn
      │         └─ Không khớp → đánh dấu sẽ tạo sản phẩm mới
      │     → đổ dữ liệu vào bảng dòng hàng, người dùng sửa trước khi lưu
      ├─ 400 VALIDATION_ERROR → báo lỗi định dạng ảnh
      └─ 500 / lỗi kết nối → báo thất bại, cho thử lại
```

### 2.2. Định dạng kết quả OCR mong đợi

```json
{
  "items": [
    { "name": "Coca Cola", "quantity": 5, "unitPrice": 12000 },
    { "name": "Nước suối", "quantity": 10, "unitPrice": 5000 }
  ],
  "importDate": "2026-05-28"
}
```

Người dùng luôn có thể sửa lại toàn bộ dữ liệu trước khi lưu phiếu.

### 2.3. Quy tắc bảo mật

- API key của OpenRouter **chỉ nằm trên server**, không đưa vào app.
- App gửi ảnh base64 lên endpoint mobile của hệ thống, server mới gọi OpenRouter.
- Không lưu cục bộ ảnh hoá đơn lâu dài.

### 2.4. Nhánh xử lý lỗi

| Tình huống | Mã lỗi | Xử lý |
|---|---|---|
| Ảnh sai định dạng hoặc quá 10MB | 400 VALIDATION_ERROR | Báo rõ yêu cầu, cho chọn ảnh khác |
| OpenRouter không trả kết quả hợp lệ | 502 / 500 | Báo thất bại, cho thử lại hoặc nhập tay |
| Token hết hạn | 401 UNAUTHORIZED | Xoá token, về màn hình đăng nhập |
| Mất mạng | IOException | Báo mất mạng, cho thử lại |

## 3. Các file liên quan

| Loại | Đường dẫn | Vai trò |
|---|---|---|
| Activity | `app/src/main/java/com/example/store_manager/ManHinhOcrHoaDon.kt` | Màn hình chọn ảnh và xem kết quả OCR (mới) |
| Activity | `app/src/main/java/com/example/store_manager/ManHinhNhapHang.kt` | Nhận dữ liệu OCR để đổ vào form |
| Dịch vụ | `app/src/main/java/com/example/store_manager/du_lieu/DichVuOcr.kt` | Gọi API OCR (mới) |
| Model | `app/src/main/java/com/example/store_manager/du_lieu/DoiTuongOcr.kt` | `KetQuaOcr`, `MatHangOcr` (mới) |
| Giao diện | `app/src/main/res/layout/ocr_hoa_don.xml` | Màn hình OCR (mới) |
| Giao diện | `app/src/main/res/layout/dong_ket_qua_ocr.xml` | Một dòng kết quả OCR (mới) |
| Hằng số | `app/src/main/res/values/strings.xml` | Chuỗi hiển thị |
| Menu | `app/src/main/java/com/example/store_manager/ManHinhCoThanhTab.kt` | Gắn handler `menu_ocr` |
| Manifest | `app/src/main/AndroidManifest.xml` | Khai báo Activity mới |

## 4. Bảng database liên quan

| Bảng | Trường dùng | Ghi chú |
|---|---|---|
| `products` | `id`, `name` | Đối chiếu tên sản phẩm đã có |
| `categories` | `id`, `name` | Gợi ý danh mục cho mặt hàng mới |
| `import_invoices` | — | Dữ liệu OCR chỉ đổ vào form, chưa lưu |

OCR không ghi trực tiếp vào database. Người dùng phải xác nhận qua màn hình Nhập
hàng rồi mới lưu phiếu.

## 5. Endpoint API được dùng

| Method | Đường dẫn | Mục đích |
|---|---|---|
| POST | `/api/v1/ocr/process` | Gửi ảnh, nhận danh sách mặt hàng (cần tạo mới) |
| POST | `/api/ocr/process` | Bản web dùng session NextAuth, mobile không gọi được |
| GET | `/api/v1/products` | Đối chiếu tên sản phẩm (đã có) |

Payload mẫu:

```json
{
  "image": "data:image/jpeg;base64,...",
  "model": "google/gemini-2.0-flash-001"
}
```

## 6. Đối chiếu với Web

| Chức năng | Vị trí trên Web | Trạng thái mobile |
|---|---|---|
| Chọn ảnh hoá đơn | `/import`, `/ai-ocr` | ❌ Cần làm |
| Xem trước ảnh | `/import` | ❌ Cần làm |
| Kiểm tra định dạng và dung lượng | `/import` | ❌ Cần làm |
| Gọi OCR và nhận kết quả | `/import` | ❌ Cần làm |
| Đổ dữ liệu vào form nhập hàng | `/import` | ❌ Cần làm |
| Chọn model OCR thủ công | `/ai-ocr` | ❌ Không cần trên mobile (model do server quyết định) |
| Kiểm tra kết nối OpenRouter | `/ai-ocr` | ❌ Không cần trên mobile (công cụ vận hành của web) |
