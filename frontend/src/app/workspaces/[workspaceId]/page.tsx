'use client'

import { useState, useEffect, useRef } from 'react'
import { useParams } from 'next/navigation'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { apiClient, unwrap } from '@/api/client'
import { milestonesApi } from '@/api/milestones'
import type { ApiResponse } from '@/types/api'
import type { MilestoneResponse, CompletionReportResponse, MilestoneStatus } from '@/api/milestones'
import { Badge } from '@/components/ui/Badge'
import { LoadingSpinner } from '@/components/ui/LoadingSpinner'
import { ProtectedRoute } from '@/components/layout/AppShell'
import { useAuthStore } from '@/store/authStore'
import { formatCurrency, formatDateTime } from '@/utils/format'

interface WorkspaceInfo {
  workspaceId: number
  ideaId: number
  title: string
  status: string
  creatorId: number
  creatorNickname: string
  creator: boolean
}

interface WorkspaceMessage {
  messageId: number
  authorId: number
  authorNickname: string
  content: string
  createdAt: string
}

interface FundUsageResponse {
  fundUsageId: number
  ideaId: number
  itemName: string
  amount: number
  usedAt: string
}

interface PreSettlementResponse {
  preSettlementId: number
  ideaId: number
  amount: number
  status: 'PENDING' | 'COMPLETED' | 'FAILED'
  requestedAt: string
}

const workspaceApi = {
  getInfo: (workspaceId: number) =>
    unwrap(apiClient.get<ApiResponse<WorkspaceInfo>>(`/workspaces/${workspaceId}`)),

  getMessages: (workspaceId: number) =>
    unwrap(apiClient.get<ApiResponse<WorkspaceMessage[]>>(`/workspaces/${workspaceId}/messages`)),

  sendMessage: (workspaceId: number, content: string) =>
    unwrap(apiClient.post<ApiResponse<WorkspaceMessage>>(`/workspaces/${workspaceId}/messages`, { content })),

  getFundUsage: (workspaceId: number) =>
    unwrap(apiClient.get<ApiResponse<FundUsageResponse[]>>(`/workspaces/${workspaceId}/fund-usage`)),
}

const fundUsageApi = {
  create: (ideaId: number, body: { itemName: string; amount: number; usedAt: string }) =>
    unwrap(apiClient.post<ApiResponse<FundUsageResponse>>(`/fund-usages/${ideaId}`, body)),

  getByIdea: (ideaId: number) =>
    unwrap(apiClient.get<ApiResponse<FundUsageResponse[]>>(`/fund-usages/${ideaId}`)),
}

const preSettlementApi = {
  request: (ideaId: number, amount: number) =>
    unwrap(apiClient.post<ApiResponse<PreSettlementResponse>>(`/pre-settlements/ideas/${ideaId}`, { amount })),

  getByIdea: (ideaId: number) =>
    unwrap(apiClient.get<ApiResponse<PreSettlementResponse[]>>(`/pre-settlements/ideas/${ideaId}`)),
}

const MILESTONE_STATUS_BADGE: Record<MilestoneStatus, { variant: 'green' | 'orange' | 'red' | 'gray'; label: string }> = {
  PENDING:     { variant: 'gray',   label: '대기' },
  IN_PROGRESS: { variant: 'orange', label: '진행중' },
  COMPLETED:   { variant: 'green',  label: '완료' },
  CANCELLED:   { variant: 'red',    label: '취소됨' },
}

const REPORT_STATUS_BADGE: Record<string, { variant: 'green' | 'orange' | 'red' | 'gray'; label: string }> = {
  SUBMITTED: { variant: 'orange', label: '검토 중' },
  APPROVED:  { variant: 'green',  label: '승인됨' },
  REJECTED:  { variant: 'red',    label: '반려됨' },
}

const PRE_SETTLEMENT_STATUS_BADGE: Record<string, { variant: 'green' | 'orange' | 'red' | 'gray'; label: string }> = {
  PENDING:   { variant: 'orange', label: '검토 중' },
  COMPLETED: { variant: 'green',  label: '완료' },
  FAILED:    { variant: 'red',    label: '실패' },
}

