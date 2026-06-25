import { create } from 'zustand'
import type { User } from '@/types/user'
import { TOKEN_KEYS } from '@/utils/constants'

interface AuthState {
  user: User | null
  isAuthenticated: boolean
  hydrated: boolean
  setUser: (user: User | null) => void
  logout: () => void
  initAuth: () => boolean
}

const getTokenFromStorage = () =>
  typeof window !== 'undefined' && !!localStorage.getItem(TOKEN_KEYS.ACCESS)

export const useAuthStore = create<AuthState>((set) => ({
  user: null,
  isAuthenticated: false,
  hydrated: false,

  setUser: (user) => set({ user, isAuthenticated: !!user }),

  logout: () => {
    if (typeof window !== 'undefined') {
      localStorage.removeItem(TOKEN_KEYS.ACCESS)
      localStorage.removeItem(TOKEN_KEYS.REFRESH)
    }
    set({ user: null, isAuthenticated: false })
  },

  initAuth: () => {
    const hasToken = getTokenFromStorage()
    set({ isAuthenticated: hasToken, hydrated: true })
    return hasToken
  },
}))
