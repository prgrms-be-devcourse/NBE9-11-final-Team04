'use client'

import { use, useRef, useState } from 'react'
import Link from 'next/link'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { disputesApi, DISPUTE_CATEGORY_LABELS, type DisputeCategory } from '@/api/disputes'
import { useAuthStore } from '@/store/authStore'
import { ProtectedRoute } from '@/components/layout/AppShell'
import { Badge } from '@/components/ui/Badge'
import { LoadingSpinner } from '@/components/ui/LoadingSpinner'
import { DISPUTE_STATUS_LABELS, type DisputeStatus } from '@/types/enums'
import { formatDateTime, getErrorMessage } from '@/utils/format'

const STATUS_STEPS: DisputeStatus[] = ['RECEIVED', 'PENDING', 'RESOLVED']

const STATUS_VARIANT: Record<DisputeStatus, 'blue' | 'green' | 'orange' | 'red' | 'gray'> = {
  RECEIVED: 'gray',
  PENDING:  'orange',
  RESOLVED: 'green',
  REJECTED: 'red',
}

function StatusTimeline({ status }: { status: DisputeStatus }) {
  const isRejected = status === 'REJECTED'
  const activeIndex = isRejected ? 1 : STATUS_STEPS.indexOf(status)

  return (
    <div style={{ display: 'flex', alignItems: 'flex-start', gap: '0', marginBottom: '32px' }}>
      {STATUS_STEPS.map((step, i) => {
        const isPast   = i < activeIndex
        const isActive = i === activeIndex
        const isFinal  = i === STATUS_STEPS.length - 1
        const isFinalRejected = isRejected && i === STATUS_STEPS.length - 1

        const circleColor = isPast || isActive
          ? (isFinalRejected ? '#ef4444' : 'var(--brand)')
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
                flexShrink: 0, zIndex: 1, position: 'relative',
              }}>
                {isPast ? '✓' : i + 1}
              </div>
              <span style={{
                fontSize: '12px', whiteSpace: 'nowrap',
                color: isActive ? (isFinalRejected ? '#ef4444' : 'var(--brand-dark)') : 'var(--fg-muted)',
                fontWeight: isActive ? 700 : 400,
              }}>
                {isFinalRejected ? '반려됨' : DISPUTE_STATUS_LABELS[step]}
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

function AppealSection({ disputeId }: { disputeId: number }) {
  const queryClient = useQueryClient()
  const fileInputRef = useRef<HTMLInputElement>(null)

  const [content, setContent] = useState('')
  const [file, setFile]       = useState<File | null>(null)
  const [error, setError]     = useState('')
  const [success, setSuccess] = useState(false)

  const mutation = useMutation({
    mutationFn: () => disputesApi.createAppeal(disputeId, content, file ?? undefined),
    onSuccess: () => {
      setSuccess(true)
      setContent('')
      setFile(null)
      queryClient.invalidateQueries({ queryKey: ['disputes', disputeId] })
    },
    onError: (err) => setError(getErrorMessage(err)),
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    if (content.trim().length < 10) { setError('소명 내용을 10자 이상 입력해주세요.'); return }
    mutation.mutate()
  }

  const inputStyle: React.CSSProperties = {
    width: '100%', border: '1.5px solid var(--border)', borderRadius: '10px',
    padding: '0 14px', fontSize: '14px', fontFamily: 'inherit',
    outline: 'none', color: 'var(--fg)', boxSizing: 'border-box',
    transition: 'border-color 0.2s', height: '44px', background: '#fff',
  }

  if (success) {
    return (
      <div style={{
        padding: '16px', background: '#f0fdf4', border: '1px solid #86efac',
        borderRadius: '10px', fontSize: '14px', color: '#15803d', textAlign: 'center',
      }}>
        ✅ 소명이 정상 접수되었습니다. 관리자 검토 후 결과를 안내드립니다.
      </div>
    )
  }

  return (
    <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
      <div>
        <label style={{ display: 'block', fontSize: '14px', fontWeight: 600, color: 'var(--fg)', marginBottom: '8px' }}>
          소명 내용 <span style={{ color: '#ef4444' }}>*</span>
        </label>
        <textarea
          value={content}
          onChange={(e) => setContent(e.target.value)}
          placeholder="소명 내용을 구체적으로 작성해주세요. (최소 10자)"
          rows={5}
          style={{
            width: '100%', border: '1.5px solid var(--border)', borderRadius: '10px',
            padding: '12px 14px', fontSize: '14px', fontFamily: 'inherit',
            lineHeight: 1.6, resize: 'vertical', outline: 'none',
            color: 'var(--fg)', boxSizing: 'border-box', transition: 'border-color 0.2s',
          }}
          onFocus={(e) => { e.target.style.borderColor = 'var(--brand)' }}
          onBlur={(e) => { e.target.style.borderColor = 'var(--border)' }}
        />
        <div style={{ fontSize: '12px', color: 'var(--fg-muted)', marginTop: '4px', textAlign: 'right' }}>
          {content.length}자
        </div>
      </div>

      <div>
        <label style={{ display: 'block', fontSize: '14px', fontWeight: 600, color: 'var(--fg)', marginBottom: '8px' }}>
          증거 파일 <span style={{ fontSize: '12px', fontWeight: 400, color: 'var(--fg-muted)' }}>(선택)</span>
        </label>
        <input ref={fileInputRef} type="file" style={{ display: 'none' }} onChange={(e) => setFile(e.target.files?.[0] ?? null)} />
        <div style={{ display: 'flex', gap: '8px', alignItems: 'center' }}>
          <input
            readOnly
            value={file?.name ?? ''}
            placeholder="파일을 선택해주세요"
            style={{ ...inputStyle, cursor: 'default', color: file ? 'var(--fg)' : 'var(--fg-muted)' }}
          />
          <button
            type="button"
            onClick={() => fileInputRef.current?.click()}
            style={{
              flexShrink: 0, height: '44px', padding: '0 16px',
              border: '1.5px solid var(--border)', borderRadius: '10px',
              background: '#fff', fontSize: '14px', cursor: 'pointer',
              fontFamily: 'inherit', color: 'var(--fg)', whiteSpace: 'nowrap',
            }}
          >
            파일 선택
          </button>
          {file && (
            <button
              type="button"
              onClick={() => setFile(null)}
              style={{
                flexShrink: 0, height: '44px', padding: '0 12px',
                border: '1.5px solid #fecaca', borderRadius: '10px',
                background: '#fff5f5', fontSize: '14px', cursor: 'pointer',
                fontFamily: 'inherit', color: '#ef4444',
              }}
            >
              ✕
            </button>
          )}
        </div>
      </div>

      {error && (
        <div style={{
          padding: '12px 14px', background: '#fff5f5', border: '1px solid #fecaca',
          borderRadius: '10px', fontSize: '14px', color: '#ef4444',
        }}>
          {error}
        </div>
      )}

      <button
        type="submit"
        disabled={mutation.isPending}
        style={{
          height: '48px', fontSize: '15px', fontWeight: 700,
          background: mutation.isPending ? 'var(--brand-tint)' : 'var(--brand)',
          color: mutation.isPending ? 'var(--brand)' : '#fff',
          border: 'none', borderRadius: '10px',
          cursor: mutation.isPending ? 'not-allowed' : 'pointer',
          fontFamily: 'inherit', transition: 'background 0.2s',
        }}
      >
        {mutation.isPending ? '제출 중...' : '소명 제출'}
      </button>
    </form>
  )
}

function DisputeDetail({ disputeId }: { disputeId: number }) {
  const user = useAuthStore((s) => s.user)

  const { data: dispute, isLoading, error } = useQuery({
    queryKey: ['disputes', disputeId],
    queryFn: () => disputesApi.getById(disputeId),
    enabled: !!disputeId,
  })

  if (isLoading) return <LoadingSpinner />

  if (error || !dispute) {
    return (
      <div style={{ padding: '80px 0', textAlign: 'center', color: 'var(--fg-muted)' }}>
        <div style={{ fontSize: '48px', marginBottom: '16px' }}>⚠️</div>
        분쟁 정보를 불러올 수 없습니다.
      </div>
    )
  }

  const isReporter = user?.id === dispute.reporterId
  const isReported = user?.id === dispute.reportedId
  const appealDeadline = dispute.createdAt
    ? new Date(new Date(dispute.createdAt).getTime() + 7 * 24 * 60 * 60 * 1000)
    : null
  const isWithinDeadline = appealDeadline ? appealDeadline > new Date() : false
  const appealLimitExceeded = dispute.appeal !== null && (dispute.appeal?.appealCount ?? 0) >= 3
  const canAppeal = isReported && dispute.status === 'RECEIVED' && isWithinDeadline && !appealLimitExceeded

  return (
    <div style={{ maxWidth: '680px', margin: '0 auto', padding: '48px 24px' }}>
      {/* 헤더 */}
      <div style={{ marginBottom: '28px' }}>
        <Link
          href={isReporter ? '/mypage/disputes' : '/mypage/disputes/received'}
          style={{ fontSize: '14px', color: 'var(--fg-muted)', textDecoration: 'none', display: 'inline-block', marginBottom: '16px' }}
        >
          ← 목록으로
        </Link>
        <div style={{ display: 'flex', alignItems: 'center', gap: '12px', flexWrap: 'wrap' }}>
          <h1 style={{ fontSize: '22px', fontWeight: 700, color: 'var(--fg)', margin: 0 }}>
            분쟁 #{dispute.id}
          </h1>
          <Badge variant={STATUS_VARIANT[dispute.status as DisputeStatus]}>
            {DISPUTE_STATUS_LABELS[dispute.status as DisputeStatus]}
          </Badge>
        </div>
        <p style={{ fontSize: '13px', color: 'var(--fg-muted)', marginTop: '6px' }}>
          접수일: {formatDateTime(dispute.createdAt)}
        </p>
      </div>

      {/* 상태 타임라인 */}
      <div style={{
        background: '#fff', border: '1px solid var(--border)',
        borderRadius: '12px', padding: '24px 28px 16px', marginBottom: '20px',
      }}>
        <h2 style={{ fontSize: '15px', fontWeight: 700, color: 'var(--fg)', marginBottom: '20px' }}>처리 현황</h2>
        <StatusTimeline status={dispute.status as DisputeStatus} />
      </div>

      {/* 신고 내용 */}
      <div style={{
        background: '#fff', border: '1px solid var(--border)',
        borderRadius: '12px', padding: '24px 28px',
        marginBottom: '20px', display: 'flex', flexDirection: 'column', gap: '18px',
      }}>
        <h2 style={{ fontSize: '15px', fontWeight: 700, color: 'var(--fg)', margin: 0 }}>신고 내용</h2>

        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
          <div style={{ background: 'var(--bg-alt)', borderRadius: '8px', padding: '12px 14px' }}>
            <div style={{ fontSize: '12px', color: 'var(--fg-muted)', marginBottom: '4px' }}>신고 유형</div>
            <div style={{ fontSize: '14px', fontWeight: 600, color: 'var(--fg)' }}>
              {DISPUTE_CATEGORY_LABELS[dispute.category as DisputeCategory]}
            </div>
          </div>
          <div style={{ background: 'var(--bg-alt)', borderRadius: '8px', padding: '12px 14px' }}>
            <div style={{ fontSize: '12px', color: 'var(--fg-muted)', marginBottom: '4px' }}>대상</div>
            {dispute.targetType === 'IDEA' ? (
              <Link
                href={`/ideas/${dispute.targetId}`}
                style={{ fontSize: '14px', fontWeight: 600, color: 'var(--brand-dark)', textDecoration: 'none' }}
              >
                아이디어 #{dispute.targetId} →
              </Link>
            ) : (
              <div style={{ fontSize: '14px', fontWeight: 600, color: 'var(--fg)' }}>
                {dispute.targetType} #{dispute.targetId}
              </div>
            )}
          </div>
          <div style={{ background: 'var(--bg-alt)', borderRadius: '8px', padding: '12px 14px' }}>
            <div style={{ fontSize: '12px', color: 'var(--fg-muted)', marginBottom: '4px' }}>내 역할</div>
            <div style={{ fontSize: '14px', fontWeight: 600, color: 'var(--fg)' }}>
              {isReporter ? '신고자' : isReported ? '피신고자' : '관련 당사자'}
            </div>
          </div>
        </div>

        <div>
          <div style={{ fontSize: '13px', fontWeight: 600, color: 'var(--fg-muted)', marginBottom: '8px' }}>신고 사유</div>
          <div style={{
            background: 'var(--bg-alt)', borderRadius: '8px', padding: '14px 16px',
            fontSize: '14px', color: 'var(--fg)', lineHeight: 1.7, whiteSpace: 'pre-wrap',
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
                fontSize: '14px', color: 'var(--brand-dark)', textDecoration: 'none',
                padding: '8px 14px', background: 'var(--brand-tint)',
                borderRadius: '8px', border: '1px solid var(--brand)',
              }}
            >
              🔗 증거 자료 보기
            </a>
          </div>
        )}
      </div>

      {/* 기존 소명 내용 */}
      {dispute.appeal && (
        <div style={{
          background: '#fff', border: '1px solid var(--border)',
          borderRadius: '12px', padding: '24px 28px', marginBottom: '20px',
          display: 'flex', flexDirection: 'column', gap: '14px',
        }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <h2 style={{ fontSize: '15px', fontWeight: 700, color: 'var(--fg)', margin: 0 }}>제출된 소명</h2>
            <span style={{ fontSize: '12px', color: 'var(--fg-muted)' }}>
              {formatDateTime(dispute.appeal.createdAt)}
            </span>
          </div>
          <div style={{
            background: 'var(--bg-alt)', borderRadius: '8px', padding: '14px 16px',
            fontSize: '14px', color: 'var(--fg)', lineHeight: 1.7, whiteSpace: 'pre-wrap',
          }}>
            {dispute.appeal.content}
          </div>
          {dispute.appeal.fileUrl && (
            <a
              href={dispute.appeal.fileUrl}
              target="_blank"
              rel="noopener noreferrer"
              style={{
                display: 'inline-flex', alignItems: 'center', gap: '6px',
                fontSize: '14px', color: 'var(--brand-dark)', textDecoration: 'none',
                padding: '8px 14px', background: 'var(--brand-tint)',
                borderRadius: '8px', border: '1px solid var(--brand)',
                alignSelf: 'flex-start',
              }}
            >
              📎 첨부 파일 보기
            </a>
          )}
        </div>
      )}

      {/* 소명 불가 안내 — 마감 또는 횟수 초과 */}
      {isReported && dispute.status === 'RECEIVED' && !canAppeal && (
        <div style={{
          padding: '16px 20px', background: '#fff5f5', border: '1px solid #fecaca',
          borderRadius: '12px', fontSize: '14px', color: '#991b1b',
          display: 'flex', alignItems: 'center', gap: '10px',
        }}>
          <span style={{ fontSize: '20px' }}>⛔</span>
          <span>
            {appealLimitExceeded
              ? '소명 횟수(3회)를 초과하여 더 이상 제출할 수 없습니다.'
              : '소명 접수 기간(7일)이 만료되었습니다.'}
          </span>
        </div>
      )}


      {/* 소명 제출 폼 — 피신고자, status=RECEIVED일 때만 */}
      {canAppeal && (
        <div style={{
          background: '#fff', border: '1.5px solid #fca5a5',
          borderRadius: '12px', padding: '24px 28px', marginBottom: '20px',
        }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '10px', marginBottom: '6px' }}>
            <span style={{ fontSize: '20px' }}>📋</span>
            <h2 style={{ fontSize: '15px', fontWeight: 700, color: 'var(--fg)', margin: 0 }}>
              {dispute.appeal ? '소명 재제출' : '소명 제출'}
            </h2>
          </div>
          {dispute.appeal ? (
            <div style={{
              padding: '10px 14px', background: '#fffbeb', border: '1px solid #fcd34d',
              borderRadius: '8px', fontSize: '13px', color: '#92400e', marginBottom: '20px',
              display: 'flex', alignItems: 'center', gap: '8px',
            }}>
              <span>⚠️</span>
              <span>관리자가 소명을 반려했습니다. 내용을 보완하여 다시 제출해 주세요. ({dispute.appeal.appealCount}/3회 사용)</span>
            </div>
          ) : (
            <p style={{ fontSize: '13px', color: 'var(--fg-muted)', marginBottom: '20px' }}>
              신고가 접수되었습니다.{appealDeadline && ` 소명 마감: ${appealDeadline.toLocaleDateString('ko-KR')} (7일 이내)`}
            </p>
          )}
          <AppealSection disputeId={disputeId} />
        </div>
      )}

      {/* 검토 중 안내 */}
      {dispute.status === 'PENDING' && (
        <div style={{
          padding: '16px 20px', background: '#fffbeb', border: '1px solid #fcd34d',
          borderRadius: '12px', fontSize: '14px', color: '#92400e',
          display: 'flex', alignItems: 'center', gap: '10px',
        }}>
          <span style={{ fontSize: '20px' }}>🔍</span>
          <span>관리자가 신고 내용을 검토하고 있습니다.</span>
        </div>
      )}

      {/* 해결됨 */}
      {dispute.status === 'RESOLVED' && (
        <div style={{
          padding: '16px 20px', background: '#f0fdf4', border: '1px solid #86efac',
          borderRadius: '12px', fontSize: '14px', color: '#15803d',
          display: 'flex', alignItems: 'center', gap: '10px',
        }}>
          <span style={{ fontSize: '20px' }}>✅</span>
          <span>분쟁이 해결 처리되었습니다. 관련 조치가 완료되었습니다.</span>
        </div>
      )}

      {/* 반려됨 */}
      {dispute.status === 'REJECTED' && (
        <div style={{
          padding: '16px 20px', background: '#fff5f5', border: '1px solid #fecaca',
          borderRadius: '12px', fontSize: '14px', color: '#991b1b',
          display: 'flex', alignItems: 'center', gap: '10px',
        }}>
          <span style={{ fontSize: '20px' }}>❌</span>
          <span>
            {isReporter ? '분쟁 신고가 반려되었습니다.' : '소명이 수용되어 신고가 반려 처리되었습니다.'}
          </span>
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
