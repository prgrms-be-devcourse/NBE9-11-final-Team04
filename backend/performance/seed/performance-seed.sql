-- SeedLink performance-test seed data
-- ------------------------------------------------------------
-- Purpose:
--   Local performance tests for funding/milestone/settlement/payment read flows.
--
-- Scope:
--   This script uses fixed ids in the 900000+ range so the team can share
--   the same k6/Postman variables.
--
-- Warning:
--   Run only on a local/test database. Do not run on production.
--
-- Common login password:
--   password
--
-- Common variables:
--   ADMIN_EMAIL=perf-admin@seedlink.test
--   PROPOSER_EMAIL=perf-proposer@seedlink.test
--   SPONSOR_EMAIL=perf-sponsor01@seedlink.test
--   IDEA_ID=900002
--   IDEA_ID_CLOSING=900001
--   IDEA_ID_SUCCESS_CLOSED=900003
--   IDEA_ID_FAILED_CLOSED=900004
--   IDEA_ID_MILESTONE=900005
--   IDEA_ID_REFUND_DUPLICATE=900006
--   IDEA_ID_DEPOSIT_DECISION=900007
--   IDEA_ID_FINAL_SETTLEMENT=900008
--   IDEA_ID_REPORT_DECISION=900009
--   IDEA_ID_FUND_USAGE_LIMIT=900010
--   IDEA_ID_PAYMENT_DEPOSIT=900011
--   MILESTONE_ID=910010
--   REPORT_ID=950001
--   REFUND_ID_PENDING=970010
--   PAYMENT_ID_CARD_PENDING=930022
--   PAYMENT_ID_VBANK_WAITING=930023
--   PAYMENT_ID_REFUNDABLE=930024
--   PAYMENT_ID_DEPOSIT_VBANK=930025

SET FOREIGN_KEY_CHECKS = 0;

START TRANSACTION;

-- Clean previous performance seed rows.
DELETE FROM vbank_ledgers WHERE idea_id BETWEEN 900000 AND 900999;
DELETE FROM payment_webhook_logs WHERE id BETWEEN 990000 AND 990999 OR order_id LIKE 'perf-order-%' OR order_id LIKE 'perf-deposit-%';
DELETE FROM vbank_deposits WHERE id BETWEEN 936000 AND 936999 OR payment_id BETWEEN 930000 AND 930999;
DELETE FROM refunds WHERE id BETWEEN 970000 AND 970999 OR payment_id BETWEEN 930000 AND 930999;
DELETE FROM settlements WHERE id BETWEEN 960000 AND 960999 OR idea_id BETWEEN 900000 AND 900999;
DELETE FROM pre_settlements WHERE id BETWEEN 955000 AND 955999 OR idea_id BETWEEN 900000 AND 900999;
DELETE FROM fund_usages WHERE id BETWEEN 952000 AND 952999 OR idea_id BETWEEN 900000 AND 900999;
DELETE FROM completion_reports WHERE id BETWEEN 950000 AND 950999 OR milestone_id BETWEEN 910000 AND 910999;
DELETE FROM payments WHERE id BETWEEN 930000 AND 930999 OR funding_id BETWEEN 920000 AND 920999 OR idea_id BETWEEN 900000 AND 900999;
DELETE FROM fundings WHERE id BETWEEN 920000 AND 920999 OR idea_id BETWEEN 900000 AND 900999;
DELETE FROM deposits WHERE id BETWEEN 940000 AND 940999 OR idea_id BETWEEN 900000 AND 900999;
DELETE FROM idea_vbank_pool WHERE id BETWEEN 935000 AND 935999 OR idea_id BETWEEN 900000 AND 900999;
DELETE FROM virtual_account WHERE id BETWEEN 934000 AND 934999;
DELETE FROM milestones WHERE id BETWEEN 910000 AND 910999 OR idea_id BETWEEN 900000 AND 900999;
DELETE FROM idea WHERE id BETWEEN 900000 AND 900999;
DELETE FROM users WHERE id BETWEEN 900000 AND 900999;

-- Users.
-- BCrypt hash is for the plain password: password
INSERT INTO users (id, email, password, name, nickname, age, role, status, created_at, updated_at)
VALUES
    (900001, 'perf-admin@seedlink.test', '$2a$10$cwc5g9KixYwQ9pFVUzP5HeFtsqr7.A700KJWc4WRFKcrc/DukIgGe', 'Perf Admin', 'perf-admin', 35, 'ADMIN', 'ACTIVE', NOW(), NOW()),
    (900002, 'perf-proposer@seedlink.test', '$2a$10$cwc5g9KixYwQ9pFVUzP5HeFtsqr7.A700KJWc4WRFKcrc/DukIgGe', 'Perf Proposer', 'perf-proposer', 32, 'USER', 'ACTIVE', NOW(), NOW()),
    (900101, 'perf-sponsor01@seedlink.test', '$2a$10$cwc5g9KixYwQ9pFVUzP5HeFtsqr7.A700KJWc4WRFKcrc/DukIgGe', 'Perf Sponsor 01', 'perf-sponsor01', 28, 'USER', 'ACTIVE', NOW(), NOW()),
    (900102, 'perf-sponsor02@seedlink.test', '$2a$10$cwc5g9KixYwQ9pFVUzP5HeFtsqr7.A700KJWc4WRFKcrc/DukIgGe', 'Perf Sponsor 02', 'perf-sponsor02', 29, 'USER', 'ACTIVE', NOW(), NOW()),
    (900103, 'perf-sponsor03@seedlink.test', '$2a$10$cwc5g9KixYwQ9pFVUzP5HeFtsqr7.A700KJWc4WRFKcrc/DukIgGe', 'Perf Sponsor 03', 'perf-sponsor03', 30, 'USER', 'ACTIVE', NOW(), NOW()),
    (900104, 'perf-sponsor04@seedlink.test', '$2a$10$cwc5g9KixYwQ9pFVUzP5HeFtsqr7.A700KJWc4WRFKcrc/DukIgGe', 'Perf Sponsor 04', 'perf-sponsor04', 31, 'USER', 'ACTIVE', NOW(), NOW()),
    (900105, 'perf-sponsor05@seedlink.test', '$2a$10$cwc5g9KixYwQ9pFVUzP5HeFtsqr7.A700KJWc4WRFKcrc/DukIgGe', 'Perf Sponsor 05', 'perf-sponsor05', 32, 'USER', 'ACTIVE', NOW(), NOW());

