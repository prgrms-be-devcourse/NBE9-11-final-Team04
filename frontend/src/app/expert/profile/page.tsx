'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import Link from 'next/link'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient, unwrap } from '@/api/client'
import type { ApiResponse } from '@/types/api'
import { usersApi } from '@/api/users'
import { ProtectedRoute } from '@/components/layout/AppShell'
import { useAuthStore } from '@/store/authStore'
import { getErrorMessage } from '@/utils/format'

type TechStack = 'TECH' | 'LIFE' | 'HEALTH' | 'EDUCATION' | 'ENVIRONMENT' | 'CULTURE' | 'ETC'

interface ExpertProfileRequest {
  techStack: TechStack
  portfolioUrl?: string
  career?: string
}

const TECH_STACK_OPTIONS: { value: TechStack; label: string; icon: string; desc: string }[] = [
  { value: 'TECH',        label: '기술/IT',    icon: '💻', desc: 'SW·HW·AI·데이터' },
  { value: 'LIFE',        label: '생활',        icon: '🏠', desc: '소비재·라이프스타일' },
  { value: 'HEALTH',      label: '건강/의료',   icon: '🏥', desc: '헬스케어·바이오' },
  { value: 'EDUCATION',   label: '교육',        icon: '📚', desc: 'EdTech·학습' },
  { value: 'ENVIRONMENT', label: '환경',        icon: '🌿', desc: '친환경·에너지' },
  { value: 'CULTURE',     label: '문화/예술',   icon: '🎨', desc: '콘텐츠·미디어' },
  { value: 'ETC',         label: '기타',        icon: '📦', desc: '그 외 분야' },
]

const STEPS = ['자격 검증', '프로필 등록', '전문가 활동']

function StepIndicator({ current }: { current: number }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0', marginBottom: '40px' }}>
      {STEPS.map((label, i) => (
        <div key={label} style={{ display: 'flex', alignItems: 'center' }}>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '6px' }}>
            <div style={{
              width: '32px', height: '32px', borderRadius: '50%',
              background: i < current ? '#22c55e' : i === current ? 'var(--brand)' : 'var(--border)',
              color: i <= current ? '#fff' : 'var(--fg-muted)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: '13px', fontWeight: 700,
            }}>
              {i < current ? '✓' : i + 1}
            </div>
            <span style={{ fontSize: '12px', fontWeight: i === current ? 700 : 400, color: i === current ? 'var(--brand-dark)' : 'var(--fg-muted)', whiteSpace: 'nowrap' }}>
              {label}
            </span>
          </div>
          {i < STEPS.length - 1 && (
            <div style={{ width: '80px', height: '2px', background: i < current ? '#22c55e' : 'var(--border)', margin: '0 8px', marginBottom: '18px' }} />
          )}
        </div>
      ))}
    </div>
  )
}