type TabKey = 'messages' | 'milestones' | 'fundUsage' | 'preSettlement'

const TABS: { key: TabKey; label: string }[] = [
  { key: 'messages',      label: '💬 메시지' },
  { key: 'milestones',    label: '📋 마일스톤' },
  { key: 'fundUsage',     label: '💰 자금 사용 내역' },
  { key: 'preSettlement', label: '💸 선정산 신청' },
]

function MessagesTab({
  workspaceId,
  currentUserId,
}: {
  workspaceId: number
  currentUserId: number
}) {
  const queryClient = useQueryClient()
  const [content, setContent] = useState('')
  const listRef = useRef<HTMLDivElement>(null)

  const { data: messages, isLoading } = useQuery({
    queryKey: ['workspace', workspaceId, 'messages'],
    queryFn: () => workspaceApi.getMessages(workspaceId),
    refetchInterval: 30_000,
  })

  useEffect(() => {
    if (listRef.current) {
      listRef.current.scrollTop = listRef.current.scrollHeight
    }
  }, [messages])

  const sendMutation = useMutation({
    mutationFn: (text: string) => workspaceApi.sendMessage(workspaceId, text),
    onSuccess: () => {
      setContent('')
      queryClient.invalidateQueries({ queryKey: ['workspace', workspaceId, 'messages'] })
    },
    onError: () => alert('메시지 전송 중 오류가 발생했습니다.'),
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    const trimmed = content.trim()
    if (!trimmed || sendMutation.isPending) return
    sendMutation.mutate(trimmed)
  }

  if (isLoading) return <LoadingSpinner />

  const items = messages ?? []

  return (
    <div style={{ display: 'flex', flexDirection: 'column', height: '560px' }}>
      <div
        ref={listRef}
        style={{
          flex: 1,
          overflowY: 'auto',
          padding: '16px',
          display: 'flex',
          flexDirection: 'column',
          gap: '12px',
          background: 'var(--bg-alt)',
          borderRadius: '12px',
          marginBottom: '12px',
        }}
      >
        {items.length === 0 ? (
          <div style={{ textAlign: 'center', color: 'var(--fg-muted)', fontSize: '14px', margin: 'auto' }}>
            아직 메시지가 없습니다.
          </div>
        ) : (
          items.map((msg) => {
            const isMine = msg.authorId === currentUserId
            return (
              <div
                key={msg.messageId}
                style={{
                  display: 'flex',
                  flexDirection: 'column',
                  alignItems: isMine ? 'flex-end' : 'flex-start',
                }}
              >
                {!isMine && (
                  <span style={{ fontSize: '12px', color: 'var(--fg-muted)', marginBottom: '4px', fontWeight: 600 }}>
                    {msg.authorNickname}
                  </span>
                )}
                <div
                  style={{
                    maxWidth: '72%',
                    padding: '10px 14px',
                    borderRadius: isMine ? '16px 4px 16px 16px' : '4px 16px 16px 16px',
                    background: isMine ? 'var(--brand)' : '#fff',
                    color: isMine ? '#fff' : 'var(--fg)',
                    fontSize: '14px',
                    lineHeight: 1.6,
                    border: isMine ? 'none' : '1px solid var(--border)',
                    wordBreak: 'break-word',
                  }}
                >
                  {msg.content}
                </div>
                <span style={{ fontSize: '11px', color: 'var(--fg-muted)', marginTop: '4px' }}>
                  {formatDateTime(msg.createdAt)}
                </span>
              </div>
            )
          })
        )}
      </div>

      <form onSubmit={handleSubmit} style={{ display: 'flex', gap: '10px' }}>
        <input
          value={content}
          onChange={(e) => setContent(e.target.value)}
          placeholder="메시지를 입력하세요..."
          style={{
            flex: 1,
            padding: '11px 14px',
            borderRadius: '10px',
            border: '1.5px solid var(--border)',
            fontSize: '14px',
            color: 'var(--fg)',
            outline: 'none',
            boxSizing: 'border-box',
          }}
        />
        <button
          type="submit"
          disabled={!content.trim() || sendMutation.isPending}
          style={{
            padding: '11px 22px',
            borderRadius: '10px',
            border: 'none',
            background: 'var(--brand)',
            color: '#fff',
            fontSize: '14px',
            fontWeight: 700,
            cursor: !content.trim() || sendMutation.isPending ? 'not-allowed' : 'pointer',
            opacity: !content.trim() || sendMutation.isPending ? 0.6 : 1,
            flexShrink: 0,
          }}
        >
          전송
        </button>
      </form>
    </div>
  )
}

function MilestoneReportForm({
  milestoneId,
  onClose,
}: {
  milestoneId: number
  onClose: () => void
}) {
  const queryClient = useQueryClient()
  const [reportContent, setReportContent] = useState('')
  const [file, setFile] = useState<File | null>(null)

  const submitMutation = useMutation({
    mutationFn: () => milestonesApi.submitCompletionReport(milestoneId, reportContent, file ?? undefined),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['workspace', 'milestone', milestoneId, 'reports'] })
      onClose()
    },
    onError: () => alert('보고서 제출 중 오류가 발생했습니다.'),
  })

  return (
    <div
      style={{
        marginTop: '12px',
        padding: '16px',
        borderRadius: '10px',
        border: '1.5px solid var(--border)',
        background: 'var(--bg-alt)',
      }}
    >
      <p style={{ fontSize: '13px', fontWeight: 700, color: 'var(--fg)', marginBottom: '10px' }}>
        완료 보고서 제출
      </p>
      <textarea
        value={reportContent}
        onChange={(e) => setReportContent(e.target.value)}
        placeholder="완료 내용을 상세히 작성하세요..."
        rows={4}
        style={{
          width: '100%',
          padding: '10px 12px',
          borderRadius: '8px',
          border: '1.5px solid var(--border)',
          fontSize: '13px',
          color: 'var(--fg)',
          resize: 'vertical',
          outline: 'none',
          boxSizing: 'border-box',
          marginBottom: '10px',
        }}
      />
      <div style={{ marginBottom: '12px' }}>
        <label style={{ fontSize: '13px', color: 'var(--fg-muted)', fontWeight: 600, display: 'block', marginBottom: '6px' }}>
          첨부 파일 (선택)
        </label>
        <input
          type="file"
          onChange={(e) => setFile(e.target.files?.[0] ?? null)}
          style={{ fontSize: '13px', color: 'var(--fg)' }}
        />
      </div>
      <div style={{ display: 'flex', gap: '8px' }}>
        <button
          onClick={() => submitMutation.mutate()}
          disabled={!reportContent.trim() || submitMutation.isPending}
          style={{
            padding: '8px 18px',
            borderRadius: '8px',
            border: 'none',
            background: 'var(--brand)',
            color: '#fff',
            fontSize: '13px',
            fontWeight: 700,
            cursor: !reportContent.trim() || submitMutation.isPending ? 'not-allowed' : 'pointer',
            opacity: !reportContent.trim() || submitMutation.isPending ? 0.6 : 1,
          }}
        >
          {submitMutation.isPending ? '제출 중...' : '제출'}
        </button>
        <button
          onClick={onClose}
          style={{
            padding: '8px 18px',
            borderRadius: '8px',
            border: '1.5px solid var(--border)',
            background: '#fff',
            color: 'var(--fg-muted)',
            fontSize: '13px',
            fontWeight: 600,
            cursor: 'pointer',
          }}
        >
          취소
        </button>
      </div>
    </div>
  )
}

