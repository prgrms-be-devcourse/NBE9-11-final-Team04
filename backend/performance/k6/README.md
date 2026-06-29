# SeedLink 로컬 성능 테스트

이 폴더는 `/Users/apple/Desktop/performance-test-plan.md` 기준으로 로컬 dev 환경에서 k6를 실행하기 위한 스크립트를 둡니다.

## 공통 실행 전제

- 브랜치: `dev`
- 서버: `http://localhost:8080`
- 테스트 전 팀 공통 seed 데이터 준비
- 실제 Toss 지급/환불은 실행하지 않음
- 쓰기/상태 변경 시나리오는 테스트 DB에서만 실행

## 폴더 구조

- `read/`: 조회 API 중심 성능 테스트
- `consistency/`: 동시 요청 정합성 테스트
- `consistency-perf/`: 정합성 테스트에 응답 시간 기준을 함께 적용한 테스트

## 팀 공통 seed 데이터

공통 seed 파일은 `performance/seed/performance-seed.sql`입니다.

이 파일은 팀원이 같은 계정과 같은 ID로 Postman/k6 시나리오를 맞춰 실행하기 위한 로컬/테스트 DB 전용 데이터입니다. 운영 DB에는 절대 실행하지 않습니다.

```bash
mysql -u root -p seedlink < performance/seed/performance-seed.sql
```

공통 로그인 비밀번호는 모두 `password`입니다.

| 역할 | 이메일 |
| --- | --- |
| 관리자 | `perf-admin@seedlink.test` |
| 제안자 | `perf-proposer@seedlink.test` |
| 후원자 1 | `perf-sponsor01@seedlink.test` |
| 후원자 2 | `perf-sponsor02@seedlink.test` |
| 후원자 3 | `perf-sponsor03@seedlink.test` |

자주 쓰는 고정 ID입니다.

| 변수 | 값 | 용도 |
| --- | --- | --- |
| `IDEA_ID` | `900002` | 진행 중 마일스톤/정산/장부 조회 기준 |
| `IDEA_ID_CLOSING` | `900001` | 펀딩 마감 직전 조회 부하 |
| `IDEA_ID_SUCCESS_CLOSED` | `900003` | 마감 후 성공 확정 스케줄러 |
| `IDEA_ID_FAILED_CLOSED` | `900004` | 마감 후 목표 미달성 환불 |
| `IDEA_ID_MILESTONE` | `900005` | 보고서 검토 집중 |
| `IDEA_ID_REFUND_DUPLICATE` | `900006` | 환불 중복 생성/콜백 정합성 |
| `IDEA_ID_DEPOSIT_DECISION` | `900007` | 보증금 환급/몰수 동시 처리 |
| `IDEA_ID_FINAL_SETTLEMENT` | `900008` | 3단계 완료/최종 정산 중복 생성 |
| `IDEA_ID_REPORT_DECISION` | `900009` | 보고서 승인/반려 동시 처리 |
| `IDEA_ID_FUND_USAGE_LIMIT` | `900010` | 자금 사용 내역 한도 정합성 |
| `IDEA_ID_PAYMENT_DEPOSIT` | `900011` | 보증금 결제/가상계좌 웹훅 |
| `MILESTONE_ID` | `910010` | 제출된 완료 보고서가 있는 마일스톤 |
| `REPORT_ID` | `950001` | 승인 대기 완료 보고서 |
| `REFUND_ID_PENDING` | `970010` | 환불 완료/실패 콜백 중복 처리 |
| `PAYMENT_ID_CARD_PENDING` | `930022` | 카드 결제 confirm 대상 |
| `PAYMENT_ID_VBANK_WAITING` | `930023` | 후원 가상계좌 입금 웹훅 대상 |
| `PAYMENT_ID_REFUNDABLE` | `930024` | 후원자 직접 환불 대상 |
| `PAYMENT_ID_DEPOSIT_VBANK` | `930025` | 보증금 가상계좌 입금 웹훅 대상 |

## 부하 단계

`LOAD_PROFILE` 값으로 부하 단계를 선택합니다.

