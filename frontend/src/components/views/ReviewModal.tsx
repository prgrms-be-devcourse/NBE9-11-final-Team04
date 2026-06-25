'use client'

import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { matchesApi, type ReviewRequest } from '@/api/matches'

const EMPTY_REVIEW: ReviewRequest = {
  feasibility: 'POSSIBLE',
  expectedPeriod: '',
  techStack: '',
  riskFactor: '',
  opinion: '',
}

export function ReviewModal({
  matchId,
  ideaTitle,
  onClose,
}: {
  matchId: number
  ideaTitle: string
  onClose: () => void
}) {
  const [form, setForm] = useState<ReviewRequest>(EMPTY_REVIEW)
  const [error, setError] = useState('')

  const reviewMutation = useMutation({
    mutationFn: () => matchesApi.createReview(matchId, form),
    onSuccess: () => {
      try {
        localStorage.setItem(`reviewed_match_${matchId}`, 'true')
      } catch { /* ignore */ }
      alert('검증서가 제출되었습니다.')
      onClose()
    },
    onError: () => setError('제출 중 오류가 발생했습니다. 다시 시도해주세요.'),
  })

  const set = (key: keyof ReviewRequest, val: string) =>
    setForm((prev) => ({ ...prev, [key]: val }))

  const isValid =
    form.expectedPeriod.trim() &&
    form.techStack.trim() &&
    form.riskFactor.trim() &&
    form.opinion.trim()

  const inputStyle = {
    width: '100%', padding: '10px 12px', borderRadius: '8px',
    border: '1.5px solid var(--border)', fontSize: '14px',
    outline: 'none', boxSizing: 'border-box' as const,
    fontFamily: 'inherit',
  }

  return (
    <div style={{
      position: 'fixed', inset: 0, zIndex: 1000,
      background: 'rgba(0,0,0,0.45)',
      display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '24px',
    }}>
      <div style={{
        background: '#fff', borderRadius: '16px', padding: '32px',
        width: '100%', maxWidth: '560px',
        display: 'flex', flexDirection: 'column', gap: '20px',
        maxHeight: '90vh', overflowY: 'auto',
      }}>
        <div>
          <h3 style={{ fontSize: '18px', fontWeight: 800, color: 'var(--fg)', marginBottom: '4px' }}>
            검증서 작성
          </h3>
          <p style={{ fontSize: '13px', color: 'var(--fg-muted)' }}>{ideaTitle}</p>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
          <label style={{ fontSize: '13px', fontWeight: 700, color: 'var(--fg)' }}>실현 가능성</label>
          <div style={{ display: 'flex', gap: '10px' }}>
            {(['POSSIBLE', 'IMPOSSIBLE'] as const).map((v) => (
              <button
                key={v}
                onClick={() => set('feasibility', v)}
                style={{
                  flex: 1, padding: '10px', borderRadius: '8px', fontWeight: 700,
                  fontSize: '14px', cursor: 'pointer', transition: 'all 0.15s',
                  border: `2px solid ${form.feasibility === v ? (v === 'POSSIBLE' ? 'var(--brand)' : '#dc2626') : 'var(--border)'}`,
                  background: form.feasibility === v ? (v === 'POSSIBLE' ? 'var(--brand)' : '#dc2626') : '#fff',
                  color: form.feasibility === v ? '#fff' : 'var(--fg)',
                }}
              >
                {v === 'POSSIBLE' ? '가능' : '불가능'}
              </button>
            ))}
          </div>
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
          <label style={{ fontSize: '13px', fontWeight: 700, color: 'var(--fg)' }}>예상 개발 기간</label>
          <input
            type="text"
            value={form.expectedPeriod}
            onChange={(e) => set('expectedPeriod', e.target.value)}
            placeholder="예: 6개월 ~ 1년"
            style={inputStyle}
          />
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
          <label style={{ fontSize: '13px', fontWeight: 700, color: 'var(--fg)' }}>필요 기술 스택</label>
          <input
            type="text"
            value={form.techStack}
            onChange={(e) => set('techStack', e.target.value)}
            placeholder="예: React, Spring Boot, MySQL"
            style={inputStyle}
          />
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
          <label style={{ fontSize: '13px', fontWeight: 700, color: 'var(--fg)' }}>리스크 요인</label>
          <textarea
            value={form.riskFactor}
            onChange={(e) => set('riskFactor', e.target.value)}
            rows={3}
            placeholder="주요 리스크 요인을 작성해주세요."
            style={{ ...inputStyle, resize: 'none' }}
          />
        </div>

        <div style={{ display: 'flex', flexDirection: 'column', gap: '8px' }}>
          <label style={{ fontSize: '13px', fontWeight: 700, color: 'var(--fg)' }}>전문가 의견</label>
          <textarea
            value={form.opinion}
            onChange={(e) => set('opinion', e.target.value)}
            rows={4}
            placeholder="아이디어에 대한 전문가 종합 의견을 작성해주세요."
            style={{ ...inputStyle, resize: 'none' }}
          />
        </div>

        {error && (
          <p style={{ fontSize: '13px', color: '#dc2626', margin: 0 }}>{error}</p>
        )}

        <div style={{ display: 'flex', gap: '10px' }}>
          <button
            onClick={() => reviewMutation.mutate()}
            disabled={!isValid || reviewMutation.isPending}
            style={{
              flex: 1, padding: '13px', borderRadius: '8px', border: 'none',
              background: isValid ? 'var(--brand)' : 'var(--border)',
              color: '#fff', fontWeight: 700, fontSize: '15px',
              cursor: isValid ? 'pointer' : 'not-allowed',
            }}
          >
            {reviewMutation.isPending ? '제출 중...' : '검증서 제출'}
          </button>
          <button
            onClick={onClose}
            style={{
              flex: 1, padding: '13px', borderRadius: '8px',
              border: '1.5px solid var(--border)', background: '#fff',
              color: 'var(--fg)', fontWeight: 600, fontSize: '15px', cursor: 'pointer',
            }}
          >
            취소
          </button>
        </div>
      </div>
    </div>
  )
}
