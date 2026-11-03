"use client";

import { useState, useEffect } from "react";
import { MainLayout } from "@/components/layout";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  DialogDescription,
} from "@/components/ui/dialog";
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table";
import { Switch } from "@/components/ui/switch";
import {
  Plus,
  Pencil,
  Trash2,
  Search,
  Ruler,
  Loader2,
  Package,
} from "lucide-react";

interface Unit {
  id: string;
  name: string;
  abbreviation: string;
  isBaseUnit: boolean;
  createdAt: string;
  _count?: {
    products: number;
  };
}

interface UnitForm {
  name: string;
  abbreviation: string;
  isBaseUnit: boolean;
}

const emptyForm: UnitForm = {
  name: "",
  abbreviation: "",
  isBaseUnit: false,
};

export default function UnitsPage() {
  const [units, setUnits] = useState<Unit[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [isDialogOpen, setIsDialogOpen] = useState(false);
  const [isDeleteDialogOpen, setIsDeleteDialogOpen] = useState(false);
  const [editingUnit, setEditingUnit] = useState<Unit | null>(null);
  const [deletingUnit, setDeletingUnit] = useState<Unit | null>(null);
  const [form, setForm] = useState<UnitForm>(emptyForm);
  const [isSubmitting, setIsSubmitting] = useState(false);

  useEffect(() => {
    fetchUnits();
  }, []);

  async function fetchUnits() {
    setIsLoading(true);
    try {
      const res = await fetch("/api/units");
      const data = await res.json();
      setUnits(data);
    } catch (error) {
      console.error("Error fetching units:", error);
    } finally {
      setIsLoading(false);
    }
  }

  function openCreateDialog() {
    setEditingUnit(null);
    setForm(emptyForm);
    setIsDialogOpen(true);
  }

  function openEditDialog(unit: Unit) {
    setEditingUnit(unit);
    setForm({
      name: unit.name,
      abbreviation: unit.abbreviation,
      isBaseUnit: unit.isBaseUnit,
    });
    setIsDialogOpen(true);
  }

  function openDeleteDialog(unit: Unit) {
    setDeletingUnit(unit);
    setIsDeleteDialogOpen(true);
  }

  async function handleSubmit() {
    if (!form.name.trim()) {
      alert("Tên đơn vị là bắt buộc");
      return;
    }
    if (!form.abbreviation.trim()) {
      alert("Viết tắt là bắt buộc");
      return;
    }

    setIsSubmitting(true);
    try {
      const url = editingUnit
        ? `/api/units/${editingUnit.id}`
        : "/api/units";
      const method = editingUnit ? "PUT" : "POST";

      const res = await fetch(url, {
        method,
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify(form),
      });

      if (!res.ok) {
        const error = await res.json();
        alert(error.error || "Có lỗi xảy ra");
        return;
      }

      setIsDialogOpen(false);
      fetchUnits();
    } catch (error) {
      console.error("Error:", error);
      alert("Có lỗi xảy ra");
    } finally {
      setIsSubmitting(false);
    }
  }

  async function handleDelete() {
    if (!deletingUnit) return;

    setIsSubmitting(true);
    try {
      const res = await fetch(`/api/units/${deletingUnit.id}`, {
        method: "DELETE",
      });

      if (!res.ok) {
        const error = await res.json();
        alert(error.error || "Có lỗi xảy ra");
        return;
      }

      setIsDeleteDialogOpen(false);
      setDeletingUnit(null);
      fetchUnits();
    } catch (error) {
      console.error("Error:", error);
      alert("Có lỗi xảy ra");
    } finally {
      setIsSubmitting(false);
    }
  }

  const filteredUnits = units.filter(
    (u) =>
      u.name.toLowerCase().includes(search.toLowerCase()) ||
      u.abbreviation.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <MainLayout>
      <div className="space-y-6">
        {/* Header */}
        <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
          <div>
            <h1 className="text-3xl font-semibold tracking-tight">
              Đơn vị tính
            </h1>
            <p className="text-muted-foreground mt-1">
              Quản lý đơn vị tính cho sản phẩm
            </p>
          </div>
          <Button onClick={openCreateDialog} className="rounded-xl">
            <Plus className="mr-2 h-4 w-4" />
            Thêm đơn vị
          </Button>
        </div>

        {/* Search */}
        <div className="relative max-w-md">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Tìm kiếm đơn vị..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pl-10 rounded-xl"
          />
        </div>

        {/* Info Card */}
        <div className="bg-blue-50 dark:bg-blue-950/20 border border-blue-200 dark:border-blue-800 rounded-2xl p-4">
          <div className="flex items-start gap-3">
            <Ruler className="h-5 w-5 text-blue-600 dark:text-blue-400 mt-0.5" />
            <div>
              <h3 className="font-medium text-blue-900 dark:text-blue-100">
                Đơn vị cơ bản vs Đơn vị quy đổi
              </h3>
              <p className="text-sm text-blue-700 dark:text-blue-300 mt-1">
                <strong>Đơn vị cơ bản</strong> (Lon, Chai, Cái) là đơn vị nhỏ nhất dùng để bán lẻ.
                <strong>Đơn vị quy đổi</strong> (Thùng) chứa nhiều đơn vị cơ bản (VD: 1 Thùng = 24 Lon).
              </p>
            </div>
          </div>
        </div>

        {/* Table */}
        <div className="bg-card rounded-2xl overflow-hidden">
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead>Tên đơn vị</TableHead>
                <TableHead>Viết tắt</TableHead>
                <TableHead>Loại</TableHead>
                <TableHead className="text-center">Số sản phẩm</TableHead>
                <TableHead className="text-right">Thao tác</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {isLoading ? (
                <TableRow>
                  <TableCell colSpan={5} className="text-center py-8">
                    <Loader2 className="h-8 w-8 animate-spin mx-auto text-muted-foreground" />
                  </TableCell>
                </TableRow>
              ) : filteredUnits.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={5} className="text-center py-8 text-muted-foreground">
                    Chưa có đơn vị nào
                  </TableCell>
                </TableRow>
              ) : (
                filteredUnits.map((unit) => (
                  <TableRow key={unit.id}>
                    <TableCell>
                      <div className="flex items-center gap-3">
                        <div className="h-10 w-10 rounded-lg bg-purple-100 dark:bg-purple-900/50 flex items-center justify-center">
                          <Ruler className="h-5 w-5 text-purple-600 dark:text-purple-400" />
                        </div>
                        <span className="font-medium">{unit.name}</span>
                      </div>
                    </TableCell>
                    <TableCell>
                      <code className="bg-muted px-2 py-1 rounded text-sm">
                        {unit.abbreviation}
                      </code>
                    </TableCell>
                    <TableCell>
                      {unit.isBaseUnit ? (
                        <span className="inline-flex items-center px-2 py-1 rounded-lg text-xs font-medium bg-green-100 text-green-700 dark:bg-green-900/50 dark:text-green-400">
                          Đơn vị cơ bản
                        </span>
                      ) : (
                        <span className="inline-flex items-center px-2 py-1 rounded-lg text-xs font-medium bg-amber-100 text-amber-700 dark:bg-amber-900/50 dark:text-amber-400">
                          Đơn vị quy đổi
                        </span>
                      )}
                    </TableCell>
                    <TableCell className="text-center">
                      <div className="flex items-center justify-center gap-1">
                        <Package className="h-4 w-4 text-muted-foreground" />
                        {unit._count?.products || 0}
                      </div>
                    </TableCell>
                    <TableCell className="text-right">
                      <div className="flex justify-end gap-2">
                        <Button
                          variant="ghost"
                          size="icon"
                          className="rounded-xl"
                          onClick={() => openEditDialog(unit)}
                        >
                          <Pencil className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="icon"
                          className="rounded-xl text-destructive hover:text-destructive"
                          onClick={() => openDeleteDialog(unit)}
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </div>

        <p className="text-sm text-muted-foreground">
          Tổng cộng: {filteredUnits.length} đơn vị
        </p>
      </div>

      {/* Create/Edit Dialog */}
      <Dialog open={isDialogOpen} onOpenChange={setIsDialogOpen}>
        <DialogContent className="rounded-2xl">
          <DialogHeader>
            <DialogTitle>
              {editingUnit ? "Sửa đơn vị" : "Thêm đơn vị mới"}
            </DialogTitle>
            <DialogDescription>
              {editingUnit
                ? "Cập nhật thông tin đơn vị"
                : "Thêm đơn vị tính mới cho sản phẩm"}
            </DialogDescription>
          </DialogHeader>

          <div className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="name">
                Tên đơn vị <span className="text-destructive">*</span>
              </Label>
              <Input
                id="name"
                value={form.name}
                onChange={(e) => setForm({ ...form, name: e.target.value })}
                placeholder="VD: Thùng, Lon, Chai"
                className="rounded-xl"
              />
            </div>

            <div className="space-y-2">
              <Label htmlFor="abbreviation">
                Viết tắt <span className="text-destructive">*</span>
              </Label>
              <Input
                id="abbreviation"
                value={form.abbreviation}
                onChange={(e) => setForm({ ...form, abbreviation: e.target.value.toLowerCase() })}
                placeholder="VD: thùng, lon, chai"
                className="rounded-xl"
              />
            </div>

            <div className="flex items-center justify-between rounded-xl border p-4">
              <div className="space-y-0.5">
                <Label htmlFor="isBaseUnit" className="cursor-pointer">
                  Đơn vị cơ bản
                </Label>
                <p className="text-sm text-muted-foreground">
                  Đơn vị nhỏ nhất dùng để bán lẻ
                </p>
              </div>
              <Switch
                id="isBaseUnit"
                checked={form.isBaseUnit}
                onCheckedChange={(checked) => setForm({ ...form, isBaseUnit: checked })}
              />
            </div>
          </div>

          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setIsDialogOpen(false)}
              className="rounded-xl"
            >
              Hủy
            </Button>
            <Button
              onClick={handleSubmit}
              disabled={isSubmitting}
              className="rounded-xl"
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Đang xử lý...
                </>
              ) : editingUnit ? (
                "Lưu thay đổi"
              ) : (
                "Thêm mới"
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* Delete Confirmation Dialog */}
      <Dialog open={isDeleteDialogOpen} onOpenChange={setIsDeleteDialogOpen}>
        <DialogContent className="rounded-2xl">
          <DialogHeader>
            <DialogTitle>Xóa đơn vị</DialogTitle>
            <DialogDescription>
              Bạn có chắc chắn muốn xóa đơn vị "{deletingUnit?.name}" không?
              Hành động này không thể hoàn tác.
            </DialogDescription>
          </DialogHeader>

          {deletingUnit?._count?.products ? (
            <div className="p-4 bg-amber-50 dark:bg-amber-900/20 rounded-xl">
              <p className="text-sm text-amber-700 dark:text-amber-400">
                Cảnh báo: Đơn vị này đang được sử dụng bởi {deletingUnit._count.products} sản phẩm.
                Không thể xóa đơn vị đang được sử dụng.
              </p>
            </div>
          ) : null}

          <DialogFooter>
            <Button
              variant="outline"
              onClick={() => setIsDeleteDialogOpen(false)}
              className="rounded-xl"
            >
              Hủy
            </Button>
            <Button
              variant="destructive"
              onClick={handleDelete}
              disabled={isSubmitting || (deletingUnit?._count?.products ?? 0) > 0}
              className="rounded-xl"
            >
              {isSubmitting ? (
                <>
                  <Loader2 className="mr-2 h-4 w-4 animate-spin" />
                  Đang xóa...
                </>
              ) : (
                "Xóa"
              )}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </MainLayout>
  );
}
