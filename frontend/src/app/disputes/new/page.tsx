'use client'

import { useState } from 'react'
import { useRouter, useSearchParams } from 'next/navigation'
import { Suspense } from 'react'
import { useMutation } from '@tanstack/react-query'
import { disputesApi } from '@/api/disputes'
import { ProtectedRoute } from '@/components/layout/AppShell'
import { getErrorMessage } from '@/utils/format'

function DisputeNewForm() {
  const router = useRouter()
  const searchParams = useSearchParams()
  const ideaId = Number(searchParams.get('ideaId'))

  const [reason, setReason] = useState('')
  const [evidenceUrl, setEvidenceUrl] = useState('')
  const [error, setError] = useState('')

  const createMutation = useMutation({
    mutationFn: () =>
      disputesApi.create({ ideaId, reason, evidenceUrl: evidenceUrl || undefined }),
    onSuccess: (data) => router.push(`/disputes/${data.id}`),
    onError: (err) => setError(getErrorMessage(err)),
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    if (!ideaId || isNaN(ideaId)) { setError('잘못된 접근입니다.'); return }
    if (reason.trim().length < 10) { setError('신고 사유를 10자 이상 입력해주세요.'); return }
    createMutation.mutate()
  }

  return (
    <div style={{ maxWidth: '680px', margin: '0 auto', padding: '48px 24px' }}>
      {/* 헤더 */}
      <div style={{ marginBottom: '32px' }}>
        <button
          onClick={() => router.back()}
          style={{ background: 'none', border: 'none', cursor: 'pointer', color: 'var(--fg-muted)', fontSize: '14px', padding: '0', marginBottom: '16px' }}
        >
          ← 뒤로
        </button>
        <h1 style={{ fontSize: '24px', fontWeight: 700, color: 'var(--fg)', marginBottom: '6px' }}>분쟁 신고</h1>
        <p style={{ fontSize: '14px', color: 'var(--fg-muted)' }}>
          허위 신고 시 서비스 이용이 제한될 수 있습니다. 사실에 기반한 신고만 접수해주세요.
        </p>
      </div>

      {/* 대상 아이디어 확인 */}
      {ideaId ? (
        <div style={{
          padding: '14px 16px',
          background: 'var(--brand-tint)',
          border: '1px solid var(--brand)',
          borderRadius: '10px',
          fontSize: '14px',
          color: 'var(--brand-dark)',
          marginBottom: '28px',
          display: 'flex',
          alignItems: 'center',
          gap: '8px',
        }}>
          <span>🎯</span>
          <span>아이디어 <strong>#{ideaId}</strong>에 대한 분쟁을 신고합니다.</span>
        </div>
      ) : (
        <div style={{
          padding: '14px 16px',
          background: '#fff5f5',
          border: '1px solid var(--error)',
          borderRadius: '10px',
          fontSize: '14px',
          color: 'var(--error)',
          marginBottom: '28px',
        }}>
          아이디어 정보가 없습니다. 아이디어 상세 페이지에서 신고해주세요.
        </div>
      )}

      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
        {/* 신고 사유 */}
        <div>
          <label style={{ display: 'block', fontSize: '14px', fontWeight: 600, color: 'var(--fg)', marginBottom: '8px' }}>
            신고 사유 <span style={{ color: 'var(--error)' }}>*</span>
          </label>
          <textarea
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            placeholder="구체적인 신고 사유를 입력해주세요. (최소 10자)"
            rows={6}
            required
            style={{
              width: '100%',
              border: '1.5px solid var(--border)',
              borderRadius: '10px',
              padding: '12px 14px',
              fontSize: '14px',
              fontFamily: 'inherit',
              lineHeight: 1.6,
              resize: 'vertical',
              outline: 'none',
              color: 'var(--fg)',
              boxSizing: 'border-box',
              transition: 'border-color 0.2s',
            }}
            onFocus={(e) => { e.target.style.borderColor = 'var(--brand)' }}
            onBlur={(e) => { e.target.style.borderColor = 'var(--border)' }}
          />
          <div style={{ fontSize: '12px', color: 'var(--fg-muted)', marginTop: '4px', textAlign: 'right' }}>
            {reason.length}자
          </div>
        </div>

        {/* 증거 URL (선택) */}
        <div>
          <label style={{ display: 'block', fontSize: '14px', fontWeight: 600, color: 'var(--fg)', marginBottom: '8px' }}>
            증거 자료 URL <span style={{ fontSize: '12px', fontWeight: 400, color: 'var(--fg-muted)' }}>(선택)</span>
          </label>
          <input
            type="url"
            value={evidenceUrl}
            onChange={(e) => setEvidenceUrl(e.target.value)}
            placeholder="https://..."
            style={{
              width: '100%',
              height: '44px',
              border: '1.5px solid var(--border)',
              borderRadius: '10px',
              padding: '0 14px',
              fontSize: '14px',
              fontFamily: 'inherit',
              outline: 'none',
              color: 'var(--fg)',
              boxSizing: 'border-box',
              transition: 'border-color 0.2s',
            }}
            onFocus={(e) => { e.target.style.borderColor = 'var(--brand)' }}
            onBlur={(e) => { e.target.style.borderColor = 'var(--border)' }}
          />
          <p style={{ fontSize: '12px', color: 'var(--fg-muted)', marginTop: '4px' }}>
            스크린샷, 외부 링크 등 관련 증거를 첨부할 수 있습니다.
          </p>
        </div>

        {error && (
          <div style={{
            padding: '12px 14px',
            background: '#fff5f5',
            border: '1px solid #fecaca',
            borderRadius: '10px',
            fontSize: '14px',
            color: 'var(--error)',
          }}>
            {error}
          </div>
        )}

        {/* 안내 박스 */}
        <div style={{
          padding: '16px',
          background: 'var(--bg-alt)',
          borderRadius: '10px',
          fontSize: '13px',
          color: 'var(--fg-muted)',
          lineHeight: 1.7,
        }}>
          <strong style={{ color: 'var(--fg)', display: 'block', marginBottom: '6px' }}>분쟁 처리 절차 안내</strong>
          <div>① 신고 접수 후 관리자가 내용을 검토합니다.</div>
          <div>② 검토 결과에 따라 해결 또는 반려 처리됩니다.</div>
          <div>③ 반려 시 소명 자료를 제출할 수 있습니다.</div>
          <div>④ 허위 신고 확인 시 이용이 제한될 수 있습니다.</div>
        </div>

        <div style={{ display: 'flex', gap: '10px' }}>
          <button
            type="button"
            onClick={() => router.back()}
            style={{
              flex: 1, height: '52px', fontSize: '15px', fontWeight: 600,
              background: '#fff', border: '1.5px solid var(--border)',
              borderRadius: '10px', cursor: 'pointer', fontFamily: 'inherit',
              color: 'var(--fg-muted)',
            }}
          >
            취소
          </button>
          <button
            type="submit"
            disabled={createMutation.isPending || !ideaId}
            style={{
              flex: 2, height: '52px', fontSize: '16px', fontWeight: 700,
              background: createMutation.isPending ? 'var(--brand-tint)' : 'var(--brand)',
              color: createMutation.isPending ? 'var(--brand)' : '#fff',
              border: 'none', borderRadius: '10px',
              cursor: createMutation.isPending ? 'not-allowed' : 'pointer',
              fontFamily: 'inherit', transition: 'background 0.2s',
            }}
          >
            {createMutation.isPending ? '신고 접수 중...' : '신고 접수'}
          </button>
        </div>
      </form>
    </div>
  )
}

export default function DisputeNewPage() {
  return (
    <ProtectedRoute>
      <Suspense>
        <DisputeNewForm />
      </Suspense>
    </ProtectedRoute>
  )
}
