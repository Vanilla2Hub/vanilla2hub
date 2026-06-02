# Decision Log

프로젝트 진행 중 확정된 핵심 결정을 기록합니다.

## D-001: 환경 분리 전략

- Status: Accepted
- Decision: 환경은 `dev`, `prod` 2개만 운영
- Reason: 홈랩 운영 복잡도를 낮추고 유지보수 부담을 줄이기 위함

## D-002: 인증 전략

- Status: Accepted
- Decision: API 인증은 JWT / API Key / Basic 선택 적용 가능 구조로 설계
- Reason: 시스템별 연동 방식이 다를 수 있어 유연성 필요

## D-003: UI 인증 통합

- Status: Accepted
- Decision: Keycloak OIDC 기반 로그인, Okta는 Keycloak의 IdP로 연동 (Keycloak → Okta 페더레이션)
- Reason: 표준 OIDC 플로우 유지 + 외부 IdP 실습/확장성 확보

## D-004: 시크릿 관리

- Status: Accepted
- Decision: 앱 시크릿은 HashiCorp Vault를 단일 소스로 사용
- Reason: 평문 시크릿 분산을 방지하고 운영 보안성을 높이기 위함

## D-005: 플랫폼 통합

- Status: Accepted
- Decision: Observability는 Prometheus/Grafana, 품질은 SonarQube, CI/CD는 CircleCI 사용
- Reason: 실무에서 널리 쓰이는 조합으로 운영/확장 경험 축적 목적

## D-006: 프로젝트 운영 컨텍스트

- Status: Accepted
- Decision: 홈랩 중심 운영 + 필요 시 AWS 테스트 환경을 보조 검증용으로 사용
- Reason: 비용과 운영 복잡도 간 균형을 맞추면서 실무 유사성을 확보

## D-007: 프론트엔드 기술 스택

- Status: Accepted
- Decision: React + TypeScript + Vite로 프론트엔드 구성
- Reason: 타입 안정성 확보, Vite의 빠른 개발 서버 및 빌드 속도, 생태계 성숙도

## D-008: Stateless 인증 구조

- Status: Accepted
- Decision: httpOnly Cookie + Keycloak OIDC JWT 검증 방식으로 stateless 인증 구현
- Reason: XSS 방어(httpOnly), 서버 세션 불필요(stateless), Spring Security OAuth2 Resource Server와 자연스럽게 통합
- Detail:
  - `JwtCookieFilter`: `access_token` 쿠키 → `Authorization: Bearer` 헤더 변환
  - `SecurityConfig`: CORS (`allowCredentials`), `STATELESS` 세션, OAuth2 Resource Server 설정
  - `AuthController`: `POST /api/auth/session` (쿠키 발급), `DELETE /api/auth/session` (로그아웃)

## D-009: 레포 구조

- Status: Accepted
- Decision: Frontend(React)와 Backend(Spring Boot)를 모노레포로 관리
- Reason: 공통 설정 및 스크립트 공유, 배포 단위 일치, 단일 PR로 full-stack 변경 추적 가능

## D-010: 로컬 dev 도메인 분리 전략

- Status: Accepted
- Decision: 로컬 dev는 포트로 분리 (`localhost:3000` frontend / `localhost:8080` backend), 프로덕션은 서브도메인 분리
- Reason: 로컬에서 `/etc/hosts` 수정 없이 바로 시작 가능, `localhost` 공유로 httpOnly 쿠키 동작 보장, Vite proxy로 CORS 우회 가능
- Detail:
  - dev: `localhost:3000` ↔ `localhost:8080`
  - prod: `app.hjeon.i234.me` ↔ `api.hjeon.i234.me`

## D-011: 인가(Authorization) 전략

- Status: Accepted
- Decision: 초기 역할은 `user` / `admin` 2단계로 분리, Keycloak Realm Role로 관리
- Reason: 단순한 구조로 시작해 필요 시 확장, Keycloak에서 역할 중앙 관리로 백엔드 코드 변경 최소화

