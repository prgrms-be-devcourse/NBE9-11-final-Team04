'use client'

import { useState } from 'react'
import { useParams, useRouter } from 'next/navigation'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { ideasApi } from '@/api/ideas'
import { fundingsApi } from '@/api/fundings'
import { paymentsApi } from '@/api/payments'
import { ProtectedRoute } from '@/components/layout/AppShell'
import { LoadingSpinner } from '@/components/ui/LoadingSpinner'
import { formatCurrency, getErrorMessage } from '@/utils/format'
import type { VbankInfo } from '@/types/funding'

function DepositPaymentForm() {
  const params = useParams()
  const router = useRouter()
  const queryClient = useQueryClient()
  const ideaId = Number(params.ideaId)

  const [paymentMethod, setPaymentMethod] = useState<'CARD' | 'VIRTUAL_ACCOUNT'>('CARD')
  const [error, setError] = useState('')
  const [vbankInfo, setVbankInfo] = useState<VbankInfo | null>(null)
  const [depositDone, setDepositDone] = useState(false)

  const { data: idea, isLoading } = useQuery({
    queryKey: ['ideas', ideaId],
    queryFn: () => ideasApi.getById(ideaId),
    enabled: !!ideaId,
  })

  const demoConfirmMutation = useMutation({
    mutationFn: (paymentId: number) => paymentsApi.demoConfirm(paymentId),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['ideas', ideaId] })
      setDepositDone(true)
    },
    onError: (err) => setError(getErrorMessage(err)),
  })

  const depositMutation = useMutation({
    mutationFn: () => fundingsApi.payDeposit(ideaId, idea!.depositAmount, paymentMethod),
    onSuccess: (data) => {
      const { payment } = data
      const isMockUrl = payment.redirectUrl?.includes('mock-pg.local')
      if (payment.redirectUrl && !isMockUrl) {
        window.location.href = payment.redirectUrl
      } else if (payment.vbank) {
        setVbankInfo(payment.vbank)
      } else {
        demoConfirmMutation.mutate(payment.paymentId)
      }
    },
    onError: (err) => setError(getErrorMessage(err)),
  })

  if (isLoading) return <LoadingSpinner />
  if (!idea) return <div style={{ padding: '80px', textAlign: 'center', color: 'var(--fg-muted)' }}>아이디어를 찾을 수 없습니다.</div>

  if (depositDone) {
    return (
      <div style={{
        minHeight: '60vh', display: 'flex', alignItems: 'center', justifyContent: 'center',
        padding: '40px 20px',
      }}>
        <div style={{
          background: '#fff', border: '1px solid var(--border)',
          borderRadius: '20px', padding: '48px 44px',
          width: '100%', maxWidth: '480px', textAlign: 'center',
          boxShadow: '0 8px 40px rgba(0,0,0,0.06)',
        }}>
          <div style={{ fontSize: '56px', marginBottom: '20px' }}>✅</div>
          <h2 style={{ fontSize: '22px', fontWeight: 800, color: 'var(--fg)', marginBottom: '10px' }}>
            보증금 납부 완료!
          </h2>
          <p style={{ fontSize: '14px', color: 'var(--fg-muted)', marginBottom: '32px', lineHeight: 1.7 }}>
            보증금이 성공적으로 납부되었습니다.<br />
            아이디어가 AI 검증 단계로 진행됩니다.
          </p>
          <button
            onClick={() => router.push(`/ideas/${ideaId}`)}
            style={{
              width: '100%', height: '50px', fontSize: '16px', fontWeight: 700,
              background: 'var(--brand)', color: '#fff',
              border: 'none', borderRadius: '10px', cursor: 'pointer',
              fontFamily: 'inherit',
            }}
          >
            아이디어 보러가기
          </button>
        </div>
      </div>
    )
  }

  if (vbankInfo) {
    return (
      <div style={{
        minHeight: '60vh', display: 'flex', alignItems: 'center', justifyContent: 'center',
        padding: '40px 20px',
      }}>
        <div style={{
          background: '#fff', border: '1px solid var(--border)',
          borderRadius: '20px', padding: '48px 44px',
          width: '100%', maxWidth: '480px', textAlign: 'center',
          boxShadow: '0 8px 40px rgba(0,0,0,0.06)',
        }}>
          <div style={{ fontSize: '56px', marginBottom: '20px' }}>🏦</div>
          <h2 style={{ fontSize: '22px', fontWeight: 800, color: 'var(--fg)', marginBottom: '24px' }}>
            가상계좌 발급 완료
          </h2>
          <div style={{
            background: 'var(--bg-alt)', borderRadius: '12px', padding: '20px 24px',
            textAlign: 'left', marginBottom: '28px', lineHeight: 2,
          }}>
            {[
              { label: '은행', value: vbankInfo.bankCode },
              { label: '계좌번호', value: vbankInfo.accountNumber },
              { label: '입금기한', value: new Date(vbankInfo.dueDate).toLocaleString('ko-KR') },
              { label: '입금금액', value: formatCurrency(idea.depositAmount) },
            ].map(({ label, value }) => (
              <div key={label} style={{ display: 'flex', justifyContent: 'space-between', fontSize: '14px' }}>
                <span style={{ color: 'var(--fg-muted)' }}>{label}</span>
                <span style={{ fontWeight: 600, color: 'var(--fg)' }}>{value}</span>
              </div>
            ))}
          </div>
          <p style={{ fontSize: '13px', color: 'var(--fg-muted)', marginBottom: '24px' }}>
            입금이 확인되면 보증금 납부가 완료됩니다.
          </p>
          <button
            onClick={() => router.push(`/ideas/${ideaId}`)}
            style={{
              width: '100%', height: '50px', fontSize: '16px', fontWeight: 700,
              background: 'var(--brand)', color: '#fff',
              border: 'none', borderRadius: '10px', cursor: 'pointer',
              fontFamily: 'inherit',
            }}
          >
            아이디어 보러가기
          </button>
        </div>
      </div>
    )
  }

  const isPending = depositMutation.isPending || demoConfirmMutation.isPending

  return (
    <div style={{
      minHeight: '60vh', display: 'flex', alignItems: 'center', justifyContent: 'center',
      padding: '40px 20px',
    }}>
      <div style={{
        background: '#fff', border: '1px solid var(--border)',
        borderRadius: '20px', padding: '48px 44px',
        width: '100%', maxWidth: '520px',
        boxShadow: '0 8px 40px rgba(0,0,0,0.06)',
      }}>
        <div style={{ marginBottom: '32px' }}>
          <div style={{
            display: 'inline-flex', alignItems: 'center', gap: '6px',
            padding: '5px 12px', borderRadius: '99px',
            background: 'var(--brand-tint)', border: '1px solid var(--brand)',
            fontSize: '13px', fontWeight: 600, color: 'var(--brand-dark)',
            marginBottom: '16px',
          }}>
            💰 보증금 납부
          </div>
          <h1 style={{ fontSize: '22px', fontWeight: 800, color: 'var(--fg)', marginBottom: '6px' }}>
            보증금을 납부해주세요
          </h1>
          <p style={{ fontSize: '14px', color: 'var(--fg-muted)', lineHeight: 1.6 }}>
            아이디어 등록이 완료되었습니다.<br />
            보증금을 납부하면 AI 검증이 시작됩니다.
          </p>
        </div>

        {/* 아이디어 정보 */}
        <div style={{
          background: 'var(--bg-alt)', borderRadius: '12px',
          padding: '16px 20px', marginBottom: '28px',
        }}>
          <p style={{ fontSize: '12px', color: 'var(--fg-muted)', marginBottom: '4px' }}>아이디어</p>
          <p style={{ fontSize: '15px', fontWeight: 700, color: 'var(--fg)' }}>{idea.title}</p>
        </div>

        {/* 보증금 금액 */}
        <div style={{ marginBottom: '24px' }}>
          <label style={{ display: 'block', fontSize: '14px', fontWeight: 600, color: 'var(--fg)', marginBottom: '8px' }}>
            납부 금액
          </label>
          <div style={{
            height: '52px', border: '1.5px solid var(--border)',
            borderRadius: '10px', padding: '0 16px',
            display: 'flex', alignItems: 'center', justifyContent: 'space-between',
            background: 'var(--bg-alt)',
          }}>
            <span style={{ fontSize: '13px', color: 'var(--fg-muted)' }}>보증금</span>
            <span style={{ fontSize: '20px', fontWeight: 800, color: 'var(--brand)' }}>
              {formatCurrency(idea.depositAmount)}
            </span>
          </div>
        </div>

        {/* 결제 수단 */}
        <div style={{ marginBottom: '28px' }}>
          <label style={{ display: 'block', fontSize: '14px', fontWeight: 600, color: 'var(--fg)', marginBottom: '10px' }}>
            결제 수단
          </label>
          <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '10px' }}>
            {(['CARD', 'VIRTUAL_ACCOUNT'] as const).map((method) => {
              const label = method === 'CARD' ? '💳 카드' : '🏦 가상계좌'
              const isSelected = paymentMethod === method
              return (
                <button
                  key={method}
                  type="button"
                  onClick={() => setPaymentMethod(method)}
                  style={{
                    height: '52px', fontSize: '15px', fontWeight: 600,
                    border: `2px solid ${isSelected ? 'var(--brand)' : 'var(--border)'}`,
                    borderRadius: '10px',
                    background: isSelected ? 'var(--brand-tint)' : '#fff',
                    color: isSelected ? 'var(--brand-dark)' : 'var(--fg-muted)',
                    cursor: 'pointer', fontFamily: 'inherit',
                    transition: 'all 0.15s',
                  }}
                >
                  {label}
                </button>
              )
            })}
          </div>
        </div>

        {error && (
          <p style={{
            fontSize: '14px', color: 'var(--error)',
            background: '#fff5f5', padding: '10px 14px',
            borderRadius: '8px', marginBottom: '16px',
          }}>
            {error}
          </p>
        )}

        <button
          onClick={() => depositMutation.mutate()}
          disabled={isPending}
          style={{
            width: '100%', height: '54px', fontSize: '17px', fontWeight: 700,
            background: isPending ? 'var(--brand-tint)' : 'var(--brand)',
            color: isPending ? 'var(--brand)' : '#fff',
            border: 'none', borderRadius: '10px',
            cursor: isPending ? 'not-allowed' : 'pointer',
            fontFamily: 'inherit', transition: 'background 0.2s',
            marginBottom: '12px',
          }}
        >
          {isPending ? '처리 중...' : `${formatCurrency(idea.depositAmount)} 납부하기`}
        </button>

        <button
          onClick={() => router.push(`/ideas/${ideaId}`)}
          disabled={isPending}
          style={{
            width: '100%', height: '44px', fontSize: '14px', fontWeight: 600,
            background: 'transparent', color: 'var(--fg-muted)',
            border: 'none', cursor: isPending ? 'not-allowed' : 'pointer',
            fontFamily: 'inherit',
          }}
        >
          나중에 납부하기
        </button>

        <div style={{
          marginTop: '20px', padding: '14px 16px',
          background: 'var(--bg-alt)', borderRadius: '10px',
          fontSize: '12px', color: 'var(--fg-muted)', lineHeight: 1.7,
        }}>
          <strong style={{ color: 'var(--fg)', display: 'block', marginBottom: '4px' }}>보증금 안내</strong>
          <ul style={{ margin: 0, paddingLeft: '16px' }}>
            <li>보증금은 마일스톤을 성실히 이행한 경우 펀딩 종료 후 환급됩니다.</li>
            <li>마일스톤 미이행 또는 분쟁 해결 시 보증금이 몰수될 수 있습니다.</li>
            <li>보증금 납부 전에는 AI 검증이 시작되지 않습니다.</li>
          </ul>
        </div>
      </div>
    </div>
  )
}

export default function DepositPage() {
  return (
    <ProtectedRoute roles={['USER']}>
      <DepositPaymentForm />
    </ProtectedRoute>
  )
}
