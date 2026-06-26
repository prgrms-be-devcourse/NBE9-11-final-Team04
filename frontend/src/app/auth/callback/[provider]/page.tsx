'use client'

import { useEffect, useState, Suspense } from 'react'
import { useParams, useSearchParams, useRouter } from 'next/navigation'
import Link from 'next/link'
import { authApi } from '@/api/auth'
import { usersApi } from '@/api/users'
import { useAuthStore } from '@/store/authStore'
import { getErrorMessage } from '@/utils/format'

type OAuthProvider = 'google' | 'kakao'
type PageState = 'loading' | 'error'

const PROVIDER_NAMES: Record<OAuthProvider, string> = {
  google: 'Google',
  kakao: '카카오',
}

function OAuthCallbackContent() {
  const params = useParams()
  const searchParams = useSearchParams()
  const router = useRouter()
  const setUser = useAuthStore((s) => s.setUser)

  const provider = params.provider as OAuthProvider
  const code = searchParams.get('code')
  const state = searchParams.get('state')
  const errorParam = searchParams.get('error')

  const [pageState, setPageState] = useState<PageState>('loading')
  const [errorMsg, setErrorMsg] = useState('')

  useEffect(() => {
    if (errorParam) {
      setErrorMsg('소셜 로그인이 취소되었습니다.')
      setPageState('error')
      return
    }
    if (!code || !state) {
      setErrorMsg('인증 코드가 없습니다.')
      setPageState('error')
      return
    }

    authApi.oauthCallback(provider, code, state)
      .then(async (res) => {
        if (res.type === 'LOGIN') {
          const user = await usersApi.getMe()
          setUser(user)
          router.replace('/')
        } else {
          // 신규 유저 → oauthToken, email, name을 쿼리로 전달하여 회원가입 완료 페이지로 이동
          const query = new URLSearchParams({
            oauthToken: res.oauthToken!,
            email: res.email!,
            name: res.name!,
            provider,
          })
          router.replace(`/signup/oauth?${query}`)
        }
      })
      .catch((err) => {
        setErrorMsg(getErrorMessage(err))
        setPageState('error')
      })
  }, [])

  if (pageState === 'loading') {
    return (
      <div style={{
        minHeight: '100vh', background: 'var(--bg-alt)',
        display: 'flex', flexDirection: 'column',
        alignItems: 'center', justifyContent: 'center', gap: '16px',
      }}>
        <div style={{
          width: '48px', height: '48px', borderRadius: '50%',
          border: '4px solid var(--brand-tint)',
          borderTopColor: 'var(--brand)',
          animation: 'spin 0.8s linear infinite',
        }} />
        <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
        <p style={{ fontSize: '16px', color: 'var(--fg-muted)' }}>
          {PROVIDER_NAMES[provider] ?? '소셜'} 로그인 처리 중...
        </p>
      </div>
    )
  }

  return (
    <div style={{
      minHeight: '100vh', background: 'var(--bg-alt)',
      display: 'flex', alignItems: 'center', justifyContent: 'center',
      padding: '40px 20px',
    }}>
      <div style={{
        background: '#fff', border: '1px solid var(--border)',
        borderRadius: 'var(--radius-2xl)', padding: '48px 44px',
        width: '100%', maxWidth: '440px', textAlign: 'center',
        boxShadow: '0 8px 40px rgba(0,0,0,0.06)',
      }}>
        <div style={{ fontSize: '48px', marginBottom: '16px' }}>😢</div>
        <h2 style={{ fontSize: '20px', fontWeight: 700, color: 'var(--fg)', marginBottom: '8px' }}>
          로그인에 실패했습니다
        </h2>
        <p style={{
          fontSize: '14px', color: 'var(--fg-muted)', marginBottom: '24px',
          padding: '10px 14px', background: '#fff5f5',
          borderRadius: '8px', border: '1px solid #fecaca',
        }}>
          {errorMsg}
        </p>
        <div style={{ display: 'flex', gap: '10px' }}>
          <Link href="/login" style={{
            flex: 1, display: 'block', textAlign: 'center',
            padding: '12px', fontSize: '14px', fontWeight: 600,
            border: '1.5px solid var(--border)', borderRadius: '8px',
            color: 'var(--fg-muted)', textDecoration: 'none',
          }}>
            로그인
          </Link>
          <Link href="/signup" style={{
            flex: 1, display: 'block', textAlign: 'center',
            padding: '12px', fontSize: '14px', fontWeight: 700,
            background: 'var(--brand)', borderRadius: '8px',
            color: '#fff', textDecoration: 'none',
          }}>
            회원가입
          </Link>
        </div>
      </div>
    </div>
  )
}

export default function OAuthCallbackPage() {
  return (
    <Suspense>
      <OAuthCallbackContent />
    </Suspense>
  )
}
