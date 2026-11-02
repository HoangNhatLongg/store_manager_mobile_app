# Màn hình cài đặt

> Hạng mục trong plan: P13 — Màn hình cài đặt
> Ngày hoàn thành: (đang cập nhật)
> Trạng thái: Chưa bắt đầu

## 1. Mục đích

Cung cấp chỗ để người dùng xem thông tin hệ thống, bật tắt chế độ tối, kiểm tra
kết nối máy chủ và quản lý một số tuỳ chọn cơ bản. Bám sát giao diện web
`/settings`.

## 2. Luồng hoạt động

### 2.1. Bố cục màn hình

Web có 4 tab: **Chung**, **AI OCR**, **Cơ sở dữ liệu**, **Thông báo**. Trên
mobile dùng 4 tab ngang ở đầu màn hình, mỗi tab là một khối nội dung.

| Tab | Nội dung trên mobile |
|---|---|
| Chung | Bật tắt dark mode, thông tin tài khoản, địa chỉ máy chủ, đổi mật khẩu |
| AI OCR | Thông tin model OCR đang dùng, kiểm tra kết nối OCR, trạng thái API key |
| Cơ sở dữ liệu | Thông tin kết nối, nút đồng bộ dữ liệu, xoá cache cục bộ |
| Thông báo | Bật tắt thông báo tồn kho thấp, bật tắt thông báo sau khi bán |

### 2.2. Luồng bật tắt dark mode

```text
Bấm công tắc dark mode
  → Lưu lựa chọn vào SharedPreferences
  → Áp dụng theme tương ứng (AppCompatDelegate.setDefaultNightMode)
  → Hiện thông báo "Đã lưu thay đổi"
```

Khác với web, mobile lưu ngay và áp dụng tức thì vì không có cơ chế server lưu
cài đặt chung cho mọi thiết bị.

### 2.3. Nhánh xử lý lỗi

| Tình huống | Mã lỗi | Xử lý |
|---|---|---|
| Kiểm tra kết nối thất bại | 500 / IOException | Hiện trạng thái "không kết nối được" |
| Token hết hạn | 401 UNAUTHORIZED | Xoá token, về màn hình đăng nhập |

## 3. Các file liên quan

| Loại | Đường dẫn | Vai trò |
|---|---|---|
| Activity | `app/src/main/java/com/example/store_manager/ManHinhCaiDat.kt` | Màn hình cài đặt (mới) |
| Dịch vụ | `app/src/main/java/com/example/store_manager/du_lieu/DichVuCaiDat.kt` | Kiểm tra kết nối, đồng bộ (mới) |
| Model | `app/src/main/java/com/example/store_manager/du_lieu/DoiTuongCaiDat.kt` | Cấu trúc cài đặt (mới) |
| Giao diện | `app/src/main/res/layout/cai_dat.xml` | Màn hình cài đặt (mới) |
| Giao diện | `app/src/main/res/layout/dong_tuy_chon.xml` | Một dòng tuỳ chọn (mới) |
| Hằng số | `app/src/main/res/values/strings.xml` | Chuỗi hiển thị |
| Menu | `app/src/main/java/com/example/store_manager/ManHinhCoThanhTab.kt` | Gắn handler `menu_cai_dat` |
| Manifest | `app/src/main/AndroidManifest.xml` | Khai báo Activity mới |
| Build | `app/build.gradle.kts` | Thêm `androidx.appcompat` cho dark mode nếu chưa có |

## 4. Bảng database liên quan

| Bảng | Trường dùng | Ghi chú |
|---|---|---|
| `settings` | `key`, `value`, `type`, `group`, `isPublic` | Bản web có dùng nhưng chưa lưu thật; mobile lưu cục bộ |

Trên mobile, cài đặt người dùng lưu trong SharedPreferences chứ không đồng bộ
lên server, vì mỗi thiết bị có thể dùng chế độ khác nhau.

## 5. Endpoint API được dùng

| Method | Đường dẫn | Mục đích |
|---|---|---|
| GET | `/api/v1/me` | Thông tin tài khoản hiện tại (đã có) |
| GET | `/api/v1/bootstrap` | Đồng bộ dữ liệu nền (đã có) |

## 6. Đối chiếu với Web

| Chức năng | Vị trí trên Web | Trạng thái mobile |
|---|---|---|
| 4 tab cài đặt | `/settings` | ❌ Cần làm |
| Bật tắt dark mode | `/settings` | ❌ Cần làm (mobile lưu cục bộ) |
| Nút "Lưu thay đổi" | `/settings` | ⛔ Không cần (mobile lưu ngay) |
| Sao lưu / khôi phục | `/settings` | ⛔ Không cần (chỉ là placeholder trên web) |
| Thông báo | `/settings` | ❌ Cần làm |
