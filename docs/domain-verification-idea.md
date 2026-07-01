# 검증 · 아이디어 — 김하늘

> **담당 범위**  
> 메인: `verification` / 서브: `idea`  
> 핵심 클래스: `VerificationService`, `VerificationAsyncProcessor`, `OpenAiVerificationService`, `ProposerHistoryScoreCalculator`, `IdeaService`, `AdminIdeaReviewService`

---

## 1. 어떤 방식으로 생각했는가

크라우드펀딩 플랫폼의 첫 관문은 **"이 아이디어를 믿을 수 있는가"**입니다. 사람이 모든 프로젝트를 검토하기엔 확장성이 없고, AI만 맡기기엔 오판 리스크가 큽니다.

그래서 **3단계 검증 파이프라인**을 설계했습니다.

```
1차: 금칙어 필터 (동기, 즉시)
2차: OpenAI 구조화 검증 (비동기, 10~30초)
3차: 관리자 최종 심사 (수동)
     + (선택) 전문가 매칭 리뷰
```

아이디어 도메인은 **프로젝트의 생명주기**를 관리합니다.

```
초안(Draft) → 발행(Published) → 검증(Verification) → 펀딩 오픈(OPEN)
→ 진행(IN_PROGRESS) → 완료(COMPLETED) / 취소(CANCELLED)
```

검증 결과는 `TrustScore`(신뢰점수)로 수치화해 후원자에게 노출합니다.

---

## 2. 이 방법을 선택한 이유

| 대안 | 채택하지 않은 이유 | 선택한 방식 |
|------|-------------------|------------|
| 관리자만 심사 | 프로젝트 증가 시 병목 | AI 1차 + 관리자 2차 |
| AI 검증을 동기 API로 | OpenAI 응답 10~30초 → HTTP 타임아웃 | `@Async` + `@TransactionalEventListener` |
| 단순 통과/실패 | 후원자에게 정보 부족 | 구조화 결과(실현가능성·리스크·개선점) + 점수 |
| 아이디어에 검증 상태 혼합 | 관심사 분리 위반 | `ProjectVerification` 별도 엔티티 |
| 신규 제안자 0점 시작 | 공정성 문제 | 기본 10점 + 이력 가산/감점 |

`VerificationRequestedEvent`를 커밋 **후** 발행해, 검증 요청 DB 저장이 확정된 뒤 AI 호출이 시작되도록 했습니다.

---

## 3. 이 기술을 고민한 이유

| 기술 | 고민 배경 |
|------|----------|
| **OpenAI GPT-4o-mini** | 비용·속도·구조화 출력(JSON) 균형 |
| **Resilience4j** | OpenAI 장애 시 Circuit Breaker + Retry로 서비스 전체 다운 방지 |
| **@Async + TransactionTemplate** | AI 호출은 트랜잭션 밖, 결과 저장만 짧은 TX |
| **금칙어 정규식 사전 컴파일** | `@PostConstruct`에서 Pattern 1회 생성 → 매 요청 재컴파일 방지 |
| **VerificationAuditLog** | 상태 전이마다 감사 로그 — 관리자 분쟁·재심사 근거 |
| **IdeaDraft + 마일스톤 3개 필수** | 발행 시점에 실행 계획 완비 강제 |
| **S3 StorageClient** | 대표·본문 이미지 분리 업로드, MIME·용량 검증 |

---

## 4. 예상했던 문제

| 예상 문제 | 대비 설계 |
|----------|----------|
| OpenAI 응답 지연·타임아웃 | 비동기 처리 + 실패 시 `PENDING_ADMIN_REVIEW` 폴백 |
| OpenAI 할루시네이션 | AI 결과는 참고용, 관리자 최종 판정 필수 |
| 금칙어 우회 (띄어쓰기 변형) | `keyword.replace(" ", "\\s*")` 정규식 |
| 검증 중 아이디어 삭제 | 비동기 처리 시작 전 `CANCELLED` 상태 확인 |
| AI 검증 중복 요청 | `AI_VERIFYING` 상태면 거부 |
| 신규 제안자 불이익 | `ProposerHistoryScoreCalculator` 기본 10점 |

---

## 5. 실제 일어났던 문제

### 5.1 OpenAI 호출 중 트랜잭션 장시간 점유

초기에는 `@Async` 없이 Controller에서 직접 OpenAI를 호출했습니다. 응답 20초+ 동안 DB 커넥션을 점유해 HikariCP 풀 고갈 위험이 있었습니다.

### 5.2 AI 실패 시 사용자 무한 대기

OpenAI API 5xx·타임아웃 시 검증 상태가 `AI_VERIFYING`에 고착되면, 사용자는 "검증 중"만 보고 재요청도 불가했습니다.

**해결**: catch 블록에서 `markPendingAdminReview()` + 관리자 알림 발행.

### 5.3 금칙어와 AI 결과 병합 로직

금칙어 탐지와 AI 검증을 각각 수행하면 결과가 충돌합니다 (AI는 통과, 금칙어는 실패).

`mergeResults()`로 **금칙어 실패가 AI 결과보다 우선**하도록 통합.

### 5.4 아이디어 발행 시 마일스톤 검증 누락

초기에는 마일스톤 없이 발행 가능해, 펀딩 성공 후 실행 계획이 없는 프로젝트가 생겼습니다.

