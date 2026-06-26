'use client'

import { useState } from 'react'
import { useRouter, useParams } from 'next/navigation'
import { useQuery, useMutation } from '@tanstack/react-query'
import { matchesApi, type ReviewRequest } from '@/api/matches'
import { ideasApi } from '@/api/ideas'
import { ProtectedRoute } from '@/components/layout/AppShell'
import { LoadingSpinner } from '@/components/ui/LoadingSpinner'
import { Badge } from '@/components/ui/Badge'
import { IDEA_CATEGORY_LABELS, IDEA_STATUS_LABELS, type IdeaCategory, type IdeaStatus } from '@/types/enums'
import { formatCurrency, formatDate } from '@/utils/format'

const EMPTY_FORM: ReviewRequest = {
  feasibility: 'POSSIBLE',
  expectedPeriod: '',
  techStack: '',
  riskFactor: '',
  opinion: '',
}

const CATEGORY_ICONS: Record<IdeaCategory, string> = {
  TECH: '💻', LIFE: '🛍️', HEALTH: '🏥', EDUCATION: '🎓', ENVIRONMENT: '🌿', CULTURE: '🎨', ETC: '📦',
}

const inputStyle = {
  width: '100%',
  padding: '11px 14px',
  borderRadius: '8px',
  border: '1.5px solid var(--border)',
  fontSize: '14px',
  outline: 'none',
  boxSizing: 'border-box' as const,
  fontFamily: 'inherit',
  color: 'var(--fg)',
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
      <label style={{ fontSize: '13px', fontWeight: 700, color: 'var(--fg)' }}>{label}</label>
      {children}
    </div>
  )
}

function IdeaSection({ icon, title, content }: { icon: string; title: string; content: string }) {
  return (
    <div>
      <div style={{ fontSize: '12px', fontWeight: 700, color: 'var(--fg-muted)', marginBottom: '4px', display: 'flex', alignItems: 'center', gap: '5px' }}>
        <span>{icon}</span> {title}
      </div>
      <p style={{ fontSize: '14px', color: 'var(--fg)', lineHeight: 1.7, whiteSpace: 'pre-wrap', margin: 0 }}>
        {content}
      </p>
    </div>
  )
}