-- Ideas.
INSERT INTO idea (
    id, user_id, title, category, one_line_intro, problem_definition, solution, goal,
    target_customer, competitor, team_intro, goal_amount, deposit_amount, current_amount,
    sponsor_count, funding_start_at, funding_end_at, reward_type, image_url, image_urls,
    status, trust_score, reject_reason, badge, deleted_at, previous_status, rejected_match_count, reject_count, admin_rejected_count,
    created_at, updated_at
)
VALUES
    (900001, 900002, 'Perf 마감 직전 펀딩', 'TECH', '마감 직전 조회 부하용 프로젝트입니다.',
     '마감 전후 집중 유입을 재현합니다.', '실시간 달성률과 후원 흐름을 검증합니다.', '마감 직전 후원 집중 테스트',
     '후원자', '기존 크라우드펀딩', 'SeedLink Perf Team', 10000000, 3000000, 7000000,
     3, DATE_SUB(NOW(), INTERVAL 3 DAY), DATE_ADD(NOW(), INTERVAL 1 HOUR), 'REWARD_POINT',
     'https://cdn.seedlink.test/perf/closing.png', '["https://cdn.seedlink.test/perf/closing-1.png"]',
     'IN_PROGRESS', 90, NULL, 'VERIFIED', NULL, NULL, 0, 0, 0, NOW(), NOW()),

    (900002, 900002, 'Perf 진행 중 마일스톤', 'TECH', '조회 부하와 장부 조회 기준 프로젝트입니다.',
     '진행 중 프로젝트 조회 성능을 확인합니다.', '마일스톤/정산/장부 조회를 제공합니다.', '진행 중 프로젝트 조회 테스트',
     '후원자', '기존 크라우드펀딩', 'SeedLink Perf Team', 10000000, 3000000, 12000000,
     5, DATE_SUB(NOW(), INTERVAL 5 DAY), DATE_ADD(NOW(), INTERVAL 7 DAY), 'REWARD_POINT',
     'https://cdn.seedlink.test/perf/in-progress.png', '["https://cdn.seedlink.test/perf/in-progress-1.png"]',
     'IN_PROGRESS', 95, NULL, 'CERTIFIED', NULL, NULL, 0, 0, 0, NOW(), NOW()),

    (900003, 900002, 'Perf 마감 성공 대기', 'LIFE', '마감 후 성공 확정 스케줄러 테스트용입니다.',
     '목표 달성 후 마감된 프로젝트를 재현합니다.', '스케줄러가 1단계를 시작해야 합니다.', '마감 후 성공 확정 테스트',
     '후원자', '기존 크라우드펀딩', 'SeedLink Perf Team', 10000000, 3000000, 12000000,
     5, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 'PAYBACK',
     'https://cdn.seedlink.test/perf/success-closed.png', '[]',
     'OPEN', 88, NULL, 'VERIFIED', NULL, NULL, 0, 0, 0, NOW(), NOW()),

    (900004, 900002, 'Perf 마감 실패 대기', 'ENVIRONMENT', '마감 후 목표 미달성 환불 테스트용입니다.',
     '목표 미달성 프로젝트를 재현합니다.', '스케줄러가 환불 장부를 생성해야 합니다.', '마감 후 실패 환불 테스트',
     '후원자', '기존 크라우드펀딩', 'SeedLink Perf Team', 10000000, 3000000, 3000000,
     2, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 'FIRST_COME',
     'https://cdn.seedlink.test/perf/failed-closed.png', '[]',
     'IN_PROGRESS', 84, NULL, 'NO_HISTORY', NULL, NULL, 0, 0, 0, NOW(), NOW()),

    (900005, 900002, 'Perf 보고서 검토 집중', 'EDUCATION', '보고서 승인/반려 부하 테스트용입니다.',
     '마일스톤 보고서 검토 집중 상황을 재현합니다.', '완료/소명 보고서 상태 전이를 검증합니다.', '보고서 검토 집중 테스트',
     '후원자', '기존 크라우드펀딩', 'SeedLink Perf Team', 10000000, 3000000, 10000000,
     4, DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_ADD(NOW(), INTERVAL 10 DAY), 'REWARD_POINT',
     'https://cdn.seedlink.test/perf/report.png', '[]',
     'IN_PROGRESS', 91, NULL, 'VERIFIED', NULL, NULL, 0, 0, 0, NOW(), NOW()),

    (900006, 900002, 'Consistency 환불 중복 검증', 'TECH', '환불 중복 생성 정합성 테스트용입니다.',
     '동일 결제건 환불 중복 생성을 검증합니다.', 'force refund와 환불 콜백 중복 호출을 확인합니다.', '환불 중복 정합성 테스트',
     '후원자', '기존 크라우드펀딩', 'SeedLink Perf Team', 3000000, 900000, 3000000,
     3, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_ADD(NOW(), INTERVAL 5 DAY), 'REWARD_POINT',
     'https://cdn.seedlink.test/perf/refund-duplicate.png', '[]',
     'IN_PROGRESS', 90, NULL, 'VERIFIED', NULL, NULL, 0, 0, 0, NOW(), NOW()),

    (900007, 900002, 'Consistency 보증금 처리 검증', 'LIFE', '보증금 환급/몰수 동시 처리 테스트용입니다.',
     '보증금 상태가 환급과 몰수로 동시에 갈라지지 않는지 검증합니다.', 'Deposit 상태 전이를 확인합니다.', '보증금 처리 정합성 테스트',
     '후원자', '기존 크라우드펀딩', 'SeedLink Perf Team', 5000000, 1500000, 5000000,
     2, DATE_SUB(NOW(), INTERVAL 10 DAY), DATE_ADD(NOW(), INTERVAL 5 DAY), 'PAYBACK',
     'https://cdn.seedlink.test/perf/deposit-decision.png', '[]',
     'IN_PROGRESS', 88, NULL, 'VERIFIED', NULL, NULL, 0, 0, 0, NOW(), NOW()),

    (900008, 900002, 'Consistency 최종 정산 검증', 'ENVIRONMENT', '최종 정산 중복 생성 테스트용입니다.',
     '3단계 완료 승인과 최종 정산 생성 중복을 검증합니다.', '최종 정산 장부 멱등성을 확인합니다.', '최종 정산 정합성 테스트',
     '후원자', '기존 크라우드펀딩', 'SeedLink Perf Team', 10000000, 3000000, 10000000,
     2, DATE_SUB(NOW(), INTERVAL 30 DAY), DATE_SUB(NOW(), INTERVAL 1 DAY), 'FIRST_COME',
     'https://cdn.seedlink.test/perf/final-settlement.png', '[]',
     'IN_PROGRESS', 92, NULL, 'CERTIFIED', NULL, NULL, 0, 0, 0, NOW(), NOW()),

    (900009, 900002, 'Consistency 보고서 승인 반려 검증', 'EDUCATION', '보고서 승인/반려 동시 처리 테스트용입니다.',
     '같은 보고서 승인과 반려가 동시에 들어오는 상황을 검증합니다.', '보고서와 마일스톤 상태 전이를 확인합니다.', '보고서 결정 정합성 테스트',
     '후원자', '기존 크라우드펀딩', 'SeedLink Perf Team', 10000000, 3000000, 10000000,
     2, DATE_SUB(NOW(), INTERVAL 20 DAY), DATE_ADD(NOW(), INTERVAL 5 DAY), 'REWARD_POINT',
     'https://cdn.seedlink.test/perf/report-decision.png', '[]',
     'IN_PROGRESS', 89, NULL, 'VERIFIED', NULL, NULL, 0, 0, 0, NOW(), NOW()),

    (900010, 900002, 'Consistency 자금 사용 한도 검증', 'TECH', '자금 사용 내역 한도 테스트용입니다.',
     '동시 자금 사용 등록이 실제 지급액을 넘지 않는지 검증합니다.', 'FundUsage와 공개 장부 정합성을 확인합니다.', '자금 사용 정합성 테스트',
     '후원자', '기존 크라우드펀딩', 'SeedLink Perf Team', 3000000, 900000, 3000000,
     2, DATE_SUB(NOW(), INTERVAL 15 DAY), DATE_ADD(NOW(), INTERVAL 5 DAY), 'REWARD_POINT',
     'https://cdn.seedlink.test/perf/fund-usage-limit.png', '[]',
     'IN_PROGRESS', 87, NULL, 'VERIFIED', NULL, NULL, 0, 0, 0, NOW(), NOW()),

    (900011, 900002, 'Payment 보증금 결제 검증', 'TECH', '보증금 결제/가상계좌 웹훅 테스트용입니다.',
     '보증금 결제 생성과 입금 웹훅을 검증합니다.', '보증금 결제 성공 시 Deposit HELD와 장부 생성을 확인합니다.', '결제 도메인 보증금 테스트',
     '후원자', '기존 크라우드펀딩', 'SeedLink Perf Team', 3000000, 900000, 0,
     0, DATE_ADD(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 10 DAY), 'REWARD_POINT',
     'https://cdn.seedlink.test/perf/payment-deposit.png', '[]',
     'IN_PROGRESS', 87, NULL, 'VERIFIED', NULL, NULL, 0, 0, 0, NOW(), NOW());

