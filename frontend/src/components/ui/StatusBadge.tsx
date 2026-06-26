import { Badge } from '@/components/ui/Badge'
import { IDEA_STATUS_LABELS, type IdeaStatus } from '@/types/enums'

const statusVariants: Record<IdeaStatus, 'blue' | 'green' | 'orange' | 'red' | 'gray'> = {
  AI_PENDING: 'blue',
  EXPERT_PENDING: 'blue',
  ADMIN_PENDING: 'orange',
  OPEN: 'green',
  IN_PROGRESS: 'green',
  COMPLETED: 'gray',
  CANCELLED: 'red',
  REJECTED: 'red',
  CANCELLATION_REQUESTED: 'orange',
  SUSPENDED: 'red',
}

export function IdeaStatusBadge({ status }: { status: IdeaStatus }) {
  return <Badge variant={statusVariants[status]}>{IDEA_STATUS_LABELS[status]}</Badge>
}
