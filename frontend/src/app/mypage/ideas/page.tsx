'use client'

import Link from 'next/link'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { ideasApi } from '@/api/ideas'
import { Badge } from '@/components/ui/Badge'
import { LoadingSpinner } from '@/components/ui/LoadingSpinner'
import { IDEA_STATUS_LABELS, type IdeaStatus } from '@/types/enums'
import { formatCurrency, formatDate } from '@/utils/format'

const STATUS_VARIANT: Record<IdeaStatus, 'blue' | 'green' | 'orange' | 'red' | 'gray'> = {
  AI_PENDING:             'gray',
  EXPERT_PENDING:         'orange',
  ADMIN_PENDING:          'orange',
  OPEN:                   'blue',
  IN_PROGRESS:            'green',
  COMPLETED:              'green',
  CANCELLED:              'red',
  REJECTED:               'red',
  CANCELLATION_REQUESTED: 'orange',
  SUSPENDED:              'red',
}

export default function MyIdeasPage() {
  const queryClient = useQueryClient()

  const { data: ideas, isLoading: ideasLoading } = useQuery({
    queryKey: ['ideas', 'me'],
    queryFn: ideasApi.getMyIdeas,
  })

  const { data: drafts, isLoading: draftsLoading } = useQuery({
    queryKey: ['ideas', 'drafts'],
    queryFn: ideasApi.getDrafts,
  })

  const deleteDraftMutation = useMutation({
    mutationFn: (draftId: number) => ideasApi.deleteDraft(draftId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['ideas', 'drafts'] }),
  })

  if (ideasLoading || draftsLoading) return <LoadingSpinner />

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '32px' }}>

      {/* 등록된 아이디어 */}
      <section>
        <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '20px' }}>
          <h2 style={{ fontSize: '20px', fontWeight: 700, color: 'var(--fg)', display: 'flex', alignItems: 'center', gap: '8px' }}>
            💡 내 아이디어
            <span style={{ fontSize: '16px', color: 'var(--brand)' }}>{ideas?.length ?? 0}</span>
          </h2>
          <Link href="/ideas/new" style={{
            padding: '8px 16px',
            background: 'var(--brand)',
            color: '#fff',
            borderRadius: '8px',
            fontSize: '14px',
            fontWeight: 600,
            textDecoration: 'none',
          }}>
            + 새 아이디어 제안
          </Link>
        </div>

        {!ideas || ideas.length === 0 ? (
          <div style={{
            padding: '40px',
            textAlign: 'center',
            background: 'var(--bg-alt)',
            borderRadius: '12px',
            color: 'var(--fg-muted)',
            fontSize: '15px',
          }}>
            아직 등록된 아이디어가 없어요.
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {ideas.map((idea) => (
              <Link key={idea.ideaId} href={`/ideas/${idea.ideaId}`} style={{
                display: 'flex',
                flexDirection: 'column',
                background: '#fff',
                border: `1px solid ${idea.status === 'SUSPENDED' ? '#fca5a5' : 'var(--border)'}`,
                borderRadius: '12px',
                overflow: 'hidden',
                transition: 'box-shadow 0.2s',
                textDecoration: 'none',
              }}>
                {idea.status === 'SUSPENDED' && (
                  <div style={{
                    padding: '8px 20px',
                    background: '#fef2f2',
                    borderBottom: '1px solid #fecaca',
                    fontSize: '13px',
                    fontWeight: 600,
                    color: '#dc2626',
                    display: 'flex',
                    alignItems: 'center',
                    gap: '6px',
                  }}>
                    ⚠️ 분쟁 처리 중으로 일시 중단된 아이디어입니다. 관리자 검토 후 재개됩니다.
                  </div>
                )}
                <div style={{ display: 'flex', alignItems: 'center', gap: '20px', padding: '20px 24px' }}>
                <div style={{
                  width: '56px',
                  height: '56px',
                  borderRadius: '10px',
                  background: 'var(--brand-tint)',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: '24px',
                  flexShrink: 0,
                }}>
                  💡
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <p style={{ fontSize: '16px', fontWeight: 700, color: 'var(--fg)', marginBottom: '6px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                    {idea.title}
                  </p>
                  <div style={{ display: 'flex', alignItems: 'center', gap: '12px', fontSize: '13px', color: 'var(--fg-muted)' }}>
                    <Badge variant={STATUS_VARIANT[idea.status as IdeaStatus]}>
                      {IDEA_STATUS_LABELS[idea.status as IdeaStatus]}
                    </Badge>
                    <span>등록일 {formatDate(idea.createdAt)}</span>
                  </div>
                </div>
                <div style={{ textAlign: 'right', flexShrink: 0 }}>
                  <p style={{ fontSize: '20px', fontWeight: 700, color: 'var(--brand)' }}>
                    {formatCurrency(idea.currentAmount)}
                  </p>
                  <p style={{ fontSize: '13px', color: 'var(--fg-muted)', marginTop: '2px' }}>
                    목표 {formatCurrency(idea.goalAmount)}
                  </p>
                </div>
                </div>
              </Link>
            ))}
          </div>
        )}
      </section>

      {/* 임시저장 */}
      <section>
        <h2 style={{ fontSize: '20px', fontWeight: 700, color: 'var(--fg)', marginBottom: '20px', display: 'flex', alignItems: 'center', gap: '8px' }}>
          📝 임시저장
          <span style={{ fontSize: '16px', color: 'var(--fg-muted)' }}>{drafts?.length ?? 0}</span>
        </h2>

        {!drafts || drafts.length === 0 ? (
          <div style={{
            padding: '40px',
            textAlign: 'center',
            background: 'var(--bg-alt)',
            borderRadius: '12px',
            color: 'var(--fg-muted)',
            fontSize: '15px',
          }}>
            임시저장된 아이디어가 없어요.
          </div>
        ) : (
          <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
            {drafts.map((draft) => (
              <div key={draft.draftId} style={{
                display: 'flex',
                alignItems: 'center',
                gap: '16px',
                background: '#fff',
                border: '1px solid var(--border)',
                borderRadius: '12px',
                padding: '16px 24px',
              }}>
                <div style={{
                  width: '44px',
                  height: '44px',
                  borderRadius: '8px',
                  background: '#f5f5f5',
                  display: 'flex',
                  alignItems: 'center',
                  justifyContent: 'center',
                  fontSize: '20px',
                  flexShrink: 0,
                }}>
                  📝
                </div>
                <div style={{ flex: 1, minWidth: 0 }}>
                  <p style={{ fontSize: '15px', fontWeight: 700, color: 'var(--fg)', marginBottom: '4px' }}>
                    {draft.title ?? '(제목 없음)'}
                  </p>
                  <p style={{ fontSize: '13px', color: 'var(--fg-muted)' }}>
                    {draft.oneLineIntro ?? '한 줄 소개 없음'} · 최종 수정 {formatDate(draft.updatedAt)}
                  </p>
                </div>
                <div style={{ display: 'flex', gap: '8px', flexShrink: 0 }}>
                  <Link href={`/ideas/new?draftId=${draft.draftId}`} style={{
                    padding: '6px 14px',
                    border: '1.5px solid var(--brand)',
                    borderRadius: '6px',
                    color: 'var(--brand-dark)',
                    fontSize: '13px',
                    fontWeight: 600,
                    textDecoration: 'none',
                  }}>
                    이어 작성
                  </Link>
                  <button
                    onClick={() => { if (confirm('임시저장을 삭제할까요?')) deleteDraftMutation.mutate(draft.draftId) }}
                    style={{
                      padding: '6px 14px',
                      border: '1px solid var(--border)',
                      borderRadius: '6px',
                      color: 'var(--fg-muted)',
                      fontSize: '13px',
                      background: 'transparent',
                      cursor: 'pointer',
                    }}
                  >
                    삭제
                  </button>
                </div>
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  )
}
