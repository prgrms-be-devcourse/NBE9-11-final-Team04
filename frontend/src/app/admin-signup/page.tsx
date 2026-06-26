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

const PASSWORD_REGEX = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&])[A-Za-z\d@$!%*#?&]{8,20}$/

function AdminSignupForm() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const setUser = useAuthStore((s) => s.setUser)

  const inviteToken = searchParams.get('token') ?? ''
  const prefillEmail = searchParams.get('email') ?? ''

  const [form, setForm] = useState({
    email: prefillEmail,
    password: '',
    confirmPassword: '',
    name: '',
    nickname: '',
    age: '' as unknown as number,
  })
  const [error, setError] = useState('')

  const signupMutation = useMutation({
    mutationFn: () =>
      authApi.adminSignup({
        inviteToken,
        email: form.email,
        password: form.password,
        name: form.name,
        nickname: form.nickname,
        age: Number(form.age),
      }),
    onSuccess: async () => {
      const user = await usersApi.getMe()
      setUser(user)
      router.replace('/admin')
    },
    onError: (err) => setError(getErrorMessage(err)),
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    if (!PASSWORD_REGEX.test(form.password)) {
      setError('비밀번호는 영문·숫자·특수문자(@$!%*#?&)를 모두 포함한 8~20자여야 합니다.')
      return
    }
    if (form.password !== form.confirmPassword) {
      setError('비밀번호가 일치하지 않습니다.')
      return
    }
    signupMutation.mutate()
  }

  if (!inviteToken) {
    return (
      <div style={{
        minHeight: '100vh', background: 'var(--bg-alt)',
        display: 'flex', alignItems: 'center', justifyContent: 'center',
        padding: '40px 20px',
      }}>
        <div style={{
          background: '#fff', border: '1px solid var(--border)',
          borderRadius: 'var(--radius-2xl)', padding: '48px 44px',
          width: '100%', maxWidth: '440px', textAlign: 'center',
          boxShadow: '0 8px 40px rgba(0,0,0,0.06)',
        }}>
          <div style={{ fontSize: '48px', marginBottom: '16px' }}>⚠️</div>
          <h2 style={{ fontSize: '20px', fontWeight: 700, color: 'var(--fg)', marginBottom: '8px' }}>
            유효하지 않은 초대 링크
          </h2>
          <p style={{ fontSize: '14px', color: 'var(--fg-muted)', marginBottom: '24px' }}>
            초대 토큰이 없거나 만료되었습니다.<br />
            관리자에게 재초대를 요청해 주세요.
          </p>
          <Link href="/login" style={{
            display: 'inline-block', padding: '12px 24px',
            background: 'var(--brand)', color: '#fff',
            borderRadius: '8px', fontWeight: 700, fontSize: '14px',
            textDecoration: 'none',
          }}>
            로그인으로 이동
          </Link>
        </div>
      </div>
    )
  }

  return (
    <div style={{
      minHeight: '100vh', background: 'var(--bg-alt)',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      padding: '40px 20px',
    }}>
      <div style={{
        background: '#fff', border: '1px solid var(--border)',
        borderRadius: 'var(--radius-2xl)', padding: '48px 44px',
        width: '100%', maxWidth: '520px',
        boxShadow: '0 8px 40px rgba(0,0,0,0.06)',
      }}>
        <div style={{ textAlign: 'center', marginBottom: '32px' }}>
          <Link href="/" style={{ fontSize: '28px', fontWeight: 700, color: 'var(--brand)', textDecoration: 'none', display: 'inline-block' }}>
            🌱 SeedLink
          </Link>
          <div style={{ fontSize: '14px', color: 'var(--fg-muted)', marginTop: '4px' }}>신뢰 기반 크라우드펀딩 플랫폼</div>
        </div>

        <div style={{
          display: 'inline-flex', alignItems: 'center', gap: '8px',
          padding: '6px 14px', borderRadius: '99px',
          background: 'var(--brand-tint)', border: '1px solid var(--brand)',
          fontSize: '13px', fontWeight: 600, color: 'var(--brand-dark)',
          marginBottom: '24px',
        }}>
          🛡️ 관리자 계정 생성
        </div>

        <h1 style={{ fontSize: '22px', fontWeight: 800, color: 'var(--fg)', marginBottom: '6px' }}>
          관리자 가입
        </h1>
        <p style={{ fontSize: '14px', color: 'var(--fg-muted)', marginBottom: '28px' }}>
          초대받은 이메일로 관리자 계정을 생성합니다.
        </p>

        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
          {/* 이메일 (읽기 전용) */}
          <div>
            <label style={{ display: 'block', fontSize: '14px', fontWeight: 600, color: 'var(--fg)', marginBottom: '6px' }}>
              이메일
            </label>
            <div style={{
              height: '44px', border: '1.5px solid var(--border)',
              borderRadius: '8px', padding: '0 14px',
              fontSize: '14px', color: 'var(--fg-muted)',
              background: 'var(--bg-alt)',
              display: 'flex', alignItems: 'center', gap: '8px',
            }}>
              <span style={{ fontSize: '12px' }}>🔒</span>
              {form.email || '(초대 이메일)'}
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
            <Input
              label="이름"
              value={form.name}
              onChange={(e) => setForm({ ...form, name: e.target.value })}
              placeholder="홍길동"
              required
            />
            <Input
              label="닉네임"
              value={form.nickname}
              onChange={(e) => setForm({ ...form, nickname: e.target.value })}
              placeholder="닉네임"
              required
            />
          </div>

          <Input
            label="나이"
            type="number"
            min={19}
            value={form.age || ''}
            onChange={(e) => setForm({ ...form, age: Number(e.target.value) })}
            placeholder="만 나이"
            required
          />

          <Input
            label="비밀번호"
            type="password"
            value={form.password}
            onChange={(e) => setForm({ ...form, password: e.target.value })}
            placeholder="영문·숫자·특수문자 포함 8~20자"
            required
          />
          <Input
            label="비밀번호 확인"
            type="password"
            value={form.confirmPassword}
            onChange={(e) => setForm({ ...form, confirmPassword: e.target.value })}
            placeholder="비밀번호 재입력"
            required
          />

          {error && (
            <p style={{
              fontSize: '14px', color: 'var(--error)',
              background: '#fff5f5', padding: '10px 14px',
              borderRadius: 'var(--radius-md)',
            }}>
              {error}
            </p>
          )}

          <button
            type="submit"
            disabled={signupMutation.isPending}
            style={{
              width: '100%', height: '52px', fontSize: '17px', fontWeight: 700,
              background: signupMutation.isPending ? 'var(--brand-tint)' : 'var(--brand)',
              color: signupMutation.isPending ? 'var(--brand)' : '#fff',
              border: 'none', borderRadius: 'var(--radius-md)',
              cursor: signupMutation.isPending ? 'not-allowed' : 'pointer',
              fontFamily: 'inherit', transition: 'background 0.2s',
            }}
          >
            {signupMutation.isPending ? '처리 중...' : '관리자 계정 생성'}
          </button>
        </form>
      </div>
    </div>
  )
}

export default function AdminSignupPage() {
  return (
    <Suspense>
      <AdminSignupForm />
    </Suspense>
  )
}
