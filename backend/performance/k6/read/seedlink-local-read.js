import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const USER_EMAIL = __ENV.USER_EMAIL;
const USER_PASSWORD = __ENV.USER_PASSWORD;
const IDEA_ID = __ENV.IDEA_ID || '1';
const MILESTONE_ID = __ENV.MILESTONE_ID || '1';
const LOAD_PROFILE = __ENV.LOAD_PROFILE || 'local';

http.setResponseCallback(http.expectedStatuses({ min: 200, max: 399 }, 401, 403, 404));

const profiles = {
  local: [
    { duration: '20s', target: 10 },
    { duration: '40s', target: 20 },
    { duration: '20s', target: 0 },
  ],
  normal: [
    { duration: '30s', target: 50 },
    { duration: '1m', target: 100 },
    { duration: '30s', target: 0 },
  ],
  target: [
    { duration: '30s', target: 75 },
    { duration: '1m', target: 150 },
    { duration: '30s', target: 0 },
  ],
  scale: [
    { duration: '1m', target: 250 },
    { duration: '2m', target: 500 },
    { duration: '1m', target: 0 },
  ],
  limit: [
    { duration: '1m', target: 500 },
    { duration: '2m', target: 1000 },
    { duration: '1m', target: 0 },
  ],
};

export const options = {
  scenarios: {
    read_traffic: {
      executor: 'ramping-vus',
      stages: profiles[LOAD_PROFILE] || profiles.local,
    },
  },
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<1000', 'p(99)<3000'],
  },
};

export function setup() {
  if (!USER_EMAIL || !USER_PASSWORD) {
    return { authHeaders: {} };
  }

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
    authHeaders: token ? { Authorization: `Bearer ${token}` } : {},
  };
}

export default function (data) {
  const publicHeaders = { Accept: 'application/json' };
  const authHeaders = { ...publicHeaders, ...data.authHeaders };

  const requests = [
    { name: 'ideas_list', request: ['GET', `${BASE_URL}/ideas`, null, { headers: publicHeaders, tags: { name: 'ideas_list' } }] },
    { name: 'ideas_top5', request: ['GET', `${BASE_URL}/ideas/top5`, null, { headers: publicHeaders, tags: { name: 'ideas_top5' } }] },
    { name: 'fundings_list', request: ['GET', `${BASE_URL}/fundings`, null, { headers: publicHeaders, tags: { name: 'fundings_list' } }] },
    { name: 'funding_detail', request: ['GET', `${BASE_URL}/fundings/${IDEA_ID}`, null, { headers: publicHeaders, tags: { name: 'funding_detail' } }] },
    { name: 'idea_detail', request: ['GET', `${BASE_URL}/ideas/${IDEA_ID}`, null, { headers: authHeaders, tags: { name: 'idea_detail' } }] },
    { name: 'milestones_by_idea', request: ['GET', `${BASE_URL}/milestones/ideas/${IDEA_ID}`, null, { headers: authHeaders, tags: { name: 'milestones_by_idea' } }] },
    { name: 'milestone_reports', request: ['GET', `${BASE_URL}/milestones/${MILESTONE_ID}/reports`, null, { headers: authHeaders, tags: { name: 'milestone_reports' } }] },
    { name: 'fund_usages', request: ['GET', `${BASE_URL}/fund-usages/${IDEA_ID}`, null, { headers: authHeaders, tags: { name: 'fund_usages' } }] },
    { name: 'vbank_ledgers', request: ['GET', `${BASE_URL}/payments/vbank-ledgers/ideas/${IDEA_ID}`, null, { headers: authHeaders, tags: { name: 'vbank_ledgers' } }] },
  ];

  const responses = http.batch(requests.map((item) => item.request));

  for (let i = 0; i < responses.length; i += 1) {
    const response = responses[i];
    const name = requests[i].name;
    check(response, {
      [`${name} status is 2xx/3xx/401/403/404`]: (res) =>
        (res.status >= 200 && res.status < 400) || [401, 403, 404].includes(res.status),
    });
  }

  sleep(1);
}
