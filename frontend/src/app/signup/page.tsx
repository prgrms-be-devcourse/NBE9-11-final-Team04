'use client'

import { useRef, useState } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useMutation } from '@tanstack/react-query'
import { authApi } from '@/api/auth'
import { usersApi } from '@/api/users'
import { useAuthStore } from '@/store/authStore'
import { Input } from '@/components/ui/Input'
import { getErrorMessage } from '@/utils/format'

const SHOW_SOCIAL = !!(process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID || process.env.NEXT_PUBLIC_KAKAO_CLIENT_ID)

const STEPS = ['기본 정보', '이메일 인증', '프로필 설정']

function StepIndicator({ current }: { current: number }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', marginBottom: '32px' }}>
      {STEPS.map((label, i) => (
        <div key={i} style={{ display: 'flex', alignItems: 'center', flex: i < STEPS.length - 1 ? 1 : undefined }}>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center' }}>
            <div style={{
              width: '30px', height: '30px', borderRadius: '50%',
              border: `2px solid ${i < current ? '#22c55e' : i === current ? 'var(--brand)' : 'var(--border)'}`,
              background: i < current ? '#22c55e' : i === current ? 'var(--brand)' : '#fff',
              color: i <= current ? '#fff' : 'var(--fg-muted)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: '12px', fontWeight: 700, flexShrink: 0,
            }}>
              {i < current ? '✓' : i + 1}
            </div>
            <span style={{
              fontSize: '11px', marginTop: '5px', whiteSpace: 'nowrap',
              color: i === current ? 'var(--brand-dark)' : 'var(--fg-muted)',
              fontWeight: i === current ? 700 : 400,
            }}>
              {label}
            </span>
          </div>
          {i < STEPS.length - 1 && (
            <div style={{ flex: 1, height: '2px', background: i < current ? '#22c55e' : 'var(--border)', margin: '0 3px', marginBottom: '18px' }} />
          )}
        </div>
      ))}
    </div>
  )
}

