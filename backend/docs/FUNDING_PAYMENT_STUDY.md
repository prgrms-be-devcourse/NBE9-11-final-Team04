# 펀딩·결제 도메인 기술 정리 & 트러블슈팅

> 면접/복기용 문서. 브랜치: `feat/-funding-payment-api`  
> 작성 기준: ERD 동기화 + FundingPaidEvent + 보조 테이블 반영 (2026-06-19)

---

## 1. 한 줄 요약

**Funding = 후원 비즈니스**, **Payment = PG 결제**로 분리했다.  
결제 완료의 기준은 **PG 승인 + DB 상태(SUCCESS/PAID)** 이다.  
`POST /fundings/ideas/{ideaId}` 한 번으로 Funding 생성 + Payment 세션까지 반환한다.  
토스 키 발급 전까지 `MockPaymentGateway`로 E2E 테스트 가능.

---

## 2. 도메인 경계 (왜 나눴는가)

| 도메인 | 책임 | 테이블 |
|--------|------|--------|
| **Funding** | 후원 요청 생성·조회, 후원 상태, 보증금 | `fundings`, `deposits` |
| **Payment** | PG 세션, 승인, 가상계좌, 웹훅 | `payments`, `vbank_deposits`, `virtual_account`, `payment_webhook_logs` |
| **Idea** (타 팀) | 프로젝트 OPEN 검증, `currentAmount` | `idea` |
| **Settlement** (타 팀) | 환불 장부, 정산 배치 | `settlement` |

**핵심 원칙**
- Funding이 Payment 테이블을 직접 건드리지 않음 → `PaymentService` 호출로 연결
- Payment는 **`funding_id`만** 참조 — `idea_id` 컬럼 없음 (ERD 의도)
- PG API는 Payment 도메인만 호출
- 결제 완료 후 `FundingPaidEvent` 발행 → **Idea 팀이 리스너로 `currentAmount` 갱신**
- 후원자 ID는 JWT에서 추출 (`@AuthenticationPrincipal`) — body에 `sponsorId` 넣지 않음

---

## 3. 패키지 구조

```
com.team04.domain/
├── funding/
│   ├── controller/ FundingController, DepositController, FundingRefundController
│   ├── service/    FundingService, DepositService, FundingRefundService
│   ├── event/      FundingPaidEvent
│   ├── entity/     Funding, Deposit, FundingTypes
│   └── repository/ FundingRepository, DepositRepository
└── payment/
    ├── controller/ PaymentController, PaymentWebhookController
    ├── service/    PaymentService, PaymentTxService, VirtualAccountService
    ├── client/     PaymentGateway, MockPaymentGateway
    ├── entity/     Payment, VbankDeposit, VirtualAccount, PaymentWebhookLog
    └── repository/ PaymentRepository, VbankDepositRepository, ...
```

**이전:** `com.team04.funding`, `com.team04.payment` (루트)  
**이후:** `com.team04.domain.*` — idea, auth, settlement와 동일한 레이아웃

---

## 4. 상태 머신

### FundingStatus
```
PENDING_PAYMENT → PAID → REFUNDED
```

### PaymentStatus
```
PENDING → SUCCESS / FAILED
SUCCESS → REFUNDED (✅ `POST /fundings/{id}/refund`)
```

### VbankDepositStatus
```
WAITING → DONE / CANCELED / EXPIRED
```

### 카드 vs 가상계좌 완료 경로
| 수단 | 완료 API |
|------|----------|
| CARD | `POST /payments/{id}/confirm` |
| VIRTUAL_ACCOUNT | `POST /payments/webhooks/toss` (confirm API 사용 안 함) |

---

## 5. API 목록

### Funding / Deposit
| Method | Path | 상태 |
|--------|------|------|
| POST | `/fundings/ideas/{ideaId}` | ✅ Funding + Payment 세션 |
| GET | `/fundings/{fundingId}` | ✅ |
| GET | `/fundings/ideas/{ideaId}` | ✅ 페이징 |
| POST | `/fundings/{fundingId}/refund` | ✅ Funding/Payment REFUNDED |
| POST | `/deposits/ideas/{ideaId}` | ✅ 보증금 HELD (Mock 납부) |
| GET | `/deposits/ideas/{ideaId}` | ✅ |
| POST | `/deposits/ideas/{ideaId}/release` | ✅ 보증금 REFUNDED |