-- Milestones.
INSERT INTO milestones (id, idea_id, step, goal, expected_result, expected_date, status, overdue_at, created_at, updated_at)
VALUES
    (910001, 900001, 1, '1단계 MVP 검증', 'MVP 데모 완료', DATE_ADD(CURDATE(), INTERVAL 7 DAY), 'PENDING', NULL, NOW(), NOW()),
    (910002, 900001, 2, '2단계 베타 테스트', '베타 사용자 50명', DATE_ADD(CURDATE(), INTERVAL 21 DAY), 'PENDING', NULL, NOW(), NOW()),
    (910003, 900001, 3, '3단계 정식 출시', '정식 출시 완료', DATE_ADD(CURDATE(), INTERVAL 35 DAY), 'PENDING', NULL, NOW(), NOW()),

    (910004, 900002, 1, '1단계 MVP 검증', 'MVP 데모 완료', DATE_SUB(CURDATE(), INTERVAL 1 DAY), 'IN_PROGRESS', NULL, NOW(), NOW()),
    (910005, 900002, 2, '2단계 베타 테스트', '베타 사용자 50명', DATE_ADD(CURDATE(), INTERVAL 14 DAY), 'PENDING', NULL, NOW(), NOW()),
    (910006, 900002, 3, '3단계 정식 출시', '정식 출시 완료', DATE_ADD(CURDATE(), INTERVAL 28 DAY), 'PENDING', NULL, NOW(), NOW()),

    (910007, 900003, 1, '1단계 성공 대기', '성공 확정 후 시작', DATE_ADD(CURDATE(), INTERVAL 7 DAY), 'PENDING', NULL, NOW(), NOW()),
    (910008, 900003, 2, '2단계 성공 대기', '베타 확장', DATE_ADD(CURDATE(), INTERVAL 21 DAY), 'PENDING', NULL, NOW(), NOW()),
    (910009, 900003, 3, '3단계 성공 대기', '정식 출시', DATE_ADD(CURDATE(), INTERVAL 35 DAY), 'PENDING', NULL, NOW(), NOW()),

    (910010, 900005, 1, '1단계 보고서 검토', '완료 보고서 검토', DATE_SUB(CURDATE(), INTERVAL 2 DAY), 'IN_PROGRESS', NULL, NOW(), NOW()),
    (910011, 900005, 2, '2단계 보고서 검토', '추가 검토', DATE_ADD(CURDATE(), INTERVAL 14 DAY), 'PENDING', NULL, NOW(), NOW()),
    (910012, 900005, 3, '3단계 보고서 검토', '최종 검토', DATE_ADD(CURDATE(), INTERVAL 28 DAY), 'PENDING', NULL, NOW(), NOW()),

    (910013, 900006, 1, '1단계 환불 중복 검증', '환불 대상 결제 검증', DATE_ADD(CURDATE(), INTERVAL 7 DAY), 'IN_PROGRESS', NULL, NOW(), NOW()),
    (910014, 900006, 2, '2단계 환불 중복 검증', '추가 결제 검증', DATE_ADD(CURDATE(), INTERVAL 21 DAY), 'PENDING', NULL, NOW(), NOW()),
    (910015, 900006, 3, '3단계 환불 중복 검증', '최종 검증', DATE_ADD(CURDATE(), INTERVAL 35 DAY), 'PENDING', NULL, NOW(), NOW()),

    (910016, 900007, 1, '1단계 보증금 처리 검증', '보증금 상태 검증', DATE_ADD(CURDATE(), INTERVAL 7 DAY), 'IN_PROGRESS', NULL, NOW(), NOW()),
    (910017, 900007, 2, '2단계 보증금 처리 검증', '보증금 후속 검증', DATE_ADD(CURDATE(), INTERVAL 21 DAY), 'PENDING', NULL, NOW(), NOW()),
    (910018, 900007, 3, '3단계 보증금 처리 검증', '최종 검증', DATE_ADD(CURDATE(), INTERVAL 35 DAY), 'PENDING', NULL, NOW(), NOW()),

    (910019, 900008, 1, '1단계 완료', '완료됨', DATE_SUB(CURDATE(), INTERVAL 20 DAY), 'COMPLETED', NULL, NOW(), NOW()),
    (910020, 900008, 2, '2단계 완료', '완료됨', DATE_SUB(CURDATE(), INTERVAL 10 DAY), 'COMPLETED', NULL, NOW(), NOW()),
    (910021, 900008, 3, '3단계 최종 정산 검증', '최종 완료 보고서 검토', DATE_SUB(CURDATE(), INTERVAL 1 DAY), 'IN_PROGRESS', NULL, NOW(), NOW()),

    (910022, 900009, 1, '1단계 보고서 결정 검증', '승인/반려 동시성 검증', DATE_SUB(CURDATE(), INTERVAL 1 DAY), 'IN_PROGRESS', NULL, NOW(), NOW()),
    (910023, 900009, 2, '2단계 보고서 결정 검증', '후속 검증', DATE_ADD(CURDATE(), INTERVAL 14 DAY), 'PENDING', NULL, NOW(), NOW()),
    (910024, 900009, 3, '3단계 보고서 결정 검증', '최종 검증', DATE_ADD(CURDATE(), INTERVAL 28 DAY), 'PENDING', NULL, NOW(), NOW()),

    (910025, 900010, 1, '1단계 자금 사용 검증', '사용 내역 한도 검증', DATE_SUB(CURDATE(), INTERVAL 1 DAY), 'IN_PROGRESS', NULL, NOW(), NOW()),
    (910026, 900010, 2, '2단계 자금 사용 검증', '후속 검증', DATE_ADD(CURDATE(), INTERVAL 14 DAY), 'PENDING', NULL, NOW(), NOW()),
    (910027, 900010, 3, '3단계 자금 사용 검증', '최종 검증', DATE_ADD(CURDATE(), INTERVAL 28 DAY), 'PENDING', NULL, NOW(), NOW());

