'use client'

import { ProtectedRoute } from '@/components/layout/AppShell'
import { Card } from '@/components/ui/Card'
import { Input } from '@/components/ui/Input'
import { Button } from '@/components/ui/Button'

export default function WorkspacePage({ params }: { params: { workspaceId: string } }) {
  return (
    <ProtectedRoute>
      <div className="mx-auto max-w-4xl px-4 py-8">
        <h1 className="text-2xl font-bold">워크스페이스 #{params.workspaceId}</h1>
        <Card className="mt-4">
          <div className="mb-4 h-64 overflow-y-auto space-y-2">
            <div className="rounded-lg bg-slate-100 p-3 text-sm">제안자: 진행 상황 공유</div>
          </div>
          <div className="flex gap-2">
            <Input placeholder="메시지..." className="flex-1" />
            <Button>전송</Button>
          </div>
        </Card>
      </div>
    </ProtectedRoute>
  )
}
