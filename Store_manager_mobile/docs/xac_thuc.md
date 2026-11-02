# Xác thực và làm mới phiên đăng nhập

> Hạng mục trong plan: P12 — Xác thực nâng cao (P12.1, P12.2)
> Ngày hoàn thành: 02/11/2026
> Trạng thái: ✅ Hoàn thành

## 1. Mục đích

Trước đây access token sống 15 phút và không có cách lấy lại. Khi người dùng
mất mạng lâu rồi có mạng lại, token đã hết hạn, mọi API trả về 401 và ứng dụng
xoá phiên đăng nhập — người dùng phải nhập lại mật khẩu dù không hề chủ động
đăng xuất.

Sau khi hoàn thành, ứng dụng tự làm mới token ở chỗ và người dùng không bị
đăng xuất oan.

## 2. Luồng hoạt động

### 2.1. Cấu trúc token

| Token | Thời hạn | Lưu ở đâu | Mục đích |
|---|---|---|---|
| Access token | 15 phút | SharedPreferences (`ma_truy_cap`) | Gửi kèm mọi request |
| Refresh token | 30 ngày | SharedPreferences (`token_lam_moi`) + DB server | Đổi lấy access token mới |

Refresh token có `tokenId` là `jti` trong JWT. `tokenId` được lưu trong bảng
`mobile_refresh_tokens` để có thể tra cứu và thu hồi.

### 2.2. Luồng đăng nhập

```text
POST /api/v1/auth/login
  → Kiểm tra email + mật khẩu (bcrypt)
  → Tạo access token (15 phút)
  → Tạo refresh token (30 ngày), lưu tokenId vào DB
  → Trả về { accessToken, refreshToken, expiresIn, user }
  → Android lưu cả hai token vào BoNhoPhienDangNhap
```

### 2.3. Luồng tự động làm mới token (đây là phần quan trọng nhất)

OkHttp gọi `Authenticator` mỗi khi server trả về 401:

```text
Request bất kỳ gặp 401
  → LamMoiTokenTuDong.authenticate() được OkHttp gọi
      ├─ Đếm số lần đã thử lại (tối đa 1 lần)
      ├─ Không có refresh token → trả null (dừng)
      ├─ Đang offline → trả null (dừng, vì request cũng sẽ thất bại)
      └─ Gọi POST /api/v1/auth/refresh bằng client riêng
          ├─ Thành công → lưu token mới vào SharedPreferences
          │              → trả lại request đã gắn Authorization mới
          │              → OkHttp gửi lại request, người dùng không nhận biết
          └─ 401 (refresh token hết hạn/bị thu hồi) → xoá phiên, trả null
```

Client riêng cho API refresh không dùng interceptor gắn token và không có
`Authenticator`, tránh gọi lại chính nó gây vòng lặp vô hạn.

### 2.4. Luồng kiểm tra token khi mở app

```text
ManHinhKhoiDong.kiemTraTokenVaDieuHuong()
  → Không có access token → sang màn hình đăng nhập
  → Gọi GET /api/v1/me
      ├─ 200 → vào màn hình theo vai trò
      ├─ 401 → thử refresh một lần
      │      ├─ Refresh thành công → gọi lại /me → vào màn hình
      │      └─ Refresh thất bại → xoá phiên, sang đăng nhập
      └─ Lỗi mạng → giữ nguyên phiên, cho vào app dùng offline
```

### 2.5. Luồng đăng xuất

```text
ManHinhCoThanhTab.dangXuat()
  → Gọi POST /api/v1/auth/logout để thu hồi refresh token trên server
  → Xoá toàn bộ SharedPreferences (access + refresh + thông tin user)
  → Mở màn hình đăng nhập với FLAG_ACTIVITY_CLEAR_TASK
```

Gọi API logout có thể thất bại khi mất mạng, nhưng phiên vẫn bị xoá cục bộ.

## 3. File liên quan

### 3.1. Backend (`store-manager_web`)

