'use client'

import Link from 'next/link'
import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { expertsApi, type TechStack } from '@/api/experts'
import { useAuthStore } from '@/store/authStore'
import { LoadingSpinner } from '@/components/ui/LoadingSpinner'
import { Button } from '@/components/ui/Button'
import { getErrorMessage } from '@/utils/format'

const STATUS_INFO = {
  PENDING_VERIFICATION: { icon: '⏳', label: '검토 중',   bg: '#fff9e6', border: '#f0c040' },
  ACTIVE:               { icon: '✅', label: '검증 완료', bg: '#f0fdf4', border: '#86efac' },
  SUSPENDED:            { icon: '🔒', label: '계정 격리', bg: '#fff5f5', border: '#fca5a5' },
  DEMOTED:              { icon: '⛔', label: '자격 취소', bg: '#f8fafc', border: '#e2e8f0' },
}

const QUALIFICATION_LABELS = {
  BUSINESS_REGISTRATION: { label: '사업자등록', icon: '🏢', numberLabel: '사업자등록번호' },
  NATIONAL_QUALIFICATION: { label: '국가자격증', icon: '📋', numberLabel: '자격증 번호' },
}

const TECH_STACK_OPTIONS: { value: TechStack; label: string; icon: string }[] = [
  { value: 'TECH',        label: '기술/IT',   icon: '💻' },
  { value: 'LIFE',        label: '생활',       icon: '🏠' },
  { value: 'HEALTH',      label: '건강/의료',  icon: '🏥' },
  { value: 'EDUCATION',   label: '교육',       icon: '📚' },
  { value: 'ENVIRONMENT', label: '환경',       icon: '🌿' },
  { value: 'CULTURE',     label: '문화/예술',  icon: '🎨' },
  { value: 'ETC',         label: '기타',       icon: '📦' },
]

const TECH_STACK_LABEL: Record<TechStack, string> = {
  TECH: '기술/IT', LIFE: '생활', HEALTH: '건강/의료',
  EDUCATION: '교육', ENVIRONMENT: '환경', CULTURE: '문화/예술', ETC: '기타',
}

function ExpertProfileEditForm({ current, onCancel }: {
  current: { techStack: TechStack | null; portfolioUrl: string | null; career: string | null }
  onCancel: () => void
}) {
  const queryClient = useQueryClient()
  const [techStack, setTechStack] = useState<TechStack | ''>(current.techStack ?? '')
  const [portfolioUrl, setPortfolioUrl] = useState(current.portfolioUrl ?? '')
  const [career, setCareer] = useState(current.career ?? '')
  const [error, setError] = useState('')

  const mutation = useMutation({
    mutationFn: () => expertsApi.updateMyProfile({
      techStack: techStack as TechStack,
      ...(portfolioUrl.trim() && { portfolioUrl: portfolioUrl.trim() }),
      ...(career.trim() && { career: career.trim() }),
    }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['experts', 'me'] })
      onCancel()
    },
    onError: (err) => setError(getErrorMessage(err)),
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!techStack) { setError('전문 분야를 선택해주세요.'); return }
    setError('')
    mutation.mutate()
  }

  return (
    <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      <div>
        <label style={{ display: 'block', fontSize: '14px', fontWeight: 600, color: 'var(--fg)', marginBottom: '10px' }}>
          전문 분야 <span style={{ color: 'var(--error)' }}>*</span>
        </label>
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '8px' }}>
          {TECH_STACK_OPTIONS.map((opt) => (
            <button
              key={opt.value}
              type="button"
              onClick={() => setTechStack(opt.value)}
              style={{
                border: `2px solid ${techStack === opt.value ? 'var(--brand)' : 'var(--border)'}`,
                borderRadius: '10px', padding: '12px 8px',
                background: techStack === opt.value ? 'var(--brand-tint)' : '#fff',
                display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '6px',
                cursor: 'pointer', transition: 'all 0.15s',
              }}
            >
              <span style={{ fontSize: '22px' }}>{opt.icon}</span>
              <span style={{ fontSize: '12px', fontWeight: 600, color: techStack === opt.value ? 'var(--brand-dark)' : 'var(--fg)' }}>
                {opt.label}
              </span>
            </button>
          ))}
        </div>
      </div>

      <div>
        <label style={{ display: 'block', fontSize: '14px', fontWeight: 600, color: 'var(--fg)', marginBottom: '6px' }}>
          포트폴리오 URL <span style={{ fontSize: '12px', fontWeight: 400, color: 'var(--fg-muted)' }}>(선택)</span>
        </label>
        <input
          type="url"
          value={portfolioUrl}
          onChange={(e) => setPortfolioUrl(e.target.value)}
          placeholder="https://github.com/yourname"
          style={{
            width: '100%', height: '44px', border: '1.5px solid var(--border)',
            borderRadius: '10px', padding: '0 12px', fontSize: '14px',
            fontFamily: 'inherit', outline: 'none', color: 'var(--fg)', boxSizing: 'border-box',
          }}
        />
      </div>

      <div>
        <label style={{ display: 'block', fontSize: '14px', fontWeight: 600, color: 'var(--fg)', marginBottom: '6px' }}>
          경력 소개 <span style={{ fontSize: '12px', fontWeight: 400, color: 'var(--fg-muted)' }}>(선택)</span>
        </label>
        <textarea
          value={career}
          onChange={(e) => setCareer(e.target.value)}
          rows={4}
          placeholder="전문 분야와 경력을 소개해주세요."
          style={{
            width: '100%', border: '1.5px solid var(--border)',
            borderRadius: '10px', padding: '12px', fontSize: '14px',
            fontFamily: 'inherit', outline: 'none', color: 'var(--fg)',
            boxSizing: 'border-box', resize: 'vertical', lineHeight: 1.6,
          }}
        />
      </div>

      {error && <p style={{ fontSize: '13px', color: 'var(--error)' }}>{error}</p>}

      <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end' }}>
        <Button variant="ghost" onClick={onCancel} type="button">취소</Button>
        <Button type="submit" loading={mutation.isPending} disabled={!techStack}>저장</Button>
      </div>
    </form>
  )
}

