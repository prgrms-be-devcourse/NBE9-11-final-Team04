# 펀딩 · 결제 · 워크스페이스 — 김정욱

> **담당 범위**  
> 메인: `funding`, `payment` / 서브: `workspace`  
> 핵심 클래스: `FundingService`, `PaymentService`, `VbankLedgerService`, `TossPaymentGateway`, `WorkspaceService`

---

## 1. 어떤 방식으로 생각했는가

후원 플랫폼에서 **돈의 흐름**은 단순 CRUD가 아닙니다. 후원 신청 → 결제 대기 → 승인/취소/환불이 동시에 들어올 수 있고, 각 단계마다 `Funding`, `Payment`, `Idea.currentAmount`, `vbank_ledgers`가 함께 움직여야 합니다.

그래서 다음 원칙으로 설계했습니다.

1. **상태 머신 분리** — `Funding`(후원 건)과 `Payment`(결제 건)를 별도 엔티티로 두고, 1:1로 연결
2. **PG 추상화** — `PaymentGateway` 인터페이스로 Mock/Toss를 교체 가능하게 분리
3. **내부 장부** — PG 응답과 별도로 `vbank_ledgers`에 IN/OUT을 기록해 잔액 추적
4. **동일 자원 직렬화** — 같은 `funding_id`에 대한 입금·취소·환불은 `SELECT FOR UPDATE`로 같은 순서로 잠금
5. **이벤트 기반 후처리** — 결제 완료 후 `FundingPaidEvent`로 달성률 갱신·SSE 전송

```
후원 신청 → Funding(PENDING_PAYMENT) + Payment(PENDING)
결제 승인 → Payment(SUCCESS) + Funding(PAID) + vbank_ledgers IN
결제 전 취소 → Payment(FAILED) + Funding(CANCELLED)
결제 후 환불 → Payment(REFUNDED) + Funding(REFUNDED) + vbank_ledgers OUT
```

---

## 2. 이 방법을 선택한 이유

| 대안 | 채택하지 않은 이유 | 선택한 방식 |
|------|-------------------|------------|
| Funding에 결제 상태만 저장 | 결제 수단·PG 키·웹훅 로그를 담기 어려움 | Payment 엔티티 분리 |
| PG 응답만으로 잔액 관리 | 환불·선정산·보증금이 섞이면 추적 불가 | VbankLedger 내부 장부 |
| 애플리케이션 synchronized | 다중 인스턴스·DB 트랜잭션 경계에서 무력화 | DB 비관적 잠금 |
| 결제 완료 시 Idea 금액 직접 수정 | 결제 도메인이 아이디어에 강결합 | Spring Event 발행 |

`PaymentGateway` 추상화는 로컬에서는 Mock + `demo-confirm`, 운영에서는 Toss를 쓰기 위함입니다. 팀 전체가 PG 없이도 개발·테스트할 수 있게 했습니다.

---

## 3. 이 기술을 고민한 이유

| 기술 | 고민 배경 |
|------|----------|
| **토스페이먼츠** | 국내 크라우드펀딩에 맞는 카드·가상계좌·지급대행·웹훅을 한 번에 제공 |
| **VbankLedger + idempotency_key** | 웹훅 중복·재시도 시 장부 이중 기록 방지 (`payment-{id}-FUNDING-PAID`) |
| **SELECT FOR UPDATE** | 입금 vs 취소 레이스에서 상태 덮어쓰기 방지 |
| **SseEmitter + ConcurrentHashMap** | 펀딩 달성률을 폴링 없이 실시간 전달 |
| **ObjectProvider\<PaymentService\>** | `createPayment` 실패 시 별도 트랜잭션으로 `failPayment` 호출 (self-invocation 우회) |
| **IdeaVbankPool** | 아이디어별 가상계좌 풀 관리로 입출금 추적 단위 명확화 |

---

## 4. 예상했던 문제

| 예상 문제 | 대비 설계 |
|----------|----------|
| 동시 후원 시 중복 결제 | `createPendingPayment`에서 Funding `FOR UPDATE` + PENDING/SUCCESS 중복 검사 |
| PG 세션 생성 후 서버 장애 | catch에서 `failPayment`로 PENDING 정리 |
| 웹훅 중복 수신 | `PaymentWebhookLog` + 장부 `idempotency_key` UNIQUE |
| 입금과 취소 동시 요청 | Funding/Payment 잠금 순서 통일 (예상했으나 초기 구현에서 누락) |
| 취소 API의 모호성 | `ideaId` 기준 최신 funding 조회 → 잘못된 건 취소 가능성 인지 |

---

## 5. 실제 일어났던 문제

### 5.1 입금 vs 취소 레이스 — 고아 장부 (치명적)

**증상**: `payments.status = FAILED`, `fundings.status = CANCELLED`인데 `vbank_ledgers`에 `FUNDING_PAID / IN`이 존재.

```
payments.id     = 931021
payments.status = FAILED
fundings.status = CANCELLED
vbank_ledgers   = FUNDING_PAID / IN / amount=110000  ← 존재
```

k6 300+ VU 동시성 테스트(`seedlink-payment-mixed-realistic.js`) + SQL 검증(`payment-ledger-validation.sql`)으로 재현.

**원인**:
- 입금: Funding만 `FOR UPDATE`, Payment는 잠금 없음
- 취소: Funding·Payment 모두 잠금 없음
- `Payment.fail()`이 SUCCESS 상태도 FAILED로 덮어씀

### 5.2 테스트 설계 오류

