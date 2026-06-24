export interface ApiResponse<T> {
  success: boolean
  code: string | null
  message: string | null
  data: T
}

export interface PageResponse<T> {
  content: T[]
  totalElements: number
  totalPages: number
  size: number
  number: number
  first: boolean
  last: boolean
}

export interface SliceResponse<T> {
  content: T[]
  number: number
  size: number
  first: boolean
  last: boolean
}