function MilestoneCard({
  milestone,
  isCreator,
}: {
  milestone: MilestoneResponse
  isCreator: boolean
}) {
  const [showForm, setShowForm] = useState(false)

  const { data: reports, isLoading: reportsLoading } = useQuery({
    queryKey: ['workspace', 'milestone', milestone.id, 'reports'],
    queryFn: () => milestonesApi.getReports(milestone.id),
  })

  const statusBadge = MILESTONE_STATUS_BADGE[milestone.status]
  const canSubmit = milestone.status === 'IN_PROGRESS' && isCreator

  return (
    <div
      style={{
        background: '#fff',
        border: '1px solid var(--border)',
        borderRadius: '12px',
        padding: '18px 20px',
      }}
    >
      <div style={{ display: 'flex', alignItems: 'flex-start', gap: '12px', marginBottom: '12px' }}>
        <div
          style={{
            width: '30px',
            height: '30px',
            borderRadius: '50%',
            background: 'var(--brand-tint)',
            color: 'var(--brand-dark)',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            fontWeight: 800,
            fontSize: '13px',
            flexShrink: 0,
          }}
        >
          {milestone.step}
        </div>
        <div style={{ flex: 1, minWidth: 0 }}>
          <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px', flexWrap: 'wrap' }}>
            <span style={{ fontSize: '15px', fontWeight: 700, color: 'var(--fg)' }}>{milestone.goal}</span>
            <Badge variant={statusBadge.variant}>{statusBadge.label}</Badge>
          </div>
          <div style={{ display: 'flex', gap: '12px', flexWrap: 'wrap', fontSize: '12px', color: 'var(--fg-muted)' }}>
            <span>기대 결과: {milestone.expectedResult}</span>
            <span>마감일: {milestone.expectedDate}</span>
          </div>
        </div>
      </div>

      {reportsLoading ? (
        <div style={{ padding: '8px 0' }}>
          <LoadingSpinner />
        </div>
      ) : (
        reports && reports.length > 0 && (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '8px', marginBottom: '12px' }}>
            {reports.map((r) => {
              const rb = REPORT_STATUS_BADGE[r.status]
              return (
                <div
                  key={r.reportId}
                  style={{
                    padding: '12px 14px',
                    borderRadius: '8px',
                    border: '1px solid var(--border)',
                    background: 'var(--bg-alt)',
                  }}
                >
                  <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '6px', flexWrap: 'wrap' }}>
                    <span style={{ fontSize: '12px', fontWeight: 700, color: 'var(--fg)' }}>
                      {r.type === 'COMPLETION' ? '완료 보고서' : '소명 보고서'}
                    </span>
                    <Badge variant={rb.variant}>{rb.label}</Badge>
                    <span style={{ fontSize: '11px', color: 'var(--fg-muted)', marginLeft: 'auto' }}>
                      {formatDateTime(r.submittedAt)}
                    </span>
                  </div>
                  <p style={{ fontSize: '13px', color: 'var(--fg)', lineHeight: 1.6, whiteSpace: 'pre-wrap', marginBottom: r.fileUrl ? '8px' : 0 }}>
                    {r.content}
                  </p>
                  {r.fileUrl && (
                    <a
                      href={r.fileUrl}
                      target="_blank"
                      rel="noopener noreferrer"
                      style={{ fontSize: '12px', color: 'var(--brand)', textDecoration: 'underline' }}
                    >
                      📎 첨부 파일 보기
                    </a>
                  )}
                  {r.rejectReason && (
                    <p style={{ fontSize: '12px', color: 'var(--error)', marginTop: '6px' }}>
                      반려 사유: {r.rejectReason}
                    </p>
                  )}
                </div>
              )
            })}
          </div>
        )
      )}

      {canSubmit && !showForm && (
        <button
          onClick={() => setShowForm(true)}
          style={{
            padding: '8px 16px',
            borderRadius: '8px',
            border: '1.5px solid var(--brand)',
            background: 'var(--brand-tint)',
            color: 'var(--brand-dark)',
            fontSize: '13px',
            fontWeight: 700,
            cursor: 'pointer',
          }}
        >
          완료 보고서 제출
        </button>
      )}

      {showForm && (
        <MilestoneReportForm milestoneId={milestone.id} onClose={() => setShowForm(false)} />
      )}
    </div>
  )
}

