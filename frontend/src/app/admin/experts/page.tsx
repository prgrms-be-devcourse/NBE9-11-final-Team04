'use client'

import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient, unwrap } from '@/api/client'
import { Badge } from '@/components/ui/Badge'
import { LoadingSpinner } from '@/components/ui/LoadingSpinner'
import type { ApiResponse, PageResponse } from '@/types/api'
import { formatDateTime } from '@/utils/format'

type ExpertStatusType = 'ACTIVE' | 'SUSPENDED' | 'PENDING_VERIFICATION' | 'DEMOTED'

interface AdminExpertResponse {
  expertProfileId: number
  userId: number
  name: string
  email: string
  qualificationType: string
  qualificationNumber: string
  fileUrl: string | null
  status: ExpertStatusType
  suspendedAt: string | null
  appealCount: number
}

const adminExpertsApi = {
  getExperts: (status: ExpertStatusType | '', page: number) =>
    unwrap(
      apiClient.get<ApiResponse<PageResponse<AdminExpertResponse>>>('/admin/experts', {
        params: { ...(status ? { status } : {}), page, size: 20 },
      }),
    ),
  restore: (expertProfileId: number) =>
    unwrap(apiClient.post<ApiResponse<void>>(`/admin/experts/${expertProfileId}/restore`)),
  demote: (expertProfileId: number) =>
    unwrap(apiClient.post<ApiResponse<void>>(`/admin/experts/${expertProfileId}/demote`)),
  approve: (expertProfileId: number) =>
    unwrap(apiClient.post<ApiResponse<void>>(`/admin/experts/${expertProfileId}/verify`)),
  reject: (expertProfileId: number) =>
    unwrap(apiClient.post<ApiResponse<void>>(`/admin/experts/${expertProfileId}/reject`)),
}

const STATUS_BADGE: Record<ExpertStatusType, { variant: 'green' | 'orange' | 'red' | 'gray'; label: string }> = {
  ACTIVE:               { variant: 'green',  label: '정상' },
  SUSPENDED:            { variant: 'red',    label: '격리' },
  PENDING_VERIFICATION: { variant: 'orange', label: '검증 보류' },
  DEMOTED:              { variant: 'gray',   label: '강등됨' },
}

const STATUS_TABS: { value: ExpertStatusType | ''; label: string }[] = [
  { value: '',                    label: '전체' },
  { value: 'ACTIVE',              label: '정상' },
  { value: 'SUSPENDED',           label: '격리' },
  { value: 'PENDING_VERIFICATION', label: '검증 보류' },
  { value: 'DEMOTED',             label: '강등됨' },
]

