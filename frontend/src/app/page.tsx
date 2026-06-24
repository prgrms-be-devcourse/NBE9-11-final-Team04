'use client'

import Link from 'next/link'
import { useQuery } from '@tanstack/react-query'
import { ideasApi } from '@/api/ideas'
import { useAuthStore } from '@/store/authStore'
import { Badge } from '@/components/ui/Badge'
import { ProgressBar } from '@/components/ui/ProgressBar'
import { IDEA_CATEGORY_LABELS, IDEA_STATUS_LABELS, type IdeaStatus } from '@/types/enums'
import { formatCurrency, calcAchievementRate } from '@/utils/format'

const FEATURES = [
  { icon: '🤖', title: 'AI 1차 검증',     desc: 'OpenAI·Claude API가 아이디어의 실현 가능성, 시장성, 위험도를 자동 분석합니다. 20점 배점 기준으로 객관적 평가.' },
  { icon: '🎓', title: '전문가 검증',      desc: '국가자격 인증 전문가(EXPERT)가 도메인 전문성으로 아이디어를 검토합니다. 매주 자동 자격 재검증으로 신뢰 유지.' },
  { icon: '🏛️', title: '관리자 최종 승인', desc: '플랫폼 관리자의 최종 게이트를 통과한 아이디어만 펀딩 오픈. AI → 전문가 → 관리자 3중 검증 구조.' },
  { icon: '📊', title: '마일스톤 투명성',  desc: '자금 사용 내역이 Append-only로 기록되어 수정·삭제 불가. 스폰서는 실시간으로 자금 흐름을 확인할 수 있습니다.' },
  { icon: '💳', title: '토스페이먼츠 결제', desc: '결제 즉시 가상 계좌 적립. 마일스톤 달성 시 단계별 정산. 목표 미달성 시 자동 전액 환불.' },
  { icon: '🔔', title: '실시간 SSE 알림',  desc: 'SSE 기술로 펀딩 달성률을 실시간 스트리밍. 마감 7일 전 자동 알림으로 스폰서가 놓치지 않도록.' },
]

const FLOW_STEPS = [
  { num: '1', label: 'AI 검증',     sub: '실현 가능성·시장성 자동 분석' },
  { num: '2', label: '전문가 검토',  sub: '도메인 전문가 심층 평가' },
  { num: '3', label: '관리자 승인',  sub: '최종 게이트 통과 후 OPEN' },
  { num: '4', label: '크라우드펀딩', sub: '스폰서 모집 & 후원 시작' },
  { num: '5', label: '마일스톤 정산', sub: '달성 기준 단계별 자금 집행' },
]

const SCORE_ITEMS = [
  { label: 'AI 검증',       val: '18/20', pct: 90 },
  { label: '마일스톤 구체성', val: '17/20', pct: 85 },
  { label: '전문가 매칭',    val: '20/20', pct: 100 },
  { label: '관리자 승인',    val: '20/20', pct: 100 },
  { label: '제안자 이력',    val: '13/20', pct: 65 },
]

const STATUS_VARIANT: Record<IdeaStatus, 'blue' | 'green' | 'orange' | 'red' | 'gray'> = {
  AI_PENDING: 'gray', EXPERT_PENDING: 'orange', ADMIN_PENDING: 'orange',
  OPEN: 'blue', IN_PROGRESS: 'green', COMPLETED: 'green', CANCELLED: 'red',
}

const inner: React.CSSProperties = { maxWidth: '1100px', margin: '0 auto', padding: '0 24px' }
const sectionPad: React.CSSProperties = { padding: '80px 0' }

function usePrimaryCta() {
  const { isAuthenticated, user } = useAuthStore()
  if (!isAuthenticated) return { href: '/signup',    label: '아이디어 제안하기 →' }
  if (user?.role === 'SPONSOR') return { href: '/ideas',     label: '아이디어 후원하기 →' }
  if (user?.role === 'EXPERT')  return { href: '/ideas',     label: '검토 아이디어 보기 →' }
  return { href: '/ideas/new', label: '아이디어 제안하기 →' }
}