function MilestonesTab({ ideaId, isCreator }: { ideaId: number; isCreator: boolean }) {
  const { data: milestones, isLoading } = useQuery({
    queryKey: ['workspace', 'milestones', ideaId],
    queryFn: () => milestonesApi.getMilestonesByIdea(ideaId),
  })

  if (isLoading) return <LoadingSpinner />

  const items = milestones ?? []

  if (items.length === 0) {
    return (
      <div
        style={{
          padding: '60px',
          textAlign: 'center',
          background: 'var(--bg-alt)',
          borderRadius: '12px',
          color: 'var(--fg-muted)',
          fontSize: '15px',
        }}
      >
        등록된 마일스톤이 없습니다.
      </div>
    )
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
      {items.map((m) => (
        <MilestoneCard key={m.id} milestone={m} isCreator={isCreator} />
      ))}
    </div>
  )
}

function FundUsageTab({ workspaceId, ideaId, isCreator }: { workspaceId: number; ideaId: number; isCreator: boolean }) {
  const queryClient = useQueryClient()
  const [itemName, setItemName] = useState('')
  const [amount, setAmount] = useState('')
  const [usedAt, setUsedAt] = useState('')

  const { data: usages, isLoading } = useQuery({
    queryKey: ['workspace', workspaceId, 'fund-usage'],
    queryFn: () => workspaceApi.getFundUsage(workspaceId),
  })

  const createMutation = useMutation({
    mutationFn: () =>
      fundUsageApi.create(ideaId, {
        itemName,
        amount: Number(amount),
        usedAt,
      }),
    onSuccess: () => {
      setItemName('')
      setAmount('')
      setUsedAt('')
      queryClient.invalidateQueries({ queryKey: ['workspace', workspaceId, 'fund-usage'] })
    },
    onError: () => alert('자금 사용 내역 등록 중 오류가 발생했습니다.'),
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!itemName.trim() || !amount || !usedAt) return
    createMutation.mutate()
  }

  if (isLoading) return <LoadingSpinner />

  const items = usages ?? []

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      {isCreator && (
        <form
          onSubmit={handleSubmit}
          style={{
            padding: '20px',
            borderRadius: '12px',
            border: '1.5px solid var(--border)',
            background: '#fff',
            display: 'flex',
            flexDirection: 'column',
            gap: '12px',
          }}
        >
          <p style={{ fontSize: '14px', fontWeight: 700, color: 'var(--fg)' }}>자금 사용 내역 등록</p>
          <div style={{ display: 'flex', gap: '10px', flexWrap: 'wrap' }}>
            <input
              value={itemName}
              onChange={(e) => setItemName(e.target.value)}
              placeholder="항목명"
              style={{
                flex: '1 1 160px',
                padding: '9px 12px',
                borderRadius: '8px',
                border: '1.5px solid var(--border)',
                fontSize: '13px',
                color: 'var(--fg)',
                outline: 'none',
              }}
            />
            <input
              type="number"
              min={1}
              value={amount}
              onChange={(e) => setAmount(e.target.value)}
              placeholder="금액"
              style={{
                flex: '1 1 120px',
                padding: '9px 12px',
                borderRadius: '8px',
                border: '1.5px solid var(--border)',
                fontSize: '13px',
                color: 'var(--fg)',
                outline: 'none',
              }}
            />
            <input
              type="date"
              value={usedAt}
              onChange={(e) => setUsedAt(e.target.value)}
              style={{
                flex: '1 1 140px',
                padding: '9px 12px',
                borderRadius: '8px',
                border: '1.5px solid var(--border)',
                fontSize: '13px',
                color: 'var(--fg)',
                outline: 'none',
              }}
            />
            <button
              type="submit"
              disabled={!itemName.trim() || !amount || !usedAt || createMutation.isPending}
              style={{
                padding: '9px 20px',
                borderRadius: '8px',
                border: 'none',
                background: 'var(--brand)',
                color: '#fff',
                fontSize: '13px',
                fontWeight: 700,
                cursor: !itemName.trim() || !amount || !usedAt || createMutation.isPending ? 'not-allowed' : 'pointer',
                opacity: !itemName.trim() || !amount || !usedAt || createMutation.isPending ? 0.6 : 1,
                flexShrink: 0,
              }}
            >
              {createMutation.isPending ? '등록 중...' : '등록'}
            </button>
          </div>
        </form>
      )}

      {items.length === 0 ? (
        <div
          style={{
            padding: '60px',
            textAlign: 'center',
            background: 'var(--bg-alt)',
            borderRadius: '12px',
            color: 'var(--fg-muted)',
            fontSize: '15px',
          }}
        >
          등록된 자금 사용 내역이 없습니다.
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
          {items.map((u) => (
            <div
              key={u.fundUsageId}
              style={{
                background: '#fff',
                border: '1px solid var(--border)',
                borderRadius: '10px',
                padding: '14px 18px',
                display: 'flex',
                alignItems: 'center',
                gap: '16px',
              }}
            >
              <div style={{ flex: 1 }}>
                <p style={{ fontSize: '14px', fontWeight: 700, color: 'var(--fg)', marginBottom: '4px' }}>
                  {u.itemName}
                </p>
                <p style={{ fontSize: '12px', color: 'var(--fg-muted)' }}>{u.usedAt}</p>
              </div>
              <p style={{ fontSize: '15px', fontWeight: 700, color: 'var(--brand-dark)' }}>
                {formatCurrency(u.amount)}
              </p>
            </div>
          ))}
        </div>
      )}
    </div>
  )
}

