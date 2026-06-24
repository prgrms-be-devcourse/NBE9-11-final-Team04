import type { IdeaCategory, IdeaStatus, RewardType } from './enums'

export interface IdeaSummary {
  ideaId: number
  userId: number
  title: string
  category: IdeaCategory
  oneLineIntro: string
  goalAmount: number
  currentAmount: number
  supporterCount: number
  fundingStartAt: string
  fundingEndAt: string
  status: IdeaStatus
  createdAt: string
}

export interface IdeaDetail {
  ideaId: number
  userId: number
  title: string
  category: IdeaCategory
  oneLineIntro: string
  problemDefinition: string
  solution: string
  goal: string
  targetCustomer: string
  competitor: string
  teamIntro: string
  goalAmount: number
  currentAmount: number
  supporterCount: number
  fundingStartAt: string
  fundingEndAt: string
  rewardType: RewardType
  status: IdeaStatus
  trustScore: number | null
  badge: string
  createdAt: string
  updatedAt: string
}

export interface CreateMilestoneRequest {
  step: number
  goal: string
  expectedResult: string
  expectedDate: string  // LocalDate: "YYYY-MM-DD"
}

export interface CreateIdeaRequest {
  title: string
  category: IdeaCategory
  oneLineIntro: string
  problemDefinition: string
  solution: string
  goal: string
  targetCustomer: string
  competitor: string
  teamIntro: string
  goalAmount: number
  fundingStartAt: string
  fundingEndAt: string
  rewardType: RewardType
  milestones: CreateMilestoneRequest[]
}

export interface IdeaDraft {
  draftId: number
  userId: number
  title: string | null
  category: IdeaCategory | null
  oneLineIntro: string | null
  problemDefinition: string | null
  solution: string | null
  goal: string | null
  targetCustomer: string | null
  competitor: string | null
  teamIntro: string | null
  goalAmount: number | null
  fundingStartAt: string | null
  fundingEndAt: string | null
  rewardType: RewardType | null
  createdAt: string
  updatedAt: string
}

export interface TrustScoreResponse {
  trustScore: number
  badge: string
  breakdown: Record<string, number>
}

export interface IdeaListParams {
  category?: IdeaCategory
  closingSoon?: boolean
  sort?: 'latest' | 'popular' | 'deadline'
  page?: number
  size?: number
  keyword?: string
}
