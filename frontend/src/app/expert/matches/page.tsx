'use client'

import { ProtectedRoute } from '@/components/layout/AppShell'
import { EmptyState } from '@/components/ui/EmptyState'
import { Button } from '@/components/ui/Button'

export default function ExpertMatchesPage() {
  return (
    <ProtectedRoute roles={['EXPERT']}>
      <div className="mx-auto max-w-4xl px-4 py-8">
        <h1 className="text-2xl font-bold">매칭 목록</h1>
        <EmptyState title="매칭 요청이 없습니다" action={<><Button variant="primary">수락</Button><Button variant="outline">거절</Button></>} />
      </div>
    </ProtectedRoute>
  )
}
