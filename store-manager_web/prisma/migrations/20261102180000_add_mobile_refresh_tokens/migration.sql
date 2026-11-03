-- CreateTable
CREATE TABLE "mobile_refresh_tokens" (
    "id" TEXT NOT NULL,
    "tokenId" TEXT NOT NULL,
    "userId" TEXT NOT NULL,
    "expiresAt" TIMESTAMP(3) NOT NULL,
    "revokedAt" TIMESTAMP(3),
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT "mobile_refresh_tokens_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "mobile_refresh_tokens_tokenId_key"
ON "mobile_refresh_tokens"("tokenId");

-- CreateIndex
CREATE INDEX "mobile_refresh_tokens_userId_idx"
ON "mobile_refresh_tokens"("userId");

-- AddForeignKey
ALTER TABLE "mobile_refresh_tokens"
ADD CONSTRAINT "mobile_refresh_tokens_userId_fkey"
FOREIGN KEY ("userId") REFERENCES "users"("id") ON DELETE CASCADE ON UPDATE CASCADE;
