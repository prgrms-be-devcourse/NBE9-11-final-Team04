-- Pre-settlement consistency checks after running:
-- performance/k6/seedlink-consistency-pre-settlement.js
--
-- Expected:
--   Every query below should return zero rows.

-- 1. 선정산 한도 초과 여부
--    non-FAILED 선정산 합계는 보증금의 2배를 초과하면 안 됩니다.
SELECT
    ps.idea_id,
    SUM(ps.amount) AS non_failed_pre_settlement_amount,
    d.amount * 2 AS pre_settlement_limit
FROM pre_settlements ps
JOIN deposits d ON d.idea_id = ps.idea_id
WHERE ps.idea_id = 900002
  AND ps.status <> 'FAILED'
GROUP BY ps.idea_id, d.amount
HAVING SUM(ps.amount) > d.amount * 2;

-- 2. 같은 선정산 ID에 대한 지급 장부 중복 여부
--    완료 콜백 시 같은 PreSettlement 기준 가상계좌 출금 장부가 중복되면 안 됩니다.
SELECT
    reference_id,
    COUNT(*) AS ledger_count
FROM vbank_ledgers
WHERE idea_id = 900002
  AND type = 'PRE_SETTLEMENT_PAID'
  AND reference_type = 'PreSettlement'
GROUP BY reference_id
HAVING COUNT(*) > 1;

-- 3. 가상계좌 장부 멱등성 키 중복 여부
--    idempotency_key는 장부 중복 생성을 막는 기준입니다.
SELECT
    idempotency_key,
    COUNT(*) AS ledger_count
FROM vbank_ledgers
WHERE idea_id = 900002
GROUP BY idempotency_key
HAVING COUNT(*) > 1;

-- 4. 선정산 상태별 분포 확인용
--    이 쿼리는 실패 검증이 아니라 결과 해석용입니다.
SELECT
    status,
    COUNT(*) AS count,
    SUM(amount) AS total_amount
FROM pre_settlements
WHERE idea_id = 900002
GROUP BY status
ORDER BY status;
