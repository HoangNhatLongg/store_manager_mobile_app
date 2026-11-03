"use client";

import { useState } from "react";
import { MainLayout } from "@/components/layout";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs";
import {
  Bot,
  Check,
  X,
  Loader2,
  FlaskConical,
  Upload,
  AlertCircle,
  Sparkles,
  Wifi,
  WifiOff,
  RefreshCw,
} from "lucide-react";

const MAX_FILE_SIZE = 10 * 1024 * 1024;
const ACCEPTED_TYPES = ["image/jpeg", "image/png", "image/webp", "image/jpg"];

interface OCRResult {
  items: Array<{
    productName: string;
    quantity: number;
    unitPrice: number;
  }>;
  confidence: number;
  model?: string;
}

interface ConnectionStatus {
  status: "idle" | "checking" | "success" | "error";
  message: string;
  apiKeyValid?: boolean;
  model?: string;
  provider?: string;
}

const formatCurrency = (value: number) => {
  return new Intl.NumberFormat("vi-VN", {
    style: "currency",
    currency: "VND",
    minimumFractionDigits: 0,
  }).format(value);
};

const MODELS = [
  { id: "google/gemini-2.5-flash", name: "Gemini 2.5 Flash", provider: "Google", badge: "FREE" },
  { id: "anthropic/claude-sonnet-4", name: "Claude Sonnet 4", provider: "Anthropic", badge: "PRO" },
  { id: "openai/gpt-4o-mini", name: "GPT-4o Mini", provider: "OpenAI", badge: "CHEAP" },
  { id: "openai/gpt-4o", name: "GPT-4o", provider: "OpenAI", badge: "PRO" },
];