## D-012: 프론트엔드 독립적 백엔드 API 설계

- Status: Accepted
- Decision: 백엔드는 순수 REST API만 제공하며 특정 프론트엔드에 종속되지 않는다. Service A와 Service B 중 어느 쪽이 붙더라도 백엔드 변경 없이 동작해야 한다.
- Reason: PoC 단계에서 Service A로 개발하되, 필요하다면 Service B로 전환하거나 병행 운영할 수 있어야 함. Service A는 dev 환경에서 PoC 용도로 유지.
- Detail:
  - Service A (브라우저): Keycloak OIDC 인증 → httpOnly Cookie 방식
  - 백엔드는 두 방식을 모두 수용, 클라이언트 전환 시 백엔드 무변경
  - CORS allowed-origins는 설정 파일로 외부화. 도메인 추가 시 코드 변경 없음

## D-013: ~~다중 JWT 발급자(Multi-issuer) 인증 전략~~ → Superseded by D-026

- Status: Superseded
- Superseded by: D-026

## D-014: Kubernetes Pod 분리 전략

- Status: Accepted
- Decision: Frontend, Backend, Redis를 각각 별도 Deployment/StatefulSet으로 운영한다. 한 Pod에 묶지 않는다.
- Reason: 컴포넌트별 독립 스케일 아웃, 장애 격리, 리소스(CPU/메모리) 개별 설정을 위함
- Detail:
  - Backend: Deployment (replicas N, HPA 가능)
  - Frontend: Deployment (replicas N, HPA 가능)
  - Redis: StatefulSet (replicas 1, PVC로 데이터 보존)
  - 같은 Pod에 묶는 경우는 sidecar 패턴(log shipper, Vault Agent Injector 등)에 한정

## D-015: 공유 캐시 전략

- Status: Accepted
- Decision: 외부 API 응답 캐시는 Redis를 공유 캐시로 사용한다. 인-프로세스 캐시(Caffeine)는 사용하지 않는다.
- Reason: Stateless 멀티 파드 환경에서 인-프로세스 캐시는 파드 간 불일치가 발생한다. Redis를 통해 모든 파드가 동일한 캐시를 참조해야 한다.
- Detail:
  - Spring Cache + Redis (`spring-boot-starter-data-redis`, `spring.cache.type=redis`)
  - 기본 TTL: 5분 (캐시별 개별 설정 가능)
  - 수동 무효화: Prov/De-prov 완료 후 `@CacheEvict` 자동 처리
  - 어드민 전체 갱신 버튼: `POST /admin/cache/evict` (allEntries=true)
  - 캐시 키: 조회 필터 조합 기준
  - 향후 트래픽 증가 시 L1(Caffeine) + L2(Redis) 2단계 캐시로 확장 가능

## D-016: DB 커넥션 풀 관리 전략

- Status: Accepted
- Decision: HikariCP `maximum-pool-size`를 `(DB max_connections - 10) ÷ 최대 파드 수`로 계산하여 설정한다.
- Reason: 파드 수 증가 시 총 DB 커넥션이 max_connections를 초과하면 즉시 장애로 이어짐. 파드 단위로 풀 크기를 제한하여 전체 커넥션 수를 통제한다.
- Detail:
  - `minimum-idle`: 0 (유휴 시 커넥션 반환)
  - `connection-timeout`: 3000ms (장애 시 빠른 실패)
  - `max-lifetime`: MariaDB `wait_timeout`보다 짧게 (기본 580000ms)
  - `@Transactional` 범위는 최대한 짧게. 외부 API 호출은 트랜잭션 밖에서 실행
  - Grafana에서 HikariCP 메트릭(`hikaricp_connections_active`) 실시간 모니터링

## D-017: 외부 API 동적 어댑터 구조

