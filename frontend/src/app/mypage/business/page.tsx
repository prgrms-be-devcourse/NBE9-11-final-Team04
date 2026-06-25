'use client'

import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { usersApi } from '@/api/users'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { LoadingSpinner } from '@/components/ui/LoadingSpinner'
import { getErrorMessage } from '@/utils/format'

export default function BusinessPage() {
  const queryClient = useQueryClient()
  const [form, setForm] = useState({ businessNumber: '', representativeName: '', openDate: '' })
  const [error, setError] = useState('')

  const { data: business, isLoading } = useQuery({
    queryKey: ['users', 'me', 'business'],
    queryFn: usersApi.getMyBusiness,
    retry: false,
  })

  const registerMutation = useMutation({
    mutationFn: () => usersApi.registerBusiness(form),
    onSuccess: () => {
      setError('')
      queryClient.invalidateQueries({ queryKey: ['users', 'me', 'business'] })
    },
    onError: (err) => setError(getErrorMessage(err)),
  })

  const deleteMutation = useMutation({
    mutationFn: usersApi.deleteMyBusiness,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: ['users', 'me', 'business'] }),
  })

  const handleRegister = () => {
    if (!/^\d{10}$/.test(form.businessNumber)) {
      setError('사업자등록번호는 하이픈 없는 10자리 숫자여야 합니다.')
      return
    }
    if (!/^\d{8}$/.test(form.openDate)) {
      setError('개업일자는 YYYYMMDD 형식의 8자리 숫자여야 합니다. (예: 20240101)')
      return
    }
    setError('')
    registerMutation.mutate()
  }

  if (isLoading) return <LoadingSpinner />

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <h2 style={{ fontSize: '20px', fontWeight: 700, color: 'var(--fg)' }}>🏢 사업자 정보</h2>

      {business ? (
        /* 등록된 사업자 정보 표시 */
        <div style={{
          background: '#fff',
          border: '1px solid var(--border)',
          borderRadius: '12px',
          padding: '24px',
          display: 'flex',
          flexDirection: 'column',
          gap: '20px',
        }}>
          <div style={{
            display: 'flex',
            alignItems: 'center',
            gap: '12px',
            padding: '16px',
            background: business.verified ? 'var(--brand-tint)' : '#fff9e6',
            border: `1px solid ${business.verified ? 'var(--brand)' : '#f0c040'}`,
            borderRadius: '10px',
          }}>
            <span style={{ fontSize: '28px' }}>{business.verified ? '✅' : '⏳'}</span>
            <div>
              <p style={{ fontSize: '15px', fontWeight: 700, color: 'var(--fg)' }}>
                {business.verified ? '인증 완료' : '인증 대기 중'}
              </p>
              <p style={{ fontSize: '13px', color: 'var(--fg-muted)' }}>
                {business.verified && business.verifiedAt
                  ? `인증일: ${new Date(business.verifiedAt).toLocaleDateString('ko-KR')}`
                  : '국세청 API를 통해 인증이 진행됩니다.'}
              </p>
            </div>
          </div>

          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
            <div>
              <p style={{ fontSize: '12px', color: 'var(--fg-muted)', marginBottom: '4px' }}>사업자등록번호</p>
              <p style={{ fontSize: '15px', fontWeight: 600, color: 'var(--fg)' }}>
                {business.businessNumber.replace(/(\d{3})(\d{2})(\d{5})/, '$1-$2-$3')}
              </p>
            </div>
          </div>

          <div style={{ borderTop: '1px solid var(--border)', paddingTop: '16px' }}>
            <Button
              variant="ghost"
              onClick={() => { if (confirm('사업자 정보를 삭제하시겠습니까?')) deleteMutation.mutate() }}
              loading={deleteMutation.isPending}
              style={{ color: 'var(--error)', borderColor: '#fde8e8' }}
            >
              사업자 정보 삭제
            </Button>
          </div>
        </div>
      ) : (
        /* 사업자 정보 등록 폼 */
        <div style={{
          background: '#fff',
          border: '1px solid var(--border)',
          borderRadius: '12px',
          padding: '24px',
          display: 'flex',
          flexDirection: 'column',
          gap: '16px',
        }}>
          <p style={{ fontSize: '14px', color: 'var(--fg-muted)', lineHeight: 1.6 }}>
            사업자 정보를 등록하면 신뢰도 점수 가점을 받을 수 있습니다.<br />
            국세청 API를 통해 실제 사업자 여부를 검증합니다.
          </p>

          <Input
            label="사업자등록번호"
            value={form.businessNumber}
            onChange={(e) => setForm({ ...form, businessNumber: e.target.value.replace(/\D/g, '') })}
            placeholder="하이픈 없이 10자리 (예: 1234567890)"
            maxLength={10}
          />
          <Input
            label="대표자명"
            value={form.representativeName}
            onChange={(e) => setForm({ ...form, representativeName: e.target.value })}
            placeholder="사업자등록증상 대표자명"
          />
          <Input
            label="개업일자"
            value={form.openDate}
            onChange={(e) => setForm({ ...form, openDate: e.target.value.replace(/\D/g, '') })}
            placeholder="YYYYMMDD 형식 8자리 (예: 20240101)"
            maxLength={8}
          />

          {error && <p style={{ fontSize: '14px', color: 'var(--error)' }}>{error}</p>}

          <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
            <Button onClick={handleRegister} loading={registerMutation.isPending}>
              등록 및 인증
            </Button>
          </div>
        </div>
      )}
    </div>
  )
}