-- Deposit state for funding/settlement tests.
INSERT INTO deposits (id, idea_id, user_id, amount, status, paid_at, released_at, payment_id, created_at, updated_at)
VALUES
    (940001, 900001, 900002, 3000000, 'HELD', DATE_SUB(NOW(), INTERVAL 4 DAY), NULL, NULL, NOW(), NOW()),
    (940002, 900002, 900002, 3000000, 'HELD', DATE_SUB(NOW(), INTERVAL 6 DAY), NULL, NULL, NOW(), NOW()),
    (940003, 900003, 900002, 3000000, 'HELD', DATE_SUB(NOW(), INTERVAL 11 DAY), NULL, NULL, NOW(), NOW()),
    (940004, 900004, 900002, 3000000, 'HELD', DATE_SUB(NOW(), INTERVAL 11 DAY), NULL, NULL, NOW(), NOW()),
    (940005, 900005, 900002, 3000000, 'HELD', DATE_SUB(NOW(), INTERVAL 21 DAY), NULL, NULL, NOW(), NOW()),
    (940006, 900006, 900002, 900000, 'HELD', DATE_SUB(NOW(), INTERVAL 11 DAY), NULL, NULL, NOW(), NOW()),
    (940007, 900007, 900002, 1500000, 'HELD', DATE_SUB(NOW(), INTERVAL 11 DAY), NULL, NULL, NOW(), NOW()),
    (940008, 900008, 900002, 3000000, 'HELD', DATE_SUB(NOW(), INTERVAL 31 DAY), NULL, NULL, NOW(), NOW()),
    (940009, 900009, 900002, 3000000, 'HELD', DATE_SUB(NOW(), INTERVAL 21 DAY), NULL, NULL, NOW(), NOW()),
    (940010, 900010, 900002, 900000, 'HELD', DATE_SUB(NOW(), INTERVAL 16 DAY), NULL, NULL, NOW(), NOW());

-- Virtual account pools for vbank lookup tests.
INSERT INTO virtual_account (id, order_id, bank_code, account_number, due_date, amount, created_at, updated_at)
VALUES
    (934001, 'perf-pool-order-900001', '088', '900001000001', DATE_ADD(NOW(), INTERVAL 30 DAY), 0, NOW(), NOW()),
    (934002, 'perf-pool-order-900002', '088', '900002000001', DATE_ADD(NOW(), INTERVAL 30 DAY), 0, NOW(), NOW()),
    (934003, 'perf-pool-order-900003', '088', '900003000001', DATE_ADD(NOW(), INTERVAL 30 DAY), 0, NOW(), NOW()),
    (934004, 'perf-pool-order-900004', '088', '900004000001', DATE_ADD(NOW(), INTERVAL 30 DAY), 0, NOW(), NOW()),
    (934005, 'perf-pool-order-900005', '088', '900005000001', DATE_ADD(NOW(), INTERVAL 30 DAY), 0, NOW(), NOW()),
    (934006, 'perf-pool-order-900006', '088', '900006000001', DATE_ADD(NOW(), INTERVAL 30 DAY), 0, NOW(), NOW()),
    (934007, 'perf-pool-order-900007', '088', '900007000001', DATE_ADD(NOW(), INTERVAL 30 DAY), 0, NOW(), NOW()),
    (934008, 'perf-pool-order-900008', '088', '900008000001', DATE_ADD(NOW(), INTERVAL 30 DAY), 0, NOW(), NOW()),
    (934009, 'perf-pool-order-900009', '088', '900009000001', DATE_ADD(NOW(), INTERVAL 30 DAY), 0, NOW(), NOW()),
    (934010, 'perf-pool-order-900010', '088', '900010000001', DATE_ADD(NOW(), INTERVAL 30 DAY), 0, NOW(), NOW());

