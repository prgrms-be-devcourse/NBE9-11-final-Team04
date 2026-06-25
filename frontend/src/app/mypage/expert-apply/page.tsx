'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { useMutation } from '@tanstack/react-query'
import { apiClient, unwrap } from '@/api/client'
import type { ApiResponse } from '@/types/api'
import { useAuthStore } from '@/store/authStore'
import { getErrorMessage } from '@/utils/format'

type QualificationType = 'BUSINESS_REGISTRATION' | 'NATIONAL_QUALIFICATION'

interface ExpertVerifyRequest {
  qualificationType: QualificationType
  qualificationNumber: string
  startDate?: string
  representativeName?: string
  fileUrl?: string
}

interface ExpertVerifyResponse {
  id: number
  qualificationType: QualificationType
  status: string
  verified: boolean
}

const TYPE_INFO: Record<QualificationType, { label: string; icon: string; desc: string }> = {
  BUSINESS_REGISTRATION: {
    label: '사업자등록',
    icon: '🏢',
    desc: '국세청 API로 실시간 검증됩니다. 승인까지 수 초가 소요됩니다.',
  },
  NATIONAL_QUALIFICATION: {
    label: '국가자격증',
    icon: '📋',
    desc: '자격증 파일을 제출하면 관리자 수동 검토 후 승인됩니다.',
  },
}

