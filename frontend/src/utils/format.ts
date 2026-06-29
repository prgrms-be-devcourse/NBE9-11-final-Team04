export function formatCurrency(amount: number): string {
  return new Intl.NumberFormat('ko-KR', {
    style: 'currency',
    currency: 'KRW',
    maximumFractionDigits: 0,
  }).format(amount)
}

export function formatDate(date: string): string {
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'long',
    day: 'numeric',
  }).format(new Date(date))
}

export function formatDateTime(date: string): string {
  return new Intl.DateTimeFormat('ko-KR', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  }).format(new Date(date))
}

export function calcAchievementRate(current: number, goal: number): number {
  if (goal <= 0) return 0
  return Math.round((current / goal) * 100)
}

export function getDaysRemaining(endDate: string): number {
  const end = new Date(endDate).getTime()
  const now = Date.now()
  return Math.max(0, Math.ceil((end - now) / (1000 * 60 * 60 * 24)))
}

export function getErrorMessage(error: unknown): string {
  if (error && typeof error === 'object' && 'response' in error) {
    const axiosError = error as { response?: { data?: { message?: string } } }
    return axiosError.response?.data?.message ?? '요청 처리 중 오류가 발생했습니다.'
  }
  if (error instanceof Error) return error.message
  return '알 수 없는 오류가 발생했습니다.'
}

export function getResponseCode(error: unknown): string | null {
  if (error && typeof error === 'object' && 'response' in error) {
    const axiosError = error as { response?: { data?: { code?: string } } }
    return axiosError.response?.data?.code ?? null
  }
  return null
}
