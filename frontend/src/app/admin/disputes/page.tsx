'use client'

import { useState } from 'react'
import Link from 'next/link'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient, unwrap } from '@/api/client'
import { Badge } from '@/components/ui/Badge'
import { LoadingSpinner } from '@/components/ui/LoadingSpinner'
import { DISPUTE_CATEGORY_LABELS, type DisputeCategory } from '@/api/disputes'
import { DISPUTE_STATUS_LABELS, type DisputeStatus } from '@/types/enums'
import type { ApiResponse, PageResponse } from '@/types/api'
import { formatDateTime } from '@/utils/format'

interface AdminDisputeItem {
  id: number
  targetType: string
  targetId: number
  category: string
  title: string
  status: DisputeStatus
  reporterId: number
  reporterNickname: string
  reportedId: number
  reportedNickname: string
  createdAt: string
  ideaStatus?: string | null
}

interface DisputeStats {
  total: number
  received: number
  pending: number
  resolved: number
  rejected: number
}

const adminDisputeApi = {
  getStats: () =>
    unwrap(apiClient.get<ApiResponse<DisputeStats>>('/admin/disputes/stats')),

  getList: (status: DisputeStatus | '', category: DisputeCategory | '', page: number) =>
    unwrap(
      apiClient.get<ApiResponse<PageResponse<AdminDisputeItem>>>('/admin/disputes', {
        params: {
          ...(status ? { status } : {}),
          ...(category ? { category } : {}),
          page,
          size: 20,
          sort: 'createdAt,desc',
        },
      }),
    ),

  updateStatus: (disputeId: number, status: DisputeStatus) =>
    unwrap(apiClient.patch<ApiResponse<void>>(`/admin/disputes/${disputeId}/status`, { status })),

  forceRefund: (disputeId: number, paymentId: number) =>
    unwrap(apiClient.post<ApiResponse<void>>(`/admin/disputes/${disputeId}/force-refund`, { paymentId })),
}

const adminIdeaApi = {
  suspend: (ideaId: number) =>
    unwrap(apiClient.patch<ApiResponse<void>>(`/admin/ideas/${ideaId}/suspend`)),
  restore: (ideaId: number) =>
    unwrap(apiClient.patch<ApiResponse<void>>(`/admin/ideas/${ideaId}/restore`)),
}

const STATUS_VARIANT: Record<DisputeStatus, 'gray' | 'orange' | 'green' | 'red'> = {
  RECEIVED: 'gray',
  PENDING:  'orange',
  RESOLVED: 'green',
  REJECTED: 'red',
}

const STATS_CONFIG: { key: keyof DisputeStats; statusValue: DisputeStatus | ''; label: string; color: string; bg: string }[] = [
  { key: 'total',    statusValue: '',         label: '전체',   color: 'var(--fg)',  bg: 'var(--bg-alt)' },
  { key: 'received', statusValue: 'RECEIVED', label: '접수됨', color: '#6b7280',   bg: '#f9fafb' },
  { key: 'pending',  statusValue: 'PENDING',  label: '검토중', color: '#d97706',   bg: '#fffbeb' },
  { key: 'resolved', statusValue: 'RESOLVED', label: '해결됨', color: '#059669',   bg: '#f0fdf4' },
  { key: 'rejected', statusValue: 'REJECTED', label: '반려됨', color: '#dc2626',   bg: '#fff5f5' },
]

const CATEGORY_TABS: { value: DisputeCategory | ''; label: string }[] = [
  { value: '', label: '전체' },
  ...Object.entries(DISPUTE_CATEGORY_LABELS).map(([value, label]) => ({
    value: value as DisputeCategory,
    label,
  })),
]

