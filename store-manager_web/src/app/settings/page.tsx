"use client";

import { useState } from "react";
import { MainLayout } from "@/components/layout";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Badge } from "@/components/ui/badge";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Settings,
  Bot,
  Database,
  Bell,
  Save,
  Check,
  Loader2,
  Key,
  FlaskConical,
  Globe,
} from "lucide-react";

export default function SettingsPage() {
  const [isDarkMode, setIsDarkMode] = useState(() => {
    if (typeof window !== "undefined") {
      return document.documentElement.classList.contains("dark");
    }
    return false;
  });
  const [selectedModel, setSelectedModel] = useState("gpt-4o");
  const [isSaving, setIsSaving] = useState(false);

  const toggleDarkMode = () => {
    document.documentElement.classList.toggle("dark");
    setIsDarkMode(!isDarkMode);
  };

  const handleSave = () => {
    setIsSaving(true);
    setTimeout(() => setIsSaving(false), 1000);
  };

  const aiModels = [
    { id: "gpt-4o", name: "GPT-4o", provider: "OpenAI" },
    { id: "claude", name: "Claude 3", provider: "Anthropic" },
    { id: "gemini", name: "Gemini Pro", provider: "Google" },
  ];

  return (
    <MainLayout>
      <div className="space-y-8">
        {/* Header */}
        <div>
          <h1 className="text-3xl font-semibold tracking-tight">Cài đặt</h1>
          <p className="text-muted-foreground mt-1">
            Quản lý cấu hình hệ thống
          </p>
        </div>

        <Tabs defaultValue="general" className="space-y-6">
          <TabsList className="rounded-xl">
            <TabsTrigger value="general" className="rounded-lg">
              <Settings className="mr-2 h-4 w-4" />
              Chung
            </TabsTrigger>
            <TabsTrigger value="ai" className="rounded-lg">
              <Bot className="mr-2 h-4 w-4" />
              AI OCR
            </TabsTrigger>
            <TabsTrigger value="database" className="rounded-lg">
              <Database className="mr-2 h-4 w-4" />
              Cơ sở dữ liệu
            </TabsTrigger>
            <TabsTrigger value="notifications" className="rounded-lg">
              <Bell className="mr-2 h-4 w-4" />
              Thông báo
            </TabsTrigger>
          </TabsList>

          {/* General Settings */}
          <TabsContent value="general" className="space-y-6">
            <div className="bg-card rounded-2xl p-6 space-y-6">
              <h2 className="text-lg font-medium">Cài đặt chung</h2>

              {/* Store Name */}
              <div className="space-y-3">
                <Label htmlFor="storeName">Tên cửa hàng</Label>
                <Input
                  id="storeName"
                  defaultValue="Cửa hàng tiện lợi ABC"
                  className="rounded-xl"
                />
              </div>

              {/* Language */}
              <div className="space-y-3">
                <Label htmlFor="language">Ngôn ngữ</Label>
                <Input
                  id="language"
                  defaultValue="Tiếng Việt"
                  className="rounded-xl"
                />
              </div>

              {/* Currency */}
              <div className="space-y-3">
                <Label htmlFor="currency">Đơn vị tiền tệ</Label>
                <Input
                  id="currency"
                  defaultValue="VND (Việt Nam Đồng)"
                  className="rounded-xl"
                />
              </div>

              {/* Dark Mode */}
              <div className="flex items-center justify-between">
                <div className="space-y-0.5">
                  <Label>Chế độ tối</Label>
                  <p className="text-sm text-muted-foreground">
                    Bật chế độ giao diện tối
                  </p>
                </div>
                <Button
                  variant={isDarkMode ? "default" : "outline"}
                  onClick={toggleDarkMode}
                  className="rounded-xl"
                >
                  {isDarkMode ? "Đang bật" : "Tắt"}
                </Button>
              </div>
            </div>
          </TabsContent>

          {/* AI OCR Settings */}
          <TabsContent value="ai" className="space-y-6">
            <div className="bg-card rounded-2xl p-6 space-y-6">
              <h2 className="text-lg font-medium flex items-center gap-2">
                <Bot className="h-5 w-5" />
                Cài đặt AI OCR
              </h2>

              {/* API Key */}
              <div className="space-y-3">
                <Label htmlFor="apiKey">API Key</Label>
                <div className="relative">
                  <Key className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
                  <Input
                    id="apiKey"
                    type="password"
                    placeholder="Nhập API key của bạn"
                    className="pl-10 rounded-xl"
                  />
                </div>
              </div>

              {/* AI Model Selection */}
              <div className="space-y-3">
                <Label>Chọn AI Model</Label>
                <div className="grid grid-cols-3 gap-3">
                  {aiModels.map((model) => (
                    <Button
                      key={model.id}
                      variant={selectedModel === model.id ? "default" : "outline"}
                      onClick={() => setSelectedModel(model.id)}
                      className="h-auto py-4 rounded-xl flex-col gap-2"
                    >
                      <span className="font-medium">{model.name}</span>
                      <Badge variant="secondary" className="text-xs">
                        {model.provider}
                      </Badge>
                    </Button>
                  ))}
                </div>
              </div>

              {/* Test Area */}
              <div className="space-y-3">
                <Label>Kiểm tra OCR</Label>
                <div className="border-2 border-dashed border-border rounded-2xl p-8 text-center">
                  <FlaskConical className="h-8 w-8 mx-auto mb-3 text-muted-foreground" />
                  <p className="text-sm text-muted-foreground">
                    Tải ảnh hóa đơn lên để kiểm tra
                  </p>
                  <Button variant="outline" className="mt-4 rounded-xl">
                    Tải ảnh
                  </Button>
                </div>
              </div>
            </div>
          </TabsContent>

          {/* Database Settings */}
          <TabsContent value="database" className="space-y-6">
            <div className="bg-card rounded-2xl p-6 space-y-6">
              <h2 className="text-lg font-medium flex items-center gap-2">
                <Database className="h-5 w-5" />
                Cơ sở dữ liệu
              </h2>

              {/* Database URL */}
              <div className="space-y-3">
                <Label htmlFor="dbUrl">Database URL</Label>
                <Input
                  id="dbUrl"
                  defaultValue="postgresql://localhost:5432/store_manager"
                  className="rounded-xl"
                />
              </div>

              {/* Backup */}
              <div className="space-y-3">
                <Label>Sao lưu</Label>
                <div className="flex gap-3">
                  <Button variant="outline" className="rounded-xl">
                    Tạo sao lưu
                  </Button>
                  <Button variant="outline" className="rounded-xl">
                    Khôi phục
                  </Button>
                </div>
              </div>

              {/* Status */}
              <div className="p-4 rounded-xl bg-emerald-50 dark:bg-emerald-950/50">
                <div className="flex items-center gap-3">
                  <Check className="h-5 w-5 text-emerald-600" />
                  <div>
                    <p className="font-medium text-emerald-700 dark:text-emerald-400">
                      Kết nối thành công
                    </p>
                    <p className="text-sm text-emerald-600/80 dark:text-emerald-400/80">
                      Database đang hoạt động bình thường
                    </p>
                  </div>
                </div>
              </div>
            </div>
          </TabsContent>

          {/* Notifications Settings */}
          <TabsContent value="notifications" className="space-y-6">
            <div className="bg-card rounded-2xl p-6 space-y-6">
              <h2 className="text-lg font-medium flex items-center gap-2">
                <Bell className="h-5 w-5" />
                Thông báo
              </h2>

              {/* Notification Toggles */}
              <div className="space-y-4">
                <div className="flex items-center justify-between p-4 rounded-xl bg-muted/30">
                  <div>
                    <p className="font-medium">Thông báo hàng sắp hết</p>
                    <p className="text-sm text-muted-foreground">
                      Nhận thông báo khi sản phẩm sắp hết hàng
                    </p>
                  </div>
                  <Button variant="outline" className="rounded-xl">
                    Bật
                  </Button>
                </div>

                <div className="flex items-center justify-between p-4 rounded-xl bg-muted/30">
                  <div>
                    <p className="font-medium">Thông báo đơn hàng mới</p>
                    <p className="text-sm text-muted-foreground">
                      Nhận thông báo khi có đơn hàng mới
                    </p>
                  </div>
                  <Button variant="outline" className="rounded-xl">
                    Bật
                  </Button>
                </div>

                <div className="flex items-center justify-between p-4 rounded-xl bg-muted/30">
                  <div>
                    <p className="font-medium">Email thông báo</p>
                    <p className="text-sm text-muted-foreground">
                      Gửi email cho các thông báo quan trọng
                    </p>
                  </div>
                  <Button variant="outline" className="rounded-xl">
                    Tắt
                  </Button>
                </div>
              </div>
            </div>
          </TabsContent>
        </Tabs>

        {/* Save Button */}
        <div className="flex justify-end">
          <Button onClick={handleSave} className="rounded-full" disabled={isSaving}>
            {isSaving ? (
              <>
                <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                Đang lưu...
              </>
            ) : (
              <>
                <Save className="mr-2 h-4 w-4" />
                Lưu thay đổi
              </>
            )}
          </Button>
        </div>
      </div>
    </MainLayout>
  );
}
