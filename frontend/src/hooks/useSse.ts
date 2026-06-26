'use client'

import { useEffect, useRef, useState, useCallback } from 'react'
import { SSE_BASE_URL, TOKEN_KEYS } from '@/utils/constants'

interface UseSseOptions<T> {
  url: string
  enabled?: boolean
  onMessage: (data: T) => void
  onError?: (error: Error) => void
}

export function useSse<T>({ url, enabled = true, onMessage, onError }: UseSseOptions<T>) {
  const [connected, setConnected] = useState(false)
  const abortRef = useRef<AbortController | null>(null)
  const reconnectRef = useRef<ReturnType<typeof setTimeout> | null>(null)
  const onMessageRef = useRef(onMessage)
  onMessageRef.current = onMessage

  const connect = useCallback(async () => {
    if (reconnectRef.current) {
      clearTimeout(reconnectRef.current)
      reconnectRef.current = null
    }

    const token = localStorage.getItem(TOKEN_KEYS.ACCESS)
    abortRef.current?.abort()
    const controller = new AbortController()
    abortRef.current = controller

    try {
      const headers: Record<string, string> = { Accept: 'text/event-stream' }
      if (token) headers['Authorization'] = `Bearer ${token}`

      const response = await fetch(`${SSE_BASE_URL}${url}`, {
        headers,
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
                // JSON이 아닌 데이터(예: "connected" 초기 이벤트)는 무시
              }
            }
          }
        }
      }
    } catch (err) {
      if ((err as Error).name !== 'AbortError') onError?.(err as Error)
    } finally {
      setConnected(false)
      // 의도적 abort가 아닌 경우(타임아웃, 서버 종료 등) 5초 후 재연결
      if (!controller.signal.aborted) {
        reconnectRef.current = setTimeout(connect, 5_000)
      }
    }
  }, [url, onError])

  useEffect(() => {
    if (!enabled) return
    connect()
    return () => {
      if (reconnectRef.current) {
        clearTimeout(reconnectRef.current)
        reconnectRef.current = null
      }
      abortRef.current?.abort()
    }
  }, [enabled, connect])

  return { connected, reconnect: connect }
}
