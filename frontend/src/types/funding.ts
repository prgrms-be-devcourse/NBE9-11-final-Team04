export interface Funding {
  fundingId: number
  ideaId: number
  sponsorId: number
  amount: number
  status: string
  createdAt: string
}

export interface FundingDetail extends Funding {
  ideaTitle?: string
  goalAmount?: number
  currentAmount?: number
  supporterCount?: number
  fundingEndAt?: string
}

export type PaymentMethod = 'CARD' | 'VIRTUAL_ACCOUNT'

export interface SponsorRequest {
  amount: number
  paymentMethod: PaymentMethod
}

export interface Milestone {
  milestoneId: number
  title: string
  description: string
  targetAmount: number
  status: string
  dueDate: string
}

export interface FundingProgressEvent {
  ideaId: number
  goalAmount: number
  currentAmount: number
  achievementRate: number
  sponsorCount: number
}

export interface VbankInfo {
  bankCode: string
  accountNumber: string
  dueDate: string
}

export interface PaymentResult {
  paymentId: number
  fundingId: number
  orderId: string
  amount: number
  status: string
  method: string
  approvedAt: string | null
  createdAt: string
  clientKey: string | null
  redirectUrl: string | null
  vbank: VbankInfo | null
}

export interface CreateFundingResponse {
  fundingId: number
  ideaId: number
  sponsorId: number
  milestoneStep: number
  amount: number
  rewardType: string
  fundingStatus: string
  createdAt: string
  payment: PaymentResult
}

export interface DepositResponse {
  depositId: number | null
  ideaId: number
  userId: number
  amount: number
  status: string
  paidAt: string | null
  releasedAt: string | null
  payment: PaymentResult
}
