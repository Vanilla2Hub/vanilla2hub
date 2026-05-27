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
| extra | JSON | 부가 정보 JSON 객체, 선택 (`null` 허용, 빈 문자열 불가) |
| sort_order | INT | 정렬 순서 |
| deleted | BOOLEAN | 논리 삭제 |
| created_at / updated_at | TIMESTAMP UTC | BaseEntity 상속 |

`extra` 컬럼은 MariaDB JSON 타입이므로 빈 문자열(`""`)을 넣으면 constraint 오류가 발생한다. 프론트엔드에서 빈 값은 `undefined`로 변환하여 `null`로 저장한다.

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

### Import 동작 규칙

- 동일 `code` 값이 이미 존재하면 **skip** (덮어쓰지 않음)
- 응답: `{ "created": N, "skipped": N }`

## 프론트엔드 구조

```
frontend/src/
├── api/codeApi.ts              # API 호출 + CSV export/import helper
├── pages/code/
│   ├── CodeManagementPage.tsx  # 메인 페이지 (Code Type / Code 2-panel)
│   ├── CodeTypeFormModal.tsx   # Code Type 등록/수정 모달
│   └── CodeFormModal.tsx       # Code 등록/수정 모달
└── i18n/
    ├── index.ts                # i18next 초기화 (localStorage 언어 유지)
    └── locales/
        ├── ko.json
        └── en.json
```

### UI 구성

- 좌측 패널: Code Type 목록 + 행 클릭으로 선택
- 우측 패널: 선택된 Code Type의 Code 목록 (Code Type 미선택 시 비활성)
- 각 패널 상단: Export / Import / 등록 버튼
- 헤더 언어 토글(EN ↔ 한국어)로 UI 전체 언어 전환, `localStorage`에 유지

## 의존성

- **Backend**: `org.apache.commons:commons-csv:1.10.0`
- **Frontend**: `react-i18next`, `i18next`
