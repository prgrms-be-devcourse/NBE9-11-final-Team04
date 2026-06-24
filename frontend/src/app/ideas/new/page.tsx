'use client'

import { Suspense } from 'react'
import { ProtectedRoute } from '@/components/layout/AppShell'
import IdeaFormView from '@/components/views/IdeaFormView'

export default function NewIdeaPage() {
  return (
    <ProtectedRoute roles={['PROPOSER']}>
      <Suspense>
        <IdeaFormView />
      </Suspense>
    </ProtectedRoute>
  )
}