function ResolveConfirmModal({
  disputeId,
  disputeTitle,
  targetType,
  onConfirm,
  onClose,
  isPending,
}: {
  disputeId: number
  disputeTitle: string
  targetType: string
  onConfirm: () => void
  onClose: () => void
  isPending: boolean
}) {
  return (
    <div style={{
      position: 'fixed', inset: 0, zIndex: 1000,
      background: 'rgba(0,0,0,0.45)',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      padding: '24px',
    }}>
      <div style={{
        background: '#fff', borderRadius: '16px', padding: '28px 32px',
        width: '100%', maxWidth: '460px',
        boxShadow: '0 8px 40px rgba(0,0,0,0.18)',
        display: 'flex', flexDirection: 'column', gap: '16px',
      }}>
        <h2 style={{ fontSize: '18px', fontWeight: 800, color: 'var(--fg)', margin: 0 }}>
          ⚠️ 신고 승인 확인
        </h2>
        <p style={{ fontSize: '14px', color: 'var(--fg-muted)', margin: 0 }}>
          분쟁 <strong style={{ color: 'var(--fg)' }}>#{disputeId} — {disputeTitle}</strong>을 승인하시겠습니까?
        </p>

        {targetType === 'IDEA' && (
          <div style={{
            padding: '14px 16px', borderRadius: '10px',
            background: '#fef2f2', border: '1px solid #fecaca',
            display: 'flex', flexDirection: 'column', gap: '8px',
          }}>
            <div style={{ fontSize: '13px', fontWeight: 700, color: '#dc2626' }}>
              승인 시 다음 작업이 즉시 실행됩니다
            </div>
            <ul style={{ margin: 0, paddingLeft: '18px', fontSize: '13px', color: '#b91c1c', lineHeight: 1.7 }}>
              <li>아이디어가 강제 취소됩니다</li>
              <li>전체 후원금이 환불 처리됩니다</li>
            </ul>
          </div>
        )}

        <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end', marginTop: '4px' }}>
          <button
            onClick={onClose}
            disabled={isPending}
            style={{
              padding: '9px 20px', borderRadius: '8px',
              border: '1.5px solid var(--border)', background: '#fff',
              color: 'var(--fg-muted)', fontWeight: 600, fontSize: '14px',
              cursor: isPending ? 'not-allowed' : 'pointer',
              opacity: isPending ? 0.6 : 1,
            }}
          >
            취소
          </button>
          <button
            onClick={onConfirm}
            disabled={isPending}
            style={{
              padding: '9px 20px', borderRadius: '8px',
              border: 'none',
              background: isPending ? '#86efac' : '#059669',
              color: '#fff', fontWeight: 700, fontSize: '14px',
              cursor: isPending ? 'not-allowed' : 'pointer',
            }}
          >
            {isPending ? '처리 중...' : '승인 확정'}
          </button>
        </div>
      </div>
    </div>
  )
}

function ForceRefundModal({
  disputeId,
  disputeTitle,
  onConfirm,
  onClose,
  isPending,
}: {
  disputeId: number
  disputeTitle: string
  onConfirm: (paymentId: number) => void
  onClose: () => void
  isPending: boolean
}) {
  const [paymentIdInput, setPaymentIdInput] = useState('')
  const parsedPaymentId = parseInt(paymentIdInput, 10)
  const isValid = !isNaN(parsedPaymentId) && parsedPaymentId > 0 && paymentIdInput.trim() !== ''

  return (
    <div style={{
      position: 'fixed', inset: 0, zIndex: 1000,
      background: 'rgba(0,0,0,0.45)',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      padding: '24px',
    }}>
      <div style={{
        background: '#fff', borderRadius: '16px', padding: '28px 32px',
        width: '100%', maxWidth: '440px',
        boxShadow: '0 8px 40px rgba(0,0,0,0.18)',
      }}>
        <h2 style={{ fontSize: '18px', fontWeight: 800, color: 'var(--fg)', marginBottom: '6px' }}>
          💸 강제 환불
        </h2>
        <p style={{ fontSize: '13px', color: 'var(--fg-muted)', marginBottom: '20px' }}>
          분쟁 <strong>#{disputeId}</strong> — {disputeTitle}
        </p>

        <label style={{ display: 'block', fontSize: '13px', fontWeight: 600, color: 'var(--fg)', marginBottom: '6px' }}>
          결제 ID (paymentId)
        </label>
        <input
          type="number"
          min={1}
          value={paymentIdInput}
          onChange={(e) => setPaymentIdInput(e.target.value)}
          placeholder="환불할 결제 ID를 입력하세요"
          style={{
            width: '100%', padding: '10px 14px', borderRadius: '8px',
            border: '1.5px solid var(--border)', fontSize: '14px',
            color: 'var(--fg)', outline: 'none', boxSizing: 'border-box',
            marginBottom: '20px',
          }}
        />

        <div style={{ display: 'flex', gap: '10px', justifyContent: 'flex-end' }}>
          <button
            onClick={onClose}
            disabled={isPending}
            style={{
              padding: '9px 20px', borderRadius: '8px',
              border: '1.5px solid var(--border)', background: '#fff',
              color: 'var(--fg-muted)', fontWeight: 600, fontSize: '14px',
              cursor: isPending ? 'not-allowed' : 'pointer',
              opacity: isPending ? 0.6 : 1,
            }}
          >
            취소
          </button>
          <button
            onClick={() => isValid && onConfirm(parsedPaymentId)}
            disabled={!isValid || isPending}
            style={{
              padding: '9px 20px', borderRadius: '8px',
              border: 'none', background: isValid && !isPending ? '#dc2626' : '#fca5a5',
              color: '#fff', fontWeight: 700, fontSize: '14px',
              cursor: isValid && !isPending ? 'pointer' : 'not-allowed',
            }}
          >
            강제 환불 실행
          </button>
        </div>
      </div>
    </div>
  )
}

