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
import { getErrorMessage } from '@/utils/format'

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

export default function IdeaFormView() {
  const router = useRouter()
  const params = useParams()
  const searchParams = useSearchParams()
  const ideaId = params.ideaId ? Number(params.ideaId) : null
  const draftId = searchParams.get('draftId') ? Number(searchParams.get('draftId')) : null
  const isEdit = !!ideaId
  const [form, setForm] = useState<CreateIdeaRequest>(defaultForm)
  const [error, setError] = useState('')
  const minDatetime = nowDatetimeLocal()

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
        fundingStartAt: idea.fundingStartAt.slice(0, 16),
        fundingEndAt: idea.fundingEndAt.slice(0, 16),
        rewardType: idea.rewardType,
      })
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
    fundingStartAt: toIsoDateTime(form.fundingStartAt),
    fundingEndAt: toIsoDateTime(form.fundingEndAt),
  })

  const createMutation = useMutation({
    mutationFn: () =>
      draftId
        ? ideasApi.publishDraft(draftId, buildSubmitForm())
        : ideasApi.create(buildSubmitForm()),
    onSuccess: (data) => router.push(`/ideas/${data.ideaId}`),
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

  return (
    <div className="mx-auto max-w-3xl px-4 py-8">
      <h1 className="text-2xl font-bold">{isEdit ? '아이디어 수정' : '아이디어 등록'}</h1>
      <form
        onSubmit={(e) => {
          e.preventDefault()
          isEdit ? updateMutation.mutate() : createMutation.mutate()
        }}
        className="mt-6 space-y-6"
      >
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

        {/* 마일스톤 */}
        <Card className="space-y-6">
          <h2 className="font-semibold text-slate-700">마일스톤 (3단계 필수)</h2>
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
                  required
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
                  required
                />
              </div>
              <div>
                <label className="mb-1 block text-sm font-medium text-slate-700">목표 완료일</label>
                <input
                  type="date"
                  value={ms.expectedDate}
                  onChange={(e) => updateMilestone(i, 'expectedDate', e.target.value)}
                  className="w-full rounded-lg border px-3 py-2 text-sm"
                  required
                />
              </div>
            </div>
          ))}
        </Card>

        {error && <p className="rounded-lg bg-red-50 px-3 py-2 text-sm text-red-600">{error}</p>}
        <div className="flex gap-3">
          <Button type="submit" loading={createMutation.isPending || updateMutation.isPending}>
            {isEdit ? '수정' : '등록'}
          </Button>
          <Button type="button" variant="outline" onClick={() => draftMutation.mutate()}>임시저장</Button>
          <Button type="button" variant="ghost" onClick={() => router.back()}>취소</Button>
        </div>
      </form>
    </div>
  )
}