export default function ExpertApplyPage() {
  const router = useRouter()
  const { user, setUser } = useAuthStore()
  const [type, setType] = useState<QualificationType | ''>('')
  const [form, setForm] = useState({
    qualificationNumber: '',
    startDate: '',
    representativeName: '',
    fileUrl: '',
  })
  const [error, setError] = useState('')
  const [success, setSuccess] = useState(false)

  const applyMutation = useMutation({
    mutationFn: () => {
      const body: ExpertVerifyRequest = {
        qualificationType: type as QualificationType,
        qualificationNumber: form.qualificationNumber,
        ...(type === 'BUSINESS_REGISTRATION' && {
          startDate: form.startDate,
          representativeName: form.representativeName,
        }),
        ...(type === 'NATIONAL_QUALIFICATION' && {
          fileUrl: form.fileUrl,
        }),
      }
      return unwrap(apiClient.post<ApiResponse<ExpertVerifyResponse>>('/experts/verify', body))
    },
    onSuccess: (data) => {
      if (data.verified) {
        // 즉시 승인 → role이 EXPERT로 바뀌었으므로 유저 정보 갱신
        if (user) setUser({ ...user, role: 'EXPERT' })
      }
      setSuccess(true)
      setError('')
    },
    onError: (err) => setError(getErrorMessage(err)),
  })

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    setError('')
    if (!type) { setError('검증 유형을 선택해주세요.'); return }
    if (!form.qualificationNumber.trim()) { setError('번호를 입력해주세요.'); return }
    if (type === 'BUSINESS_REGISTRATION') {
      if (!form.startDate.trim()) { setError('개업일자를 입력해주세요.'); return }
      if (!form.representativeName.trim()) { setError('대표자명을 입력해주세요.'); return }
    }
    if (type === 'NATIONAL_QUALIFICATION' && !form.fileUrl.trim()) {
      setError('자격증 파일 URL을 입력해주세요.'); return
    }
    applyMutation.mutate()
  }

  if (success) {
    const verified = applyMutation.data?.verified
    return (
      <div style={{ maxWidth: '560px' }}>
        <div style={{
          background: verified ? '#f0fdf4' : 'var(--brand-tint)',
          border: `1px solid ${verified ? '#86efac' : 'var(--brand)'}`,
          borderRadius: '16px', padding: '40px 36px', textAlign: 'center',
        }}>
          <div style={{ fontSize: '48px', marginBottom: '16px' }}>{verified ? '🎉' : '⏳'}</div>
          <h2 style={{ fontSize: '22px', fontWeight: 800, color: 'var(--fg)', marginBottom: '10px' }}>
            {verified ? '전문가 승인 완료!' : '검토 중입니다'}
          </h2>
          <p style={{ fontSize: '15px', color: 'var(--fg-muted)', lineHeight: 1.7 }}>
            {verified
              ? '자격 검증이 완료되었습니다.\n이제 전문가로 활동할 수 있습니다.'
              : '자격증 파일을 제출했습니다.\n관리자 검토 후 승인되면 알림을 드립니다.'}
          </p>
          <button
            onClick={() => router.push(verified ? '/expert/profile' : '/')}
            style={{
              marginTop: '24px', height: '48px', padding: '0 32px',
              background: 'var(--brand)', color: '#fff',
              border: 'none', borderRadius: '99px',
              fontSize: '15px', fontWeight: 700, cursor: 'pointer',
            }}
          >
            {verified ? '전문가 프로필 등록하기 →' : '홈으로 이동'}
          </button>
        </div>
      </div>
    )
  }

  return (
    <div style={{ maxWidth: '560px' }}>
      <div style={{ marginBottom: '28px' }}>
        <h1 style={{ fontSize: '24px', fontWeight: 800, color: 'var(--fg)', marginBottom: '8px' }}>전문가 신청</h1>
        <p style={{ fontSize: '14px', color: 'var(--fg-muted)', lineHeight: 1.6 }}>
          자격을 검증하면 전문가로 전환되어 아이디어 검토 활동에 참여할 수 있습니다.
        </p>
      </div>

      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>

        {/* 검증 유형 선택 */}
        <div>
          <label style={{ display: 'block', fontSize: '14px', fontWeight: 600, color: 'var(--fg)', marginBottom: '10px' }}>
            검증 유형
          </label>
          <div style={{ display: 'flex', flexDirection: 'column', gap: '10px' }}>
            {(Object.keys(TYPE_INFO) as QualificationType[]).map((t) => (
              <button
                key={t}
                type="button"
                onClick={() => setType(t)}
                style={{
                  border: `1.5px solid ${type === t ? 'var(--brand)' : 'var(--border)'}`,
                  borderRadius: '12px', padding: '16px 18px',
                  background: type === t ? 'var(--brand-tint)' : '#fff',
                  textAlign: 'left', cursor: 'pointer',
                  display: 'flex', alignItems: 'flex-start', gap: '14px',
                  transition: 'all 0.2s',
                }}
              >
                <span style={{ fontSize: '24px', marginTop: '2px' }}>{TYPE_INFO[t].icon}</span>
                <div>
                  <div style={{ fontSize: '15px', fontWeight: 700, color: 'var(--fg)', marginBottom: '4px' }}>
                    {TYPE_INFO[t].label}
                  </div>
                  <div style={{ fontSize: '13px', color: 'var(--fg-muted)', lineHeight: 1.5 }}>
                    {TYPE_INFO[t].desc}
                  </div>
                </div>
              </button>
            ))}
          </div>
        </div>

        {/* 공통 입력: 번호 */}
        {type && (
          <div>
            <label style={{ display: 'block', fontSize: '14px', fontWeight: 600, color: 'var(--fg)', marginBottom: '6px' }}>
              {type === 'BUSINESS_REGISTRATION' ? '사업자등록번호' : '자격증 번호'}
            </label>
            <input
              value={form.qualificationNumber}
              onChange={(e) => setForm({ ...form, qualificationNumber: e.target.value })}
              placeholder={type === 'BUSINESS_REGISTRATION' ? '000-00-00000' : '자격증 번호 입력'}
              style={{
                width: '100%', height: '44px', border: '1.5px solid var(--border)',
                borderRadius: '10px', padding: '0 14px', fontSize: '14px',
                fontFamily: 'inherit', outline: 'none', color: 'var(--fg)', boxSizing: 'border-box',
              }}
            />
          </div>
        )}

        {/* 사업자등록 추가 필드 */}
        {type === 'BUSINESS_REGISTRATION' && (
          <>
            <div>
              <label style={{ display: 'block', fontSize: '14px', fontWeight: 600, color: 'var(--fg)', marginBottom: '6px' }}>
                개업일자
              </label>
              <input
                value={form.startDate}
                onChange={(e) => setForm({ ...form, startDate: e.target.value })}
                placeholder="YYYYMMDD (예: 20230101)"
                maxLength={8}
                style={{
                  width: '100%', height: '44px', border: '1.5px solid var(--border)',
                  borderRadius: '10px', padding: '0 14px', fontSize: '14px',
                  fontFamily: 'inherit', outline: 'none', color: 'var(--fg)', boxSizing: 'border-box',
                }}
              />
            </div>
            <div>
              <label style={{ display: 'block', fontSize: '14px', fontWeight: 600, color: 'var(--fg)', marginBottom: '6px' }}>
                대표자명
              </label>
              <input
                value={form.representativeName}
                onChange={(e) => setForm({ ...form, representativeName: e.target.value })}
                placeholder="홍길동"
                style={{
                  width: '100%', height: '44px', border: '1.5px solid var(--border)',
                  borderRadius: '10px', padding: '0 14px', fontSize: '14px',
                  fontFamily: 'inherit', outline: 'none', color: 'var(--fg)', boxSizing: 'border-box',
                }}
              />
            </div>
          </>
        )}

        {/* 국가자격증 파일 URL */}
        {type === 'NATIONAL_QUALIFICATION' && (
          <div>
            <label style={{ display: 'block', fontSize: '14px', fontWeight: 600, color: 'var(--fg)', marginBottom: '6px' }}>
              자격증 파일 URL
            </label>
            <input
              type="url"
              value={form.fileUrl}
              onChange={(e) => setForm({ ...form, fileUrl: e.target.value })}
              placeholder="https://..."
              style={{
                width: '100%', height: '44px', border: '1.5px solid var(--border)',
                borderRadius: '10px', padding: '0 14px', fontSize: '14px',
                fontFamily: 'inherit', outline: 'none', color: 'var(--fg)', boxSizing: 'border-box',
              }}
            />
            <p style={{ fontSize: '12px', color: 'var(--fg-muted)', marginTop: '4px' }}>
              S3 등에 업로드 후 URL을 입력해주세요.
            </p>
          </div>
        )}

        {error && (
          <p style={{ fontSize: '14px', color: 'var(--error)', background: '#fff5f5', padding: '10px 14px', borderRadius: '10px' }}>
            {error}
          </p>
        )}

        {type && (
          <button
            type="submit"
            disabled={applyMutation.isPending}
            style={{
              height: '52px', fontSize: '16px', fontWeight: 700,
              background: applyMutation.isPending ? 'var(--brand-tint)' : 'var(--brand)',
              color: applyMutation.isPending ? 'var(--brand)' : '#fff',
              border: 'none', borderRadius: '12px',
              cursor: applyMutation.isPending ? 'not-allowed' : 'pointer',
              fontFamily: 'inherit', transition: 'background 0.2s',
            }}
          >
            {applyMutation.isPending ? '검증 중...' : '전문가 신청하기'}
          </button>
        )}
      </form>
    </div>
  )
}
