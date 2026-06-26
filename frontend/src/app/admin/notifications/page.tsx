'use client'

import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { apiClient, unwrap } from '@/api/client'
import type { ApiResponse } from '@/types/api'

type TargetRole = 'USER' | 'EXPERT' | 'ADMIN' | null

interface SendNotificationRequest {
  targetRole: TargetRole
  title: string
  message: string
}

const TARGET_TABS: { value: TargetRole; label: string }[] = [
  { value: null,     label: '전체' },
  { value: 'USER',   label: '일반 사용자' },
  { value: 'EXPERT', label: '전문가' },
  { value: 'ADMIN',  label: '관리자' },
]

const adminNotificationsApi = {
  send: (body: SendNotificationRequest) =>
    unwrap(apiClient.post<ApiResponse<void>>('/admin/notifications', body)),
}

export default function AdminNotificationsPage() {
  const [targetRole, setTargetRole] = useState<TargetRole>(null)
  const [title, setTitle] = useState('')
  const [message, setMessage] = useState('')

  const sendMutation = useMutation({
    mutationFn: () =>
      adminNotificationsApi.send({ targetRole, title, message }),
    onSuccess: () => {
      alert('공지가 발송되었습니다.')
      setTargetRole(null)
      setTitle('')
      setMessage('')
    },
    onError: () => alert('공지 발송 중 오류가 발생했습니다.'),
  })

  const handleSubmit = () => {
    if (!title.trim()) {
      alert('제목을 입력해주세요.')
      return
    }
    if (!message.trim()) {
      alert('내용을 입력해주세요.')
      return
    }
    sendMutation.mutate()
  }

  const isPending = sendMutation.isPending

  return (
    <>
      <div style={{ marginBottom: '20px' }}>
        <h1 style={{ fontSize: '22px', fontWeight: 800, color: 'var(--fg)', marginBottom: '4px' }}>
          공지 발송
        </h1>
        <p style={{ fontSize: '14px', color: 'var(--fg-muted)' }}>
          대상 그룹을 선택하고 공지를 발송합니다.
        </p>
      </div>

      <div style={{
        background: '#fff',
        border: '1px solid var(--border)',
        borderRadius: '16px',
        padding: '28px',
        display: 'flex',
        flexDirection: 'column',
        gap: '24px',
      }}>

        {/* 대상 선택 */}
        <div>
          <label style={{ display: 'block', fontSize: '14px', fontWeight: 600, color: 'var(--fg)', marginBottom: '10px' }}>
            발송 대상
          </label>
          <div style={{
            display: 'flex',
            gap: '4px',
            background: 'var(--bg-alt)',
            borderRadius: '10px',
            padding: '4px',
          }}>
            {TARGET_TABS.map((tab) => {
              const isActive = targetRole === tab.value
              return (
                <button
                  key={String(tab.value)}
                  onClick={() => setTargetRole(tab.value)}
                  style={{
                    flex: 1,
                    padding: '8px 12px',
                    borderRadius: '8px',
                    border: 'none',
                    background: isActive ? '#fff' : 'transparent',
                    boxShadow: isActive ? '0 1px 3px rgba(0,0,0,0.08)' : 'none',
                    fontWeight: isActive ? 700 : 500,
                    fontSize: '13px',
                    color: isActive ? 'var(--brand-dark)' : 'var(--fg-muted)',
                    cursor: 'pointer',
                    transition: 'all 0.15s',
                  }}
                >
                  {tab.label}
                </button>
              )
            })}
          </div>
        </div>

        {/* 제목 */}
        <div>
          <label style={{ display: 'block', fontSize: '14px', fontWeight: 600, color: 'var(--fg)', marginBottom: '8px' }}>
            제목
          </label>
          <input
            type="text"
            value={title}
            onChange={(e) => setTitle(e.target.value)}
            placeholder="공지 제목을 입력하세요"
            style={{
              width: '100%',
              padding: '10px 14px',
              border: '1.5px solid var(--border)',
              borderRadius: '10px',
              fontSize: '14px',
              color: 'var(--fg)',
              background: '#fff',
              outline: 'none',
              boxSizing: 'border-box',
              transition: 'border-color 0.15s',
            }}
            onFocus={(e) => { e.currentTarget.style.borderColor = 'var(--brand)' }}
            onBlur={(e) => { e.currentTarget.style.borderColor = 'var(--border)' }}
          />
        </div>

        {/* 내용 */}
        <div>
          <label style={{ display: 'block', fontSize: '14px', fontWeight: 600, color: 'var(--fg)', marginBottom: '8px' }}>
            내용
          </label>
          <textarea
            rows={5}
            value={message}
            onChange={(e) => setMessage(e.target.value)}
            placeholder="공지 내용을 입력하세요"
            style={{
              width: '100%',
              padding: '10px 14px',
              border: '1.5px solid var(--border)',
              borderRadius: '10px',
              fontSize: '14px',
              color: 'var(--fg)',
              background: '#fff',
              outline: 'none',
              resize: 'vertical',
              boxSizing: 'border-box',
              lineHeight: '1.6',
              transition: 'border-color 0.15s',
            }}
            onFocus={(e) => { e.currentTarget.style.borderColor = 'var(--brand)' }}
            onBlur={(e) => { e.currentTarget.style.borderColor = 'var(--border)' }}
          />
        </div>

        {/* 발송 버튼 */}
        <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
          <button
            onClick={handleSubmit}
            disabled={isPending}
            style={{
              padding: '12px 32px',
              borderRadius: '10px',
              border: 'none',
              background: isPending ? 'var(--fg-muted)' : 'var(--brand)',
              color: '#fff',
              fontSize: '15px',
              fontWeight: 700,
              cursor: isPending ? 'not-allowed' : 'pointer',
              opacity: isPending ? 0.7 : 1,
              transition: 'all 0.15s',
            }}
          >
            {isPending ? '발송 중...' : '📢 공지 발송'}
          </button>
        </div>
      </div>
    </>
  )
}