**POST body (`CreateFundingRequest`)**
```json
{ "amount": 10000, "paymentMethod": "CARD" }
```
`paymentMethod`: `CARD` | `VIRTUAL_ACCOUNT`

**POST 응답 (`CreateFundingResponse`)**
```json
{
  "fundingId": 1,
  "ideaId": 10,
  "sponsorId": 5,
  "amount": 10000,
  "fundingStatus": "PENDING_PAYMENT",
  "createdAt": "...",
  "payment": {
    "paymentId": 1,
    "orderId": "order-1-...",
    "status": "PENDING",
    "clientKey": "test_ck_mock_seedlink",
    "redirectUrl": "...",
    "vbank": null
  }
}
```

### Payment
| Method | Path | 설명 |
|--------|------|------|
| POST | `/payments` | 결제 PENDING 생성 + Mock PG 세션 |
| POST | `/payments/{id}/confirm` | 카드 승인 |
| GET | `/payments/{id}` | 조회 |
| POST | `/payments/webhooks/toss` | 가상계좌 입금 웹훅 (인증 제외, 시크릿 헤더 필요) |

---

## 6. 핵심 플로우

### 6-0. 후원 생성 (Entry Point — 권장)

```
[Client] POST /fundings/ideas/{ideaId}
    Body: { amount, paymentMethod }
    Header: Authorization: Bearer {JWT}
    ↓
[FundingService] Idea OPEN/기간/자기후원 검증
    ↓
Funding PENDING_PAYMENT 저장
    ↓
[PaymentService] createPayment() — 기존 카드/가상계좌 플로우
    ↓
CreateFundingResponse { funding, payment } 반환
```

### 6-1. 카드 결제 (Happy Path)

```
[Client] POST /fundings/ideas/{ideaId} { amount, paymentMethod: CARD }
    ↓
[Tx1] PaymentTxService.createPendingPayment()
    - Funding 비관적 락 (findByIdForUpdate)
    - 중복 PENDING/SUCCESS 검증
    - Payment PENDING 저장, orderId 서버 발급
    ↓
[Tx 밖] MockPaymentGateway.createSession()
    ↓
[Client] POST /payments/{id}/confirm { paymentKey, amount }
    ↓
[Tx] prepareConfirm() — 검증만, 락 X
    ↓
[Tx 밖] MockPaymentGateway.confirm()
    ↓
[Tx2] completeCardPayment()
    - Funding 비관적 락
    - Payment SUCCESS, Funding PAID
    - FundingPaidEvent 발행 (Idea currentAmount는 Idea 팀 리스너)
```

### 6-2. 가상계좌

```
POST /fundings/ideas/{ideaId} { amount, paymentMethod: VIRTUAL_ACCOUNT }
    ↓
[Tx1] PENDING 저장
[Tx 밖] createSession + issueVirtualAccount
[Tx2] VbankDeposit WAITING 저장
    ↓
(사용자 입금)
    ↓
POST /payments/webhooks/toss
    Header: X-Webhook-Secret
    Body: { orderId, status: "DONE", amount }
    ↓
시크릿 검증 → PG verify → completeDepositWebhook()
    → Funding PAID + FundingPaidEvent 발행 (Idea 갱신은 Idea 팀 리스너)
```

> **레거시 경로:** `POST /payments`에 `fundingId`를 직접 넘기는 방식도 동작하지만, 정상 플로우는 `/fundings` 진입을 사용한다.

---

## 7. 멱등성 (Idempotency) — 어디에 뒀는가

### 7-1. 웹훅 멱등 (가장 명시적)

**위치:** `PaymentTxService.completeDepositWebhook()`

```java
if (payment.getStatus() == PaymentStatus.SUCCESS) {
    return;  // 이미 완료 → 200 OK, 추가 처리 없음
}
```

- PG가 웹훅을 **여러 번** 보내도 두 번째부터는 no-op
- HTTP 응답은 항상 성공 (재전송 방지)

### 7-2. 중복 결제 방지 (생성 단계)

**위치:** `PaymentTxService.createPendingPayment()`

```java
validateNoSuccessfulPayment(fundingId);  // SUCCESS 있으면 F004/P006
validateNoPendingPayment(fundingId);   // PENDING 있으면 F004
```

- funding 1건당 **동시에 PENDING 1개**만 허용
- orderId는 UUID suffix로 **서버 발급** (클라이언트가 만들지 않음)

### 7-3. confirm 멱등

**위치:** `validatePaymentConfirmable()`

