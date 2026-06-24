export type Role = 'PROPOSER' | 'EXPERT' | 'SPONSOR' | 'ADMIN'

export type IdeaStatus =
  | 'AI_PENDING'
  | 'EXPERT_PENDING'
  | 'ADMIN_PENDING'
  | 'OPEN'
  | 'IN_PROGRESS'
  | 'COMPLETED'
  | 'CANCELLED'

export type IdeaCategory =
  | 'TECH'
  | 'LIFE'
  | 'HEALTH'
  | 'EDUCATION'
  | 'ENVIRONMENT'
  | 'CULTURE'
  | 'ETC'

export type RewardType = 'REWARD_POINT' | 'FIRST_COME' | 'PAYBACK'

export const REWARD_TYPE_LABELS: Record<RewardType, string> = {
  REWARD_POINT: '포인트 지급',
  FIRST_COME:   '선착순 혜택',
  PAYBACK:      '페이백',
}

export type PaymentStatus = 'PENDING' | 'SUCCESS' | 'REFUNDED' | 'FAILED'

export type DisputeStatus = 'RECEIVED' | 'PENDING' | 'RESOLVED' | 'REJECTED'

export type UserStatus = 'ACTIVE' | 'SUSPENDED' | 'DELETED'

export const IDEA_STATUS_LABELS: Record<IdeaStatus, string> = {
  AI_PENDING: 'AI 검증 대기',
  EXPERT_PENDING: '전문가 검증 대기',
  ADMIN_PENDING: '관리자 승인 대기',
  OPEN: '펀딩 진행중',
  IN_PROGRESS: '사업 진행중',
  COMPLETED: '완료',
  CANCELLED: '취소됨',
}

export const IDEA_CATEGORY_LABELS: Record<IdeaCategory, string> = {
  TECH: '기술',
  LIFE: '생활',
  HEALTH: '건강',
  EDUCATION: '교육',
  ENVIRONMENT: '환경',
  CULTURE: '문화',
  ETC: '기타',
}

export const PAYMENT_STATUS_LABELS: Record<PaymentStatus, string> = {
  PENDING: '결제 대기',
  SUCCESS: '결제 완료',
  REFUNDED: '환불됨',
  FAILED: '결제 실패',
}

export const DISPUTE_STATUS_LABELS: Record<DisputeStatus, string> = {
  RECEIVED: '접수됨',
  PENDING: '검토중',
  RESOLVED: '해결됨',
  REJECTED: '반려됨',
}

export const ROLE_LABELS: Record<Role, string> = {
  PROPOSER: '제안자',
  EXPERT: '전문가',
  SPONSOR: '스폰서',
  ADMIN: '관리자',
}
