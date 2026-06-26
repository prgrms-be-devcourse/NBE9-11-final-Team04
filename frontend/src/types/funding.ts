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

export interface SponsorRequest {
  amount: number
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
  fundingId: number
  currentAmount: number
  goalAmount: number
  achievementRate: number
  supporterCount: number
}