-- Payment-specific virtual accounts for webhook/virtual-account scenarios.
INSERT INTO virtual_account (id, order_id, bank_code, account_number, due_date, amount, created_at, updated_at)
VALUES
    (934011, 'perf-order-930023', '088', '930023000001', DATE_ADD(NOW(), INTERVAL 3 DAY), 700000, NOW(), NOW()),
    (934012, 'perf-deposit-930025', '088', '930025000001', DATE_ADD(NOW(), INTERVAL 3 DAY), 900000, NOW(), NOW());

INSERT INTO idea_vbank_pool (id, idea_id, pool_order_id, virtual_account_id, status, created_at, updated_at)
VALUES
    (935001, 900001, 'perf-pool-order-900001', 934001, 'ACTIVE', NOW(), NOW()),
    (935002, 900002, 'perf-pool-order-900002', 934002, 'ACTIVE', NOW(), NOW()),
    (935003, 900003, 'perf-pool-order-900003', 934003, 'ACTIVE', NOW(), NOW()),
    (935004, 900004, 'perf-pool-order-900004', 934004, 'ACTIVE', NOW(), NOW()),
    (935005, 900005, 'perf-pool-order-900005', 934005, 'ACTIVE', NOW(), NOW()),
    (935006, 900006, 'perf-pool-order-900006', 934006, 'ACTIVE', NOW(), NOW()),
    (935007, 900007, 'perf-pool-order-900007', 934007, 'ACTIVE', NOW(), NOW()),
    (935008, 900008, 'perf-pool-order-900008', 934008, 'ACTIVE', NOW(), NOW()),
    (935009, 900009, 'perf-pool-order-900009', 934009, 'ACTIVE', NOW(), NOW()),
    (935010, 900010, 'perf-pool-order-900010', 934010, 'ACTIVE', NOW(), NOW());

-- Funding and payment rows.
INSERT INTO fundings (id, idea_id, sponsor_id, milestone_step, amount, reward_type, status, refunded_at, amount_applied_to_idea, created_at, updated_at)
VALUES
    (920001, 900002, 900101, 1, 3000000, 'REWARD_POINT', 'PAID', NULL, TRUE, NOW(), NOW()),
    (920002, 900002, 900102, 1, 3000000, 'REWARD_POINT', 'PAID', NULL, TRUE, NOW(), NOW()),
    (920003, 900002, 900103, 1, 3000000, 'REWARD_POINT', 'PAID', NULL, TRUE, NOW(), NOW()),
    (920004, 900002, 900104, 1, 3000000, 'REWARD_POINT', 'PAID', NULL, TRUE, NOW(), NOW()),
    (920005, 900003, 900101, 0, 6000000, 'PAYBACK', 'PAID', NULL, TRUE, NOW(), NOW()),
    (920006, 900003, 900102, 0, 6000000, 'PAYBACK', 'PAID', NULL, TRUE, NOW(), NOW()),
    (920007, 900004, 900101, 0, 1500000, 'FIRST_COME', 'PAID', NULL, TRUE, NOW(), NOW()),
    (920008, 900004, 900102, 0, 1500000, 'FIRST_COME', 'PAID', NULL, TRUE, NOW(), NOW()),
    (920009, 900005, 900103, 1, 5000000, 'REWARD_POINT', 'PAID', NULL, TRUE, NOW(), NOW()),
    (920010, 900005, 900104, 1, 5000000, 'REWARD_POINT', 'PAID', NULL, TRUE, NOW(), NOW()),
    (920011, 900006, 900101, 1, 1000000, 'REWARD_POINT', 'PAID', NULL, TRUE, NOW(), NOW()),
    (920012, 900006, 900102, 1, 1000000, 'REWARD_POINT', 'PAID', NULL, TRUE, NOW(), NOW()),
    (920013, 900006, 900103, 1, 1000000, 'REWARD_POINT', 'PAID', NULL, TRUE, NOW(), NOW()),
    (920014, 900007, 900101, 1, 2500000, 'PAYBACK', 'PAID', NULL, TRUE, NOW(), NOW()),
    (920015, 900007, 900102, 1, 2500000, 'PAYBACK', 'PAID', NULL, TRUE, NOW(), NOW()),
    (920016, 900008, 900101, 3, 5000000, 'FIRST_COME', 'PAID', NULL, TRUE, NOW(), NOW()),
    (920017, 900008, 900102, 3, 5000000, 'FIRST_COME', 'PAID', NULL, TRUE, NOW(), NOW()),
    (920018, 900009, 900103, 1, 5000000, 'REWARD_POINT', 'PAID', NULL, TRUE, NOW(), NOW()),
    (920019, 900009, 900104, 1, 5000000, 'REWARD_POINT', 'PAID', NULL, TRUE, NOW(), NOW()),
    (920020, 900010, 900101, 1, 1500000, 'REWARD_POINT', 'PAID', NULL, TRUE, NOW(), NOW()),
    (920021, 900010, 900102, 1, 1500000, 'REWARD_POINT', 'PAID', NULL, TRUE, NOW(), NOW());

-- Payment-domain fundings.
-- 920022: 카드 confirm 대상, 920023: 가상계좌 웹훅 대상, 920024: 후원자 직접 환불 대상.
INSERT INTO fundings (id, idea_id, sponsor_id, milestone_step, amount, reward_type, status, refunded_at, amount_applied_to_idea, created_at, updated_at)
VALUES
    (920022, 900002, 900105, 1, 600000, 'REWARD_POINT', 'PENDING_PAYMENT', NULL, FALSE, NOW(), NOW()),
    (920023, 900002, 900104, 1, 700000, 'REWARD_POINT', 'PENDING_PAYMENT', NULL, FALSE, NOW(), NOW()),
    (920024, 900002, 900103, 1, 800000, 'REWARD_POINT', 'PAID', NULL, TRUE, NOW(), NOW());

