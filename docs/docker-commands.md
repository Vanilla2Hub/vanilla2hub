# Docker 실행 명령어 모음

## 사전 준비

`.env` 파일이 프로젝트 루트에 없으면 `.env.example`을 복사해 생성한다.

```bash
cp .env.example .env
# .env 파일에서 KEYCLOAK_ISSUER_URI 등 환경에 맞게 수정
```

---

## 로컬 개발 스택 (DB + Redis + Backend + Frontend)

### 전체 기동

```bash
docker compose up -d
```

### 빌드 후 기동 (코드 변경 시)

```bash
# 전체 재빌드
docker compose up -d --build

# 특정 서비스만 재빌드
docker compose up -d --build backend
docker compose up -d --build frontend
```

### 중지

```bash
docker compose down
```

### 중지 + 볼륨 삭제 (DB 초기화 포함)

```bash
docker compose down -v
```

---

## 서비스별 개별 실행

```bash
# DB + Redis만 기동 (로컬 백엔드/프론트 직접 실행 시)
docker compose up -d db redis

# 백엔드만 재시작
docker compose restart backend

# 프론트엔드만 재시작
docker compose restart frontend
```

---

## 로그 확인

```bash
# 전체 로그 (실시간)
docker compose logs -f

# 서비스별 로그
docker compose logs -f backend
docker compose logs -f frontend
docker compose logs -f db
```

---

## 상태 확인

```bash
docker compose ps
```

---

## SonarQube (코드 품질 분석)

### SonarQube 서버 기동

```bash
docker compose -f docker-compose.sonar.yml up -d
```

> 초기 기동 후 http://localhost:9000 접속 가능 (admin / admin)

### 코드 분석 실행

```bash
# 루트 디렉터리에서
./gradlew :backend:test :backend:jacocoTestReport sonar
```

> `~/.gradle/gradle.properties`에 아래 항목이 있어야 한다.
> ```
> systemProp.sonar.host.url=http://localhost:9000
> systemProp.sonar.login=<발급한 토큰>
> ```

### SonarQube 중지

```bash
docker compose -f docker-compose.sonar.yml down
```

---

## 접속 주소

| 서비스 | URL |
|--------|-----|
| Frontend | http://localhost:3001 |
| Backend API | http://localhost:8080 |
| MariaDB | localhost:3306 |
| Redis | localhost:6379 |
| SonarQube | http://localhost:9000 |
