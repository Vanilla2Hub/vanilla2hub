# Code Management

공통 코드(Code Type / Code) CRUD 및 CSV 내보내기/가져오기 기능 명세.

## 개념

| 개념 | 설명 |
|------|------|
| **Code Type** | 코드 그룹 (예: `CONNECTOR_TYPE`, `STATUS`) |
| **Code** | Code Type에 속하는 개별 코드 값 (예: `OKTA`, `ACTIVE`) |

Code는 반드시 Code Type에 속해야 한다. Code Type을 삭제하면 하위 Code도 함께 논리 삭제된다.

## 데이터 모델

### code_type

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| code | VARCHAR(50) UNIQUE | 대문자, 변경 불가 |
| name | VARCHAR(100) | |
| description | VARCHAR(500) | 선택 |
| attribute_schema | JSON | Code의 부가 속성 스키마 정의, 선택 |
| system_default | BOOLEAN | true이면 수정/삭제 불가 (Flyway DML로만 변경) |
| sort_order | INT | 정렬 순서 |
| deleted | BOOLEAN | 논리 삭제 |
| created_at / updated_at | TIMESTAMP UTC | BaseEntity 상속 |

### code

| 컬럼 | 타입 | 설명 |
|------|------|------|
| id | BIGINT PK | |
| code_type_id | BIGINT FK | code_type.id |
| code | VARCHAR(50) | 대문자, 변경 불가 |
| name | VARCHAR(100) | |
| description | VARCHAR(500) | 선택 |
| extra | JSON | `attribute_schema` 기반 속성 값 저장 (JSON 객체), 선택 |
| system_default | BOOLEAN | true이면 수정/삭제 불가 |
| sort_order | INT | 정렬 순서 |
| deleted | BOOLEAN | 논리 삭제 |
| created_at / updated_at | TIMESTAMP UTC | BaseEntity 상속 |

## Attribute Schema

Code Type은 `attribute_schema` 컬럼에 JSON 배열로 하위 Code의 부가 속성 필드를 정의할 수 있다.  
Code 등록/수정 시 이 스키마를 기반으로 동적 입력 폼이 렌더링되고, 서버 측 유효성 검증이 수행된다.

### AttributeField 구조

```json
[
  {
    "key": "auth_type",
    "label": "인증 유형",
    "type": "select",
    "required": true,
    "editable": true,
    "defaultValue": null,
    "options": ["OAUTH2", "API_KEY", "BASIC"],
    "refCodeTypeCode": null
  }
]
```

| 필드 | 타입 | 설명 |
|------|------|------|
| key | String | 내부 식별자, 최초 등록 후 변경 불가 |
| label | String | 화면 표시 이름 |
| type | String | `text` / `number` / `boolean` / `select` / `code_ref` |
| required | boolean | 코드 등록 시 필수 여부 |
| editable | boolean | false이면 코드 수정 시 기존 값 강제 유지 |
| defaultValue | String | 값 없을 때 자동 적용 |
| options | String[] | `select` 타입일 때 선택 가능 값 목록 |
| refCodeTypeCode | String | `code_ref` 타입일 때 참조 Code Type 코드 |

### 스키마 변경 규칙

- **key 추가**: 자유롭게 추가 가능
- **key 수정**: 불가 (삭제 후 재추가)
- **key 삭제**: 해당 key의 값이 모든 Code.extra에서 일괄 제거됨 (`JSON_REMOVE`)
- **required false→true 전환**: 기존 Code 중 값 없는 항목이 있으면 전환 불가 (409)

### code_ref 타입

다른 Code Type의 코드 값을 참조하는 필드. Code 등록 폼에서 해당 Code Type의 코드 목록을 Select Box로 표시한다.

- 참조 중인 코드를 삭제하려 하면 어디서 참조 중인지 표시하고 삭제 불가 (409)
- Code Type 삭제 시에도 해당 Code Type을 참조하는 code_ref 필드가 있으면 삭제 불가

## system_default

`system_default = true`인 Code Type 및 Code는 API를 통한 수정/삭제가 불가능하다.  
초기 데이터가 필요한 경우 Flyway DML 마이그레이션으로만 삽입하고 해당 플래그를 true로 설정한다.

```sql
INSERT INTO code_type (code, name, system_default, sort_order, deleted)
VALUES ('CONNECTOR_TYPE', '커넥터 유형', 1, 0, 0);
```

## Redis 캐싱

서버 기동 시 모든 Code 데이터를 Redis에 적재하고, CRUD 변경 시 해당 Code Type의 캐시를 즉시 갱신한다.

### 동작

| 시점 | 동작 |
|------|------|
| 서버 시작 (`ApplicationReadyEvent`) | 모든 Code Type 순회 → Redis 적재 (warm-up) |
| Code / Code Type CRUD | 해당 Code Type 캐시 refresh |
| Code Type 삭제 | 해당 캐시 key evict |

