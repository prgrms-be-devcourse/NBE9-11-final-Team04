import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const ADMIN_EMAIL = __ENV.ADMIN_EMAIL || 'perf-admin@seedlink.test';
const ADMIN_PASSWORD = __ENV.ADMIN_PASSWORD || 'password';
const MILESTONE_ID = __ENV.MILESTONE_ID || '910022';
const VUS = Number(__ENV.VUS || '50');
const ITERATIONS = Number(__ENV.ITERATIONS || VUS);
const MAX_DURATION = __ENV.MAX_DURATION || '3m';
const REQUEST_TIMEOUT = __ENV.REQUEST_TIMEOUT || '120s';
const DEBUG_UNEXPECTED = __ENV.DEBUG_UNEXPECTED === 'true';

const approveAccepted = new Counter('report_approve_accepted');
const rejectAccepted = new Counter('report_reject_accepted');
const rejectedAlreadyProcessed = new Counter('report_decision_rejected_already_processed');
const unexpected = new Counter('report_decision_unexpected');
const status400 = new Counter('report_decision_status_400');
const status500 = new Counter('report_decision_status_500');
const codeM005 = new Counter('report_decision_code_M005');

http.setResponseCallback(http.expectedStatuses({ min: 200, max: 599 }));

export const options = {
  scenarios: {
    concurrent_report_decision: {
      executor: 'shared-iterations',
      vus: VUS,
      iterations: ITERATIONS,
      maxDuration: MAX_DURATION,
    },
  },
  thresholds: {
    http_req_failed: ['rate==0'],
    report_approve_accepted: ['count<=1'],
    report_reject_accepted: ['count<=1'],
    report_decision_unexpected: ['count==0'],
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
  const action = __ITER % 2 === 0 ? 'approve' : 'reject';
  const url = action === 'approve'
    ? `${BASE_URL}/admin/milestones/${MILESTONE_ID}/reports/approve/completion`
    : `${BASE_URL}/admin/milestones/${MILESTONE_ID}/reports/reject`;
  const body = action === 'approve'
    ? null
    : JSON.stringify({ reason: `동시성 반려 검증 ${__ITER}` });

  const response = http.post(
    url,
    body,
    {
      headers: data.headers,
      timeout: REQUEST_TIMEOUT,
      tags: { name: `report_${action}_concurrent_request` },
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
  if (code === 'M005') {
    codeM005.add(1);
  }

  const isAccepted = response.status === 200;
  const isAlreadyProcessedRejected = response.status === 400 && code === 'M005';

  if (isAccepted && action === 'approve') {
    approveAccepted.add(1);
  } else if (isAccepted && action === 'reject') {
    rejectAccepted.add(1);
  } else if (isAlreadyProcessedRejected) {
    rejectedAlreadyProcessed.add(1);
  } else {
    unexpected.add(1);
    if (DEBUG_UNEXPECTED) {
      console.log(`unexpected action=${action}, status=${response.status}, code=${code}, body=${response.body}`);
    }
  }

  check(response, {
    'report decision accepted once or rejected as already processed': () => isAccepted || isAlreadyProcessedRejected,
  });
}
