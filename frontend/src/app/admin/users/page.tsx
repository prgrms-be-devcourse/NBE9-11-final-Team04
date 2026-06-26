'use client'

import { useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient, unwrap } from '@/api/client'
import { Badge } from '@/components/ui/Badge'
import { LoadingSpinner } from '@/components/ui/LoadingSpinner'
import type { ApiResponse, PageResponse } from '@/types/api'
import { formatDate } from '@/utils/format'

type UserRole = 'USER' | 'EXPERT' | 'ADMIN'
type UserStatus = 'ACTIVE' | 'SUSPENDED' | 'WITHDRAWN'

interface AdminUserResponse {
  id: number
  email: string
  name: string
  nickname: string
  role: UserRole
  status: UserStatus
  createdAt: string
}

const adminUsersApi = {
  getUsers: (status: UserStatus | '', role: UserRole | '', page: number) =>
    unwrap(
      apiClient.get<ApiResponse<PageResponse<AdminUserResponse>>>('/admin/users', {
        params: { ...(status ? { status } : {}), ...(role ? { role } : {}), page, size: 20 },
      }),
    ),
  updateStatus: (userId: number, status: 'ACTIVE' | 'SUSPENDED') =>
    unwrap(apiClient.patch<ApiResponse<void>>(`/admin/users/${userId}/status`, { status })),
  forceWithdraw: (userId: number) =>
    unwrap(apiClient.delete<ApiResponse<void>>(`/admin/users/${userId}`)),
}

const ROLE_VARIANT: Record<UserRole, 'gray' | 'green' | 'orange'> = {
  USER: 'gray',
  EXPERT: 'green',
  ADMIN: 'orange',
}

const ROLE_LABEL: Record<UserRole, string> = {
  USER: '일반',
  EXPERT: '전문가',
  ADMIN: '관리자',
}

const STATUS_VARIANT: Record<UserStatus, 'green' | 'orange' | 'red'> = {
  ACTIVE: 'green',
  SUSPENDED: 'orange',
  WITHDRAWN: 'red',
}

const STATUS_LABEL: Record<UserStatus, string> = {
  ACTIVE: '활성',
  SUSPENDED: '정지',
  WITHDRAWN: '탈퇴',
}

const STATUS_TABS: { value: UserStatus | ''; label: string }[] = [
  { value: '',           label: '전체' },
  { value: 'ACTIVE',    label: '활성' },
  { value: 'SUSPENDED', label: '정지' },
  { value: 'WITHDRAWN', label: '탈퇴' },
]

const ROLE_TABS: { value: UserRole | ''; label: string }[] = [
  { value: '',       label: '전체' },
  { value: 'USER',   label: '일반' },
  { value: 'EXPERT', label: '전문가' },
  { value: 'ADMIN',  label: '관리자' },
]

