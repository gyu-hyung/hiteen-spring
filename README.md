# HITEEN 개발 환경 구축 가이드

이 문서는 HITEEN 프로젝트의 로컬 개발 환경을 **Docker + Kubernetes (Minikube)** 기반으로 설정하는 방법을 설명합니다.  
모든 서비스(PostgreSQL, MongoDB, Redis, Soketi)는 `k8s/` 디렉토리 내 매니페스트와 `hiteen-install.sh` 스크립트를 통해 배포됩니다.

---

## 1. 프로젝트 정보

- **Group**: `kr.jiasoft`
- **Version**: `0.0.1-SNAPSHOT`
- **Java**: 17
- **Kotlin**: 1.9.25
- **Spring Boot**: 3.5.4
- **주요 스택**:
    - Spring WebFlux (리액티브 서버)
    - Spring Security
    - R2DBC + PostgreSQL
    - 위치저장용 MongoDB (Reactive)
    - Redis (Reactive)
    - Swagger (springdoc-openapi)
    - JWT (jjwt)
    - Soketi (WebSocket 서버)

---

## 2. 사전 요구사항

### 필수 도구 설치
- [Docker](https://docs.docker.com/get-docker/)
- [kubectl](https://kubernetes.io/docs/tasks/tools/)
- [Minikube](https://minikube.sigs.k8s.io/docs/start/)

---
