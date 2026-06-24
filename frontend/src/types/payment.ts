import type { PaymentStatus } from './enums'

export interface Payment {
  paymentId: number
  fundingId: number
  orderId: string
  amount: number
  status: PaymentStatus
  method: string
  approvedAt: string | null
  createdAt: string
}
