'use client'

import { useEffect, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { usersApi } from '@/api/users'
import { useAuthStore } from '@/store/authStore'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { LoadingSpinner } from '@/components/ui/LoadingSpinner'
import { getErrorMessage } from '@/utils/format'

export default function ProfileSection() {
  const setUser = useAuthStore((s) => s.setUser)
  const queryClient = useQueryClient()
  const fileInputRef = useRef<HTMLInputElement>(null)

  const [error, setError] = useState('')
  const [success, setSuccess] = useState(false)
  const [imagePreview, setImagePreview] = useState<string | null>(null)
  const [form, setForm] = useState({ nickname: '', intro: '', portfolioUrl: '' })

  const { data: user, isLoading } = useQuery({ queryKey: ['users', 'me'], queryFn: usersApi.getMe })

  useEffect(() => {
    if (user) {
      setForm({ nickname: user.nickname, intro: user.intro ?? '', portfolioUrl: user.portfolioUrl ?? '' })
      setImagePreview(user.profileImage ?? null)
    }
  }, [user])

  const imageMutation = useMutation({
    mutationFn: (file: File) => usersApi.updateProfileImage(file),
    onSuccess: (updated) => {
      setUser(updated)
      setImagePreview(updated.profileImage ?? null)
      queryClient.invalidateQueries({ queryKey: ['users', 'me'] })
    },
    onError: (err) => setError(getErrorMessage(err)),
  })

  const deleteImageMutation = useMutation({
    mutationFn: () => usersApi.deleteProfileImage(),
    onSuccess: (updated) => {
      setUser(updated)
      setImagePreview(null)
      queryClient.invalidateQueries({ queryKey: ['users', 'me'] })
    },
    onError: (err) => setError(getErrorMessage(err)),
  })

  const saveMutation = useMutation({
    mutationFn: () => usersApi.updateMe({
      nickname: form.nickname,
      intro: form.intro || undefined,
      portfolioUrl: form.portfolioUrl || undefined,
    }),
    onSuccess: (updated) => {
      setUser(updated)
      setSuccess(true)
      setError('')
      queryClient.invalidateQueries({ queryKey: ['users', 'me'] })
      setTimeout(() => setSuccess(false), 2000)
    },
    onError: (err) => { setError(getErrorMessage(err)); setSuccess(false) },
  })

  const handleImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    e.target.value = ''
    setImagePreview(URL.createObjectURL(file))
    imageMutation.mutate(file)
  }

  if (isLoading) return <LoadingSpinner />
  if (!user) return null

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <h2 style={{ fontSize: '20px', fontWeight: 700, color: 'var(--fg)' }}>내 정보</h2>

      <div style={{
        background: '#fff', border: '1px solid var(--border)',
        borderRadius: '12px', padding: '24px',
        display: 'flex', flexDirection: 'column', gap: '20px',
      }}>
        {/* 프로필 이미지 */}
        <div style={{ display: 'flex', alignItems: 'center', gap: '20px' }}>
          <div style={{ position: 'relative', flexShrink: 0 }}>
            <div style={{
              width: '80px', height: '80px', borderRadius: '50%',
              background: 'var(--brand-tint)', border: '2px solid var(--border)',
              overflow: 'hidden', display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: '32px',
            }}>
              {imagePreview
                ? <img src={imagePreview} alt="profile" style={{ width: '100%', height: '100%', objectFit: 'cover' }} />
                : '👤'}
            </div>
            <button
              onClick={() => fileInputRef.current?.click()}
              disabled={imageMutation.isPending}
              style={{
                position: 'absolute', bottom: 0, right: 0,
                width: '26px', height: '26px', borderRadius: '50%',
                background: 'var(--brand)', border: '2px solid #fff',
                cursor: 'pointer', display: 'flex', alignItems: 'center', justifyContent: 'center',
                fontSize: '12px',
              }}
            >
              {imageMutation.isPending ? '⏳' : '✏️'}
            </button>
            <input ref={fileInputRef} type="file" accept="image/*" style={{ display: 'none' }} onChange={handleImageChange} />
          </div>
          <div>
            <div style={{ fontSize: '16px', fontWeight: 700, color: 'var(--fg)' }}>{user.nickname}</div>
            <div style={{ fontSize: '13px', color: 'var(--fg-muted)', marginTop: '2px' }}>{user.email}</div>
            <div style={{ display: 'flex', gap: '12px', marginTop: '8px' }}>
              <button
                onClick={() => fileInputRef.current?.click()}
                disabled={imageMutation.isPending || deleteImageMutation.isPending}
                style={{ fontSize: '13px', color: 'var(--brand-dark)', background: 'none', border: 'none', cursor: 'pointer', padding: 0 }}
              >
                {imageMutation.isPending ? '업로드 중...' : (imagePreview ? '사진 변경' : '사진 등록')}
              </button>
              {imagePreview && (
                <button
                  onClick={() => deleteImageMutation.mutate()}
                  disabled={imageMutation.isPending || deleteImageMutation.isPending}
                  style={{ fontSize: '13px', color: 'var(--error)', background: 'none', border: 'none', cursor: 'pointer', padding: 0 }}
                >
                  {deleteImageMutation.isPending ? '삭제 중...' : '사진 삭제'}
                </button>
              )}
            </div>
          </div>
        </div>

        <hr style={{ border: 'none', borderTop: '1px solid var(--border)', margin: 0 }} />

        {/* 읽기 전용 */}
        <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '16px' }}>
          <div>
            <p style={{ fontSize: '12px', color: 'var(--fg-muted)', marginBottom: '4px' }}>이메일</p>
            <p style={{ fontSize: '14px', fontWeight: 500, color: 'var(--fg)' }}>{user.email}</p>
          </div>
          <div>
            <p style={{ fontSize: '12px', color: 'var(--fg-muted)', marginBottom: '4px' }}>가입일</p>
            <p style={{ fontSize: '14px', fontWeight: 500, color: 'var(--fg)' }}>
              {user.createdAt ? new Date(user.createdAt).toLocaleDateString('ko-KR') : '-'}
            </p>
          </div>
        </div>

        <Input
          label="닉네임"
          value={form.nickname}
          onChange={(e) => setForm({ ...form, nickname: e.target.value })}
        />

        <div>
          <label style={{ display: 'block', fontSize: '14px', fontWeight: 600, color: 'var(--fg)', marginBottom: '6px' }}>소개</label>
          <textarea
            value={form.intro}
            onChange={(e) => setForm({ ...form, intro: e.target.value })}
            rows={3}
            placeholder="자신을 소개해주세요"
            style={{
              width: '100%', borderRadius: '8px', border: '1.5px solid var(--border)',
              padding: '10px 12px', fontSize: '14px', color: 'var(--fg)',
              resize: 'vertical', outline: 'none', boxSizing: 'border-box',
              fontFamily: 'inherit', transition: 'border-color 0.2s',
            }}
            onFocus={(e) => { e.target.style.borderColor = 'var(--brand)' }}
            onBlur={(e) => { e.target.style.borderColor = 'var(--border)' }}
          />
        </div>

        <Input
          label="포트폴리오 URL"
          value={form.portfolioUrl}
          onChange={(e) => setForm({ ...form, portfolioUrl: e.target.value })}
          placeholder="https://..."
        />

        {error && <p style={{ fontSize: '14px', color: 'var(--error)' }}>{error}</p>}
        {success && <p style={{ fontSize: '14px', color: 'var(--brand-dark)' }}>저장되었습니다.</p>}

        <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
          <Button onClick={() => saveMutation.mutate()} loading={saveMutation.isPending}>
            저장하기
          </Button>
        </div>
      </div>
    </div>
  )
}
