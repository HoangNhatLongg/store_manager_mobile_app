-- CreateTable
CREATE TABLE "sync_operations" (
    "id" TEXT NOT NULL,
    "operationId" TEXT NOT NULL,
    "entityType" TEXT NOT NULL,
    "entityId" TEXT NOT NULL,
    "operationType" TEXT NOT NULL,
    "status" TEXT NOT NULL,
    "result" JSONB,
    "errorCode" TEXT,
    "errorMessage" TEXT,
    "createdById" TEXT,
    "createdAt" TIMESTAMP(3) NOT NULL DEFAULT CURRENT_TIMESTAMP,
    "updatedAt" TIMESTAMP(3) NOT NULL,

    CONSTRAINT "sync_operations_pkey" PRIMARY KEY ("id")
);

-- CreateIndex
CREATE UNIQUE INDEX "sync_operations_operationId_key"
ON "sync_operations"("operationId");

-- CreateIndex
CREATE INDEX "sync_operations_entityType_entityId_idx"
ON "sync_operations"("entityType", "entityId");

-- CreateIndex
CREATE INDEX "sync_operations_createdById_idx"
ON "sync_operations"("createdById");