| File | Vai trò |
|---|---|
| `prisma/schema.prisma` | Model `MobileRefreshToken` + quan hệ với `User` |
| `prisma/migrations/20261102180000_add_mobile_refresh_tokens/migration.sql` | Tạo bảng `mobile_refresh_tokens` |
| `src/lib/mobile-jwt.ts` | `createMobileAccessToken`, `createMobileRefreshToken`, `verifyMobileRefreshToken` |
| `src/app/api/v1/auth/login/route.ts` | Cấp access + refresh token |
| `src/app/api/v1/auth/refresh/route.ts` | Xoay vòng token (thu hồi cũ, cấp mới) |
| `src/app/api/v1/auth/logout/route.ts` | Thu hồi refresh token |
| `src/lib/mobile-auth.ts` | Hàm đọc user từ header `Authorization` (không đổi) |

### 3.2. Android (`Store_manager_mobile`)

| File | Vai trò |
|---|---|
| `du_lieu/LamMoiTokenTuDong.kt` | `okhttp3.Authenticator` tự refresh khi gặp 401 |
| `du_lieu/BoNhoPhienDangNhap.kt` | Lưu access + refresh token, `luuMaTruyCap()`, `luuTokenLamMoi()` |
| `du_lieu/DichVuApi.kt` | Khai báo endpoint refresh/logout, đăng ký `Authenticator` vào OkHttp |
| `du_lieu/DichVuTaiKhoan.kt` | `kiemTraToken()` thử refresh trước khi kết luận token hết hạn |
| `du_lieu/DichVuDangNhap.kt` | `dangXuat()` gọi API logout rồi xoá phiên cục bộ |
| `du_lieu/DoiTuongDangNhap.kt` | Thêm `YeuCauLamMoiToken`, `PhanHoiLamMoiToken`, `YeuCauDangXuat` |
| `ManHinhCoThanhTab.kt` | `dangXuat()` gọi API thay vì xoá phiên trực tiếp |

## 4. Bảng dữ liệu liên quan

### 4.1. `mobile_refresh_tokens` (PostgreSQL)

| Cột | Kiểu | Ghi chú |
|---|---|---|
| `id` | TEXT | Khóa chính, `cuid()` |
| `tokenId` | TEXT | Duy nhất, khớp với `tokenId` trong JWT |
| `userId` | TEXT | Khóa ngoại tới `users.id`, xoá cascade |
| `expiresAt` | TIMESTAMP | Hạn 30 ngày kể từ lần cấp |
| `revokedAt` | TIMESTAMP | Có giá trị nghĩa là token đã bị thu hồi |
| `createdAt` | TIMESTAMP | Thời điểm cấp token |

### 4.2. SharedPreferences `phien_dang_nhap` (thiết bị)

| Khóa | Nội dung |
|---|---|
| `ma_truy_cap` | Access token hiện tại |
| `token_lam_moi` | Refresh token hiện tại |
| `id_nguoi_dung` | Id user |
| `email` | Email đăng nhập |
| `ten` | Tên hiển thị |
| `vai_tro` | ADMIN / MANAGER / STAFF |

## 5. Quy tắc bắt buộc

1. **Refresh token phải xoay vòng.** Mỗi lần refresh, token cũ bị đánh dấu
   `revokedAt` và token mới được cấp. Token cũ không dùng lại được (server trả 401).
2. **Chỉ thử lại request 1 lần.** `responseCount(response) >= 2` thì dừng, tránh
   vòng lặp vô hạn.
3. **Không refresh khi offline.** `KiemTraKetNoi.coKetNoi()` phải trả `true`,
   vì lúc đó request cũng không thành công.
4. **Client refresh phải tách biệt.** Không dùng chung client có `Authenticator`
   để tránh gọi lại chính nó.
5. **Không nuốt `CancellationException`.** Mọi `try/catch` quanh network call
   phải `throw` lại `CancellationException`, nếu không sẽ phá cơ chế huỷ coroutine.

## 6. Kiểm thử

| Tình huống | Kết quả mong đợi |
|---|---|
| Đăng nhập thành công | Trả về `accessToken` + `refreshToken` |
| Gọi `/api/v1/me` với access token mới | 200 |
| Gọi `/api/v1/auth/refresh` | 200, access token mới khác token cũ |
| Dùng lại refresh token cũ | 401 `INVALID_REFRESH_TOKEN` |
| Mất mạng > 15 phút rồi có mạng lại | App tự refresh, không bị đăng xuất |
| Bấm đăng xuất | Refresh token bị thu hồi trên server, phiên xoá cục bộ |
| Mở app khi offline | Vẫn vào được app, dùng dữ liệu trong Room |
