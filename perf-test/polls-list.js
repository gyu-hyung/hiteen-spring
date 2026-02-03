import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL =  'http://dev.hiteen.co.kr';
const targetUrl = '/api/polls?size=20&type=all';
const TOKEN = 'eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIwMTA5NTM5MzYzNyIsImlhdCI6MTc2ODc4NTIyNSwiZXhwIjoxNzY5NjQ5MjI1fQ.-CZSLnHyy5uOSO9r8XZXuUgMO_E7PwbddFjI-b_mHeIcfF3rUyaSklk_InPKiE97CRtZK223oYm5fsBV1TZL0g';

if (!TOKEN) {
  throw new Error('TOKEN env is required. e.g. export TOKEN="<JWT>"');
}

export const options = {
  vus: 150,
  duration: '5m',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<1000'],
  },
};

export default function () {
  const params = {
    headers: {
      Authorization: `Bearer ${TOKEN}`,
      Accept: 'application/json',
    },
    tags: { name: 'GET /api/polls' },
  };

  const url = `${BASE_URL}${targetUrl}`;
  const res = http.get(url, params);

  check(res, {
    'status is 200': (r) => r.status === 200,
  });

  sleep(0.1);
}

