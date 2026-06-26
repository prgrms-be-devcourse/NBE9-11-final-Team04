'use client'

import { ProtectedRoute } from '@/components/layout/AppShell'
import { Card } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'

export default function ExpertProfilePage() {
  return (
    <ProtectedRoute roles={['EXPERT']}>
      <div className="mx-auto max-w-2xl px-4 py-8">
        <h1 className="text-2xl font-bold">전문가 프로필 등록</h1>
        <Card className="mt-6"><Button>프로필 등록 (API 연동 예정)</Button></Card>
      </div>
    </ProtectedRoute>
  )
}
