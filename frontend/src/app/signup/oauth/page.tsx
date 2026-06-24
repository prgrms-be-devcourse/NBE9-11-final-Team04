'use client'

import { useState, Suspense } from 'react'
import Link from 'next/link'
import { useRouter, useSearchParams } from 'next/navigation'
import { useMutation } from '@tanstack/react-query'
import { authApi } from '@/api/auth'
import { usersApi } from '@/api/users'
import { useAuthStore } from '@/store/authStore'
import { Input } from '@/components/ui/Input'
import { type Role } from '@/types/enums'
import { getErrorMessage } from '@/utils/format'

const PROVIDER_LABELS: Record<string, { label: string; icon: string; color: string; bg: string }> = {
  google: { label: 'Google', icon: '🔵', color: '#444', bg: '#fff' },
  kakao:  { label: '카카오', icon: '🟡', color: '#3c1e1e', bg: '#FEE500' },
}

const ROLES: { value: Role; icon: string; label: string; desc: string }[] = [
  { value: 'PROPOSER', icon: '💡', label: '제안자', desc: '아이디어를 제안하고 크라우드펀딩으로 자금을 모읍니다.' },
  { value: 'EXPERT',   icon: '🎓', label: '전문가', desc: '국가 자격 기반으로 아이디어를 전문적으로 검토합니다.' },
  { value: 'SPONSOR',  icon: '💰', label: '스폰서', desc: '신뢰 검증된 아이디어에 후원하고 성장을 지원합니다.' },
]

const STEPS = ['역할 선택', '정보 입력']

function StepIndicator({ current }: { current: number }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', marginBottom: '32px' }}>
      {STEPS.map((label, i) => (
        <div key={i} style={{ display: 'flex', alignItems: 'center', flex: i < STEPS.length - 1 ? 1 : undefined }}>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
            <div style={{
              width: '32px', height: '32px', borderRadius: '50%',
              border: `2px solid ${i < current ? '#22c55e' : i === current ? 'var(--brand)' : 'var(--border)'}`,
              background: i < current ? '#22c55e' : i === current ? 'var(--brand)' : '#fff',
              color: i <= current ? '#fff' : 'var(--fg-muted)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: '13px', fontWeight: 700,
            }}>
              {i < current ? '✓' : i + 1}
            </div>
            <span style={{
              fontSize: '12px', marginTop: '6px', whiteSpace: 'nowrap',
              color: i === current ? 'var(--brand-dark)' : 'var(--fg-muted)',
              fontWeight: i === current ? 700 : 400,
            }}>
              {label}
            </span>
          </div>
          {i < STEPS.length - 1 && (
            <div style={{ flex: 1, height: '2px', background: i < current ? '#22c55e' : 'var(--border)', margin: '0 4px', marginBottom: '20px' }} />
          )}
        </div>
      ))}
    </div>
  )
}

