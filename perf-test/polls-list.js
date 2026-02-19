import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE_URL =  'http://api.hiteen.kr';
const targetUrl = '/api/polls?size=20&type=all';
const TOKEN = 'eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIwMTA5NTM5MzYzNyIsImlhdCI6MTc2OTczNzU5NSwiZXhwIjoyMDg1MDk3NTk1fQ.8favsmq9VkUB8K2Y8cJ4GYYEBTmduavqxPeKSWG-0LCycUWhKJsbUqdiG287NPkxiui7qEjP1tgqrYGdhTOqYw';

if (!TOKEN) {
  throw new Error('TOKEN env is required. e.g. export TOKEN="<JWT>"');
}

export const options = {
  vus: 100,
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

