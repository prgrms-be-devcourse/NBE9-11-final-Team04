'use client'

import Link from 'next/link'
import { useEffect } from 'react'
import { usePathname, useRouter } from 'next/navigation'
import { useQueryClient } from '@tanstack/react-query'
import { ProtectedRoute } from '@/components/layout/AppShell'
import { useAuthStore } from '@/store/authStore'
import { ideasApi } from '@/api/ideas'
import { ROLE_LABELS } from '@/types/enums'
import type { Role } from '@/types/enums'

interface NavItem {
  path: string
  label: string
  icon: string
  exact?: boolean
  roles?: Role[]
}

const navItems: NavItem[] = [
  { path: '/mypage',               label: '내 정보',    icon: '📊', exact: true },
  { path: '/mypage/ideas',         label: '내 아이디어', icon: '💡', roles: ['USER'] },
  { path: '/mypage/matches',       label: '매칭 목록',  icon: '🤝', roles: ['EXPERT'] },
  { path: '/mypage/payments',      label: '결제 내역',  icon: '💰', roles: ['USER'] },
  { path: '/mypage/notifications', label: '알림',       icon: '🔔' },
]

const settingItems: NavItem[] = [
  { path: '/mypage/account',  label: '계정 설정',  icon: '⚙️' },
  { path: '/mypage/business', label: '사업자 정보', icon: '🏢' },
]

function Sidebar() {
  const pathname = usePathname()
  const router = useRouter()
  const { user, logout } = useAuthStore()

  const visibleItems = navItems.filter(
    (item) => !item.roles || (user?.role && item.roles.includes(user.role as Role))
  )

  const handleLogout = () => {
    logout()
    router.replace('/login')
  }

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
        {/* 프로필 헤더 */}
        <div style={{ background: 'var(--brand)', padding: '32px 24px', textAlign: 'center' }}>
          <div style={{
            width: '80px',
            height: '80px',
            borderRadius: '50%',
            background: 'rgba(255,255,255,0.2)',
            border: '3px solid rgba(255,255,255,0.4)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontSize: '32px',
            margin: '0 auto 16px',
            overflow: 'hidden',
          }}>
            {user?.profileImage
              ? <img src={user.profileImage} alt="profile" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
              : '👤'}
          </div>
          <div style={{ fontSize: '20px', fontWeight: 700, color: '#fff', marginBottom: '6px' }}>
            {user?.nickname ?? user?.name ?? '사용자'}
          </div>
          <span style={{
            display: 'inline-block',
            fontSize: '12px',
            fontWeight: 700,
            color: 'var(--brand-dark)',
            background: 'rgba(255,255,255,0.9)',
            padding: '3px 10px',
            borderRadius: '99px',
          }}>
            {user?.role ? ROLE_LABELS[user.role as Role] : ''}
          </span>
          <div style={{ fontSize: '13px', color: 'rgba(255,255,255,0.75)', marginTop: '8px' }}>
            {user?.email ?? ''}
          </div>
        </div>

        {/* 사이드 네비게이션 */}
        <nav style={{ padding: '12px 0' }}>
          {visibleItems.map((item) => {
            const active = item.exact
              ? pathname === item.path
              : pathname.startsWith(item.path)
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

          <div style={{ height: '1px', background: 'var(--border)', margin: '8px 24px' }} />

          {settingItems.map((item) => {
            const active = pathname.startsWith(item.path)
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

          <div style={{ height: '1px', background: 'var(--border)', margin: '8px 24px' }} />

          <button
            onClick={handleLogout}
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '12px',
              padding: '12px 24px',
              fontSize: '15px',
              fontWeight: 500,
              color: 'var(--error)',
              background: 'transparent',
              border: 'none',
              borderLeft: '3px solid transparent',
              width: '100%',
              cursor: 'pointer',
              textAlign: 'left',
            }}
          >
            <span style={{ fontSize: '18px' }}>🚪</span>
            로그아웃
          </button>
        </nav>
      </div>
    </aside>
  )
}

function MyPageContent({ children }: { children: React.ReactNode }) {
  const queryClient = useQueryClient()
  const { user } = useAuthStore()

  useEffect(() => {
    if (user?.role === 'USER') {
      queryClient.prefetchQuery({ queryKey: ['ideas', 'me'],     queryFn: ideasApi.getMyIdeas, staleTime: 30_000 })
      queryClient.prefetchQuery({ queryKey: ['ideas', 'drafts'], queryFn: ideasApi.getDrafts,  staleTime: 30_000 })
    }
  }, [queryClient, user?.role])

  return (
    <div style={{ maxWidth: '1100px', margin: '0 auto', padding: '32px 24px' }}>
      <div style={{
        display: 'grid',
        gridTemplateColumns: '260px 1fr',
        gap: '36px',
        alignItems: 'start',
      }}>
        <Sidebar />
        <main>{children}</main>
      </div>
    </div>
  )
}

export default function MyPageLayout({ children }: { children: React.ReactNode }) {
  return (
    <ProtectedRoute>
      <MyPageContent>{children}</MyPageContent>
    </ProtectedRoute>
  )
}
