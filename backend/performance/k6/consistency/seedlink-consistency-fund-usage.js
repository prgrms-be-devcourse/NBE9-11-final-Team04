import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const USER_EMAIL = __ENV.USER_EMAIL || 'perf-proposer@seedlink.test';
const USER_PASSWORD = __ENV.USER_PASSWORD || 'password';
const IDEA_ID = __ENV.IDEA_ID || '900010';
const AMOUNT = Number(__ENV.AMOUNT || '100000');
const VUS = Number(__ENV.VUS || '50');
const ITERATIONS = Number(__ENV.ITERATIONS || VUS);
const MAX_DURATION = __ENV.MAX_DURATION || '3m';
const REQUEST_TIMEOUT = __ENV.REQUEST_TIMEOUT || '120s';
const DEBUG_UNEXPECTED = __ENV.DEBUG_UNEXPECTED === 'true';

const accepted = new Counter('fund_usage_accepted');
const rejectedByLimit = new Counter('fund_usage_rejected_by_limit');
const unexpected = new Counter('fund_usage_unexpected');
const status400 = new Counter('fund_usage_status_400');
const status500 = new Counter('fund_usage_status_500');
const codeFu004 = new Counter('fund_usage_code_FU004');

http.setResponseCallback(http.expectedStatuses({ min: 200, max: 599 }));

export const options = {
  scenarios: {
    concurrent_fund_usage: {
      executor: 'shared-iterations',
      vus: VUS,
      iterations: ITERATIONS,
      maxDuration: MAX_DURATION,
    },
  },
  thresholds: {
    http_req_failed: ['rate==0'],
    fund_usage_accepted: ['count<=1'],
    fund_usage_unexpected: ['count==0'],
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
    `${BASE_URL}/fund-usages/${IDEA_ID}`,
    JSON.stringify({
      itemName: `동시성 검증 지출 ${__ITER}`,
      amount: AMOUNT,
      usedAt: new Date().toISOString().slice(0, 10),
    }),
    {
      headers: data.headers,
      timeout: REQUEST_TIMEOUT,
      tags: { name: 'fund_usage_concurrent_request' },
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
  if (code === 'FU004') {
    codeFu004.add(1);
  }

  const isAccepted = response.status === 200;
  const isLimitRejected = response.status === 400 && code === 'FU004';

  if (isAccepted) {
    accepted.add(1);
  } else if (isLimitRejected) {
    rejectedByLimit.add(1);
  } else {
    unexpected.add(1);
    if (DEBUG_UNEXPECTED) {
      console.log(`unexpected status=${response.status}, code=${code}, body=${response.body}`);
    }
  }

  check(response, {
    'fund usage accepted or rejected by limit': () => isAccepted || isLimitRejected,
  });
}
