# Cert-Manager 설치 및 Let's Encrypt SSL 인증서 자동화 가이드

## 개요

cert-manager는 Kubernetes에서 SSL/TLS 인증서를 자동으로 발급하고 갱신해주는 도구입니다.
Let's Encrypt를 사용하여 무료 SSL 인증서를 자동으로 관리합니다.

## 장점

| 항목 | 수동 관리 | cert-manager |
|------|----------|--------------|
| 인증서 갱신 | ❌ 90일마다 수동 갱신 | ✅ 자동 갱신 |
| 관리 편의성 | ❌ 직접 발급/적용 | ✅ YAML만 작성 |
| 실수 가능성 | ❌ 갱신 누락 위험 | ✅ 자동화 |
| 확장성 | ❌ 도메인마다 수동 | ✅ Ingress에 추가만 |

---

## 1. cert-manager 설치

```bash
# cert-manager 설치 (CRD 포함)
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.14.4/cert-manager.yaml

# 설치 완료 대기 (약 1분)
kubectl wait --for=condition=Available deployment --all -n cert-manager --timeout=120s

# 설치 확인
kubectl get pods -n cert-manager
```

### 설치 확인 결과 예시
```
NAME                                       READY   STATUS    RESTARTS   AGE
cert-manager-6d4b4c6f9c-xxxxx              1/1     Running   0          1m
cert-manager-cainjector-7d4b4c6f9c-xxxxx   1/1     Running   0          1m
cert-manager-webhook-7d4b4c6f9c-xxxxx      1/1     Running   0          1m
```

### cert-manager hostNetwork 설정 (필요한 경우)

