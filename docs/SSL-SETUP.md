# SSL 인증서 적용 가이드

## 개요

Hiteen 운영 환경에 SSL/TLS 인증서를 적용하는 방법을 설명합니다.

- **도메인**: `*.hiteen.kr` (와일드카드 인증서)
- **적용일**: 2026-02-04
- **인증서 타입**: Nginx용 SSL 인증서

---

## 인증서 파일 구성

인증서 zip 파일(`wildcard_hiteen_kr_Nginx.zip`) 압축 해제 시 포함된 파일:

| 파일명 | 설명 |
|--------|------|
| `*.hiteen.kr_cert.pem` | 서버 인증서 |
| `*.hiteen.kr_chain.pem` | 중간 인증서 (체인) |
| `*.hiteen.kr_key.key` | 개인키 |
| `*.hiteen.kr_root.pem` | 루트 인증서 |
| `password.txt` | 개인키 비밀번호 (있는 경우) |

---

## 적용 절차

### 1. 인증서 파일 준비

```bash
# zip 파일 압축 해제
unzip wildcard_hiteen_kr_Nginx.zip -d nginx_cert
cd nginx_cert
unzip hiteen_kr.zip

# 인증서 체인 생성 (서버 인증서 + 중간 인증서)
cat "*.hiteen.kr_cert.pem" "*.hiteen.kr_chain.pem" > fullchain.pem
```

### 2. 인증서 파일을 K8s 마스터 노드로 복사

```bash
# 로컬에서 운영 서버로 파일 복사
scp fullchain.pem root@49.247.170.233:/tmp/
scp "*.hiteen.kr_key.key" root@49.247.170.233:/tmp/hiteen_kr_key.key
```

### 3. Kubernetes TLS Secret 생성

```bash
# 기존 Secret 삭제 (있다면)
kubectl delete secret hiteen-api-tls -n hiteen --ignore-not-found

# TLS Secret 생성
kubectl create secret tls hiteen-api-tls \
  --cert=/tmp/fullchain.pem \
  --key=/tmp/hiteen_kr_key.key \
  -n hiteen

# 확인
kubectl get secret hiteen-api-tls -n hiteen
```

### 4. Helm Chart 설정

`hiteen-app-chart/values.yaml`에서 TLS 설정:

```yaml
ingress:
  enabled: true
  className: nginx
  annotations:
    nginx.ingress.kubernetes.io/proxy-body-size: "50m"
    nginx.ingress.kubernetes.io/client-max-body-size: "50m"
    nginx.ingress.kubernetes.io/websocket-services: "hiteen-api"
    nginx.ingress.kubernetes.io/ssl-redirect: "true"
    nginx.ingress.kubernetes.io/force-ssl-redirect: "true"

  hosts:
    - host: api.hiteen.kr
      paths:
        - path: /
          pathType: Prefix
          backend:
            serviceName: hiteen-api
            servicePort: 8080

  tls:
    - secretName: hiteen-api-tls
      hosts:
        - api.hiteen.kr
        - "*.hiteen.kr"
```

### 5. Helm 업그레이드

```bash
helm upgrade hiteen-app ./hiteen-app-chart -n hiteen
```

### 6. 로드밸런서 설정

외부에서 HTTPS 접근을 위해 로드밸런서에서 443 포트 포워딩 필요:

| 외부 포트 | 노드 포트 | 프로토콜 |
|-----------|-----------|----------|
| 80 | 30956 | HTTP |
| 443 | 30544 | HTTPS |

**HAProxy 설정 예시** (`/etc/haproxy/haproxy.cfg`):

```
frontend https_front
    bind *:443
    mode tcp
    default_backend https_back

backend https_back
    mode tcp
    server node1 49.247.170.21:30544 check
```

**Nginx 설정 예시** (`/etc/nginx/nginx.conf`):

```nginx
stream {
    upstream https_backend {
        server 49.247.170.21:30544;
    }
    
    server {
        listen 443;
        proxy_pass https_backend;
    }
}
```

---

## 검증

### 내부 테스트 (K8s 노드에서)

```bash
# HTTPS 테스트
curl -k https://localhost:30544/actuator/health -H "Host: api.hiteen.kr"

# HTTP → HTTPS 리다이렉트 확인
curl -I http://localhost:30956/actuator/health -H "Host: api.hiteen.kr"
# 308 Permanent Redirect 응답 확인
```

### 외부 테스트

```bash
# HTTPS 접근 테스트
curl https://api.hiteen.kr/actuator/health

# 인증서 정보 확인
openssl s_client -connect api.hiteen.kr:443 -servername api.hiteen.kr < /dev/null 2>/dev/null | openssl x509 -noout -dates
```

---

## 인증서 갱신

인증서 만료 전에 갱신 필요. 갱신 절차:

1. 새 인증서 파일 준비 (위 1단계와 동일)
2. 기존 Secret 삭제 후 새로 생성:

```bash
kubectl delete secret hiteen-api-tls -n hiteen
kubectl create secret tls hiteen-api-tls \
  --cert=/tmp/fullchain.pem \
  --key=/tmp/hiteen_kr_key.key \
  -n hiteen

# Ingress Controller 재시작 (캐시 갱신)
kubectl rollout restart deployment ingress-nginx-controller -n ingress-nginx
```

---

## 트러블슈팅

### YAML 파싱 에러 (와일드카드 호스트)

`*.hiteen.kr` 같은 와일드카드 호스트는 YAML에서 특수문자로 인식됨.
`ingress.yaml` 템플릿에서 `quote` 함수 사용:

```yaml
# 잘못된 예
- {{ . }}

# 올바른 예
- {{ . | quote }}
```

### 443 포트 연결 실패

1. NodePort 확인:
```bash
kubectl get svc -n ingress-nginx
```

2. 로드밸런서에서 443 → NodePort(30544) 포워딩 설정 확인

3. 방화벽에서 443 포트 허용 확인

---

## 인프라 정보

| 구성요소 | IP/호스트 | 포트 |
|----------|-----------|------|
| 도메인 | api.hiteen.kr | 80, 443 |
| 로드밸런서 | 49.247.170.116 | 80→30956, 443→30544 |
| K8s 마스터 | 49.247.170.233 | - |
| K8s 워커 | 49.247.170.21 | 30956(HTTP), 30544(HTTPS) |

---

## 관련 파일

- `hiteen-app-chart/values.yaml` - Ingress TLS 설정
- `hiteen-app-chart/templates/ingress.yaml` - Ingress 템플릿
- `ssl/nginx_cert/` - SSL 인증서 원본 파일