export default function ExpertStatusPage() {
  const { user } = useAuthStore()
  const [editing, setEditing] = useState(false)

  const { data: profile, isLoading, isError } = useQuery({
    queryKey: ['experts', 'me'],
    queryFn: expertsApi.getMyProfile,
    retry: false,
  })

  if (isLoading) return <LoadingSpinner />

  const isExpert = user?.role === 'EXPERT'

  // 신청 기록 없음
  if (isError || !profile) {
    return (
      <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
        <h2 style={{ fontSize: '20px', fontWeight: 700, color: 'var(--fg)' }}>🎓 전문가 신청</h2>
        <div style={{ background: '#fff', border: '1px solid var(--border)', borderRadius: '12px', padding: '32px 24px' }}>
          <p style={{ fontSize: '14px', color: 'var(--fg-muted)', lineHeight: 1.7, marginBottom: '20px' }}>
            아직 전문가 신청을 하지 않았습니다.<br />
            자격 검증 후 전문가로 활동할 수 있습니다.
          </p>
          <Link href="/expert-apply" style={{
            display: 'inline-block', height: '44px', lineHeight: '44px',
            padding: '0 24px', background: 'var(--brand)', color: '#fff',
            borderRadius: '10px', fontSize: '15px', fontWeight: 700, textDecoration: 'none',
          }}>
            전문가 신청하기 →
          </Link>
        </div>
      </div>
    )
  }

  const statusInfo = STATUS_INFO[profile.status]
  const qual = QUALIFICATION_LABELS[profile.qualificationType]

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <h2 style={{ fontSize: '20px', fontWeight: 700, color: 'var(--fg)' }}>
          {isExpert ? '🎓 전문가 프로필' : '🎓 전문가 신청'}
        </h2>
        {isExpert && !editing && (
          <Button variant="ghost" onClick={() => setEditing(true)}>수정</Button>
        )}
      </div>

      {/* 상태 배지 */}
      <div style={{
        display: 'flex', alignItems: 'center', gap: '10px',
        background: statusInfo.bg, border: `1px solid ${statusInfo.border}`,
        borderRadius: '10px', padding: '12px 16px',
      }}>
        <span style={{ fontSize: '20px' }}>{statusInfo.icon}</span>
        <span style={{ fontSize: '14px', fontWeight: 700, color: 'var(--fg)' }}>{statusInfo.label}</span>
      </div>

      {/* 자격 정보 */}
      <div style={{ background: '#fff', border: '1px solid var(--border)', borderRadius: '12px', padding: '20px 24px' }}>
        <p style={{ fontSize: '12px', fontWeight: 600, color: 'var(--fg-muted)', marginBottom: '14px', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
          자격 정보
        </p>
        <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <span style={{ fontSize: '14px', color: 'var(--fg-muted)' }}>검증 유형</span>
            <span style={{ fontSize: '14px', fontWeight: 600 }}>{qual.icon} {qual.label}</span>
          </div>
          <div style={{ height: '1px', background: 'var(--border)' }} />
          <div style={{ display: 'flex', justifyContent: 'space-between' }}>
            <span style={{ fontSize: '14px', color: 'var(--fg-muted)' }}>{qual.numberLabel}</span>
            <span style={{ fontSize: '14px', fontWeight: 600, fontFamily: 'monospace' }}>{profile.qualificationNumber}</span>
          </div>
        </div>
      </div>

      {/* 전문가 프로필 (EXPERT 전용) */}
      {isExpert && (
        <div style={{ background: '#fff', border: '1px solid var(--border)', borderRadius: '12px', padding: '20px 24px' }}>
          <p style={{ fontSize: '12px', fontWeight: 600, color: 'var(--fg-muted)', marginBottom: '14px', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
            프로필 정보
          </p>

          {editing ? (
            <ExpertProfileEditForm
              current={{ techStack: profile.techStack, portfolioUrl: profile.portfolioUrl, career: profile.career }}
              onCancel={() => setEditing(false)}
            />
          ) : (
            <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ fontSize: '14px', color: 'var(--fg-muted)' }}>전문 분야</span>
                <span style={{ fontSize: '14px', fontWeight: 600 }}>
                  {profile.techStack ? TECH_STACK_LABEL[profile.techStack] : <span style={{ color: 'var(--fg-muted)' }}>미등록</span>}
                </span>
              </div>
              <div style={{ height: '1px', background: 'var(--border)' }} />
              <div style={{ display: 'flex', justifyContent: 'space-between' }}>
                <span style={{ fontSize: '14px', color: 'var(--fg-muted)' }}>포트폴리오</span>
                <span style={{ fontSize: '14px', fontWeight: 600 }}>
                  {profile.portfolioUrl
                    ? <a href={profile.portfolioUrl} target="_blank" rel="noreferrer" style={{ color: 'var(--brand-dark)' }}>{profile.portfolioUrl}</a>
                    : <span style={{ color: 'var(--fg-muted)' }}>미등록</span>}
                </span>
              </div>
              <div style={{ height: '1px', background: 'var(--border)' }} />
              <div style={{ display: 'flex', flexDirection: 'column', gap: '6px' }}>
                <span style={{ fontSize: '14px', color: 'var(--fg-muted)' }}>경력 소개</span>
                <p style={{ fontSize: '14px', lineHeight: 1.7, margin: 0, whiteSpace: 'pre-wrap' }}>
                  {profile.career ?? <span style={{ color: 'var(--fg-muted)' }}>미등록</span>}
                </p>
              </div>
            </div>
          )}
        </div>
      )}

      {/* USER: ACTIVE이면 2단계 안내 */}
      {!isExpert && profile.status === 'ACTIVE' && (
        <div style={{
          background: 'var(--brand-tint)', border: '1px solid var(--brand)',
          borderRadius: '12px', padding: '18px 20px',
          display: 'flex', justifyContent: 'space-between', alignItems: 'center',
        }}>
          <div>
            <p style={{ fontSize: '14px', fontWeight: 700, color: 'var(--brand-dark)', marginBottom: '4px' }}>2단계: 프로필 등록</p>
            <p style={{ fontSize: '13px', color: 'var(--fg-muted)' }}>전문 분야와 경력을 등록하면 매칭 요청을 받을 수 있습니다.</p>
          </div>
          <Link href="/expert/profile" style={{
            flexShrink: 0, height: '40px', lineHeight: '40px', padding: '0 18px',
            background: 'var(--brand)', color: '#fff', borderRadius: '10px',
            fontSize: '14px', fontWeight: 700, textDecoration: 'none', marginLeft: '16px',
          }}>
            프로필 등록 →
          </Link>
        </div>
      )}

      {!isExpert && profile.status === 'DEMOTED' && (
        <Link href="/expert-apply" style={{
          display: 'block', textAlign: 'center', height: '44px', lineHeight: '44px',
          background: 'var(--brand)', color: '#fff', borderRadius: '10px',
          fontSize: '15px', fontWeight: 700, textDecoration: 'none',
        }}>
          재신청하기 →
        </Link>
      )}
    </div>
  )
}
