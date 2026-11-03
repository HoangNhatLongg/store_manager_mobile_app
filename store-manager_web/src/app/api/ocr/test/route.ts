import { NextResponse } from "next/server";
import { getServerSession } from "next-auth";
import { authOptions } from "@/lib/auth";

export async function POST() {
  try {
    const session = await getServerSession(authOptions);
    if (!session) {
      return NextResponse.json({ error: "Unauthorized" }, { status: 401 });
    }

    const apiKey = process.env.OPENROUTER_API_KEY;
    const model = process.env.OPENROUTER_MODEL || "google/gemini-2.0-flash-001";

    if (!apiKey) {
      return NextResponse.json({
        success: false,
        error: "OPENROUTER_API_KEY not configured. Add to .env file."
      }, { status: 500 });
    }

    console.log("[Test] Testing OpenRouter connection...");

    try {
      const response = await fetch("https://openrouter.ai/api/v1/chat/completions", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Authorization: `Bearer ${apiKey}`,
          "HTTP-Referer": process.env.NEXTAUTH_URL || "http://localhost:3000",
          "X-Title": "Store Manager OCR",
        },
        body: JSON.stringify({
          model: model,
          messages: [
            {
              role: "user",
              content: "Reply with exactly: {\"status\": \"ok\"}"
            }
          ],
          max_tokens: 50,
        }),
      });

      if (response.ok) {
        const data = await response.json();
        return NextResponse.json({
          success: true,
          message: "Connected successfully to OpenRouter",
          model: model,
          provider: "OpenRouter",
        });
      } else {
        const errorData = await response.json().catch(() => ({}));
        const errorMsg = errorData?.error?.message || "Unknown error";
        
        return NextResponse.json({
          success: false,
          error: `OpenRouter Error: ${errorMsg}`,
          status: response.status,
        }, { status: response.status });
      }
    } catch (fetchError) {
      return NextResponse.json({
        success: false,
        error: `Connection failed: ${fetchError instanceof Error ? fetchError.message : "Network error"}`,
      }, { status: 500 });
    }
  } catch (error) {
    console.error("Test connection error:", error);
    return NextResponse.json(
      { error: "Internal server error" },
      { status: 500 }
    );
  }
}
