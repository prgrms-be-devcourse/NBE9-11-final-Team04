import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const USER_EMAIL = __ENV.USER_EMAIL || 'perf-sponsor03@seedlink.test';
const USER_PASSWORD = __ENV.USER_PASSWORD || 'password';
const PAYMENT_ID = __ENV.PAYMENT_ID || '930024';
const VUS = Number(__ENV.VUS || '50');
const ITERATIONS = Number(__ENV.ITERATIONS || VUS);
const MAX_DURATION = __ENV.MAX_DURATION || '3m';
const REQUEST_TIMEOUT = __ENV.REQUEST_TIMEOUT || '120s';
const DEBUG_UNEXPECTED = __ENV.DEBUG_UNEXPECTED === 'true';

const accepted = new Counter('payment_refund_accepted');
const rejectedAlreadyProcessed = new Counter('payment_refund_rejected_already_processed');
const unexpected = new Counter('payment_refund_unexpected');
const status400 = new Counter('payment_refund_status_400');
const status500 = new Counter('payment_refund_status_500');
const codeP002 = new Counter('payment_refund_code_P002');
const codeP007 = new Counter('payment_refund_code_P007');
const codeS006 = new Counter('payment_refund_code_S006');

http.setResponseCallback(http.expectedStatuses({ min: 200, max: 599 }));

export const options = {
  scenarios: {
    concurrent_payment_refund: {
      executor: 'shared-iterations',
      vus: VUS,
      iterations: ITERATIONS,
      maxDuration: MAX_DURATION,
    },
  },
  thresholds: {
    http_req_failed: ['rate==0'],
    payment_refund_accepted: ['count<=1'],
    payment_refund_unexpected: ['count==0'],
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
    `${BASE_URL}/payments/${PAYMENT_ID}/refund`,
    null,
    {
      headers: data.headers,
      timeout: REQUEST_TIMEOUT,
      tags: { name: 'payment_refund_concurrent_request' },
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
  if (code === 'P002') {
    codeP002.add(1);
  }
  if (code === 'P007') {
    codeP007.add(1);
  }
  if (code === 'S006') {
    codeS006.add(1);
  }

  const isAccepted = response.status === 200;
  const isAlreadyProcessedRejected = response.status === 400
    && ['P002', 'P007', 'S006'].includes(code);

  if (isAccepted) {
    accepted.add(1);
  } else if (isAlreadyProcessedRejected) {
    rejectedAlreadyProcessed.add(1);
  } else {
    unexpected.add(1);
    if (DEBUG_UNEXPECTED) {
      console.log(`unexpected status=${response.status}, code=${code}, body=${response.body}`);
    }
  }

  check(response, {
    'payment refund accepted or rejected as already processed': () => isAccepted || isAlreadyProcessedRejected,
  });
}