export default function AdminExpertsPage() {
  const queryClient = useQueryClient()
  const [statusFilter, setStatusFilter] = useState<ExpertStatusType | ''>('')
  const [page, setPage] = useState(0)

  const { data, isLoading } = useQuery({
    queryKey: ['admin', 'experts', statusFilter, page],
    queryFn: () => adminExpertsApi.getExperts(statusFilter, page),
  })

  const restoreMutation = useMutation({
    mutationFn: (expertProfileId: number) => adminExpertsApi.restore(expertProfileId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'experts'] }),
    onError: () => alert('복구 처리 중 오류가 발생했습니다.'),
  })

  const demoteMutation = useMutation({
    mutationFn: (expertProfileId: number) => adminExpertsApi.demote(expertProfileId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'experts'] }),
    onError: () => alert('강등 처리 중 오류가 발생했습니다.'),
  })

  const approveMutation = useMutation({
    mutationFn: (expertProfileId: number) => adminExpertsApi.approve(expertProfileId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'experts'] }),
    onError: () => alert('승인 처리 중 오류가 발생했습니다.'),
  })

  const rejectMutation = useMutation({
    mutationFn: (expertProfileId: number) => adminExpertsApi.reject(expertProfileId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'experts'] }),
    onError: () => alert('거절 처리 중 오류가 발생했습니다.'),
  })

  const experts = data?.content ?? []
  const totalPages = data?.totalPages ?? 1
  const totalElements = data?.totalElements ?? 0
  const isMutating =
    restoreMutation.isPending ||
    demoteMutation.isPending ||
    approveMutation.isPending ||
    rejectMutation.isPending

  const handleStatusChange = (value: ExpertStatusType | '') => {
    setStatusFilter(value)
    setPage(0)
  }

  return (
    <>
      <div style={{ marginBottom: '20px' }}>
        <h1 style={{ fontSize: '22px', fontWeight: 800, color: 'var(--fg)', marginBottom: '4px' }}>
          전문가 관리
        </h1>
        <p style={{ fontSize: '14px', color: 'var(--fg-muted)' }}>
          전문가 목록을 조회하고 상태를 관리합니다.
        </p>
      </div>

      {/* 상태 필터 */}
      <div style={{
        display: 'flex', gap: '4px', marginBottom: '20px',
        background: 'var(--bg-alt)', borderRadius: '10px', padding: '4px',
      }}>
        {STATUS_TABS.map((tab) => (
          <button
            key={tab.value}
            onClick={() => handleStatusChange(tab.value)}
            style={{
              flex: 1, padding: '8px 12px', borderRadius: '8px', border: 'none',
              background: statusFilter === tab.value ? '#fff' : 'transparent',
              boxShadow: statusFilter === tab.value ? '0 1px 3px rgba(0,0,0,0.08)' : 'none',
              fontWeight: statusFilter === tab.value ? 700 : 500,
              fontSize: '13px',
              color: statusFilter === tab.value ? 'var(--brand-dark)' : 'var(--fg-muted)',
              cursor: 'pointer', transition: 'all 0.15s',
            }}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* 목록 */}
      {isLoading ? (
        <LoadingSpinner />
      ) : experts.length === 0 ? (
        <div style={{
          padding: '60px', textAlign: 'center',
          background: 'var(--bg-alt)', borderRadius: '12px',
          color: 'var(--fg-muted)', fontSize: '15px',
        }}>
          해당하는 전문가가 없습니다.
        </div>
      ) : (
        <>
          <div style={{ fontSize: '13px', color: 'var(--fg-muted)', marginBottom: '12px' }}>
            총 {totalElements}명
          </div>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {experts.map((expert) => (
              <div key={expert.expertProfileId} style={{
                background: '#fff', border: '1px solid var(--border)',
                borderRadius: '12px', padding: '18px 20px',
                display: 'flex', alignItems: 'center', gap: '16px',
                flexWrap: 'wrap',
              }}>
                <div style={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column', gap: '6px' }}>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                    <span style={{ fontSize: '15px', fontWeight: 700, color: 'var(--fg)' }}>
                      {expert.name}
                    </span>
                    <Badge variant={STATUS_BADGE[expert.status].variant}>
                      {STATUS_BADGE[expert.status].label}
                    </Badge>
                    {expert.appealCount > 0 && (
                      <Badge variant="orange">소명 {expert.appealCount}건</Badge>
                    )}
                  </div>
                  <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap', fontSize: '13px', color: 'var(--fg-muted)' }}>
                    <span>{expert.email}</span>
                    <span style={{ color: 'var(--border)' }}>|</span>
                    <span>{expert.qualificationType} · {expert.qualificationNumber}</span>
                    {expert.status === 'SUSPENDED' && expert.suspendedAt && (
                      <>
                        <span style={{ color: 'var(--border)' }}>|</span>
                        <span>격리 {formatDateTime(expert.suspendedAt)}</span>
                      </>
                    )}
                    {expert.status === 'PENDING_VERIFICATION' && expert.fileUrl && (
                      <>
                        <span style={{ color: 'var(--border)' }}>|</span>
                        <a
                          href={expert.fileUrl}
                          target="_blank"
                          rel="noopener noreferrer"
                          style={{ color: 'var(--brand-dark)', fontWeight: 600, textDecoration: 'underline' }}
                        >
                          📎 자격증 파일 보기
                        </a>
                      </>
                    )}
                  </div>
                </div>

                {expert.status === 'SUSPENDED' && (
                  <div style={{ display: 'flex', gap: '8px', flexShrink: 0, flexWrap: 'wrap' }}>
                    <button
                      onClick={() => {
                        if (confirm(`"${expert.name}" 전문가를 복구하시겠습니까?`)) {
                          restoreMutation.mutate(expert.expertProfileId)
                        }
                      }}
                      disabled={isMutating}
                      style={{
                        padding: '8px 16px', borderRadius: '8px',
                        border: '1.5px solid #86efac', background: '#f0fdf4',
                        color: '#1a7a3f', fontWeight: 700, fontSize: '13px',
                        cursor: isMutating ? 'not-allowed' : 'pointer',
                        opacity: isMutating ? 0.7 : 1,
                      }}
                    >
                      ✅ 복구
                    </button>
                    <button
                      onClick={() => {
                        if (confirm('전문가 권한을 영구 박탈합니다. 계속할까요?')) {
                          demoteMutation.mutate(expert.expertProfileId)
                        }
                      }}
                      disabled={isMutating}
                      style={{
                        padding: '8px 16px', borderRadius: '8px',
                        border: '1.5px solid #fca5a5', background: '#fff',
                        color: '#dc2626', fontWeight: 700, fontSize: '13px',
                        cursor: isMutating ? 'not-allowed' : 'pointer',
                        opacity: isMutating ? 0.7 : 1,
                      }}
                    >
                      ⛔ 강등
                    </button>
                  </div>
                )}

                {expert.status === 'PENDING_VERIFICATION' && (
                  <div style={{ display: 'flex', gap: '8px', flexShrink: 0, flexWrap: 'wrap' }}>
                    <button
                      onClick={() => {
                        if (confirm(`"${expert.name}" 전문가의 국가자격증을 승인하시겠습니까?`)) {
                          approveMutation.mutate(expert.expertProfileId)
                        }
                      }}
                      disabled={isMutating}
                      style={{
                        padding: '8px 16px', borderRadius: '8px',
                        border: '1.5px solid #86efac', background: '#f0fdf4',
                        color: '#1a7a3f', fontWeight: 700, fontSize: '13px',
                        cursor: isMutating ? 'not-allowed' : 'pointer',
                        opacity: isMutating ? 0.7 : 1,
                      }}
                    >
                      ✅ 승인
                    </button>
                    <button
                      onClick={() => {
                        if (confirm(`"${expert.name}" 전문가의 국가자격증을 거절하시겠습니까? 프로필이 삭제되며 재신청이 가능합니다.`)) {
                          rejectMutation.mutate(expert.expertProfileId)
                        }
                      }}
                      disabled={isMutating}
                      style={{
                        padding: '8px 16px', borderRadius: '8px',
                        border: '1.5px solid #fca5a5', background: '#fff',
                        color: '#dc2626', fontWeight: 700, fontSize: '13px',
                        cursor: isMutating ? 'not-allowed' : 'pointer',
                        opacity: isMutating ? 0.7 : 1,
                      }}
                    >
                      ❌ 거절
                    </button>
                  </div>
                )}
              </div>
            ))}
          </div>
        </>
      )}

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
    </>
  )
}