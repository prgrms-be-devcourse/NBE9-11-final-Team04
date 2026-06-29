import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const ADMIN_EMAIL = __ENV.ADMIN_EMAIL || 'perf-admin@seedlink.test';
const ADMIN_PASSWORD = __ENV.ADMIN_PASSWORD || 'password';
const MILESTONE_ID = __ENV.MILESTONE_ID || '910021';
const VUS = Number(__ENV.VUS || '50');
const ITERATIONS = Number(__ENV.ITERATIONS || VUS);
const MAX_DURATION = __ENV.MAX_DURATION || '3m';
const REQUEST_TIMEOUT = __ENV.REQUEST_TIMEOUT || '120s';
const DEBUG_UNEXPECTED = __ENV.DEBUG_UNEXPECTED === 'true';

const accepted = new Counter('final_settlement_accepted');
const rejectedAlreadyProcessed = new Counter('final_settlement_rejected_already_processed');
const unexpected = new Counter('final_settlement_unexpected');
const status400 = new Counter('final_settlement_status_400');
const status409 = new Counter('final_settlement_status_409');
const status500 = new Counter('final_settlement_status_500');
const codeM005 = new Counter('final_settlement_code_M005');
const codeS003 = new Counter('final_settlement_code_S003');

http.setResponseCallback(http.expectedStatuses({ min: 200, max: 599 }));

export const options = {
  scenarios: {
    concurrent_final_settlement: {
      executor: 'shared-iterations',
      vus: VUS,
      iterations: ITERATIONS,
      maxDuration: MAX_DURATION,
    },
  },
  thresholds: {
    http_req_failed: ['rate==0'],
    final_settlement_accepted: ['count<=1'],
    final_settlement_unexpected: ['count==0'],
  },
};

export function setup() {
  const login = http.post(
    `${BASE_URL}/auth/login`,
    JSON.stringify({ email: ADMIN_EMAIL, password: ADMIN_PASSWORD }),
    { headers: { 'Content-Type': 'application/json' } },
  );

  check(login, {
    'admin login status is 200': (res) => res.status === 200,
    'admin login has access token': (res) => Boolean(res.json('data.accessToken')),
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
    `${BASE_URL}/admin/milestones/${MILESTONE_ID}/reports/approve/completion`,
    null,
    {
      headers: data.headers,
      timeout: REQUEST_TIMEOUT,
      tags: { name: 'final_settlement_approve_completion_concurrent_request' },
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
  if (response.status === 409) {
    status409.add(1);
  }
  if (response.status >= 500) {
    status500.add(1);
  }
  if (code === 'M005') {
    codeM005.add(1);
  }
  if (code === 'S003') {
    codeS003.add(1);
  }

  const isAccepted = response.status === 200;
  const isAlreadyProcessedRejected = [400, 409].includes(response.status)
    && ['M005', 'S003'].includes(code);

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
    'final settlement approved once or rejected as already processed': () => isAccepted || isAlreadyProcessedRejected,
  });
}