function OAuthSignupForm() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const setUser = useAuthStore((s) => s.setUser)

  const oauthToken = searchParams.get('oauthToken') ?? ''
  const email      = searchParams.get('email') ?? ''
  const name       = searchParams.get('name') ?? ''
  const provider   = searchParams.get('provider') ?? 'google'

  const providerMeta = PROVIDER_LABELS[provider] ?? PROVIDER_LABELS.google

  const [step, setStep] = useState(0)
  const [role, setRole] = useState<Role | ''>('')
  const [nickname, setNickname] = useState('')
  const [displayName, setDisplayName] = useState(name)
  const [age, setAge] = useState<string>('')
  const [error, setError] = useState('')

  const registerMutation = useMutation({
    mutationFn: () =>
      authApi.oauthRegister({
        oauthToken,
        name: displayName,
        nickname,
        age: Number(age),
        role: role as Role,
      }),
    onSuccess: async () => {
      const user = await usersApi.getMe()
      setUser(user)
      router.replace('/ideas')
    },
    onError: (err) => setError(getErrorMessage(err)),
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    if (!displayName.trim()) { setError('이름을 입력해주세요.'); return }
    if (!nickname.trim())    { setError('닉네임을 입력해주세요.'); return }
    if (!age || Number(age) < 19) { setError('만 19세 이상만 가입할 수 있습니다.'); return }
    registerMutation.mutate()
  }

  if (!oauthToken) {
    return (
      <div style={{ textAlign: 'center', padding: '48px 0' }}>
        <div style={{ fontSize: '48px', marginBottom: '16px' }}>⚠️</div>
        <p style={{ color: 'var(--fg-muted)' }}>잘못된 접근입니다.</p>
        <Link href="/login" style={{ color: 'var(--brand-dark)', fontWeight: 700 }}>로그인으로 이동</Link>
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
        {/* 로고 */}
        <div style={{ textAlign: 'center', marginBottom: '28px' }}>
          <Link href="/" style={{ fontSize: '28px', fontWeight: 700, color: 'var(--brand)', textDecoration: 'none' }}>
            🌱 SeedLink
          </Link>
          <div style={{ fontSize: '14px', color: 'var(--fg-muted)', marginTop: '4px' }}>신뢰 기반 크라우드펀딩 플랫폼</div>
        </div>

        {/* 소셜 배지 */}
        <div style={{
          display: 'inline-flex', alignItems: 'center', gap: '8px',
          padding: '6px 14px', borderRadius: '99px',
          background: providerMeta.bg,
          border: `1px solid ${provider === 'kakao' ? '#f0d000' : 'var(--border)'}`,
          fontSize: '13px', fontWeight: 600, color: providerMeta.color,
          marginBottom: '20px',
        }}>
          {providerMeta.icon} {providerMeta.label} 계정으로 가입
        </div>

        <StepIndicator current={step} />

        {/* STEP 0: 역할 선택 */}
        {step === 0 && (
          <div>
            <h2 style={{ fontSize: '20px', fontWeight: 700, color: 'var(--fg)', marginBottom: '6px' }}>역할을 선택해주세요</h2>
            <p style={{ fontSize: '14px', color: 'var(--fg-muted)', marginBottom: '24px' }}>가입 후에는 역할을 변경할 수 없습니다.</p>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px', marginBottom: '28px' }}>
              {ROLES.map((r) => (
                <button
                  key={r.value}
                  onClick={() => setRole(r.value)}
                  style={{
                    border: `1.5px solid ${role === r.value ? 'var(--brand)' : 'var(--border)'}`,
                    borderRadius: 'var(--radius-lg)', padding: '16px 18px',
                    cursor: 'pointer',
                    background: role === r.value ? 'var(--brand-tint)' : '#fff',
                    textAlign: 'left', transition: 'all 0.2s',
                    display: 'flex', alignItems: 'center', gap: '14px',
                  }}
                >
                  <span style={{ fontSize: '28px' }}>{r.icon}</span>
                  <div>
                    <div style={{ fontSize: '15px', fontWeight: 700, color: 'var(--fg)' }}>{r.label}</div>
                    <div style={{ fontSize: '13px', color: 'var(--fg-muted)', marginTop: '2px' }}>{r.desc}</div>
                  </div>
                </button>
              ))}
            </div>

            <button
              onClick={() => { if (!role) { setError('역할을 선택해주세요.'); return } setError(''); setStep(1) }}
              disabled={!role}
              style={{
                width: '100%', height: '52px', fontSize: '16px', fontWeight: 700,
                background: role ? 'var(--brand)' : 'var(--border)',
                color: role ? '#fff' : 'var(--fg-muted)',
                border: 'none', borderRadius: 'var(--radius-md)',
                cursor: role ? 'pointer' : 'not-allowed',
                fontFamily: 'inherit', transition: 'background 0.2s',
              }}
            >
              다음 단계 →
            </button>
            {error && <p style={{ fontSize: '14px', color: 'var(--error)', marginTop: '12px', textAlign: 'center' }}>{error}</p>}
          </div>
        )}

        {/* STEP 1: 정보 입력 */}
        {step === 1 && (
          <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <h2 style={{ fontSize: '20px', fontWeight: 700, color: 'var(--fg)', marginBottom: '0' }}>정보 입력</h2>

            {/* 이메일 — 읽기 전용 */}
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
                {email}
              </div>
              <p style={{ fontSize: '12px', color: 'var(--fg-muted)', marginTop: '4px' }}>
                {providerMeta.label} 계정의 이메일로 고정됩니다.
              </p>
            </div>

            <Input
              label="이름"
              value={displayName}
              onChange={(e) => setDisplayName(e.target.value)}
              placeholder="홍길동"
              required
            />
            <Input
              label="닉네임"
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              placeholder="사용할 닉네임"
              required
            />
            <Input
              label="나이"
              type="number"
              min={19}
              value={age}
              onChange={(e) => setAge(e.target.value)}
              placeholder="만 나이"
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

            <div style={{ display: 'flex', gap: '10px' }}>
              <button
                type="button"
                onClick={() => { setError(''); setStep(0) }}
                style={{
                  flex: 1, height: '52px', fontSize: '15px', fontWeight: 600,
                  background: '#fff', border: '1.5px solid var(--border)',
                  borderRadius: 'var(--radius-md)', cursor: 'pointer',
                  fontFamily: 'inherit', color: 'var(--fg-muted)',
                }}
              >
                ← 이전
              </button>
              <button
                type="submit"
                disabled={registerMutation.isPending}
                style={{
                  flex: 2, height: '52px', fontSize: '16px', fontWeight: 700,
                  background: registerMutation.isPending ? 'var(--brand-tint)' : 'var(--brand)',
                  color: registerMutation.isPending ? 'var(--brand)' : '#fff',
                  border: 'none', borderRadius: 'var(--radius-md)',
                  cursor: registerMutation.isPending ? 'not-allowed' : 'pointer',
                  fontFamily: 'inherit', transition: 'background 0.2s',
                }}
              >
                {registerMutation.isPending ? '가입 중...' : '가입 완료'}
              </button>
            </div>
          </form>
        )}
      </div>
    </div>
  )
}

export default function OAuthSignupPage() {
  return (
    <Suspense>
      <OAuthSignupForm />
    </Suspense>
  )
}