export default function AdminUsersPage() {
  const queryClient = useQueryClient()
  const [statusFilter, setStatusFilter] = useState<UserStatus | ''>('')
  const [roleFilter, setRoleFilter] = useState<UserRole | ''>('')
  const [page, setPage] = useState(0)

  const { data, isLoading } = useQuery({
    queryKey: ['admin', 'users', statusFilter, roleFilter, page],
    queryFn: () => adminUsersApi.getUsers(statusFilter, roleFilter, page),
  })

  const updateStatusMutation = useMutation({
    mutationFn: ({ userId, status }: { userId: number; status: 'ACTIVE' | 'SUSPENDED' }) =>
      adminUsersApi.updateStatus(userId, status),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'users'] }),
    onError: () => alert('상태 변경 중 오류가 발생했습니다.'),
  })

  const forceWithdrawMutation = useMutation({
    mutationFn: (userId: number) => adminUsersApi.forceWithdraw(userId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['admin', 'users'] }),
    onError: () => alert('강제 탈퇴 처리 중 오류가 발생했습니다.'),
  })

  const users = data?.content ?? []
  const totalPages = data?.totalPages ?? 1
  const totalElements = data?.totalElements ?? 0
  const isMutating = updateStatusMutation.isPending || forceWithdrawMutation.isPending

  return (
    <>
        <div style={{ marginBottom: '24px' }}>
          <h1 style={{ fontSize: '22px', fontWeight: 800, color: 'var(--fg)', marginBottom: '4px' }}>
            사용자 관리
          </h1>
          <p style={{ fontSize: '14px', color: 'var(--fg-muted)' }}>
            회원 상태를 조회하고 정지·강제 탈퇴를 처리합니다.
          </p>
        </div>

        {/* 상태 필터 */}
        <div style={{
          display: 'flex', gap: '4px', marginBottom: '10px',
          background: 'var(--bg-alt)', borderRadius: '10px', padding: '4px',
        }}>
          {STATUS_TABS.map((tab) => (
            <button
              key={tab.value}
              onClick={() => { setStatusFilter(tab.value); setPage(0) }}
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

        {/* 역할 필터 */}
        <div style={{ display: 'flex', gap: '6px', marginBottom: '20px' }}>
          {ROLE_TABS.map((tab) => (
            <button
              key={tab.value}
              onClick={() => { setRoleFilter(tab.value); setPage(0) }}
              style={{
                padding: '5px 14px', borderRadius: '99px',
                border: `1.5px solid ${roleFilter === tab.value ? 'var(--brand)' : 'var(--border)'}`,
                background: roleFilter === tab.value ? 'var(--brand-tint)' : '#fff',
                color: roleFilter === tab.value ? 'var(--brand-dark)' : 'var(--fg-muted)',
                fontWeight: roleFilter === tab.value ? 700 : 500,
                fontSize: '12px', cursor: 'pointer',
                transition: 'all 0.15s',
              }}
            >
              {tab.label}
            </button>
          ))}
        </div>

        {isLoading ? (
          <LoadingSpinner />
        ) : users.length === 0 ? (
          <div style={{
            padding: '60px', textAlign: 'center',
            background: 'var(--bg-alt)', borderRadius: '12px',
            color: 'var(--fg-muted)', fontSize: '15px',
          }}>
            해당하는 사용자가 없습니다.
          </div>
        ) : (
          <>
            <div style={{ fontSize: '13px', color: 'var(--fg-muted)', marginBottom: '12px' }}>
              총 {totalElements}명
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
              {users.map((user) => (
                <div key={user.id} style={{
                  background: '#fff', border: '1px solid var(--border)',
                  borderRadius: '12px', padding: '18px 20px',
                  display: 'flex', alignItems: 'center', gap: '16px',
                  flexWrap: 'wrap',
                }}>
                  <div style={{ flex: 1, minWidth: 0, display: 'flex', flexDirection: 'column', gap: '6px' }}>
                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', flexWrap: 'wrap' }}>
                      <span style={{ fontSize: '15px', fontWeight: 700, color: 'var(--fg)' }}>
                        {user.nickname}
                      </span>
                      <Badge variant={ROLE_VARIANT[user.role]}>{ROLE_LABEL[user.role]}</Badge>
                      <Badge variant={STATUS_VARIANT[user.status]}>{STATUS_LABEL[user.status]}</Badge>
                    </div>
                    <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap', fontSize: '13px', color: 'var(--fg-muted)' }}>
                      <span>{user.email}</span>
                      <span>가입일 {formatDate(user.createdAt)}</span>
                    </div>
                  </div>

                  <div style={{ display: 'flex', gap: '8px', flexShrink: 0, flexWrap: 'wrap' }}>
                    {user.status === 'ACTIVE' && (
                      <button
                        onClick={() => {
                          if (confirm(`"${user.nickname}" 계정을 정지하시겠습니까?`)) {
                            updateStatusMutation.mutate({ userId: user.id, status: 'SUSPENDED' })
                          }
                        }}
                        disabled={isMutating}
                        style={{
                          padding: '8px 16px', borderRadius: '8px',
                          border: '1.5px solid #fcd34d', background: '#fffbeb',
                          color: '#a05c00', fontWeight: 700, fontSize: '13px',
                          cursor: isMutating ? 'not-allowed' : 'pointer',
                          opacity: isMutating ? 0.7 : 1,
                        }}
                      >
                        정지
                      </button>
                    )}
                    {user.status === 'SUSPENDED' && (
                      <button
                        onClick={() => {
                          if (confirm(`"${user.nickname}" 계정 정지를 해제하시겠습니까?`)) {
                            updateStatusMutation.mutate({ userId: user.id, status: 'ACTIVE' })
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
                        해제
                      </button>
                    )}
                    {user.status !== 'WITHDRAWN' && (
                      <button
                        onClick={() => {
                          if (confirm(`"${user.nickname}" 계정을 강제 탈퇴 처리하시겠습니까?\n이 작업은 되돌릴 수 없습니다.`)) {
                            forceWithdrawMutation.mutate(user.id)
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
                        강제 탈퇴
                      </button>
                    )}
                  </div>
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
