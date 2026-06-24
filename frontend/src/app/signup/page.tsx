'use client'

import { useRef, useState } from 'react'
import Link from 'next/link'
import { useRouter } from 'next/navigation'
import { useMutation } from '@tanstack/react-query'
import { authApi } from '@/api/auth'
import { usersApi } from '@/api/users'
import { useAuthStore } from '@/store/authStore'
import { Input } from '@/components/ui/Input'
import { type Role } from '@/types/enums'
import { getErrorMessage } from '@/utils/format'

const SHOW_SOCIAL = !!(process.env.NEXT_PUBLIC_GOOGLE_CLIENT_ID || process.env.NEXT_PUBLIC_KAKAO_CLIENT_ID)

const ROLES: { value: Role; icon: string; label: string; sub: string; desc: string }[] = [
  { value: 'PROPOSER', icon: '💡', label: '제안자',  sub: 'PROPOSER', desc: '아이디어를 제안하고 크라우드펀딩으로 자금을 모읍니다.' },
  { value: 'EXPERT',   icon: '🎓', label: '전문가',  sub: 'EXPERT',   desc: '국가 자격 기반으로 아이디어를 전문적으로 검토합니다.' },
  { value: 'SPONSOR',  icon: '💰', label: '스폰서',  sub: 'SPONSOR',  desc: '신뢰 검증된 아이디어에 후원하고 성장을 지원합니다.' },
]

const ROLE_ICONS: Record<Role, string> = { PROPOSER: '💡', EXPERT: '🎓', SPONSOR: '💰', ADMIN: '🛡️' }
const ROLE_LABELS: Record<Role, string> = { PROPOSER: '제안자', EXPERT: '전문가', SPONSOR: '스폰서', ADMIN: '관리자' }

