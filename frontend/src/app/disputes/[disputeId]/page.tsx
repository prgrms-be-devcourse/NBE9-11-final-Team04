'use client'

import { use, useState } from 'react'
import Link from 'next/link'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { disputesApi } from '@/api/disputes'
import { useAuthStore } from '@/store/authStore'
import { ProtectedRoute } from '@/components/layout/AppShell'
import { Badge } from '@/components/ui/Badge'
import { LoadingSpinner } from '@/components/ui/LoadingSpinner'
import { DISPUTE_STATUS_LABELS, type DisputeStatus } from '@/types/enums'
import { formatDateTime, getErrorMessage } from '@/utils/format'

const STATUS_STEPS: DisputeStatus[] = ['RECEIVED', 'PENDING', 'RESOLVED']

const STATUS_VARIANT: Record<DisputeStatus, 'blue' | 'green' | 'orange' | 'red' | 'gray'> = {
  RECEIVED: 'blue',
  PENDING:  'orange',
  RESOLVED: 'green',
  REJECTED: 'red',
}

function StatusTimeline({ status }: { status: DisputeStatus }) {
  const isRejected = status === 'REJECTED'
  const activeIndex = isRejected
    ? 1
    : STATUS_STEPS.indexOf(status)

  return (
    <div style={{ display: 'flex', alignItems: 'flex-start', gap: '0', marginBottom: '32px' }}>
      {STATUS_STEPS.map((step, i) => {
        const isPast   = i < activeIndex
        const isActive = i === activeIndex
        const isFinal  = i === STATUS_STEPS.length - 1

        const resolvedLabel = isRejected && i === STATUS_STEPS.length - 1 ? '반려됨' : DISPUTE_STATUS_LABELS[step]
        const resolvedVariant = isRejected && i === STATUS_STEPS.length - 1 ? '#ef4444' : undefined

        const circleColor = isPast || isActive
          ? (isRejected && i === STATUS_STEPS.length - 1 ? '#ef4444' : 'var(--brand)')
          : 'var(--border)'

        return (
          <div key={step} style={{ display: 'flex', alignItems: 'flex-start', flex: isFinal ? undefined : 1 }}>
            <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '8px' }}>
              <div style={{
                width: '36px', height: '36px', borderRadius: '50%',
                background: isPast ? circleColor : isActive ? circleColor : '#fff',
                border: `2px solid ${circleColor}`,
                display: 'flex', alignItems: 'center', justifyContent: 'center',
                fontSize: '14px', fontWeight: 700,
                color: isPast || isActive ? '#fff' : 'var(--fg-muted)',
                flexShrink: 0,
                zIndex: 1,
                position: 'relative',
              }}>
                {isPast ? '✓' : i + 1}
              </div>
              <span style={{
                fontSize: '12px', whiteSpace: 'nowrap',
                color: isActive ? (resolvedVariant ?? 'var(--brand-dark)') : isPast ? 'var(--fg-muted)' : 'var(--fg-muted)',
                fontWeight: isActive ? 700 : 400,
              }}>
                {resolvedLabel}
              </span>
            </div>
            {!isFinal && (
              <div style={{
                flex: 1, height: '2px',
                background: isPast ? 'var(--brand)' : 'var(--border)',
                margin: '17px 4px 0',
              }} />
            )}
          </div>
        )
      })}
    </div>
  )
}