| 문제 | 원인 |
|------|------|
| 100 VU가 같은 payment에 몰림 | k6 `__ITER`를 row 인덱스로 사용 |
| 취소가 엉뚱한 funding 대상 | 후원자 5명 공유 + API가 ideaId 기준 최신 funding 조회 |

### 5.3 인프라 한계 (앱 버그 아님)

300+ VU에서 Tomcat accept 큐·HikariCP 포화로 HTTP 500·타임아웃 다수 발생. 완료된 요청의 장부 정합성은 대체로 일치.

---

## 6. 해결 사례

### 사례 1: 입금·취소 레이스 수정

**원칙**: 동일 `funding_id` 처리 시 항상 `funding FOR UPDATE → payment FOR UPDATE → 검증 → 상태 전이`.

```java
// completeCardPayment — AFTER
Funding funding = fundingRepository.findByIdForUpdate(paymentRef.getFundingId());
Payment payment = paymentRepository.findByIdForUpdate(paymentId);
validatePaymentConfirmable(payment, amount);
validateFundingPayable(funding, amount);
payment.complete(paymentKey);
funding.markAsPaid();
vbankLedgerService.recordIn(...);
```

```java
// cancelMySponsorship — AFTER
Funding funding = fundingRepository.findByIdForUpdate(candidate.getId());
if (funding.getStatus() == FundingStatus.PAID) {
    // 이미 입금됐으면 환불 경로
}
Payment payment = paymentRepository
    .findFirstByFundingIdAndStatusForUpdate(funding.getId(), "PENDING");
payment.fail();
funding.markAsCancelled();
```

```java
// Payment.fail() — AFTER
public void fail() {
    if (this.status == PaymentStatus.PENDING) {
        this.status = PaymentStatus.FAILED;
        return;
    }
    if (this.status == PaymentStatus.SUCCESS) {
        throw new CustomException(ErrorCode.PAYMENT_ALREADY_DONE);
    }
}
```

### 사례 2: k6 시드 설계 개선

행마다 전용 후원자 계정(`perf-concurrent-deposit{N}@`) 생성 → 취소 API가 올바른 funding을 찾도록 보장.

### 사례 3: 레이스 재현 테스트 추가

`PaymentFundingCancelRaceTest` — `confirmPayment` vs `cancelMySponsorship` 2스레드 × 5회 반복, 고아 장부 assert.

---

## 7. 해결의 이유

| 해결책 | 왜 효과적인가 |
|--------|--------------|
| Funding → Payment 잠금 순서 통일 | 두 트랜잭션이 같은 row를 다른 순서로 읽는 데드락·레이스 제거 |
| 잠금 후 상태 재검증 | 대기 중 상대 트랜잭션이 상태를 바꿔도 장부 기록 전에 차단 |
| `fail()` SUCCESS 가드 | 마지막 방어선 — 잠금 실패 시에도 SUCCESS 덮어쓰기 불가 |
| idempotency_key | 웹훅·재시도 시 장부 이중 INSERT를 DB 제약으로 차단 |
| PG Gateway 분리 | 수정·검증을 Mock 환경에서 반복 가능 |

비관적 잠금은 처리량을 일부 희생하지만, **금융에 가까운 도메인에서 정합성 > 처리량**이라 판단했습니다.

---

## 8. 결과

### 정합성 검증

| 항목 | 수정 전 (300+ VU) | 수정 후 (102 VU) |
|------|-------------------|------------------|
| `FAIL_CANCELLED_WITH_LEDGER_IN` | 간헐 다수 | **0건** |
| 장부 SQL 검증 | FAIL | **PASS** |

### 테스트 체계

| 도구 | 내용 |
|------|------|
| `PaymentFundingCancelRaceTest` | 2스레드 레이스 반복 테스트 |
| `PaymentWebhookIdempotencyTest` | 웹훅 멱등성 |
| `FundingPaymentE2ETest` | 후원→결제 E2E |
| k6 `seedlink-payment-mixed-realistic.js` | 100~1000 VU 입금·취소·환불 동시 |
| Newman Postman | 역할별 결제 시나리오 16/16 PASS |

### 담당 API 요약

| 구분 | 엔드포인트 |
|------|-----------|
| 펀딩 | `POST /fundings/{ideaId}/sponsors`, `DELETE .../sponsors/me`, `GET .../sse` |
| 결제 | `POST /payments`, `POST /payments/{id}/confirm`, `POST /payments/webhooks/**` |
| 장부 | `GET /payments/vbank-ledgers/ideas/{ideaId}` |
| 워크스페이스 | `GET/POST /workspaces/{ideaId}/messages` |

### 한 줄 요약

> k6 동시성 테스트로 **입금 vs 취소 레이스 시 고아 장부** 버그를 발견하고, `FOR UPDATE` 잠금·`fail()` 가드로 수정해 **장부 정합성 PASS**를 확인했습니다. PG 추상화와 VbankLedger로 Mock/운영 환경 모두에서 결제 흐름을 검증 가능하게 구현했습니다.

### 관련 파일

| 구분 | 경로 |
|------|------|
| 결제 서비스 | `backend/src/main/java/com/team04/domain/payment/service/PaymentService.java` |
| 펀딩 서비스 | `backend/src/main/java/com/team04/domain/funding/service/FundingService.java` |
| 장부 서비스 | `backend/src/main/java/com/team04/domain/payment/service/VbankLedgerService.java` |
| 레이스 테스트 | `backend/src/test/java/com/team04/domain/funding/PaymentFundingCancelRaceTest.java` |
| 상세 기술 문서 | `backend/docs/portfolio-payment-concurrency-testing.md` |
