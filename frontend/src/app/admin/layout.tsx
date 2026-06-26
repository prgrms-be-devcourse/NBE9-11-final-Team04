'use client'

import Link from 'next/link'
import { usePathname } from 'next/navigation'
import { ProtectedRoute } from '@/components/layout/AppShell'

const NAV_ITEMS = [
  { path: '/admin',                    label: '아이디어 관리',  icon: '🏛️', exact: true },
  { path: '/admin/disputes',           label: '분쟁 관리',      icon: '🚨' },
  { path: '/admin/users',              label: '사용자 관리',    icon: '👥' },
  { path: '/admin/experts',            label: '전문가 관리',    icon: '🎓' },
  { path: '/admin/milestones',         label: '마일스톤 관리',  icon: '📋' },
  { path: '/admin/settlements',        label: '정산/보증금',    icon: '💼' },
  { path: '/admin/notifications',      label: '공지 발송',      icon: '📢' },
  { path: '/admin/invite',             label: '관리자 초대',    icon: '✉️' },
]

function AdminSidebar() {
  const pathname = usePathname()

  return (
    <aside>
      <div style={{
        background: '#fff',
        border: '1px solid var(--border)',
        borderRadius: '16px',
        overflow: 'hidden',
        position: 'sticky',
        top: 'calc(var(--unb-height) + var(--nav-height) + 20px)',
      }}>
        <div style={{ background: 'var(--brand)', padding: '24px', textAlign: 'center' }}>
          <div style={{ fontSize: '32px', marginBottom: '8px' }}>🛡️</div>
          <div style={{ fontSize: '16px', fontWeight: 700, color: '#fff' }}>관리자 패널</div>
        </div>

        <nav style={{ padding: '12px 0' }}>
          {NAV_ITEMS.map((item) => {
            const active = item.exact ? pathname === item.path : pathname.startsWith(item.path)
            return (
              <Link
                key={item.path}
                href={item.path}
                style={{
                  display: 'flex',
                  alignItems: 'center',
                  gap: '12px',
                  padding: '12px 24px',
                  fontSize: '15px',
                  fontWeight: active ? 700 : 500,
                  color: active ? 'var(--brand-dark)' : 'var(--fg-muted)',
                  background: active ? 'var(--brand-tint)' : 'transparent',
                  borderLeft: `3px solid ${active ? 'var(--brand)' : 'transparent'}`,
                  transition: 'background 0.15s, color 0.15s',
                  textDecoration: 'none',
                }}
              >
                <span style={{ fontSize: '18px' }}>{item.icon}</span>
                {item.label}
              </Link>
            )
          })}
        </nav>
      </div>
    </aside>
  )
}

export default function AdminLayout({ children }: { children: React.ReactNode }) {
  return (
    <ProtectedRoute roles={['ADMIN']}>
      <div style={{ maxWidth: '1200px', margin: '0 auto', padding: '32px 24px' }}>
        <div style={{
          display: 'grid',
          gridTemplateColumns: '220px 1fr',
          gap: '32px',
          alignItems: 'start',
        }}>
          <AdminSidebar />
          <main>{children}</main>
        </div>
      </div>
    </ProtectedRoute>
  )
}