function DisputeDetail({ disputeId }: { disputeId: number }) {
  const queryClient = useQueryClient()
  const user = useAuthStore((s) => s.user)

  const [appealContent, setAppealContent] = useState('')
  const [appealFileUrl, setAppealFileUrl] = useState('')
  const [appealError, setAppealError] = useState('')
  const [appealSuccess, setAppealSuccess] = useState(false)

  const { data: dispute, isLoading, error } = useQuery({
    queryKey: ['disputes', disputeId],
    queryFn: () => disputesApi.getById(disputeId),
    enabled: !!disputeId,
  })

  const appealMutation = useMutation({
    mutationFn: () =>
      disputesApi.createAppeal(disputeId, {
        content: appealContent,
        fileUrl: appealFileUrl || undefined,
      }),
    onSuccess: () => {
      setAppealSuccess(true)
      setAppealContent('')
      setAppealFileUrl('')
      queryClient.invalidateQueries({ queryKey: ['disputes', disputeId] })
    },
    onError: (err) => setAppealError(getErrorMessage(err)),
  })

  const handleAppealSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    setAppealError('')
    if (appealContent.trim().length < 10) {
      setAppealError('소명 내용을 10자 이상 입력해주세요.')
      return
    }
    appealMutation.mutate()
  }

  if (isLoading) return <LoadingSpinner />

  if (error || !dispute) {
    return (
      <div style={{ padding: '80px 0', textAlign: 'center', color: 'var(--fg-muted)' }}>
        <div style={{ fontSize: '48px', marginBottom: '16px' }}>⚠️</div>
        분쟁 정보를 불러올 수 없습니다.
      </div>
    )
  }

  const isReporter  = user?.id === dispute.reporterId
  const isProposer  = user?.id === dispute.proposerId
  const canAppeal   = dispute.status === 'REJECTED' && isReporter

  return (
    <div style={{ maxWidth: '680px', margin: '0 auto', padding: '48px 24px' }}>
      {/* 헤더 */}
      <div style={{ marginBottom: '28px' }}>
        <Link href="/ideas" style={{ fontSize: '14px', color: 'var(--fg-muted)', textDecoration: 'none', display: 'inline-block', marginBottom: '16px' }}>
          ← 목록으로
        </Link>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
          <h1 style={{ fontSize: '22px', fontWeight: 700, color: 'var(--fg)', margin: 0 }}>
            분쟁 #{dispute.id}
          </h1>
          <Badge variant={STATUS_VARIANT[dispute.status]}>
            {DISPUTE_STATUS_LABELS[dispute.status]}
          </Badge>
        </div>
        <p style={{ fontSize: '13px', color: 'var(--fg-muted)', marginTop: '6px' }}>
          접수일: {formatDateTime(dispute.createdAt)}
        </p>
      </div>

      {/* 상태 타임라인 */}
      <div style={{
        background: '#fff',
        border: '1px solid var(--border)',
        borderRadius: '12px',
        padding: '24px 28px 16px',
        marginBottom: '20px',
      }}>
        <h2 style={{ fontSize: '15px', fontWeight: 700, color: 'var(--fg)', marginBottom: '20px' }}>처리 현황</h2>
        <StatusTimeline status={dispute.status} />
      </div>

      {/* 신고 내용 */}
      <div style={{
        background: '#fff',
        border: '1px solid var(--border)',
        borderRadius: '12px',
        padding: '24px 28px',
        marginBottom: '20px',
        display: 'flex',
        flexDirection: 'column',
        gap: '18px',
      }}>
        <h2 style={{ fontSize: '15px', fontWeight: 700, color: 'var(--fg)', margin: 0 }}>신고 내용</h2>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
          <div style={{ background: 'var(--bg-alt)', borderRadius: '8px', padding: '12px 14px' }}>
            <div style={{ fontSize: '12px', color: 'var(--fg-muted)', marginBottom: '4px' }}>대상 아이디어</div>
            <Link
              href={`/ideas/${dispute.ideaId}`}
              style={{ fontSize: '14px', fontWeight: 600, color: 'var(--brand-dark)', textDecoration: 'none' }}
            >
              아이디어 #{dispute.ideaId} →
            </Link>
          </div>
          <div style={{ background: 'var(--bg-alt)', borderRadius: '8px', padding: '12px 14px' }}>
            <div style={{ fontSize: '12px', color: 'var(--fg-muted)', marginBottom: '4px' }}>역할</div>
            <div style={{ fontSize: '14px', fontWeight: 600, color: 'var(--fg)' }}>
              {isReporter ? '신고자 (나)' : isProposer ? '피신고자 (나)' : '관련 당사자'}
            </div>
          </div>
        </div>

        <div>
          <div style={{ fontSize: '13px', fontWeight: 600, color: 'var(--fg-muted)', marginBottom: '8px' }}>신고 사유</div>
          <div style={{
            background: 'var(--bg-alt)',
            borderRadius: '8px',
            padding: '14px 16px',
            fontSize: '14px',
            color: 'var(--fg)',
            lineHeight: 1.7,
            whiteSpace: 'pre-wrap',
          }}>
            {dispute.reason}
          </div>
        </div>

        {dispute.evidenceUrl && (
          <div>
            <div style={{ fontSize: '13px', fontWeight: 600, color: 'var(--fg-muted)', marginBottom: '8px' }}>증거 자료</div>
            <a
              href={dispute.evidenceUrl}
              target="_blank"
              rel="noopener noreferrer"
              style={{
                display: 'inline-flex', alignItems: 'center', gap: '6px',
                fontSize: '14px', color: 'var(--brand-dark)',
                textDecoration: 'none',
                padding: '8px 14px',
                background: 'var(--brand-tint)',
                borderRadius: '8px',
                border: '1px solid var(--brand)',
              }}
            >
              🔗 증거 자료 보기
            </a>
          </div>
        )}
      </div>

      {/* 소명 제출 — 반려됐을 때 신고자에게만 표시 */}
      {canAppeal && (
        <div style={{
          background: '#fff',
          border: '1.5px solid #fca5a5',
          borderRadius: '12px',
          padding: '24px 28px',
          marginBottom: '20px',
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '6px' }}>
            <span style={{ fontSize: '20px' }}>📋</span>
            <h2 style={{ fontSize: '15px', fontWeight: 700, color: 'var(--fg)', margin: 0 }}>소명 제출</h2>
          </div>
          <p style={{ fontSize: '13px', color: 'var(--fg-muted)', marginBottom: '20px' }}>
            분쟁이 반려되었습니다. 추가 소명 자료를 제출하면 관리자가 재검토합니다.
          </p>

          {appealSuccess ? (
            <div style={{
              padding: '16px',
              background: '#f0fdf4',
              border: '1px solid #86efac',
              borderRadius: '10px',
              fontSize: '14px',
              color: '#15803d',
              textAlign: 'center',
            }}>
              ✅ 소명이 정상 접수되었습니다. 관리자 검토 후 결과를 안내드립니다.
            </div>
          ) : (
            <form onSubmit={handleAppealSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
              <div>
                <label style={{ display: 'block', fontSize: '14px', fontWeight: 600, color: 'var(--fg)', marginBottom: '8px' }}>
                  소명 내용 <span style={{ color: 'var(--error)' }}>*</span>
                </label>
                <textarea
                  value={appealContent}
                  onChange={(e) => setAppealContent(e.target.value)}
                  placeholder="소명 내용을 구체적으로 작성해주세요. (최소 10자)"
                  rows={5}
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
                  }}
                  onFocus={(e) => { e.target.style.borderColor = 'var(--brand)' }}
                  onBlur={(e) => { e.target.style.borderColor = 'var(--border)' }}
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '14px', fontWeight: 600, color: 'var(--fg)', marginBottom: '8px' }}>
                  첨부 자료 URL <span style={{ fontSize: '12px', fontWeight: 400, color: 'var(--fg-muted)' }}>(선택)</span>
                </label>
                <input
                  type="url"
                  value={appealFileUrl}
                  onChange={(e) => setAppealFileUrl(e.target.value)}
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
                  }}
                  onFocus={(e) => { e.target.style.borderColor = 'var(--brand)' }}
                  onBlur={(e) => { e.target.style.borderColor = 'var(--border)' }}
                />
              </div>

              {appealError && (
                <div style={{
                  padding: '12px 14px',
                  background: '#fff5f5',
                  border: '1px solid #fecaca',
                  borderRadius: '10px',
                  fontSize: '14px',
                  color: 'var(--error)',
                }}>
                  {appealError}
                </div>
              )}

              <button
                type="submit"
                disabled={appealMutation.isPending}
                style={{
                  height: '48px', fontSize: '15px', fontWeight: 700,
                  background: appealMutation.isPending ? 'var(--brand-tint)' : 'var(--brand)',
                  color: appealMutation.isPending ? 'var(--brand)' : '#fff',
                  border: 'none', borderRadius: '10px',
                  cursor: appealMutation.isPending ? 'not-allowed' : 'pointer',
                  fontFamily: 'inherit', transition: 'background 0.2s',
                }}
              >
                {appealMutation.isPending ? '제출 중...' : '소명 제출'}
              </button>
            </form>
          )}
        </div>
      )}

      {/* 처리 완료 상태 안내 */}
      {dispute.status === 'RESOLVED' && (
        <div style={{
          padding: '16px 20px',
          background: '#f0fdf4',
          border: '1px solid #86efac',
          borderRadius: '12px',
          fontSize: '14px',
          color: '#15803d',
          display: 'flex',
          alignItems: 'center',
          gap: '10px',
        }}>
          <span style={{ fontSize: '20px' }}>✅</span>
          <span>분쟁이 해결 처리되었습니다. 관련 조치가 완료되었습니다.</span>
        </div>
      )}

      {dispute.status === 'REJECTED' && !canAppeal && (
        <div style={{
          padding: '16px 20px',
          background: '#fff5f5',
          border: '1px solid #fecaca',
          borderRadius: '12px',
          fontSize: '14px',
          color: '#991b1b',
          display: 'flex',
          alignItems: 'center',
          gap: '10px',
        }}>
          <span style={{ fontSize: '20px' }}>❌</span>
          <span>분쟁 신고가 반려되었습니다.</span>
        </div>
      )}
    </div>
  )
}

export default function DisputeDetailPage({ params }: { params: Promise<{ disputeId: string }> }) {
  const { disputeId } = use(params)
  return (
    <ProtectedRoute>
      <DisputeDetail disputeId={Number(disputeId)} />
    </ProtectedRoute>
  )
}
