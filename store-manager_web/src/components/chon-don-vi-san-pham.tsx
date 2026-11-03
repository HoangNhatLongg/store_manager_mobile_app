"use client";

import { useEffect, useState } from "react";
import { Label } from "@/components/ui/label";
import { Input } from "@/components/ui/input";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";

export interface DonViTinh {
  id: string;
  name: string;
  abbreviation: string;
  isBaseUnit: boolean;
}

/** Don vi da gan cho san pham. */
export interface DonViSanPham {
  id: string;
  unitId: string;
  conversionQty: number;
  price: string;
  isDefault: boolean;
  isPurchase: boolean;
  unit: DonViTinh;
}

/**
 * Du lieu don vi do form cha gui len server.
 *
 * `defaultUnit` la don vi ban le co ban, dung de dem ton kho.
 * `purchaseUnit` la don vi goc dung de nhap hang, co the null.
 */
export interface DonViForm {
  defaultUnit: { unitId: string; price: number } | null;
  purchaseUnit: { unitId: string; conversionQty: number; price: number } | null;
}

interface ChonDonViSanPhamProps {
  /** Danh sach don vi tinh cua he thong. */
  donViTinh: DonViTinh[];
  /** Don vi da gan cho san pham (dung khi sua san pham). */
  donViHienTai?: DonViSanPham[];
  /** Gia ban cua san pham, dung de tinh gia don vi goc khi chua nhap. */
  giaBan?: number;
  /** Gia tri hien tai cua form. */
  value: DonViForm;
  /** Callback khi nguoi dung thay doi. */
  onChange: (value: DonViForm) => void;
  /** Cho phep bo qua don vi goc. Mac dinh la true. */
  choPhepBoQuaDonViGoc?: boolean;
}

/**
 * Form chon don vi ban le mac dinh va don vi goc nhap hang cho san pham.
 *
 * Voi san pham moi, don vi ban le mac dinh la bat buoc. Don vi goc co the
 * chon sau, mac dinh trung voi don vi ban le mac dinh.
 */
