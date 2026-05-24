# Vanilla2Hub Roadmap

## Phase 1: Foundation (Current Focus)
1. ✅ 모노레포 기본 구성 — Gradle multi-module, Spring Boot 3.5, React+Vite, Flyway V1·V2, BaseEntity
2. 인프라 설정 (K8s, MariaDB, Vault, Keycloak)
3. 로그인 및 인증/인가 구조 (Keycloak OIDC + JwtCookieFilter + Multi-issuer)
4. 시스템 공통 코드 관리 (하드코딩 방지를 위한 코드/그룹 UI CRUD)
5. 논리적 애플리케이션 원장 CRUD

## Phase 2: Ingestion Engine
5. 배치 잡 디자인
6. 워커 노드 디자인
7. 커넥션(동적 어댑터) 관련 디자인/개발
8. 타겟 시스템 리소스 수집 로직 개발

## Phase 3: Core Value (Mapping)
9. 리소스 매핑 룰 - 자동 매핑 엔진
10. 리소스 매핑 UI - 미아 리소스 수동 매핑 (Drag & Drop 등)

## Phase 4: Enterprise Readiness
11. 시각화 (대시보드)
12. 리포트 추출
13. 외부 API 제공 (Webhook, REST)
