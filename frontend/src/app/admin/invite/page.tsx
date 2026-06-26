'use client'

import { useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { authApi } from '@/api/auth'
import { getErrorMessage } from '@/utils/format'

export default function AdminInvitePage() {
  const [email, setEmail] = useState('')
  const [sent, setSent] = useState(false)
  const [error, setError] = useState('')

  const inviteMutation = useMutation({
    mutationFn: () => authApi.adminInvite(email),
    onSuccess: () => {
      setSent(true)
      setError('')
    },
    onError: (err) => setError(getErrorMessage(err)),
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    setSent(false)
    inviteMutation.mutate()
  }

  return (
    <>
      <div style={{ marginBottom: '24px' }}>
        <h1 style={{ fontSize: '22px', fontWeight: 800, color: 'var(--fg)', marginBottom: '4px' }}>
          관리자 초대
        </h1>
        <p style={{ fontSize: '14px', color: 'var(--fg-muted)' }}>
          입력한 이메일로 관리자 가입 초대 링크를 발송합니다.
        </p>
      </div>

      <div style={{
        background: '#fff',
        border: '1px solid var(--border)',
        borderRadius: '16px',
        padding: '32px',
        maxWidth: '520px',
      }}>
        <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
          <div>
            <label style={{ display: 'block', fontSize: '14px', fontWeight: 600, color: 'var(--fg)', marginBottom: '8px' }}>
              초대할 이메일 주소
            </label>
            <input
              type="email"
              value={email}
              onChange={(e) => { setEmail(e.target.value); setSent(false) }}
              placeholder="admin@seedlink.com"
              required
              style={{
                width: '100%',
                height: '46px',
                border: '1.5px solid var(--border)',
                borderRadius: '10px',
                padding: '0 14px',
                fontSize: '15px',
                fontFamily: 'inherit',
                outline: 'none',
                color: 'var(--fg)',
                boxSizing: 'border-box',
                transition: 'border-color 0.2s',
              }}
              onFocus={(e) => { e.target.style.borderColor = 'var(--brand)' }}
              onBlur={(e) => { e.target.style.borderColor = 'var(--border)' }}
            />
          </div>

          {sent && (
            <div style={{
              padding: '14px 16px',
              background: '#f0fdf4',
              border: '1px solid #86efac',
              borderRadius: '10px',
              fontSize: '14px',
              color: '#1a7a3f',
            }}>
              ✅ <strong>{email}</strong>로 초대 링크를 발송했습니다.
            </div>
          )}

          {error && (
            <div style={{
              padding: '14px 16px',
              background: '#fff5f5',
              border: '1px solid #fecaca',
              borderRadius: '10px',
              fontSize: '14px',
              color: 'var(--error)',
            }}>
              {error}
            </div>
          )}

          <button
            type="submit"
            disabled={inviteMutation.isPending}
            style={{
              height: '48px',
              fontSize: '15px',
              fontWeight: 700,
              background: inviteMutation.isPending ? 'var(--brand-tint)' : 'var(--brand)',
              color: inviteMutation.isPending ? 'var(--brand)' : '#fff',
              border: 'none',
              borderRadius: '10px',
              cursor: inviteMutation.isPending ? 'not-allowed' : 'pointer',
              fontFamily: 'inherit',
              transition: 'background 0.2s',
            }}
          >
            {inviteMutation.isPending ? '발송 중...' : '초대 이메일 발송'}
          </button>
        </form>

        <div style={{
          marginTop: '24px',
          padding: '16px',
          background: 'var(--bg-alt)',
          borderRadius: '10px',
          fontSize: '13px',
          color: 'var(--fg-muted)',
          lineHeight: 1.7,
        }}>
          <strong style={{ color: 'var(--fg)', display: 'block', marginBottom: '6px' }}>초대 안내</strong>
          <ul style={{ margin: 0, paddingLeft: '18px' }}>
            <li>수신자는 이메일의 링크를 통해 관리자 계정을 생성할 수 있습니다.</li>
            <li>초대 링크는 일정 시간 후 만료됩니다.</li>
            <li>초대 링크는 한 번만 사용 가능합니다.</li>
          </ul>
        </div>
      </div>
    </>
  )
}