export default function AIOCRPage() {
  const [selectedModel, setSelectedModel] = useState("google/gemini-2.0-flash-001");
  const [isProcessing, setIsProcessing] = useState(false);
  const [isTestingConnection, setIsTestingConnection] = useState(false);
  const [resultError, setResultError] = useState<string | null>(null);
  
  const [testImage, setTestImage] = useState<string | null>(null);
  const [testImageFile, setTestImageFile] = useState<File | null>(null);
  const [ocrResult, setOcrResult] = useState<OCRResult | null>(null);
  const [connectionStatus, setConnectionStatus] = useState<ConnectionStatus>({
    status: "idle",
    message: "Chưa kiểm tra kết nối",
  });

  async function handleTestConnection() {
    setIsTestingConnection(true);
    setConnectionStatus({ status: "checking", message: "Đang kiểm tra kết nối..." });

    try {
      const res = await fetch("/api/ocr/test", {
        method: "POST",
      });

      const data = await res.json();

      if (res.ok && data.success) {
        setConnectionStatus({
          status: "success",
          message: "Kết nối thành công!",
          apiKeyValid: true,
          model: data.model,
          provider: data.provider,
        });
      } else {
        setConnectionStatus({
          status: "error",
          message: data.error || "Kết nối thất bại",
          apiKeyValid: false,
        });
      }
    } catch (err) {
      setConnectionStatus({
        status: "error",
        message: err instanceof Error ? err.message : "Không thể kết nối server",
        apiKeyValid: false,
      });
    } finally {
      setIsTestingConnection(false);
    }
  }

  function handleImageSelect(event: React.ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    if (!file) return;

    if (!ACCEPTED_TYPES.includes(file.type)) {
      setResultError("Chỉ chấp nhận file ảnh (JPG, PNG, WebP)");
      return;
    }

    if (file.size > MAX_FILE_SIZE) {
      setResultError("File quá lớn. Vui lòng chọn file nhỏ hơn 10MB");
      return;
    }

    setResultError(null);
    setTestImageFile(file);
    
    const reader = new FileReader();
    reader.onload = (e) => {
      setTestImage(e.target?.result as string);
      setOcrResult(null);
    };
    reader.readAsDataURL(file);
  }

  function clearImage() {
    setTestImage(null);
    setTestImageFile(null);
    setOcrResult(null);
    setResultError(null);
  }

  async function handleTest() {
    if (!testImageFile) return;

    setIsProcessing(true);
    setResultError(null);
    setOcrResult(null);

    try {
      const formData = new FormData();
      formData.append("image", testImageFile);
      formData.append("model", selectedModel);

      const res = await fetch("/api/ocr/process", {
        method: "POST",
        body: formData,
      });

      const data = await res.json();

      if (!res.ok) {
        setResultError(data.error || "Có lỗi xảy ra");
        return;
      }

      setOcrResult(data);
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : "Có lỗi xảy ra";
      setResultError(errorMessage);
    } finally {
      setIsProcessing(false);
    }
  }

  return (
    <MainLayout>
      <div className="space-y-8">
        <div>
          <h1 className="text-3xl font-semibold tracking-tight">AI OCR</h1>
          <p className="text-muted-foreground mt-1">
            Nhận diện hóa đơn bằng OpenRouter AI
          </p>
        </div>

        <Tabs defaultValue="test" className="space-y-6">
          <TabsList className="rounded-xl">
            <TabsTrigger value="test" className="rounded-lg">
              <FlaskConical className="mr-2 h-4 w-4" />
              Thử nghiệm
            </TabsTrigger>
            <TabsTrigger value="connection" className="rounded-lg">
              <Wifi className="mr-2 h-4 w-4" />
              Kết nối
            </TabsTrigger>
            <TabsTrigger value="config" className="rounded-lg">
              <Bot className="mr-2 h-4 w-4" />
              Cấu hình
            </TabsTrigger>
          </TabsList>

          {/* Test Tab */}
          <TabsContent value="test" className="space-y-6">
            {/* Model Selection */}
            <div className="bg-card rounded-2xl p-4">
              <div className="flex items-center gap-4 flex-wrap">
                <Label className="whitespace-nowrap">Chọn mô hình:</Label>
                <Select value={selectedModel} onValueChange={setSelectedModel}>
                  <SelectTrigger className="w-[250px] rounded-xl">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {MODELS.map((model) => (
                      <SelectItem key={model.id} value={model.id}>
                        <div className="flex items-center gap-2">
                          <span>{model.name}</span>
                          {model.badge === "FREE" && (
                            <span className="text-xs bg-green-100 text-green-700 px-1.5 py-0.5 rounded">
                              FREE
                            </span>
                          )}
                          {model.badge === "CHEAP" && (
                            <span className="text-xs bg-blue-100 text-blue-700 px-1.5 py-0.5 rounded">
                              CHEAP
                            </span>
                          )}
                        </div>
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                <span className="text-sm text-muted-foreground">
                  {MODELS.find(m => m.id === selectedModel)?.provider}
                </span>
              </div>
            </div>

            <div className="grid gap-6 lg:grid-cols-2">
              {/* Upload */}
              <div className="bg-card rounded-2xl p-6 space-y-4">
                <h2 className="text-lg font-medium flex items-center gap-2">
                  <Upload className="h-5 w-5" />
                  Tải ảnh lên
                </h2>
                
                <div 
                  className="border-2 border-dashed border-border rounded-2xl p-8 text-center hover:border-primary/50 transition-colors cursor-pointer"
                  onClick={() => document.getElementById("imageInput")?.click()}
                >
                  <input
                    id="imageInput"
                    type="file"
                    accept={ACCEPTED_TYPES.join(",")}
                    onChange={handleImageSelect}
                    className="hidden"
                  />
                  
                  {testImage ? (
                    <div className="space-y-4">
                      {/* eslint-disable-next-line @next/next/no-img-element */}
                      <img
                        src={testImage}
                        alt="Preview"
                        className="max-h-64 mx-auto rounded-xl shadow-sm"
                      />
                      <Button
                        variant="outline"
                        size="sm"
                        onClick={(e) => {
                          e.stopPropagation();
                          clearImage();
                        }}
                        className="rounded-xl"
                      >
                        <X className="h-4 w-4 mr-2" />
                        Xóa ảnh
                      </Button>
                    </div>
                  ) : (
                    <>
                      <Upload className="h-12 w-12 mx-auto mb-4 text-muted-foreground" />
                      <p className="text-lg font-medium">Kéo thả hoặc click để chọn ảnh</p>
                      <p className="text-sm text-muted-foreground mt-2">
                        Chấp nhận: JPG, PNG, WebP (tối đa 10MB)
                      </p>
                    </>
                  )}
                </div>

                {resultError && (
                  <div className="flex items-center gap-2 p-3 bg-destructive/10 text-destructive rounded-xl">
                    <AlertCircle className="h-4 w-4" />
                    <p className="text-sm">{resultError}</p>
                  </div>
                )}

                <Button
                  className="w-full rounded-xl"
                  onClick={handleTest}
                  disabled={!testImageFile || isProcessing}
                >
                  {isProcessing ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      Đang xử lý...
                    </>
                  ) : (
                    <>
                      <Sparkles className="mr-2 h-4 w-4" />
                      Nhận diện hóa đơn
                    </>
                  )}
                </Button>
              </div>

              {/* Result */}
              <div className="bg-card rounded-2xl p-6 space-y-4">
                <h2 className="text-lg font-medium">Kết quả</h2>
                
                {!ocrResult && !resultError ? (
                  <div className="text-center py-16 text-muted-foreground">
                    <FlaskConical className="h-12 w-12 mx-auto mb-4 opacity-50" />
                    <p>Chưa có kết quả</p>
                    <p className="text-sm">
                      Tải ảnh lên và nhấn &quot;Nhận diện&quot;
                    </p>
                  </div>
                ) : ocrResult ? (
                  <div className="space-y-4">
                    <div className="flex items-center justify-between text-sm">
                      <span className="text-muted-foreground">Model:</span>
                      <span className="font-medium">{ocrResult.model || selectedModel}</span>
                    </div>
                    <div className="flex items-center justify-between text-sm">
                      <span className="text-muted-foreground">Độ chính xác:</span>
                      <span className="font-medium">
                        {Math.round((ocrResult.confidence || 0) * 100)}%
                      </span>
                    </div>

                    {ocrResult.items && ocrResult.items.length > 0 ? (
                      <div className="border rounded-xl overflow-hidden">
                        <table className="w-full text-sm">
                          <thead className="bg-muted">
                            <tr>
                              <th className="text-left p-3 font-medium">Sản phẩm</th>
                              <th className="text-center p-3 font-medium">SL</th>
                              <th className="text-right p-3 font-medium">Đơn giá</th>
                            </tr>
                          </thead>
                          <tbody>
                            {ocrResult.items.map((item, i) => (
                              <tr key={i} className="border-t">
                                <td className="p-3">{item.productName}</td>
                                <td className="p-3 text-center">{item.quantity}</td>
                                <td className="p-3 text-right">{formatCurrency(item.unitPrice)}</td>
                              </tr>
                            ))}
                          </tbody>
                        </table>
                      </div>
                    ) : (
                      <p className="text-center text-muted-foreground py-4">
                        Không tìm thấy sản phẩm nào
                      </p>
                    )}
                  </div>
                ) : null}
              </div>
            </div>
          </TabsContent>

          {/* Connection Tab */}
          <TabsContent value="connection" className="space-y-6">
            <div className="bg-card rounded-2xl p-6 space-y-6">
              <div className="flex items-center justify-between">
                <div>
                  <h2 className="text-lg font-medium flex items-center gap-2">
                    <Wifi className="h-5 w-5" />
                    Kiểm tra kết nối OpenRouter
                  </h2>
                  <p className="text-sm text-muted-foreground mt-1">
                    Xác minh API key và kết nối với OpenRouter
                  </p>
                </div>
                <Button
                  variant="outline"
                  className="rounded-xl"
                  onClick={handleTestConnection}
                  disabled={isTestingConnection}
                >
                  {isTestingConnection ? (
                    <>
                      <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                      Đang kiểm tra...
                    </>
                  ) : (
                    <>
                      <RefreshCw className="mr-2 h-4 w-4" />
                      Kiểm tra kết nối
                    </>
                  )}
                </Button>
              </div>

              {/* Status Card */}
              <div className={`rounded-xl p-6 ${
                connectionStatus.status === "success" 
                  ? "bg-green-50 dark:bg-green-950/30 border border-green-200 dark:border-green-800"
                  : connectionStatus.status === "error"
                  ? "bg-red-50 dark:bg-red-950/30 border border-red-200 dark:border-red-800"
                  : connectionStatus.status === "checking"
                  ? "bg-blue-50 dark:bg-blue-950/30 border border-blue-200 dark:border-blue-800"
                  : "bg-muted"
              }`}>
                <div className="flex items-center gap-4">
                  {connectionStatus.status === "success" && (
                    <div className="p-3 bg-green-100 dark:bg-green-900 rounded-full">
                      <Check className="h-6 w-6 text-green-600 dark:text-green-400" />
                    </div>
                  )}
                  {connectionStatus.status === "error" && (
                    <div className="p-3 bg-red-100 dark:bg-red-900 rounded-full">
                      <X className="h-6 w-6 text-red-600 dark:text-red-400" />
                    </div>
                  )}
                  {connectionStatus.status === "checking" && (
                    <div className="p-3 bg-blue-100 dark:bg-blue-900 rounded-full">
                      <Loader2 className="h-6 w-6 text-blue-600 dark:text-blue-400 animate-spin" />
                    </div>
                  )}
                  {connectionStatus.status === "idle" && (
                    <div className="p-3 bg-muted-foreground/20 rounded-full">
                      <WifiOff className="h-6 w-6 text-muted-foreground" />
                    </div>
                  )}
                  
                  <div className="flex-1">
                    <p className={`font-medium ${
                      connectionStatus.status === "success" 
                        ? "text-green-700 dark:text-green-300"
                        : connectionStatus.status === "error"
                        ? "text-red-700 dark:text-red-300"
                        : ""
                    }`}>
                      {connectionStatus.status === "idle" && "Chưa kiểm tra"}
                      {connectionStatus.status === "checking" && "Đang kiểm tra..."}
                      {connectionStatus.status === "success" && "Kết nối thành công!"}
                      {connectionStatus.status === "error" && "Kết nối thất bại"}
                    </p>
                    <p className="text-sm text-muted-foreground mt-1">
                      {connectionStatus.message}
                    </p>
                    {connectionStatus.model && (
                      <p className="text-sm mt-2">
                        <span className="text-muted-foreground">Model: </span>
                        <span className="font-medium">{connectionStatus.model}</span>
                      </p>
                    )}
                    {connectionStatus.provider && (
                      <p className="text-sm mt-1">
                        <span className="text-muted-foreground">Provider: </span>
                        <span className="font-medium">{connectionStatus.provider}</span>
                      </p>
                    )}
                  </div>
                </div>
              </div>

              {/* Instructions */}
              {connectionStatus.status === "error" && (
                <div className="bg-yellow-50 dark:bg-yellow-950/30 border border-yellow-200 dark:border-yellow-800 rounded-xl p-4">
                  <p className="font-medium text-yellow-800 dark:text-yellow-200 mb-2">
                    Hướng dẫn khắc phục:
                  </p>
                  <ol className="text-sm text-yellow-700 dark:text-yellow-300 space-y-1 list-decimal list-inside">
                    <li>Lấy API key tại <a href="https://openrouter.ai/keys" target="_blank" className="underline">openrouter.ai/keys</a></li>
                    <li>Thêm vào file <code className="bg-yellow-100 dark:bg-yellow-900 px-1 rounded">.env</code>: OPENROUTER_API_KEY=&quot;sk-or-...&quot;</li>
                    <li>Restart dev server</li>
                    <li>Kiểm tra lại kết nối</li>
                  </ol>
                </div>
              )}
            </div>
          </TabsContent>

          {/* Config Tab */}
          <TabsContent value="config" className="space-y-6">
            <div className="bg-card rounded-2xl p-6 space-y-4">
              <h2 className="text-lg font-medium flex items-center gap-2">
                <Bot className="h-5 w-5" />
                OpenRouter Configuration
              </h2>
              
              <div className="bg-muted/50 rounded-xl p-4">
                <p className="text-sm font-medium mb-2">Cấu hình API Key:</p>
                <p className="text-sm text-muted-foreground">
                  Thêm OpenRouter API key vào file <code className="bg-muted px-1 rounded">.env</code>:
                </p>
                <div className="bg-muted p-2 rounded mt-2 text-sm font-mono">
                  OPENROUTER_API_KEY=&quot;sk-or-v1-...&quot;
                </div>
              </div>

              <div className="bg-blue-50 dark:bg-blue-950/30 border border-blue-200 dark:border-blue-800 rounded-xl p-4">
                <p className="font-medium text-blue-800 dark:text-blue-200 mb-2">
                  Tại sao nên dùng OpenRouter?
                </p>
                <ul className="text-sm text-blue-700 dark:text-blue-300 space-y-1">
                  <li>• Hỗ trợ 300+ models từ nhiều nhà cung cấp</li>
                  <li>• Gemini 2.0 Flash <strong>MIỄN PHÍ</strong></li>
                  <li>• Claude, GPT, Gemini trong một API</li>
                  <li>• Đăng ký tại <a href="https://openrouter.ai" target="_blank" className="underline">openrouter.ai</a></li>
                </ul>
              </div>

              <div className="bg-muted/50 rounded-xl p-4">
                <p className="text-sm font-medium mb-2">Models có sẵn:</p>
                <div className="grid gap-2">
                  {MODELS.map((model) => (
                    <div key={model.id} className="flex items-center justify-between text-sm">
                      <span>{model.name}</span>
                      {model.badge === "FREE" && (
                        <span className="text-xs bg-green-100 text-green-700 px-2 py-0.5 rounded">
                          FREE
                        </span>
                      )}
                      {model.badge === "CHEAP" && (
                        <span className="text-xs bg-blue-100 text-blue-700 px-2 py-0.5 rounded">
                          CHEAP
                        </span>
                      )}
                      {model.badge === "PRO" && (
                        <span className="text-xs bg-purple-100 text-purple-700 px-2 py-0.5 rounded">
                          PRO
                        </span>
                      )}
                    </div>
                  ))}
                </div>
              </div>
            </div>
          </TabsContent>
        </Tabs>
      </div>
    </MainLayout>
  );
}