```java
if (payment.getStatus() == PaymentStatus.SUCCESS) {
    throw PAYMENT_ALREADY_DONE;  // 웹훅과 달리 confirm은 에러 반환
}
```

- 웹훅: 조용히 return (PG 재시도 친화)
- confirm: 이미 SUCCESS면 **409** — 클라이언트 중복 호출 감지용

### 7-4. orderId / paymentKey 유니크

- `payments.order_id` UNIQUE
- `payments.payment_key` UNIQUE
- DB 레벨 이중 방어 (애플리케이션 검증 + 제약)

### 7-5. 아직 없는 멱등

- [ ] PG confirm API 자체의 idempotency-key (토스 연동 시)
- [ ] 웹훅 이벤트 ID 저장 테이블 (동일 이벤트 UUID 중복 수신)
- [x] `FundingService.createFunding` — Idea 검증 + PaymentService 오케스트레이션 (2026-06-18)

---

## 8-A. Payment 인프라 설계 정리 (면접용)

> 이 섹션은 **왜 이렇게 설계했는지**를 한 번에 설명하기 위한 요약이다.

### 8-A-1. 아키텍처 한 장 요약

```
Client
  → FundingController (비즈니스 진입)
  → FundingService (Idea 검증 + Funding 저장)
  → PaymentService (오케스트레이션, PG 호출)
       ↔ PaymentGateway (Mock / Toss)
  → PaymentTxService (짧은 DB Tx, 락, 상태 전이)
       ↔ FundingRepository (비관적 락)
       ↔ IdeaRepository (누적금 갱신)
```

| 레이어 | 클래스 | 책임 | Tx 범위 |
|--------|--------|------|---------|
| API | `FundingController` | 후원 요청 수신, JWT 후원자 추출 | 없음 |
| 비즈니스 | `FundingService` | Idea 검증, Funding 생성, Payment 위임 | Funding save만 (repo 기본 Tx) |
| 오케스트레이션 | `PaymentService` | PG 세션/승인/웹훅 **네트워크 호출** | **Tx 밖** |
| DB 전용 | `PaymentTxService` | PENDING 생성, SUCCESS 완료, Idea 갱신 | **짧은 Tx** |
| 인프라 | `PaymentGateway` | PG 추상화 | 없음 |

**면접 한 줄:** "결제는 **비즈니스(Funding)와 PG(Payment)를 분리**하고, DB 쓰기는 **PaymentTxService**에 모아 **PG 호출은 트랜잭션 밖**으로 뺐다."

---

### 8-A-2. 비관적 락 (Pessimistic Lock)

**위치:** `FundingRepository.findByIdForUpdate()`

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT f FROM Funding f WHERE f.id = :id")
Optional<Funding> findByIdForUpdate(@Param("id") Long id);
```

| 시점 | 이유 |
|------|------|
| Payment PENDING 생성 | 동일 funding에 동시 결제 요청 직렬화 |
| 카드/웹훅 완료 | Funding PAID + Idea 금액 갱신 원자성 |

**왜 Funding만 락?**  
Payment는 `fundingId` 기준으로 검증된다. 충돌 단위는 "한 건의 후원"이므로 Funding row를 잠근다.

**낙관적 락(@Version) 대신 비관적 락을 쓴 이유**  
결제는 충돌 시 재시도보다 **선점(먼저 잠근 쪽만 진행)** 이 맞다. 재시도 루프는 PG 중복 호출·사용자 혼란을 유발한다.

---

### 8-A-3. 멱등성 (Idempotency) 4단 방어

| 단계 | 위치 | 동작 | HTTP |
|------|------|------|------|
| 웹훅 재수신 | `completeDepositWebhook()` | `SUCCESS`면 early return | 200 (no-op) |
| 중복 PENDING | `createPendingPayment()` | funding당 PENDING 1개 | 409 F004 |
| 중복 SUCCESS | `validateNoSuccessfulPayment()` | SUCCESS 존재 시 차단 | 409 P006 |
| confirm 재호출 | `validatePaymentConfirmable()` | 이미 SUCCESS면 예외 | 409 P006 |
| DB 제약 | `payments.order_id` UNIQUE | 서버 발급 orderId | DB 에러 |

**웹훅 vs confirm 차이**  
- 웹훅: PG가 재전송하므로 **조용히 return** (200 OK)  
- confirm: 클라이언트 중복 호출 감지용 **409 반환**

**orderId 서버 발급**  
`order-{fundingId}-{uuid12}` — 클라이언트가 만들면 금액 조작·충돌 위험.

---

### 8-A-4. 트랜잭션 분리 (핵심 리팩터링)

#### 문제 (초기 안티패턴)
```java
@Transactional
public void confirm() {
    funding = findByIdForUpdate();  // 락 획득
    paymentGateway.confirm();       // PG 네트워크 대기 (수 초)
    payment.complete();             // 락 + 커넥션 계속 점유
}
```
→ PG 지연 시 **DB 커넥션 풀 고갈** + **락 장기 점유** → 동시 요청 대기열

#### 해결
```
PaymentService (Tx 없음)
  Tx1: prepareConfirm / createPending  (짧게)
  PG 호출                             (Tx 밖, 네트워크)
  Tx2: completeCardPayment            (짧게, 락 → 완료 → 해제)
