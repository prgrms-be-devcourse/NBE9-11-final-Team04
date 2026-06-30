'use client'

import { useState } from 'react'
import { useRouter } from 'next/navigation'
import { useMutation } from '@tanstack/react-query'
import { usersApi } from '@/api/users'
import { useAuthStore } from '@/store/authStore'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { getErrorMessage } from '@/utils/format'

const PASSWORD_REGEX = /^(?=.*[A-Za-z])(?=.*\d)(?=.*[@$!%*#?&]).{8,20}$/

export default function AccountPage() {
  const router = useRouter()
  const { logout } = useAuthStore()
  const [pwForm, setPwForm] = useState({ currentPassword: '', newPassword: '', confirmPassword: '' })
  const [pwError, setPwError] = useState('')
  const [pwSuccess, setPwSuccess] = useState(false)

  const updatePasswordMutation = useMutation({
    mutationFn: () => usersApi.updatePassword({
      currentPassword: pwForm.currentPassword,
      newPassword: pwForm.newPassword,
    }),
    onSuccess: () => {
      setPwSuccess(true)
      setPwError('')
      setPwForm({ currentPassword: '', newPassword: '', confirmPassword: '' })
      setTimeout(() => setPwSuccess(false), 3000)
    },
    onError: (err) => { setPwError(getErrorMessage(err)); setPwSuccess(false) },
  })

  const deleteMutation = useMutation({
    mutationFn: usersApi.deleteMe,
    onSuccess: () => { logout(); router.replace('/') },
  })

  const handlePasswordSubmit = () => {
    if (!PASSWORD_REGEX.test(pwForm.newPassword)) {
      setPwError('비밀번호는 8~20자, 영문·숫자·특수문자(@$!%*#?&)를 모두 포함해야 합니다.')
      return
    }
    if (pwForm.newPassword !== pwForm.confirmPassword) {
      setPwError('새 비밀번호가 일치하지 않습니다.')
      return
    }
    setPwError('')
    updatePasswordMutation.mutate()
  }

  const handleDeleteAccount = () => {
    if (!confirm('정말 탈퇴하시겠습니까?\n탈퇴 후 복구가 불가능합니다.')) return
    deleteMutation.mutate()
  }

  return (
    <div style={{ display: 'flex', flexDirection: 'column', gap: '24px' }}>
      <h2 style={{ fontSize: '20px', fontWeight: 700, color: 'var(--fg)' }}>계정 설정</h2>

      {/* 비밀번호 변경 */}
      <div style={{
        background: '#fff',
        border: '1px solid var(--border)',
        borderRadius: '12px',
        padding: '24px',
        display: 'flex',
        flexDirection: 'column',
        gap: '16px',
      }}>
        <h3 style={{ fontSize: '16px', fontWeight: 700, color: 'var(--fg)', marginBottom: '4px' }}>
          🔑 비밀번호 변경
        </h3>

        <Input
          label="현재 비밀번호"
          type="password"
          value={pwForm.currentPassword}
          onChange={(e) => setPwForm({ ...pwForm, currentPassword: e.target.value })}
          placeholder="현재 비밀번호 입력"
        />
        <Input
          label="새 비밀번호"
          type="password"
          value={pwForm.newPassword}
          onChange={(e) => setPwForm({ ...pwForm, newPassword: e.target.value })}
          placeholder="영문·숫자·특수문자 포함 8~20자"
        />
        <div>
          <Input
            label="새 비밀번호 확인"
            type="password"
            value={pwForm.confirmPassword}
            onChange={(e) => setPwForm({ ...pwForm, confirmPassword: e.target.value })}
            placeholder="새 비밀번호 재입력"
          />
          {pwForm.confirmPassword && (
            <p style={{ fontSize: '12px', marginTop: '4px', color: pwForm.newPassword === pwForm.confirmPassword ? '#22c55e' : 'var(--error)' }}>
              {pwForm.newPassword === pwForm.confirmPassword ? '비밀번호가 일치합니다.' : '비밀번호가 일치하지 않습니다.'}
            </p>
          )}
        </div>

        {pwError && <p style={{ fontSize: '14px', color: 'var(--error)' }}>{pwError}</p>}
        {pwSuccess && <p style={{ fontSize: '14px', color: 'var(--brand-dark)' }}>비밀번호가 변경되었습니다.</p>}

        <div style={{ display: 'flex', justifyContent: 'flex-end' }}>
          <Button onClick={handlePasswordSubmit} loading={updatePasswordMutation.isPending}>
            변경하기
          </Button>
        </div>
      </div>

      {/* 회원 탈퇴 */}
      <div style={{
        background: '#fff',
        border: '1px solid #fde8e8',
        borderRadius: '12px',
        padding: '24px',
        display: 'flex',
        flexDirection: 'column',
        gap: '12px',
      }}>
        <h3 style={{ fontSize: '16px', fontWeight: 700, color: 'var(--error)' }}>
          🚨 회원 탈퇴
        </h3>
        <p style={{ fontSize: '14px', color: 'var(--fg-muted)', lineHeight: 1.6 }}>
          탈퇴 시 계정 및 모든 데이터가 삭제되며 복구할 수 없습니다.<br />
          진행 중인 펀딩이나 결제가 있을 경우 처리 후 탈퇴해 주세요.
        </p>
        <div>
          <Button
            variant="ghost"
            onClick={handleDeleteAccount}
            loading={deleteMutation.isPending}
            style={{ color: 'var(--error)', borderColor: 'var(--error)' }}
          >
            회원 탈퇴
          </Button>
        </div>
      </div>
    </div>
  )
}