INSERT INTO payments (id, funding_id, payment_key, order_id, method, amount, purpose, idea_id, status, approved_at, refunded_at, toss_webhook_secret, created_at, updated_at)
VALUES
    (930001, 920001, 'perf-payment-key-930001', 'perf-order-930001', 'CARD', 3000000, 'SPONSORSHIP', NULL, 'SUCCESS', NOW(), NULL, NULL, NOW(), NOW()),
    (930002, 920002, 'perf-payment-key-930002', 'perf-order-930002', 'CARD', 3000000, 'SPONSORSHIP', NULL, 'SUCCESS', NOW(), NULL, NULL, NOW(), NOW()),
    (930003, 920003, 'perf-payment-key-930003', 'perf-order-930003', 'VIRTUAL_ACCOUNT', 3000000, 'SPONSORSHIP', NULL, 'SUCCESS', NOW(), NULL, 'perf-secret-930003', NOW(), NOW()),
    (930004, 920004, 'perf-payment-key-930004', 'perf-order-930004', 'CARD', 3000000, 'SPONSORSHIP', NULL, 'SUCCESS', NOW(), NULL, NULL, NOW(), NOW()),
    (930005, 920005, 'perf-payment-key-930005', 'perf-order-930005', 'CARD', 6000000, 'SPONSORSHIP', NULL, 'SUCCESS', NOW(), NULL, NULL, NOW(), NOW()),
    (930006, 920006, 'perf-payment-key-930006', 'perf-order-930006', 'CARD', 6000000, 'SPONSORSHIP', NULL, 'SUCCESS', NOW(), NULL, NULL, NOW(), NOW()),
    (930007, 920007, 'perf-payment-key-930007', 'perf-order-930007', 'CARD', 1500000, 'SPONSORSHIP', NULL, 'SUCCESS', NOW(), NULL, NULL, NOW(), NOW()),
    (930008, 920008, 'perf-payment-key-930008', 'perf-order-930008', 'CARD', 1500000, 'SPONSORSHIP', NULL, 'SUCCESS', NOW(), NULL, NULL, NOW(), NOW()),
    (930009, 920009, 'perf-payment-key-930009', 'perf-order-930009', 'CARD', 5000000, 'SPONSORSHIP', NULL, 'SUCCESS', NOW(), NULL, NULL, NOW(), NOW()),
    (930010, 920010, 'perf-payment-key-930010', 'perf-order-930010', 'CARD', 5000000, 'SPONSORSHIP', NULL, 'SUCCESS', NOW(), NULL, NULL, NOW(), NOW()),
    (930011, 920011, 'perf-payment-key-930011', 'perf-order-930011', 'CARD', 1000000, 'SPONSORSHIP', NULL, 'SUCCESS', NOW(), NULL, NULL, NOW(), NOW()),
    (930012, 920012, 'perf-payment-key-930012', 'perf-order-930012', 'CARD', 1000000, 'SPONSORSHIP', NULL, 'SUCCESS', NOW(), NULL, NULL, NOW(), NOW()),
    (930013, 920013, 'perf-payment-key-930013', 'perf-order-930013', 'CARD', 1000000, 'SPONSORSHIP', NULL, 'SUCCESS', NOW(), NULL, NULL, NOW(), NOW()),
    (930014, 920014, 'perf-payment-key-930014', 'perf-order-930014', 'CARD', 2500000, 'SPONSORSHIP', NULL, 'SUCCESS', NOW(), NULL, NULL, NOW(), NOW()),
    (930015, 920015, 'perf-payment-key-930015', 'perf-order-930015', 'CARD', 2500000, 'SPONSORSHIP', NULL, 'SUCCESS', NOW(), NULL, NULL, NOW(), NOW()),
    (930016, 920016, 'perf-payment-key-930016', 'perf-order-930016', 'CARD', 5000000, 'SPONSORSHIP', NULL, 'SUCCESS', NOW(), NULL, NULL, NOW(), NOW()),
    (930017, 920017, 'perf-payment-key-930017', 'perf-order-930017', 'CARD', 5000000, 'SPONSORSHIP', NULL, 'SUCCESS', NOW(), NULL, NULL, NOW(), NOW()),
    (930018, 920018, 'perf-payment-key-930018', 'perf-order-930018', 'CARD', 5000000, 'SPONSORSHIP', NULL, 'SUCCESS', NOW(), NULL, NULL, NOW(), NOW()),
    (930019, 920019, 'perf-payment-key-930019', 'perf-order-930019', 'CARD', 5000000, 'SPONSORSHIP', NULL, 'SUCCESS', NOW(), NULL, NULL, NOW(), NOW()),
    (930020, 920020, 'perf-payment-key-930020', 'perf-order-930020', 'CARD', 1500000, 'SPONSORSHIP', NULL, 'SUCCESS', NOW(), NULL, NULL, NOW(), NOW()),
    (930021, 920021, 'perf-payment-key-930021', 'perf-order-930021', 'CARD', 1500000, 'SPONSORSHIP', NULL, 'SUCCESS', NOW(), NULL, NULL, NOW(), NOW());

-- Payment-domain rows.
-- 930022: 카드 confirm 대상, 930023: 가상계좌 입금 웹훅 대상,
-- 930024: 후원자 직접 환불 대상, 930025: 보증금 가상계좌 입금 웹훅 대상.
INSERT INTO payments (id, funding_id, payment_key, order_id, method, amount, purpose, idea_id, status, approved_at, refunded_at, toss_webhook_secret, created_at, updated_at)
VALUES
    (930022, 920022, NULL, 'perf-order-930022', 'CARD', 600000, 'SPONSORSHIP', NULL, 'PENDING', NULL, NULL, NULL, NOW(), NOW()),
    (930023, 920023, 'perf-vbank-key-930023', 'perf-order-930023', 'VIRTUAL_ACCOUNT', 700000, 'SPONSORSHIP', NULL, 'PENDING', NULL, NULL, 'perf-toss-secret-930023', NOW(), NOW()),
    (930024, 920024, 'perf-payment-key-930024', 'perf-order-930024', 'CARD', 800000, 'SPONSORSHIP', NULL, 'SUCCESS', NOW(), NULL, NULL, NOW(), NOW()),
    (930025, NULL, 'perf-vbank-key-930025', 'perf-deposit-930025', 'VIRTUAL_ACCOUNT', 900000, 'DEPOSIT', 900011, 'PENDING', NULL, NULL, 'perf-toss-secret-930025', NOW(), NOW());

INSERT INTO vbank_deposits (id, payment_id, virtual_account_id, bank_code, account_number, due_date, deposit_status, deposited_at, created_at, updated_at)
VALUES
    (936001, 930023, 934011, '088', '930023000001', DATE_ADD(NOW(), INTERVAL 3 DAY), 'WAITING', NULL, NOW(), NOW()),
    (936002, 930025, 934012, '088', '930025000001', DATE_ADD(NOW(), INTERVAL 3 DAY), 'WAITING', NULL, NOW(), NOW());

INSERT INTO payment_webhook_logs (id, event_id, order_id, status, amount, provider, created_at, updated_at)
VALUES
    (990001, 'perf-existing-webhook-1', 'perf-order-930023', 'DONE', 700000, 'TOSS', NOW(), NOW());