export function ChonDonViSanPham({
  donViTinh,
  donViHienTai = [],
  giaBan = 0,
  value,
  onChange,
  choPhepBoQuaDonViGoc = true,
}: ChonDonViSanPhamProps) {
  const [heSoQuyDoi, setHeSoQuyDoi] = useState(1);

  // Khi sua san pham, nap he so quy doi cua don vi goc dang gan. Neu khong
  // nap, o he so quy doi se hien thi sai so voi du lieu da luu.
  useEffect(() => {
    const heSo = value.purchaseUnit?.conversionQty;
    if (heSo && heSo > 0) {
      setHeSoQuyDoi(heSo);
    }
    // Chi chay khi don vi goc thay doi.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [value.purchaseUnit?.unitId, value.purchaseUnit?.conversionQty]);

  // Lan dau mo form, tu dong chon don vi mac dinh da gan neu dang sua san pham.
  useEffect(() => {
    if (value.defaultUnit) return;

    const donViMacDinh =
      donViHienTai.find((donVi) => donVi.isDefault) ??
      donViHienTai.find((donVi) => donVi.conversionQty === 1);

    if (donViMacDinh) {
      onChange({
        defaultUnit: {
          unitId: donViMacDinh.unitId,
          price: parseFloat(donViMacDinh.price),
        },
        purchaseUnit: value.purchaseUnit,
      });
    }
    // Chi chay mot lan khi form duoc mo.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [donViHienTai]);

  const donViMacDinh = value.defaultUnit
    ? donViTinh.find((donVi) => donVi.id === value.defaultUnit?.unitId)
    : undefined;

  const donViGocChon = value.purchaseUnit
    ? donViTinh.find((donVi) => donVi.id === value.purchaseUnit?.unitId)
    : undefined;

  /**
   * Chon don vi ban le mac dinh. Don vi goc cu chinh thi giu nguyen.
   */
  const chonDonViMacDinh = (unitId: string) => {
    const donVi = donViTinh.find((item) => item.id === unitId);
    if (!donVi) return;

    const giaMacDinh = donViHienTai.find(
      (item) => item.unitId === unitId && item.isDefault
    );

    // Don vi goc dang chon khac don vi moi thi reset ve don vi mac dinh.
    const goc =
      value.purchaseUnit && value.purchaseUnit.unitId !== unitId
        ? null
        : value.purchaseUnit;

    onChange({
      defaultUnit: {
        unitId,
        price: giaMacDinh ? parseFloat(giaMacDinh.price) : giaBan,
      },
      purchaseUnit: goc,
    });
  };

  /**
   * Chon don vi goc nhap hang.
   */
  const chonDonViGoc = (unitId: string) => {
    if (unitId === "none") {
      // Bo don vi goc rieng, don vi mac dinh dong vai tro goc.
      onChange({
        defaultUnit: value.defaultUnit,
        purchaseUnit: null,
      });
      return;
    }

    const donVi = donViTinh.find((item) => item.id === unitId);
    if (!donVi) return;

    const giaMacDinh = value.defaultUnit?.price ?? giaBan;

    onChange({
      defaultUnit: value.defaultUnit,
      purchaseUnit: {
        unitId,
        conversionQty: heSoQuyDoi,
        price: giaMacDinh * heSoQuyDoi,
      },
    });
  };

  // Khi doi he so quy doi thi cap nhat lai gia cua don vi goc.
  const doiHeSoQuyDoi = (giaTri: number) => {
    const heSo = Math.max(1, giaTri || 1);
    setHeSoQuyDoi(heSo);

    if (!value.purchaseUnit) return;

    const giaMacDinh = value.defaultUnit?.price ?? giaBan;
    onChange({
      defaultUnit: value.defaultUnit,
      purchaseUnit: {
        ...value.purchaseUnit,
        conversionQty: heSo,
        price: giaMacDinh * heSo,
      },
    });
  };

  return (
    <div className="space-y-4 rounded-xl border p-4">
      <div className="space-y-2">
        <Label htmlFor="don-vi-mac-dinh">
          Đơn vị bán lẻ mặc định{" "}
          <span className="text-destructive">*</span>
        </Label>
        <Select
          value={value.defaultUnit?.unitId || "none"}
          onValueChange={chonDonViMacDinh}
        >
          <SelectTrigger id="don-vi-mac-dinh" className="rounded-xl">
            <SelectValue placeholder="Chọn đơn vị bán lẻ mặc định" />
          </SelectTrigger>
          <SelectContent>
            {donViTinh.map((donVi) => (
              <SelectItem key={donVi.id} value={donVi.id}>
                {donVi.name} ({donVi.abbreviation})
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <p className="text-sm text-muted-foreground">
          Đơn vị dùng để bán lẻ và tính tồn kho. Ví dụ: Lon, Chai, Gói.
        </p>
      </div>

      <div className="space-y-2">
        <Label htmlFor="don-vi-goc">Đơn vị gốc (nhập hàng)</Label>
        <Select
          value={value.purchaseUnit?.unitId || "none"}
          onValueChange={chonDonViGoc}
        >
          <SelectTrigger id="don-vi-goc" className="rounded-xl">
            <SelectValue placeholder="Dùng luôn đơn vị bán lẻ mặc định" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="none">
              Dùng luôn đơn vị bán lẻ mặc định
            </SelectItem>
            {donViTinh.map((donVi) => (
              <SelectItem key={donVi.id} value={donVi.id}>
                {donVi.name} ({donVi.abbreviation})
              </SelectItem>
            ))}
          </SelectContent>
        </Select>
        <p className="text-sm text-muted-foreground">
          Đơn vị dùng khi nhập hàng, ví dụ Thùng, Lốc. Hệ thống tự quy đổi
          sang đơn vị bán lẻ để cộng tồn kho.
        </p>
      </div>

      {value.purchaseUnit && donViGocChon && donViGocChon.id !== value.defaultUnit?.unitId && (
        <div className="space-y-2">
          <Label htmlFor="he-so-quy-doi">
            1 {donViGocChon.name} = bao nhiêu{" "}
            {donViMacDinh?.name ?? "đơn vị bán lẻ"}?
          </Label>
          <Input
            id="he-so-quy-doi"
            type="number"
            min="1"
            value={heSoQuyDoi}
            onChange={(e) => doiHeSoQuyDoi(parseInt(e.target.value) || 1)}
            className="rounded-xl"
          />
          {value.defaultUnit && (
            <p className="text-sm text-muted-foreground">
              Giá nhập dự kiến:{" "}
              {new Intl.NumberFormat("vi-VN", {
                style: "currency",
                currency: "VND",
              }).format(value.purchaseUnit.price)}{" "}
              / {donViGocChon.abbreviation}
            </p>
          )}
        </div>
      )}

      {!choPhepBoQuaDonViGoc && !value.purchaseUnit && (
        <p className="text-sm text-amber-600">
          Sản phẩm cần ít nhất một đơn vị để nhập hàng. Vui lòng chọn đơn vị
          gốc.
        </p>
      )}
    </div>
  );
}
