import { NextRequest, NextResponse } from "next/server";
import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/auth";

const MAX_FILE_SIZE = 10 * 1024 * 1024;

export const AVAILABLE_MODELS = [
  { id: "google/gemini-2.0-flash-001", name: "Gemini 2.0 Flash", provider: "Google", badge: "FREE" },
  { id: "anthropic/claude-sonnet-4", name: "Claude Sonnet 4", provider: "Anthropic", badge: "PRO" },
  { id: "openai/gpt-4o-mini", name: "GPT-4o Mini", provider: "OpenAI", badge: "CHEAP" },
  { id: "openai/gpt-4o", name: "GPT-4o", provider: "OpenAI", badge: "PRO" },
];

export async function GET() {
  return NextResponse.json({ models: AVAILABLE_MODELS });
}

export async function POST(request: NextRequest) {
  try {
    const session = await getServerSession(authOptions);
    if (!session) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const formData = await request.formData();
    const image = formData.get("image") as File | null;
    const model = formData.get("model") as string | null;

    if (!image) {
      return NextResponse.json({ error: "No image provided" }, { status: 400 });
    }

    if (image.size > MAX_FILE_SIZE) {
      return NextResponse.json({ error: "File too large" }, { status: 400 });
    }

    const allowedTypes = ["image/jpeg", "image/png", "image/webp", "image/jpg"];
    if (!allowedTypes.includes(image.type)) {
      return NextResponse.json({ error: "Invalid file type" }, { status: 400 });
    }

    const bytes = await image.arrayBuffer();
    const buffer = Buffer.from(bytes);
    const base64 = buffer.toString("base64");
    const dataUri = `data:${image.type};base64,${base64}`;

    const selectedModel = model || "google/gemini-2.0-flash-001";

    const prompt = `You are an expert at reading Vietnamese invoices/receipts. Extract product information with categories.

Available categories (match if applicable): Thực phẩm, Đồ uống, Bánh kẹo, Sữa, Rau củ, Trái cây, Thịt, Cá, Gia vị, Đồ gia dụng, Mỹ phẩm, Dược phẩm, Sách, Văn phòng phẩm, Khác

Return ONLY valid JSON (no markdown):
{"items":[{"productName":"Tên sản phẩm","quantity":1,"unitPrice":10000,"category":"Danh mục phù hợp nhất hoặc \"Khác\""}],"confidence":0.9}

Rules:
- Only list actual products, not totals or taxes
- Quantity and unitPrice must be numbers
- Match category from the available list or use "Khác"
- If category cannot be determined, use "Khác"`;

    // OpenRouter API configuration
    const apiKey = process.env.OPENROUTER_API_KEY;

    if (!apiKey) {
      return NextResponse.json(
        { error: "OPENROUTER_API_KEY not configured. Add to .env file." },
        { status: 500 }
      );
    }

    console.log("[OCR] Using OpenRouter with model:", selectedModel);

    // OpenRouter uses OpenAI-compatible API
    const response = await fetch("https://openrouter.ai/api/v1/chat/completions", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        Authorization: `Bearer ${apiKey}`,
        "HTTP-Referer": process.env.NEXTAUTH_URL || "http://localhost:3000",
        "X-Title": "Store Manager OCR",
      },
      body: JSON.stringify({
        model: selectedModel,
        messages: [{
          role: "user",
          content: [
            { type: "text", text: prompt },
            { type: "image_url", image_url: { url: dataUri, detail: "high" } },
          ],
        }],
        max_tokens: 2000,
      }),
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({}));
      const errorMsg = errorData?.error?.message || "Unknown error";
      console.error("[OCR] OpenRouter error:", errorMsg);
      return NextResponse.json(
        { error: `OpenRouter API error: ${errorMsg}` },
        { status: response.status }
      );
    }

    const data = await response.json();
    const content = data.choices?.[0]?.message?.content || "{}";

    console.log("[OCR] Response content:", content.substring(0, 200));

    try {
      const cleanContent = content.trim().replace(/^```json\n?|\n?```$/g, "");
      const result = JSON.parse(cleanContent.trim());
      return NextResponse.json({ ...result, model: selectedModel });
    } catch {
      console.error("[OCR] Failed to parse response:", content);
      return NextResponse.json({ items: [], confidence: 0 });
    }
  } catch (error) {
    console.error("OCR error:", error);
    return NextResponse.json({ error: "Internal server error" }, { status: 500 });
  }
}
