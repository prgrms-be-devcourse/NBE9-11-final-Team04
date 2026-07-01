# 정산 · 마일스톤 — 배재현

> **담당 범위**  
> 메인: `settlement` / 서브: `milestone`  
> 핵심 클래스: `SettlementService`, `PreSettlementService`, `RefundService`, `MilestoneService`, `SettlementScheduler`, `MilestoneScheduler`, `PayoutRetryScheduler`

---

## 1. 어떤 방식으로 생각했는가

펀딩이 끝나면 **돈의 방향**이 갈립니다. 성공하면 제안자에게 단계별로 지급하고, 실패하면 후원자에게 돌려줘야 합니다. 이 흐름을 세 레이어로 나눴습니다.

```
[장부 레이어]  Settlement — 정산·환불 "의도" 기록 (멱등성 키)
[실행 레이어]  Refund / PreSettlement — 후원자별 환불·선정산 건 생성
[지급 레이어]  PayoutRetryScheduler — PG 지급대행 비동기 처리
```

마일스톤은 **자금 집행의 단위**입니다. 3단계 완료 보고 → 선정산 → 최종 정산. 기한 초과 시 보증금 몰수로 제안자 책임을 묻습니다.

```
펀딩 마감 (SettlementScheduler)
  ├─ 성공 → 1단계 마일스톤 시작
  └─ 실패 → 환불 Settlement + Refund 일괄 생성

마일스톤 완료 보고 승인
  └─ 선정산(PreSettlement) 신청 → 스케줄러 지급

3단계 완료
  └─ 최종 정산(Settlement) — 수수료 1% 차감, 누적 선정산 차감
```

---

## 2. 이 방법을 선택한 이유

| 대안 | 채택하지 않은 이유 | 선택한 방식 |
|------|-------------------|------------|
| 결제 완료 시 즉시 제안자에게 송금 | 마일스톤 미이행 시 후원자 보호 불가 | 선정산 + 최종 정산 분리 |
| 환불을 PaymentService에서 직접 | 정산 정책(목표 미달·이행 중단·먹튀)이 복잡 | RefundService로 환불 정책 집중 |
| 선정산 즉시 PG 호출 | API 타임아웃·실패 시 사용자 경험 저하 | REQUESTED 저장 후 스케줄러 비동기 지급 |
| 마일스톤 기한 초과 즉시 취소 | 소명 기회 없음 | overdueAt 기록 → 7일 유예 → 몰수 |

`SettlementScheduler`에서 `TransactionTemplate`으로 **프로젝트별 단일 트랜잭션**을 보장합니다. Settlement 생성 후 Refund 생성이 실패하면 전체 롤백되어, "환불 장부만 있고 Refund 없음" 상태를 방지합니다.

---

## 3. 이 기술을 고민한 이유

| 기술 | 고민 배경 |
|------|----------|
| **멱등성 키** (`idea-{id}-FINAL`) | 스케줄러 재실행·중복 호출 시 이중 정산 방지 |
| **VbankLedger 연동** | 선정산 OUT·환불 OUT을 장부에 기록해 잔액 추적 |
| **Spring @Retryable** | 선정산 동시 요청 시 `PessimisticLockingFailureException` 재시도 |
| **Idea FOR UPDATE** | 같은 아이디어에 대한 동시 선정산 한도 검증 직렬화 |
| **TransactionSynchronization** | 정산 생성 커밋 후 지급대행 이벤트 발행 (미커밋 지급 방지) |
| **환불 비율 분배** | 이행 중단 시 후원금 잔액을 후원 비율대로 분배, 먹튀 시 보증금 추가 |

---

## 4. 예상했던 문제

| 예상 문제 | 대비 설계 |
|----------|----------|
| 펀딩 마감일 배치 중 일부 프로젝트 실패 | 프로젝트별 try-catch, 실패 건 로그 후 다음 건 처리 |
| 선정산 한도 초과 (보증금 2배) | ideaId 기준 SUM 누적 + FOR UPDATE |
| PG 지급대행 일시 장애 | PayoutRetryScheduler 60초 주기 재시도, max 3회 |
| 목표 미달 환불 시 이미 환불된 건 | `excludeAlreadyRefundedPayments` 필터 |
| 마일스톤 소명 후 기한 리셋 | overdueAt 초기화로 7일 유예 재시작 |

---

## 5. 실제 일어났던 문제

### 5.1 선정산 동시 요청 시 한도 검증 오류

여러 선정산 요청이 동시에 들어오면, `REPEATABLE_READ` 스냅샷 기준으로 각각 한도 내로 판단해 **누적 한도를 초과**할 수 있었습니다.

**원인**: 트랜잭션 첫 DB 접근이 일반 SELECT였고, FOR UPDATE가 한도 검증 이후에 실행됨.

### 5.2 환불 재원 계산 복잡성

이행 중단 환불은 단순 전액 환불이 아닙니다.

- 후원금 잔액 = `SUM(payment SUCCESS) + 보증금 - SUM(pre_settlement COMPLETED)`
- 정당한 사유: 후원금 잔액만 비율 분배
- 먹튀/단순 포기: 보증금 전액을 후원자에게 추가 분배

초기에는 보증금 처리 시점(즉시 몰수 vs 지급 콜백 후)이 혼재되어 상태 불일치가 발생했습니다.

### 5.3 지급대행 API와 정산 상태 불일치

