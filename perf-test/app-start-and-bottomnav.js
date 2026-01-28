import http from 'k6/http';
import { check, group, sleep } from 'k6';

/**
 * 앱 최초 실행 + 바텀네비 각 탭 리스트 API들을 묶어서 때리는 시나리오.
 *
 * - BASE_URL, TOKEN은 환경변수로 주입 가능
 * - 위치/핀은 lat/lng 파라미터를 필요로 해서 기본값을 env로 받음
 *
 * required env:
 *   TOKEN=...  (JWT)
 * optional env:
 *   BASE_URL=http://dev.hiteen.co.kr
 *   LAT=37.5666
 *   LNG=127.0000
 */

const BASE_URL = 'http://dev.hiteen.co.kr';
const TOKEN = 'eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiIwMTA5NTM5MzYzNyIsImlhdCI6MTc2ODc4NTIyNSwiZXhwIjoxNzY5NjQ5MjI1fQ.-CZSLnHyy5uOSO9r8XZXuUgMO_E7PwbddFjI-b_mHeIcfF3rUyaSklk_InPKiE97CRtZK223oYm5fsBV1TZL0g';

const LAT = 37.5666;
const LNG = 127.0;

if (!TOKEN) {
  throw new Error('TOKEN env is required. e.g. TOKEN="<JWT>" k6 run ...');
}

function authHeaders() {
  return {
    headers: {
      Authorization: `Bearer ${TOKEN}`,
      Accept: 'application/json',
      'Content-Type': 'application/json',
    },
  };
}

export const options = {
  vus: 100,
  duration: '10m',
  thresholds: {
    http_req_failed: ['rate<0.01'],
    http_req_duration: ['p(95)<1500'],
  },
};

export default function () {
  const common = authHeaders();

  group('startup: app open', () => {
    // 1) POST /api/user-details
    // 서버 스펙상 대부분 optional 필드라 최소 payload로 호출
    const userDetailsRes = http.post(
      `${BASE_URL}/api/user-details`,
      JSON.stringify({
        deviceOs: 'k6',
        deviceVersion: '0',
        deviceDetail: 'perf-test',
      }),
      {
        ...common,
        tags: { name: 'POST /api/user-details' },
      }
    );
    check(userDetailsRes, { 'user-details 200': (r) => r.status === 200 });

    // 2) GET /api/attend
    const attendRes = http.get(`${BASE_URL}/api/attend`, {
      ...common,
      tags: { name: 'GET /api/attend' },
    });
    check(attendRes, { 'attend 200': (r) => r.status === 200 });

    // 3) GET /api/pins?lat&lng&radius
    const pinsRes = http.get(
      `${BASE_URL}/api/pins?lat=${encodeURIComponent(LAT)}&lng=${encodeURIComponent(LNG)}&radius=5000`,
      {
        ...common,
        tags: { name: 'GET /api/pins' },
      }
    );
    check(pinsRes, { 'pins 200': (r) => r.status === 200 });

    // 4) GET /api/friends
    const friendsRes = http.get(`${BASE_URL}/api/friends`, {
      ...common,
      tags: { name: 'GET /api/friends' },
    });
    check(friendsRes, { 'friends 200': (r) => r.status === 200 });

    // 5) POST /api/location
    const now = Date.now();
    const locationRes = http.post(
      `${BASE_URL}/api/location`,
      JSON.stringify({ lat: LAT, lng: LNG, timestamp: now }),
      {
        ...common,
        tags: { name: 'POST /api/location' },
      }
    );
    check(locationRes, { 'location 200': (r) => r.status === 200 });
  });

  group('bottom-nav: teenstory (boards)', () => {
    const res = http.get(`${BASE_URL}/api/boards?size=20&category=POST`, {
      ...common,
      tags: { name: 'GET /api/boards' },
    });
    check(res, { 'boards 200': (r) => r.status === 200 });
  });

  group('bottom-nav: teenpoll (polls)', () => {
    const res = http.get(`${BASE_URL}/api/polls?size=20&type=all`, {
      ...common,
      tags: { name: 'GET /api/polls' },
    });
    check(res, { 'polls 200': (r) => r.status === 200 });
  });

  group('bottom-nav: teennow (friends + pins)', () => {
    const friendsRes = http.get(`${BASE_URL}/api/friends`, {
      ...common,
      tags: { name: 'GET /api/friends (teennow)' },
    });
    check(friendsRes, { 'friends(teennow) 200': (r) => r.status === 200 });

    const pinsRes = http.get(
      `${BASE_URL}/api/pins?lat=${encodeURIComponent(LAT)}&lng=${encodeURIComponent(LNG)}&radius=5000`,
      {
        ...common,
        tags: { name: 'GET /api/pins (teennow)' },
      }
    );
    check(pinsRes, { 'pins(teennow) 200': (r) => r.status === 200 });
  });

  group('bottom-nav: teenpick (recommend today)', () => {
    // 실제 경로: /api/interests/users/recommend/today
    const res = http.get(`${BASE_URL}/api/interests/users/recommend/today`, {
      ...common,
      tags: { name: 'GET /api/interests/users/recommend/today' },
    });
    check(res, { 'recommend today 200': (r) => r.status === 200 });
  });

  group('bottom-nav: teenplay (games)', () => {
    const res = http.get(`${BASE_URL}/api/games`, {
      ...common,
      tags: { name: 'GET /api/games' },
    });
    check(res, { 'games 200': (r) => r.status === 200 });
  });

  sleep(1.0);
}