Pod에서 외부 HTTPS 연결이 안 되는 경우 (Let's Encrypt 서버 연결 타임아웃) hostNetwork 모드를 활성화합니다.

```bash
# cert-manager에 hostNetwork 추가
kubectl patch deployment cert-manager -n cert-manager --type='json' -p='[
  {"op": "add", "path": "/spec/template/spec/hostNetwork", "value": true},
  {"op": "add", "path": "/spec/template/spec/dnsPolicy", "value": "ClusterFirstWithHostNet"}
]'

# 재시작 완료 대기
kubectl rollout status deployment/cert-manager -n cert-manager --timeout=120s
```

**증상:** ClusterIssuer가 Ready 상태가 안 되고 다음 에러 발생
```
Failed to register ACME account: Get "https://acme-v02.api.letsencrypt.org/directory": dial tcp 172.65.32.248:443: i/o timeout
```

---

## 2. ClusterIssuer 생성 (Let's Encrypt)

### Production 환경 (실제 인증서)

```bash
cat <<EOF | kubectl apply -f -
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: gud5603@gmail.com  # 인증서 만료 알림 받을 이메일
    privateKeySecretRef:
      name: letsencrypt-prod-key
    solvers:
      - http01:
          ingress:
            class: nginx
EOF
```

### Staging 환경 (테스트용 - Rate Limit 없음)

```bash
cat <<EOF | kubectl apply -f -
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-staging
spec:
  acme:
    server: https://acme-staging-v02.api.letsencrypt.org/directory
    email: gud5603@gmail.com
    privateKeySecretRef:
      name: letsencrypt-staging-key
    solvers:
      - http01:
          ingress:
            class: nginx
EOF
```

### ClusterIssuer 확인

```bash
kubectl get clusterissuer
kubectl describe clusterissuer letsencrypt-prod
```

---

## 3. Ingress에 TLS 설정

### 방법 1: kubectl patch로 기존 Ingress 수정

```bash
kubectl patch ingress hiteen-api-ingress -n hiteen --type='json' -p='[
  {"op": "add", "path": "/metadata/annotations/cert-manager.io~1cluster-issuer", "value": "letsencrypt-prod"},
  {"op": "replace", "path": "/spec/tls", "value": [{"hosts": ["beta-api.hiteen.kr"], "secretName": "hiteen-beta-tls"}]}
]'
```

### 방법 2: values-dev.yaml에 설정 (Helm)

```yaml
ingress:
  enabled: true
  className: nginx
  annotations:
    nginx.ingress.kubernetes.io/proxy-body-size: "50m"
    cert-manager.io/cluster-issuer: "letsencrypt-prod"  # cert-manager 사용

  hosts:
    - host: beta-api.hiteen.kr
      paths:
        - path: /
          pathType: Prefix
          backend:
            serviceName: hiteen-api
            servicePort: 8080

  tls:
    - hosts:
        - beta-api.hiteen.kr
      secretName: hiteen-beta-tls  # 인증서가 저장될 Secret 이름
```

---

## 4. Ingress Controller hostNetwork 설정 (선택사항)

표준 포트(80/443)로 접근하려면 hostNetwork 모드를 활성화합니다.

```bash
kubectl patch deployment ingress-nginx-controller -n ingress-nginx --type='json' -p='[
  {"op": "add", "path": "/spec/template/spec/hostNetwork", "value": true},
  {"op": "add", "path": "/spec/template/spec/dnsPolicy", "value": "ClusterFirstWithHostNet"}
]'

# 재시작 완료 대기 (중요! - 이후 Ingress 패치 시 webhook 에러 방지)
kubectl rollout status deployment/ingress-nginx-controller -n ingress-nginx --timeout=120s
```

### 보안그룹 설정

클라우드 콘솔(NHN Cloud 등)에서 인바운드 규칙 추가:
- TCP 80 → 0.0.0.0/0
- TCP 443 → 0.0.0.0/0

---

## 5. 인증서 발급 확인

```bash
# Certificate 리소스 확인
kubectl get certificate -n hiteen

# 상세 정보 확인
kubectl describe certificate hiteen-beta-tls -n hiteen

# 인증서 Secret 확인
kubectl get secret hiteen-beta-tls -n hiteen

# 인증서 상태 실시간 확인
kubectl get certificate -n hiteen -w
```

### 정상 발급 시 출력 예시
```
NAME              READY   SECRET            AGE
hiteen-beta-tls   True    hiteen-beta-tls   2m
```

### 인증서 발급 실패 시 디버깅

```bash
# CertificateRequest 확인
kubectl get certificaterequest -n hiteen

# Challenge 확인 (HTTP-01 검증 상태)
kubectl get challenge -n hiteen

# cert-manager 로그 확인
kubectl logs -n cert-manager deployment/cert-manager -f
```

---

## 6. 테스트

```bash
# SSL 인증서 확인
curl -v https://beta-api.hiteen.kr/actuator/health 2>&1 | grep -A5 "Server certificate"

# API 호출 테스트
curl https://beta-api.hiteen.kr/actuator/health
```

---

## 7. 인증서 갱신

cert-manager는 **만료 30일 전에 자동으로 갱신**합니다.
수동 갱신이 필요한 경우:

```bash
# 인증서 Secret 삭제 (자동으로 재발급됨)
kubectl delete secret hiteen-beta-tls -n hiteen

# 또는 Certificate 리소스 재생성
kubectl delete certificate hiteen-beta-tls -n hiteen
# Ingress의 tls 설정이 있으면 자동으로 다시 생성됨
```

---

## 8. 문제 해결

### 인증서 발급이 안 될 때

1. **DNS 확인**: 도메인이 서버 IP를 가리키는지 확인
   ```bash
   nslookup beta-api.hiteen.kr
   ```

2. **80 포트 접근 확인**: Let's Encrypt HTTP-01 검증에 필요
   ```bash
   curl http://beta-api.hiteen.kr/.well-known/acme-challenge/test
   ```

3. **ClusterIssuer 상태 확인**
   ```bash
   kubectl describe clusterissuer letsencrypt-prod
   ```

4. **Challenge 상태 확인**
   ```bash
   kubectl describe challenge -n hiteen
   ```

### Rate Limit 에러

Let's Encrypt는 도메인당 주 50개 인증서 제한이 있습니다.
테스트 시에는 `letsencrypt-staging`을 사용하세요.

---

## 9. 전체 설치 스크립트

```bash
#!/bin/bash
# cert-manager 및 Let's Encrypt 설정 스크립트

set -e

NAMESPACE="hiteen"
DOMAIN="beta-api.hiteen.kr"
EMAIL="gud5603@gmail.com"
SECRET_NAME="hiteen-beta-tls"

echo "=== 1. cert-manager 설치 ==="
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v1.14.4/cert-manager.yaml
kubectl wait --for=condition=Available deployment --all -n cert-manager --timeout=120s

echo "=== 2. cert-manager hostNetwork 설정 (외부 HTTPS 연결 문제 해결) ==="
kubectl patch deployment cert-manager -n cert-manager --type='json' -p='[
  {"op": "add", "path": "/spec/template/spec/hostNetwork", "value": true},
  {"op": "add", "path": "/spec/template/spec/dnsPolicy", "value": "ClusterFirstWithHostNet"}
]' || echo "이미 설정됨"
kubectl rollout status deployment/cert-manager -n cert-manager --timeout=120s

echo "=== 3. ClusterIssuer 생성 ==="
cat <<EOF | kubectl apply -f -
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: letsencrypt-prod
spec:
  acme:
    server: https://acme-v02.api.letsencrypt.org/directory
    email: $EMAIL
    privateKeySecretRef:
      name: letsencrypt-prod-key
    solvers:
      - http01:
          ingress:
            class: nginx
EOF

echo "=== 4. Ingress Controller hostNetwork 설정 (80/443 포트 직접 사용) ==="
kubectl patch deployment ingress-nginx-controller -n ingress-nginx --type='json' -p='[
  {"op": "add", "path": "/spec/template/spec/hostNetwork", "value": true},
  {"op": "add", "path": "/spec/template/spec/dnsPolicy", "value": "ClusterFirstWithHostNet"}
]' || echo "이미 설정됨"
kubectl rollout status deployment/ingress-nginx-controller -n ingress-nginx --timeout=120s

echo "=== 5. Ingress TLS 설정 ==="
kubectl patch ingress hiteen-api-ingress -n $NAMESPACE --type='json' -p="[
  {\"op\": \"add\", \"path\": \"/metadata/annotations/cert-manager.io~1cluster-issuer\", \"value\": \"letsencrypt-prod\"},
  {\"op\": \"replace\", \"path\": \"/spec/tls\", \"value\": [{\"hosts\": [\"$DOMAIN\"], \"secretName\": \"$SECRET_NAME\"}]}
]"

echo "=== 6. 인증서 발급 대기 ==="
echo "인증서 발급까지 1-2분 소요됩니다..."
sleep 30
kubectl get certificate -n $NAMESPACE

echo "=== 완료 ==="
echo "인증서 상태 확인: kubectl get certificate -n $NAMESPACE"
echo "테스트: curl https://$DOMAIN/actuator/health"
```

---

## 참고 자료

- [cert-manager 공식 문서](https://cert-manager.io/docs/)
- [Let's Encrypt 문서](https://letsencrypt.org/docs/)
- [Ingress-NGINX TLS 설정](https://kubernetes.github.io/ingress-nginx/user-guide/tls/)

