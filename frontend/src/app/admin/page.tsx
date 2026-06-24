'use client'

import { ProtectedRoute } from '@/components/layout/AppShell'
import { Card } from '@/components/ui/Card'
import { Button } from '@/components/ui/Button'

export default function AdminDashboardPage() {
  return (
    <ProtectedRoute roles={['ADMIN']}>
      <div className="mx-auto max-w-7xl px-4 py-8">
        <h1 className="text-2xl font-bold">관리자 대시보드</h1>
        <div className="mt-6 grid gap-4 sm:grid-cols-4">
          {['대기 아이디어', '진행 펀딩', '미처리 분쟁', '정지 전문가'].map((label) => (
            <Card key={label}><p className="text-sm text-slate-500">{label}</p><p className="text-3xl font-bold">-</p></Card>
          ))}
        </div>
        <Card className="mt-8">
          <h2 className="font-semibold">아이디어 승인 대기</h2>
          <div className="mt-4 flex gap-2"><Button size="sm">승인</Button><Button size="sm" variant="danger">반려</Button></div>
        </Card>
      </div>
    </ProtectedRoute>
  )
}
