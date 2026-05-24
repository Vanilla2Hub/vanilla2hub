# Vanilla2Hub

사내에 흩어진 레거시 IT 리소스와 클라우드 자원들을 수집하여, 비즈니스 기준점인 **논리적 애플리케이션(Logical App)** 원장에 매핑해주는 가시성 확보 허브입니다.

> CAASM / IGA Hub — 프로비저닝 툴이 아닌 **가시성(Visibility)** 플랫폼

## Tech Stack

| 영역 | 기술 |
|------|------|
| Backend | Java 17, Spring Boot 3.5, Spring Data JPA, Flyway |
| Frontend | React 19, TypeScript, Vite |
| Database | MariaDB (스키마 관리: Flyway) |
| Cache | Redis (Spring Cache) |
| Auth | Keycloak (OIDC), HashiCorp Vault |
| Infra | Kubernetes (Kind), Prometheus + Grafana |
| CI/CD | CircleCI, SonarQube |
| Build | Gradle 8.13 (multi-module) |

## 프로젝트 구조

```
vanilla2hub/
├── gradlew                          # Gradle wrapper (루트에서 실행)
├── settings.gradle.kts              # 모듈 선언
├── build.gradle.kts                 # 루트 공통 설정
├── backend/                         # Spring Boot 서브모듈
│   └── src/main/resources/
│       └── db/migration/            # Flyway 마이그레이션
└── frontend/                        # React + Vite
    └── src/
```

## 시작하기

### 사전 요구사항

- Java 17+
- Node.js 20+
- Docker (MariaDB, Redis, Keycloak 로컬 실행용)

### Backend

```bash
# dev 프로파일로 실행
./gradlew :backend:bootRun --args='--spring.profiles.active=dev'

# 테스트
./gradlew :backend:test

# 빌드
./gradlew :backend:build
```

### Frontend

```bash
cd frontend
npm install
npm run dev      # localhost:3000 (Vite dev server, /api → localhost:8080 proxy)
npm run build
npm run lint
```

## 환경별 도메인

| 환경 | Frontend | Backend |
|------|----------|---------|
| dev | `localhost:3000` | `localhost:8080` |
| prod | `app.hjeon.i234.me` | `api.hjeon.i234.me` |

## 문서

- [Architecture](docs/ARCHITECTURE.md)
- [Decision Log](docs/DECISIONS.md)
- [Roadmap](docs/ROADMAP.md)