export default function HomePage() {
  const { isAuthenticated } = useAuthStore()
  const cta = usePrimaryCta()
  const { data: topIdeas } = useQuery({ queryKey: ['ideas', 'top5'], queryFn: ideasApi.getTop5, retry: false })

  return (
    <div>

      {/* ===== HERO ===== */}
      <section style={{ background: 'var(--brand-tint)', padding: '80px 0 72px' }}>
        <div style={{ ...inner, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '64px', alignItems: 'center' }}>
          {/* 좌측 텍스트 */}
          <div>
            <span style={{
              display: 'inline-block', fontSize: '13px', fontWeight: 700,
              color: 'var(--brand-dark)', background: 'rgba(0,176,225,0.12)',
              padding: '5px 14px', borderRadius: '99px', marginBottom: '24px',
            }}>
              신뢰 기반 크라우드펀딩 플랫폼
            </span>
            <h1 style={{ fontSize: 'clamp(32px, 4vw, 52px)', fontWeight: 700, lineHeight: 1.2, letterSpacing: '-0.02em', color: 'var(--fg)', marginBottom: '20px' }}>
              아이디어를<br /><em style={{ color: 'var(--brand)', fontStyle: 'normal' }}>자금으로</em>,<br />자금을 신뢰로
            </h1>
            <p style={{ fontSize: '18px', color: 'var(--fg-muted)', lineHeight: 1.7, marginBottom: '36px', maxWidth: '480px' }}>
              AI 검증 → 전문가 평가 → 관리자 승인의 3단계 신뢰 구조 위에서,
              소규모 사업가의 아이디어가 크라우드펀딩으로 실현됩니다.
            </p>
            <div style={{ display: 'flex', gap: '16px', flexWrap: 'wrap', marginBottom: '48px' }}>
              <Link href={cta.href} style={{
                display: 'inline-flex', alignItems: 'center', gap: '6px',
                background: 'var(--brand)', color: '#fff',
                padding: '14px 28px', borderRadius: 'var(--radius-md)',
                fontSize: '16px', fontWeight: 700, textDecoration: 'none',
                transition: 'background 0.2s',
              }}>
                {cta.label}
              </Link>
              <Link href="/ideas" style={{
                display: 'inline-flex', alignItems: 'center', gap: '6px',
                border: '1.5px solid var(--brand)', color: 'var(--brand-dark)',
                padding: '14px 28px', borderRadius: 'var(--radius-md)',
                fontSize: '16px', fontWeight: 700, textDecoration: 'none', background: '#fff',
              }}>
                진행 중 펀딩 보기
              </Link>
            </div>
            <div style={{ display: 'flex', gap: '40px' }}>
              {[['87점', '평균 신뢰도 점수'], ['3단계', '검증 프로세스'], ['100%', '자금 사용 공개']].map(([num, label]) => (
                <div key={label}>
                  <div style={{ fontSize: '28px', fontWeight: 700, color: 'var(--fg)', letterSpacing: '-0.02em' }}>{num}</div>
                  <div style={{ fontSize: '13px', color: 'var(--fg-muted)', marginTop: '2px' }}>{label}</div>
                </div>
              ))}
            </div>
          </div>

          {/* 우측 카드 */}
          <div style={{ display: 'flex', flexDirection: 'column', gap: '16px' }}>
            <div style={{ background: '#fff', border: '1px solid var(--border)', borderRadius: '16px', padding: '24px', boxShadow: '0 4px 24px rgba(0,176,225,0.08)' }}>
              <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', marginBottom: '16px' }}>
                <span style={{ fontSize: '16px', fontWeight: 700, color: 'var(--fg)' }}>유기농 간식 구독 박스</span>
                <span style={{ fontSize: '12px', fontWeight: 700, color: '#fff', background: 'var(--brand)', padding: '3px 10px', borderRadius: '99px' }}>IN_PROGRESS</span>
              </div>
              <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px', color: 'var(--fg-muted)', marginBottom: '8px' }}>
                <span>펀딩 달성률</span><span style={{ fontWeight: 700, color: 'var(--brand)' }}>78%</span>
              </div>
              <div style={{ height: '8px', background: 'var(--brand-tint)', borderRadius: '99px', overflow: 'hidden', marginBottom: '16px' }}>
                <div style={{ width: '78%', height: '100%', background: 'var(--brand)', borderRadius: '99px' }} />
              </div>
              <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr 1fr', gap: '12px', marginBottom: '16px' }}>
                {[['7,800,000원', '누적 후원'], ['D-14', '마감까지'], ['142명', '스폰서']].map(([val, lbl]) => (
                  <div key={lbl} style={{ textAlign: 'center' }}>
                    <div style={{ fontSize: '15px', fontWeight: 700, color: 'var(--fg)' }}>{val}</div>
                    <div style={{ fontSize: '12px', color: 'var(--fg-muted)' }}>{lbl}</div>
                  </div>
                ))}
              </div>
              <div style={{ display: 'flex', alignItems: 'center', gap: '12px', padding: '12px 16px', background: 'var(--brand-tint)', borderRadius: '10px' }}>
                <div style={{ fontSize: '28px', fontWeight: 700, color: 'var(--brand)', lineHeight: 1 }}>88</div>
                <div>
                  <div style={{ fontSize: '13px', fontWeight: 700, color: 'var(--fg)' }}>신뢰도 점수</div>
                  <div style={{ fontSize: '12px', color: 'var(--fg-muted)' }}>AI검증 + 전문가 + 관리자</div>
                </div>
                <div style={{ marginLeft: 'auto', fontSize: '12px', fontWeight: 700, color: '#fff', background: '#22c55e', padding: '3px 10px', borderRadius: '99px' }}>✓ CERTIFIED</div>
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* ===== 서비스 특징 ===== */}
      <section style={{ ...sectionPad, background: '#fff' }}>
        <div style={inner}>
          <span style={{ display: 'inline-block', fontSize: '13px', fontWeight: 700, color: 'var(--brand-dark)', background: 'var(--brand-tint)', padding: '5px 14px', borderRadius: '99px', marginBottom: '16px' }}>서비스 특징</span>
          <h2 style={{ fontSize: 'clamp(24px, 3vw, 36px)', fontWeight: 700, lineHeight: 1.2, letterSpacing: '-0.02em', color: 'var(--fg)', marginBottom: '12px' }}>왜 SeedLink인가요?</h2>
          <p style={{ fontSize: '18px', color: 'var(--fg-muted)', lineHeight: 1.6, marginBottom: '48px', maxWidth: '560px' }}>
            검증과 자금 투명성이라는 신뢰 구조 위에서 아이디어가 실현됩니다.
          </p>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '24px' }}>
            {FEATURES.map((f) => (
              <div key={f.title} style={{ background: '#fff', border: '1px solid var(--border)', borderRadius: '12px', padding: '28px 24px', transition: 'box-shadow 0.2s' }}>
                <div style={{ fontSize: '36px', marginBottom: '16px' }}>{f.icon}</div>
                <div style={{ fontSize: '17px', fontWeight: 700, color: 'var(--fg)', marginBottom: '10px' }}>{f.title}</div>
                <div style={{ fontSize: '14px', color: 'var(--fg-muted)', lineHeight: 1.7 }}>{f.desc}</div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ===== 신뢰도 점수 ===== */}
      <section style={{ ...sectionPad, background: 'var(--bg-alt)' }}>
        <div style={{ ...inner, display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '64px', alignItems: 'center' }}>
          {/* 좌측: 점수 보드 */}
          <div style={{ background: '#fff', border: '1px solid var(--border)', borderRadius: '16px', padding: '32px' }}>
            <div style={{ fontSize: '16px', fontWeight: 700, color: 'var(--fg)', marginBottom: '20px' }}>📊 신뢰도 점수 산정 방식</div>
            <div style={{ display: 'flex', alignItems: 'baseline', gap: '6px', marginBottom: '12px' }}>
              <span style={{ fontSize: '52px', fontWeight: 700, color: 'var(--fg)', letterSpacing: '-0.03em', lineHeight: 1 }}>88</span>
              <span style={{ fontSize: '20px', color: 'var(--fg-muted)' }}>/100</span>
            </div>
            <div style={{ display: 'flex', gap: '8px', marginBottom: '28px' }}>
              <span style={{ fontSize: '12px', fontWeight: 700, color: '#fff', background: '#22c55e', padding: '3px 10px', borderRadius: '99px' }}>✓ CERTIFIED</span>
              <span style={{ fontSize: '12px', fontWeight: 700, color: 'var(--brand-dark)', background: 'var(--brand-tint)', padding: '3px 10px', borderRadius: '99px' }}>80점 이상</span>
            </div>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '14px' }}>
              {SCORE_ITEMS.map((s) => (
                <div key={s.label}>
                  <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '14px', marginBottom: '6px' }}>
                    <span style={{ color: 'var(--fg-muted)' }}>{s.label}</span>
                    <span style={{ fontWeight: 700, color: 'var(--fg)' }}>{s.val}</span>
                  </div>
                  <div style={{ height: '6px', background: 'var(--brand-tint)', borderRadius: '99px', overflow: 'hidden' }}>
                    <div style={{ width: `${s.pct}%`, height: '100%', background: 'var(--brand)', borderRadius: '99px' }} />
                  </div>
                </div>
              ))}
            </div>
          </div>
          {/* 우측: 설명 */}
          <div>
            <span style={{ display: 'inline-block', fontSize: '13px', fontWeight: 700, color: 'var(--brand-dark)', background: 'var(--brand-tint)', padding: '5px 14px', borderRadius: '99px', marginBottom: '16px' }}>신뢰 시스템</span>
            <h2 style={{ fontSize: 'clamp(24px, 3vw, 36px)', fontWeight: 700, lineHeight: 1.2, letterSpacing: '-0.02em', color: 'var(--fg)', marginBottom: '20px' }}>
              투명한 점수,<br />검증된 아이디어
            </h2>
            <p style={{ fontSize: '16px', color: 'var(--fg-muted)', lineHeight: 1.7, marginBottom: '32px' }}>
              모든 아이디어는 5가지 기준으로 100점 만점 신뢰도 점수를 받습니다. 80점 이상이면 CERTIFIED 배지가 자동으로 부여됩니다.
            </p>
            <div style={{ display: 'flex', flexDirection: 'column', gap: '20px' }}>
              {[
                { icon: '🏢', title: '사업자등록 가점',  desc: '국세청 API로 실제 사업자 진위를 확인합니다. 보유 시 신뢰도 가점 적용.' },
                { icon: '💰', title: '보증금 기탁 구조',  desc: '제안자는 펀딩 시작 전 보증금을 예치합니다. 부정 행위 시 보증금 몰취 — 무책임한 제안을 구조적으로 차단.' },
                { icon: '⚖️', title: '분쟁 조정 시스템', desc: 'RECEIVED → IN_REVIEW → RESOLVED 단계로 투명하게 분쟁을 처리합니다.' },
              ].map((t) => (
                <div key={t.title} style={{ display: 'flex', gap: '16px', alignItems: 'flex-start' }}>
                  <div style={{ width: '44px', height: '44px', borderRadius: '12px', background: 'var(--brand-tint)', display: 'flex', alignItems: 'center', justifyContent: 'center', fontSize: '22px', flexShrink: 0 }}>{t.icon}</div>
                  <div>
                    <div style={{ fontSize: '15px', fontWeight: 700, color: 'var(--fg)', marginBottom: '4px' }}>{t.title}</div>
                    <div style={{ fontSize: '14px', color: 'var(--fg-muted)', lineHeight: 1.6 }}>{t.desc}</div>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      </section>

      {/* ===== 이용 방법 ===== */}
      <section style={{ ...sectionPad, background: 'var(--brand-tint)' }}>
        <div style={inner}>
          <span style={{ display: 'inline-block', fontSize: '13px', fontWeight: 700, color: 'var(--brand-dark)', background: 'rgba(0,176,225,0.12)', padding: '5px 14px', borderRadius: '99px', marginBottom: '16px' }}>이용 방법</span>
          <h2 style={{ fontSize: 'clamp(24px, 3vw, 36px)', fontWeight: 700, lineHeight: 1.2, letterSpacing: '-0.02em', color: 'var(--fg)', marginBottom: '48px' }}>
            아이디어에서 펀딩까지,<br />5단계 검증 플로우
          </h2>
          <div style={{ display: 'grid', gridTemplateColumns: 'repeat(5, 1fr)', gap: '16px' }}>
            {FLOW_STEPS.map((s, i) => (
              <div key={s.label} style={{ background: '#fff', border: '1px solid var(--border)', borderRadius: '12px', padding: '24px 16px', textAlign: 'center', position: 'relative' }}>
                <div style={{
                  width: '40px', height: '40px', borderRadius: '50%',
                  background: i < 2 ? '#22c55e' : i === 2 ? 'var(--brand)' : 'var(--border)',
                  color: i <= 2 ? '#fff' : 'var(--fg-muted)',
                  display: 'flex', alignItems: 'center', justifyContent: 'center',
                  fontSize: '16px', fontWeight: 700, margin: '0 auto 14px',
                }}>
                  {i < 2 ? '✓' : s.num}
                </div>
                <div style={{ fontSize: '15px', fontWeight: 700, color: 'var(--fg)', marginBottom: '6px' }}>{s.label}</div>
                <div style={{ fontSize: '13px', color: 'var(--fg-muted)', lineHeight: 1.5 }}>{s.sub}</div>
                <div style={{ marginTop: '12px' }}>
                  <span style={{
                    fontSize: '11px', fontWeight: 700, padding: '3px 8px', borderRadius: '99px',
                    background: i < 2 ? '#f0fdf4' : i === 2 ? 'var(--brand-tint)' : '#f5f5f5',
                    color: i < 2 ? '#22c55e' : i === 2 ? 'var(--brand-dark)' : 'var(--fg-muted)',
                  }}>
                    {i < 2 ? '완료' : i === 2 ? '진행 중' : '대기'}
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ===== 인기 프로젝트 TOP5 ===== */}
      <section style={{ ...sectionPad, background: '#fff' }}>
        <div style={inner}>
          <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', marginBottom: '36px' }}>
            <div>
              <span style={{ display: 'inline-block', fontSize: '13px', fontWeight: 700, color: 'var(--brand-dark)', background: 'var(--brand-tint)', padding: '5px 14px', borderRadius: '99px', marginBottom: '12px' }}>HOT</span>
              <h2 style={{ fontSize: 'clamp(22px, 3vw, 32px)', fontWeight: 700, letterSpacing: '-0.02em', color: 'var(--fg)' }}>지금 주목받는 아이디어</h2>
            </div>
            <Link href="/ideas" style={{ fontSize: '14px', fontWeight: 600, color: 'var(--brand-dark)', textDecoration: 'none' }}>전체 보기 →</Link>
          </div>

          {topIdeas && topIdeas.length > 0 ? (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '20px' }}>
              {topIdeas.slice(0, 6).map((idea) => (
                <Link key={idea.ideaId} href={`/ideas/${idea.ideaId}`} style={{ textDecoration: 'none' }}>
                  <div style={{
                    background: '#fff', border: '1px solid var(--border)', borderRadius: '14px',
                    padding: '24px', height: '100%', transition: 'box-shadow 0.2s', display: 'flex', flexDirection: 'column', gap: '12px',
                  }}>
                    <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                      <span style={{ fontSize: '12px', fontWeight: 600, color: 'var(--brand-dark)', background: 'var(--brand-tint)', padding: '3px 8px', borderRadius: '99px' }}>
                        {IDEA_CATEGORY_LABELS[idea.category]}
                      </span>
                      <Badge variant={STATUS_VARIANT[idea.status as IdeaStatus]}>
                        {IDEA_STATUS_LABELS[idea.status as IdeaStatus]}
                      </Badge>
                    </div>
                    <div>
                      <p style={{ fontSize: '16px', fontWeight: 700, color: 'var(--fg)', marginBottom: '6px', overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
                        {idea.title}
                      </p>
                      <p style={{ fontSize: '13px', color: 'var(--fg-muted)', lineHeight: 1.5, display: '-webkit-box', WebkitLineClamp: 2, WebkitBoxOrient: 'vertical', overflow: 'hidden' }}>
                        {idea.oneLineIntro}
                      </p>
                    </div>
                    <div style={{ marginTop: 'auto' }}>
                      <ProgressBar value={calcAchievementRate(idea.currentAmount, idea.goalAmount)} />
                      <div style={{ display: 'flex', justifyContent: 'space-between', fontSize: '13px', color: 'var(--fg-muted)', marginTop: '8px' }}>
                        <span style={{ fontWeight: 700, color: 'var(--brand)' }}>{formatCurrency(idea.currentAmount)}</span>
                        <span>{idea.supporterCount}명 후원</span>
                      </div>
                    </div>
                  </div>
                </Link>
              ))}
            </div>
          ) : (
            <div style={{ display: 'grid', gridTemplateColumns: 'repeat(3, 1fr)', gap: '20px' }}>
              {[1, 2, 3].map((i) => (
                <div key={i} style={{ background: '#f5f5f5', borderRadius: '14px', padding: '24px', height: '180px', animation: 'pulse 1.5s infinite' }} />
              ))}
            </div>
          )}
        </div>
      </section>

      {/* ===== CTA ===== */}
      <section style={{ background: 'var(--fg)', padding: '80px 0' }}>
        <div style={{ ...inner, textAlign: 'center' }}>
          <div style={{ fontSize: '13px', fontWeight: 700, color: 'var(--brand)', letterSpacing: '0.08em', textTransform: 'uppercase', marginBottom: '16px' }}>
            지금 시작하세요
          </div>
          <h2 style={{ fontSize: 'clamp(32px, 4vw, 52px)', fontWeight: 700, letterSpacing: '-0.03em', lineHeight: 1.15, color: '#fff', marginBottom: '16px' }}>
            당신의 아이디어가<br /><em style={{ color: 'var(--brand)', fontStyle: 'normal' }}>현실이 되는 곳</em>
          </h2>
          <p style={{ fontSize: '18px', color: 'rgba(255,255,255,0.6)', marginBottom: '40px' }}>
            3단계 검증과 투명한 자금 구조로, 아이디어를 안심하고 펀딩하세요.
          </p>
          <div style={{ display: 'flex', gap: '16px', justifyContent: 'center', flexWrap: 'wrap' }}>
            <Link href={cta.href} style={{
              display: 'inline-flex', alignItems: 'center', gap: '8px',
              background: 'var(--brand)', color: '#fff',
              fontSize: '18px', fontWeight: 700, padding: '16px 36px',
              borderRadius: 'var(--radius-md)', textDecoration: 'none', transition: 'background 0.2s',
            }}>
              {cta.label}
            </Link>
            <Link href="/ideas" style={{
              display: 'inline-flex', alignItems: 'center', gap: '8px',
              border: '1.5px solid rgba(255,255,255,0.3)', color: 'rgba(255,255,255,0.85)',
              fontSize: '18px', fontWeight: 600, padding: '16px 36px',
              borderRadius: 'var(--radius-md)', textDecoration: 'none',
            }}>
              펀딩 탐색하기
            </Link>
          </div>
        </div>
      </section>

    </div>
  )
}