`REQUIRED_MILESTONE_COUNT = 3` 강제 + `IdeaDraftMilestoneConverter`로 초안→발행 시 마일스톤 일괄 생성.

### 5.5 관리자 반려 후 재심사 상태 전이

관리자 반려 시 `PENDING_ADMIN_REVIEW`에서만 재요청 가능하도록 상태 전이 조건을 명시적으로 추가.

---

## 6. 해결 사례

### 사례 1: 비동기 AI 검증 파이프라인

```java
// VerificationService — 동기: 접수만
verification.startAiVerification();
eventPublisher.publishEvent(new VerificationRequestedEvent(saved.getId(), request));

// VerificationAsyncProcessor — 비동기: AI 호출
@Async("verificationTaskExecutor")
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void processAiVerification(VerificationRequestedEvent event) {
    // 1. 취소 여부 확인 (짧은 TX)
    // 2. OpenAI 호출 (TX 밖)
    // 3. 결과 저장 (짧은 TX)
}
```

AI I/O를 트랜잭션 밖으로 빼서 커넥션 점유 시간을 수 ms로 축소.

### 사례 2: OpenAI 실패 폴백

```java
catch (Exception exception) {
    transactionTemplate.executeWithoutResult(status -> {
        verification.markPendingAdminReview();
        audit(..., "AI 검증 호출 실패로 관리자 재시도 필요");
        publishPendingAdminReviewNotification(verification.getIdeaId());
    });
}
```

AI 장애가 서비스 중단으로 이어지지 않고, 관리자 수동 심사로 전환.

### 사례 3: 신뢰점수 산출

```java
// ProposerHistoryScoreCalculator
기본 10점
+ 사업자 등록 인증 +5
+ 완료 프로젝트 1건당 +5
- 해결된 분쟁(피신고) 1건당 -5
→ 0~20점 클램핑
```

### 사례 4: 아이디어 초안 라이프사이클

- 최대 50개 초안, 30일 보존
- 이미지 MIME·5MB·최대 10장 검증
- 발행 시 마일스톤 3개 + 보증금 한도(목표액 30%) 검증

---

## 7. 해결의 이유

| 해결책 | 왜 효과적인가 |
|--------|--------------|
| AFTER_COMMIT 이벤트 | 검증 요청이 DB에 없는데 AI가 돌아가는幽霊 처리 방지 |
| TX 밖 AI 호출 | 외부 API 지연이 DB 커넥션 풀에 영향 없음 |
| 금칙어 우선 병합 | AI가 놓친 부적절 콘텐츠를 규칙 기반으로 보완 |
| PENDING_ADMIN_REVIEW 폴백 | AI 의존도를 낮추고 운영 연속성 확보 |
| 감사 로그 | 재심사·분쟁 시 "왜 이 상태인가" 추적 가능 |

검증 도메인의 핵심은 **AI를 보조 수단으로 쓰되, 시스템이 AI 장애에 죽지 않게** 만드는 것입니다.

---

## 8. 결과

### 구현 규모

| 항목 | 수량 |
|------|------|
| Idea API | 22개 (초안·발행·북마크·이미지·신고 등) |
| Verification API | 4개 (요청·조회·관리자 판정) |
| 단위 테스트 | VerificationServiceTest, VerificationAsyncProcessorTest, ProposerHistoryScoreCalculatorTest, IdeaFlowTest, IdeaServiceTest, AdminIdeaReviewServiceTest |

### 검증 상태 흐름

```
DRAFT → AI_VERIFYING → AI_PASSED → EXPERT_MATCHING → PENDING_ADMIN_REVIEW → APPROVED
                  ↘ AI_FAILED          ↘ 관리자 반려 → 재심사 가능
                  ↘ PENDING_ADMIN_REVIEW (AI 장애 폴백)
```

### 담당 API 요약

| 구분 | 엔드포인트 |
|------|-----------|
| 아이디어 | `POST /ideas`, `POST /ideas/drafts`, `POST /ideas/drafts/{id}/publish` |
| 검증 | `POST /verifications`, `GET /verifications/ideas/{ideaId}` |
| 관리자 | `PATCH /admin/ideas/{id}/approve`, `PATCH /admin/verifications/{id}` |
| 신뢰점수 | `GET /ideas/{id}/trust-score` |

### 한 줄 요약

> OpenAI 비동기 검증 + 금칙어 사전 필터 + 관리자 심사 3단계 파이프라인을 구축했고, AI 장애 폴백·신뢰점수 산출·감사 로그로 **AI 의존 없이도 운영 가능한 검증 시스템**을 완성했습니다.

### 관련 파일

| 구분 | 경로 |
|------|------|
| 검증 서비스 | `backend/src/main/java/com/team04/domain/verification/service/VerificationService.java` |
| 비동기 처리 | `backend/src/main/java/com/team04/domain/verification/service/VerificationAsyncProcessor.java` |
| 신뢰점수 | `backend/src/main/java/com/team04/domain/verification/service/ProposerHistoryScoreCalculator.java` |
| 아이디어 서비스 | `backend/src/main/java/com/team04/domain/idea/service/IdeaService.java` |
| OpenAI 클라이언트 | `backend/src/main/java/com/team04/infra/openai/OpenAiVerificationClient.java` |