function PreSettlementTab({ ideaId, isCreator }: { ideaId: number; isCreator: boolean }) {
  const queryClient = useQueryClient()
  const [amountInput, setAmountInput] = useState('')

  const { data: settlements, isLoading } = useQuery({
    queryKey: ['workspace', 'pre-settlements', ideaId],
    queryFn: () => preSettlementApi.getByIdea(ideaId),
  })

  const requestMutation = useMutation({
    mutationFn: () => preSettlementApi.request(ideaId, Number(amountInput)),
    onSuccess: () => {
      setAmountInput('')
      queryClient.invalidateQueries({ queryKey: ['workspace', 'pre-settlements', ideaId] })
    },
    onError: () => alert('선정산 신청 중 오류가 발생했습니다.'),
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!amountInput || Number(amountInput) <= 0) return
    requestMutation.mutate()
  }

  if (isLoading) return <LoadingSpinner />

  const items = settlements ?? []

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
      {isCreator && (
        <form
          onSubmit={handleSubmit}
          style={{
            padding: '20px',
            borderRadius: '12px',
            border: '1.5px solid var(--border)',
            background: '#fff',
            display: 'flex',
            flexDirection: 'column',
            gap: '12px',
          }}
        >
          <p style={{ fontSize: '14px', fontWeight: 700, color: 'var(--fg)' }}>선정산 신청</p>
          <div style={{ display: 'flex', gap: '10px' }}>
            <input
              type="number"
              min={1}
              value={amountInput}
              onChange={(e) => setAmountInput(e.target.value)}
              placeholder="신청 금액 (원)"
              style={{
                flex: 1,
                padding: '10px 14px',
                borderRadius: '8px',
                border: '1.5px solid var(--border)',
                fontSize: '14px',
                color: 'var(--fg)',
                outline: 'none',
              }}
            />
            <button
              type="submit"
              disabled={!amountInput || Number(amountInput) <= 0 || requestMutation.isPending}
              style={{
                padding: '10px 22px',
                borderRadius: '8px',
                border: 'none',
                background: 'var(--brand)',
                color: '#fff',
                fontSize: '14px',
                fontWeight: 700,
                cursor: !amountInput || Number(amountInput) <= 0 || requestMutation.isPending ? 'not-allowed' : 'pointer',
                opacity: !amountInput || Number(amountInput) <= 0 || requestMutation.isPending ? 0.6 : 1,
                flexShrink: 0,
              }}
            >
              {requestMutation.isPending ? '신청 중...' : '선정산 신청'}
            </button>
          </div>
        </form>
      )}

      {items.length === 0 ? (
        <div
          style={{
            padding: '60px',
            textAlign: 'center',
            background: 'var(--bg-alt)',
            borderRadius: '12px',
            color: 'var(--fg-muted)',
            fontSize: '15px',
          }}
        >
          선정산 신청 내역이 없습니다.
        </div>
      ) : (
        <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
          {items.map((s) => {
            const sb = PRE_SETTLEMENT_STATUS_BADGE[s.status]
            return (
              <div
                key={s.preSettlementId}
                style={{
                  background: '#fff',
                  border: '1px solid var(--border)',
                  borderRadius: '10px',
                  padding: '14px 18px',
                  display: 'flex',
                  alignItems: 'center',
                  gap: '16px',
                }}
              >
                <div style={{ flex: 1 }}>
                  <p style={{ fontSize: '15px', fontWeight: 700, color: 'var(--brand-dark)', marginBottom: '4px' }}>
                    {formatCurrency(s.amount)}
                  </p>
                  <p style={{ fontSize: '12px', color: 'var(--fg-muted)' }}>
                    신청일: {formatDateTime(s.requestedAt)}
                  </p>
                </div>
                <Badge variant={sb.variant}>{sb.label}</Badge>
              </div>
            )
          })}
        </div>
      )}
    </div>
  )
}

