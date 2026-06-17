# 반영/장애 관리 시스템

에듀넷 서비스 운영(SM) 단계의 **반영 계획 → 반영 이력 → 장애 → 장애 분석**을
하나의 계층 구조로 관리하는 시스템입니다.
반영 계획서 자동 생성, git diff 기반 사이드이펙트/취약점 분석, LLM 기반 문서 생성을
지원합니다.

---

## 데이터 계층 구조

```
ReleasePlan(반영 계획서)
└── ReleaseHistory(반영 이력 = SR 1건, 엑셀 "시스템 반영 작업 요청" 한 행)
    └── Incident(장애)
        └── IncidentAnalysis(장애 분석)
```

- **반영 계획서**: 공유 Excel 업로드(+선택적 git 정보)로 뼈대를 자동 생성합니다.
  여러 날짜 시트가 들어있는 공유 Excel은 **일괄 등록**으로 시트(날짜)별 계획서를
  한 번에 만들 수 있습니다(아래 "엑셀 일괄 등록" 참고).
- **반영 이력**: 엑셀의 한 행이 곧 SR 1건과 1:1로 매핑됩니다. SR 번호만 입력하면
  서비스/작업내용/요청자 등 상세 정보는 **레드마인 연동**으로 채워집니다(연동은 추후 구현 예정).
- git 커밋 매핑은 별도 git 커밋 목록 API(`/api/git/commits`)로 조회해 사용자가 직접 매핑합니다.

---

## 기술 스택

| 구분 | 기술 |
|------|------|
| 프론트엔드 | Vue 3 (`<script setup>`), Vite, Vue Router, Axios, Tailwind CSS |
| 백엔드 | Spring Boot 3.2, JDK 17, Gradle |
| 데이터베이스 | PostgreSQL |
| AI | 로컬 LLM API (kwaklabs.com) |

---

## 사전 요구사항

- JDK 17 이상
- Node.js 18 이상
- PostgreSQL
- git CLI (사이드이펙트/취약점 분석 및 커밋 목록 조회에 사용)

---

## 1. 데이터베이스 준비

PostgreSQL에 접속하여 데이터베이스를 생성합니다.

```sql
CREATE DATABASE incident_db;
```

최초 실행 시 `data.sql`의 샘플 데이터가 자동으로 삽입됩니다(idempotent).

---

## 2. 백엔드 실행

### 설정 파일 (프로파일 분리)

공통 설정과 로컬 비밀 값은 분리되어 있습니다.

| 파일 | 용도 | git 추적 |
|------|------|----------|
| `application.yml` | 공통 설정(포트, JPA, SQL init 등) | ✅ |
| `application-local.yml` | DB 접속/LLM/git 등 로컬 비밀 값 | ❌ (gitignore) |
| `application-local.yml.example` | 로컬 설정 템플릿 | ✅ |

최초 1회, 템플릿을 복사해 실제 값을 채웁니다.

```bash
cd backend/src/main/resources
cp application-local.yml.example application-local.yml
# application-local.yml 의 datasource / ai.llm / git 값을 환경에 맞게 수정
```

`application-local.yml` 주요 항목:

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/incident_db
    username: postgres        # DB 계정으로 변경
    password: password        # DB 비밀번호로 변경

ai:
  llm:
    url: https://kwaklabs.com/api/v1/kwakai/chat  # 로컬 LLM API 주소

git:
  default-repo: /path/to/repo                     # 기본 저장소 경로
  repositories:                                   # 시스템별 저장소 분기
    edunet: /path/to/edunet
```

> 프로파일은 기본 `local`이 활성화됩니다(`SPRING_PROFILES_ACTIVE` 환경변수로 변경 가능).

### 실행

```bash
cd backend
./gradlew bootRun
```

서버가 시작되면 `http://localhost:8080` 에서 API가 제공됩니다.

---

## 3. 프론트엔드 실행

```bash
cd frontend

# 의존성 설치 (최초 1회)
npm install

# 개발 서버 실행
npm run dev
```

> 프론트엔드 개발 서버는 `/api` 요청을 자동으로 `http://localhost:8080` 으로 프록시합니다.

---

## 4. 주요 기능

### 반영 계획서 목록 (아코디언)
- 제목 검색 + 페이지네이션
- 제목을 클릭하면 해당 계획서의 **반영 이력 목록(SR 단위)** 이 아래에 전체 폭으로 펼쳐집니다.
- 각 행에서 **SR 번호를 인라인으로 입력/수정**할 수 있고, 최종확인 상태가 표시됩니다.

### 반영 계획서 생성
- 공유 Excel 업로드 → 반영 계획서 뼈대 자동 생성
- 선택적으로 git 저장소/커밋 범위 정보 포함