- `local`: 로컬 장비 확인용
- `normal`: 100명
- `target`: 150명
- `scale`: 500명
- `limit`: 1000명

기본값은 로컬 장비 보호를 위해 `local`입니다.

## 1차 권장 실행: 조회 중심

펀딩 마감 전후 집중 유입, 프로젝트 자금 흐름 조회 집중을 먼저 확인합니다.

```bash
BASE_URL=http://localhost:8080 \
USER_EMAIL=perf-sponsor01@seedlink.test \
USER_PASSWORD=password \
IDEA_ID=900002 \
MILESTONE_ID=910010 \
LOAD_PROFILE=normal \
k6 run performance/k6/read/seedlink-local-read.js
```

로그인이 필요한 조회 API는 `USER_EMAIL`, `USER_PASSWORD`가 있을 때만 인증 헤더를 붙입니다.

## 쓰기/동시성 시나리오 주의

아래 시나리오는 데이터 상태를 바꾸므로 같은 seed 데이터와 복구 절차를 맞춘 뒤 별도 실행해야 합니다.

- 완료/소명 보고서 제출
- 관리자 승인/반려
- 선정산 신청
- 강제 환불
- 환불 완료/실패 콜백
- 지급 완료/실패 콜백
- 웹훅 멱등성 테스트
- 카드 결제 승인
- 가상계좌 입금 웹훅
- 후원자 직접 환불
- 보증금 결제 입금 웹훅

이 시나리오들은 같은 ID에 반복 요청하면 정상적으로 400 계열 상태 전이 오류가 날 수 있습니다. 성능 테스트에서는 오류율만 보지 말고 중복 생성 여부, 상태 전이 정합성, 장부 중복 여부를 함께 확인해야 합니다.

## 결제 도메인 테스트용 고정 데이터

결제 쪽은 같은 요청을 반복하면 상태가 바뀌므로 테스트 전 seed 데이터를 다시 넣어 초기화합니다.

| 시나리오 | 고정 ID | 기대 흐름 |
| --- | --- | --- |
| 카드 결제 승인 | `PAYMENT_ID_CARD_PENDING=930022` | `PENDING` 결제를 confirm/demo-confirm 하면 `SUCCESS`, Funding은 `PAID` |
| 후원 가상계좌 입금 웹훅 | `PAYMENT_ID_VBANK_WAITING=930023` | `WAITING` 가상계좌 입금 웹훅 후 Payment `SUCCESS`, Funding `PAID`, 웹훅 로그 `DONE` |
| 후원자 직접 환불 | `PAYMENT_ID_REFUNDABLE=930024` | Payment `SUCCESS` + Funding `PAID` 상태에서 환불 후 둘 다 환불 상태, vbank ledger 출금 기록 |
| 보증금 가상계좌 입금 웹훅 | `PAYMENT_ID_DEPOSIT_VBANK=930025` | 보증금 Payment `SUCCESS`, Deposit `HELD`, vbank ledger 보증금 입금 기록 |
| 웹훅 멱등성 기준 로그 | `perf-existing-webhook-1` | 같은 eventId 재호출 시 중복 처리 없이 무시되는지 확인 |

## 정합성 테스트: 선정산 동시 신청

선정산은 아이디어 단위 한도 검증이 핵심이므로 동시 요청 테스트를 별도로 실행합니다.

실행 전 seed 데이터를 다시 넣어 테스트 상태를 초기화합니다.

```bash
mysql -u root seedlink < performance/seed/performance-seed.sql
```

동시에 100개의 선정산 신청을 보냅니다.

```bash
BASE_URL=http://localhost:8080 \
USER_EMAIL=perf-proposer@seedlink.test \
USER_PASSWORD=password \
IDEA_ID=900002 \
AMOUNT=100000 \
VUS=100 \
ITERATIONS=100 \
MAX_DURATION=3m \
REQUEST_TIMEOUT=120s \
k6 run performance/k6/consistency/seedlink-consistency-pre-settlement.js
```