-- Completion reports for report-list and admin-pending-report scenarios.
INSERT INTO completion_reports (id, milestone_id, type, content, status, submitted_at, file_url, reject_reason)
VALUES
    (950001, 910010, 'COMPLETION', '1단계 완료 보고서 성능 테스트 데이터입니다.', 'SUBMITTED', DATE_SUB(NOW(), INTERVAL 2 HOUR), 'milestone/reports/completion/perf-950001.pdf', NULL),
    (950002, 910004, 'COMPLETION', '반려된 완료 보고서 성능 테스트 데이터입니다.', 'REJECTED', DATE_SUB(NOW(), INTERVAL 1 DAY), 'milestone/reports/completion/perf-950002.pdf', '증빙 자료가 부족합니다.'),
    (950003, 910004, 'APPEAL', '소명 보고서 성능 테스트 데이터입니다.', 'SUBMITTED', DATE_SUB(NOW(), INTERVAL 1 HOUR), 'milestone/reports/appeal/perf-950003.pdf', NULL),
    (950010, 910021, 'COMPLETION', '3단계 최종 정산 검증용 완료 보고서입니다.', 'SUBMITTED', DATE_SUB(NOW(), INTERVAL 2 HOUR), 'milestone/reports/completion/perf-950010.pdf', NULL),
    (950011, 910022, 'COMPLETION', '승인/반려 동시성 검증용 완료 보고서입니다.', 'SUBMITTED', DATE_SUB(NOW(), INTERVAL 1 HOUR), 'milestone/reports/completion/perf-950011.pdf', NULL),
    (950012, 910022, 'APPEAL', '1회차 반려된 소명 보고서입니다.', 'REJECTED', DATE_SUB(NOW(), INTERVAL 3 DAY), 'milestone/reports/appeal/perf-950012.pdf', '소명 자료 부족'),
    (950013, 910022, 'APPEAL', '2회차 반려된 소명 보고서입니다.', 'REJECTED', DATE_SUB(NOW(), INTERVAL 2 DAY), 'milestone/reports/appeal/perf-950013.pdf', '추가 증빙 부족'),
    (950014, 910022, 'APPEAL', '3회차 소명 보고서 동시 반려 검증 대상입니다.', 'SUBMITTED', DATE_SUB(NOW(), INTERVAL 30 MINUTE), 'milestone/reports/appeal/perf-950014.pdf', NULL);

-- Fund usage rows for fund usage read load.
INSERT INTO fund_usages (id, idea_id, item_name, amount, used_at, created_at, updated_at)
VALUES
    (952001, 900002, '개발 서버 비용', 300000, DATE_SUB(CURDATE(), INTERVAL 3 DAY), NOW(), NOW()),
    (952002, 900002, '디자인 외주 비용', 500000, DATE_SUB(CURDATE(), INTERVAL 2 DAY), NOW(), NOW()),
    (952003, 900005, '콘텐츠 제작 비용', 400000, DATE_SUB(CURDATE(), INTERVAL 1 DAY), NOW(), NOW()),
    (952010, 900010, '자금 사용 한도 기준 데이터', 900000, DATE_SUB(CURDATE(), INTERVAL 1 DAY), NOW(), NOW());

