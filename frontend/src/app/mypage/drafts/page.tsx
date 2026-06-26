'use client'

import Link from 'next/link'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { ideasApi } from '@/api/ideas'
import { formatDate } from '@/utils/format'

export default function DraftsPage() {
  const queryClient = useQueryClient()

  const { data: drafts, isLoading } = useQuery({
    queryKey: ['ideas', 'drafts'],
    queryFn: ideasApi.getDrafts,
  })

  const deleteMutation = useMutation({
    mutationFn: (draftId: number) => ideasApi.deleteDraft(draftId),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['ideas', 'drafts'] }),
  })

  if (isLoading) {
    return <p style={{ color: 'var(--fg-muted)', fontSize: '14px' }}>불러오는 중...</p>
  }

  if (!drafts || drafts.length === 0) {
    return (
      <div style={{ textAlign: 'center', padding: '48px 0', color: 'var(--fg-muted)' }}>
        <p style={{ fontSize: '16px' }}>임시저장된 아이디어가 없어요.</p>
        <Link href="/ideas/new" style={{ display: 'inline-block', marginTop: '16px', color: 'var(--brand)', fontWeight: 600 }}>
          아이디어 등록하기 →
        </Link>
      </div>
    )
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '12px' }}>
      {drafts.map((draft) => (
        <div key={draft.draftId} style={{
          background: '#fff',
          border: '1px solid var(--border)',
          borderRadius: 'var(--radius-lg)',
          padding: '20px 24px',
          display: 'flex',
          alignItems: 'center',
          gap: '16px',
        }}>
          <div style={{ flex: 1 }}>
            <p style={{ fontSize: '16px', fontWeight: 700, color: 'var(--fg)', marginBottom: '4px' }}>
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
              borderRadius: 'var(--radius-sm)',
              color: 'var(--brand-dark)',
              fontSize: '13px',
              fontWeight: 600,
            }}>
              이어 작성
            </Link>
            <button
              onClick={() => {
                if (confirm('임시저장을 삭제할까요?')) deleteMutation.mutate(draft.draftId)
              }}
              style={{
                padding: '6px 14px',
                border: '1px solid var(--border)',
                borderRadius: 'var(--radius-sm)',
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
  )
}
