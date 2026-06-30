'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import Link from 'next/link'
import { useMutation } from '@tanstack/react-query'
import { apiClient, unwrap } from '@/api/client'
import type { ApiResponse } from '@/types/api'
import { useAuthStore } from '@/store/authStore'
import { ProtectedRoute } from '@/components/layout/AppShell'
import { getErrorMessage } from '@/utils/format'

type QualificationType = 'BUSINESS_REGISTRATION' | 'NATIONAL_QUALIFICATION'

interface ExpertVerifyRequest {
  qualificationType: QualificationType
  qualificationNumber: string
  startDate?: string
  representativeName?: string
}

interface ExpertVerifyResponse {
  id: number
  qualificationType: QualificationType
  status: string
  verified: boolean
}

const TYPE_INFO: Record<QualificationType, { label: string; icon: string; desc: string; detail: string }> = {
  BUSINESS_REGISTRATION: {
    label: '사업자등록',
    icon: '🏢',
    desc: '국세청 API로 실시간 검증됩니다.',
    detail: '사업자등록번호, 개업일자, 대표자명을 입력하면 국세청 API를 통해 즉시 자동 검증됩니다.',
  },
  NATIONAL_QUALIFICATION: {
    label: '국가자격증',
    icon: '📋',
    desc: '관리자 검토 후 승인됩니다.',
    detail: '자격증 파일을 제출하면 관리자가 수동 검토합니다.',
  },
}

const STEPS = ['자격 검증', '프로필 등록', '전문가 활동']

function StepIndicator({ current }: { current: number }) {
  return (
    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'center', gap: '0', marginBottom: '40px' }}>
      {STEPS.map((label, i) => (
        <div key={label} style={{ display: 'flex', alignItems: 'center' }}>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '6px' }}>
            <div style={{
              width: '32px', height: '32px', borderRadius: '50%',
              background: i < current ? '#22c55e' : i === current ? 'var(--brand)' : 'var(--border)',
              color: i <= current ? '#fff' : 'var(--fg-muted)',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: '13px', fontWeight: 700,
            }}>
              {i < current ? '✓' : i + 1}
            </div>
            <span style={{ fontSize: '12px', fontWeight: i === current ? 700 : 400, color: i === current ? 'var(--brand-dark)' : 'var(--fg-muted)', whiteSpace: 'nowrap' }}>
              {label}
            </span>
          </div>
          {i < STEPS.length - 1 && (
            <div style={{ width: '80px', height: '2px', background: i < current ? '#22c55e' : 'var(--border)', margin: '0 8px', marginBottom: '18px' }} />
          )}
        </div>
      ))}
    </div>
  )
}

