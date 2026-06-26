'use client'

import { useState, useEffect } from 'react'
import { useRouter, useParams, useSearchParams } from 'next/navigation'
import { useMutation, useQuery } from '@tanstack/react-query'
import { ideasApi } from '@/api/ideas'
import { Button } from '@/components/ui/Button'
import { Input } from '@/components/ui/Input'
import { Card } from '@/components/ui/Card'
import { IDEA_CATEGORY_LABELS, REWARD_TYPE_LABELS, type IdeaCategory, type RewardType } from '@/types/enums'
import type { CreateIdeaRequest, CreateMilestoneRequest } from '@/types/idea'
import { formatCurrency, getErrorMessage } from '@/utils/format'

const emptyMilestone = (step: number): CreateMilestoneRequest => ({
  step,
  goal: '',
  expectedResult: '',
  expectedDate: '',
})

const defaultForm: CreateIdeaRequest = {
  title: '',
  category: 'TECH',
  oneLineIntro: '',
  problemDefinition: '',
  solution: '',
  goal: '',
  targetCustomer: '',
  competitor: '',
  teamIntro: '',
  goalAmount: 1_000_000,
  depositAmount: 0,
  fundingStartAt: '',
  fundingEndAt: '',
  rewardType: 'REWARD_POINT',
  milestones: [emptyMilestone(1), emptyMilestone(2), emptyMilestone(3)],
}

const MILESTONE_LABELS = ['1단계', '2단계', '3단계']

const FIELD_PLACEHOLDERS: Partial<Record<keyof CreateIdeaRequest, string>> = {
  problemDefinition: '해결하려는 문제를 설명해주세요',
  solution: '문제 해결 방법을 설명해주세요',
  goal: '최종 목표를 설명해주세요',
  targetCustomer: '주요 고객층을 설명해주세요',
  competitor: '경쟁사 및 차별점을 설명해주세요',
  teamIntro: '팀 소개 및 역량을 설명해주세요',
}

const FIELD_LABELS: Partial<Record<keyof CreateIdeaRequest, string>> = {
  problemDefinition: '문제 정의',
  solution: '해결 방안',
  goal: '목표',
  targetCustomer: '타겟 고객',
  competitor: '경쟁사 분석',
  teamIntro: '팀 소개',
}

function toIsoDateTime(value: string): string {
  if (!value) return value
  return value.length === 16 ? `${value}:00` : value
}

function nowDatetimeLocal(): string {
  const now = new Date()
  now.setMinutes(now.getMinutes() + 1)
  return now.toISOString().slice(0, 16)
}

function StepIndicator({ current }: { current: 1 | 2 }) {
  const steps = [
    { num: 1, label: '아이디어 정보' },
    { num: 2, label: '마일스톤' },
  ]
  return (
    <div style={{ display: 'flex', alignItems: 'center', gap: '0', marginBottom: '32px' }}>
      {steps.map((s, i) => (
        <div key={s.num} style={{ display: 'flex', alignItems: 'center', flex: i < steps.length - 1 ? 1 : undefined }}>
          <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '6px' }}>
            <div style={{
              width: '36px', height: '36px', borderRadius: '50%',
              background: current === s.num ? 'var(--brand)' : current > s.num ? 'var(--brand)' : '#e2e8f0',
              color: current >= s.num ? '#fff' : '#94a3b8',
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              fontSize: '15px', fontWeight: 700,
              transition: 'all 0.2s',
            }}>
              {current > s.num ? '✓' : s.num}
            </div>
            <span style={{
              fontSize: '12px', fontWeight: current === s.num ? 700 : 500,
              color: current === s.num ? 'var(--brand-dark)' : '#94a3b8',
              whiteSpace: 'nowrap',
            }}>
              {s.label}
            </span>
          </div>
          {i < steps.length - 1 && (
            <div style={{
              flex: 1, height: '2px', margin: '0 8px', marginBottom: '18px',
              background: current > 1 ? 'var(--brand)' : '#e2e8f0',
              transition: 'background 0.3s',
            }} />
          )}
        </div>
      ))}
    </div>
  )
}

