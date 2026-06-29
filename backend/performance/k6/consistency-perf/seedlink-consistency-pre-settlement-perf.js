import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const USER_EMAIL = __ENV.USER_EMAIL || 'perf-proposer@seedlink.test';
const USER_PASSWORD = __ENV.USER_PASSWORD || 'password';
const IDEA_ID = __ENV.IDEA_ID || '900002';
const AMOUNT = Number(__ENV.AMOUNT || '100000');
const VUS = Number(__ENV.VUS || '100');
const ITERATIONS = Number(__ENV.ITERATIONS || VUS);
const MAX_DURATION = __ENV.MAX_DURATION || '3m';
const REQUEST_TIMEOUT = __ENV.REQUEST_TIMEOUT || '120s';
const DEBUG_UNEXPECTED = __ENV.DEBUG_UNEXPECTED === 'true';

const accepted = new Counter('pre_settlement_accepted');
const rejectedByLimit = new Counter('pre_settlement_rejected_by_limit');
const unexpected = new Counter('pre_settlement_unexpected');
const conflictOrLockRejected = new Counter('pre_settlement_conflict_or_lock_rejected');
const status400 = new Counter('pre_settlement_status_400');
const status500 = new Counter('pre_settlement_status_500');
const codePs001 = new Counter('pre_settlement_code_PS001');
const codePs005 = new Counter('pre_settlement_code_PS005');
const codeS006 = new Counter('pre_settlement_code_S006');

http.setResponseCallback(http.expectedStatuses({ min: 200, max: 599 }));

export const options = {
  scenarios: {
    concurrent_pre_settlement_perf: {
      executor: 'shared-iterations',
      vus: VUS,
      iterations: ITERATIONS,
      maxDuration: MAX_DURATION,
    },
  },
  thresholds: {
    http_req_failed: ['rate==0'],
    pre_settlement_unexpected: ['count==0'],

    // 정합성 테스트에 응답 시간 기준을 함께 얹어, 동시 요청 상황의 처리 속도까지 확인한다.
    http_req_duration: ['p(95)<1000', 'p(99)<3000'],
    iteration_duration: ['p(95)<3000'],
  },
};

export function setup() {
  const login = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({ email: USER_EMAIL, password: USER_PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } },
  );

  check(login, {
    'login status is 200': (res) => res.status === 200,
    'login has access token': (res) => Boolean(res.json('data.accessToken')),
  });

  const token = login.json('data.accessToken');
  return {
    headers: {
      Accept: 'application/json',
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`,
    },
  };
}

export default function (data) {
  const response = http.post(
    `${BASE_URL}/pre-settlements/ideas/${IDEA_ID}`,
    JSON.stringify({ amount: AMOUNT }),
    {
      headers: data.headers,
      timeout: REQUEST_TIMEOUT,
      tags: { name: 'pre_settlement_concurrent_perf_request' },
    },
  );

  let code = null;
  try {
    code = response.json('code');
  } catch (error) {
    code = null;
  }

  if (response.status === 400) {
    status400.add(1);
  }
  if (response.status >= 500) {
    status500.add(1);
  }
  if (code === 'PS001') {
    codePs001.add(1);
  }
  if (code === 'PS005') {
    codePs005.add(1);
  }
  if (code === 'S006') {
    codeS006.add(1);
  }

  const isAccepted = response.status === 200;
  const isLimitRejected = response.status === 400 && code === 'PS001';
  const isConflictOrLockRejected = response.status >= 400
    && response.status < 500
    && ['PS005', 'S006'].includes(code);

  if (isAccepted) {
    accepted.add(1);
  } else if (isLimitRejected) {
    rejectedByLimit.add(1);
  } else if (isConflictOrLockRejected) {
    conflictOrLockRejected.add(1);
    unexpected.add(1);
    if (DEBUG_UNEXPECTED) {
      console.log(`conflict/lock status=${response.status}, code=${code}, body=${response.body}`);
    }
  } else {
    unexpected.add(1);
    if (DEBUG_UNEXPECTED) {
      console.log(`unexpected status=${response.status}, code=${code}, body=${response.body}`);
    }
  }

  check(response, {
    'pre-settlement accepted or rejected by limit': () => isAccepted || isLimitRejected,
  });
}
