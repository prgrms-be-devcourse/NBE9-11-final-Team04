'use client'

import { useState, Suspense } from 'react'
import Link from 'next/link'
import { useRouter, useSearchParams } from 'next/navigation'
import { useMutation } from '@tanstack/react-query'
import { authApi } from '@/api/auth'
import { usersApi } from '@/api/users'
import { useAuthStore } from '@/store/authStore'
import { Input } from '@/components/ui/Input'
import { getErrorMessage } from '@/utils/format'

function LoginForm() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const setUser = useAuthStore((s) => s.setUser)
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')

  const from = searchParams.get('from') ?? '/'

  const loginMutation = useMutation({
    mutationFn: () => authApi.login({ email, password }),
    onSuccess: async () => {
      const user = await usersApi.getMe()
      setUser(user)
      router.replace(from)
    },
    onError: (err) => setError(getErrorMessage(err)),
  })

  return (
    <div style={{
      minHeight: '100vh',
      background: 'var(--bg-alt)',
      display: 'flex',
      alignItems: 'center',
      justifyContent: 'center',
      padding: '40px 20px',
    }}>
      <div style={{
        background: '#fff',
        border: '1px solid var(--border)',
        borderRadius: 'var(--radius-2xl)',
        padding: '48px 44px',
        width: '100%',
        maxWidth: '460px',
        boxShadow: '0 8px 40px rgba(0,0,0,0.06)',
      }}>
        {/* 로고 */}
        <div style={{ textAlign: 'center', marginBottom: '32px' }}>
          <Link href="/" style={{ fontSize: '28px', fontWeight: 700, color: 'var(--brand)', textDecoration: 'none', display: 'inline-block' }}>🌱 SeedLink</Link>
          <div style={{ fontSize: '14px', color: 'var(--fg-muted)', marginTop: '4px' }}>신뢰 기반 크라우드펀딩 플랫폼</div>
        </div>

        <h1 style={{ fontSize: '24px', fontWeight: 700, color: 'var(--fg)', marginBottom: '8px' }}>로그인</h1>
        <p style={{ fontSize: '15px', color: 'var(--fg-muted)', marginBottom: '32px' }}>SeedLink에 오신 것을 환영합니다</p>

        {/* 소셜 로그인 */}
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginBottom: '24px' }}>
          <button
            onClick={() => authApi.oauthRedirect('google')}
            style={{
              display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '10px',
              height: '50px', border: '1.5px solid var(--border)', borderRadius: 'var(--radius-md)',
              fontSize: '15px', fontWeight: 500, color: 'var(--fg)', cursor: 'pointer',
              background: '#fff', transition: 'border-color 0.2s, background 0.2s',
            }}
          >
            <span style={{ fontSize: '20px' }}>🔵</span> Google로 계속하기
          </button>
          <button
            onClick={() => authApi.oauthRedirect('kakao')}
            style={{
              display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '10px',
              height: '50px', border: '1.5px solid #f0d000', borderRadius: 'var(--radius-md)',
              fontSize: '15px', fontWeight: 500, color: '#3c1e1e', cursor: 'pointer',
              background: '#FEE500', transition: 'opacity 0.2s',
            }}
          >
            <span style={{ fontSize: '20px' }}>🟡</span> 카카오로 계속하기
          </button>
        </div>

        {/* 구분선 */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', marginBottom: '24px' }}>
          <div style={{ flex: 1, height: '1px', background: 'var(--border)' }} />
          <span style={{ fontSize: '13px', color: 'var(--fg-muted)', whiteSpace: 'nowrap' }}>또는 이메일로 로그인</span>
          <div style={{ flex: 1, height: '1px', background: 'var(--border)' }} />
        </div>

        {/* 이메일/비밀번호 폼 */}
        <form
          onSubmit={(e) => { e.preventDefault(); setError(''); loginMutation.mutate() }}
          style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}
        >
          <Input label="이메일" type="email" value={email} onChange={(e) => setEmail(e.target.value)} placeholder="example@seedlink.com" required />
          <Input label="비밀번호" type="password" value={password} onChange={(e) => setPassword(e.target.value)} placeholder="비밀번호 입력" required />

          {error && (
            <p style={{ fontSize: '14px', color: 'var(--error)', background: '#fff5f5', padding: '10px 14px', borderRadius: 'var(--radius-md)' }}>
              {error}
            </p>
          )}

          <button
            type="submit"
            disabled={loginMutation.isPending}
            style={{
              width: '100%', height: '52px', fontSize: '17px', fontWeight: 700,
              background: loginMutation.isPending ? 'var(--brand-tint)' : 'var(--brand)',
              color: loginMutation.isPending ? 'var(--brand)' : '#fff',
              border: 'none', borderRadius: 'var(--radius-md)', cursor: loginMutation.isPending ? 'not-allowed' : 'pointer',
              transition: 'background 0.2s', fontFamily: 'inherit',
            }}
          >
            {loginMutation.isPending ? '로그인 중...' : '로그인'}
          </button>
        </form>

        <p style={{ textAlign: 'center', marginTop: '24px', fontSize: '14px', color: 'var(--fg-muted)' }}>
          계정이 없으신가요?{' '}
          <Link href="/signup" style={{ color: 'var(--brand-dark)', fontWeight: 700 }}>회원가입</Link>
        </p>
      </div>
    </div>
  )
}

export default function LoginPage() {
  return (
    <Suspense>
      <LoginForm />
    </Suspense>
  )
}