이 테스트에서 `200` 성공과 `PS001` 한도 초과 거절은 모두 정상입니다.
중요한 것은 그 외 500 오류나 예상하지 못한 400 오류가 없어야 한다는 점입니다.

테스트 후 DB 정합성을 확인합니다.

```bash
mysql -u root seedlink < performance/validation/pre-settlement-consistency-check.sql
```

검증 쿼리 1~3은 결과가 없어야 통과입니다.
마지막 상태별 분포 쿼리는 성공/거절 이후 DB 상태를 해석하기 위한 참고용입니다.

## 정합성 테스트: 후원자 직접 환불 동시 요청

동일 결제건에 환불 요청이 동시에 들어와도 결제/후원/장부가 한 번만 환불 처리되는지 확인합니다.

실행 전 seed 데이터를 다시 넣어 테스트 상태를 초기화합니다.

```bash
mysql -u root seedlink < performance/seed/performance-seed.sql
```

동시에 50개의 환불 요청을 보냅니다.

```bash
BASE_URL=http://localhost:8080 \
USER_EMAIL=perf-sponsor03@seedlink.test \
USER_PASSWORD=password \
PAYMENT_ID=930024 \
VUS=50 \
ITERATIONS=50 \
MAX_DURATION=3m \
REQUEST_TIMEOUT=120s \
DEBUG_UNEXPECTED=true \
k6 run performance/k6/consistency/seedlink-consistency-payment-refund.js
```

정상 기준은 성공이 최대 1건이고, 나머지는 이미 환불되었거나 환불 가능한 상태가 아니라는 400 계열 거절이어야 합니다.
500 오류와 예상하지 못한 응답은 없어야 합니다.

테스트 후 DB 정합성을 확인합니다.

```sql
SELECT id, status, refunded_at
FROM payments
WHERE id = 930024;

SELECT id, status, refunded_at
FROM fundings
WHERE id = 920024;

SELECT id, idea_id, type, direction, amount, idempotency_key
FROM vbank_ledgers
WHERE idempotency_key = 'payment-930024-SPONSOR-REFUND';

SELECT id, current_amount
FROM idea
WHERE id = 900002;
```

## 정합성 테스트: 자금 사용 내역 한도 동시 등록

선정산으로 실제 지급받은 금액을 초과하는 자금 사용 내역이 동시 등록되지 않는지 확인합니다.

seed 기준 `IDEA_ID_FUND_USAGE_LIMIT=900010`은 선정산 완료액 1,000,000원, 기존 자금 사용액 900,000원이므로 남은 한도는 100,000원입니다.
동시에 100,000원 사용 내역을 여러 번 등록하면 1건만 성공하고 나머지는 `FU004`로 거절되어야 합니다.

실행 전 seed 데이터를 다시 넣어 테스트 상태를 초기화합니다.

```bash
mysql -u root seedlink < performance/seed/performance-seed.sql
```

```bash
BASE_URL=http://localhost:8080 \
USER_EMAIL=perf-proposer@seedlink.test \
USER_PASSWORD=password \
IDEA_ID=900010 \
AMOUNT=100000 \
VUS=50 \
ITERATIONS=50 \
MAX_DURATION=3m \
REQUEST_TIMEOUT=120s \
DEBUG_UNEXPECTED=true \
k6 run performance/k6/consistency/seedlink-consistency-fund-usage.js
```

정상 기준은 성공이 최대 1건이고, 나머지는 `FU004` 한도 초과 거절이어야 합니다.
500 오류와 예상하지 못한 응답은 없어야 합니다.

테스트 후 DB 정합성을 확인합니다.

```sql
SELECT idea_id, COUNT(*) AS count, SUM(amount) AS total_amount
FROM fund_usages
WHERE idea_id = 900010
GROUP BY idea_id;

SELECT idea_id, type, direction, amount, affects_balance, idempotency_key
FROM vbank_ledgers
WHERE idea_id = 900010
ORDER BY id DESC;
```

## 정합성 테스트: 보증금 환급/몰수 동시 처리