function StatusActionButtons({
  dispute,
  onResolve,
  onAction,
  onForceRefund,
  onSuspendIdea,
  onRestoreIdea,
  disabled,
}: {
  dispute: AdminDisputeItem
  onResolve: () => void
  onAction: (status: DisputeStatus, label: string) => void
  onForceRefund: () => void
  onSuspendIdea: () => void
  onRestoreIdea: () => void
  disabled: boolean
}) {
  const btnBase: React.CSSProperties = {
    padding: '7px 14px', borderRadius: '8px', border: 'none',
    fontWeight: 700, fontSize: '13px', cursor: disabled ? 'not-allowed' : 'pointer',
    opacity: disabled ? 0.6 : 1,
  }

  // RECEIVED: 검토 시작만 가능
  if (dispute.status === 'RECEIVED') {
    return (
      <button
        onClick={() => onAction('PENDING', '검토 시작')}
        disabled={disabled}
        style={{ ...btnBase, background: 'var(--brand)', color: '#fff' }}
      >
        🔍 검토 시작
      </button>
    )
  }

  // PENDING: 승인/반려/소명 재요청 + 아이디어면 프로젝트 중단
  if (dispute.status === 'PENDING') {
    return (
      <div style={{ display: 'flex', gap: '8px', flexWrap: 'wrap' }}>
        <button
          onClick={onResolve}
          disabled={disabled}
          style={{ ...btnBase, background: '#059669', color: '#fff' }}
        >
          ✅ 승인
        </button>
        <button
          onClick={() => onAction('REJECTED', '신고 기각 (반려)')}
          disabled={disabled}
          style={{ ...btnBase, background: '#fff', border: '1.5px solid #fca5a5', color: '#dc2626' }}
        >
          ❌ 반려
        </button>
        <button
          onClick={() => onAction('RECEIVED', '소명 재요청')}
          disabled={disabled}
          style={{ ...btnBase, background: '#f59e0b', color: '#fff' }}
        >
          🔄 소명 재요청
        </button>
        {dispute.targetType === 'IDEA' && (
          dispute.ideaStatus === 'SUSPENDED' ? (
            <button
              onClick={onRestoreIdea}
              disabled={disabled}
              style={{ ...btnBase, background: '#fff', border: '1.5px solid #6ee7b7', color: '#059669' }}
            >
              ▶ 프로젝트 재시작
            </button>
          ) : (
            <button
              onClick={onSuspendIdea}
              disabled={disabled}
              style={{ ...btnBase, background: '#fff', border: '1.5px solid #d1d5db', color: '#6b7280' }}
            >
              ⏸ 프로젝트 중단
            </button>
          )
        )}
      </div>
    )
  }

  // RESOLVED: 아이디어면 강제 환불 가능
  if (dispute.status === 'RESOLVED' && dispute.targetType === 'IDEA') {
    return (
      <button
        onClick={onForceRefund}
        disabled={disabled}
        style={{ ...btnBase, background: '#fff', border: '1.5px solid #fca5a5', color: '#dc2626' }}
      >
        💸 강제 환불
      </button>
    )
  }

  return null
}

