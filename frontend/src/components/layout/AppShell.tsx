'use client'

import Link from 'next/link'
import { usePathname, useRouter } from 'next/navigation'
import { useEffect, useRef, useState, useCallback } from 'react'
import axios from 'axios'
import type { Notification } from '@/types/notification'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { authApi } from '@/api/auth'
import { notificationsApi } from '@/api/notifications'
import { usersApi } from '@/api/users'
import { useAuthStore } from '@/store/authStore'
import { setTokens } from '@/api/client'
import { useNotifications } from '@/hooks/useNotifications'
import { type Role } from '@/types/enums'
import { API_BASE_URL, TOKEN_KEYS } from '@/utils/constants'
import type { ApiResponse } from '@/types/api'
import type { TokenResponse } from '@/types/auth'

function parseJwtExp(token: string): number | null {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]))
    return typeof payload.exp === 'number' ? payload.exp : null
  } catch {
    return null
  }
}

export function Unb() {
  const { isAuthenticated } = useAuthStore()
  return (
    <div style={{ background: 'var(--fg)', height: 'var(--unb-height)', display: 'flex', alignItems: 'center', fontSize: '13px', position: 'sticky', top: 0, zIndex: 101 }}>
      <div style={{ maxWidth: 'var(--content-width)', margin: '0 auto', padding: '0 24px', display: 'flex', alignItems: 'center', justifyContent: 'space-between', width: '100%' }}>
        <nav style={{ display: 'flex', gap: '24px' }}>
          <Link href="/" style={{ color: 'rgba(255,255,255,0.7)', fontSize: '13px', transition: 'color 0.2s' }}
            onMouseEnter={e => (e.currentTarget.style.color = '#fff')}
            onMouseLeave={e => (e.currentTarget.style.color = 'rgba(255,255,255,0.7)')}>
            홈
          </Link>
          <Link href="/ideas" style={{ color: 'rgba(255,255,255,0.7)', fontSize: '13px', transition: 'color 0.2s' }}
            onMouseEnter={e => (e.currentTarget.style.color = '#fff')}
            onMouseLeave={e => (e.currentTarget.style.color = 'rgba(255,255,255,0.7)')}>
            아이디어
          </Link>
        </nav>
        <nav style={{ display: 'flex', gap: '16px' }}>
          <Link href="#" style={{ color: 'rgba(255,255,255,0.7)', fontSize: '13px', transition: 'color 0.2s' }}>고객센터</Link>
          {isAuthenticated && (
            <Link href="/mypage" style={{ color: 'rgba(255,255,255,0.7)', fontSize: '13px', transition: 'color 0.2s' }}>마이페이지</Link>
          )}
        </nav>
      </div>
    </div>
  )
}