function WorkspaceContent({ workspaceId }: { workspaceId: number }) {
  const { user } = useAuthStore()
  const [activeTab, setActiveTab] = useState<TabKey>('messages')

  const { data: workspace, isLoading, isError } = useQuery({
    queryKey: ['workspace', workspaceId],
    queryFn: () => workspaceApi.getInfo(workspaceId),
    retry: false,
  })

  if (isLoading) return <LoadingSpinner />

  if (isError || !workspace) {
    return (
      <div
        style={{
          padding: '80px 40px',
          textAlign: 'center',
          background: 'var(--bg-alt)',
          borderRadius: '16px',
          color: 'var(--fg-muted)',
          fontSize: '16px',
        }}
      >
        접근 권한이 없습니다.
      </div>
    )
  }

  return (
    <div
      style={{
        maxWidth: '860px',
        margin: '0 auto',
        padding: '40px 24px',
      }}
    >
      <div style={{ marginBottom: '28px' }}>
        <h1 style={{ fontSize: '22px', fontWeight: 800, color: 'var(--fg)', marginBottom: '6px' }}>
          {workspace.title}
        </h1>
        <p style={{ fontSize: '14px', color: 'var(--fg-muted)' }}>
          제안자: {workspace.creatorNickname}
        </p>
      </div>

      <div
        style={{
          display: 'flex',
          gap: '4px',
          borderBottom: '2px solid var(--border)',
          marginBottom: '28px',
        }}
      >
        {TABS.map((tab) => (
          <button
            key={tab.key}
            onClick={() => setActiveTab(tab.key)}
            style={{
              padding: '10px 18px',
              fontSize: '14px',
              fontWeight: activeTab === tab.key ? 700 : 500,
              color: activeTab === tab.key ? 'var(--brand)' : 'var(--fg-muted)',
              background: 'none',
              border: 'none',
              borderBottom: activeTab === tab.key ? '2px solid var(--brand)' : '2px solid transparent',
              marginBottom: '-2px',
              cursor: 'pointer',
              transition: 'color 0.2s',
              whiteSpace: 'nowrap',
            }}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {activeTab === 'messages' && user && (
        <MessagesTab workspaceId={workspaceId} currentUserId={user.id} />
      )}
      {activeTab === 'milestones' && (
        <MilestonesTab ideaId={workspace.ideaId} isCreator={workspace.creator} />
      )}
      {activeTab === 'fundUsage' && (
        <FundUsageTab workspaceId={workspaceId} ideaId={workspace.ideaId} isCreator={workspace.creator} />
      )}
      {activeTab === 'preSettlement' && (
        <PreSettlementTab ideaId={workspace.ideaId} isCreator={workspace.creator} />
      )}
    </div>
  )
}

export default function WorkspacePage() {
  const params = useParams()
  const workspaceId = Number(params.workspaceId)

  return (
    <ProtectedRoute>
      <WorkspaceContent workspaceId={workspaceId} />
    </ProtectedRoute>
  )
}