선정산 API에서 PG를 직접 호출하던 초기 설계에서, PG 타임아웃 시 `REQUESTED`인데 실제로는 지급됐거나, 반대로 `FAILED`인데 미지급인 경우가 있었습니다.

### 5.4 마일스톤 기한 정책 조정

처음에는 기한 초과 즉시 취소를 검토했으나, 제안자 소명 권리 보장을 위해 **overdueAt 기록 → 7일 유예 → 몰수** 2단계로 변경했습니다.

---

## 6. 해결 사례

### 사례 1: 선정산 동시 요청 — FOR UPDATE 선행

```java
// PreSettlementService — 트랜잭션 첫 DB 접근을 FOR UPDATE로
Idea lockedIdea = ideaRepository.findByIdForUpdate(ideaId)
    .orElseThrow(() -> new CustomException(ErrorCode.IDEA_NOT_FOUND));
// 이후 누적 한도 검증 → REQUESTED 저장
```

`@Retryable(retryFor = PessimisticLockingFailureException.class, maxAttempts = 3)`로 잠금 경합 시 재시도.

### 사례 2: 선정산 지급 비동기 분리

```
신청 API: PreSettlement(REQUESTED) 저장만
PayoutRetryScheduler (60초): processPreSettlementPayout() 호출
실패 시: FAILED → retryFailedPreSettlements() 재시도
```

API 응답과 PG 지급을 분리해 타임아웃·재시도를 스케줄러에 위임.

### 사례 3: 펀딩 실패 환불 원자성

```java
// SettlementScheduler — 프로젝트별 TransactionTemplate
transactionTemplate.executeWithoutResult(status -> {
    settlementService.createGoalNotMetRefundSettlement(ideaId);
    settlementService.createGoalNotMetDepositRefundSettlement(ideaId);
    refundService.createGoalNotMetRefunds(ideaId);
    ideaService.cancelIdea(ideaId);
});
```

### 사례 4: 환불 재원 분배 정책 코드화

`RefundService.createCancelRefunds(ideaId, isJustified)`에서 보증금 포함 여부·비율 분배를 단일 메서드로 통합. 보증금 몰수는 즉시, 환급은 지급 콜백에서 처리하도록 시점 분리.

---

## 7. 해결의 이유

| 해결책 | 왜 효과적인가 |
|--------|--------------|
| Idea FOR UPDATE 선행 | 동시 선정산의 한도 검증을 DB 레벨에서 직렬화 |
| 신청/지급 분리 | API는 빠르게 응답, PG 실패는 스케줄러가 흡수 |
| TransactionTemplate per idea | 부분 환불 상태 방지 — 전부 성공 또는 전부 롤백 |
| 멱등성 키 | 스케줄러 재실행해도 이중 정산 불가 |
| 2단계 마일스톤 스케줄러 | 소명 기회 보장 + 장기 미이행 시 제재 |

정산 도메인의 핵심은 **"언제, 누구에게, 얼마를, 왜"**를 장부에 남기고, PG 지급은 비동기로 분리하는 것입니다.

---

## 8. 결과

### 구현 규모

| 항목 | 수량 |
|------|------|
| Settlement API | 2 + PreSettlement 4 + Refund 3 |
| 배치 스케줄러 | SettlementScheduler, PayoutRetryScheduler, MilestoneScheduler |
| 단위 테스트 | SettlementServiceTest, RefundServiceTest, MilestoneServiceTest, SettlementSchedulerTest, PayoutRetrySchedulerTest |

### 검증

| 시나리오 | 결과 |
|----------|------|
| 펀딩 목표 미달 → 자동 환불 | Settlement + Refund 일괄 생성 확인 |
| 선정산 동시 요청 | FOR UPDATE + Retry로 한도 초과 방지 |
| 지급대행 실패 → 재시도 | PayoutRetryScheduler로 FAILED 건 복구 |
| 마일스톤 7일 유예 후 몰수 | MilestoneScheduler 2단계 처리 |

### 담당 API 요약

| 구분 | 엔드포인트 |
|------|-----------|
| 정산 | `GET /settlements/ideas/{ideaId}`, `GET /settlements/{id}` |
| 선정산 | `POST /pre-settlements/ideas/{ideaId}`, `GET /pre-settlements/ideas/{ideaId}` |
| 환불 | `GET /refunds/me`, `POST /refunds/{id}/retry` |
| 마일스톤 | `GET /milestones/ideas/{ideaId}`, `POST /milestones/{id}/reports` |

### 한 줄 요약

> 펀딩 마감부터 선정산·최종 정산·환불까지 **장부(Settlement) + 실행(Refund/PreSettlement) + 지급(Scheduler)** 3레이어로 분리했고, `FOR UPDATE`·멱등성 키·비동기 지급대행으로 금액 정합성과 재시도 안정성을 확보했습니다.

### 관련 파일

| 구분 | 경로 |
|------|------|
| 정산 서비스 | `backend/src/main/java/com/team04/domain/settlement/service/SettlementService.java` |
| 환불 서비스 | `backend/src/main/java/com/team04/domain/settlement/service/RefundService.java` |
| 선정산 서비스 | `backend/src/main/java/com/team04/domain/settlement/service/PreSettlementService.java` |
| 마일스톤 서비스 | `backend/src/main/java/com/team04/domain/milestone/service/MilestoneService.java` |
| 배치 | `backend/src/main/java/com/team04/infra/batch/SettlementScheduler.java` |