function ExpertApplyContent() {
  const router = useRouter()
  const { user, setUser } = useAuthStore()
  const [type, setType] = useState<QualificationType | ''>('')
  const [form, setForm] = useState({
    qualificationNumber: '',
    startDate: '',
    representativeName: '',
  })
  const [file, setFile] = useState<File | null>(null)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState(false)

  const applyMutation = useMutation({
    mutationFn: () => {
      const data: ExpertVerifyRequest = {
        qualificationType: type as QualificationType,
        qualificationNumber: form.qualificationNumber,
        ...(type === 'BUSINESS_REGISTRATION' && {
          startDate: form.startDate,
          representativeName: form.representativeName,
        }),
      }

      const formData = new FormData()
      formData.append('data', new Blob([JSON.stringify(data)], { type: 'application/json' }))
      if (file) {
        formData.append('file', file)
      }

      return unwrap(apiClient.post<ApiResponse<ExpertVerifyResponse>>('/experts/verify', formData, {
        headers: { 'Content-Type': 'multipart/form-data' },
      }))
    },
    onSuccess: (data) => {
      if (data.verified && user) setUser({ ...user, role: 'EXPERT' })
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
    if (type === 'NATIONAL_QUALIFICATION' && !file) {
      setError('자격증 파일을 첨부해주세요.'); return
    }
    applyMutation.mutate()
  }

  if (success) {
    const verified = applyMutation.data?.verified
    return (
      <div style={{ maxWidth: '560px', margin: '0 auto' }}>
        <StepIndicator current={verified ? 1 : 2} />
        <div style={{
          background: verified ? '#f0fdf4' : 'var(--brand-tint)',
          border: `1px solid ${verified ? '#86efac' : 'var(--brand)'}`,
          borderRadius: '16px', padding: '48px 40px', textAlign: 'center',
        }}>
          <div style={{ fontSize: '56px', marginBottom: '20px' }}>{verified ? '🎉' : '⏳'}</div>
          <h2 style={{ fontSize: '24px', fontWeight: 800, color: 'var(--fg)', marginBottom: '12px' }}>
            {verified ? '자격 검증 완료!' : '검토 중입니다'}
          </h2>
          <p style={{ fontSize: '15px', color: 'var(--fg-muted)', lineHeight: 1.7, marginBottom: '32px' }}>
            {verified
              ? '자격이 확인되었습니다.\n다음 단계에서 전문가 프로필을 등록해 활동을 시작하세요.'
              : '자격증 파일을 제출했습니다.\n관리자 검토 후 승인되면 알림을 드립니다.'}
          </p>
          {verified ? (
            <button
              onClick={() => router.push('/expert/profile')}
              style={{
                height: '52px', padding: '0 40px',
                background: 'var(--brand)', color: '#fff',
                border: 'none', borderRadius: '99px',
                fontSize: '16px', fontWeight: 700, cursor: 'pointer',
              }}
            >
              프로필 등록하기 →
            </button>
          ) : (
            <Link href="/" style={{
              display: 'inline-block', height: '52px', lineHeight: '52px', padding: '0 40px',
              background: 'var(--brand)', color: '#fff',
              borderRadius: '99px', fontSize: '16px', fontWeight: 700, textDecoration: 'none',
            }}>
              홈으로 이동
            </Link>
          )}
        </div>
      </div>
    )
  }

  return (
    <div style={{ maxWidth: '600px', margin: '0 auto' }}>
      <StepIndicator current={0} />

      <div style={{ marginBottom: '32px', textAlign: 'center' }}>
        <h1 style={{ fontSize: '28px', fontWeight: 800, color: 'var(--fg)', marginBottom: '10px' }}>전문가 자격 검증</h1>
        <p style={{ fontSize: '15px', color: 'var(--fg-muted)', lineHeight: 1.6 }}>
          보유하신 자격 유형을 선택하고 정보를 입력해주세요.<br />
          검증 완료 후 전문가 활동이 가능합니다.
        </p>
      </div>

      <form onSubmit={handleSubmit} style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>

        {/* 검증 유형 선택 */}
        <div>
          <label style={{ display: 'block', fontSize: '14px', fontWeight: 600, color: 'var(--fg)', marginBottom: '12px' }}>
            검증 유형 선택
          </label>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '12px' }}>
            {(Object.keys(TYPE_INFO) as QualificationType[]).map((t) => (
              <button
                key={t}
                type="button"
                onClick={() => setType(t)}
                style={{
                  border: `2px solid ${type === t ? 'var(--brand)' : 'var(--border)'}`,
                  borderRadius: '14px', padding: '20px 16px',
                  background: type === t ? 'var(--brand-tint)' : '#fff',
                  textAlign: 'center', cursor: 'pointer',
                  display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '10px',
                  transition: 'all 0.2s',
                }}
              >
                <span style={{ fontSize: '32px' }}>{TYPE_INFO[t].icon}</span>
                <div style={{ fontSize: '15px', fontWeight: 700, color: 'var(--fg)' }}>
                  {TYPE_INFO[t].label}
                </div>
                <div style={{ fontSize: '12px', color: 'var(--fg-muted)', lineHeight: 1.5 }}>
                  {TYPE_INFO[t].desc}
                </div>
              </button>
            ))}
          </div>
          {type && (
            <p style={{ fontSize: '13px', color: 'var(--brand-dark)', background: 'var(--brand-tint)', padding: '10px 14px', borderRadius: '8px', marginTop: '10px', lineHeight: 1.6 }}>
              ℹ️ {TYPE_INFO[type].detail}
            </p>
          )}
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
                width: '100%', height: '48px', border: '1.5px solid var(--border)',
                borderRadius: '10px', padding: '0 14px', fontSize: '15px',
                fontFamily: 'inherit', outline: 'none', color: 'var(--fg)', boxSizing: 'border-box',
              }}
            />
          </div>
        )}

        {/* 사업자등록 추가 필드 */}
        {type === 'BUSINESS_REGISTRATION' && (
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
            <div>
              <label style={{ display: 'block', fontSize: '14px', fontWeight: 600, color: 'var(--fg)', marginBottom: '6px' }}>
                개업일자
              </label>
              <input
                value={form.startDate}
                onChange={(e) => setForm({ ...form, startDate: e.target.value })}
                placeholder="YYYYMMDD"
                maxLength={8}
                style={{
                  width: '100%', height: '48px', border: '1.5px solid var(--border)',
                  borderRadius: '10px', padding: '0 14px', fontSize: '15px',
                  fontFamily: 'inherit', outline: 'none', color: 'var(--fg)', boxSizing: 'border-box',
                }}
              />
              <p style={{ fontSize: '12px', color: 'var(--fg-muted)', marginTop: '4px' }}>예: 20230101</p>
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
                  width: '100%', height: '48px', border: '1.5px solid var(--border)',
                  borderRadius: '10px', padding: '0 14px', fontSize: '15px',
                  fontFamily: 'inherit', outline: 'none', color: 'var(--fg)', boxSizing: 'border-box',
                }}
              />
            </div>
          </div>
        )}

        {/* 국가자격증 파일 첨부 */}
        {type === 'NATIONAL_QUALIFICATION' && (
          <div>
            <label style={{ display: 'block', fontSize: '14px', fontWeight: 600, color: 'var(--fg)', marginBottom: '6px' }}>
              자격증 파일 첨부
            </label>
            <input
              type="file"
              accept=".pdf,.jpg,.jpeg,.png,.doc,.docx"
              onChange={(e) => setFile(e.target.files?.[0] ?? null)}
              style={{
                width: '100%', height: '48px', border: '1.5px solid var(--border)',
                borderRadius: '10px', padding: '10px 14px', fontSize: '15px',
                fontFamily: 'inherit', outline: 'none', color: 'var(--fg)', boxSizing: 'border-box',
              }}
            />
            <p style={{ fontSize: '12px', color: 'var(--fg-muted)', marginTop: '4px' }}>
              자격증 사본 파일을 첨부해주세요. (PDF, 이미지, 문서 파일)
            </p>
          </div>
        )}

        {error && (
          <p style={{ fontSize: '14px', color: 'var(--error)', background: '#fff5f5', padding: '12px 16px', borderRadius: '10px' }}>
            {error}
          </p>
        )}

        {type && (
          <button
            type="submit"
            disabled={applyMutation.isPending}
            style={{
              height: '56px', fontSize: '17px', fontWeight: 700,
              background: applyMutation.isPending ? 'var(--brand-tint)' : 'var(--brand)',
              color: applyMutation.isPending ? 'var(--brand)' : '#fff',
              border: 'none', borderRadius: '12px',
              cursor: applyMutation.isPending ? 'not-allowed' : 'pointer',
              fontFamily: 'inherit', transition: 'background 0.2s',
            }}
          >
            {applyMutation.isPending ? '검증 중...' : '자격 검증 시작하기'}
          </button>
        )}
      </form>
    </div>
  )
}

export default function ExpertApplyPage() {
  return (
    <ProtectedRoute roles={['USER']}>
      <div style={{ minHeight: '80vh', padding: '60px 24px' }}>
        <div style={{ maxWidth: '680px', margin: '0 auto' }}>
          <Link href="/" style={{ display: 'inline-flex', alignItems: 'center', gap: '6px', fontSize: '14px', color: 'var(--fg-muted)', textDecoration: 'none', marginBottom: '32px' }}>
            ← 홈으로
          </Link>
          <ExpertApplyContent />
        </div>
      </div>
    </ProtectedRoute>
  )
}