-- Pre-settlement rows.
INSERT INTO pre_settlements (id, idea_id, amount, status, requested_at)
VALUES
    (955001, 900002, 1000000, 'COMPLETED', DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (955002, 900005, 1000000, 'REQUESTED', DATE_SUB(NOW(), INTERVAL 1 HOUR)),
    (955003, 900005, 500000, 'FAILED', DATE_SUB(NOW(), INTERVAL 3 HOUR)),
    (955010, 900010, 1000000, 'COMPLETED', DATE_SUB(NOW(), INTERVAL 2 DAY)),
    (955011, 900008, 1000000, 'COMPLETED', DATE_SUB(NOW(), INTERVAL 10 DAY)),
    (955012, 900005, 500000, 'FAILED', DATE_SUB(NOW(), INTERVAL 6 HOUR));

-- Settlement rows.
INSERT INTO settlements (id, idea_id, type, total_amount, platform_fee, payout_amount, status, idempotency_key, memo, created_at, updated_at)
VALUES
    (960001, 900002, 'INTERIM', 1000000, 0, 1000000, 'COMPLETED', 'perf-900002-pre-settlement-1', '선정산 완료 성능 테스트 데이터', NOW(), NOW()),
    (960002, 900003, 'FINAL', 12000000, 120000, 11880000, 'PENDING', 'perf-900003-final', '마감 성공 최종 정산 대기', NOW(), NOW()),
    (960003, 900004, 'INTERIM', 3000000, 0, 3000000, 'REFUNDED', 'perf-900004-goal-not-met-refund', '목표 미달성 환불 장부', NOW(), NOW()),
    (960004, 900005, 'FINAL', 10000000, 100000, 9900000, 'FAILED', 'perf-900005-final-failed', '지급 실패 재처리 대상', NOW(), NOW()),
    (960010, 900008, 'INTERIM', 1000000, 0, 1000000, 'COMPLETED', 'perf-900008-pre-settlement-1', '최종 정산 차감용 선정산 장부', NOW(), NOW()),
    (960011, 900006, 'INTERIM', 3000000, 0, 3000000, 'REFUNDED', 'perf-900006-cancel-refund-existing', '환불 중복 검증용 기존 환불 정산', NOW(), NOW()),
    (960012, 900007, 'FINAL', 1500000, 0, 1500000, 'PENDING', 'perf-900007-deposit-decision', '보증금 환급/몰수 동시성 검증 대상', NOW(), NOW());

-- Refund rows for refund-list/callback scenarios.
INSERT INTO refunds (id, payment_id, sponsor_id, amount, reason, status, created_at, updated_at)
VALUES
    (970001, 930007, 900101, 1500000, 'GOAL_NOT_MET', 'PENDING', NOW(), NOW()),
    (970002, 930008, 900102, 1500000, 'GOAL_NOT_MET', 'FAILED', NOW(), NOW()),
    (970010, 930011, 900101, 1000000, 'CANCELLED', 'PENDING', NOW(), NOW()),
    (970011, 930012, 900102, 1000000, 'CANCELLED', 'FAILED', NOW(), NOW());

-- Vbank ledgers for project fund-flow read load.
INSERT INTO vbank_ledgers (
    id, idea_id, type, direction, amount, balance_after, affects_balance,
    idempotency_key, reference_type, reference_id, memo, created_at, updated_at
)
VALUES
    (980001, 900002, 'DEPOSIT_PAID', 'IN', 3000000, 3000000, TRUE, 'perf-ledger-900002-deposit', 'DEPOSIT', 940002, '보증금 입금', NOW(), NOW()),
    (980002, 900002, 'FUNDING_PAID', 'IN', 3000000, 6000000, TRUE, 'perf-ledger-900002-funding-1', 'PAYMENT', 930001, '후원금 입금', NOW(), NOW()),
    (980003, 900002, 'FUNDING_PAID', 'IN', 3000000, 9000000, TRUE, 'perf-ledger-900002-funding-2', 'PAYMENT', 930002, '후원금 입금', NOW(), NOW()),
    (980004, 900002, 'PRE_SETTLEMENT_PAID', 'OUT', 1000000, 8000000, TRUE, 'perf-ledger-900002-pre-settlement', 'PRE_SETTLEMENT', 955001, '선정산 지급', NOW(), NOW()),
    (980005, 900002, 'FUND_USAGE_RECORDED', 'OUT', 300000, 8000000, FALSE, 'perf-ledger-900002-fund-usage-1', 'FUND_USAGE', 952001, '자금 사용 내역 공개', NOW(), NOW()),
    (980006, 900004, 'DEPOSIT_PAID', 'IN', 3000000, 3000000, TRUE, 'perf-ledger-900004-deposit', 'DEPOSIT', 940004, '보증금 입금', NOW(), NOW()),
    (980007, 900004, 'FUNDING_PAID', 'IN', 3000000, 6000000, TRUE, 'perf-ledger-900004-funding', 'PAYMENT', 930007, '후원금 입금', NOW(), NOW()),
    (980008, 900004, 'SPONSOR_REFUND_PAID', 'OUT', 1500000, 4500000, TRUE, 'perf-ledger-900004-refund-1', 'REFUND', 970001, '후원자 환불', NOW(), NOW()),
    (980009, 900005, 'DEPOSIT_PAID', 'IN', 3000000, 3000000, TRUE, 'perf-ledger-900005-deposit', 'DEPOSIT', 940005, '보증금 입금', NOW(), NOW()),
    (980010, 900005, 'FUNDING_PAID', 'IN', 10000000, 13000000, TRUE, 'perf-ledger-900005-funding', 'PAYMENT', 930009, '후원금 입금', NOW(), NOW()),
    (980011, 900006, 'DEPOSIT_PAID', 'IN', 900000, 900000, TRUE, 'perf-ledger-900006-deposit', 'DEPOSIT', 940006, '보증금 입금', NOW(), NOW()),
    (980012, 900006, 'FUNDING_PAID', 'IN', 3000000, 3900000, TRUE, 'perf-ledger-900006-funding', 'PAYMENT', 930011, '후원금 입금', NOW(), NOW()),
    (980013, 900007, 'DEPOSIT_PAID', 'IN', 1500000, 1500000, TRUE, 'perf-ledger-900007-deposit', 'DEPOSIT', 940007, '보증금 입금', NOW(), NOW()),
    (980014, 900007, 'FUNDING_PAID', 'IN', 5000000, 6500000, TRUE, 'perf-ledger-900007-funding', 'PAYMENT', 930014, '후원금 입금', NOW(), NOW()),
    (980015, 900008, 'DEPOSIT_PAID', 'IN', 3000000, 3000000, TRUE, 'perf-ledger-900008-deposit', 'DEPOSIT', 940008, '보증금 입금', NOW(), NOW()),
    (980016, 900008, 'FUNDING_PAID', 'IN', 10000000, 13000000, TRUE, 'perf-ledger-900008-funding', 'PAYMENT', 930016, '후원금 입금', NOW(), NOW()),
    (980017, 900008, 'PRE_SETTLEMENT_PAID', 'OUT', 1000000, 12000000, TRUE, 'perf-ledger-900008-pre-settlement', 'PRE_SETTLEMENT', 955011, '선정산 지급', NOW(), NOW()),
    (980018, 900009, 'DEPOSIT_PAID', 'IN', 3000000, 3000000, TRUE, 'perf-ledger-900009-deposit', 'DEPOSIT', 940009, '보증금 입금', NOW(), NOW()),
    (980019, 900009, 'FUNDING_PAID', 'IN', 10000000, 13000000, TRUE, 'perf-ledger-900009-funding', 'PAYMENT', 930018, '후원금 입금', NOW(), NOW()),
    (980020, 900010, 'DEPOSIT_PAID', 'IN', 900000, 900000, TRUE, 'perf-ledger-900010-deposit', 'DEPOSIT', 940010, '보증금 입금', NOW(), NOW()),
    (980021, 900010, 'FUNDING_PAID', 'IN', 3000000, 3900000, TRUE, 'perf-ledger-900010-funding', 'PAYMENT', 930020, '후원금 입금', NOW(), NOW()),
    (980022, 900010, 'PRE_SETTLEMENT_PAID', 'OUT', 1000000, 2900000, TRUE, 'perf-ledger-900010-pre-settlement', 'PRE_SETTLEMENT', 955010, '선정산 지급', NOW(), NOW()),
    (980023, 900010, 'FUND_USAGE_RECORDED', 'OUT', 900000, 2900000, FALSE, 'perf-ledger-900010-fund-usage', 'FUND_USAGE', 952010, '자금 사용 내역 공개', NOW(), NOW());

-- Payment-domain ledger base rows.
INSERT INTO vbank_ledgers (
    id, idea_id, type, direction, amount, balance_after, affects_balance,
    idempotency_key, reference_type, reference_id, memo, created_at, updated_at
)
VALUES
    (980024, 900002, 'FUNDING_PAID', 'IN', 800000, 8800000, TRUE, 'perf-ledger-900002-refundable-payment', 'PAYMENT', 930024, '환불 대상 후원금 입금', NOW(), NOW()),
    (980025, 900011, 'LEGACY_OPENING_BALANCE', 'IN', 0, 0, TRUE, 'legacy-opening-900011', 'Idea', 900011, '보증금 결제 테스트 시작 잔액', NOW(), NOW());

COMMIT;

SET FOREIGN_KEY_CHECKS = 1;
