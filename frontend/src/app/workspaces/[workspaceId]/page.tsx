'use client'

import { useState } from 'react'
import { useParams } from 'next/navigation'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { workspacesApi } from '@/api/workspaces'
import { ProtectedRoute } from '@/components/layout/AppShell'
import { Card } from '@/components/ui/Card'
import { Input } from '@/components/ui/Input'
import { Button } from '@/components/ui/Button'
import { LoadingSpinner } from '@/components/ui/LoadingSpinner'
import { getErrorMessage } from '@/utils/format'
import Link from 'next/link'

function WorkspaceContent() {
  const params = useParams()
  const workspaceId = Number(params.workspaceId)
  const queryClient = useQueryClient()
  const [content, setContent] = useState('')
  const [error, setError] = useState('')

  const { data: workspace, isLoading, error: loadError } = useQuery({
    queryKey: ['workspaces', workspaceId],
    queryFn: () => workspacesApi.get(workspaceId),
    enabled: !!workspaceId,
    retry: false,
  })

  const { data: messages } = useQuery({
    queryKey: ['workspaces', workspaceId, 'messages'],
    queryFn: () => workspacesApi.getMessages(workspaceId),
    enabled: !!workspaceId && !!workspace,
  })

  const sendMutation = useMutation({
    mutationFn: () => workspacesApi.sendMessage(workspaceId, content.trim()),
    onSuccess: () => {
      setContent('')
      queryClient.invalidateQueries({ queryKey: ['workspaces', workspaceId, 'messages'] })
    },
    onError: (err) => setError(getErrorMessage(err)),
  })

  if (isLoading) return <LoadingSpinner />
  if (loadError || !workspace) {
    return (
      <div className="py-16 text-center">
        <p className="text-red-600">{getErrorMessage(loadError) || '워크스페이스에 접근할 수 없습니다.'}</p>
        <p className="mt-2 text-sm text-slate-500">결제 완료 후원자 또는 제안자만 입장할 수 있습니다.</p>
        <Link href="/ideas" className="mt-4 inline-block text-indigo-600">아이디어 목록</Link>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-4xl px-4 py-8">
      <Link href={`/ideas/${workspace.ideaId}`} className="text-sm text-indigo-600">← 아이디어로 돌아가기</Link>
      <h1 className="mt-4 text-2xl font-bold">{workspace.title}</h1>
      <p className="mt-1 text-sm text-slate-500">
        제안자 {workspace.creatorNickname} · 상태 {workspace.status}
        {workspace.creator ? ' · 내 프로젝트' : ' · 후원자 워크스페이스'}
      </p>
      <Card className="mt-6">
        <h2 className="font-semibold">진행 소통</h2>
        <div className="mb-4 mt-4 max-h-80 space-y-2 overflow-y-auto">
          {(messages ?? []).length === 0 ? (
            <p className="text-sm text-slate-400">아직 메시지가 없습니다.</p>
          ) : (
            messages!.map((m) => (
              <div key={m.id} className="rounded-lg bg-slate-100 p-3 text-sm">
                <span className="font-semibold text-slate-700">{m.authorNickname}</span>
                <span className="ml-2 text-xs text-slate-400">
                  {new Date(m.createdAt).toLocaleString('ko-KR')}
                </span>
                <p className="mt-1 text-slate-800">{m.content}</p>
              </div>
            ))
          )}
        </div>
        <div className="flex gap-2">
          <Input
            placeholder="메시지를 입력하세요..."
            className="flex-1"
            value={content}
            onChange={(e) => setContent(e.target.value)}
            onKeyDown={(e) => {
              if (e.key === 'Enter' && content.trim()) sendMutation.mutate()
            }}
          />
          <Button
            onClick={() => content.trim() && sendMutation.mutate()}
            loading={sendMutation.isPending}
            disabled={!content.trim()}
          >
            전송
          </Button>
        </div>
        {error && <p className="mt-2 text-sm text-red-600">{error}</p>}
      </Card>
    </div>
  )
}

export default function WorkspacePage() {
  return (
    <ProtectedRoute>
      <WorkspaceContent />
    </ProtectedRoute>
  )
}
