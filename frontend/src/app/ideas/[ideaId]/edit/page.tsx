'use client'

import { ProtectedRoute } from '@/components/layout/AppShell'
import IdeaFormView from '@/components/views/IdeaFormView'

export default function EditIdeaPage() {
  return (
    <ProtectedRoute roles={['USER']}>
      <IdeaFormView />
    </ProtectedRoute>
  )
}
