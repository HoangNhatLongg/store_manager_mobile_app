"use client";

import { useState, useEffect } from "react";
import { signIn, useSession } from "next-auth/react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Loader2, Eye, EyeOff, ShoppingBag } from "lucide-react";

export default function LoginPage() {
  const router = useRouter();
  const { status } = useSession();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    if (status === "authenticated") {
      router.push("/");
    }
  }, [status, router]);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError("");

    try {
      const result = await signIn("credentials", {
        email,
        password,
        redirect: false,
      });

      if (result?.error) {
        setError("Email hoặc mật khẩu không đúng");
        setIsLoading(false);
      } else {
        router.push("/");
        router.refresh();
      }
    } catch {
      setError("Đã xảy ra lỗi. Vui lòng thử lại.");
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen flex">
      {/* Left Side - Branding */}
      <div className="hidden lg:flex lg:w-1/2 bg-gradient-to-br from-emerald-500 to-emerald-700 p-12 flex-col justify-between">
        <div>
          <Link href="/" className="flex items-center gap-3 text-white">
            <div className="h-12 w-12 rounded-2xl bg-white/20 backdrop-blur flex items-center justify-center">
              <ShoppingBag className="h-7 w-7 text-white" />
            </div>
            <div>
              <h1 className="text-2xl font-bold">Store Manager</h1>
              <p className="text-emerald-100 text-sm">Quản lý tự động</p>
            </div>
          </Link>
        </div>

        <div className="space-y-6">
          <h2 className="text-4xl font-bold text-white leading-tight">
            Quản lý cửa hàng<br />thông minh hơn
          </h2>
          <p className="text-emerald-100 text-lg max-w-md">
            Hệ thống quản lý cửa hàng tạp hóa với AI OCR giúp bạn nhập hàng nhanh chóng và theo dõi tồn kho dễ dàng.
          </p>
          <div className="flex gap-4">
            <div className="bg-white/10 backdrop-blur-sm rounded-2xl p-4">
              <p className="text-3xl font-bold text-white">10x</p>
              <p className="text-emerald-100 text-sm">Nhanh hơn</p>
            </div>
            <div className="bg-white/10 backdrop-blur-sm rounded-2xl p-4">
              <p className="text-3xl font-bold text-white">99%</p>
              <p className="text-emerald-100 text-sm">Độ chính xác</p>
            </div>
            <div className="bg-white/10 backdrop-blur-sm rounded-2xl p-4">
              <p className="text-3xl font-bold text-white">24/7</p>
              <p className="text-emerald-100 text-sm">Hỗ trợ</p>
            </div>
          </div>
        </div>

        <p className="text-emerald-200 text-sm">
          © 2026 Store Manager. All rights reserved.
        </p>
      </div>

      {/* Right Side - Login Form */}
      <div className="flex-1 flex items-center justify-center p-8 bg-background">
        <div className="w-full max-w-md space-y-8">
          {/* Mobile Logo */}
          <div className="lg:hidden text-center mb-8">
            <Link href="/" className="inline-flex items-center gap-3">
              <div className="h-12 w-12 rounded-2xl bg-primary flex items-center justify-center">
                <ShoppingBag className="h-7 w-7 text-primary-foreground" />
              </div>
              <div className="text-left">
                <h1 className="text-2xl font-bold">Store Manager</h1>
                <p className="text-muted-foreground text-sm">AI-powered</p>
              </div>
            </Link>
          </div>

          <div className="text-center lg:text-left">
            <h2 className="text-3xl font-bold tracking-tight">Đăng nhập</h2>
            <p className="text-muted-foreground mt-2">
              Chào mừng bạn quay trở lại
            </p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-6">
            {error && (
              <div className="bg-destructive/10 text-destructive text-sm p-4 rounded-xl text-center">
                {error}
              </div>
            )}

            <div className="space-y-3">
              <Label htmlFor="email">Email</Label>
              <Input
                id="email"
                type="email"
                placeholder="Nhập email của bạn"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                required
                className="h-12"
                autoComplete="email"
              />
            </div>

            <div className="space-y-3">
              <Label htmlFor="password">Mật khẩu</Label>
              <div className="relative">
                <Input
                  id="password"
                  type={showPassword ? "text" : "password"}
                  placeholder="Nhập mật khẩu"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  required
                  className="h-12 pr-12"
                  autoComplete="current-password"
                />
                <button
                  type="button"
                  onClick={() => setShowPassword(!showPassword)}
                  className="absolute right-3 top-1/2 -translate-y-1/2 text-muted-foreground hover:text-foreground transition-colors"
                >
                  {showPassword ? (
                    <EyeOff className="h-5 w-5" />
                  ) : (
                    <Eye className="h-5 w-5" />
                  )}
                </button>
              </div>
            </div>

            <div className="flex items-center justify-between">
              <label className="flex items-center gap-2 text-sm">
                <input
                  type="checkbox"
                  className="rounded border-input w-4 h-4"
                />
                <span className="text-muted-foreground">Ghi nhớ đăng nhập</span>
              </label>
              <Link
                href="/forgot-password"
                className="text-sm text-primary hover:underline"
              >
                Quên mật khẩu?
              </Link>
            </div>

            <Button
              type="submit"
              className="w-full h-12 rounded-xl"
              disabled={isLoading}
            >
              {isLoading ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Đang đăng nhập...
                </>
              ) : (
                "Đăng nhập"
              )}
            </Button>
          </form>

          {/* Demo Credentials */}
          <div className="bg-muted/50 rounded-2xl p-4 space-y-2">
            <p className="text-sm font-medium text-muted-foreground">Tài khoản demo:</p>
            <div className="text-sm space-y-1">
              <p><span className="text-muted-foreground">Email:</span> <code className="bg-muted px-2 py-0.5 rounded">admin@store.com</code></p>
              <p><span className="text-muted-foreground">Mật khẩu:</span> <code className="bg-muted px-2 py-0.5 rounded">admin123</code></p>
            </div>
          </div>

          <p className="text-center text-sm text-muted-foreground lg:hidden">
            © 2026 Store Manager. All rights reserved.
          </p>
        </div>
      </div>
    </div>
  );
}
