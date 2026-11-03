'use client';

import Link from 'next/link';
import { usePathname } from 'next/navigation';
import {
  LayoutDashboard,
  Package,
  Tags,
  FileText,
  ShoppingCart,
  Archive,
  Bot,
  Settings,
  ChevronLeft,
  ChevronRight,
  Menu,
  X,
  Truck,
  Ruler,
  TrendingUp,
} from 'lucide-react';
import { useState } from 'react';
import { cn } from '@/lib/utils';
import { Button } from '@/components/ui/button';

const navigation = [
  { name: 'Dashboard', href: '/', icon: LayoutDashboard },
  { name: 'Sản phẩm', href: '/products', icon: Package },
  { name: 'Danh mục', href: '/categories', icon: Tags },
  { name: 'Đơn vị', href: '/units', icon: Ruler },
  { name: 'Nhà cung cấp', href: '/suppliers', icon: Truck },
  { name: 'Nhập hàng', href: '/import', icon: FileText },
  { name: 'Bán hàng', href: '/sales', icon: ShoppingCart },
  { name: 'Doanh thu', href: '/revenue', icon: TrendingUp },
  { name: 'Tồn kho', href: '/inventory', icon: Archive },
  { name: 'AI OCR', href: '/ai-ocr', icon: Bot },
  { name: 'Cài đặt', href: '/settings', icon: Settings },
];

export function Sidebar() {
  const pathname = usePathname();
  const [isCollapsed, setIsCollapsed] = useState(false);
  const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

  return (
    <>
      {/* Mobile Menu Button */}
      <button
        className="lg:hidden fixed top-4 left-4 z-50 p-2.5 bg-background/90 backdrop-blur-sm border border-border/50 rounded-xl shadow-sm"
        onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
      >
        {isMobileMenuOpen ? <X size={20} /> : <Menu size={20} />}
      </button>

      {/* Mobile Overlay */}
      {isMobileMenuOpen && (
        <div
          className="lg:hidden fixed inset-0 bg-background/80 backdrop-blur-sm z-30"
          onClick={() => setIsMobileMenuOpen(false)}
        />
      )}

      {/* Sidebar */}
      <aside
        className={cn(
          'fixed inset-y-0 left-0 z-40 flex flex-col transition-all duration-300',
          isCollapsed ? 'w-[72px]' : 'w-64',
          isMobileMenuOpen
            ? 'translate-x-0'
            : '-translate-x-full lg:translate-x-0',
        )}
      >
        <div
          className={cn(
            'flex flex-col h-full bg-background',
            !isCollapsed && 'border-r border-border/50',
          )}
        >
          {/* Logo */}
          <div
            className={cn(
              'flex items-center h-16 px-4',
              isCollapsed ? 'justify-center' : 'justify-between',
            )}
          >
            {!isCollapsed && (
              <Link href="/" className="flex items-center gap-3">
                <div className="h-9 w-9 rounded-xl bg-primary flex items-center justify-center shadow-sm">
                  <span className="text-lg font-bold text-primary-foreground">
                    S
                  </span>
                </div>
                <div>
                  <h1 className="text-lg font-semibold">Store Manager</h1>
                  <p className="text-xs text-muted-foreground">
                    Quản lý tự động
                  </p>
                </div>
              </Link>
            )}
            {isCollapsed && (
              <div className="h-9 w-9 rounded-xl bg-primary flex items-center justify-center shadow-sm">
                <span className="text-lg font-bold text-primary-foreground">
                  S
                </span>
              </div>
            )}
          </div>

          {/* Navigation */}
          <nav className="flex-1 px-3 py-4 space-y-1 overflow-y-auto">
            {navigation.map((item) => {
              const isActive = pathname === item.href;
              return (
                <Link
                  key={item.name}
                  href={item.href}
                  onClick={() => setIsMobileMenuOpen(false)}
                  className={cn(
                    'flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium transition-all duration-200',
                    isActive
                      ? 'bg-primary/10 text-primary'
                      : 'text-muted-foreground hover:bg-muted hover:text-foreground',
                    isCollapsed && 'justify-center px-2',
                  )}
                  title={isCollapsed ? item.name : undefined}
                >
                  <item.icon
                    size={20}
                    className={cn(isActive && 'text-primary')}
                  />
                  {!isCollapsed && <span>{item.name}</span>}
                </Link>
              );
            })}
          </nav>

          {/* Collapse Button - Desktop Only */}
          <div className="hidden lg:flex px-3 py-3 border-t border-border/50">
            <Button
              variant="ghost"
              size="sm"
              className="w-full justify-center text-muted-foreground hover:text-foreground"
              onClick={() => setIsCollapsed(!isCollapsed)}
            >
              {isCollapsed ? (
                <ChevronRight size={20} />
              ) : (
                <>
                  <ChevronLeft size={20} className="mr-2" />
                  <span>Thu gọn</span>
                </>
              )}
            </Button>
          </div>
        </div>
      </aside>
    </>
  );
}