### 엑셀 일괄 등록 (시트 = 날짜)
- 시트명이 `MM.DD` 형식인 다중 시트 Excel을 업로드하면, **시트(날짜)별로 반영 계획서**를 생성합니다.
- 계획서 제목은 **연도 2026 고정**으로 `2026-MM-DD` 형식입니다.
- 각 시트 상단 "시스템 반영 작업 요청" 표의 **행 1건 = SR 1건 = 반영 이력 1건**으로 저장합니다.
  - 사용 컬럼: 서비스 / 작업내용 / 요청자 / 작업자 / TEST URL(검수·운영 분리) / TEST 상세 /
    Frontend / Backend / 비고 / 최종확인 (그 외 컬럼·하단 반영 대상 표는 무시)
  - `No`(B열)가 숫자이고 작업내용 또는 TEST 상세가 채워진 행만 SR로 인식합니다.
- **이미 같은 날짜(제목)의 계획서가 DB에 있으면 해당 시트는 무시**합니다(중복 등록 방지).
- 날짜 형식이 잘못되었거나 SR 행이 없는 시트도 건너뜁니다.
- `summarize=true`(기본값)이면 시트별 작업내용을 **LLM으로 한 줄 요약**해 `summary`에 저장하고
  목록에서 날짜 옆에 함께 보여줍니다. LLM 호출이 실패하면 요약 없이 계획서만 생성합니다.
- 응답으로 생성/스킵 결과 요약(생성 건수·요약, 기존 스킵 날짜, 무효 시트)을 돌려줍니다.

### 사이드이펙트 / 취약점 분석
- 반영 계획서 상세 화면에서 git diff(`From ~ To` 커밋) 기반으로 분석 보고서를 생성·다운로드

### 장애 / 장애 분석
- 반영 이력 하위로 장애를 등록하고, 장애별 분석 문서를 생성

### 화면 탐색
- 모든 화면 상단에 전체 계층 경로를 보여주는 **브레드크럼** 제공

---

## 5. API 엔드포인트 요약

```
# 반영 계획서
GET    /api/release-plans?keyword=&page=0&size=10
POST   /api/release-plans                       # Excel 업로드로 생성
POST   /api/release-plans/import                # 다중 시트 Excel 일괄 등록(시트=날짜, 중복 날짜 무시)
GET    /api/release-plans/{id}
DELETE /api/release-plans/{id}                  # 계획서 + 하위(반영 이력/장애/장애 분석) 삭제
POST   /api/release-plans/{id}/side-effect
POST   /api/release-plans/{id}/vuln-check

# 반영 이력 (반영 계획서 하위)
GET    /api/release-plans/{planId}/histories
POST   /api/release-plans/{planId}/histories
GET    /api/release-histories/{id}
PATCH  /api/release-histories/{id}/sr-number
PATCH  /api/release-histories/{id}/final-confirm

# git 커밋 (저장소는 백엔드에서 system 키로 분기)
GET    /api/git/systems
GET    /api/git/commits?system=&count=

# 장애 (반영 이력 하위)
GET    /api/release-histories/{historyId}/incidents
POST   /api/release-histories/{historyId}/incidents
GET    /api/incidents/{id}

# 장애 분석 (장애 하위)
GET    /api/incidents/{incidentId}/analyses
POST   /api/incidents/{incidentId}/analyses

# 문서 다운로드
GET    /api/document/{filename}/download
```

---

## 6. 프로젝트 구조

```
incident-management/
├── backend/                          # Spring Boot 3 백엔드
│   ├── build.gradle
│   └── src/main/
│       ├── java/com/incident/management/
│       │   ├── common/               # GitAdapter (git CLI 호출)
│       │   ├── config/               # GitProperties 등 설정
│       │   ├── controller/           # REST 컨트롤러
│       │   ├── service/              # 비즈니스 로직, GitService, RedmineService
│       │   ├── entity/               # JPA 엔티티 (ReleasePlan/ReleaseHistory/Incident/...)
│       │   ├── repository/           # Spring Data JPA
│       │   ├── dto/                  # Request / Response DTO
│       │   └── exception/            # 전역 예외 처리
│       └── resources/
│           ├── application.yml               # 공통 설정 (추적)
│           ├── application-local.yml         # 로컬 비밀 값 (gitignore)
│           ├── application-local.yml.example # 로컬 설정 템플릿 (추적)
│           └── data.sql                      # 샘플 시드 데이터
└── frontend/                         # Vue 3 프론트엔드
    ├── package.json
    └── src/
        ├── views/                    # 목록/상세 화면
        ├── components/               # Breadcrumb 등 공통 컴포넌트
        ├── services/api.js           # Axios API 클라이언트
        └── router/                   # Vue Router
```