export default function ReviewWritePage() {
  const router = useRouter()
  const params = useParams()
  const matchId = Number(params.matchId)

  const [form, setForm] = useState<ReviewRequest>(EMPTY_FORM)
  const [error, setError] = useState('')

  const set = (key: keyof ReviewRequest, val: string) =>
    setForm((prev) => ({ ...prev, [key]: val }))

  const { data: matches, isLoading: matchesLoading } = useQuery({
    queryKey: ['matches'],
    queryFn: matchesApi.getMyMatches,
  })

  const match = matches?.find((m) => m.matchId === matchId)

  const { data: idea, isLoading: ideaLoading } = useQuery({
    queryKey: ['ideas', match?.ideaId],
    queryFn: () => ideasApi.getById(match!.ideaId),
    enabled: !!match?.ideaId,
  })

  const reviewMutation = useMutation({
    mutationFn: () => matchesApi.createReview(matchId, form),
    onSuccess: () => {
      try {
        localStorage.setItem(`reviewed_match_${matchId}`, 'true')
      } catch { /* ignore */ }
      router.push('/expert/matches')
    },
    onError: () => setError('제출 중 오류가 발생했습니다. 다시 시도해주세요.'),
  })

  const isLoading = matchesLoading || ideaLoading
  const isValid =
    form.expectedPeriod.trim() &&
    form.techStack.trim() &&
    form.riskFactor.trim() &&
    form.opinion.trim()

  if (isLoading) return (
    <ProtectedRoute roles={['EXPERT']}>
      <LoadingSpinner />
    </ProtectedRoute>
  )

  if (!match || match.status !== 'ACCEPTED') {
    return (
      <ProtectedRoute roles={['EXPERT']}>
        <div style={{
          maxWidth: '600px', margin: '80px auto', padding: '0 24px',
          textAlign: 'center', color: 'var(--fg-muted)',
        }}>
          <div style={{ fontSize: '48px', marginBottom: '16px' }}>⚠️</div>
          <p style={{ fontSize: '16px' }}>유효하지 않은 매칭이거나 검증서를 작성할 수 없는 상태입니다.</p>
          <button
            onClick={() => router.push('/expert/matches')}
            style={{
              marginTop: '20px', padding: '10px 24px', borderRadius: '8px',
              border: '1.5px solid var(--border)', background: '#fff',
              color: 'var(--fg)', fontSize: '14px', fontWeight: 600, cursor: 'pointer',
            }}
          >
            매칭 목록으로
          </button>
        </div>
      </ProtectedRoute>
    )
  }

  const ideaSections = idea ? [
    { icon: '❓', title: '문제 정의', content: idea.problemDefinition },
    { icon: '💡', title: '해결 방안', content: idea.solution },
    { icon: '🎯', title: '목표', content: idea.goal },
    { icon: '👥', title: '목표 고객', content: idea.targetCustomer },
    { icon: '⚔️', title: '경쟁사 분석', content: idea.competitor },
    { icon: '🤝', title: '팀 소개', content: idea.teamIntro },
  ].filter((s) => s.content) : []

  return (
    <ProtectedRoute roles={['EXPERT']}>
      <div style={{ maxWidth: '1200px', margin: '0 auto', padding: '32px 24px' }}>

        {/* 헤더 */}
        <button
          onClick={() => router.back()}
          style={{
            display: 'inline-flex', alignItems: 'center', gap: '6px',
            fontSize: '14px', color: 'var(--fg-muted)', background: 'none',
            border: 'none', cursor: 'pointer', padding: 0, marginBottom: '24px',
          }}
        >
          ← 매칭 목록으로
        </button>

        <h1 style={{ fontSize: '22px', fontWeight: 800, color: 'var(--fg)', marginBottom: '24px' }}>
          검증서 작성
        </h1>

        {/* 2컬럼 레이아웃 */}
        <div style={{
          display: 'grid',
          gridTemplateColumns: '1fr 480px',
          gap: '28px',
          alignItems: 'start',
        }}>

          {/* ===== 왼쪽: 아이디어 상세 ===== */}
          <div style={{
            position: 'sticky',
            top: 'calc(var(--unb-height, 0px) + var(--nav-height, 0px) + 24px)',
            display: 'flex', flexDirection: 'column', gap: '16px',
            maxHeight: 'calc(100vh - 160px)',
            overflowY: 'auto',
          }}>
            {idea ? (
              <>
                {/* 카테고리 + 상태 */}
                <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                  <span style={{
                    display: 'inline-flex', alignItems: 'center', gap: '5px',
                    fontSize: '12px', fontWeight: 700,
                    color: 'var(--brand-dark)', background: 'var(--brand-tint)',
                    padding: '3px 9px', borderRadius: '6px',
                  }}>
                    {CATEGORY_ICONS[idea.category]} {IDEA_CATEGORY_LABELS[idea.category]}
                  </span>
                  <Badge variant="orange">
                    {IDEA_STATUS_LABELS[idea.status as IdeaStatus]}
                  </Badge>
                </div>

                {/* 제목 + 한 줄 소개 */}
                <div>
                  <h2 style={{ fontSize: '20px', fontWeight: 800, color: 'var(--fg)', marginBottom: '6px' }}>
                    {idea.title}
                  </h2>
                  <p style={{ fontSize: '14px', color: 'var(--fg-muted)', lineHeight: 1.6 }}>
                    {idea.oneLineIntro}
                  </p>
                </div>

                {/* 펀딩 정보 */}
                <div style={{
                  background: 'var(--bg-alt)',
                  borderRadius: '10px',
                  padding: '14px 16px',
                  display: 'grid',
                  gridTemplateColumns: '1fr 1fr',
                  gap: '10px 20px',
                }}>
                  {[
                    { label: '목표 금액', value: formatCurrency(idea.goalAmount) },
                    { label: '보증금', value: formatCurrency(idea.depositAmount) },
                    { label: '펀딩 시작', value: formatDate(idea.fundingStartAt) },
                    { label: '펀딩 종료', value: formatDate(idea.fundingEndAt) },
                  ].map(({ label, value }) => (
                    <div key={label}>
                      <div style={{ fontSize: '11px', color: 'var(--fg-muted)', marginBottom: '2px' }}>{label}</div>
                      <div style={{ fontSize: '13px', fontWeight: 600, color: 'var(--fg)' }}>{value}</div>
                    </div>
                  ))}
                </div>

                {/* 아이디어 본문 섹션들 */}
                <div style={{
                  background: '#fff',
                  border: '1px solid var(--border)',
                  borderRadius: '12px',
                  padding: '20px',
                  display: 'flex',
                  flexDirection: 'column',
                  gap: '18px',
                }}>
                  {ideaSections.map((s) => (
                    <IdeaSection key={s.title} icon={s.icon} title={s.title} content={s.content} />
                  ))}
                </div>
              </>
            ) : (
              <div style={{ padding: '40px', textAlign: 'center', color: 'var(--fg-muted)' }}>
                아이디어 정보를 불러올 수 없습니다.
              </div>
            )}
          </div>

          {/* ===== 오른쪽: 검증서 폼 ===== */}
          <div style={{
            background: '#fff', border: '1px solid var(--border)',
            borderRadius: '14px', padding: '28px',
            display: 'flex', flexDirection: 'column', gap: '22px',
          }}>
            <h2 style={{ fontSize: '17px', fontWeight: 700, color: 'var(--fg)', margin: 0 }}>
              📋 검증서 항목 작성
            </h2>

            {/* 실현 가능성 */}
            <Field label="실현 가능성">
              <div style={{ display: 'flex', gap: '10px' }}>
                {(['POSSIBLE', 'IMPOSSIBLE'] as const).map((v) => (
                  <button
                    key={v}
                    type="button"
                    onClick={() => set('feasibility', v)}
                    style={{
                      flex: 1, padding: '11px', borderRadius: '8px', fontWeight: 700,
                      fontSize: '14px', cursor: 'pointer', transition: 'all 0.15s',
                      border: `2px solid ${form.feasibility === v
                        ? (v === 'POSSIBLE' ? 'var(--brand)' : '#dc2626')
                        : 'var(--border)'}`,
                      background: form.feasibility === v
                        ? (v === 'POSSIBLE' ? 'var(--brand)' : '#dc2626')
                        : '#fff',
                      color: form.feasibility === v ? '#fff' : 'var(--fg)',
                    }}
                  >
                    {v === 'POSSIBLE' ? '✅ 가능' : '❌ 불가능'}
                  </button>
                ))}
              </div>
            </Field>

            {/* 예상 개발 기간 */}
            <Field label="예상 개발 기간">
              <input
                type="text"
                value={form.expectedPeriod}
                onChange={(e) => set('expectedPeriod', e.target.value)}
                placeholder="예: 6개월 ~ 1년"
                style={inputStyle}
              />
            </Field>

            {/* 필요 기술 스택 */}
            <Field label="필요 기술 스택">
              <input
                type="text"
                value={form.techStack}
                onChange={(e) => set('techStack', e.target.value)}
                placeholder="예: React, Spring Boot, MySQL"
                style={inputStyle}
              />
            </Field>

            {/* 리스크 요인 */}
            <Field label="리스크 요인">
              <textarea
                value={form.riskFactor}
                onChange={(e) => set('riskFactor', e.target.value)}
                rows={3}
                placeholder="주요 리스크 요인을 작성해주세요."
                style={{ ...inputStyle, resize: 'vertical', minHeight: '80px' }}
              />
            </Field>

            {/* 전문가 의견 */}
            <Field label="전문가 의견">
              <textarea
                value={form.opinion}
                onChange={(e) => set('opinion', e.target.value)}
                rows={5}
                placeholder="아이디어에 대한 종합적인 전문가 의견을 작성해주세요."
                style={{ ...inputStyle, resize: 'vertical', minHeight: '120px' }}
              />
            </Field>

            {error && (
              <p style={{
                fontSize: '13px', color: '#dc2626',
                background: '#fef2f2', border: '1px solid #fecaca',
                padding: '10px 14px', borderRadius: '8px', margin: 0,
              }}>
                {error}
              </p>
            )}

            {/* 제출 버튼 */}
            <div style={{ display: 'flex', gap: '10px', paddingTop: '4px' }}>
              <button
                onClick={() => reviewMutation.mutate()}
                disabled={!isValid || reviewMutation.isPending}
                style={{
                  flex: 1, padding: '14px', borderRadius: '10px', border: 'none',
                  background: isValid && !reviewMutation.isPending ? 'var(--brand)' : 'var(--border)',
                  color: '#fff', fontWeight: 700, fontSize: '15px',
                  cursor: isValid && !reviewMutation.isPending ? 'pointer' : 'not-allowed',
                  transition: 'background 0.15s',
                }}
              >
                {reviewMutation.isPending ? '제출 중...' : '검증서 제출'}
              </button>
              <button
                onClick={() => router.back()}
                style={{
                  padding: '14px 20px', borderRadius: '10px',
                  border: '1.5px solid var(--border)', background: '#fff',
                  color: 'var(--fg)', fontWeight: 600, fontSize: '15px', cursor: 'pointer',
                }}
              >
                취소
              </button>
            </div>
          </div>
        </div>
      </div>
    </ProtectedRoute>
  )
}