### 설계 결정

- **캐시 TTL**: `codes` 캐시는 TTL = 0 (무기한). 명시적 refresh/evict로만 관리
- **Redis 장애 시**: DB fallback (try-catch로 예외 무시하고 DB 직접 조회)
- **인-프로세스 캐시(Caffeine) 사용 금지**: 파드 간 불일치 방지 (D-015 참고)
- **`@Cacheable` 미사용**: DB fallback 로직을 명시적으로 제어하기 위해 `CacheManager` 직접 조작

### 설정

```yaml
spring:
  cache:
    type: redis  # dev/prod
    # type: none  # test
```

## API

Base path: `/api/code-types`

| Method | Path | 설명 |
|--------|------|------|
| GET | `/api/code-types` | Code Type 전체 조회 |
| POST | `/api/code-types` | Code Type 등록 |
| PUT | `/api/code-types/{id}` | Code Type 수정 |
| DELETE | `/api/code-types/{id}` | Code Type 삭제 (논리 삭제) |
| GET | `/api/code-types/export` | Code Type CSV 다운로드 |
| POST | `/api/code-types/import` | Code Type CSV 업로드 |
| GET | `/api/code-types/{id}/codes` | Code 목록 조회 |
| POST | `/api/code-types/{id}/codes` | Code 등록 |
| PUT | `/api/code-types/{id}/codes/{codeId}` | Code 수정 |
| DELETE | `/api/code-types/{id}/codes/{codeId}` | Code 삭제 (논리 삭제) |
| GET | `/api/code-types/{id}/codes/export` | Code CSV 다운로드 |
| POST | `/api/code-types/{id}/codes/import` | Code CSV 업로드 |

## CSV Export / Import

### 형식

Code Type CSV 컬럼: `code`, `name`, `description`, `sortOrder`

Code CSV 컬럼: `code`, `name`, `description`, `extra`, `sortOrder`

- 인코딩: UTF-8 BOM (`0xEF 0xBB 0xBF`) — Excel 직접 열기 지원
- `extra` JSON 필드는 Apache Commons CSV(RFC 4180)가 자동으로 이중 따옴표 처리하여 CSV 파싱 오류 없이 저장/복원된다.
  - 예: `{"key": "value"}` → CSV 내 `"{""key"": ""value""}"`
- `attribute_schema`는 CSV 대상에서 제외 (API 또는 UI로 별도 관리)

### Import 동작 규칙

- 동일 `code` 값이 이미 존재하면 **skip** (덮어쓰지 않음)
- 응답: `{ "created": N, "skipped": N }`

## 프론트엔드 구조

```
frontend/src/
├── api/codeApi.ts              # API 호출 + CSV export/import helper
├── pages/code/
│   ├── CodeManagementPage.tsx  # 메인 페이지 (Code Type / Code 2-panel)
│   ├── CodeTypeFormModal.tsx   # Code Type 등록/수정 모달 (attribute_schema 편집 포함)
│   └── CodeFormModal.tsx       # Code 등록/수정 모달 (스키마 기반 동적 필드 렌더링)
└── i18n/
    ├── index.ts                # i18next 초기화 (localStorage 언어 유지)
    └── locales/
        ├── ko.json
        └── en.json
```

### UI 구성

- 좌측 패널: Code Type 목록 + 행 클릭으로 선택 (자물쇠 아이콘 = system_default)
- 우측 패널: 선택된 Code Type의 Code 목록 (Code Type 미선택 시 비활성)
- 각 패널 상단: Export / Import / 등록 버튼
- system_default 레코드는 수정/삭제 버튼 비활성화
- 코드 목록의 extra 컬럼은 attribute_schema의 label 기준으로 Tag 형태로 표시
- 헤더 언어 토글(EN ↔ 한국어)로 UI 전체 언어 전환, `localStorage`에 유지

### 주요 구현 포인트

- `CodeManagementPage`에서 `selectedCodeType`은 `useState`가 아닌 `codeTypes` 쿼리에서 파생  
  → CodeType 수정 후 캐시 갱신 시 자동으로 최신 스키마 반영
- `CodeTypeFormModal`의 type 변경에 반응하는 options/refCodeTypeCode 필드는  
  `Form.Item noStyle shouldUpdate` 패턴으로 구현 (Ant Design Form.List 안에서 반응형 처리)
- `CodeFormModal`의 `code_ref` 필드는 `useQueries`로 참조 Code Type별 코드 목록 병렬 fetch

## 의존성

- **Backend**: `org.apache.commons:commons-csv:1.10.0`
- **Frontend**: `react-i18next`, `i18next`
