import http from 'k6/http';
import { check } from 'k6';
import { Counter } from 'k6/metrics';

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';
const ADMIN_EMAIL = __ENV.ADMIN_EMAIL || 'perf-admin@seedlink.test';
const ADMIN_PASSWORD = __ENV.ADMIN_PASSWORD || 'password';
const IDEA_ID = __ENV.IDEA_ID || '900007';
const VUS = Number(__ENV.VUS || '50');
const ITERATIONS = Number(__ENV.ITERATIONS || VUS);
const MAX_DURATION = __ENV.MAX_DURATION || '3m';
const REQUEST_TIMEOUT = __ENV.REQUEST_TIMEOUT || '120s';
const DEBUG_UNEXPECTED = __ENV.DEBUG_UNEXPECTED === 'true';

const releaseAccepted = new Counter('deposit_release_accepted');
const forfeitAccepted = new Counter('deposit_forfeit_accepted');
const rejectedAlreadyProcessed = new Counter('deposit_decision_rejected_already_processed');
const unexpected = new Counter('deposit_decision_unexpected');
const status400 = new Counter('deposit_decision_status_400');
const status500 = new Counter('deposit_decision_status_500');
const codeP007 = new Counter('deposit_decision_code_P007');
const codeS003 = new Counter('deposit_decision_code_S003');
const codeS006 = new Counter('deposit_decision_code_S006');

http.setResponseCallback(http.expectedStatuses({ min: 200, max: 599 }));

export const options = {
    scenarios: {
        concurrent_deposit_decision_perf: {
            executor: 'shared-iterations',
            vus: VUS,
            iterations: ITERATIONS,
            maxDuration: MAX_DURATION,
        },
    },
    thresholds: {
        // 정합성 기준
        http_req_failed: ['rate==0'],
        deposit_decision_unexpected: ['count==0'],

        // 성능 기준
        http_req_duration: ['p(95)<1000', 'p(99)<3000'],
        iteration_duration: ['p(95)<3000'],
    },
};

export function setup() {
    const login = http.post(
        `${BASE_URL}/auth/login`,
        JSON.stringify({
            email: ADMIN_EMAIL,
            password: ADMIN_PASSWORD,
        }),
        {
            headers: {
                'Content-Type': 'application/json',
            },
        },
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
    const action = __ITER % 2 === 0 ? 'release' : 'forfeit';

    const response = http.post(
        `${BASE_URL}/admin/settlements/ideas/${IDEA_ID}/deposit/${action}`,
        null,
        {
            headers: data.headers,
            timeout: REQUEST_TIMEOUT,
            tags: {
                name: `deposit_${action}_concurrent_perf_request`,
            },
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

    if (code === 'P007') {
        codeP007.add(1);
    }

    if (code === 'S003') {
        codeS003.add(1);
    }

    if (code === 'S006') {
        codeS006.add(1);
    }

    const isAccepted = response.status === 200;

    const isAlreadyProcessedRejected =
        [400, 409].includes(response.status) &&
        ['P007', 'S003', 'S006'].includes(code);

    if (isAccepted && action === 'release') {
        releaseAccepted.add(1);
    } else if (isAccepted && action === 'forfeit') {
        forfeitAccepted.add(1);
    } else if (isAlreadyProcessedRejected) {
        rejectedAlreadyProcessed.add(1);
    } else {
        unexpected.add(1);

        if (DEBUG_UNEXPECTED) {
            console.log(
                `unexpected action=${action}, status=${response.status}, code=${code}, body=${response.body}`,
            );
        }
    }

    check(response, {
        'deposit decision accepted or rejected as already processed': () =>
            isAccepted || isAlreadyProcessedRejected,
    });
}