const STEPS = ['역할 선택', '기본 정보', '이메일 인증', '프로필 설정']

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
    role: '' as Role,
    name: '',
    nickname: '',
    age: '' as unknown as number,
    email: '',
    password: '',
    confirmPassword: '',
  })

  const sendOtpMutation = useMutation({
    mutationFn: () => authApi.sendEmailVerify({ email: form.email, nickname: form.nickname }),
    onSuccess: () => { setError(''); setStep(2) },
    onError: (err) => setError(getErrorMessage(err)),
  })

  const signupMutation = useMutation({
    mutationFn: () => authApi.signup({
      email: form.email,
      password: form.password,
      name: form.name,
      nickname: form.nickname,
      age: Number(form.age),
      role: form.role,
    }),
    onSuccess: async () => {
      const user = await usersApi.getMe()
      setUser(user)
      setError('')
      setStep(3)
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
    mutationFn: () => usersApi.updateProfile({
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

        {/* STEP 0: 역할 선택 */}
        {step === 0 && (
          <div>
            <h2 style={{ fontSize: '20px', fontWeight: 700, color: 'var(--fg)', marginBottom: '6px' }}>역할을 선택해주세요</h2>
            <p style={{ fontSize: '14px', color: 'var(--fg-muted)', marginBottom: '24px' }}>가입 후에는 역할을 변경할 수 없습니다.</p>

            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px', marginBottom: '28px' }}>
              {ROLES.map((r) => (
                <button
                  key={r.value}
                  onClick={() => setForm({ ...form, role: r.value })}
                  style={{
                    border: `1.5px solid ${form.role === r.value ? 'var(--brand)' : 'var(--border)'}`,
                    borderRadius: 'var(--radius-lg)',
                    padding: '20px 16px',
                    cursor: 'pointer',
                    background: form.role === r.value ? 'var(--brand-tint)' : '#fff',
                    textAlign: 'left',
                    transition: 'all 0.2s',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '8px' }}>
                    <span style={{ fontSize: '24px' }}>{r.icon}</span>
                    <div>
                      <div style={{ fontSize: '16px', fontWeight: 700, color: 'var(--fg)' }}>{r.label}</div>
                      <div style={{ fontSize: '12px', fontWeight: 700, color: 'var(--brand-dark)' }}>{r.sub}</div>
                    </div>
                  </div>
                  <div style={{ fontSize: '13px', color: 'var(--fg-muted)', lineHeight: 1.5 }}>{r.desc}</div>
                </button>
              ))}
            </div>

            <button
              onClick={() => { if (!form.role) { setError('역할을 선택해주세요.'); return } setError(''); setStep(1) }}
              disabled={!form.role}
              style={{
                width: '100%', height: '52px', fontSize: '17px', fontWeight: 700,
                background: form.role ? 'var(--brand)' : 'var(--border)',
                color: form.role ? '#fff' : 'var(--fg-muted)',
                border: 'none', borderRadius: 'var(--radius-md)', cursor: form.role ? 'pointer' : 'not-allowed',
                fontFamily: 'inherit', transition: 'background 0.2s',
              }}
            >
              이메일로 가입하기 →
            </button>

            {SHOW_SOCIAL && (
              <>
                <div style={{ display: 'flex', alignItems: 'center', gap: '10px', margin: '16px 0' }}>
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

            {error && <p style={{ fontSize: '14px', color: 'var(--error)', marginTop: '12px', textAlign: 'center' }}>{error}</p>}
          </div>
        )}

        {/* STEP 1: 기본 정보 */}
        {step === 1 && (
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

            {error && (
              <p style={{ fontSize: '14px', color: 'var(--error)', background: '#fff5f5', padding: '10px 14px', borderRadius: 'var(--radius-md)' }}>{error}</p>
            )}

            <div style={{ display: 'flex', gap: '10px' }}>
              <button type="button" onClick={() => { setError(''); setStep(0) }}
                style={{ flex: 1, height: '52px', fontSize: '15px', fontWeight: 600, background: '#fff', border: '1.5px solid var(--border)', borderRadius: 'var(--radius-md)', cursor: 'pointer', fontFamily: 'inherit', color: 'var(--fg-muted)' }}>
                ← 이전
              </button>
              <button type="submit" disabled={sendOtpMutation.isPending}
                style={{ flex: 2, height: '52px', fontSize: '17px', fontWeight: 700, background: sendOtpMutation.isPending ? 'var(--brand-tint)' : 'var(--brand)', color: sendOtpMutation.isPending ? 'var(--brand)' : '#fff', border: 'none', borderRadius: 'var(--radius-md)', cursor: sendOtpMutation.isPending ? 'not-allowed' : 'pointer', fontFamily: 'inherit' }}>
                {sendOtpMutation.isPending ? '발송 중...' : '인증 코드 받기 →'}
              </button>
            </div>
          </form>
        )}

        {/* STEP 2: 이메일 인증 */}
        {step === 2 && (
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
              <button type="button" onClick={() => { setError(''); setStep(1) }}
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

        {/* STEP 3: 프로필 설정 */}
        {step === 3 && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
            {/* 축하 배너 */}
            <div style={{ textAlign: 'center', padding: '20px 0 4px' }}>
              <div style={{ fontSize: '40px', marginBottom: '10px' }}>🎉</div>
              <h2 style={{ fontSize: '20px', fontWeight: 800, color: 'var(--fg)', marginBottom: '6px' }}>
                가입을 축하합니다!
              </h2>
              <div style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', padding: '4px 12px', borderRadius: '99px', background: 'var(--brand-tint)', fontSize: '13px', fontWeight: 700, color: 'var(--brand-dark)' }}>
                {ROLE_ICONS[form.role]} {ROLE_LABELS[form.role]}
              </div>
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
                placeholder={
                  form.role === 'EXPERT'   ? '보유 전문 분야와 경력을 소개해주세요.' :
                  form.role === 'PROPOSER' ? '어떤 아이디어를 실현하고 싶은지 소개해주세요.' :
                                             '후원 관심 분야나 투자 성향을 소개해주세요.'
                }
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
                {form.role === 'EXPERT' ? '포트폴리오 / 자격 증빙 URL' : '포트폴리오 URL'}
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
              {form.role === 'EXPERT' && (
                <p style={{ fontSize: '12px', color: 'var(--fg-muted)', marginTop: '4px' }}>
                  자격증, 경력 증빙 링크를 첨부하면 전문가 검증에 활용됩니다.
                </p>
              )}
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

        {step < 3 && (
          <p style={{ textAlign: 'center', marginTop: '24px', fontSize: '14px', color: 'var(--fg-muted)' }}>
            이미 계정이 있으신가요?{' '}
            <Link href="/login" style={{ color: 'var(--brand-dark)', fontWeight: 700 }}>로그인</Link>
          </p>
        )}
      </div>
    </div>
  )
}
