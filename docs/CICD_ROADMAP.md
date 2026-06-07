# Vanilla2Hub CI/CD Roadmap

> 제품 기능 로드맵은 [ROADMAP.md](./ROADMAP.md) 참고

## 개요

```
GitHub Push
    ↓
CircleCI (빌드 / 테스트 / 이미지 빌드)
    ↓
공개 이미지 → GHCR (ghcr.io/vanilla2hub/...)
비공개 이미지 → Harbor on NAS (harbor.local/vanilla2hub/...)
    ↓
K3s on NAS (자동 배포)
```

---

## Phase 1 — 기반 구축

> 목표: 코드 push 시 자동 빌드 및 테스트

- [ ] GitHub 브랜치 전략 확정
  - `main` → 프로덕션 배포
  - `develop` → 스테이징 빌드
  - `feature/*` → 테스트만 실행
- [ ] CircleCI 연동 및 기본 `.circleci/config.yml` 작성
- [ ] Spring Boot 단위 테스트 자동 실행
- [ ] React 빌드 + 린트 자동 실행
- [ ] Gradle 의존성 캐시 설정 (크레딧 절약, DLC 비활성화)

---

## Phase 2 — 이미지 빌드 & 레지스트리

> 목표: 빌드 결과물을 컨테이너 이미지로 관리

- [ ] Dockerfile 멀티스테이지 빌드 최적화
  - API: `eclipse-temurin:17-jdk` → `eclipse-temurin:17-jre-alpine`
  - Frontend: `node` → `nginx:alpine`
- [ ] GHCR 연동 및 자동 push (`main` 머지 시)
  - 공개 이미지: `ghcr.io/vanilla2hub/api`, `ghcr.io/vanilla2hub/frontend`
- [ ] 이미지 태그 전략 확정
  - `$CIRCLE_SHA1` (커밋 기반) + `latest`
- [ ] Cloudflare Tunnel 설정 (공유기 포트 개방 없이 NAS 외부 노출)
- [ ] Harbor on NAS 설치 (K3s Helm)
  - 비공개 이미지 저장 (Keycloak SPI 커스텀 이미지 등)
  - Docker Hub 베이스 이미지 프록시 캐시
- [ ] 공개/비공개 이미지 push 분기 처리 (CircleCI workflow)

---

## Phase 3 — K3s 자동 배포

> 목표: 이미지 push 후 NAS K3s 자동 롤아웃

- [ ] K3s 매니페스트 작성
  - `Deployment` / `Service` / `Ingress` (Traefik)
  - `ConfigMap` / `Secret` (Vault 연동)
- [ ] Keycloak, PostgreSQL K3s 마이그레이션
- [ ] CircleCI → NAS SSH 배포 구성 (초기)
  ```bash
  kubectl set image deployment/api api=ghcr.io/vanilla2hub/api:$CIRCLE_SHA1
  ```
- [ ] `imagePullSecret` 설정 (GHCR / Harbor)
- [ ] Flyway 마이그레이션 K8s Job 자동 실행 (배포 전 선행)
- [ ] ArgoCD 도입 및 GitOps 전환 (SSH 배포 대체)
  - GitHub 매니페스트 push → ArgoCD 자동 동기화

---

## Phase 4 — 품질 게이트

> 목표: 머지 전 코드 품질 기준 강제화

- [ ] SonarCloud 연동 (공개 리포 무료, 풀 기능 사용 가능)
  - 브랜치 분석 / PR decoration / Quality Gate 포함
- [ ] CircleCI → SonarCloud 분석 연동 (`SONAR_TOKEN` 환경변수 등록)
- [ ] 커버리지 임계값 설정 (예: 70% 미만 시 빌드 실패)
- [ ] PR 단계에서 게이트 적용 (머지 블로킹)
- [ ] OWASP Dependency-Check 연동 (오픈소스 취약점 스캔)
- [ ] SonarQube 개발 머신(WSL) Docker 구성 — 비공개 소스 분석 전용
  ```bash
  docker run -d --name sonarqube -p 9000:9000 sonarqube:lts-community
  ```

---

## Phase 5 — 관측성 & 안정화

> 목표: 배포 파이프라인 운영 가시성 확보

- [ ] Prometheus + Grafana on K3s
  - Spring Boot Actuator 메트릭 수집
  - K3s 클러스터 메트릭 수집
- [ ] Loki 로그 수집 연동
- [ ] 파이프라인 실패 시 알림 (Slack / Discord)
- [ ] 롤백 전략 수립
  - 이전 SHA 태그로 재배포
  - ArgoCD 히스토리 기반 롤백

---

## 인프라 스펙 기준

| 단계 | 권장 RAM | 비고 |
|------|---------|------|
| Phase 1–2 | 16GB | 현재 NAS 구성 |
| Phase 3 | 16GB | ArgoCD ~200MB 추가 |
| Phase 4 | 16GB | SonarQube는 개발 머신(WSL)에서 운영 |
| Phase 5 | 32GB | 풀 관측성 스택 |

---

## 사용 도구

| 역할 | 도구 |
|------|------|
| CI/CD | CircleCI (Free) |
| 공개 레지스트리 | GitHub Container Registry (GHCR) |
| 비공개 레지스트리 | Harbor on NAS |
| 외부 노출 | Cloudflare Tunnel (Zero Trust) |
| 오케스트레이션 | K3s on NAS |
| GitOps | ArgoCD |
| 코드 품질 (공개) | SonarCloud (공개 리포 무료) |
| 코드 품질 (비공개) | SonarQube (개발 머신 WSL Docker) |
| 관측성 | Prometheus + Grafana + Loki |