function ExpertProfileContent() {
  const router = useRouter()
  const queryClient = useQueryClient()
  const { setUser } = useAuthStore()
  const [techStack, setTechStack] = useState<TechStack | ''>('')
  const [portfolioUrl, setPortfolioUrl] = useState('')
  const [career, setCareer] = useState('')
  const [error, setError] = useState('')

  const profileMutation = useMutation({
    mutationFn: () => {
      const body: ExpertProfileRequest = {
        techStack: techStack as TechStack,
        ...(portfolioUrl.trim() && { portfolioUrl: portfolioUrl.trim() }),
        ...(career.trim() && { career: career.trim() }),
      }
      return unwrap(apiClient.post<ApiResponse<unknown>>('/experts/profile', body))
    },
    onSuccess: async () => {
      const updatedUser = await usersApi.getMe()
      setUser(updatedUser)
      queryClient.setQueryData(['users', 'me'], updatedUser)
      router.push('/expert/matches')
    },
    onError: (err) => setError(getErrorMessage(err)),
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    if (!techStack) { setError('전문 분야를 선택해주세요.'); return }
    profileMutation.mutate()
  }

  return (
    <div style={{ maxWidth: '680px', margin: '0 auto' }}>
      <StepIndicator current={1} />

      <div style={{ marginBottom: '32px', textAlign: 'center' }}>
        <h1 style={{ fontSize: '28px', fontWeight: 800, color: 'var(--fg)', marginBottom: '10px' }}>전문가 프로필 등록</h1>
        <p style={{ fontSize: '15px', color: 'var(--fg-muted)', lineHeight: 1.6 }}>
          전문 분야를 선택하고 경력을 소개해주세요.<br />
          아이디어 제안자가 적합한 전문가를 찾을 때 활용됩니다.
        </p>
      </div>

      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '28px' }}>

        {/* 전문 분야 선택 */}
        <div>
          <label style={{ display: 'block', fontSize: '15px', fontWeight: 700, color: 'var(--fg)', marginBottom: '14px' }}>
            전문 분야 <span style={{ color: 'var(--error)' }}>*</span>
          </label>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '10px' }}>
            {TECH_STACK_OPTIONS.map((opt) => (
              <button
                key={opt.value}
                type="button"
                onClick={() => setTechStack(opt.value)}
                style={{
                  border: `2px solid ${techStack === opt.value ? 'var(--brand)' : 'var(--border)'}`,
                  borderRadius: '12px', padding: '16px 10px',
                  background: techStack === opt.value ? 'var(--brand-tint)' : '#fff',
                  textAlign: 'center', cursor: 'pointer',
                  display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px',
                  transition: 'all 0.2s',
                }}
              >
                <span style={{ fontSize: '26px' }}>{opt.icon}</span>
                <span style={{ fontSize: '13px', fontWeight: 700, color: techStack === opt.value ? 'var(--brand-dark)' : 'var(--fg)' }}>
                  {opt.label}
                </span>
                <span style={{ fontSize: '11px', color: 'var(--fg-muted)', lineHeight: 1.3 }}>
                  {opt.desc}
                </span>
              </button>
            ))}
          </div>
        </div>

        {/* 포트폴리오 URL */}
        <div>
          <label style={{ display: 'block', fontSize: '15px', fontWeight: 700, color: 'var(--fg)', marginBottom: '8px' }}>
            포트폴리오 URL <span style={{ fontSize: '13px', fontWeight: 400, color: 'var(--fg-muted)' }}>(선택)</span>
          </label>
          <input
            type="url"
            value={portfolioUrl}
            onChange={(e) => setPortfolioUrl(e.target.value)}
            placeholder="https://github.com/yourname 또는 개인 사이트"
            style={{
              width: '100%', height: '48px', border: '1.5px solid var(--border)',
              borderRadius: '10px', padding: '0 14px', fontSize: '15px',
              fontFamily: 'inherit', outline: 'none', color: 'var(--fg)', boxSizing: 'border-box',
            }}
          />
          <p style={{ fontSize: '12px', color: 'var(--fg-muted)', marginTop: '4px' }}>
            GitHub, LinkedIn, 개인 블로그 등 전문성을 확인할 수 있는 URL
          </p>
        </div>

        {/* 경력 소개 */}
        <div>
          <label style={{ display: 'block', fontSize: '15px', fontWeight: 700, color: 'var(--fg)', marginBottom: '8px' }}>
            경력 소개 <span style={{ fontSize: '13px', fontWeight: 400, color: 'var(--fg-muted)' }}>(선택)</span>
          </label>
          <textarea
            value={career}
            onChange={(e) => setCareer(e.target.value)}
            placeholder={'예) 10년간 헬스케어 스타트업에서 제품 개발을 담당했습니다.\n현재는 의료기기 스타트업 CTO로 재직 중이며, 임상시험 및 인허가 경험이 있습니다.'}
            rows={5}
            style={{
              width: '100%', border: '1.5px solid var(--border)',
              borderRadius: '10px', padding: '14px', fontSize: '15px',
              fontFamily: 'inherit', outline: 'none', color: 'var(--fg)',
              boxSizing: 'border-box', resize: 'vertical', lineHeight: 1.6,
            }}
          />
          <p style={{ fontSize: '12px', color: 'var(--fg-muted)', marginTop: '4px' }}>
            아이디어 제안자에게 보여지는 전문가 소개글입니다. 구체적일수록 매칭 확률이 높아집니다.
          </p>
        </div>

        {error && (
          <p style={{ fontSize: '14px', color: 'var(--error)', background: '#fff5f5', padding: '12px 16px', borderRadius: '10px' }}>
            {error}
          </p>
        )}

        <button
          type="submit"
          disabled={profileMutation.isPending || !techStack}
          style={{
            height: '56px', fontSize: '17px', fontWeight: 700,
            background: !techStack || profileMutation.isPending ? 'var(--brand-tint)' : 'var(--brand)',
            color: !techStack || profileMutation.isPending ? 'var(--brand)' : '#fff',
            border: 'none', borderRadius: '12px',
            cursor: !techStack || profileMutation.isPending ? 'not-allowed' : 'pointer',
            fontFamily: 'inherit', transition: 'background 0.2s',
          }}
        >
          {profileMutation.isPending ? '등록 중...' : '전문가 프로필 등록 완료'}
        </button>

        <p style={{ textAlign: 'center', fontSize: '13px', color: 'var(--fg-muted)' }}>
          나중에 등록하려면{' '}
          <Link href="/expert/matches" style={{ color: 'var(--brand-dark)', fontWeight: 600 }}>
            매칭 관리 페이지
          </Link>
          에서 설정할 수 있습니다.
        </p>
      </form>
    </div>
  )
}

export default function ExpertProfilePage() {
  return (
    <ProtectedRoute roles={['USER', 'EXPERT']}>
      <div style={{ minHeight: '80vh', padding: '60px 24px' }}>
        <ExpertProfileContent />
      </div>
    </ProtectedRoute>
  )
}
