'use client'

import { useEffect, useRef, useState, useCallback } from 'react'
import { API_BASE_URL, TOKEN_KEYS } from '@/utils/constants'

interface UseSseOptions<T> {
  url: string
  enabled?: boolean
  onMessage: (data: T) => void
  onError?: (error: Error) => void
}

export function useSse<T>({ url, enabled = true, onMessage, onError }: UseSseOptions<T>) {
  const [connected, setConnected] = useState(false)
  const abortRef = useRef<AbortController | null>(null)
  const onMessageRef = useRef(onMessage)
  onMessageRef.current = onMessage

  const connect = useCallback(async () => {
    const token = localStorage.getItem(TOKEN_KEYS.ACCESS)
    if (!token) return

    abortRef.current?.abort()
    const controller = new AbortController()
    abortRef.current = controller

    try {
      const response = await fetch(`${API_BASE_URL}${url}`, {
        headers: { Authorization: `Bearer ${token}`, Accept: 'text/event-stream' },
        credentials: 'include',
        signal: controller.signal,
      })

      if (!response.ok || !response.body) throw new Error('SSE 연결에 실패했습니다.')

      setConnected(true)
      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        if (done) break
        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() ?? ''
        for (const line of lines) {
          if (line.startsWith('data:')) {
            const raw = line.slice(5).trim()
            if (raw && raw !== '[DONE]') {
              try {
                onMessageRef.current(JSON.parse(raw) as T)
              } catch {
                onMessageRef.current(raw as T)
              }
            }
          }
        }
      }
    } catch (err) {
      if ((err as Error).name !== 'AbortError') onError?.(err as Error)
    } finally {
      setConnected(false)
    }
  }, [url, onError])

  useEffect(() => {
    if (!enabled) return
    connect()
    return () => abortRef.current?.abort()
  }, [enabled, connect])

  return { connected, reconnect: connect }
}