- Status: Accepted
- Decision: 외부 REST API 연동은 특정 시스템에 종속되지 않는 동적 어댑터 구조로 설계한다. Provider 설정은 DB에서 관리하고 인증정보는 Vault에서 런타임 주입한다.
- Reason: Okta, Saviynt 등 특정 외부 시스템이 확정되지 않았으며, 향후 Provider 추가/교체 시 핵심 서비스 코드 변경 없이 어댑터 구현만으로 확장 가능해야 함
- Detail:
  - `AccessConnector` (조회용), `ProvisioningConnector` (실행용) 인터페이스 분리
  - `ConnectorRegistry`: 런타임 등록/조회/리로드. 재시작 없이 설정 변경 반영
  - `connector_config` 테이블: name, connector_type, base_url, auth_type, vault_secret_path, timeout_ms, retry_count, enabled
  - 인증정보는 DB 저장 금지. vault_secret_path만 저장하고 Vault에서 런타임 조회 (D-004 일관성 유지)
  - 공통 HTTP 정책(Timeout/Retry/ErrorMapping/TraceId)은 WebClient 빌더 레벨에서 일괄 적용

## D-018: 데이터 모델 네이밍 컨벤션 (PK / FK)

- Status: Accepted
- Decision: PK 컬럼명은 항상 `id` (BIGINT AUTO_INCREMENT). FK 컬럼명은 항상 `<참조테이블>_id`. JOIN 작성 시 테이블 별칭(alias) 사용을 의무화한다.
- Reason: JPA / Spring Data / Hibernate 디폴트와 일치해 BaseEntity 상속이 단순. 단일 테이블 쿼리에서 짧고 자연스러움. 도구·OSS·생태계 호환성 우수. 운영 환경 실습 목적상 업계 표준을 따르는 학습 가치가 큼.
- Detail:
  - PK: `id BIGINT AUTO_INCREMENT PRIMARY KEY`
  - FK: 참조하는 테이블명을 그대로 컬럼명에 사용 (예: `provisioning_request.connector_config_id`)
  - JOIN: `JOIN provisioning_request pr ON pr.connector_config_id = cc.id` 형태. `USING` 절은 사용하지 않음
  - 별칭 컨벤션은 `docs/DATA_MODEL.md`에 명시

## D-019: BaseEntity (`@MappedSuperclass`) 패턴 도입

- Status: Accepted
- Decision: 모든 엔티티의 공통 필드(`id`, `createdAt`, `updatedAt`)는 `@MappedSuperclass`로 추출한다. INSERT만 있는 테이블(`audit_log`)은 `createdAt`만 갖는 부모 클래스를 별도로 둔다.
- Reason: DRY 원칙, 일관성, 향후 audit 컬럼이나 soft-delete 추가 시 단일 지점 변경.
- Detail:
  - `BaseEntity`: `id` + `createdAt` + `updatedAt` (Spring Data JPA Auditing 기반)
  - `CreatedAtEntity`: `id` + `createdAt`만 (audit_log 등 immutable insert-only 테이블용)
  - 적용 시 `@EnableJpaAuditing` 활성화 필요
  - PK 컬럼명은 D-018에 따라 `id`로 통일되어 별도 `@AttributeOverride` 불필요

## D-020: 시간 컬럼 — TIMESTAMP + UTC 컨벤션

- Status: Accepted
- Decision: 모든 시간 컬럼은 `TIMESTAMP` 타입을 사용하고, JDBC 연결과 Hibernate에 UTC 컨벤션을 강제한다.
- Reason: 운영 환경 실습 목적. 멀티 머신(Windows Desktop / Macbook / Synology NAS) 환경에서 timezone 변환 버그를 회피하려면 저장은 UTC로 정규화하는 표준 운영 패턴이 적합.
- Detail:
  - JDBC URL: `jdbc:mysql://...?serverTimezone=UTC&useLegacyDatetimeCode=false`
  - Hibernate: `spring.jpa.properties.hibernate.jdbc.time_zone=UTC`
  - 앱 코드: `Instant` 또는 UTC `LocalDateTime` 사용
  - 표시 시점에만 사용자 timezone으로 변환 (프론트엔드 책임)