export default function AdminDisputesPage() {
  const queryClient = useQueryClient()
  const [statusFilter, setStatusFilter] = useState<DisputeStatus | ''>('')
  const [categoryFilter, setCategoryFilter] = useState<DisputeCategory | ''>('')
  const [page, setPage] = useState(0)
  const [forceRefundTarget, setForceRefundTarget] = useState<{ disputeId: number; disputeTitle: string } | null>(null)
  const [resolveTarget, setResolveTarget] = useState<{
    disputeId: number; disputeTitle: string; targetType: string
  } | null>(null)

  const { data: stats } = useQuery({
    queryKey: ['admin', 'disputes', 'stats'],
    queryFn: adminDisputeApi.getStats,
    staleTime: 30_000,
  })

  const { data, isLoading } = useQuery({
    queryKey: ['admin', 'disputes', statusFilter, categoryFilter, page],
    queryFn: () => adminDisputeApi.getList(statusFilter, categoryFilter, page),
  })

  const updateMutation = useMutation({
    mutationFn: ({ id, status }: { id: number; status: DisputeStatus }) =>
      adminDisputeApi.updateStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'disputes'] })
    },
    onError: () => alert('처리 중 오류가 발생했습니다.'),
  })

  const resolveMutation = useMutation({
    mutationFn: (disputeId: number) =>
      adminDisputeApi.updateStatus(disputeId, 'RESOLVED'),
    onSuccess: () => {
      setResolveTarget(null)
      queryClient.invalidateQueries({ queryKey: ['admin', 'disputes'] })
    },
    onError: () => alert('승인 처리 중 오류가 발생했습니다.'),
  })

  const suspendMutation = useMutation({
    mutationFn: (ideaId: number) => adminIdeaApi.suspend(ideaId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'disputes'] })
      alert('프로젝트가 중단되었습니다.')
    },
    onError: () => alert('프로젝트 중단 처리 중 오류가 발생했습니다.'),
  })

  const restoreMutation = useMutation({
    mutationFn: (ideaId: number) => adminIdeaApi.restore(ideaId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin', 'disputes'] })
      alert('프로젝트가 재시작되었습니다.')
    },
    onError: () => alert('프로젝트 재시작 처리 중 오류가 발생했습니다.'),
  })

  const forceRefundMutation = useMutation({
    mutationFn: ({ disputeId, paymentId }: { disputeId: number; paymentId: number }) =>
      adminDisputeApi.forceRefund(disputeId, paymentId),
    onSuccess: () => {
      setForceRefundTarget(null)
      queryClient.invalidateQueries({ queryKey: ['admin', 'disputes'] })
    },
    onError: () => alert('강제 환불 처리 중 오류가 발생했습니다.'),
  })

  const items = data?.content ?? []
  const totalPages = data?.totalPages ?? 1
  const totalElements = data?.totalElements ?? 0
  const isMutating = updateMutation.isPending || resolveMutation.isPending || suspendMutation.isPending || restoreMutation.isPending || forceRefundMutation.isPending

  const handleStatusChange = (value: DisputeStatus | '') => {
    setStatusFilter(value)
    setPage(0)
  }

  const handleCategoryChange = (value: DisputeCategory | '') => {
    setCategoryFilter(value)
    setPage(0)
  }

  return (
    <>
      {/* 헤더 */}
      <div style={{ marginBottom: '24px' }}>
        <h1 style={{ fontSize: '22px', fontWeight: 800, color: 'var(--fg)', marginBottom: '4px' }}>
          분쟁 관리
        </h1>
        <p style={{ fontSize: '14px', color: 'var(--fg-muted)' }}>
          접수된 분쟁 신고를 검토하고 처리합니다.
        </p>
      </div>

      {/* 통계 카드 */}
      {stats && (
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: '12px', marginBottom: '24px' }}>
          {STATS_CONFIG.map((s) => {
            const isActive = statusFilter === s.statusValue
            return (
              <button
                key={s.key}
                onClick={() => handleStatusChange(s.statusValue)}
                style={{
                  background: s.bg, border: `2px solid ${isActive ? s.color : 'var(--border)'}`,
                  borderRadius: '10px', padding: '14px 16px', textAlign: 'center',
                  cursor: 'pointer', transition: 'all 0.15s',
                  boxShadow: isActive ? `0 0 0 3px ${s.color}22` : 'none',
                  outline: 'none',
                }}
              >
                <div style={{ fontSize: '24px', fontWeight: 800, color: s.color }}>{stats[s.key]}</div>
                <div style={{ fontSize: '12px', color: isActive ? s.color : 'var(--fg-muted)', fontWeight: isActive ? 700 : 400, marginTop: '2px' }}>
                  {s.label}
                </div>
              </button>
            )
          })}
        </div>
      )}

      {/* 카테고리 필터 */}
      <div style={{ display: 'flex', gap: '6px', marginBottom: '20px', flexWrap: 'wrap' }}>
        {CATEGORY_TABS.map((tab) => {
          const isActive = categoryFilter === tab.value
          return (
            <button
              key={tab.value}
              onClick={() => handleCategoryChange(tab.value)}
              style={{
                flexShrink: 0, padding: '6px 14px', borderRadius: '99px',
                border: `1.5px solid ${isActive ? 'var(--brand)' : 'var(--border)'}`,
                background: isActive ? 'var(--brand-tint)' : '#fff',
                color: isActive ? 'var(--brand-dark)' : 'var(--fg-muted)',
                fontWeight: isActive ? 700 : 500, fontSize: '13px',
                cursor: 'pointer', transition: 'all 0.15s', whiteSpace: 'nowrap',
              }}
            >
              {tab.label}
            </button>
          )
        })}
      </div>

      {/* 목록 */}
      {isLoading ? (
        <LoadingSpinner />
      ) : items.length === 0 ? (
        <div style={{
          padding: '60px', textAlign: 'center',
          background: 'var(--bg-alt)', borderRadius: '12px',
          color: 'var(--fg-muted)', fontSize: '15px',
        }}>
          해당 분쟁이 없습니다.
        </div>
      ) : (
        <>
          <div style={{ fontSize: '13px', color: 'var(--fg-muted)', marginBottom: '12px' }}>
            총 {totalElements}건
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {items.map((d) => (
              <div key={d.id} style={{
                background: '#fff', border: '1px solid var(--border)',
                borderRadius: '12px', padding: '18px 20px',
                display: 'flex', flexDirection: 'column', gap: '12px',
              }}>
                {/* 제목 행 */}
                <div style={{ display: 'flex', alignItems: 'flex-start', gap: '12px', justifyContent: 'space-between' }}>
                  <div style={{ flex: 1, minWidth: 0 }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px', flexWrap: 'wrap' }}>
                      <Link
                        href={`/disputes/${d.id}`}
                        target="_blank"
                        style={{ fontSize: '15px', fontWeight: 700, color: 'var(--fg)', textDecoration: 'none' }}
                        onMouseEnter={(e) => (e.currentTarget.style.color = 'var(--brand)')}
                        onMouseLeave={(e) => (e.currentTarget.style.color = 'var(--fg)')}
                      >
                        #{d.id} {d.title} ↗
                      </Link>
                      <Badge variant={STATUS_VARIANT[d.status]}>
                        {DISPUTE_STATUS_LABELS[d.status]}
                      </Badge>
                    </div>
                    <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap', fontSize: '13px', color: 'var(--fg-muted)' }}>
                      <span style={{
                        padding: '2px 8px', borderRadius: '99px', fontSize: '12px',
                        background: 'var(--brand-tint)', color: 'var(--brand-dark)', fontWeight: 600,
                      }}>
                        {DISPUTE_CATEGORY_LABELS[d.category as DisputeCategory] ?? d.category}
                      </span>
                      {d.targetType === 'IDEA' && (
                        <Link
                          href={`/ideas/${d.targetId}`}
                          target="_blank"
                          style={{ color: 'var(--fg-muted)', textDecoration: 'none', fontWeight: 500 }}
                        >
                          아이디어 #{d.targetId} →
                        </Link>
                      )}
                      <span>{formatDateTime(d.createdAt)}</span>
                    </div>
                  </div>
                </div>

                {/* 신고자/피신고자 + 액션 */}
                <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '12px', flexWrap: 'wrap' }}>
                  <div style={{ display: 'flex', gap: '16px', fontSize: '13px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <span style={{ color: 'var(--fg-muted)' }}>신고자</span>
                      <span style={{ fontWeight: 600, color: 'var(--fg)' }}>{d.reporterNickname}</span>
                    </div>
                    <span style={{ color: 'var(--border)' }}>→</span>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '6px' }}>
                      <span style={{ color: 'var(--fg-muted)' }}>피신고자</span>
                      <span style={{ fontWeight: 600, color: '#dc2626' }}>{d.reportedNickname}</span>
                    </div>
                  </div>
                  <StatusActionButtons
                    dispute={d}
                    onResolve={() => setResolveTarget({ disputeId: d.id, disputeTitle: d.title, targetType: d.targetType })}
                    onAction={(status, label) => {
                      if (confirm(`분쟁 #${d.id}을 [${label}] 처리하시겠습니까?`)) {
                        updateMutation.mutate({ id: d.id, status })
                      }
                    }}
                    onForceRefund={() => setForceRefundTarget({ disputeId: d.id, disputeTitle: d.title })}
                    onSuspendIdea={() => {
                      if (confirm(`아이디어 #${d.targetId}를 일시 중단하시겠습니까?`)) {
                        suspendMutation.mutate(d.targetId)
                      }
                    }}
                    onRestoreIdea={() => {
                      if (confirm(`아이디어 #${d.targetId}를 다시 재시작하시겠습니까?`)) {
                        restoreMutation.mutate(d.targetId)
                      }
                    }}
                    disabled={isMutating}
                  />
                </div>
              </div>
            ))}
          </div>
        </>
      )}

      {/* 페이지네이션 */}
      {totalPages > 1 && (
        <div style={{ display: 'flex', justifyContent: 'center', gap: '8px', marginTop: '24px' }}>
          <button
            onClick={() => setPage((p) => Math.max(0, p - 1))}
            disabled={page === 0}
            style={{
              padding: '8px 16px', borderRadius: '8px',
              border: '1.5px solid var(--border)', background: '#fff',
              color: page === 0 ? 'var(--fg-muted)' : 'var(--fg)',
              fontSize: '14px', cursor: page === 0 ? 'not-allowed' : 'pointer',
            }}
          >
            이전
          </button>
          <span style={{ padding: '8px 16px', fontSize: '14px', color: 'var(--fg-muted)' }}>
            {page + 1} / {totalPages}
          </span>
          <button
            onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))}
            disabled={page >= totalPages - 1}
            style={{
              padding: '8px 16px', borderRadius: '8px',
              border: '1.5px solid var(--border)', background: '#fff',
              color: page >= totalPages - 1 ? 'var(--fg-muted)' : 'var(--fg)',
              fontSize: '14px', cursor: page >= totalPages - 1 ? 'not-allowed' : 'pointer',
            }}
          >
            다음
          </button>
        </div>
      )}

      {/* 승인 확인 모달 */}
      {resolveTarget && (
        <ResolveConfirmModal
          disputeId={resolveTarget.disputeId}
          disputeTitle={resolveTarget.disputeTitle}
          targetType={resolveTarget.targetType}
          onConfirm={() => resolveMutation.mutate(resolveTarget.disputeId)}
          onClose={() => setResolveTarget(null)}
          isPending={resolveMutation.isPending}
        />
      )}

      {/* 강제 환불 모달 */}
      {forceRefundTarget && (
        <ForceRefundModal
          disputeId={forceRefundTarget.disputeId}
          disputeTitle={forceRefundTarget.disputeTitle}
          onConfirm={(paymentId) =>
            forceRefundMutation.mutate({ disputeId: forceRefundTarget.disputeId, paymentId })
          }
          onClose={() => setForceRefundTarget(null)}
          isPending={forceRefundMutation.isPending}
        />
      )}
    </>
  )
}