같은 보증금에 대해 관리자 환급 판정과 몰수 판정이 동시에 들어왔을 때 상태와 장부가 꼬이지 않는지 확인합니다.

seed 기준 `IDEA_ID_DEPOSIT_DECISION=900007`은 보증금 `HELD` 상태입니다.
테스트는 짝수 요청은 환급, 홀수 요청은 몰수 API를 호출합니다.

실행 전 seed 데이터를 다시 넣어 테스트 상태를 초기화합니다.

```bash
mysql -u root seedlink < performance/seed/performance-seed.sql
```

```bash
BASE_URL=http://localhost:8080 \
ADMIN_EMAIL=perf-admin@seedlink.test \
ADMIN_PASSWORD=password \
IDEA_ID=900007 \
VUS=50 \
ITERATIONS=50 \
MAX_DURATION=3m \
REQUEST_TIMEOUT=120s \
DEBUG_UNEXPECTED=true \
k6 run performance/k6/consistency/seedlink-consistency-deposit-decision.js
```

정상 기준은 둘 중 하나의 최종 정책만 반영되는 것입니다.
테스트 응답만으로 최종 정책 충돌 여부를 완전히 판단하기 어렵기 때문에 DB 확인이 필수입니다.

```sql
SELECT id, idea_id, amount, status, released_at
FROM deposits
WHERE idea_id = 900007;

SELECT id, idea_id, type, total_amount, payout_amount, status, idempotency_key, memo
FROM settlements
WHERE idea_id = 900007
ORDER BY id;

SELECT id, idea_id, type, direction, amount, affects_balance, idempotency_key
FROM vbank_ledgers
WHERE idea_id = 900007
ORDER BY id;
```

통과 기준입니다.

- `deposits.status`는 `REFUNDED` 또는 `FORFEITED` 중 하나여야 합니다.
- `REFUNDED`와 `FORFEITED`가 함께 성공한 것처럼 장부가 남으면 실패입니다.
- 같은 idempotency key의 정산/가상계좌 장부가 중복되면 실패입니다.
- 500 오류와 예상하지 못한 응답은 없어야 합니다.

## 정합성 테스트: 최종 정산 중복 생성

3단계 완료 보고서 승인 요청이 동시에 들어와도 완료 보고서 승인, 마일스톤 완료, 최종 정산, 보증금 환급 장부가 한 번만 생성되는지 확인합니다.

seed 기준 `IDEA_ID_FINAL_SETTLEMENT=900008`, `MILESTONE_ID_FINAL=910021`, `REPORT_ID_FINAL=950010`을 사용합니다.

실행 전 seed 데이터를 다시 넣어 테스트 상태를 초기화합니다.

```bash
mysql -u root seedlink < performance/seed/performance-seed.sql
```

```bash
BASE_URL=http://localhost:8080 \
ADMIN_EMAIL=perf-admin@seedlink.test \
ADMIN_PASSWORD=password \
MILESTONE_ID=910021 \
VUS=50 \
ITERATIONS=50 \
MAX_DURATION=3m \
REQUEST_TIMEOUT=120s \
DEBUG_UNEXPECTED=true \
k6 run performance/k6/consistency/seedlink-consistency-final-settlement.js
```

정상 기준은 성공이 최대 1건이고, 나머지는 이미 처리된 보고서/마일스톤/정산으로 인한 정상 거절이어야 합니다.
500 오류와 예상하지 못한 응답은 없어야 합니다.

테스트 후 DB 정합성을 확인합니다.

```sql
SELECT id, status
FROM idea
WHERE id = 900008;

SELECT id, idea_id, step, status
FROM milestones
WHERE idea_id = 900008
ORDER BY step;

SELECT id, milestone_id, type, status
FROM completion_reports
WHERE milestone_id = 910021
ORDER BY id;

SELECT id, idea_id, type, total_amount, platform_fee, payout_amount, status, idempotency_key, memo
FROM settlements
WHERE idea_id = 900008
ORDER BY id;

SELECT id, idea_id, type, direction, amount, affects_balance, idempotency_key
FROM vbank_ledgers
WHERE idea_id = 900008
ORDER BY id;
```