## D-021: JSON 컬럼 타입 채택

- Status: Accepted
- Decision: 구조화된 비정형 데이터(`provisioning_request.payload`, `audit_log.metadata`)는 MariaDB `JSON` 타입을 사용한다. (`TEXT`가 아님)
- Reason: DB 레벨에서 JSON 형식 검증(`JSON_VALID`) 자동 적용으로 무결성 확보. 디버깅 시 `JSON_EXTRACT` 사용 가능. 향후 generated column + index로 확장 가능. JSON 타입은 내부적으로 LONGTEXT 저장이라 일반 문자열 추출에도 제약 없음. SQL Server / SQLite 등으로 이식 시에는 컬럼 타입 1줄만 변경하면 되므로 이식 비용은 낮음.
- Detail:
  - DDL: `metadata JSON NULL`, `payload JSON NULL`
  - JPA 매핑: `@Column(columnDefinition = "JSON") private String payload;` (Java는 그냥 String)
  - 직렬화/역직렬화는 애플리케이션(Jackson)에서 담당

## D-022: Flyway 마이그레이션 전략 — 단일 소스, 단계 분리

- Status: Accepted
- Decision: 모든 DB 스키마 변경은 Flyway가 단일 진실 공급원. Spring Batch 메타테이블도 Flyway로 관리하며, `spring.batch.jdbc.initialize-schema=NEVER`로 자동 생성을 비활성화한다. 한 마이그레이션 = 한 책임 원칙.
- Reason: 운영 일관성, PR 리뷰 용이성, 롤백 지점 명확화.
- Detail:
  - V1: `connector_config` (M2 #4)
  - V2: `audit_log` (M2 #4 · D-023)
  - V3: Spring Batch 메타테이블 (M2 #4) — `spring-batch-core/org/springframework/batch/core/schema-mariadb.sql`을 그대로 복사
  - V4: `provisioning_request` + `connector_health_log` (M4)
  - 의존성: `org.flywaydb:flyway-core` + `flyway-mysql`
  - Liquibase는 채택하지 않음

## D-023: `audit_log` Early Adoption

- Status: Accepted
- Decision: `audit_log` 테이블을 M2 #4 시점에 V2로 도입한다 (기능 도메인 작업 이전).
- Reason: 처음부터 감사 로그를 누적해 향후 디버깅·운영 패턴 분석 기회 확보. 늦게 도입할수록 누락된 이력 구간이 생겨 감사 가치 저하.
- Detail:
  - 컬럼: `actor`, `actor_type` (USER/SERVICE_B), `action`, `target_type`, `target_id`, `result`, `metadata` (JSON), `trace_id`, `created_at`
  - INSERT-only. UPDATE/DELETE 금지 (운영 정책)
  - 보존 정책: 1년 후 archive (M5 운영 런북에서 정의)

## D-024: `connector_health_log` 상태 변화 시점만 기록

- Status: Accepted
- Decision: `connector_health_log`는 모든 헬스체크 결과가 아닌 **status가 직전과 다를 때**만 INSERT한다.
- Reason: 저장량 절감(주기적 체크 대비 1/100 수준). 실질 정보량(상태 전이)은 그대로 유지. 현재 상태 조회는 최신 1건 SELECT로 해결 가능.
- Detail:
  - 구현: 새 헬스체크 직전 마지막 status를 조회해 비교 후 INSERT 여부 결정
  - 현재 상태 쿼리 패턴은 `docs/DATA_MODEL.md` 3.4 절 참고
  - FK 정책: `ON DELETE CASCADE` (커넥터 삭제 시 이력도 정리)

## D-025: 빌드 도구 — Gradle 8.13 (multi-module)

- Status: Accepted
- Decision: 빌드 도구로 Gradle 8.13을 선택하고, 루트 + `backend` 서브모듈의 multi-module 구조로 구성한다. Maven은 채택하지 않는다.
- Reason: Kotlin DSL 기반의 타입 안전한 빌드 스크립트, 빠른 빌드(incremental build / build cache), 향후 서브모듈 추가(e.g. shared-lib, batch-worker) 시 루트 `settings.gradle.kts`에 `include`만 추가하면 되는 확장성.
- Detail:
  - 루트: `settings.gradle.kts` (모듈 선언), `build.gradle.kts` (공통 설정)
  - `backend/build.gradle.kts`: Spring Boot 플러그인 + 의존성 정의
  - Gradle Wrapper 사용 (`./gradlew`) — 팀원 로컬에 Gradle 설치 불필요
  - 주요 명령: `./gradlew :backend:bootRun`, `./gradlew :backend:test`, `./gradlew :backend:build`

## D-027: Code Type Attribute Schema 설계

- Status: Accepted
- Decision: Code Type에 `attribute_schema` JSON 컬럼을 두어 하위 Code의 부가 속성 필드를 동적으로 정의한다. Code의 속성 값은 기존 `extra` JSON 컬럼에 저장한다.
- Reason: 코드 유형마다 필요한 부가 정보가 다르다. 별도 컬럼이나 별도 테이블 없이, 스키마 정의와 값 저장 모두 JSON 컬럼을 활용해 단순하게 확장 가능하게 구성한다.
- Detail:
  - `attribute_schema`: `AttributeField[]` JSON 배열 (key, label, type, required, editable, defaultValue, options, refCodeTypeCode)
  - 지원 타입: `text`, `number`, `boolean`, `select`, `code_ref`
  - `code_ref`: 다른 Code Type을 참조. 해당 Code Type 삭제 또는 참조 코드 삭제 시 참조 무결성 검사 후 409 반환
  - key 삭제 시 모든 Code.extra에서 해당 key 일괄 제거 (`JSON_REMOVE` native query)
  - `system_default = true`인 Code Type / Code는 API를 통한 수정/삭제 불가 (Flyway DML로만 관리)
  - 서버 기동 시 Redis warm-up, CRUD 시 즉시 refresh (TTL=0, 명시적 무효화)

## D-028: SonarQube 외부 인프라로 분리

- Status: Accepted
- Decision: SonarQube를 프로젝트 외부 인프라로 운영한다. `docker-compose.sonar.yml`을 레포에서 제거하고 `localhost:9000`의 외부 서비스에 연결한다.
- Reason: SonarQube는 개발 시에만 사용하는 외부 품질 도구로, 프로젝트 배포 단위에 포함할 필요가 없다. 레포에 묶으면 다른 프로젝트에서 재사용이 어렵고 불필요한 의존성이 생긴다.
- Detail:
  - 분석 실행: `./gradlew sonar` (루트에서 실행, `build.gradle`의 `sonar` 블록 유지)
  - SonarQube 프로젝트 키: `vanilla2hub`

## D-026: 단일 JWT 발급자(Keycloak) + Postman 지원 인증 전략

- Status: Accepted
- Supersedes: D-013
- Decision: JWT 검증은 Keycloak 단일 issuer로 단순화한다. 브라우저는 httpOnly Cookie, Postman 등 API 클라이언트는 `Authorization: Bearer` 헤더로 직접 호출한다.
- Reason: Service B 연동 계획 제거 및 Vault 서명키 검증 복잡도 불필요. Postman 개발 편의성 확보가 더 중요.
- Detail:
  - `JwtCookieFilter`: `Authorization` 헤더가 이미 있으면 통과(Postman). 없으면 `access_token` 쿠키 → `Bearer` 헤더 변환(브라우저)
  - Spring OAuth2 Resource Server: `spring.security.oauth2.resourceserver.jwt.issuer-uri` 단일 설정으로 Keycloak JWKS 검증
  - `POST /api/auth/session`: 클라이언트가 `Authorization: Bearer <keycloak_token>`으로 호출 → Spring이 JWT 검증 후 httpOnly Cookie 발급
  - `DELETE /api/auth/session`: Cookie 즉시 만료
  - `app.cookie.secure`: dev=false, prod=true (yml 외부화)