export default function IdeaFormView() {
  const router = useRouter()
  const params = useParams()
  const searchParams = useSearchParams()
  const ideaId = params.ideaId ? Number(params.ideaId) : null
  const draftId = searchParams.get('draftId') ? Number(searchParams.get('draftId')) : null
  const isEdit = !!ideaId

  const [step, setStep] = useState<1 | 2>(1)
  const [form, setForm] = useState<CreateIdeaRequest>(defaultForm)
  const [preSettlement, setPreSettlement] = useState<boolean | null>(null)
  const [error, setError] = useState('')
  const minDatetime = nowDatetimeLocal()
  const maxDeposit = Math.floor(form.goalAmount * 0.3)

  useQuery({
    queryKey: ['ideas', ideaId],
    queryFn: async () => {
      const idea = await ideasApi.getById(ideaId!)
      setForm({
        ...defaultForm,
        title: idea.title,
        category: idea.category,
        oneLineIntro: idea.oneLineIntro,
        problemDefinition: idea.problemDefinition,
        solution: idea.solution,
        goal: idea.goal,
        targetCustomer: idea.targetCustomer,
        competitor: idea.competitor,
        teamIntro: idea.teamIntro,
        goalAmount: idea.goalAmount,
        depositAmount: idea.depositAmount,
        fundingStartAt: idea.fundingStartAt.slice(0, 16),
        fundingEndAt: idea.fundingEndAt.slice(0, 16),
        rewardType: idea.rewardType,
      })
      setPreSettlement(idea.depositAmount > 0)
      return idea
    },
    enabled: isEdit,
  })

  const { data: drafts } = useQuery({
    queryKey: ['ideas', 'drafts'],
    queryFn: ideasApi.getDrafts,
    enabled: !!draftId,
  })

  useEffect(() => {
    if (!draftId || !drafts) return
    const draft = drafts.find((d) => d.draftId === draftId)
    if (!draft) return
    setForm({
      ...defaultForm,
      title: draft.title ?? '',
      category: draft.category ?? 'TECH',
      oneLineIntro: draft.oneLineIntro ?? '',
      problemDefinition: draft.problemDefinition ?? '',
      solution: draft.solution ?? '',
      goal: draft.goal ?? '',
      targetCustomer: draft.targetCustomer ?? '',
      competitor: draft.competitor ?? '',
      teamIntro: draft.teamIntro ?? '',
      goalAmount: draft.goalAmount ?? 1_000_000,
      fundingStartAt: draft.fundingStartAt ? draft.fundingStartAt.slice(0, 16) : '',
      fundingEndAt: draft.fundingEndAt ? draft.fundingEndAt.slice(0, 16) : '',
      rewardType: draft.rewardType ?? 'REWARD_POINT',
    })
  }, [draftId, drafts])

  const buildSubmitForm = (): CreateIdeaRequest => ({
    ...form,
    depositAmount: preSettlement ? form.depositAmount : 0,
    fundingStartAt: toIsoDateTime(form.fundingStartAt),
    fundingEndAt: toIsoDateTime(form.fundingEndAt),
  })

  const validateStep1 = (): string => {
    if (!form.title.trim()) return '제목을 입력해주세요.'
    if (!form.oneLineIntro.trim()) return '한 줄 소개를 입력해주세요.'
    if (!form.goalAmount || form.goalAmount <= 0) return '목표 금액을 입력해주세요.'
    if (!form.fundingStartAt) return '펀딩 시작일을 입력해주세요.'
    if (!form.fundingEndAt) return '펀딩 종료일을 입력해주세요.'
    if (!form.problemDefinition.trim()) return '문제 정의를 입력해주세요.'
    if (!form.solution.trim()) return '해결 방안을 입력해주세요.'
    if (!form.goal.trim()) return '목표를 입력해주세요.'
    if (!form.targetCustomer.trim()) return '타겟 고객을 입력해주세요.'
    if (!form.competitor.trim()) return '경쟁사 분석을 입력해주세요.'
    if (!form.teamIntro.trim()) return '팀 소개를 입력해주세요.'
    if (preSettlement === null) return '선정산 신청 여부를 선택해주세요.'
    if (preSettlement && (!form.depositAmount || form.depositAmount <= 0)) return '보증금을 입력해주세요.'
    if (preSettlement && form.depositAmount > maxDeposit) return `보증금은 ${formatCurrency(maxDeposit)} 이하여야 합니다.`
    return ''
  }

  const validateStep2 = (): string => {
    for (let i = 0; i < form.milestones.length; i++) {
      const ms = form.milestones[i]
      if (!ms.goal.trim()) return `${MILESTONE_LABELS[i]} 목표를 입력해주세요.`
      if (!ms.expectedResult?.trim()) return `${MILESTONE_LABELS[i]} 기대 결과를 입력해주세요.`
      if (!ms.expectedDate) return `${MILESTONE_LABELS[i]} 목표 완료일을 입력해주세요.`
    }
    return ''
  }

  const handleNext = () => {
    const err = validateStep1()
    if (err) { setError(err); return }
    setError('')
    setStep(2)
    window.scrollTo({ top: 0, behavior: 'smooth' })
  }

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault()
    const err = validateStep2()
    if (err) { setError(err); return }
    setError('')
    isEdit ? updateMutation.mutate() : createMutation.mutate()
  }

  const createMutation = useMutation({
    mutationFn: () =>
      draftId
        ? ideasApi.publishDraft(draftId, buildSubmitForm())
        : ideasApi.create(buildSubmitForm()),
    onSuccess: (data) => {
      if (data.depositAmount > 0) {
        router.push(`/ideas/${data.ideaId}/deposit`)
      } else {
        router.push(`/ideas/${data.ideaId}`)
      }
    },
    onError: (err) => setError(getErrorMessage(err)),
  })

  const updateMutation = useMutation({
    mutationFn: () => ideasApi.update(ideaId!, buildSubmitForm()),
    onSuccess: (data) => router.push(`/ideas/${data.ideaId}`),
    onError: (err) => setError(getErrorMessage(err)),
  })

  const buildDraftForm = () => {
    const { milestones, ...rest } = form
    return Object.fromEntries(
      Object.entries({
        ...rest,
        fundingStartAt: toIsoDateTime(form.fundingStartAt) || null,
        fundingEndAt: toIsoDateTime(form.fundingEndAt) || null,
        goalAmount: form.goalAmount || null,
      }).map(([k, v]) => [k, v === '' ? null : v]),
    )
  }

  const draftMutation = useMutation({
    mutationFn: () => ideasApi.createDraft(buildDraftForm()),
    onSuccess: () => alert('임시저장되었습니다.'),
    onError: (err) => setError(getErrorMessage(err)),
  })

  const updateMilestone = (index: number, field: keyof CreateMilestoneRequest, value: string) => {
    const milestones = form.milestones.map((m, i) =>
      i === index ? { ...m, [field]: value } : m,
    )
    setForm({ ...form, milestones })
  }

  const handlePreSettlementChange = (value: boolean) => {
    setPreSettlement(value)
    if (!value) setForm({ ...form, depositAmount: 0 })
    setError('')
  }

  const showStep1 = step === 1
  const showStep2 = step === 2

  return (
    <div className="mx-auto max-w-3xl px-4 py-8">
      <h1 className="text-2xl font-bold" style={{ marginBottom: '24px' }}>
        {isEdit ? '아이디어 수정' : '아이디어 등록'}
      </h1>

      <StepIndicator current={step} />

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* ===== STEP 1: 아이디어 정보 ===== */}
        {showStep1 && (
          <>
            {/* 기본 정보 */}
            <Card className="space-y-4">
              <h2 className="font-semibold text-slate-700">기본 정보</h2>
              <Input label="제목" value={form.title} onChange={(e) => setForm({ ...form, title: e.target.value })} required />
              <div>
                <label className="mb-1 block text-sm font-medium text-slate-700">카테고리</label>
                <select value={form.category} onChange={(e) => setForm({ ...form, category: e.target.value as IdeaCategory })} className="w-full rounded-lg border px-3 py-2 text-sm">
                  {Object.entries(IDEA_CATEGORY_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
                </select>
              </div>
              <Input label="한 줄 소개" value={form.oneLineIntro} onChange={(e) => setForm({ ...form, oneLineIntro: e.target.value })} required />
              <Input label="목표 금액 (원)" type="number" value={form.goalAmount} onChange={(e) => setForm({ ...form, goalAmount: Number(e.target.value) })} required />
              <Input label="펀딩 시작일" type="datetime-local" value={form.fundingStartAt} min={minDatetime} onChange={(e) => setForm({ ...form, fundingStartAt: e.target.value })} required />
              <Input label="펀딩 종료일 (시작일 기준 2~8주 후)" type="datetime-local" value={form.fundingEndAt} min={form.fundingStartAt || minDatetime} onChange={(e) => setForm({ ...form, fundingEndAt: e.target.value })} required />
              <div>
                <label className="mb-1 block text-sm font-medium text-slate-700">보상 방식</label>
                <select value={form.rewardType} onChange={(e) => setForm({ ...form, rewardType: e.target.value as RewardType })} className="w-full rounded-lg border px-3 py-2 text-sm">
                  {Object.entries(REWARD_TYPE_LABELS).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
                </select>
              </div>

              {/* 선정산 */}
              <div>
                <label className="mb-2 block text-sm font-medium text-slate-700">선정산 신청 여부</label>
                <div className="flex gap-3">
                  {([
                    { value: false, label: '미신청', desc: '선정산 없이 펀딩 진행' },
                    { value: true,  label: '신청',   desc: '마일스톤 완료 시 선정산 수령' },
                  ] as const).map(({ value, label, desc }) => {
                    const selected = preSettlement === value
                    return (
                      <button
                        key={String(value)}
                        type="button"
                        onClick={() => handlePreSettlementChange(value)}
                        className="flex-1"
                        style={{
                          padding: '12px 16px',
                          borderRadius: '10px',
                          border: `2px solid ${selected ? 'var(--brand)' : '#e2e8f0'}`,
                          background: selected ? 'var(--brand-tint)' : '#fff',
                          cursor: 'pointer',
                          textAlign: 'left',
                          transition: 'all 0.15s',
                        }}
                      >
                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '2px' }}>
                          <span style={{
                            width: '16px', height: '16px', borderRadius: '50%', flexShrink: 0,
                            border: `2px solid ${selected ? 'var(--brand)' : '#94a3b8'}`,
                            background: selected ? 'var(--brand)' : '#fff',
                            display: 'flex', alignItems: 'center', justifyContent: 'center',
                          }}>
                            {selected && <span style={{ width: '6px', height: '6px', borderRadius: '50%', background: '#fff', display: 'block' }} />}
                          </span>
                          <span style={{ fontSize: '14px', fontWeight: 700, color: selected ? 'var(--brand-dark)' : '#374151' }}>
                            {label}
                          </span>
                        </div>
                        <p style={{ fontSize: '12px', color: '#6b7280', marginLeft: '24px' }}>{desc}</p>
                      </button>
                    )
                  })}
                </div>

                {preSettlement === true && (
                  <div className="mt-4 space-y-3">
                    <div>
                      <label className="mb-1 block text-sm font-medium text-slate-700">
                        보증금 (원)
                        <span className="ml-2 text-xs font-normal text-slate-400">
                          최대 {formatCurrency(maxDeposit)} (목표금액의 30%)
                        </span>
                      </label>
                      <input
                        type="number"
                        value={form.depositAmount || ''}
                        onChange={(e) => setForm({ ...form, depositAmount: Number(e.target.value) })}
                        min={1}
                        max={maxDeposit}
                        placeholder="보증금을 입력하세요"
                        className="w-full rounded-lg border px-3 py-2 text-sm"
                        style={{ borderColor: form.depositAmount > maxDeposit ? '#ef4444' : undefined }}
                      />
                      {form.depositAmount > maxDeposit && (
                        <p className="mt-1 text-xs text-red-500">
                          목표금액의 30%({formatCurrency(maxDeposit)})를 초과했습니다.
                        </p>
                      )}
                    </div>
                    <div style={{ background: '#f0f9ff', border: '1px solid #bae6fd', borderRadius: '10px', padding: '14px 16px' }}>
                      <p style={{ fontSize: '13px', fontWeight: 700, color: '#0369a1', marginBottom: '8px' }}>💡 선정산 신청 안내</p>
                      <ul style={{ margin: 0, paddingLeft: '16px', fontSize: '12px', color: '#0c4a6e', lineHeight: 1.8 }}>
                        <li>보증금은 <strong>목표금액의 최대 30%</strong>까지 설정 가능합니다.</li>
                        <li>마일스톤 완료 시 납부한 보증금의 <strong>최대 2배</strong>까지 선정산을 신청할 수 있습니다.</li>
                        <li>보증금은 모든 마일스톤 성실 이행 완료 후 <strong>전액 환급</strong>됩니다.</li>
                        <li>마일스톤 미이행 시 보증금이 <strong>몰수</strong>될 수 있습니다.</li>
                      </ul>
                    </div>
                  </div>
                )}
              </div>
            </Card>

            {/* 상세 내용 */}
            <Card className="space-y-4">
              <h2 className="font-semibold text-slate-700">상세 내용</h2>
              {(['problemDefinition', 'solution', 'goal', 'targetCustomer', 'competitor', 'teamIntro'] as const).map((field) => (
                <div key={field}>
                  <label className="mb-1 block text-sm font-medium text-slate-700">{FIELD_LABELS[field]}</label>
                  <textarea
                    value={form[field]}
                    onChange={(e) => setForm({ ...form, [field]: e.target.value })}
                    rows={3}
                    className="w-full rounded-lg border px-3 py-2 text-sm"
                    placeholder={FIELD_PLACEHOLDERS[field]}
                    required
                  />
                </div>
              ))}
            </Card>
          </>
        )}

        {/* ===== STEP 2: 마일스톤 ===== */}
        {showStep2 && (
          <Card className="space-y-6">
            <div>
              <h2 className="font-semibold text-slate-700">마일스톤 (3단계 필수)</h2>
              <p style={{ fontSize: '13px', color: '#6b7280', marginTop: '4px' }}>
                펀딩 목표를 달성하기 위한 단계별 계획을 입력해주세요.
              </p>
            </div>
            {form.milestones.map((ms, i) => (
              <div key={i} className="space-y-3 rounded-lg border border-slate-100 bg-slate-50 p-4">
                <p className="text-sm font-semibold text-slate-600">{MILESTONE_LABELS[i]}</p>
                <div>
                  <label className="mb-1 block text-sm font-medium text-slate-700">목표</label>
                  <textarea
                    value={ms.goal}
                    onChange={(e) => updateMilestone(i, 'goal', e.target.value)}
                    rows={2}
                    className="w-full rounded-lg border px-3 py-2 text-sm"
                    placeholder={`${MILESTONE_LABELS[i]} 달성 목표를 입력하세요`}
                  />
                </div>
                <div>
                  <label className="mb-1 block text-sm font-medium text-slate-700">기대 결과</label>
                  <textarea
                    value={ms.expectedResult}
                    onChange={(e) => updateMilestone(i, 'expectedResult', e.target.value)}
                    rows={2}
                    className="w-full rounded-lg border px-3 py-2 text-sm"
                    placeholder="달성 시 기대되는 결과를 입력하세요"
                  />
                </div>
                <div>
                  <label className="mb-1 block text-sm font-medium text-slate-700">목표 완료일</label>
                  <input
                    type="date"
                    value={ms.expectedDate}
                    onChange={(e) => updateMilestone(i, 'expectedDate', e.target.value)}
                    className="w-full rounded-lg border px-3 py-2 text-sm"
                  />
                </div>
              </div>
            ))}
          </Card>
        )}

        {error && <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{error}</p>}

        {/* ===== 버튼 영역 ===== */}
        {step === 1 && (
          <div className="flex gap-3">
            <Button type="button" onClick={handleNext}>다음 →</Button>
            {!isEdit && <Button type="button" variant="outline" onClick={() => draftMutation.mutate()}>임시저장</Button>}
            <Button type="button" variant="ghost" onClick={() => router.back()}>취소</Button>
          </div>
        )}

        {!isEdit && step === 2 && (
          <div className="flex gap-3">
            <Button
              type="button"
              variant="outline"
              onClick={() => { setError(''); setStep(1); window.scrollTo({ top: 0, behavior: 'smooth' }) }}
            >
              ← 이전
            </Button>
            <Button type="submit" loading={createMutation.isPending}>등록</Button>
            <Button type="button" variant="outline" onClick={() => draftMutation.mutate()}>임시저장</Button>
          </div>
        )}

        {isEdit && step === 2 && (
          <div className="flex gap-3">
            <Button
              type="button"
              variant="outline"
              onClick={() => { setError(''); setStep(1); window.scrollTo({ top: 0, behavior: 'smooth' }) }}
            >
              ← 이전
            </Button>
            <Button type="submit" loading={updateMutation.isPending}>수정</Button>
            <Button type="button" variant="ghost" onClick={() => router.back()}>취소</Button>
          </div>
        )}
      </form>
    </div>
  )
}