export default function SignupPage() {
  const router = useRouter()
  const setUser = useAuthStore((s) => s.setUser)

  const fileInputRef = useRef<HTMLInputElement>(null)
  const [step, setStep] = useState(0)
  const [error, setError] = useState('')
  const [otpCode, setOtpCode] = useState('')
  const [profile, setProfile] = useState({ intro: '', portfolioUrl: '' })
  const [imagePreview, setImagePreview] = useState<string | null>(null)
  const [form, setForm] = useState({
    name: '',
    nickname: '',
    age: '' as unknown as number,
    email: '',
    password: '',
    confirmPassword: '',
  })

  const sendOtpMutation = useMutation({
    mutationFn: () => authApi.sendEmailVerify({ email: form.email }),
    onSuccess: () => { setError(''); setStep(1) },
    onError: (err) => setError(getErrorMessage(err)),
  })

  const signupMutation = useMutation({
    mutationFn: () => authApi.signup({
      email: form.email,
      password: form.password,
      name: form.name,
      nickname: form.nickname,
      age: Number(form.age),
    }),
    onSuccess: async () => {
      const user = await usersApi.getMe()
      setUser(user)
      setError('')
      setStep(2)
    },
    onError: (err) => setError(getErrorMessage(err)),
  })

  const verifyOtpMutation = useMutation({
    mutationFn: () => authApi.confirmEmailVerify({ email: form.email, otp: otpCode }),
    onSuccess: () => signupMutation.mutate(),
    onError: (err) => setError(getErrorMessage(err)),
  })

  const imageUploadMutation = useMutation({
    mutationFn: (file: File) => usersApi.updateProfileImage(file),
    onError: (err) => setError(getErrorMessage(err)),
  })

  const profileMutation = useMutation({
    mutationFn: () => usersApi.updateMe({
      nickname: form.nickname,
      intro: profile.intro || undefined,
      portfolioUrl: profile.portfolioUrl || undefined,
    }),
    onSuccess: () => router.push('/'),
    onError: (err) => setError(getErrorMessage(err)),
  })

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    setImagePreview(URL.createObjectURL(file))
    imageUploadMutation.mutate(file)
  }

  const handleInfoSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    if (form.password !== form.confirmPassword) { setError('비밀번호가 일치하지 않습니다.'); return }
    if (form.password.length < 8) { setError('비밀번호는 8자 이상이어야 합니다.'); return }
    sendOtpMutation.mutate()
  }

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
        maxWidth: '520px',
        boxShadow: '0 8px 40px rgba(0,0,0,0.06)',
      }}>
        {/* 로고 */}
        <div style={{ textAlign: 'center', marginBottom: '28px' }}>
          <Link href="/" style={{ fontSize: '28px', fontWeight: 700, color: 'var(--brand)', textDecoration: 'none', display: 'inline-block' }}>🌱 SeedLink</Link>
          <div style={{ fontSize: '14px', color: 'var(--fg-muted)', marginTop: '4px' }}>신뢰 기반 크라우드펀딩 플랫폼</div>
        </div>

        <StepIndicator current={step} />

        {/* STEP 0: 기본 정보 */}
        {step === 0 && (
          <form onSubmit={handleInfoSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <h2 style={{ fontSize: '20px', fontWeight: 700, color: 'var(--fg)', marginBottom: '0' }}>기본 정보 입력</h2>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
              <Input label="이름" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} placeholder="홍길동" required />
              <Input label="닉네임" value={form.nickname} onChange={(e) => setForm({ ...form, nickname: e.target.value })} placeholder="사용할 닉네임" required />
            </div>

            <Input label="나이" type="number" min={19} value={form.age || ''} onChange={(e) => setForm({ ...form, age: Number(e.target.value) })} placeholder="만 나이" required />
            <Input label="이메일" type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} placeholder="example@seedlink.com" required />
            <Input label="비밀번호" type="password" value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} placeholder="8자 이상" required />
            <Input label="비밀번호 확인" type="password" value={form.confirmPassword} onChange={(e) => setForm({ ...form, confirmPassword: e.target.value })} placeholder="비밀번호 재입력" required />

            {SHOW_SOCIAL && (
              <>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', margin: '4px 0' }}>
                  <div style={{ flex: 1, height: '1px', background: 'var(--border)' }} />
                  <span style={{ fontSize: '12px', color: 'var(--fg-muted)', whiteSpace: 'nowrap' }}>또는 소셜 계정으로 가입</span>
                  <div style={{ flex: 1, height: '1px', background: 'var(--border)' }} />
                </div>
                <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
                  {process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID && (
                    <button type="button" onClick={() => authApi.oauthRedirect('google')}
                      style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '10px', height: '48px', border: '1.5px solid var(--border)', borderRadius: 'var(--radius-md)', fontSize: '14px', fontWeight: 500, color: 'var(--fg)', background: '#fff', cursor: 'pointer', fontFamily: 'inherit' }}>
                      <span style={{ fontSize: '18px' }}>🔵</span> Google로 가입하기
                    </button>
                  )}
                  {process.env.NEXT_PUBLIC_KAKAO_CLIENT_ID && (
                    <button type="button" onClick={() => authApi.oauthRedirect('kakao')}
                      style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '10px', height: '48px', border: '1.5px solid #f0d000', borderRadius: 'var(--radius-md)', fontSize: '14px', fontWeight: 500, color: '#3c1e1e', background: '#FEE500', cursor: 'pointer', fontFamily: 'inherit' }}>
                      <span style={{ fontSize: '18px' }}>🟡</span> 카카오로 가입하기
                    </button>
                  )}
                </div>
              </>
            )}

            {error && (
              <p style={{ fontSize: '14px', color: 'var(--error)', background: '#fff5f5', padding: '10px 14px', borderRadius: 'var(--radius-md)' }}>{error}</p>
            )}

            <button type="submit" disabled={sendOtpMutation.isPending}
              style={{ width: '100%', height: '52px', fontSize: '17px', fontWeight: 700, background: sendOtpMutation.isPending ? 'var(--brand-tint)' : 'var(--brand)', color: sendOtpMutation.isPending ? 'var(--brand)' : '#fff', border: 'none', borderRadius: 'var(--radius-md)', cursor: sendOtpMutation.isPending ? 'not-allowed' : 'pointer', fontFamily: 'inherit' }}>
              {sendOtpMutation.isPending ? '발송 중...' : '인증 코드 받기 →'}
            </button>
          </form>
        )}

        {/* STEP 1: 이메일 인증 */}
        {step === 1 && (
          <form onSubmit={(e) => { e.preventDefault(); setError(''); verifyOtpMutation.mutate() }}
            style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <h2 style={{ fontSize: '20px', fontWeight: 700, color: 'var(--fg)', marginBottom: '0' }}>이메일 인증</h2>

            <div style={{ padding: '14px 16px', background: 'var(--brand-tint)', border: '1px solid var(--brand)', borderRadius: 'var(--radius-md)', fontSize: '14px', color: 'var(--brand-dark)' }}>
              <strong>{form.email}</strong>로 인증 코드를 발송했습니다.<br />
              <span style={{ color: 'var(--fg-muted)', fontSize: '13px' }}>스팸함도 확인해 주세요. 코드는 5분간 유효합니다.</span>
            </div>

            <Input label="인증 코드" value={otpCode} onChange={(e) => setOtpCode(e.target.value)} placeholder="6자리 코드 입력" required />

            {error && (
              <p style={{ fontSize: '14px', color: 'var(--error)', background: '#fff5f5', padding: '10px 14px', borderRadius: 'var(--radius-md)' }}>{error}</p>
            )}

            <div style={{ display: 'flex', gap: '10px' }}>
              <button type="button" onClick={() => { setError(''); setStep(0) }}
                style={{ flex: 1, height: '52px', fontSize: '15px', fontWeight: 600, background: '#fff', border: '1.5px solid var(--border)', borderRadius: 'var(--radius-md)', cursor: 'pointer', fontFamily: 'inherit', color: 'var(--fg-muted)' }}>
                ← 이전
              </button>
              <button type="submit" disabled={verifyOtpMutation.isPending || signupMutation.isPending}
                style={{ flex: 2, height: '52px', fontSize: '17px', fontWeight: 700, background: (verifyOtpMutation.isPending || signupMutation.isPending) ? 'var(--brand-tint)' : 'var(--brand)', color: (verifyOtpMutation.isPending || signupMutation.isPending) ? 'var(--brand)' : '#fff', border: 'none', borderRadius: 'var(--radius-md)', cursor: (verifyOtpMutation.isPending || signupMutation.isPending) ? 'not-allowed' : 'pointer', fontFamily: 'inherit' }}>
                {(verifyOtpMutation.isPending || signupMutation.isPending) ? '처리 중...' : '인증 완료 및 가입'}
              </button>
            </div>

            <button type="button" onClick={() => { setError(''); sendOtpMutation.mutate() }} disabled={sendOtpMutation.isPending}
              style={{ background: 'none', border: 'none', fontSize: '14px', color: 'var(--brand-dark)', cursor: 'pointer', textDecoration: 'underline' }}>
              인증 코드 재발송
            </button>
          </form>
        )}

        {/* STEP 2: 프로필 설정 */}
        {step === 2 && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
            <div style={{ textAlign: 'center', padding: '20px 0 4px' }}>
              <div style={{ fontSize: '40px', marginBottom: '10px' }}>🎉</div>
              <h2 style={{ fontSize: '20px', fontWeight: 800, color: 'var(--fg)', marginBottom: '6px' }}>
                가입을 축하합니다!
              </h2>
              <p style={{ fontSize: '14px', color: 'var(--fg-muted)', marginTop: '10px', lineHeight: 1.6 }}>
                프로필을 설정하면 다른 사용자에게<br />더 잘 알려질 수 있어요.
              </p>
            </div>

            {/* 프로필 이미지 */}
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '10px' }}>
              <div style={{ position: 'relative' }}>
                <div style={{
                  width: '88px', height: '88px', borderRadius: '50%',
                  background: 'var(--brand-tint)', border: '2px solid var(--border)',
                  overflow: 'hidden', display: 'flex', alignItems: 'center', justifyContent: 'center',
                  fontSize: '36px',
                }}>
                  {imagePreview
                    ? <img src={imagePreview} alt="preview" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                    : '👤'}
                </div>
                <button
                  type="button"
                  onClick={() => fileInputRef.current?.click()}
                  disabled={imageUploadMutation.isPending}
                  style={{
                    position: 'absolute', bottom: 0, right: 0,
                    width: '28px', height: '28px', borderRadius: '50%',
                    background: 'var(--brand)', border: '2px solid #fff',
                    cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center',
                    fontSize: '13px',
                  }}
                >
                  {imageUploadMutation.isPending ? '⏳' : '✏️'}
                </button>
                <input ref={fileInputRef} type="file" accept="image/*" style={{ display: 'none' }} onChange={handleImageChange} />
              </div>
              <span style={{ fontSize: '13px', color: 'var(--fg-muted)' }}>
                {imageUploadMutation.isPending ? '업로드 중...' : '프로필 사진 설정 (선택)'}
              </span>
            </div>

            {/* 자기소개 */}
            <div>
              <label style={{ display: 'block', fontSize: '14px', fontWeight: 600, color: 'var(--fg)', marginBottom: '8px' }}>
                자기소개
                <span style={{ fontSize: '12px', fontWeight: 400, color: 'var(--fg-muted)', marginLeft: '6px' }}>(선택)</span>
              </label>
              <textarea
                value={profile.intro}
                onChange={(e) => setProfile({ ...profile, intro: e.target.value })}
                placeholder="자신을 소개해주세요."
                rows={4}
                style={{
                  width: '100%', border: '1.5px solid var(--border)', borderRadius: '10px',
                  padding: '12px 14px', fontSize: '14px', fontFamily: 'inherit',
                  lineHeight: 1.6, resize: 'vertical', outline: 'none',
                  color: 'var(--fg)', boxSizing: 'border-box', transition: 'border-color 0.2s',
                }}
                onFocus={(e) => { e.target.style.borderColor = 'var(--brand)' }}
                onBlur={(e) => { e.target.style.borderColor = 'var(--border)' }}
              />
            </div>

            {/* 포트폴리오 URL */}
            <div>
              <label style={{ display: 'block', fontSize: '14px', fontWeight: 600, color: 'var(--fg)', marginBottom: '8px' }}>
                포트폴리오 URL
                <span style={{ fontSize: '12px', fontWeight: 400, color: 'var(--fg-muted)', marginLeft: '6px' }}>(선택)</span>
              </label>
              <input
                type="url"
                value={profile.portfolioUrl}
                onChange={(e) => setProfile({ ...profile, portfolioUrl: e.target.value })}
                placeholder="https://..."
                style={{
                  width: '100%', height: '44px', border: '1.5px solid var(--border)',
                  borderRadius: '10px', padding: '0 14px', fontSize: '14px',
                  fontFamily: 'inherit', outline: 'none', color: 'var(--fg)',
                  boxSizing: 'border-box', transition: 'border-color 0.2s',
                }}
                onFocus={(e) => { e.target.style.borderColor = 'var(--brand)' }}
                onBlur={(e) => { e.target.style.borderColor = 'var(--border)' }}
              />
            </div>

            {error && (
              <p style={{ fontSize: '14px', color: 'var(--error)', background: '#fff5f5', padding: '10px 14px', borderRadius: 'var(--radius-md)' }}>{error}</p>
            )}

            <div style={{ display: 'flex', gap: '10px' }}>
              <button
                type="button"
                onClick={() => router.push('/')}
                style={{
                  flex: 1, height: '52px', fontSize: '15px', fontWeight: 600,
                  background: '#fff', border: '1.5px solid var(--border)',
                  borderRadius: 'var(--radius-md)', cursor: 'pointer',
                  fontFamily: 'inherit', color: 'var(--fg-muted)',
                }}
              >
                건너뛰기
              </button>
              <button
                type="button"
                onClick={() => { setError(''); profileMutation.mutate() }}
                disabled={profileMutation.isPending}
                style={{
                  flex: 2, height: '52px', fontSize: '16px', fontWeight: 700,
                  background: profileMutation.isPending ? 'var(--brand-tint)' : 'var(--brand)',
                  color: profileMutation.isPending ? 'var(--brand)' : '#fff',
                  border: 'none', borderRadius: 'var(--radius-md)',
                  cursor: profileMutation.isPending ? 'not-allowed' : 'pointer',
                  fontFamily: 'inherit', transition: 'background 0.2s',
                }}
              >
                {profileMutation.isPending ? '저장 중...' : '저장하고 시작하기 →'}
              </button>
            </div>
          </div>
        )}

        {step < 2 && (
          <p style={{ textAlign: 'center', marginTop: '24px', fontSize: '14px', color: 'var(--fg-muted)' }}>
            이미 계정이 있으신가요?{' '}
            <Link href="/login" style={{ color: 'var(--brand-dark)', fontWeight: 700 }}>로그인</Link>
          </p>
        )}
      </div>
    </div>
  )
}