통과 기준입니다.

- 완료 보고서 `950010`은 `APPROVED`여야 합니다.
- 마일스톤 `910021`은 `COMPLETED`여야 합니다.
- 아이디어 `900008`은 `COMPLETED`여야 합니다.
- 최종 정산 장부는 1건만 생성되어야 합니다.
- 보증금 환급 장부는 1건만 생성되어야 합니다.
- 같은 의미의 settlement/vbank ledger 중복이 없어야 합니다.

## 정합성 테스트: 보고서 승인/반려 동시 처리

같은 완료 보고서에 대해 관리자 승인과 반려가 동시에 들어왔을 때 보고서가 `APPROVED`와 `REJECTED`로 동시에 처리되지 않는지 확인합니다.

seed 기준 `IDEA_ID_REPORT_DECISION=900009`, `MILESTONE_ID=910022`, 완료 보고서 `950011`을 사용합니다.

실행 전 seed 데이터를 다시 넣어 테스트 상태를 초기화합니다.

```bash
mysql -u root seedlink < performance/seed/performance-seed.sql
```

```bash
BASE_URL=http://localhost:8080 \
ADMIN_EMAIL=perf-admin@seedlink.test \
ADMIN_PASSWORD=password \
MILESTONE_ID=910022 \
VUS=50 \
ITERATIONS=50 \
MAX_DURATION=3m \
REQUEST_TIMEOUT=120s \
DEBUG_UNEXPECTED=true \
k6 run performance/k6/consistency/seedlink-consistency-report-decision.js
```

정상 기준은 승인 또는 반려 중 하나만 성공하고, 나머지는 이미 처리된 보고서/마일스톤 상태로 인한 `M005` 정상 거절이어야 합니다.
500 오류와 예상하지 못한 응답은 없어야 합니다.

테스트 후 DB 정합성을 확인합니다.

```sql
SELECT id, idea_id, step, status
FROM milestones
WHERE idea_id = 900009
ORDER BY step;

SELECT id, milestone_id, type, status, reject_reason
FROM completion_reports
WHERE milestone_id = 910022
ORDER BY id;

SELECT id, idea_id, type, total_amount, platform_fee, payout_amount, status, idempotency_key, memo
FROM settlements
WHERE idea_id = 900009
ORDER BY id;
```

통과 기준입니다.

- 완료 보고서 `950011`은 `APPROVED` 또는 `REJECTED` 중 하나여야 합니다.
- 승인과 반려가 모두 성공한 흔적이 없어야 합니다.
- 승인 성공이면 다음 마일스톤 전환 또는 최종 처리 흐름이 1회만 발생해야 합니다.
- 반려 성공이면 반려 사유가 1회만 저장되어야 합니다.
- 500 오류와 예상하지 못한 응답은 없어야 합니다.

## 정합성 + 성능 테스트: 자금 사용 내역 150 VU

자금 사용 내역 한도 동시 등록 시나리오에 성능 threshold를 함께 적용한 스크립트입니다.
정합성 기준과 함께 `p95 < 1s`, `p99 < 3s`를 확인합니다.

실행 전 seed 데이터를 다시 넣어 테스트 상태를 초기화합니다.

```bash
mysql -u root seedlink < performance/seed/performance-seed.sql
```

```bash
BASE_URL=http://localhost:8080 \
USER_EMAIL=perf-proposer@seedlink.test \
USER_PASSWORD=password \
IDEA_ID=900010 \
AMOUNT=100000 \
VUS=150 \
ITERATIONS=150 \
MAX_DURATION=5m \
REQUEST_TIMEOUT=180s \
DEBUG_UNEXPECTED=true \
k6 run performance/k6/consistency-perf/seedlink-consistency-fund-usage-perf-150.js
```

통과 기준입니다.

- `fund_usage_accepted <= 1`
- `fund_usage_unexpected = 0`
- `http_req_failed = 0`
- `http_req_duration p95 < 1000ms`
- `http_req_duration p99 < 3000ms`