export function Gnb() {
  const pathname = usePathname()
  const { isAuthenticated, user, setUser, logout, initAuth, hydrated } = useAuthStore()
  const { data: notifData, unreadCount, latestNotification } = useNotifications(isAuthenticated)
  const queryClient = useQueryClient()
  const [bellOpen, setBellOpen] = useState(false)
  const bellRef = useRef<HTMLDivElement>(null)
  const [toast, setToast] = useState<Notification | null>(null)
  const [toastVisible, setToastVisible] = useState(false)
  const toastTimerRef = useRef<ReturnType<typeof setTimeout> | null>(null)

  const dismissToast = useCallback(() => {
    setToastVisible(false)
    setTimeout(() => setToast(null), 300)
  }, [])

  useEffect(() => {
    if (!latestNotification) return
    setToast(latestNotification)
    setToastVisible(true)
    if (toastTimerRef.current) clearTimeout(toastTimerRef.current)
    toastTimerRef.current = setTimeout(dismissToast, 4000)
  }, [latestNotification, dismissToast])

  useEffect(() => {
    if (!bellOpen) return
    const handler = (e: MouseEvent) => {
      if (bellRef.current && !bellRef.current.contains(e.target as Node)) {
        setBellOpen(false)
      }
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [bellOpen])

  const markRead = useMutation({
    mutationFn: notificationsApi.markAsRead,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notifications'] }),
  })
  const markAll = useMutation({
    mutationFn: notificationsApi.markAllAsRead,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['notifications'] }),
  })

  useEffect(() => {
    initAuth()
  }, [initAuth])

  const { data: me } = useQuery({
    queryKey: ['users', 'me'],
    queryFn: usersApi.getMe,
    enabled: isAuthenticated && !user,
  })

  useEffect(() => {
    if (me) setUser(me)
  }, [me, setUser])

  // 토큰 만료 자동 감지: 이미 만료됐으면 즉시 리프레시, 아니면 타이머 예약
  useEffect(() => {
    const token = typeof window !== 'undefined' ? localStorage.getItem(TOKEN_KEYS.ACCESS) : null
    if (!token) return

    const exp = parseJwtExp(token)
    if (!exp) return

    const doRefresh = () =>
      axios
        .post<ApiResponse<TokenResponse>>(`${API_BASE_URL}/auth/token-refresh`, null, { withCredentials: true })
        .then(({ data }) => setTokens(data.data))
        .catch(() => {
          useAuthStore.getState().logout()
          window.location.href = '/login'
        })

    const msUntilExpiry = exp * 1000 - Date.now()

    if (msUntilExpiry <= 0) {
      // 이미 만료 → 즉시 리프레시 시도
      doRefresh()
    } else {
      // 만료 1분 전에 자동 리프레시
      const timer = setTimeout(doRefresh, Math.max(0, msUntilExpiry - 60_000))
      return () => clearTimeout(timer)
    }
  // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  const handleLogout = async () => {
    try { await authApi.logout() } catch { /* ignore */ }
    logout()
    window.location.href = '/'
  }

  const navItems = (() => {
    const explore = { href: '/ideas', label: '아이디어 탐색' }
    switch (user?.role) {
      case 'USER':
        return [explore, { href: '/mypage/ideas', label: '내 아이디어' }, { href: '/fundings', label: '펀딩' }, { href: '/mypage/payments', label: '후원 내역' }]
      case 'EXPERT':
        return [explore, { href: '/expert/matches', label: '매칭 관리' }]
      case 'ADMIN':
        return [explore]
      default:
        return [explore, { href: '/fundings', label: '펀딩' }]
    }
  })()

  return (
    <>
    <header style={{
      background: 'var(--brand)',
      height: 'var(--nav-height)',
      position: 'sticky',
      top: 0,
      zIndex: 100,
      boxShadow: '0 2px 8px rgba(0,176,225,0.25)',
    }}>
      <div style={{
        maxWidth: 'var(--content-width)',
        margin: '0 auto',
        padding: '0 24px',
        height: '100%',
        display: 'flex',
        alignItems: 'center',
        gap: '32px',
      }}>
        <Link href="/" style={{ display: 'flex', alignItems: 'center', gap: '8px', color: '#fff', fontSize: '22px', fontWeight: 700, letterSpacing: '-0.02em', whiteSpace: 'nowrap' }}>
          <span style={{ width: '32px', height: '32px', background: 'rgba(255,255,255,0.2)', borderRadius: '50%', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '16px' }}>🌱</span>
          SeedLink
        </Link>

        <nav style={{ display: 'flex', gap: '4px', flex: 1 }}>
          {navItems.map(item => {
            const isActive = pathname.startsWith(item.href)
            return (
              <Link key={item.href} href={item.href} style={{
                color: isActive ? '#fff' : 'rgba(255,255,255,0.85)',
                fontSize: '16px',
                fontWeight: 500,
                padding: '8px 14px',
                borderRadius: 'var(--radius-md)',
                background: isActive ? 'rgba(255,255,255,0.15)' : 'transparent',
                transition: 'background 0.2s, color 0.2s',
              }}>
                {item.label}
              </Link>
            )
          })}
        </nav>

        <div style={{ display: 'flex', alignItems: 'center', gap: '10px', opacity: hydrated ? 1 : 0, transition: 'opacity 0.15s' }}>
          {isAuthenticated ? (
            <>
              {/* 알림 벨 + 팝업 */}
              <div ref={bellRef} style={{ position: 'relative' }}>
                <button
                  onClick={() => setBellOpen((v) => !v)}
                  style={{
                    position: 'relative', display: 'flex',
                    width: '36px', height: '36px',
                    background: bellOpen ? 'rgba(255,255,255,0.25)' : 'rgba(255,255,255,0.15)',
                    borderRadius: '50%', border: 'none', cursor: 'pointer',
                    alignItems: 'center', justifyContent: 'center',
                    fontSize: '20px', transition: 'background 0.2s',
                  }}
                >
                  🔔
                  {unreadCount > 0 && (
                    <span style={{
                      position: 'absolute', top: '1px', right: '1px',
                      minWidth: '17px', height: '17px',
                      background: '#f65c5c',
                      border: '2px solid var(--brand)',
                      borderRadius: '99px',
                      fontSize: '10px', fontWeight: 700, color: '#fff',
                      display: 'flex', alignItems: 'center', justifyContent: 'center',
                      padding: '0 3px', lineHeight: 1,
                    }}>
                      {unreadCount > 9 ? '9+' : unreadCount}
                    </span>
                  )}
                </button>

                {bellOpen && (
                  <div style={{
                    position: 'absolute', top: 'calc(100% + 12px)', right: '-8px',
                    width: '360px',
                    background: '#fff',
                    border: '1px solid var(--border)',
                    borderRadius: '16px',
                    boxShadow: '0 8px 32px rgba(0,0,0,0.12)',
                    zIndex: 200,
                    overflow: 'hidden',
                  }}>
                    {/* 말풍선 꼬리 */}
                    <div style={{
                      position: 'absolute', top: '-7px', right: '18px',
                      width: '14px', height: '14px',
                      background: '#fff',
                      border: '1px solid var(--border)',
                      borderBottom: 'none', borderRight: 'none',
                      transform: 'rotate(45deg)',
                    }} />

                    {/* 헤더 */}
                    <div style={{
                      display: 'flex', alignItems: 'center', justifyContent: 'space-between',
                      padding: '16px 18px 12px',
                      borderBottom: '1px solid var(--border)',
                    }}>
                      <span style={{ fontSize: '15px', fontWeight: 700, color: 'var(--fg)' }}>
                        알림
                        {unreadCount > 0 && (
                          <span style={{ marginLeft: '6px', fontSize: '13px', fontWeight: 700, color: '#fff', background: 'var(--brand)', padding: '1px 7px', borderRadius: '99px' }}>
                            {unreadCount}
                          </span>
                        )}
                      </span>
                      <button
                        onClick={() => markAll.mutate()}
                        disabled={markAll.isPending || unreadCount === 0}
                        style={{
                          fontSize: '12px', color: 'var(--brand-dark)', fontWeight: 600,
                          background: 'none', border: 'none', cursor: unreadCount === 0 ? 'default' : 'pointer',
                          opacity: unreadCount === 0 ? 0.4 : 1,
                        }}
                      >
                        전체 읽음
                      </button>
                    </div>

                    {/* 알림 목록 */}
                    <div style={{ maxHeight: '360px', overflowY: 'auto' }}>
                      {!notifData?.content?.length ? (
                        <div style={{ padding: '40px 0', textAlign: 'center', fontSize: '14px', color: 'var(--fg-muted)' }}>
                          알림이 없습니다.
                        </div>
                      ) : (
                        notifData.content.slice(0, 10).map((n) => (
                          <div
                            key={n.id}
                            onClick={() => { if (!n.isRead) markRead.mutate(n.id) }}
                            style={{
                              display: 'flex', gap: '12px', alignItems: 'flex-start',
                              padding: '13px 18px',
                              borderBottom: '1px solid var(--border)',
                              background: n.isRead ? '#fff' : 'var(--brand-tint)',
                              borderLeft: n.isRead ? 'none' : '3px solid var(--brand)',
                              cursor: n.isRead ? 'default' : 'pointer',
                              transition: 'background 0.15s',
                            }}
                          >
                            <div style={{
                              width: '32px', height: '32px', borderRadius: '50%',
                              background: n.isRead ? 'var(--bg-alt)' : 'rgba(0,176,225,0.15)',
                              display: 'flex', alignItems: 'center', justifyContent: 'center',
                              fontSize: '15px', flexShrink: 0,
                            }}>
                              🔔
                            </div>
                            <div style={{ flex: 1, minWidth: 0 }}>
                              <p style={{ fontSize: '13px', fontWeight: n.isRead ? 500 : 700, color: 'var(--fg)', marginBottom: '3px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                                {n.title}
                              </p>
                              <p style={{ fontSize: '12px', color: 'var(--fg-muted)', lineHeight: 1.4, display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                                {n.message}
                              </p>
                            </div>
                            {!n.isRead && (
                              <div style={{ width: '7px', height: '7px', borderRadius: '50%', background: 'var(--brand)', flexShrink: 0, marginTop: '4px' }} />
                            )}
                          </div>
                        ))
                      )}
                    </div>

                    {/* 하단 전체 보기 */}
                    <Link
                      href="/mypage/notifications"
                      onClick={() => setBellOpen(false)}
                      style={{
                        display: 'block', textAlign: 'center',
                        padding: '12px',
                        fontSize: '13px', fontWeight: 600, color: 'var(--brand-dark)',
                        borderTop: '1px solid var(--border)',
                        textDecoration: 'none',
                        background: '#fafafa',
                      }}
                    >
                      전체 알림 보기 →
                    </Link>
                  </div>
                )}
              </div>

              {/* 전문가 신청 버튼 (USER 전용) */}
              {user?.role === 'USER' && (
                <Link
                  href="/expert-apply"
                  style={{
                    display: 'inline-flex', alignItems: 'center', gap: '5px',
                    fontSize: '13px', fontWeight: 700,
                    color: 'var(--brand-dark)',
                    background: '#fff',
                    padding: '6px 14px',
                    borderRadius: '99px',
                    textDecoration: 'none',
                    whiteSpace: 'nowrap',
                    transition: 'background 0.2s',
                  }}
                >
                  🎓 전문가 신청
                </Link>
              )}

              {/* 관리자 버튼 */}
              {user?.role === 'ADMIN' && (
                <Link
                  href="/admin"
                  title="관리자 페이지"
                  style={{
                    display: 'flex',
                    width: '36px', height: '36px',
                    background: pathname.startsWith('/admin') ? 'rgba(255,255,255,0.25)' : 'rgba(255,255,255,0.15)',
                    borderRadius: '50%', border: 'none',
                    alignItems: 'center', justifyContent: 'center',
                    fontSize: '20px', transition: 'background 0.2s',
                    textDecoration: 'none',
                  }}
                >
                  ⚙️
                </Link>
              )}

              {/* 유저 pill */}
              <Link href="/mypage" style={{
                display: 'flex', alignItems: 'center', gap: '8px',
                background: 'rgba(255,255,255,0.15)',
                borderRadius: '99px',
                padding: '5px 14px 5px 5px',
                transition: 'background 0.2s',
              }}>
                <span style={{
                  width: '26px', height: '26px', borderRadius: '50%',
                  background: '#fff',
                  color: 'var(--brand-dark)',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  fontSize: '12px', fontWeight: 700, flexShrink: 0,
                }}>
                  {(user?.nickname ?? user?.name ?? '?')[0].toUpperCase()}
                </span>
                <span style={{ color: '#fff', fontSize: '14px', fontWeight: 600, whiteSpace: 'nowrap' }}>
                  {user?.nickname ?? user?.name ?? '마이페이지'} 님
                </span>
              </Link>

              {/* 로그아웃 */}
              <button onClick={handleLogout} style={{
                color: 'rgba(255,255,255,0.7)',
                fontSize: '13px',
                fontWeight: 500,
                padding: '5px 12px',
                border: '1px solid rgba(255,255,255,0.25)',
                borderRadius: '99px',
                background: 'transparent',
                cursor: 'pointer',
                transition: 'all 0.2s',
                whiteSpace: 'nowrap',
              }}>
                로그아웃
              </button>
            </>
          ) : (
            <>
              <Link href="/login" style={{
                color: 'rgba(255,255,255,0.85)',
                fontSize: '14px', fontWeight: 500,
                padding: '6px 16px',
                border: '1px solid rgba(255,255,255,0.4)',
                borderRadius: '99px',
                transition: 'background 0.2s',
              }}>
                로그인
              </Link>
              <Link href="/signup" style={{
                color: 'var(--brand-dark)',
                fontSize: '14px', fontWeight: 700,
                padding: '7px 18px',
                background: '#ffffff',
                borderRadius: '99px',
                transition: 'background 0.2s',
                whiteSpace: 'nowrap',
              }}>
                회원가입
              </Link>
            </>
          )}
        </div>
      </div>
    </header>

      {/* 알림 토스트 */}
      {toast && (
        <div
          onClick={dismissToast}
          style={{
            position: 'fixed',
            top: '80px',
            right: '24px',
            zIndex: 9999,
            width: '320px',
            background: '#fff',
            border: '1px solid var(--border)',
            borderLeft: '4px solid var(--brand)',
            borderRadius: '12px',
            boxShadow: '0 8px 24px rgba(0,0,0,0.12)',
            padding: '14px 16px',
            display: 'flex',
            gap: '12px',
            alignItems: 'flex-start',
            cursor: 'pointer',
            opacity: toastVisible ? 1 : 0,
            transform: toastVisible ? 'translateX(0)' : 'translateX(24px)',
            transition: 'opacity 0.3s, transform 0.3s',
          }}
        >
          <span style={{ fontSize: '20px', flexShrink: 0, marginTop: '1px' }}>🔔</span>
          <div style={{ flex: 1, minWidth: 0 }}>
            <p style={{ fontSize: '13px', fontWeight: 700, color: 'var(--fg)', marginBottom: '3px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
              {toast.title}
            </p>
            <p style={{ fontSize: '12px', color: 'var(--fg-muted)', lineHeight: 1.4, display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
              {toast.message}
            </p>
          </div>
          <button
            onClick={(e) => { e.stopPropagation(); dismissToast() }}
            style={{ background: 'none', border: 'none', color: 'var(--fg-muted)', fontSize: '16px', cursor: 'pointer', padding: '0', lineHeight: 1, flexShrink: 0 }}
          >
            ×
          </button>
        </div>
      )}
    </>
  )
}

export function Footer() {
  return (
    <footer style={{ background: '#1a1f20', padding: '48px 0 32px' }}>
      <div style={{ maxWidth: 'var(--content-width)', margin: '0 auto', padding: '0 24px' }}>
        <div style={{ display: 'grid', gridTemplateColumns: '2fr 1fr 1fr 1fr', gap: '40px' }}>
          <div>
            <div style={{ fontSize: '20px', fontWeight: 700, color: '#fff', marginBottom: '12px' }}>SeedLink</div>
            <div style={{ fontSize: '14px', color: 'rgba(255,255,255,0.4)', lineHeight: 1.6, maxWidth: '280px' }}>
              검증과 자금 투명성 위에서 아이디어를 실현하는 신뢰 기반 크라우드펀딩 플랫폼
            </div>
          </div>
          <div>
            <div style={{ fontSize: '14px', fontWeight: 700, color: 'rgba(255,255,255,0.7)', marginBottom: '16px' }}>서비스</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              {[['아이디어 탐색', '/ideas'], ['펀딩 목록', '/fundings'], ['전문가 소개', '#']].map(([label, href]) => (
                <Link key={href} href={href} style={{ fontSize: '14px', color: 'rgba(255,255,255,0.4)', transition: 'color 0.2s' }}>{label}</Link>
              ))}
            </div>
          </div>
          <div>
            <div style={{ fontSize: '14px', fontWeight: 700, color: 'rgba(255,255,255,0.7)', marginBottom: '16px' }}>지원</div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              {[['고객센터', '#'], ['이용약관', '#'], ['개인정보처리방침', '#']].map(([label, href]) => (
                <Link key={label} href={href} style={{ fontSize: '14px', color: 'rgba(255,255,255,0.4)', transition: 'color 0.2s' }}>{label}</Link>
              ))}
            </div>
          </div>
          <div>
            <div style={{ fontSize: '14px', fontWeight: 700, color: 'rgba(255,255,255,0.7)', marginBottom: '16px' }}>팀 소개</div>
            <div style={{ fontSize: '14px', color: 'rgba(255,255,255,0.4)', lineHeight: 1.6 }}>
              팀명: 4달라<br />
              기간: 2026.06 ~ 07
            </div>
          </div>
        </div>
        <div style={{ marginTop: '48px', paddingTop: '24px', borderTop: '1px solid rgba(255,255,255,0.08)', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <span style={{ fontSize: '13px', color: 'rgba(255,255,255,0.3)' }}>© 2026 4달라. All rights reserved.</span>
          <span style={{ fontSize: '13px', color: 'rgba(255,255,255,0.3)' }}>Team 4달라</span>
        </div>
      </div>
    </footer>
  )
}

const AUTH_PATHS = ['/login', '/signup']

export function AppShell({ children }: { children: React.ReactNode }) {
  const pathname = usePathname()
  const isAuth = AUTH_PATHS.includes(pathname)

  if (isAuth) {
    return <>{children}</>
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', minHeight: '100vh' }}>
      <Gnb />
      <main style={{ flex: 1 }}>{children}</main>
      <Footer />
    </div>
  )
}

interface ProtectedRouteProps {
  children: React.ReactNode
  roles?: Role[]
}

export function ProtectedRoute({ children, roles }: ProtectedRouteProps) {
  const router = useRouter()
  const pathname = usePathname()
  const { isAuthenticated, user, setUser, initAuth, hydrated } = useAuthStore()

  useEffect(() => {
    initAuth()
  }, [initAuth])

  const { data: me, isLoading } = useQuery({
    queryKey: ['users', 'me'],
    queryFn: usersApi.getMe,
    enabled: isAuthenticated && !user,
  })

  useEffect(() => {
    if (me) setUser(me)
  }, [me, setUser])

  useEffect(() => {
    if (hydrated && !isAuthenticated) {
      router.replace(`/login?from=${encodeURIComponent(pathname)}`)
    }
  }, [hydrated, isAuthenticated, router, pathname])

  useEffect(() => {
    if (hydrated && roles && user && !roles.includes(user.role)) {
      router.replace('/')
    }
  }, [hydrated, roles, user, router])

  if (!hydrated || isLoading) {
    return (
      <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '96px 0' }}>
        <div style={{ width: '32px', height: '32px', borderRadius: '50%', border: '3px solid var(--brand-tint)', borderTopColor: 'var(--brand)', animation: 'spin 0.8s linear infinite' }} />
      </div>
    )
  }

  if (!isAuthenticated) return null

  if (roles && user && !roles.includes(user.role)) return null

  return <>{children}</>
}