```

| 메서드 | Tx | PG |
|--------|----|----|
| `createPayment` | Tx1 → (PG) → Tx2(가상계좌만) | 밖 |
| `confirmPayment` | Tx(prepare) → (PG) → Tx(complete) | 밖 |
| `processDepositWebhook` | (PG verify) → Tx(complete) | 밖 |

**PG 실패 보상:** `createPayment`에서 PG 예외 → `failPayment()`로 PENDING → FAILED

**self-invocation 함정 회피:** `@Transactional`은 별도 빈(`PaymentTxService`)으로 분리 — 같은 클래스 내부 호출은 프록시 안 탐.

---

### 8-A-5. Gateway 추상화 (Mock → Toss 교체)

```java
public interface PaymentGateway {
    PaymentSessionResult createSession(String orderId, long amount, PaymentMethod method);
    PaymentConfirmResult confirm(String paymentKey, String orderId, long amount);
    VirtualAccountIssueResult issueVirtualAccount(String orderId, long amount);
    PaymentVerifyResult verifyVirtualAccountDeposit(String orderId, long amount);
}
```

| 구현체 | Profile | 용도 |
|--------|---------|------|
| `MockPaymentGateway` | 현재 유일 `@Component` | 로컬·CI E2E |
| `TossPaymentGateway` | `@Profile("!local")` (예정) | RestClient 실연동 |

**Mock 동작 요약**
- `confirm`: `paymentKey`가 `fail-`로 시작하면 실패
- `issueVirtualAccount`: 은행 088, 12자리 가짜 계좌, 만료 3일
- `verifyVirtualAccountDeposit`: orderId/amount 형식만 검증 (스텁)

**왜 Mock을 먼저?** 토스 테스트 키·가상계좌 연동 전에 API 계약·상태 전이·웹훅 플로우를 팀과 맞추기 위함.

---

### 8-A-6. 웹훅 보안 2단

1. **공유 시크릿:** `X-Webhook-Secret` 헤더 (`payment.webhook.secret`, 기본 `dev-webhook-secret`)
2. **Read-through Verification:** `paymentGateway.verifyVirtualAccountDeposit()` — Mock은 형식 검증, Toss는 실제 입금 조회

`/payments/webhooks/**`는 `permitAll()`이지만, 위 2단 없이는 결제 완료 불가.

**운영 추가 권장:** Toss HMAC 서명, IP allowlist, 웹훅 이벤트 ID 멱등 테이블.

---

### 8-A-7. 결제 완료 정의 (서버 기준)

| 조건 | Funding | Payment | Idea |
|------|---------|---------|------|
| 카드/가상계좌 | `PAID` | `SUCCESS` | **`FundingPaidEvent` 수신 후 Idea 팀이 갱신** |

**프론트 결제창 성공 화면 ≠ 후원 완료.** 반드시 서버 DB 상태를 확인한다.  
Idea `currentAmount`는 Payment/Funding API 응답이 아니라 **Idea API**로 확인한다.

---

### 8-A-8. 면접 예상 Q&A (확장)

| 질문 | 답변 포인트 |
|------|-------------|
| 펀딩과 결제를 왜 분리? | 변경 주기·장애 반경 분리. PG 장애가 후원 조회까지 전파되지 않음 |
| 트랜잭션에서 PG 호출하면? | 락+커넥션 장기 점유 → 풀 고갈. Tx는 DB만, PG는 밖 |
| 동시에 같은 후원에 결제 2번? | Funding 비관적 락 + funding당 PENDING 1개 + SUCCESS 중복 차단 |
| 웹훅 2번 오면? | SUCCESS early return, 200 OK, DB 변경 없음 |
| Mock PG 한계? | verify는 스텁. 운영 전 Toss 연동 + 실입금 검증 필수 |
| sponsorId를 body로 받지 않는 이유? | JWT 위조 방지. 타인 명의 후원 불가 |

---

## 8. 동시성 — 비관적 락

**위치:** `FundingRepository.findByIdForUpdate()`

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT f FROM Funding f WHERE f.id = :id")
Optional<Funding> findByIdForUpdate(@Param("id") Long id);
```

**쓰는 시점**
- 결제 PENDING 생성
- 카드/웹훅 완료 처리 (Funding PAID 전환)

**왜 Funding만 락?**
- Payment는 fundingId 기준으로 검증
- 동일 funding에 대한 **동시 결제 시도**를 직렬화

**면접 포인트:** 낙관적 락(@Version) vs 비관적 락 — 결제는 충돌 시 재시도보다 **선점**이 맞다고 판단

---

## 9. 트랜잭션 분리 (PR 리뷰 대응)

### 9-1. 문제

초기 구현: `@Transactional` 메서드 **안에서** `paymentGateway.confirm()` 호출  
+ `findByIdForUpdate`로 **비관적 락** 유지

→ PG 네트워크 지연 시 **DB 커넥션 + 락**을 오래 쥠 → 풀 고갈·대기열 위험

### 9-2. 해결 — PaymentService / PaymentTxService 분리

| 클래스 | 역할 |
|--------|------|
| `PaymentService` | 오케스트레이션, **PG 호출 (Tx 밖)** |
| `PaymentTxService` | **짧은 DB 트랜잭션**만 |

```
createPayment:
  Tx1 → PG 호출 → Tx2(가상계좌만)

confirmPayment:
  Tx(prepare) → PG confirm → Tx(complete)

webhook:
  PG verify → Tx(complete)
```

### 9-3. PG 실패 시 보상

`createPayment`에서 PG 호출 예외 발생 → `failPayment()`로 PENDING → FAILED

---

## 10. PG 연동 — MockPaymentGateway

**인터페이스:** `PaymentGateway`

| 메서드 | Mock 동작 |
|--------|-----------|
| `createSession` | clientKey + redirectUrl 반환 |
| `confirm` | paymentKey `fail-` prefix면 실패 |
| `issueVirtualAccount` | 은행코드 088, 가짜 계좌번호 |
| `verifyVirtualAccountDeposit` | orderId/amount 유효성만 검사 (스텁) |

**토스 연동 시:** `@Profile("!local")` `TossPaymentGateway` 추가, Mock 교체  
**HTTP 클라이언트:** 팀 컨벤션 `RestClient` (WebClient 아님)

---

## 11. 웹훅 보안

### 11-1. 문제 (PR 리뷰)

`/payments/webhooks/**` = `permitAll()`  
+ 검증 없음 → **가짜 orderId로 결제 완료** 가능 (결제 우회)

### 11-2. 적용한 방어 (2단)

1. **공유 시크릿 헤더**
   - `X-Webhook-Secret: dev-webhook-secret` (설정: `payment.webhook.secret`)
   - 틀리면 `403 FORBIDDEN`

2. **Read-through Verification (스텁)**
   - `paymentGateway.verifyVirtualAccountDeposit(orderId, amount)`
   - Mock: 형식 검증만
   - 토스: `GET /v1/payments/orders/{orderId}` 등으로 **실제 입금 확인**

### 11-3. 토스 연동 시 추가 권장

- [ ] Toss 웹훅 **서명(HMAC)** 검증
- [ ] IP allowlist
- [x] 웹훅 이벤트 ID 멱등 테이블 → `payment_webhook_logs`

---

## 12. 트러블슈팅 로그

### 12-1. UTF-8 BOM (`\ufeff`) — Java 컴파일 실패

**증상**
```
error: illegal character: '\ufeff'
```

**원인**
- Cursor 전역 설정 `"files.encoding": "utf8bom"`
- Write 도구로 생성된 파일 앞에 BOM 붙음
- Java 컴파일러는 BOM 인식 못 함

**해결**
1. Cursor 설정 → `"files.encoding": "utf8"` (BOM 없음)
2. `.editorconfig` → `charset = utf-8`
3. 기존 파일 BOM strip (PowerShell 바이트 제거)

---

### 12-2. IDE 빨간줄 — "PaymentService cannot be resolved"

**증상:** Gradle compileJava 성공, IDE만 에러

**원인**
- 워크스페이스 루트가 repo 전체, Gradle 프로젝트는 `backend/`
- Java Language Server 동기화 안 됨
- 새 패키지 untracked 상태

**해결**
- `Java: Clean Java Language Server Workspace` → Reload
- 또는 `backend` 폴더만 열기

---

### 12-3. Git 패키지 이동 — `git add funding/` 실패

**증상**
```
could not open directory '.../funding/': No such file or directory
```

**원인:** 폴더는 이미 `domain/funding`으로 이동, Git은 rename으로 추적 중

**해결:** `git add domain/funding/` 만 하면 됨. `git status`에 `renamed:`로 표시되면 OK

---

### 12-4. `@Transactional` + self-invocation

**개념:** 같은 클래스 내부에서 `@Transactional` 메서드 호출 → **프록시 안 탐**

**해결:** `PaymentTxService` 별도 빈으로 분리 (이번 리팩터링)

---

### 12-5. 가상계좌 confirm API 호출

**문제:** 가상계좌에 `POST /payments/{id}/confirm` 호출하면?

**처리:** `PAYMENT_NOT_READY` — 가상계좌는 **웹훅으로만** 완료

---

## 13. 미구현 / 후속 작업

| 항목 | 담당/비고 |
|------|-----------|
| ~~`FundingService.createFunding`~~ | ✅ |
| ~~`virtual_account` 테이블~~ | ✅ `VirtualAccount` 엔티티 |
| ~~`payment_webhook_logs`~~ | ✅ 웹훅 멱등 |
| ~~Deposit API~~ | ✅ Mock HELD (PG 연동은 후속) |
| ~~환불 상태 전이~~ | ✅ `FundingRefundService` (PG 환불 API는 후속) |
| ~~가상계좌 EXPIRED~~ | ✅ `VbankExpireScheduler` |
| **Idea `currentAmount` 갱신** | **Idea 팀** — `FundingPaidEvent` 리스너 구현 필요 |
| `TossPaymentGateway` | 키 발급 후 RestClient 연동 |
| 보증금 PG 실결제 | Deposit + Payment 연동 |
| Flyway 마이그레이션 | `ddl-auto: none` 운영 환경 |
| PG 환불 API | `FundingRefundService`는 DB만 반영 |

---

## 14. 면접용 말하기 스크립트

### Q. 펀딩과 결제를 왜 분리했나?

> 후원(비즈니스)과 PG(인프라)의 변경 주기·장애 반경이 다릅니다. PG 장애가 후원 조회까지 막히지 않게, Funding은 PaymentService 인터페이스만 알면 됩니다.

### Q. 결제 완료를 어떻게 정의했나?

> PG 승인(또는 가상계좌 입금 확인) + DB에서 Payment SUCCESS, Funding PAID. PG만 성공하고 DB 실패, 또는 그 반대 모두 불완전한 상태로 봅니다.

### Q. 멱등성은?

> 웹훅은 SUCCESS면 early return. 생성 시 funding당 PENDING 1개 제한. orderId 서버 발급 + UNIQUE. confirm 중복은 409.

### Q. 트랜잭션에서 PG 호출하면?

> 비관적 락 + 네트워크 대기로 커넥션 풀 고갈 위험. Tx는 DB만, PG는 Tx 밖으로 분리했습니다.

### Q. 웹훅 보안은?

> permitAll이지만 X-Webhook-Secret + PG verify 2단. 운영에선 Toss 서명 검증 추가 예정.

### Q. Mock PG를 쓴 이유?

> 토스 테스트 키·가상계좌 연동 전에 API·상태 전이·웹훅 플로우를 팀과 맞추기 위함. PaymentGateway 인터face로 교체 가능.

---

## 15. 로컬 테스트 치트시트 (Postman)

### 사전 조건
- Idea 상태: `OPEN` (또는 `IN_PROGRESS`)
- 펀딩 기간 내 (`fundingStartAt` ~ `fundingEndAt`)
- JWT: 후원자(SPONSOR) 토큰 — `sponsorId`는 body에 넣지 않음

### 카드 E2E (권장 플로우)
```http
# 1. 후원 + 결제 세션 생성
POST /fundings/ideas/{ideaId}
Authorization: Bearer {JWT}
Content-Type: application/json

{
  "amount": 10000,
  "paymentMethod": "CARD"
}

# 응답: data.payment.paymentId, data.payment.clientKey 사용

# 2. 카드 승인
POST /payments/{paymentId}/confirm
Authorization: Bearer {JWT}
Content-Type: application/json

{
  "paymentKey": "mock-key-success",
  "amount": 10000
}

# 3. 확인 (Funding/Payment)
GET /fundings/{fundingId}  → status: PAID

# 4. 확인 (Idea — Idea 팀 리스너 구현 후)
GET /ideas/{ideaId}          → currentAmount, supporterCount 증가
```

### 보증금
```http
POST /deposits/ideas/{ideaId}
Authorization: Bearer {창작자 JWT}
{ "amount": 100000 }
```

### 가상계좌 E2E
```http
# 1. 후원 + 가상계좌 발급
POST /fundings/ideas/{ideaId}
Authorization: Bearer {JWT}

{
  "amount": 50000,
  "paymentMethod": "VIRTUAL_ACCOUNT"
}

# 응답: data.payment.orderId, data.payment.vbank.accountNumber

# 2. 입금 웹훅 (인증 불필요, 시크릿 헤더 필요)
POST /payments/webhooks/toss
X-Webhook-Secret: dev-webhook-secret
Content-Type: application/json

{
  "eventId": "toss-event-uuid-optional",
  "orderId": "{응답의 orderId}",
  "status": "DONE",
  "amount": 50000
}
```
`eventId` 생략 시 `auto-{orderId}-{amount}`로 자동 생성. 동일 `eventId` 재수신은 no-op.

### 레거시 (fundingId를 이미 알 때)
```http
POST /payments
{ "fundingId": 1, "amount": 10000, "method": "CARD" }
```

### 실패 테스트
- confirm `paymentKey`: `"fail-anything"` → PAYMENT_FAILED
- OPEN이 아닌 Idea 후원 → F005
- 본인 프로젝트 후원 → F006
- 펀딩 기간 외 → F002

### 자동화 테스트
```bash
cd backend
./gradlew test --tests "com.team04.domain.funding.FundingPaymentE2ETest"
```

---

## 16. 참고 파일 빠른 링크

| 주제 | 파일 |
|------|------|
| 오케스트레이션 | `domain/payment/service/PaymentService.java` |
| DB 트랜잭션 | `domain/payment/service/PaymentTxService.java` |
| Mock PG | `domain/payment/client/MockPaymentGateway.java` |
| 웹훅 | `domain/payment/controller/PaymentWebhookController.java` |
| 비관적 락 | `domain/funding/repository/FundingRepository.java` |
| 후원 생성 | `domain/funding/service/FundingService.java` |
| 후원+결제 응답 | `domain/funding/dto/response/CreateFundingResponse.java` |
| E2E 테스트 | `src/test/java/.../funding/FundingPaymentE2ETest.java` |

---

## 17. ERD ↔ 코드 동기화 (2026-06-19)

### 관계도 (ERD에 그려야 할 최종 형태)

```
idea ──< fundings ──< payments ──< vbank_deposits
  │                      │
  │                      └──> virtual_account (FK: virtual_account_id)
  │
  └──< deposits

users ──< fundings (sponsor_id)
users ──< deposits (user_id)

payment_webhook_logs  (order_id, event_id UNIQUE)
```

### 테이블별 컬럼 매핑

#### `fundings`
| ERD 컬럼 | 코드 필드 | 비고 |
|----------|-----------|------|
| id | id | PK |
| idea_id | ideaId | Long FK |
| sponsor_id | sponsorId | Long FK |
| milestone_step | milestoneStep | 마일스톤 IN_PROGRESS/PENDING 기준 자동 |
| amount | amount | |
| reward_type | rewardType | 후원 시점 Idea.rewardType 스냅샷 |
| status | status | PENDING_PAYMENT / PAID / REFUNDED |
| refunded_at | refundedAt | 환불 시 |
| created_at / updated_at | BaseEntity | |

#### `payments` — **`idea_id` 없음 (의도적)**
| ERD 컬럼 | 코드 필드 |
|----------|-----------|
| funding_id | fundingId |
| payment_key | paymentKey |
| order_id | orderId (UNIQUE) |
| method | method |
| amount | amount |
| status | status (SUCCESS — 기획서 DONE과 동일 의미) |
| approved_at | approvedAt |
| refunded_at | refundedAt |

#### `vbank_deposits`
| ERD 컬럼 | 코드 필드 |
|----------|-----------|
| payment_id | paymentId (UNIQUE) |
| virtual_account_id | virtualAccountId → `virtual_account.id` |
| bank_code | bankCode |
| account_number | accountNumber |
| due_date | dueDate |
| deposit_status | depositStatus |
| deposited_at | depositedAt |

#### `virtual_account` (신규)
| 컬럼 | 코드 필드 |
|------|-----------|
| order_id | orderId (UNIQUE) |
| bank_code | bankCode |
| account_number | accountNumber |
| due_date | dueDate |
| amount | amount |

#### `payment_webhook_logs` (신규)
| 컬럼 | 코드 필드 |
|------|-----------|
| event_id | eventId (UNIQUE, 멱등) |
| order_id | orderId |
| status | status |
| amount | amount |
| provider | provider (`toss`) |

#### `deposits`
| ERD 컬럼 | 코드 필드 |
|----------|-----------|
| idea_id | ideaId |
| user_id | userId |
| amount | amount |
| status | status (HELD / REFUNDED / FORFEITED) |
| paid_at | paidAt |
| released_at | releasedAt |

### ERD에 추가할 제약
- `payments.order_id` UNIQUE
- `payments.payment_key` UNIQUE
- `vbank_deposits.payment_id` UNIQUE
- `payment_webhook_logs.event_id` UNIQUE
- `virtual_account.order_id` UNIQUE
- FK: fundings→idea, payments→fundings, vbank_deposits→payments, vbank_deposits→virtual_account

---

## 18. Idea 팀 협업 가이드 (필수)

### 배경
Payment/Funding 도메인은 ERD대로 **`payments`에 `idea_id`가 없다.**  
결제 완료 시 Funding만 `PAID`가 되고, Idea 누적금은 **이벤트로 위임**한다.

### Funding 팀이 하는 일 (완료)
1. `PaymentTxService` — Payment SUCCESS + Funding PAID까지만 처리
2. `PaymentService` — 완료 후 `FundingPaidEvent` 발행

```java
// funding/event/FundingPaidEvent.java
public record FundingPaidEvent(
    Long fundingId,
    Long ideaId,
    Long sponsorId,
    Long amount
) {}
```

### Idea 팀이 해야 할 일

**1. 이벤트 리스너 구현** (`idea` 패키지에 추가)

```java
@Component
@RequiredArgsConstructor
public class IdeaFundingPaidListener {

    private final IdeaRepository ideaRepository;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onFundingPaid(FundingPaidEvent event) {
        Idea idea = ideaRepository.findByIdForUpdate(event.ideaId())
            .orElseThrow(() -> new CustomException(ErrorCode.IDEA_NOT_FOUND));
        idea.addFundingAmount(event.amount());
    }
}
```

**2. `Idea` 엔티티에 도메인 메서드** (이미 존재)

```java
public void addFundingAmount(Long amount) {
    this.currentAmount += amount;
    this.supporterCount += 1;
}
```

**3. `IdeaRepository`에 비관적 락 조회** (이미 존재)

```java
Optional<Idea> findByIdForUpdate(Long id);
```

### 왜 Idea 팀이 해야 하나?
- ERD: `idea ← fundings ← payments` — Idea 갱신은 Idea 도메인 책임
- Payment가 `IdeaRepository`를 직접 쓰면 도메인 경계 위반
- `@TransactionalEventListener(AFTER_COMMIT)`으로 결제 Tx 커밋 **이후** 갱신 → 정합성

### Idea 팀 체크리스트
- [ ] `IdeaFundingPaidListener` 구현
- [ ] 통합 테스트: 후원 완료 후 `GET /ideas/{id}` → `currentAmount` 증가 확인
- [ ] (선택) 목표 달성 시 `IdeaStatus` → `IN_PROGRESS` 전이 검토
- [ ] (선택) 환불 시 `FundingRefundedEvent` 수신해 `currentAmount` 차감

### 슬랙/PR에 전달할 한 줄
> "결제 완료되면 `FundingPaidEvent(ideaId, amount)` 날아갑니다. Idea 쪽에서 `@TransactionalEventListener`로 `currentAmount`/`supporterCount` 올려주세요."

---

*마지막 업데이트: 2026-06-19 — ERD 동기화, FundingPaidEvent, 보조 테이블, Idea 팀 가이드*
